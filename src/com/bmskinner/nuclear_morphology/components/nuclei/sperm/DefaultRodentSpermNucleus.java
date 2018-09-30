/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/

package com.bmskinner.nuclear_morphology.components.nuclei.sperm;

import ij.gui.Roi;
import ij.process.FloatPolygon;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileIndexFinder;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileIndexFinder.NoDetectedIndexException;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.generic.BooleanProfile;
import com.bmskinner.nuclear_morphology.components.generic.DefaultBorderPoint;
import com.bmskinner.nuclear_morphology.components.generic.DoubleEquation;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.LineEquation;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.UnprofilableObjectException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclei.AbstractAsymmetricNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.rules.RuleSet;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;

/**
 * The standard rodent sperm nucleus
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultRodentSpermNucleus extends AbstractAsymmetricNucleus {

    private static final long serialVersionUID = 1L;

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
    public DefaultRodentSpermNucleus(Roi roi, IPoint centreOfMass, File f, int channel, int[] position, int number) {
        super(roi, centreOfMass, f, channel, position, number);
    }

    protected DefaultRodentSpermNucleus(Nucleus n) throws UnprofilableObjectException {
        super(n);
    }

    @Override
    public Nucleus duplicate() {
        try {
            return new DefaultRodentSpermNucleus(this);
        } catch (UnprofilableObjectException e) {
            stack("Cannot duplicate nucleus", e);
            warn("Error duplicating nucleus");
        }
        return null;
    }

    @Override
    protected double calculateStatistic(PlottableStatistic stat) {
        double result = super.calculateStatistic(stat);

        if (PlottableStatistic.HOOK_LENGTH.equals(stat)) {
            return getHookOrBodyLength(true);

        }

        if (PlottableStatistic.BODY_WIDTH.equals(stat)) {
            return getHookOrBodyLength(false);
        }

        return result;

    }

    @Override
    public void setBorderTag(Tag tag, int i) {
        super.setBorderTag(tag, i);

        // If the flat region moved, update the cached lengths
        if (this.hasBorderTag(Tag.TOP_VERTICAL) && this.hasBorderTag(Tag.BOTTOM_VERTICAL)) {

            if (tag.equals(Tag.TOP_VERTICAL) || tag.equals(Tag.BOTTOM_VERTICAL)) {

                // Invalidate previous data
                setStatistic(PlottableStatistic.HOOK_LENGTH, STAT_NOT_CALCULATED);
                setStatistic(PlottableStatistic.BODY_WIDTH, STAT_NOT_CALCULATED);

                calculateHookAndBodyLength();
            }
        }

    }

    private double getHookOrBodyLength(boolean useHook) {

        // check stat is present before calling a getStatistic
        if (hasStatistic(PlottableStatistic.HOOK_LENGTH) || hasStatistic(PlottableStatistic.BODY_WIDTH)) {

            if (getStatistic(PlottableStatistic.HOOK_LENGTH) == STAT_NOT_CALCULATED
                    || getStatistic(PlottableStatistic.BODY_WIDTH) == STAT_NOT_CALCULATED) {
                calculateHookAndBodyLength();
            }

        } else {
            calculateHookAndBodyLength();
        }

        double stat = useHook ? getStatistic(PlottableStatistic.HOOK_LENGTH)
                : getStatistic(PlottableStatistic.BODY_WIDTH);

        stat = stat == BORDER_POINT_NOT_PRESENT ? 0 : stat; // -2 is the error
                                                            // code when TV and
                                                            // BV are not
                                                            // present. Using -1
                                                            // will cause
                                                            // infinite loop.

        return stat;

    }

    private void calculateHookAndBodyLength() {

        // Start with the vertically rotated nucleus
        Nucleus testNucleus = getVerticallyRotatedNucleus();

        if (testNucleus == null) {
            setStatistic(PlottableStatistic.HOOK_LENGTH, ERROR_CALCULATING_STAT);
            setStatistic(PlottableStatistic.BODY_WIDTH, ERROR_CALCULATING_STAT);
            return;
        }

        if (!testNucleus.hasBorderTag(Tag.TOP_VERTICAL) || !testNucleus.hasBorderTag(Tag.BOTTOM_VERTICAL)) {
            setStatistic(PlottableStatistic.HOOK_LENGTH, BORDER_POINT_NOT_PRESENT);
            setStatistic(PlottableStatistic.BODY_WIDTH, BORDER_POINT_NOT_PRESENT);
            return;
        }

        /*
         * Get the X position of the top vertical
         */
        double vertX;
        try {
            vertX = testNucleus.getBorderPoint(Tag.TOP_VERTICAL).getX();
        } catch (UnavailableBorderTagException e) {
            stack("Cannot get border tag", e);
            setStatistic(PlottableStatistic.HOOK_LENGTH, BORDER_POINT_NOT_PRESENT);
            setStatistic(PlottableStatistic.BODY_WIDTH, BORDER_POINT_NOT_PRESENT);
            return;

        }

        /*
         * Find the x values in the bounding box of the vertical nucleus.
         */
        FloatPolygon p = testNucleus.toPolygon();
        double maxBoundingX = p.getBounds().getMaxX();
        double minBoundingX = p.getBounds().getMinX();

        if (vertX < minBoundingX || vertX > maxBoundingX) {


            // The chosen vertical points is outside the bounding box of the
            // nucleus
            IndexOutOfBoundsException e = new IndexOutOfBoundsException("Vertical point x is outside nucleus bounds");
            stack("Vertical point " + vertX + " is out of bounds " + minBoundingX + " - " + maxBoundingX, e);
            setStatistic(PlottableStatistic.HOOK_LENGTH, ERROR_CALCULATING_STAT);
            setStatistic(PlottableStatistic.BODY_WIDTH, ERROR_CALCULATING_STAT);
            return;
        }
        
        /*
         * To determine if the point is hook or hump, take the X position of the
         * tip. This must lie on the hook side of the vertX
         */

        double dHook = 0;
        double dBody = 0;

        if(testNucleus.isClockwiseRP()){
        	dBody = vertX - minBoundingX;
        	dHook = maxBoundingX - vertX;
        	
        } else{
        	dHook = vertX - minBoundingX;
        	dBody = maxBoundingX - vertX;
        }

        setStatistic(PlottableStatistic.HOOK_LENGTH, dHook);
        setStatistic(PlottableStatistic.BODY_WIDTH, dBody);
    }

    /**
     * Get a copy of the points in the hook roi
     * 
     * @return
     */
    public List<IBorderPoint> getHookRoi() {

        List<IBorderPoint> result = new ArrayList<>(0);

        IBorderPoint testPoint;
        IBorderPoint referencePoint;
        IBorderPoint interSectionPoint;
        IBorderPoint orientationPoint;

        try {

            testPoint = this.getBorderPoint (Tag.REFERENCE_POINT);
            referencePoint = this.getBorderPoint (Tag.REFERENCE_POINT);
            interSectionPoint = this.getBorderPoint (Tag.INTERSECTION_POINT);
            orientationPoint = this.getBorderPoint (Tag.ORIENTATION_POINT);

        } catch (UnavailableBorderTagException e) {
            fine("Cannot get border tag", e);
            return result;
        }

        /*
         * Go from the reference point. We hit either the IP or the OP depending
         * on direction. On hitting one, move to the other and continue until
         * we're back at the RP
         */

        // boolean hasHitPoint = false;
        int i = 0;
        IBorderPoint continuePoint = null;

        while (testPoint.hasNextPoint()) {
            result.add(testPoint);

            if (testPoint.overlapsPerfectly(interSectionPoint)) {
                continuePoint = orientationPoint;
                break;
            }

            if (testPoint.overlapsPerfectly(orientationPoint)) {
                continuePoint = interSectionPoint;
                break;
            }

            testPoint = testPoint.nextPoint();

            /*
             * Only allow the loop to go around the nucleus once
             */
            if (testPoint.overlapsPerfectly(referencePoint)) {
                break;
            }

            i++;
            if (i > 1000) {
                warn("Forced break");
                break;
            }
        }

        if (continuePoint == null) {
            warn("Error getting roi - IP and OP not found");
            return result;
        }

        /*
         * Continue until we're back at the RP
         */
        while (continuePoint.hasNextPoint()) {
            result.add(continuePoint);
            // IJ.log("Continue point :"+continuePoint.toString());
            if (continuePoint.overlapsPerfectly(referencePoint.prevPoint())) {
                break;
            }

            continuePoint = continuePoint.nextPoint();
            i++;
            if (i > 2000) {
                warn("Forced break for continue point");
                break;
            }
        }
        return result;

    }

    /*
     * Identify key points: tip, estimated tail position
     */
    @Override
    public void findPointsAroundBorder() throws ComponentCreationException {

        try {

            RuleSet rpSet = RuleSet.mouseSpermRPRuleSet();
            IProfile p = this.getProfile(rpSet.getType());
            ProfileIndexFinder f = new ProfileIndexFinder();

            try {

                int tipIndex = f.identifyIndex(p, rpSet);

                // find tip - use the least angle method
                setBorderTag(Tag.REFERENCE_POINT, tipIndex);

                // decide if the profile is right or left handed; flip if needed
                if (!this.isProfileOrientationOK() && canReverse) {
                    this.reverse(); // reverses all profiles, border array and
                                    // tagged points

                    // the number of border points can change when reversing
                    // due to float interpolation from different starting
                    // positions
                    // so do the whole thing again
                    initialise(this.getWindowProportion(ProfileType.ANGLE));
                    canReverse = false;
                    findPointsAroundBorder();
                }

            } catch (NoDetectedIndexException e) {
                fine("Unable to detect RP in nucleus");
                setBorderTag(Tag.REFERENCE_POINT, 0);
            }

            /*
             * Find the tail point using multiple independent methods. Find a
             * consensus point
             * 
             * Method 1: Use the list of local minima to detect the tail corner
             * This is the corner furthest from the tip. Can be confused as to
             * which side of the sperm head is chosen
             */
            IBorderPoint spermTail2;

            spermTail2 = findTailPointFromMinima();
            this.addTailEstimatePosition(spermTail2);

            /*
             * Method 3: Find the narrowest diameter around the nuclear CoM Draw
             * a line orthogonal, and pick the intersecting border points The
             * border furthest from the tip is the tail
             */
            IBorderPoint spermTail1;

            spermTail1 = this.findTailByNarrowestWidthMethod();
            this.addTailEstimatePosition(spermTail1);

            /*
             * Given distinct methods for finding a tail, take a position
             * between them on roi
             */
            int consensusTailIndex = this.getPositionBetween(spermTail2, spermTail1);
            IBorderPoint consensusTail = this.getBorderPoint(consensusTailIndex);

            setBorderTag(Tag.ORIENTATION_POINT, consensusTailIndex);

            setBorderTag(Tag.INTERSECTION_POINT, this.getBorderIndex(this.findOppositeBorder(consensusTail)));

        } catch (UnavailableBorderTagException e) {
            stack("Error gettting tail position", e);
        } catch (UnavailableProfileTypeException e1) {
            stack("Cannot get profile type", e1);
        }

    }

    /**
     * Create a polygon from the given list of border points
     * 
     * @param list
     *            the list of points
     * @return a polygon enclosing the points
     */
    private FloatPolygon createRoiPolygon(List<IBorderPoint> list) {
        float[] xpoints = new float[list.size() + 1];
        float[] ypoints = new float[list.size() + 1];

        for (int i = 0; i < list.size(); i++) {
            IBorderPoint p = list.get(i);
            xpoints[i] = (float) p.getX();
            ypoints[i] = (float) p.getY();
        }

        // Ensure the polygon is closed
        xpoints[list.size()] = (float) list.get(0).getX();
        ypoints[list.size()] = (float) list.get(0).getY();

        return new FloatPolygon(xpoints, ypoints);
    }

    /**
     * Check if the given point is in the hook side of the nucleus
     * 
     * @param p
     *            the point to test, which must lie within the nucleus
     * @return
     */
    public boolean isHookSide(IPoint p) {

        if (!containsPoint(p)) {
            throw new IllegalArgumentException("Requested point is not in the nucleus: " + p.toString());
        }

        /*
         * Find out which side has been captured. The hook side has the
         * reference point
         */
        FloatPolygon poly = createRoiPolygon(getHookRoi());

        return poly.contains((float) p.getX(), (float) p.getY());

    }

    @Override
    public Nucleus getVerticallyRotatedNucleus() {
        super.getVerticallyRotatedNucleus();
        if (verticalNucleus == null) {
            fine("Unknown error creating vertical nucleus");
            return null;
        }

        /*
         * Get the X position of the reference point
         */
        double vertX;
        try {
            vertX = verticalNucleus.getBorderPoint(Tag.REFERENCE_POINT).getX();
        } catch (UnavailableBorderTagException e) {
            stack("Cannot get RP from vertical nucleus; returning default orientation", e);
            return verticalNucleus;
        }
        /*
         * If the reference point is left of the centre of mass, the nucleus is
         * pointing left. If not, flip the nucleus
         */

        if (vertX > verticalNucleus.getCentreOfMass().getX()) {
            clockwiseRP = true; // this is only set to true, as the default is
                                // false, and will become false after the
                                // nucleus is flipped
            verticalNucleus.flipXAroundPoint(verticalNucleus.getCentreOfMass());
        }

        return verticalNucleus;
    }


    /*
     * Detect the tail based on a list of local minima in an NucleusBorderPoint
     * array. The putative tail is the point furthest from the sum of the
     * distances from the CoM and the tip
     */
    public IBorderPoint findTailPointFromMinima()
            throws UnavailableBorderTagException, UnavailableProfileTypeException {

        // we cannot be sure that the greatest distance between two points will
        // be the endpoints
        // because the hook may begin to curve back on itself. We supplement
        // this basic distance with
        // the distances of each point from the centre of mass. The points with
        // the combined greatest
        // distance are both far from each other and far from the centre, and
        // are a more robust estimate
        // of the true ends of the signal
        double tipToCoMDistance;
        try {
            tipToCoMDistance = this.getBorderPoint (Tag.REFERENCE_POINT).getLengthTo(this.getCentreOfMass());
        } catch (UnavailableBorderTagException e) {
            fine("Cannot get border tag", e);
            throw new UnavailableBorderTagException("Cannot get RP", e);
        }
        BooleanProfile array = this.getProfile(ProfileType.ANGLE).getLocalMinima(5);

        double maxDistance = 0;
        IBorderPoint tail;

        tail = this.getBorderPoint(Tag.REFERENCE_POINT);
        // start at tip, move round

        for (int i = 0; i < array.size(); i++) {
            if (array.get(i) == true) {

                double distanceAcrossCoM = tipToCoMDistance + this.getCentreOfMass().getLengthTo(getBorderPoint(i));
                double distanceBetweenEnds;
                distanceBetweenEnds = this.getBorderPoint(Tag.REFERENCE_POINT).getLengthTo(getBorderPoint(i));

                double totalDistance = distanceAcrossCoM + distanceBetweenEnds;

                if (totalDistance > maxDistance) {
                    maxDistance = totalDistance;
                    tail = getBorderPoint(i);
                }
            }
        }
        return tail;
    }

    /*
     * This is a method for finding a tail point independent of local minima:
     * Find the narrowest diameter around the nuclear CoM Draw a line
     * orthogonal, and pick the intersecting border points The border furthest
     * from the tip is the tail
     */
    public IBorderPoint findTailByNarrowestWidthMethod() throws UnavailableBorderTagException {

        // Find the narrowest point around the CoM
        // For a position in teh roi, draw a line through the CoM to the
        // intersection point
        // Measure the length; if < min length..., store equation and border(s)

        double minDistance = this.getStatistic(PlottableStatistic.MAX_FERET);
        IBorderPoint reference;

        reference = this.getBorderPoint(Tag.REFERENCE_POINT);

        for (int i = 0; i < this.getBorderLength(); i++) {

            IBorderPoint p = this.getBorderPoint(i);
            IBorderPoint opp = this.findOppositeBorder(p);
            double distance = p.getLengthTo(opp);

            if (distance < minDistance) {
                minDistance = distance;
                reference = p;
            }
        }
        // this.minFeretPoint1 = reference;
        // this.minFeretPoint2 = this.findOppositeBorder(reference);

        // Using the point, draw a line from teh CoM to the border. Measure the
        // angle to an intersection point
        // if close to 90, and the distance to the tip > CoM-tip, keep the point
        // return the best point
        double difference = 90;
        IBorderPoint tail = new DefaultBorderPoint(0, 0);
        for (int i = 0; i < this.getBorderLength(); i++) {

            IBorderPoint p = this.getBorderPoint(i);
            double angle = this.getCentreOfMass().findSmallestAngle(reference, p);

            if (Math.abs(90 - angle) < difference && p.getLengthTo(this.getBorderPoint(Tag.REFERENCE_POINT)) > this
                    .getCentreOfMass().getLengthTo(this.getBorderPoint(Tag.REFERENCE_POINT))) {
                difference = 90 - angle;
                tail = p;
            }

        }
        return tail;
    }

    /*
     * ----------------------- Methods for dividing the nucleus to hook and hump
     * sides -----------------------
     */

    /*
     * In order to split the nuclear roi into hook and hump sides, we need to
     * get an intersection point of the line through the tail and centre of mass
     * with the opposite border of the nucleus.
     */
    private int findIntersectionPointForNuclearSplit() throws UnavailableBorderTagException {
        // test if each point from the tail intersects the splitting line
        // determine the coordinates of the point intersected as int
        // for each xvalue of each point in array, get the line y value
        // at the point the yvalues are closest and not the tail point is the
        // intersesction
        LineEquation lineEquation = new DoubleEquation(this.getCentreOfMass(),
                this.getBorderPoint(Tag.ORIENTATION_POINT));

        double minDeltaY = 100;
        int minDeltaYIndex = 0;

        for (int i = 0; i < this.getBorderLength(); i++) {
            double x = this.getBorderPoint(i).getX();
            double y = this.getBorderPoint(i).getY();
            double yOnLine = lineEquation.getY(x);

            double distanceToTail = this.getBorderPoint(i).getLengthTo(this.getBorderPoint(Tag.ORIENTATION_POINT));

            double deltaY = Math.abs(y - yOnLine);
            if (deltaY < minDeltaY && distanceToTail > this.getStatistic(PlottableStatistic.MAX_FERET) / 2) { // exclude
                                                                                                              // points
                                                                                                              // too
                                                                                                              // close
                                                                                                              // to
                                                                                                              // the
                                                                                                              // tail
                minDeltaY = deltaY;
                minDeltaYIndex = i;
            }
        }
        return minDeltaYIndex;
    }

    public void splitNucleusToHeadAndHump() {

        if (!this.hasBorderTag(Tag.INTERSECTION_POINT)) {
            int index;
            try {
                index = findIntersectionPointForNuclearSplit();
            } catch (UnavailableBorderTagException e) {
                stack("Cannot get border tag", e);
                return;
            }
            this.setBorderTag(Tag.INTERSECTION_POINT, index);
        }
    }

    /*
     * ----------------------- Methods for measuring signal positions
     * -----------------------
     */

    // needs to override AsymmetricNucleus version because hook/hump
    @Override
    public void calculateSignalAnglesFromPoint(IBorderPoint p) {

        super.calculateSignalAnglesFromPoint(p);

        if (this.getSignalCollection().hasSignal()) {

            // IJ.log(this.dumpInfo(BORDER_TAGS));

            // update signal angles with hook or hump side
            for (UUID i : signalCollection.getSignalGroupIds()) {

                if (signalCollection.hasSignal(i)) {

                    List<INuclearSignal> signals = signalCollection.getSignals(i);

                    for (INuclearSignal n : signals) {

                        /*
                         * Angle begins from the orientation point
                         */

                        double angle = n.getStatistic(PlottableStatistic.ANGLE);

                        try {
                            // This com is offset, not original
                            IPoint com = n.getCentreOfMass();

                            // These rois are offset, not original
                            if (this.isHookSide(com)) {
                                angle = 360 - angle;

                            }

                        } catch (Exception e) {
                            // IJ.log(this.getNameAndNumber()+": Error detected:
                            // falling back on default angle: "+e.getMessage());
                        } finally {

                            n.setStatistic(PlottableStatistic.ANGLE, angle);

                        }
                    }
                }
            }
        }
    }

    @Override
    public void rotate(double angle) {
        if (angle != 0)
            super.rotate(angle);
    }

    @Override
    public String dumpInfo(int type) {
        String result = super.dumpInfo(type);
        return result;
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if(!this.hasBorderTag(Tag.REFERENCE_POINT))
        	warn("Nucleus "+this.getNameAndNumber()+" has no RP");
        calculateHookAndBodyLength();

    }

    private synchronized void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }
}
