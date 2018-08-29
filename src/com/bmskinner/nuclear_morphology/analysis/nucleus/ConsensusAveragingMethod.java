/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.analysis.nucleus;

import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bmskinner.nuclear_morphology.analysis.ComponentMeasurer;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.Profileable;
import com.bmskinner.nuclear_morphology.components.generic.BorderTagObject;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderPointException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.UnprofilableObjectException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclei.DefaultConsensusNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.NucleusFactory;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.stats.Stats;
import com.bmskinner.nuclear_morphology.utility.CircleTools;

/**
 * This method refolds the consensus nucleus based on averaging the positions of
 * equally spaced points around the perimeter of each vertical nucleus in the
 * dataset.
 * 
 * @author ben
 * @since 1.13.5
 *
 */
public class ConsensusAveragingMethod extends SingleDatasetAnalysisMethod {

    private static final double PROFILE_LENGTH = 200d;

    public ConsensusAveragingMethod(final IAnalysisDataset dataset) {
        super(dataset);
    }

    @Override
    public IAnalysisResult call() throws Exception {

        run();
        IAnalysisResult r = new DefaultAnalysisResult(dataset);
        return r;
    }

    private void run() {

        try {
            List<IPoint> border = getPointAverage();
            Nucleus refoldNucleus = makeConsensus(border);
            dataset.getCollection().setConsensus(refoldNucleus);

        } catch (Exception e) {
            error("Error getting points", e);
        }
    }

    private Nucleus makeConsensus(List<IPoint> list)
            throws UnprofilableObjectException, MissingOptionException, ComponentCreationException,
            UnavailableBorderTagException, ProfileException, UnavailableProfileTypeException {

        IPoint com = IPoint.makeNew(0, 0);

        NucleusFactory fact = new NucleusFactory(dataset.getCollection().getNucleusType());
        Nucleus n = fact.buildInstance(list, new File("Empty"), 0, com);

        // Calculate the stats for the new consensus
        // Required for angle window size calculation
        double perim = ComponentMeasurer.calculatePerimeter(n);
        n.setStatistic(PlottableStatistic.PERIMETER, perim);
        n.initialise(Profileable.DEFAULT_PROFILE_WINDOW_PROPORTION);

        DefaultConsensusNucleus cons = new DefaultConsensusNucleus(n, dataset.getCollection().getNucleusType());

        for (Tag tag : BorderTagObject.values()) {

            if (Tag.INTERSECTION_POINT.equals(tag)) // not relevant here
                continue;

            if (dataset.getCollection().getProfileCollection().hasBorderTag(tag)) {
                IProfile median = dataset.getCollection().getProfileCollection().getProfile(ProfileType.ANGLE, tag,
                        Stats.MEDIAN);
                int newIndex = cons.getProfile(ProfileType.ANGLE).findBestFitOffset(median);
                cons.setBorderTag(tag, newIndex);
            }

        }

        // Check the profile generated

        IProfile median = dataset.getCollection().getProfileCollection().getProfile(ProfileType.ANGLE,
                Tag.REFERENCE_POINT, Stats.MEDIAN);
        IProfile nucProfile = cons.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
        double diff = median.absoluteSquareDifference(nucProfile);

        // // Adjust segments to fit size

        if(dataset.getCollection().getProfileCollection().hasSegments()) {
        	ISegmentedProfile profile = cons.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
        	List<IBorderSegment> segs = dataset.getCollection().getProfileCollection().getSegments(Tag.REFERENCE_POINT);
        	List<IBorderSegment> newSegs = IBorderSegment.scaleSegments(segs, profile.size());
        	profile.setSegments(newSegs);
        	cons.setProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, profile);
        }
        
        // Do not use DefaultNucleus::roateVertically; it will not align properly
        if (cons.hasBorderTag(Tag.TOP_VERTICAL) && cons.hasBorderTag(Tag.BOTTOM_VERTICAL)) {
            cons.alignPointsOnVertical(cons.getBorderTag(Tag.TOP_VERTICAL), cons.getBorderTag(Tag.BOTTOM_VERTICAL));

            if (cons.getBorderPoint(Tag.REFERENCE_POINT).getX() > cons.getCentreOfMass().getX())
                cons.flipXAroundPoint(cons.getCentreOfMass());
        }
        return cons;

    }

    private List<IPoint> getPointAverage() throws UnavailableBorderTagException, UnavailableProfileTypeException,
            ProfileException, UnavailableBorderPointException, MissingOptionException {

        List<IPoint> result = new ArrayList<>();

        final Map<Double, List<IPoint>> map = new HashMap<Double, List<IPoint>>();

        IPoint com = IPoint.makeNew(0, 0);
        dataset.getCollection().getNuclei().stream().forEach(n -> {
            try {
                
                Nucleus v = n.getVerticallyRotatedNucleus();

                v.moveCentreOfMass(com);
                IProfile p = v.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);

                for (int i = 0; i < PROFILE_LENGTH; i++) {

                    double d = ((double) i) / PROFILE_LENGTH;

                    if (map.get(d) == null) {
                        map.put(d, new ArrayList<IPoint>());
                    }
                    List<IPoint> list = map.get(d);

                    int index = p.getIndexOfFraction(d);

                    int offset = v.getOffsetBorderIndex(Tag.REFERENCE_POINT, index);

                    IPoint point;
                    point = v.getBorderPoint(offset);
                    list.add(point);
                }
            } catch (Exception e1) {
                error("Error on nucleus " + n.getNameAndNumber(), e1);
            }

        });

        for (int i = 0; i < PROFILE_LENGTH; i++) {

            double d = ((double) i) / PROFILE_LENGTH;
            List<IPoint> list = map.get(d);
            result.add(calculateMedianPoint(list));
            fireProgressEvent();
        }

        return result;
    }

    private synchronized IPoint calculateMedianPoint(List<IPoint> list) {
        double[] xpoints = new double[list.size()];
        double[] ypoints = new double[list.size()];

        for (int i = 0; i < list.size(); i++) {
            IPoint p = list.get(i);

            xpoints[i] = p.getX();
            ypoints[i] = p.getY();
        }

        double xMed = Stats.quartile(xpoints, Stats.MEDIAN);
        double yMed = Stats.quartile(ypoints, Stats.MEDIAN);

        IPoint avgRP = IPoint.makeNew(xMed, yMed);
        return avgRP;
    }

    private static boolean intersects(Path2D path, Line2D line) {
        double x1 = -1, y1 = -1, x2 = -1, y2 = -1;
        for (PathIterator pi = path.getPathIterator(null); !pi.isDone(); pi.next()) {
            double[] coordinates = new double[6];
            switch (pi.currentSegment(coordinates)) {
            case PathIterator.SEG_MOVETO:
            case PathIterator.SEG_LINETO: {
                if (x1 == -1 && y1 == -1) {
                    x1 = coordinates[0];
                    y1 = coordinates[1];
                    break;
                }
                if (x2 == -1 && y2 == -1) {
                    x2 = coordinates[0];
                    y2 = coordinates[1];
                    break;
                }
                break;
            }
            }
            if (x1 != -1 && y1 != -1 && x2 != -1 && y2 != -1) {
                Line2D segment = new Line2D.Double(x1, y1, x2, y2);
                if (segment.intersectsLine(line)) {
                    return true;
                }
                x1 = -1;
                y1 = -1;
                x2 = -1;
                y2 = -1;
            }
        }
        return false;
    }

    /**
     * Find the points at the intersection of two circles, and return one of
     * them
     * 
     * @param first
     * @param r1
     * @param com
     * @param r2
     * @return
     */
    private IPoint calculateSecondPoint(IPoint first, double r1, IPoint com, double r2) {

        // IPoint lastPoint; // needed to get the position of second point
        // correct

        /*
         * Looking for the intersection of the circles with radii described
         */
        IPoint[] inters = CircleTools.findIntersections(first, r1, com, r2);

        IPoint i0 = inters[0];
        IPoint i1 = inters[1];

        // Check the angle with the last point in the object

        // IPoint[] interesctions = null;
        // boolean loop = true;
        // while( loop && r1 > 1 && r1 < 1000){
        // try {
        // interesctions = CircleTools.findIntersections(first, r1, com, r2 );
        // loop=false;
        // } catch(IllegalArgumentException e){
        // log(e.getMessage());
        // r1++;
        // log("Increasing distance to "+r1);
        //
        // }
        // }

        return i0.getX() < 0 ? i0 : i1;

        // log("Second point possible values:");
        // log(i0.toString());
        // log(i1.toString());
        // return i0;
    }

}
