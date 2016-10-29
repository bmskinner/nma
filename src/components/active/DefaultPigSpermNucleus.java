package components.active;

import java.io.File;

import analysis.profiles.ProfileIndexFinder;
import analysis.profiles.RuleSet;
import components.generic.IPoint;
import components.generic.IProfile;
import components.generic.Tag;
import components.nuclear.IBorderPoint;
import components.nuclei.Nucleus;
import ij.gui.Roi;

public class DefaultPigSpermNucleus extends AbstractAsymmetricNucleus {

	private static final long serialVersionUID = 1L;

	public DefaultPigSpermNucleus(Roi roi, File f, int channel, int[] position, int number) {
		super(roi, f, channel, position, number);

	}

	/**
	 * Construct with an ROI, a source image and channel, and the original position in the source image
	 * @param roi
	 * @param f
	 * @param channel
	 * @param position
	 * @param centreOfMass
	 */
	public DefaultPigSpermNucleus(Roi roi, File f, int channel, int[] position, int number, IPoint centreOfMass){
		super(roi, f, channel, position, number, centreOfMass );
	}

	protected DefaultPigSpermNucleus(Nucleus n) {
		super(n);
	}

	@Override
	public Nucleus duplicate(){			
		return new DefaultPigSpermNucleus(this);
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
    	
    	if(!this.isProfileOrientationOK()){
			this.reverse();
		}
    	      
    }
}
