package components.active;

import java.awt.Rectangle;
import java.awt.Shape;

import analysis.profiles.ProfileCreator;
import components.generic.ISegmentedProfile;
import components.generic.ProfileType;
import components.nuclear.NucleusType;
import components.nuclei.Nucleus;
import ij.process.FloatPolygon;

public class DefaultConsensusNucleus extends DefaultNucleus {
	
	private static final long serialVersionUID = 1L;
	
	private NucleusType type;
	
	public DefaultConsensusNucleus(Nucleus n, NucleusType type) {
		
		super(n);
		this.type = type;
	}
	
	public NucleusType getType(){
		return this.type;
	}
	
	@Override
	public int[] getPosition(){
		Rectangle bounds = getVerticallyRotatedNucleus().createPolygon().getBounds();
		int newWidth  = (int) bounds.getWidth();
		int newHeight = (int) bounds.getHeight();
		int newX      = (int) bounds.getX();
		int newY      = (int) bounds.getY();

		int[] newPosition = { newX, newY, newWidth, newHeight };
		return  newPosition;
	}
	
	@Override
	public void calculateProfiles() {
		
		/*
		 * The CurveRefolder currently only uses the angle profile
		 * so ignore the others to speed refolding
		 */
		ProfileCreator creator = new ProfileCreator(this);

		ISegmentedProfile profile = creator.createProfile(ProfileType.ANGLE);
				
		profileMap.put(ProfileType.ANGLE, profile);

	}
	
	@Override
	public FloatPolygon createOriginalPolygon(){
		
		// There is no original position for a consensus
		return this.createPolygon();
	}
	
	@Override
	public Shape toOriginalShape(){
		
		// There is no original position for a consensus
		return this.toShape();
	}

}
