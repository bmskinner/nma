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
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileIndexFinder;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileIndexFinder.NoDetectedIndexException;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalAnalyser;
import com.bmskinner.nuclear_morphology.components.UnavailableBorderPointException;
import com.bmskinner.nuclear_morphology.components.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.cells.SegmentedCellularComponent;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.profiles.IProfile;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.profiles.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.profiles.UnprofilableObjectException;
import com.bmskinner.nuclear_morphology.components.rules.PriorityAxis;
import com.bmskinner.nuclear_morphology.components.rules.RuleSet;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.components.signals.DefaultSignalCollection;
import com.bmskinner.nuclear_morphology.components.signals.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.signals.ISignalCollection;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.utility.AngleTools;

import ij.gui.Roi;

/**
 * The standard round nucleus, implementing {@link Nucleus}. All non-round
 * nuclei extend this.
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultNucleus extends SegmentedCellularComponent implements Nucleus {
	
	private static final Logger LOGGER = Logger.getLogger(DefaultNucleus.class.getName());

    private static final long serialVersionUID = 1L;

    /** The number of the nucleus in its image, for display */
    private int nucleusNumber;
    
    /** Store the landmarks to be used for orientation */
    private Landmark t = null;
    private Landmark b = null;
    private Landmark r = null;
    private Landmark l = null;
    private Landmark x = null;
    private Landmark y = null;
    private PriorityAxis priorityAxis = PriorityAxis.Y;
    

    /** FISH signals in the nucleus */
    protected ISignalCollection signalCollection = new DefaultSignalCollection();

    protected transient boolean canReverse = true;
    
    
    
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
    public DefaultNucleus(@NonNull Roi roi, @NonNull IPoint centreOfMass, File source, 
    		int channel, int[] position, int number, @Nullable UUID id, RuleSetCollection rsc) {
        super(roi, centreOfMass, source, channel, position, id);
        this.nucleusNumber = number;
        t = rsc.getTopLandmark().orElse(null);
        b = rsc.getBottomLandmark().orElse(null);
        l = rsc.getLeftLandmark().orElse(null);
        r = rsc.getRightLandmark().orElse(null);
        x = rsc.getSecondaryX().orElse(null);
        y = rsc.getSecondaryY().orElse(null);
        priorityAxis = rsc.getPriorityAxis().orElse(PriorityAxis.Y);        
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
    public DefaultNucleus(@NonNull Roi roi, @NonNull IPoint centreOfMass, @NonNull File f, 
    		int channel, int[] position, int number, RuleSetCollection rsc) {
        this(roi, centreOfMass, f, channel, position, number, null, rsc);
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
        signalCollection = n.getSignalCollection().duplicate();
        
        t = n.getTopLandmark();
        b = n.getBottomLandmark();
        l = n.getLeftLandmark();
        r = n.getRightLandmark();
        x = n.getSecondaryX();
        y = n.getSecondaryX();
        priorityAxis = n.getPriorityAxis();
    }

    @Override
    public Nucleus duplicate() {
        try {
            return new DefaultNucleus(this);
        } catch (UnprofilableObjectException e) {
            LOGGER.warning("Duplication failed");
            LOGGER.log(Loggable.STACK, "Error duplicating nucleus", e);
        }
        return null;
    }
    
    

    @Override
	public @Nullable Landmark getTopLandmark() {
		return t;
	}

	@Override
	public @Nullable Landmark getBottomLandmark() {
		return b;
	}

	@Override
	public @Nullable Landmark getLeftLandmark() {
		return l;
	}

	@Override
	public @Nullable Landmark getRightLandmark() {
		return r;
	}

	@Override
	public @Nullable Landmark getSecondaryX() {
		return x;
	}

	@Override
	public @Nullable Landmark getSecondaryY() {
		return y;
	}

	@Override
	public @Nullable PriorityAxis getPriorityAxis() {
		return priorityAxis;
	}

	/*
     * Finds the key points of interest around the border of the Nucleus. Can
     * use several different methods, and take a best-fit, or just use one. The
     * default in a round nucleus is to get the longest diameter and set this as
     * the head/tail axis.
     */
    @Override
    public void findLandmarks(@NonNull RuleSetCollection rsc) throws ComponentCreationException {
    	
    	
    	for(Landmark lm : rsc.getTags()) {
    		LOGGER.finer(()->"Locating "+lm);
    		
    		try {
    			for(RuleSet rule : rsc.getRuleSets(lm)) {
    				IProfile p = this.getProfile(rule.getType());
    				ProfileIndexFinder f = new ProfileIndexFinder();
    				int index = f.identifyIndex(p, rule);
    				setBorderTag(lm, index);
    			}
    		} catch (UnavailableProfileTypeException e) {
    			LOGGER.log(Loggable.STACK, "Error getting profile type", e);
    		} catch (NoDetectedIndexException e) {
    			LOGGER.fine("Unable to detect "+lm+" in nucleus");
    		}
    	}
    	
    	if (!this.isProfileOrientationOK() && canReverse) {
            reverse();
            profileMap.clear();
            initialise(windowProportion);
            canReverse = false;
            findLandmarks(rsc);
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
            LOGGER.log(Loggable.STACK, "Unable to get border point", e);
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
    protected void calculateStatistic(Measurement stat) {

        super.calculateStatistic(stat);

        // Note - variability will remain zero here
        // These stats are specific to nuclei
                
        if (Measurement.ELLIPTICITY.equals(stat))
            setStatistic(stat, calculateEllipticity());
        
        if (Measurement.ASPECT.equals(stat))
        	setStatistic(stat, calculateAspect());
        
        if (Measurement.ELONGATION.equals(stat))
        	setStatistic(stat, calculateElongation());
                
        if (Measurement.REGULARITY.equals(stat))
        	setStatistic(stat, calculateRegularity());

        if (Measurement.BOUNDING_HEIGHT.equals(stat))
        	setStatistic(stat, getVerticallyRotatedNucleus().getBounds().getHeight());

        if (Measurement.BOUNDING_WIDTH.equals(stat))
        	setStatistic(stat, getVerticallyRotatedNucleus().getBounds().getWidth());
    }
    
    /**
     * Calculate the elongation of the object 
     * @return
     */
    private double calculateElongation() {
    	double h = getVerticallyRotatedNucleus().getBounds().getHeight();
        double w = getVerticallyRotatedNucleus().getBounds().getWidth();
        return (h-w)/(h+w);
    }


    /**
     * Calculate the regularity of the object 
     * @return
     */
    private double calculateRegularity() {
    	double h = getVerticallyRotatedNucleus().getBounds().getHeight();
        double w = getVerticallyRotatedNucleus().getBounds().getWidth();
        double a = this.getStatistic(Measurement.AREA);
        return (Math.PI*h*w)/(4*a);
    }
    
    /**
     * Calculate the aspect of the object 
     * @return
     */
    private double calculateAspect() {
    	 return 1d/calculateEllipticity();
    }

    /**
     * Calculate the ellipticity of the object 
     * @return
     */
    private double calculateEllipticity() {
        double h = getVerticallyRotatedNucleus().getBounds().getHeight();
        double w = getVerticallyRotatedNucleus().getBounds().getWidth();

        return h / w;
    }

    protected void setSignals(ISignalCollection collection) {
        signalCollection = collection;
    }

    @Override
	public ISignalCollection getSignalCollection() {
        return signalCollection;
    }

    public void updateSignalAngle(UUID channel, int signal, double angle) {
        signalCollection.getSignals(channel).get(signal).setStatistic(Measurement.ANGLE, angle);
    }

    // do not move this into SignalCollection - it is overridden in
    // RodentSpermNucleus
    @Override
	public void calculateSignalAnglesFromPoint(@NonNull IPoint p) {
    	
        for (UUID signalGroup : signalCollection.getSignalGroupIds()) {

            if (signalCollection.hasSignal(signalGroup)) {
                for (INuclearSignal s : signalCollection.getSignals(signalGroup)) {

                    double angle = this.getCentreOfMass().findAbsoluteAngle(p, s.getCentreOfMass());
                    s.setStatistic(Measurement.ANGLE, angle);

                }
            }
        }
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
            profile = this.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT);
        } catch (ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException e) {
        	LOGGER.log(Loggable.STACK, "Error getting profile", e);
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
    public Nucleus getVerticallyRotatedNucleus() {
        // Make an exact copy of the nucleus
        LOGGER.finest( "Creating vertical nucleus");
        Nucleus verticalNucleus = this.duplicate();

        // At this point the new nucleus was created at the original image
        // coordinates
        // of the template nucleus, then moved to the current CoM.
        // Now align the nucleus on vertical.

        verticalNucleus.alignVertically();

        double h = verticalNucleus.getBounds().getHeight();
        double w = verticalNucleus.getBounds().getWidth();

        setStatistic(Measurement.BOUNDING_HEIGHT, h);
        setStatistic(Measurement.BOUNDING_WIDTH, w);

        double aspect = h / w;
        setStatistic(Measurement.ELLIPTICITY, aspect);

        setStatistic(Measurement.BODY_WIDTH, STAT_NOT_CALCULATED);
        setStatistic(Measurement.HOOK_LENGTH, STAT_NOT_CALCULATED);

        return verticalNucleus;
    }

    @Override
    public void moveCentreOfMass(@NonNull IPoint point) {

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

    	try {
    		// Use the points defined in the RuleSetCollection
    		// to determine how to orient the nucleus
    		if(priorityAxis.equals(PriorityAxis.Y)) {
    			alignVerticallyPriorityY();

    		} else {
    			// Same logic but now if X axis is the priority
    			alignVerticallyPriorityX();
    		}
    	} catch (UnavailableBorderTagException e) {
    		LOGGER.log(Loggable.STACK, "Cannot get border tag or profile", e);
    		try {
    			rotatePointToBottom(getBorderPoint(Landmark.ORIENTATION_POINT));
    		} catch (UnavailableBorderTagException e1) {
    			LOGGER.log(Loggable.STACK, "Cannot get border tag", e1);
    		}
    	}
    }
    
    private void alignVerticallyPriorityY() throws UnavailableBorderTagException {
    	// Check if t and b are present
		if(hasBorderTag(t) && hasBorderTag(b)) {
			IPoint topPoint    = getBorderPoint(t);
			IPoint bottomPoint = getBorderPoint(b);
			if(topPoint != bottomPoint) {
    			alignPointsOnVertical(topPoint, bottomPoint);
			} else if(hasBorderTag(y)) {
				rotatePointToBottom(getBorderPoint(y));
			}
		} else if(hasBorderTag(y)) { // if no t and b, fall back to y
			rotatePointToBottom(getBorderPoint(y));
		}
		
		// Now check x, and flip as needed
		if(hasBorderTag(l) && hasBorderTag(r)) {
			IPoint leftPoint  = getBorderPoint(l);
			IPoint rightPoint = getBorderPoint(r);
			if(leftPoint.isRightOf(rightPoint)) {
				flipHorizontal();
			}
		} else if(hasBorderTag(l)) {
			IPoint leftPoint  = getBorderPoint(l);
			if(leftPoint.isRightOf(getCentreOfMass()))
				flipHorizontal();
		} else if(hasBorderTag(r)) {
			IPoint rightPoint = getBorderPoint(r);
			if(rightPoint.isLeftOf(getCentreOfMass()))
				flipHorizontal();
		} else if(hasBorderTag(x)) {
			IPoint leftPoint = getBorderPoint(x);
			if(leftPoint.isRightOf(getCentreOfMass()))
				flipHorizontal();
		}
    }
    
    
    private void alignVerticallyPriorityX() throws UnavailableBorderTagException {
    	// Check if l and r are present
		if(hasBorderTag(t) && hasBorderTag(b)) {
			IPoint leftPoint  = getBorderPoint(l);
			IPoint rightPoint = getBorderPoint(r);
			if(leftPoint != rightPoint) {
    			alignPointsOnHorizontal(leftPoint, rightPoint);
			} else {
				rotatePointToLeft(getBorderPoint(x));
			}
		} else if(hasBorderTag(y)) { // if no l and r, fall back to x
			rotatePointToLeft(getBorderPoint(x));
		}
		
		// Now check y, and flip as needed
		if(hasBorderTag(t) && hasBorderTag(b)) {
			IPoint topPoint  = getBorderPoint(t);
			IPoint bottomPoint = getBorderPoint(b);
			if(topPoint.isBelow(bottomPoint)) {
				flipVertical();
			}
		} else if(hasBorderTag(t)) {
			IPoint topPoint  = getBorderPoint(t);
			if(topPoint.isBelow(getCentreOfMass()))
				flipVertical();
		} else if(hasBorderTag(b)) {
			IPoint bottomPoint = getBorderPoint(b);
			if(bottomPoint.isAbove(getCentreOfMass()))
				flipVertical();
		} else if(hasBorderTag(y)) {
			IPoint bottomPoint = getBorderPoint(y);
			if(bottomPoint.isAbove(getCentreOfMass()))
				flipVertical();
		}
    }
    
    @Override
    public void flipHorizontal(@NonNull IPoint p) {
        super.flipHorizontal(p);

        for (UUID id : signalCollection.getSignalGroupIds()) {
            signalCollection.getSignals(id).stream().forEach(s -> s.flipHorizontal(p));
        }

    }
    
    @Override
    public void flipVertical(@NonNull IPoint p) {
        super.flipVertical(p);

        for (UUID id : signalCollection.getSignalGroupIds()) {
            signalCollection.getSignals(id).stream().forEach(s -> s.flipVertical(p));
        }

    }

    @Override
    public void rotate(double angle) {

        super.rotate(angle);

        if (angle != 0) {

            for (UUID id : signalCollection.getSignalGroupIds()) {

                signalCollection.getSignals(id).parallelStream().forEach(s -> {

                    s.rotate(angle);

                    // get the new signal centre of mass based on the nucleus rotation

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
    @Override
	public String toString() {
        String newLine = System.getProperty("line.separator");
        StringBuilder builder = new StringBuilder(super.toString()+newLine);

        builder.append(this.getNameAndNumber());
        builder.append(newLine);
        builder.append(this.getID().toString());
        builder.append(newLine);
        builder.append(this.getSignalCollection().toString());
        builder.append(newLine);
        return builder.toString();
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

        }
		return byName;

    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(b, l, nucleusNumber, priorityAxis, r, signalCollection, t, x, y);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		DefaultNucleus other = (DefaultNucleus) obj;
		return Objects.equals(b, other.b) && Objects.equals(l, other.l) && nucleusNumber == other.nucleusNumber
				&& priorityAxis == other.priorityAxis && Objects.equals(r, other.r)
				&& Objects.equals(signalCollection, other.signalCollection) && Objects.equals(t, other.t)
				&& Objects.equals(x, other.x) && Objects.equals(y, other.y);
	}
    
    

}
