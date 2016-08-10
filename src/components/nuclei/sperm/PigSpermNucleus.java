/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
  /*
  -----------------------
  PIG SPERM NUCLEUS CLASS
  -----------------------
*/  
package components.nuclei.sperm;

import ij.gui.Roi;

import java.io.File;
import java.io.IOException;

import analysis.profiles.ProfileIndexFinder;
import analysis.profiles.RuleSet;
import components.generic.BooleanProfile;
import components.generic.BorderTag;
import components.generic.BorderTagObject;
import components.generic.Profile;
import components.generic.ProfileType;
import components.nuclear.BorderPoint;
import components.nuclear.NucleusType;
import components.nuclear.SignalCollection;
import components.nuclei.Nucleus;
import components.nuclei.RoundNucleus;

public class PigSpermNucleus 
    extends SpermNucleus 
  {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
    * The point of the nucleus orthogonal to the narrowest
    * diameter, through the centre of mass.
    * Used in tail identification functions only.
    */
//    private BorderPoint orthPoint1;

  /**
  * Constructor using a Nucleus; passes up
  * to the SpermNucleus constructor
  *
  * @param n the Nucleus to construct from
 * @throws Exception 
  */
    public PigSpermNucleus(RoundNucleus n) throws Exception{
      super(n);
    }

    public PigSpermNucleus() {
    	super();
    }

    public PigSpermNucleus (Roi roi, File file, int number, double[] position) { // construct from an roi
		super(roi, file, number, position);
	}
    
    public Nucleus duplicate(){
		try {
			PigSpermNucleus duplicate = new PigSpermNucleus(this);
			return duplicate;
		} catch (Exception e) {
			return null;
		}
	}
    

    
  /**
  * {@inheritDoc}
  * <p>
  * This method overrides the Nucleus method, and uses a single measure
  * to find the pig sperm tail. This is using the maximum angle in the 
  * Profile.
 * @throws Exception 
  */
    @Override
    public void findPointsAroundBorder() throws Exception{
    	
    	RuleSet rpSet = RuleSet.pigSpermRPRuleSet();
		Profile p     = this.getProfile(rpSet.getType());
		ProfileIndexFinder f = new ProfileIndexFinder();
		int rpIndex = f.identifyIndex(p, rpSet);
		
		if( rpIndex== -1 ){
			finest("RP index was not found in nucleus, setting to zero in profile");
			rpIndex = 0;
		}
		
    	setBorderTag(BorderTagObject.REFERENCE_POINT, rpIndex);
    	
    	/*
    	 * The OP is the same as the RP in pigs
    	 */
    	setBorderTag(BorderTagObject.ORIENTATION_POINT, rpIndex);
    	
    	
    	
    	/*
    	 * The IP is opposite the OP
    	 */
    	BorderPoint op = this.getBorderPoint(rpIndex);
    	int ipIndex = getBorderIndex(this.findOppositeBorder(op));
    	setBorderTag(BorderTagObject.INTERSECTION_POINT, ipIndex);
    	
    	if(!this.isProfileOrientationOK()){
			this.reverse();
		}
    	      
    }
    
    /*
      -----------------------
      Methods for detecting the head and tail
      -----------------------
    */

    /**
    * This method attempts to find the pig sperm tail point
    * using the lowest angles in the Profile. The two lowest
    * minima should be either side of the tail. Not currently
    * used.
     * @throws Exception 
    * @see Profile
    */
//    private BorderPoint findTailByMinima() throws Exception{
//
//      // the two lowest minima are at the tail-end corners. 
//      // between them lies the tail. Find the two lowest minima,
//      // and get the point between them
//
//      
//      Profile angleProfile = this.getProfile(ProfileType.REGULAR);
//      
//      BooleanProfile minima = angleProfile.getLocalMinima(5);
//
//      // sort minima by interior angle
//      int lowestMinima = 0;
//      int secondLowestMinima = 0;
//
//      for(int i=0; i<minima.size();i++){
//        if(minima.get(i)==true){
//          if (angleProfile.get(i)<angleProfile.get(lowestMinima)){
//            secondLowestMinima = lowestMinima;
//            lowestMinima = i;
//          }
//        }
//      }
//      for(int i=0; i<minima.size();i++){
//        if(minima.get(i)==true){
//          if (angleProfile.get(i)<angleProfile.get(secondLowestMinima) && 
//        		  angleProfile.get(i)>angleProfile.get(lowestMinima)){
//            secondLowestMinima = i;
//          }
//        }
//      }
//
//      BorderPoint a = this.getBorderPoint(lowestMinima);
//      BorderPoint b = this.getBorderPoint(secondLowestMinima);
//
//      BorderPoint tailPoint = this.getBorderPoint(this.getPositionBetween(a, b));
//      return tailPoint;
//    }

    /**
    * This method attempts to find the pig sperm tail point
    * using the highest angle in the Profile. The highest point 
    * should be at the tail, since the nucleus has an inward 'dent'
     * @throws Exception 
    * @see Profile
    */
//    private int findTailByMaxima() throws Exception{
//      // the tail is the ?only local maximum with an interior angle above the 180 line
//
//    	return this.getProfile(ProfileType.REGULAR).getIndexOfMax();
//    }


    /**
    * This method attempts to find the pig sperm tail point
    * using the narrowest point across the centre of mass.
    * The narrowest diameter through the CoM is orthogonal to 
    * the tail. Of the two orthogonal border points, the point with
    * the highest angle is the tail.
     * @throws Exception 
    * @see Profile
    */
//    private BorderPoint findTailByNarrowestPoint() throws Exception{
//
//      BorderPoint narrowPoint = this.getNarrowestDiameterPoint();
//      this.orthPoint1  = this.findOrthogonalBorderPoint(narrowPoint);
//      BorderPoint orthPoint2  = this.findOppositeBorder(orthPoint1);
//
//      // NucleusBorderPoint[] array = { orthPoint1, orthPoint2 };
//      Profile angleProfile = this.getProfile(ProfileType.REGULAR);
//      // the tail should be a maximum, hence have a high angle
//      BorderPoint tailPoint  = angleProfile.get(this.getBorderIndex(orthPoint1)) >
//      									angleProfile.get(this.getBorderIndex(orthPoint2))
//                                    ? orthPoint1
//                                    : orthPoint2;
//      return tailPoint;
//    }
    
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
  	  finest("\tReading pig sperm nucleus");
  	  in.defaultReadObject();
  	  finest("\tRead pig sperm nucleus");
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
  	  finest("\tWriting pig sperm nucleus");
  	  out.defaultWriteObject();
  	  finest("\tWrote pig sperm nucleus");
    }
    

}