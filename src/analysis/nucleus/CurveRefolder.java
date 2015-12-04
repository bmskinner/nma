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

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import analysis.AnalysisDataset;
import analysis.AnalysisWorker;
import utility.Constants;
import utility.Utils;
import components.CellCollection;
import components.generic.BorderTag;
import components.generic.Equation;
import components.generic.Profile;
import components.generic.ProfileCollectionType;
import components.generic.SegmentedProfile;
import components.generic.XYPoint;
import components.nuclear.NucleusBorderPoint;
import components.nuclear.NucleusType;
import components.nuclei.ConsensusNucleus;
import components.nuclei.Nucleus;
import components.nuclei.RoundNucleus;


public class CurveRefolder extends AnalysisWorker {

	private Profile targetCurve;

	private ConsensusNucleus refoldNucleus;
	
	private CellCollection collection;
//	private CountDownLatch doneSignal;
	
	private CurveRefoldingMode mode = CurveRefoldingMode.FAST; 				 // the dafault mode
	
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
	public CurveRefolder(AnalysisDataset dataset, CurveRefoldingMode refoldMode, Logger logger) throws Exception {
		super(dataset, logger);
//		this.doneSignal = doneSignal;
		this.setProgressTotal(refoldMode.maxIterations());

		collection = dataset.getCollection();
		
		log(Level.FINE, "Creating refolder");


		// make an entirely new nucleus to play with
		log(Level.FINEST, "Fetching best refold candiate");

		Nucleus n = (Nucleus)collection.getNucleusMostSimilarToMedian(BorderTag.ORIENTATION_POINT);	
		
		log(Level.FINEST, "Creating consensus nucleus template");
		refoldNucleus = new ConsensusNucleus(n, collection.getNucleusType());

		log(Level.FINEST, "Refolding nucleus of class: "+collection.getNucleusType().toString());
		log(Level.FINEST, "Subject: "+refoldNucleus.getImageName()+"-"+refoldNucleus.getNucleusNumber());

		Profile targetProfile 	= collection.getProfileCollection(ProfileCollectionType.REGULAR).getProfile(BorderTag.ORIENTATION_POINT, Constants.MEDIAN);
		Profile q25 			= collection.getProfileCollection(ProfileCollectionType.REGULAR).getProfile(BorderTag.ORIENTATION_POINT, Constants.LOWER_QUARTILE);
		Profile q75 			= collection.getProfileCollection(ProfileCollectionType.REGULAR).getProfile(BorderTag.ORIENTATION_POINT, Constants.UPPER_QUARTILE);

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
			// smooth the candidate nucleus to remove jagged edges
			this.smoothCurve(0); // smooth with no offset
			this.smoothCurve(1); // smooth with intercalated offset
			
			// carry out the refolding
			this.refoldCurve();

			// smooth the refolded nucleus to remove jagged edges
			this.smoothCurve(0); // smooth with no offset
			this.smoothCurve(1); // smooth with offset 1 to intercalate
						
			firePropertyChange("Cooldown", getProgress(), Constants.Progress.COOLDOWN.code());

			// orient refolded nucleus to put tail at the bottom
			refoldNucleus.rotatePointToBottom(refoldNucleus.getBorderTag(BorderTag.ORIENTATION_POINT));

			// if rodent sperm, put tip on left if needed
			if(collection.getNucleusType().equals(NucleusType.RODENT_SPERM)){
				if(refoldNucleus.getBorderTag(BorderTag.REFERENCE_POINT).getX()>0){
					refoldNucleus.flipXAroundPoint(refoldNucleus.getCentreOfMass());
				}
			}

			collection.addConsensusNucleus(refoldNucleus);

		} catch(Exception e){
			logError("Unable to refold nucleus", e);
			return false;
		}
		return true;
	}
	
	/*
		The main function to be called externally;
		all other functions will hang off this
	*/
	public void refoldCurve() throws Exception {

		try{
			refoldNucleus.moveCentreOfMass(new XYPoint(0, 0));
		} catch(Exception e){
			throw new Exception("Unable to move centre of mass");
		}

		try{
			double score = refoldNucleus.getAngleProfile(BorderTag.ORIENTATION_POINT).absoluteSquareDifference(targetCurve);
			
//			fileLogger.log(Level.INFO, "Refolding curve: initial score: "+(int)score);
			log(Level.FINE, "Refolding curve: initial score: "+(int)score);

//			double originalScore = score;
			double prevScore = score*2;
			int i=0;
			
//			(prevScore - score)/prevScore > 0.001 ||
			
//			if(this.mode.equals(CurveRefoldingMode.FAST)){
				while(  i<mode.maxIterations()){ // iterate until converging
					prevScore = score;
					score = this.iterateOverNucleus();
					i++;
					publish(i);
					log(Level.FINE, "Iteration "+i+": "+(int)score);
//					programLogger.log(Level.FINE, "Iteration "+i+": "+(int)score);
				}
//			}

//			if(this.mode==INTENSIVE_MODE){
//
//				while(score > (originalScore*0.1) && i<MAX_ITERATIONS_INTENSIVE){ // iterate until 0.6 original score, or 1000 iterations
//					prevScore = score;
//					score = this.iterateOverNucleus();
//					i++;
//					publish(i);
//					if(i%50==0){
//						fileLogger.log(Level.FINE, "Iteration "+i+": "+(int)score);
//						logger.log(Level.FINE, "Iteration "+i+": "+(int)score);
////						logger.log("Iteration "+i+": "+(int)score, Logger.DEBUG);
//					}
//				}
//			}
//
//			if(this.mode==BRUTAL_MODE){
//
//				while(score > (originalScore*0.1) && i<MAX_ITERATIONS_BRUTAL){ // iterate until 0.1 original score, or 10000 iterations
//					prevScore = score;
//					score = this.iterateOverNucleus();
//					i++;
//					publish(i);
//					if(i%50==0){
//						fileLogger.log(Level.FINE, "Iteration "+i+": "+(int)score);
//						logger.log(Level.FINE, "Iteration "+i+": "+(int)score);
//					}
//				}
//			}
//			fileLogger.log(Level.INFO, "Refolded curve: final score: "+(int)score);
			log(Level.FINE, "Refolded curve: final score: "+(int)score);

		} catch(Exception e){
			throw new Exception("Cannot calculate scores: "+e.getMessage());
		}
	}

	public void setMode(CurveRefoldingMode s){
		this.mode = s;
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
		boolean skip = false;

//		int i=offset;
		for (int i = offset; i<refoldNucleus.getLength(); i++){
//		for(NucleusBorderPoint p : refoldNucleus.getBorderList() ){
			
			if(skip){
//				i++;
				continue; // ensure we only carry out the smoothing every other point
			}
			skip = !skip;
			
			int prevIndex = Utils.wrapIndex(i-1, refoldNucleus.getLength());
			int nextIndex = Utils.wrapIndex(i+1, refoldNucleus.getLength());
						
			NucleusBorderPoint thisPoint = refoldNucleus.getBorderPoint(i);
			NucleusBorderPoint prevPoint = refoldNucleus.getBorderPoint(prevIndex);
			NucleusBorderPoint nextPoint = refoldNucleus.getBorderPoint(nextIndex);

			/* get the point o,  half way between the previous point p and next point n:
			 * 
			 *     p  o  n
			 *      \   /
			 *        x
			 * 
			 */

			Equation eq = new Equation(prevPoint, nextPoint);
			double distance = prevPoint.getLengthTo(nextPoint) / 2;
			XYPoint newPoint = eq.getPointOnLine(prevPoint, distance);
						
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
			XYPoint replacementPoint = eq2.getPointOnLine(newPoint, distance2);
			
			boolean ok = checkPositionIsOK(newPoint, refoldNucleus, i, minDistance, maxDistance);

			if(ok){
				// update the new position
				refoldNucleus.updatePoint(i, replacementPoint.getX(), replacementPoint.getY());
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
	private double iterateOverNucleus() throws Exception {

		SegmentedProfile refoldProfile = refoldNucleus.getAngleProfile(BorderTag.ORIENTATION_POINT);

		// Get the difference between the candidate nucleus profile and the median profile
		double similarityScore = refoldProfile.absoluteSquareDifference(targetCurve);

		// Get the median distance between each border point in the refold candidate nucleus.
		// Use this to establish the max and min distances a point can migrate from its neighbours
		double medianDistanceBetweenPoints = refoldNucleus.getMedianDistanceBetweenPoints();
		double minDistance = medianDistanceBetweenPoints * 0.5;
		double maxDistance = medianDistanceBetweenPoints * 1.2;

		for(int i=0; i<refoldNucleus.getLength(); i++){
			
			// make all changes to a fresh nucleus before buggering up the real one
			RoundNucleus testNucleus = new RoundNucleus( (RoundNucleus)refoldNucleus);

			double score = testNucleus.getAngleProfile(BorderTag.ORIENTATION_POINT).absoluteSquareDifference(targetCurve);

			// Get a copy of the point at this index
			NucleusBorderPoint p = testNucleus.getPoint(i);

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
			XYPoint newPoint = new XYPoint(newX, newY);
			
			boolean ok = checkPositionIsOK(newPoint, testNucleus, i, minDistance, maxDistance);

			if(	ok ){
				
				// Update the test nucleus
				testNucleus.updatePoint(i, newPoint);

				// Measure the new profile & compare
				testNucleus.calculateAngleProfile(refoldNucleus.getAngleProfileWindowSize());

				// Get the new score
				score = testNucleus.getAngleProfile(BorderTag.ORIENTATION_POINT).absoluteSquareDifference(targetCurve);

				// Apply the change if better fit
				if(score < similarityScore) {
					refoldNucleus.updatePoint(i, newPoint);
					refoldNucleus.calculateAngleProfile(refoldNucleus.getAngleProfileWindowSize());
					similarityScore = score;
				}
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
	private boolean checkPositionIsOK(XYPoint point,  Nucleus n, int index, double min, double max){
		double distanceToPrev = point.getLengthTo( n.getPoint( Utils.wrapIndex(index-1, n.getLength()) ) );
		double distanceToNext = point.getLengthTo( n.getPoint( Utils.wrapIndex(index+1, n.getLength()) ) );

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

		for(int i=0;i<n.getLength();i++){
			XYPoint p = n.getBorderPoint(i);
			double distance = p.getLengthTo(n.getCentreOfMass());
			double pAngle = RoundNucleus.findAngleBetweenXYPoints( p, n.getCentreOfMass(), new XYPoint(0,-10));
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