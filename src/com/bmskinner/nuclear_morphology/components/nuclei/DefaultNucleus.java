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


package com.bmskinner.nuclear_morphology.components.nuclei;

import ij.gui.Roi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileIndexFinder;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileIndexFinder.NoDetectedIndexException;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalAnalyser;
import com.bmskinner.nuclear_morphology.charting.datasets.ChartDatasetCreationException;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.ProfileableCellularComponent;
import com.bmskinner.nuclear_morphology.components.SegmentedCellularComponent;
import com.bmskinner.nuclear_morphology.components.generic.DefaultBorderPoint;
import com.bmskinner.nuclear_morphology.components.generic.DoubleEquation;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.LineEquation;
import com.bmskinner.nuclear_morphology.components.generic.OpenBorderSegment;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderPointException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.UnprofilableObjectException;
import com.bmskinner.nuclear_morphology.components.nuclear.DefaultSignalCollection;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalCollection;
import com.bmskinner.nuclear_morphology.components.rules.RuleSet;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.utility.AngleTools;

/**
 * The standard round nucleus, implementing {@link Nucleus}. All non-round
 * nuclei extend this.
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultNucleus extends SegmentedCellularComponent implements Nucleus {

    private static final long serialVersionUID = 1L;

    protected int nucleusNumber; // the number of the nucleus in the current
                                 // image

    protected ISignalCollection signalCollection = new DefaultSignalCollection();

    protected transient Nucleus verticalNucleus = null; // cache the vertically
                                                        // rotated nucleus

    protected transient boolean canReverse = true;

    /**
     * Construct with an ROI, a source image and channel, and the original
     * position in the source image
     * 
     * @param roi
     * @param f
     * @param channel
     * @param position
     * @param centreOfMass
     */
    public DefaultNucleus(@NonNull Roi roi, @NonNull IPoint centreOfMass, @NonNull File f, int channel, int[] position, int number) {
        super(roi, centreOfMass, f, channel, position);
        this.nucleusNumber = number;
    }

    /**
     * Construct from a template Nucleus
     * 
     * @param n the template
     * @throws UnprofilableObjectException
     */
    protected DefaultNucleus(Nucleus n) throws UnprofilableObjectException {
        super(n);
        nucleusNumber = n.getNucleusNumber();
        signalCollection = new DefaultSignalCollection(n.getSignalCollection());
        finest("Created new nucleus");
    }

    @Override
    public Nucleus duplicate() {
        try {
            return new DefaultNucleus(this);
        } catch (UnprofilableObjectException e) {
            warn("Duplication failed");
            stack("Error duplicating nucleus", e);
        }
        return null;
    }

    /*
     * Finds the key points of interest around the border of the Nucleus. Can
     * use several different methods, and take a best-fit, or just use one. The
     * default in a round nucleus is to get the longest diameter and set this as
     * the head/tail axis.
     */
    @Override
    public void findPointsAroundBorder() throws ComponentCreationException {

        try {

        	// estimate the RP using the round rules
            RuleSet rpSet = RuleSet.roundRPRuleSet();
            IProfile p = this.getProfile(rpSet.getType());
            ProfileIndexFinder f = new ProfileIndexFinder();
            int rpIndex = f.identifyIndex(p, rpSet);

            setBorderTag(Tag.REFERENCE_POINT, rpIndex);
            setBorderTag(Tag.ORIENTATION_POINT, rpIndex);

            if (!this.isProfileOrientationOK() && canReverse) {
                reverse();

                // the number of border points can change when reversing
                // due to float interpolation from different starting positions
                // so do the whole thing again
                initialise(this.getWindowProportion(ProfileType.ANGLE));
                canReverse = false;
                findPointsAroundBorder();
            }

        } catch (UnavailableProfileTypeException e) {
            stack("Error getting profile type", e);
        } catch (NoDetectedIndexException e) {
            fine("Unable to detect RP in nucleus");
            setBorderTag(Tag.REFERENCE_POINT, 0);
            setBorderTag(Tag.ORIENTATION_POINT, 0);
        }
    }

    @Override
    public void initialise(double proportion) throws ComponentCreationException {

        super.initialise(proportion);

        try {
            SignalAnalyser s = new SignalAnalyser();
            s.calculateSignalDistancesFromCoM(this);
            s.calculateFractionalSignalDistancesFromCoM(this);
        } catch (UnavailableBorderPointException e) {
            stack("Unable to get border point", e);
        }
    }

    @Override
    public int getNucleusNumber() {
        return nucleusNumber;
    }

    @Override
    public String getNameAndNumber() {
        return getSourceFileName() + "-" + getNucleusNumber();
    }

    @Override
    public String getPathAndNumber() {
        return getSourceFile() + File.separator + nucleusNumber;
    }

    @Override
    public void setScale(double scale) {
        super.setScale(scale);

        for (INuclearSignal s : this.getSignalCollection().getAllSignals()) {
            s.setScale(scale);
        }

    }

    @Override
    protected double calculateStatistic(PlottableStatistic stat) {

        double result = super.calculateStatistic(stat);

        // Note - variability will remain zero here
        // These stats are specific to nuclei
        
        if (PlottableStatistic.ASPECT.equals(stat))
            return getAspectRatio();

        if (PlottableStatistic.BOUNDING_HEIGHT.equals(stat))
            return getVerticallyRotatedNucleus().getBounds().getHeight();

        if (PlottableStatistic.BOUNDING_WIDTH.equals(stat))
            return getVerticallyRotatedNucleus().getBounds().getWidth();

        if (PlottableStatistic.OP_RP_ANGLE.equals(stat)) {
            try {
                result = getCentreOfMass().findSmallestAngle(this.getBorderTag(Tag.REFERENCE_POINT),
                        this.getBorderTag(Tag.ORIENTATION_POINT));
            } catch (UnavailableBorderTagException e) {
                stack("Cannot get border tag", e);
                result = 0;
            }
        }

        return result;
    }

    private double getAspectRatio() {
        double h = getVerticallyRotatedNucleus().getBounds().getHeight();
        double w = getVerticallyRotatedNucleus().getBounds().getWidth();

        return h / w;
    }

    protected void setSignals(ISignalCollection collection) {
        signalCollection = collection;
    }

    @Override
    public boolean isClockwiseRP() {
        return false;
    }

    public ISignalCollection getSignalCollection() {
        return signalCollection;
    }

    public void updateSignalAngle(UUID channel, int signal, double angle) {
        signalCollection.getSignals(channel).get(signal).setStatistic(PlottableStatistic.ANGLE, angle);
    }

    // do not move this into SignalCollection - it is overridden in
    // RodentSpermNucleus
    public void calculateSignalAnglesFromPoint(@NonNull IBorderPoint p) {

    	//TODO - there is an issue with pigs. The smallest angle wrt the OP is not the correct angle to choose
    	
        for (UUID signalGroup : signalCollection.getSignalGroupIds()) {

            if (signalCollection.hasSignal(signalGroup)) {

                List<INuclearSignal> signals = signalCollection.getSignals(signalGroup);

                for (INuclearSignal s : signals) {

                    double angle = this.getCentreOfMass().findAbsoluteAngle(p, s.getCentreOfMass());
                    s.setStatistic(PlottableStatistic.ANGLE, angle);

                }
            }
        }
    }

    /*
     * Get a readout of the state of the nucleus Used only for debugging
     */
    public String dumpInfo(int type) {
        String result = "";
        result += "Dumping nucleus info: " + this.getNameAndNumber() + "\n";
        result += "    Border length: " + this.getBorderLength() + "\n";
        result += "    CoM: " + this.getCentreOfMass().toString() + "\n";
        if (type == ALL_POINTS || type == BORDER_POINTS) {
            result += "    Border:\n";
            for (int i = 0; i < this.getBorderLength(); i++) {
                IBorderPoint p = this.getBorderPoint(i);
                result += "      Index " + i + ": " + p.getX() + "\t" + p.getY() + "\t" + this.getBorderTag(i) + "\n";
            }
        }
        if (type == ALL_POINTS || type == BORDER_TAGS) {
            result += "    Points of interest:\n";
            Map<Tag, Integer> pointHash = this.getBorderTags();

            for (Tag s : pointHash.keySet()) {
                IBorderPoint p = getBorderPoint(pointHash.get(s));
                result += "    " + s + ": " + p.getX() + "    " + p.getY() + " at index " + pointHash.get(s) + "\n";
            }
        }
        return result;
    }

    /**
     * Checks if the smoothed array nuclear shape profile has the appropriate
     * orientation.Counts the number of points above 180 degrees in each half of
     * the array.
     * TODO -- this is mouse sperm specific
     * @return
     */
    @Override
    public boolean isProfileOrientationOK() {
        int frontPoints = 0;
        int rearPoints = 0;

        IProfile profile;
        try {
            profile = this.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
        } catch (ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException e) {
            fine("Error getting profile", e);
            return false;
        }

        int midPoint = getBorderLength() >> 1;
        for (int i = 0; i < getBorderLength(); i++) { // integrate points
                                                           // over 180
            if (i < midPoint) 
                frontPoints += profile.get(i);
            if (i > midPoint) 
                rearPoints += profile.get(i);
        }
        
        // if the maxIndex is closer to the end than the beginning
        return frontPoints > rearPoints;
    }

    @Override
    public void updateVerticallyRotatedNucleus() {
    	verticalNucleus = null;
        verticalNucleus = getVerticallyRotatedNucleus();
    }

    @Override
    public Nucleus getVerticallyRotatedNucleus() {
        if (verticalNucleus != null)
            return verticalNucleus;

        // Make an exact copy of the nucleus
        finer("Creating vertical nucleus");
        verticalNucleus = this.duplicate();

        // At this point the new nucleus was created at the original image
        // coordinates
        // of the template nucleus, then moved to the current CoM.
        // Now align the nucleus on vertical.

        verticalNucleus.alignVertically();

        double h = verticalNucleus.getBounds().getHeight();
        double w = verticalNucleus.getBounds().getWidth();

        setStatistic(PlottableStatistic.BOUNDING_HEIGHT, h);
        setStatistic(PlottableStatistic.BOUNDING_WIDTH, w);

        double aspect = h / w;
        setStatistic(PlottableStatistic.ASPECT, aspect);

        setStatistic(PlottableStatistic.BODY_WIDTH, STAT_NOT_CALCULATED);
        setStatistic(PlottableStatistic.HOOK_LENGTH, STAT_NOT_CALCULATED);

        return verticalNucleus;
    }

    @Override
    public void moveCentreOfMass(IPoint point) {

        double diffX = point.getX() - getCentreOfMass().getX();
        double diffY = point.getY() - getCentreOfMass().getY();
        offset(diffX, diffY);
    }

    @Override
    public void offset(double xOffset, double yOffset) {

        super.offset(xOffset, yOffset);

        // Move signals within the nucleus
        if (signalCollection != null) {
            for (INuclearSignal s : this.signalCollection.getAllSignals()) {
                s.offset(xOffset, yOffset);
            }
        }
    }

    /*
     * ############################################# 
     * Methods implementing the Rotatable interface
     * #############################################
     */

    @Override
    public void alignVertically() {

        boolean useTVandBV = hasBorderTag(Tag.TOP_VERTICAL) && hasBorderTag(Tag.BOTTOM_VERTICAL);
        int topPoint = getBorderIndex(Tag.TOP_VERTICAL);
        int bottomPoint = getBorderIndex(Tag.BOTTOM_VERTICAL);

        // check if the point was set but not found
        useTVandBV &= topPoint!=BORDER_INDEX_NOT_FOUND;
        useTVandBV &= bottomPoint!=BORDER_INDEX_NOT_FOUND;
        useTVandBV &= topPoint != bottomPoint; // Situation when something went very wrong
        
        if (useTVandBV) {

            try {
            	IBorderPoint[] points = getBorderPointsForVerticalAlignment();
                alignPointsOnVertical(points[0], points[1]);

            } catch (UnavailableBorderTagException | UnavailableProfileTypeException e) {
                stack("Cannot get border tag or profile", e);
                try {
                    rotatePointToBottom(getBorderPoint(Tag.ORIENTATION_POINT));
                } catch (UnavailableBorderTagException e1) {
                    stack("Cannot get border tag", e1);
                }
            }
        } else {

            // Default if top and bottom vertical points have not been specified
            try {
                rotatePointToBottom(getBorderPoint(Tag.ORIENTATION_POINT));
            } catch (UnavailableBorderTagException e) {
                stack("Cannot get border tag", e);
            }
        }

    }

    /**
     * Detect the points that can be used for vertical alignment.These are based
     * on the BorderTags TOP_VERTICAL and BOTTOM_VETICAL. The actual points
     * returned are not necessarily on the border of the nucleus; a bibble
     * correction is performed on the line drawn between the two border points,
     * minimising the sum-of-squares to each border point within the region
     * covered by the line.
     * 
     * @return
     * @throws UnavailableBorderTagException
     * @throws UnavailableProfileTypeException
     */
    private IBorderPoint[] getBorderPointsForVerticalAlignment()
            throws UnavailableBorderTagException, UnavailableProfileTypeException {

        IBorderPoint topPoint;
        IBorderPoint bottomPoint;

        topPoint = this.getBorderTag(Tag.TOP_VERTICAL);
        bottomPoint = this.getBorderTag(Tag.BOTTOM_VERTICAL);

        // Find the border points between the top and bottom verticals
        List<IBorderPoint> pointsInRegion = new ArrayList<IBorderPoint>();

        int topIndex = this.getBorderIndex(Tag.TOP_VERTICAL);
        int btmIndex = this.getBorderIndex(Tag.BOTTOM_VERTICAL);
        int totalSize = this.getProfile(ProfileType.ANGLE).size();

        // A segment has built in methods for iterating through just the points
        // it contains
        // TODO: This has problems if we have short regions. Replace.
        
        for(int i= topIndex; i<=btmIndex; i++){
            
        }
        IBorderSegment region = new OpenBorderSegment(topIndex, btmIndex, totalSize);

        int index = topIndex;

        Iterator<Integer> it = region.iterator();

        while (it.hasNext()) {
            index = it.next();
            pointsInRegion.add(this.getBorderPoint(index));
        }

        // As an anti-bibble defence, get a best fit line acrosss the region
        // Use the line of best fit to find appropriate top and bottom vertical
        // points
        LineEquation eq = DoubleEquation.calculateBestFitLine(pointsInRegion);

        // Take values along the best fit line that are close to the original TV
        // and BV

        // What about when the TV or BV are in the bibble? TODO

        IBorderPoint top = new DefaultBorderPoint(topPoint.getX(), eq.getY(topPoint.getX()));
        IBorderPoint btm = new DefaultBorderPoint(eq.getX(bottomPoint.getY()), bottomPoint.getY());

        return new IBorderPoint[] { top, btm };

    }

    @Override
    public void flipXAroundPoint(IPoint p) {
        super.flipXAroundPoint(p);

        for (UUID id : signalCollection.getSignalGroupIds()) {
            signalCollection.getSignals(id).stream().forEach(s -> s.flipXAroundPoint(p));
        }

    }

    @Override
    public void rotate(double angle) {

        super.rotate(angle);

        if (angle != 0) {

            for (UUID id : signalCollection.getSignalGroupIds()) {

                signalCollection.getSignals(id).parallelStream().forEach(s -> {

                    s.rotate(angle);

                    // get the new signal centre of mass based on the nucleus
                    // rotation

                    IPoint p = AngleTools.rotateAboutPoint(s.getCentreOfMass(), getCentreOfMass(), angle);
                    s.moveCentreOfMass(p);
                });

            }
        }
    }

    /*
     * ############################################# 
     * Object methods
     * #############################################
     */

    /**
     * Describes the nucleus state
     * 
     * @return
     */
    public String toString() {
        String newLine = System.getProperty("line.separator");
        StringBuilder b = new StringBuilder();

        b.append(this.getNameAndNumber());
        b.append(newLine);
        b.append(this.getSignalCollection().toString());
        b.append(newLine);
        return b.toString();
    }

    @Override
    public int compareTo(Nucleus n) {

        int number = this.getNucleusNumber();
        String name = this.getSourceFileNameWithoutExtension();

        // Compare on image name.
        // If that is equal, compare on nucleus number

        int byName = name.compareTo(n.getSourceFileNameWithoutExtension());

        if (byName == 0) {

            if (number < n.getNucleusNumber()) {
                return -1;
            } else if (number > n.getNucleusNumber()) {
                return 1;
            } else {
                return 0;
            }

        } else {
            return byName;
        }

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();

        result = prime * result + nucleusNumber;

        result = prime * result + ((signalCollection == null) ? 0 : signalCollection.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DefaultNucleus other = (DefaultNucleus) obj;
        if (nucleusNumber != other.nucleusNumber)
            return false;

        if (signalCollection == null) {
            if (other.signalCollection != null)
                return false;
        } else if (!signalCollection.equals(other.signalCollection))
            return false;
        return true;
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

        in.defaultReadObject();
        this.verticalNucleus = null;
    }
}
