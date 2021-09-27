/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.components.nuclei;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileIndexFinder;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileIndexFinder.NoDetectedIndexException;
import com.bmskinner.nuclear_morphology.components.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.generic.DefaultBorderPoint;
import com.bmskinner.nuclear_morphology.components.generic.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.profiles.BooleanProfile;
import com.bmskinner.nuclear_morphology.components.profiles.IProfile;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.profiles.Tag;
import com.bmskinner.nuclear_morphology.components.profiles.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.profiles.UnprofilableObjectException;
import com.bmskinner.nuclear_morphology.components.rules.RuleSet;
import com.bmskinner.nuclear_morphology.components.signals.INuclearSignal;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.gui.Roi;
import ij.process.FloatPolygon;

/**
 * The standard rodent sperm nucleus
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultRodentSpermNucleus extends AbstractAsymmetricNucleus {
	
	private static final Logger LOGGER = Logger.getLogger(DefaultRodentSpermNucleus.class.getName());

    private static final long serialVersionUID = 1L;

    /**
     * Construct with an ROI, a source image and channel, and the original
     * position in the source image. It sets the immutable original centre of
     * mass, and the mutable current centre of mass. It also assigns a random ID
     * to the component.
     * 
     * @param roi the roi of the object
     * @param centerOfMass the original centre of mass of the component
     * @param source the image file the component was found in
     * @param channel the RGB channel the component was found in
     * @param position the bounding position of the component in the original image
     * @param id the id of the component. Only use when deserialising!
     */
    public DefaultRodentSpermNucleus(@NonNull Roi roi, @NonNull IPoint centreOfMass, File source, int channel, int[] position, int number, @NonNull UUID id) {
        super(roi, centreOfMass, source, channel, position, number, id);
    }
    
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
            LOGGER.log(Loggable.STACK, "Cannot duplicate nucleus", e);
            LOGGER.warning("Error duplicating nucleus");
        }
        return null;
    }

    @Override
    protected double calculateStatistic(Measurement stat) {
        double result = super.calculateStatistic(stat);

        if (Measurement.HOOK_LENGTH.equals(stat))
            return getHookOrBodyLength(true);

        if (Measurement.BODY_WIDTH.equals(stat))
            return getHookOrBodyLength(false);

        return result;
    }

    @Override
    public void setBorderTag(Tag tag, int i) {
        super.setBorderTag(tag, i);

        // If the flat region moved, update the cached lengths
        if (this.hasBorderTag(Tag.TOP_VERTICAL) && this.hasBorderTag(Tag.BOTTOM_VERTICAL) && hasBorderTag(Tag.REFERENCE_POINT)) {

            if (tag.equals(Tag.TOP_VERTICAL) || tag.equals(Tag.BOTTOM_VERTICAL)) {

                // Invalidate previous data
                setStatistic(Measurement.HOOK_LENGTH, STAT_NOT_CALCULATED);
                setStatistic(Measurement.BODY_WIDTH, STAT_NOT_CALCULATED);

                calculateHookAndBodyLength();
            }
        }

    }

    private double getHookOrBodyLength(boolean useHook) {

        // check stat is present before calling a getStatistic
        if (hasStatistic(Measurement.HOOK_LENGTH) || hasStatistic(Measurement.BODY_WIDTH)) {

            if (getStatistic(Measurement.HOOK_LENGTH) == STAT_NOT_CALCULATED
                    || getStatistic(Measurement.BODY_WIDTH) == STAT_NOT_CALCULATED) {
                calculateHookAndBodyLength();
            }

        } else {
            calculateHookAndBodyLength();
        }

        double stat = useHook ? getStatistic(Measurement.HOOK_LENGTH)
                : getStatistic(Measurement.BODY_WIDTH);

        stat = stat == BORDER_POINT_NOT_PRESENT ? 0 : stat; // -2 is the error
                                                            // code when TV and
                                                            // BV are not
                                                            // present. Using -1
                                                            // will cause
                                                            // infinite loop.

        return stat;

    }
    
    /**
     * Given a test nucleus, determine the hook length and body width.
     * Used because invoking Nucleus::getVerticallyRotatedNucleus clears
     * these stats
     * @param n
     */
    private synchronized void calculateHookAndBodyLength(Nucleus n) {
    	 if (n == null) {
             setStatistic(Measurement.HOOK_LENGTH, ERROR_CALCULATING_STAT);
             setStatistic(Measurement.BODY_WIDTH, ERROR_CALCULATING_STAT);
             return;
         }

         if (!n.hasBorderTag(Tag.TOP_VERTICAL) || !n.hasBorderTag(Tag.BOTTOM_VERTICAL)) {
             setStatistic(Measurement.HOOK_LENGTH, BORDER_POINT_NOT_PRESENT);
             setStatistic(Measurement.BODY_WIDTH, BORDER_POINT_NOT_PRESENT);
             return;
         }

         /*
          * Get the X position of the top vertical
          */
         double vertX;
         try {
             vertX = n.getBorderPoint(Tag.TOP_VERTICAL).getX();
         } catch (UnavailableBorderTagException e) {
             LOGGER.log(Loggable.STACK, "Cannot get border tag", e);
             setStatistic(Measurement.HOOK_LENGTH, BORDER_POINT_NOT_PRESENT);
             setStatistic(Measurement.BODY_WIDTH, BORDER_POINT_NOT_PRESENT);
             return;
         }

         /* Find the x values in the bounding box of the vertical nucleus.  */
         FloatPolygon p = n.toPolygon();
         double maxBoundingX = p.getBounds().getMaxX();
         double minBoundingX = p.getBounds().getMinX();
         
         /*
          * To determine if the point is hook or hump, take the X position of the
          * tip. This must lie on the hook side of the vertX
          */

         double dHook = 0;
         double dBody = 0;

         if(n.isClockwiseRP()){
         	dBody = vertX - minBoundingX;
         	dHook = maxBoundingX - vertX;
         	
         } else{
         	dHook = vertX - minBoundingX;
         	dBody = maxBoundingX - vertX;
         }

         setStatistic(Measurement.HOOK_LENGTH, dHook);
         setStatistic(Measurement.BODY_WIDTH, dBody);
    }
    

    private void calculateHookAndBodyLength() {
        // Start with the vertically rotated nucleus
        Nucleus testNucleus = getVerticallyRotatedNucleus();
        calculateHookAndBodyLength(testNucleus);
    }


    @Override
    public void findPointsAroundBorder() throws ComponentCreationException {
        /* Identify key points: tip, estimated tail position  */
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
                LOGGER.log(Loggable.STACK, "Unable to detect RP in nucleus");
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
            LOGGER.log(Loggable.STACK, "Error gettting tail position", e);
        } catch (UnavailableProfileTypeException e1) {
            LOGGER.log(Loggable.STACK, "Cannot get profile type", e1);
        }
    }
    
    @Override
	protected Nucleus createVerticallyRotatedNucleus() {
    	Nucleus verticalNucleus = super.getVerticallyRotatedNucleus();
    	if (verticalNucleus == null) {
    		LOGGER.fine("Unknown error creating vertical nucleus");
    		return null;
    	}
    	try {
    		// Calculate clockwise RP by angle of RP/OP
    		IPoint rp = getBorderPoint(Tag.REFERENCE_POINT);
    		IPoint op = getBorderPoint(Tag.ORIENTATION_POINT);
    		double angle = getCentreOfMass().findAbsoluteAngle(rp, op);
    		clockwiseRP = angle<180;
    		orientationChecked = true;
    		
    	} catch (UnavailableBorderTagException e) {
    		LOGGER.log(Loggable.STACK, "Cannot get RP or OP from nucleus; returning default orientation", e);
    		orientationChecked = false;
    	}
    	
    	if(clockwiseRP) 
     	   verticalNucleus.flipHorizontal();

    	return verticalNucleus;
    }
    
    @Override
   public Nucleus getVerticallyRotatedNucleus() {
       // Ensure that the hook and body are recalculated
       Nucleus testNucleus = createVerticallyRotatedNucleus();
       calculateHookAndBodyLength(testNucleus);
       return testNucleus;
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
        	LOGGER.log(Loggable.STACK, "Cannot get border tag", e);
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

        double minDistance = this.getStatistic(Measurement.MAX_FERET);
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
    
    @Override // needs to override AsymmetricNucleus version because hook/hump calculation
    public void calculateSignalAnglesFromPoint(IBorderPoint p) {

        super.calculateSignalAnglesFromPoint(p);

        if (this.getSignalCollection().hasSignal()) {

            // update signal angles depending on nucleus orientation
            for (UUID i : signalCollection.getSignalGroupIds()) {

                if (signalCollection.hasSignal(i)) {

                    List<INuclearSignal> signals = signalCollection.getSignals(i);

                    for (INuclearSignal n : signals) {

                        /* Angle begins from the orientation point */
                        double angle = n.getStatistic(Measurement.ANGLE);
                        if(!isClockwiseRP())
                        	angle = 360-angle;    
                        n.setStatistic(Measurement.ANGLE, angle);
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

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        calculateHookAndBodyLength();
    }
}
