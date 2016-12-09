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
	CURVE REFOLDER CLASS
	-----------------------
	Contains the code for taking a profile, and an ideal profile, and
		making the profile fit
 */
package analysis.nucleus;

import analysis.AnalysisWorker;
import analysis.IAnalysisDataset;
import analysis.profiles.ProfileException;
import stats.Quartile;
import utility.Constants;
import components.ICellCollection;
import components.generic.Equation;
import components.generic.FloatPoint;
import components.generic.IPoint;
import components.generic.IProfile;
import components.generic.ISegmentedProfile;
import components.generic.ProfileType;
import components.generic.Tag;
import components.generic.UnavailableBorderTagException;
import components.generic.UnavailableProfileTypeException;
import components.generic.UnprofilableObjectException;
import components.nuclear.IBorderPoint;
import components.nuclear.NucleusType;
import components.nuclei.DefaultConsensusNucleus;
import components.nuclei.Nucleus;


public class CurveRefolder extends AnalysisWorker {

	private IProfile targetCurve;

	private Nucleus refoldNucleus;
	
	private ICellCollection collection;
	
	private CurveRefoldingMode mode = CurveRefoldingMode.FAST; 				 // the dafault mode
	
	private int pointUpdateCounter = 0;
	
	public enum CurveRefoldingMode {
		
		FAST ("Fast", 50),
		INTENSIVE("Intensive", 1000),
		BRUTAL ("Brutal", 10000);
		
		private int iterations;
		private String name;
		
		CurveRefoldingMode(String name, int iterations){
			this.name = name;
			this.iterations = iterations;
		}
		
		public String toString(){
			return this.name;
		}
		
		public int maxIterations(){
			return this.iterations;
		}
	}
			
	/**
	 * construct from a collection of cells and the mode of refolding
	 * @param collection
	 * @param refoldMode
	 * @throws Exception
	 */
	public CurveRefolder(IAnalysisDataset dataset, CurveRefoldingMode refoldMode) throws Exception {
		super(dataset);

		dataset.getCollection().setRefolding(true);
		this.setProgressTotal(refoldMode.maxIterations());

		collection = dataset.getCollection();
		
		fine("Creating refolder");


		// make an entirely new nucleus to play with
		finest("Fetching best refold candiate");

		Nucleus n = collection.getNucleusMostSimilarToMedian(Tag.REFERENCE_POINT);	
		
		finest("Creating consensus nucleus template");
		refoldNucleus = new DefaultConsensusNucleus(n, collection.getNucleusType());

		finest("Refolding nucleus of class: "+collection.getNucleusType().toString());
		finest("Subject: "+refoldNucleus.getSourceFileName()+"-"+refoldNucleus.getNucleusNumber());

		IProfile targetProfile 	= collection.getProfileCollection().getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN);
		IProfile q25 			= collection.getProfileCollection().getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.LOWER_QUARTILE);
		IProfile q75 			= collection.getProfileCollection().getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.UPPER_QUARTILE);

		if(targetProfile==null){
			throw new Exception("Null reference to target profile");
		}
		if(q25==null || q75==null){
			throw new Exception("Null reference to q25 or q75 profile");
		}
		
		

		this.targetCurve 	= targetProfile;
		this.setMode(refoldMode);
	}
	
	@Override
	protected Boolean doInBackground() {
		

		try{ 

			refoldNucleus.moveCentreOfMass( IPoint.makeNew(0, 0));

			if(collection.size()>1){
				
				smoothCurve(); // smooth the candidate nucleus to remove jagged edges
				refoldCurve(); // carry out the refolding
				smoothCurve(); // smooth the refolded nucleus to remove jagged edges
			}
						
			firePropertyChange("Cooldown", getProgress(), Constants.Progress.COOLDOWN.code());

			// orient refolded nucleus to put tail at the bottom
			refoldNucleus.alignVertically();
								
			
			// if rodent sperm, put tip on left if needed
			if(collection.getNucleusType().equals(NucleusType.RODENT_SPERM)){
				if(refoldNucleus.getBorderTag(Tag.REFERENCE_POINT).getX()>0){
					refoldNucleus.flipXAroundPoint(refoldNucleus.getCentreOfMass());
				}
			}

//			refoldNucleus.updateVerticallyRotatedNucleus();
//			 Update the bounding box to reflect the rotated nucleus position
//			Rectangle bounds = refoldNucleus.getVerticallyRotatedNucleus().createPolygon().getBounds();
//			int newWidth  = (int) bounds.getWidth();
//			int newHeight = (int) bounds.getHeight();
//			int newX      = (int) bounds.getX();
//			int newY      = (int) bounds.getY();
//
//			int[] newPosition = { newX, newY, newWidth, newHeight };
//			refoldNucleus.setPosition(newPosition);
//			
			collection.setConsensusNucleus(refoldNucleus);
			
			fine("Updated "+pointUpdateCounter+" border points");

		} catch(Exception e){
			error("Unable to refold nucleus", e);
			return false;
		} finally {
			collection.setRefolding(false);
		}
		return true;
	}
	
	/*
		The main function to be called externally;
		all other functions will hang off this
	*/
	public void refoldCurve() throws Exception {
		
		try{
			double score = refoldNucleus.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT)
					.absoluteSquareDifference(targetCurve);
			

			fine("Refolding curve: initial score: "+(int)score);

//			double prevScore = score*2;
			int i=0;
			

				while(  i<mode.maxIterations()){ // iterate until converging
//					prevScore = score;
					score = this.iterateOverNucleus();
					publish(++i);
					fine("Iteration "+i+": "+(int)score);
				}

			fine("Refolded curve: final score: "+(int)score);

		} catch(Exception e){
			throw new Exception("Cannot calculate scores: "+e.getMessage());
		}
	}

	public void setMode(CurveRefoldingMode s){
		this.mode = s;
	}
	
	private void smoothCurve() throws Exception {
//		this.smoothCurve(0); // smooth with no offset
//		this.smoothCurve(1); // smooth with intercalated offset
	}
	
	/**
	 * Smooth jagged edges in the refold nucleus 
	 * @throws Exception 
	 */
	private void smoothCurve(int offset) throws Exception{

		
		// Get the median distance between each border point in the refold candidate nucleus.
		// Use this to establish the max and min distances a point can migrate from its neighbours
		double medianDistanceBetweenPoints = refoldNucleus.getMedianDistanceBetweenPoints();
		double minDistance = medianDistanceBetweenPoints * 0.5;
		double maxDistance = medianDistanceBetweenPoints * 1.2;
				
		/*
		 * Draw a line between the next and previous point
		 * Move the point to the centre of the line
		 * Move ahead two points
		 * 
		 */
//		boolean skip = false;

//		int i=offset;
		for (int i = offset; i<refoldNucleus.getBorderLength(); i+=2){
						
			IBorderPoint thisPoint = refoldNucleus.getBorderPoint(i);
			IBorderPoint prevPoint = thisPoint.prevPoint();
			IBorderPoint nextPoint = thisPoint.nextPoint();

			/* get the point o,  half way between the previous point p and next point n:
			 * 
			 *     p  o  n
			 *      \   /
			 *        x
			 * 
			 */

			Equation eq = new Equation(prevPoint, nextPoint);
			double distance = prevPoint.getLengthTo(nextPoint) / 2;
			IPoint newPoint = eq.getPointOnLine(prevPoint, distance);
						
			/* get the point r,  half way between o and this point x:
			 * 
			 *     p  o  n
			 *      \ r  /
			 *        x
			 * 
			 * This should smooth the curve without completely blunting corners
			 */
			Equation eq2 = new Equation(newPoint, thisPoint);
			double distance2 = newPoint.getLengthTo(thisPoint) / 2;
			IPoint replacementPoint = eq2.getPointOnLine(newPoint, distance2);
			
			boolean ok = checkPositionIsOK(newPoint, refoldNucleus, i, minDistance, maxDistance);

			if(ok){
				// update the new position
				refoldNucleus.updateBorderPoint(i, replacementPoint.getX(), replacementPoint.getY());
			}
		}
	}


	/*
		Go over the target nucleus, adjusting each point.
		Keep the change if it helps get closer to the target profile

		Changes to make:
			Random mutation to the X and Y position. Must remain
			within a certain range of neighbours
	*/
	private double iterateOverNucleus() throws ProfileException, UnavailableBorderTagException, UnavailableProfileTypeException {

		ISegmentedProfile refoldProfile = refoldNucleus.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);

		// Get the difference between the candidate nucleus profile and the median profile
		double similarityScore = refoldProfile.absoluteSquareDifference(targetCurve);

		// Get the median distance between each border point in the refold candidate nucleus.
		// Use this to establish the max and min distances a point can migrate from its neighbours
		// This is the 'habitable zone' a point can occupy
		double medianDistanceBetweenPoints = refoldNucleus.getMedianDistanceBetweenPoints();
		double minDistance = medianDistanceBetweenPoints * 0.5;
		double maxDistance = medianDistanceBetweenPoints * 1.2;

		
		// make all changes to a fresh nucleus before buggering up the real one
		finest("Creating test nucleus based on refold candidate");
				
		Nucleus testNucleus;
		try {
			
			testNucleus = new DefaultConsensusNucleus( refoldNucleus, NucleusType.ROUND);
			
			finest("Beginning border tests");
			for(int i=0; i<refoldNucleus.getBorderLength(); i++){
				similarityScore = improveBorderPoint(i, minDistance, maxDistance, similarityScore, testNucleus);
			}
			
		} catch(Error e){
			warn("Error making new consensus");
			fine("Error in construction", e);
		} catch (UnprofilableObjectException e) {
			warn("Cannot create the test nucleus");
			fine("Error in nucleus constructor", e);
		}
		
		
		testNucleus = null;
		return similarityScore;
	}
	
	
	/**
	 * Try a random modification to the given border point position, and measure the effect
	 * on the similarity score to the median profile
	 * @param index
	 * @param minDistance
	 * @param maxDistance
	 * @param similarityScore
	 * @return
	 * @throws ProfileException 
	 * @throws UnavailableBorderTagException 
	 * @throws UnavailableProfileTypeException 
	 */
	private double improveBorderPoint(int index, double minDistance, double maxDistance, double similarityScore, Nucleus testNucleus) throws ProfileException, UnavailableBorderTagException, UnavailableProfileTypeException{
//		// make all changes to a fresh nucleus before buggering up the real one
		finest("Testing point "+index);
		double score = testNucleus.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT).absoluteSquareDifference(targetCurve);

//		log("3bi testNucleus has "+ testNucleus.getProfile(ProfileType.ANGLE).getSegmentCount());
		
		// Get a copy of the point at this index
		IBorderPoint p = testNucleus.getBorderPoint(index);
		
//		log("Test point at index "+index+" at "+p.toString());

		// Save the old position
		double oldX = p.getX();
		double oldY = p.getY();

		// Make a random adjustment to the x and y positions. Move them more extensively when the score is high
		double xDelta =  0.5 - Math.min( Math.random() * (similarityScore/1000), 1); // when score is 1000, change by up to 1. When score is 300, change byup to 0.33
		double yDelta =  0.5 - Math.min( Math.random() * (similarityScore/1000), 1); // when score is 1000, change by up to 1. When score is 300, change byup to 0.33

		// Apply the calculated deltas to the x and y positions
		double newX = oldX + xDelta;
		double newY = oldY + yDelta;


		// Check the new point is valid
		IPoint newPoint = new FloatPoint(newX, newY);
		
		boolean ok = checkPositionIsOK(newPoint, testNucleus, index, minDistance, maxDistance);

		if(	ok ){

//			log("\tNew point accepted at index "+index+" at "+newPoint.toString());
			// Update the test nucleus
			testNucleus.updateBorderPoint(index, newPoint);

			finer("Testing profiles");
			try {
				testNucleus.calculateProfiles();
			} catch (ProfileException e) {
				warn("Cannot calculate profiles in test nucleus");
				fine("Error calculating profiles", e);
			}

			// Get the new score
			score = testNucleus.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT).absoluteSquareDifference(targetCurve);

			// Apply the change if better fit
			if(score < similarityScore) {
				refoldNucleus.updateBorderPoint(index, newPoint);
				pointUpdateCounter++;
				
				finer("Re-calculating profiles");
				try {
					refoldNucleus.calculateProfiles();
				} catch (ProfileException e) {
					warn("Cannot calculate profiles in consensus");
					fine("Error calculating profiles", e);
				}

				similarityScore = score;
			}
		}

		return similarityScore;
		
	}
	
	
	/**
	 * // Do not apply a change if the distance from the surrounding points changes too much
	 * @param point the new point to test
	 * @param n the nucleus
	 * @param index the point position in the nucleus
	 * @param min the min acceptable distance between points
	 * @param max the max acceptable distance between points
	 * @return
	 */
	private boolean checkPositionIsOK(IPoint point,  Nucleus n, int index, double min, double max){
		double distanceToPrev = point.getLengthTo( n.getBorderPoint( n.wrapIndex(index-1) ) );
		double distanceToNext = point.getLengthTo( n.getBorderPoint( n.wrapIndex(index+1) ) );

		boolean ok = true;
		if(	distanceToNext > max ){
			ok = false;
			
		}
		
		if(distanceToNext < min){
			ok = false;
		}
		
		if(distanceToPrev > max){
			ok = false;
		}
			
		if(distanceToPrev < min){
			ok = false; 
		}
		return ok;
	}

	
	/**
	 * Go through the nucleus outline. Measure the angle to the tail 
	 * and the distance to the CoM. If closest to target angle, return distance.
	 * @param angle the target angle
	 * @param n the nucleus to measure
	 * @return
	 */
	public static double getDistanceFromAngle(double angle, Nucleus n){

		double bestDiff = 180;
		double bestDistance = 180;

		for(int i=0;i<n.getBorderLength();i++){
			IPoint p = n.getBorderPoint(i);
			double distance = p.getLengthTo(n.getCentreOfMass());
			double pAngle = n.getCentreOfMass().findAngle( p, new FloatPoint(0,-10));
			if(p.getX()<0){
				pAngle = 360-pAngle;
			}

			if(Math.abs(angle-pAngle) < bestDiff){

				bestDiff = Math.abs(angle-pAngle);
				bestDistance = distance;
			}
		}
		return bestDistance;
	}

}