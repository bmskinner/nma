package components.active;

import java.io.File;

import analysis.profiles.ProfileIndexFinder;
import analysis.profiles.RuleSet;
import components.active.generic.UnprofilableObjectException;
import components.generic.IPoint;
import components.generic.IProfile;
import components.generic.ProfileType;
import components.generic.Tag;
import components.nuclear.IBorderPoint;
import components.nuclei.Nucleus;
import ij.gui.Roi;

/**
 * The standard pig sperm nucleus
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultPigSpermNucleus extends AbstractAsymmetricNucleus {

	private static final long serialVersionUID = 1L;

//	public DefaultPigSpermNucleus(Roi roi, File f, int channel, int[] position, int number) {
//		super(roi, f, channel, position, number);
//
//	}

	/**
	 * Construct with an ROI, a source image and channel, and the original position in the source image
	 * @param roi
	 * @param f
	 * @param channel
	 * @param position
	 * @param centreOfMass
	 */
	public DefaultPigSpermNucleus(Roi roi, IPoint centreOfMass, File f, int channel, int[] position, int number){
		super(roi, f, channel, position, number, centreOfMass );
	}

	protected DefaultPigSpermNucleus(Nucleus n) throws UnprofilableObjectException {
		super(n);
	}

	@Override
	public Nucleus duplicate(){			
		try {
			return new DefaultPigSpermNucleus(this);
		} catch (UnprofilableObjectException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
    public void findPointsAroundBorder() {
    	
    	RuleSet rpSet = RuleSet.pigSpermRPRuleSet();
		IProfile p     = this.getProfile(rpSet.getType());
		ProfileIndexFinder f = new ProfileIndexFinder();
		int rpIndex = f.identifyIndex(p, rpSet);
		
		if( rpIndex== -1 ){
			finest("RP index was not found in nucleus, setting to zero in profile");
			rpIndex = 0;
		}
		
    	setBorderTag(Tag.REFERENCE_POINT, rpIndex);
    	
    	/*
    	 * The OP is the same as the RP in pigs
    	 */
    	setBorderTag(Tag.ORIENTATION_POINT, rpIndex);
    	    	
    	/*
    	 * The IP is opposite the OP
    	 */
    	IBorderPoint op = this.getBorderPoint(rpIndex);
    	int ipIndex = getBorderIndex(this.findOppositeBorder(op));
    	setBorderTag(Tag.INTERSECTION_POINT, ipIndex);
    	
    	// decide if the profile is right or left handed; flip if needed
    	if(!this.isProfileOrientationOK() && canReverse){
    		this.reverse(); // reverses all profiles, border array and tagged points

    		// the number of border points can change when reversing
    		// due to float interpolation from different starting positions
    		// so do the whole thing again
    		initialise(this.getWindowProportion(ProfileType.ANGLE));
    		canReverse = false;
    		findPointsAroundBorder();
    	} 
    	      
    }
}
