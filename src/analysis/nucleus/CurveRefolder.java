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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import utility.Constants;
import utility.Equation;
import utility.Logger;
import utility.Utils;
import utility.Constants.BorderTag;
import components.CellCollection;
import components.CellCollection.NucleusType;
import components.CellCollection.ProfileCollectionType;
import components.generic.Profile;
import components.generic.SegmentedProfile;
import components.generic.XYPoint;
import components.nuclear.NucleusBorderPoint;
import components.nuclei.ConsensusNucleus;
import components.nuclei.Nucleus;
import components.nuclei.RoundNucleus;


public class CurveRefolder extends SwingWorker<Boolean, Integer>{

	private Profile targetCurve;

	private ConsensusNucleus refoldNucleus;
	
	private CellCollection collection;
	private CountDownLatch doneSignal;
	
	
	private static Logger logger;


	public static final int FAST_MODE 		= 0; // default; iterate until convergence
	public static final int INTENSIVE_MODE 	= 1; // iterate until value
	public static final int BRUTAL_MODE 	= 2; // iterate until value
	private int mode = FAST_MODE; 				 // the dafault mode
	
	public static final int MAX_ITERATIONS_FAST 		= 50;
	public static final int MAX_ITERATIONS_INTENSIVE 	= 1000;
	public static final int MAX_ITERATIONS_BRUTAL 		= 10000;

	public static Map<String, Integer> MODES = new LinkedHashMap<String, Integer>();

	static {
		MODES.put("Fast", FAST_MODE);
		MODES.put("Intensive", INTENSIVE_MODE);
		MODES.put("Brutal", BRUTAL_MODE);
	}
			
	/**
	 * construct from a collection of cells and the mode of refolding
	 * @param collection
	 * @param refoldMode
	 * @throws Exception
	 */
	public CurveRefolder(CellCollection collection, String refoldMode, CountDownLatch doneSignal) throws Exception {
		this.doneSignal = doneSignal;
		logger = new Logger(collection.getDebugFile(), "CurveRefolder");

		// make an entirely new nucleus to play with
		logger.log("Fetching best refold candiate", Logger.DEBUG);
		Nucleus n = (Nucleus)collection.getNucleusMostSimilarToMedian(BorderTag.ORIENTATION_POINT);	
		
		logger.log("Creating consensus nucleus template", Logger.DEBUG);
		refoldNucleus = new ConsensusNucleus(n, collection.getNucleusType());

		logger.log("Refolding nucleus of class: "+collection.getNucleusType().toString());
		logger.log("Subject: "+refoldNucleus.getImageName()+"-"+refoldNucleus.getNucleusNumber(), Logger.DEBUG);

		Profile targetProfile 	= collection.getProfileCollection(ProfileCollectionType.REGULAR).getProfile(BorderTag.ORIENTATION_POINT, 50);
		Profile q25 			= collection.getProfileCollection(ProfileCollectionType.REGULAR).getProfile(BorderTag.ORIENTATION_POINT, 25);
		Profile q75 			= collection.getProfileCollection(ProfileCollectionType.REGULAR).getProfile(BorderTag.ORIENTATION_POINT, 75);

		if(targetProfile==null){
			throw new Exception("Null reference to target profile");
		}
		if(q25==null || q75==null){
			throw new Exception("Null reference to q25 or q75 profile");
		}


		this.targetCurve 	= targetProfile;
		this.collection 	= collection;
		this.setMode(refoldMode);
	}
	
	@Override
	protected Boolean doInBackground() {
		
		logger = new Logger(collection.getDebugFile(), "CurveRefolder");
		try{ 

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
			logger.log("Curve refolding complete: trigger done()", Logger.DEBUG);
			doneSignal.countDown();

		} catch(Exception e){
			logger.error("Unable to refold nucleus", e);
			return false;
		} 
		return true;
	}
	
	@Override
	protected void process( List<Integer> integers ) {
		//update the number of entries added
		int lastCycle = integers.get(integers.size()-1);
		
		int maxCycles = this.mode == FAST_MODE
						? MAX_ITERATIONS_FAST
						: this.mode == FAST_MODE
							? MAX_ITERATIONS_INTENSIVE
							: MAX_ITERATIONS_BRUTAL;

		int percent = (int) ( (double) lastCycle / (double) maxCycles * 100);

		setProgress(percent); // the integer representation of the percent
	}
	
	@Override
	public void done() {
		try {
			if(this.get()){
				logger.log("Firing successful worker task", Logger.DEBUG );
				firePropertyChange("Finished", getProgress(), Constants.Progress.FINISHED.code());
			} else {
				logger.log("Firing error in worker task", Logger.DEBUG );
				firePropertyChange("Error", getProgress(), Constants.Progress.ERROR.code());
			}
		} catch (InterruptedException e) {
			logger.error("Unable to refold nucleus", e);
		} catch (ExecutionException e) {
			logger.error("Unable to refold nucleus", e);
		}
		
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
			
			logger.log("Refolding curve: initial score: "+(int)score, Logger.INFO);

			double originalScore = score;
			double prevScore = score*2;
			int i=0;
			
			if(this.mode==FAST_MODE){
				while( (prevScore - score)/prevScore > 0.001 || i<MAX_ITERATIONS_FAST){ // iterate until converging
					prevScore = score;
					score = this.iterateOverNucleus();
					i++;
					publish(i);
					logger.log("Iteration "+i+": "+(int)score, Logger.DEBUG);
				}
			}

			if(this.mode==INTENSIVE_MODE){

				while(score > (originalScore*0.1) && i<MAX_ITERATIONS_INTENSIVE){ // iterate until 0.6 original score, or 1000 iterations
					prevScore = score;
					score = this.iterateOverNucleus();
					i++;
					publish(i);
					if(i%50==0){
						logger.log("Iteration "+i+": "+(int)score, Logger.DEBUG);
					}
				}
			}

			if(this.mode==BRUTAL_MODE){

				while(score > (originalScore*0.1) && i<MAX_ITERATIONS_BRUTAL){ // iterate until 0.1 original score, or 10000 iterations
					prevScore = score;
					score = this.iterateOverNucleus();
					i++;
					publish(i);
					if(i%50==0){
						logger.log("Iteration "+i+": "+(int)score, Logger.DEBUG);
					}
				}
			}
						
			logger.log("Refolded curve: final score: "+(int)score, Logger.INFO);
		} catch(Exception e){
			throw new Exception("Cannot calculate scores: "+e.getMessage());
		}
	}

	public void setMode(String s){
		this.mode = CurveRefolder.MODES.get(s);
	}
	
	/**
	 * Smooth jagged edges in the refold nucleus 
	 * @throws Exception 
	 */
	private void smoothCurve(int offset) throws Exception{

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

			// update the new position
			refoldNucleus.updatePoint(i, replacementPoint.getX(), replacementPoint.getY());
//			i++;
		}
	}


	/*
		Go over the target nucleus, adjusting each point.
		Keep the change if it helps get closer to the target profile

		Changes to make:
			Random mutation to the X and Y position. Must remain
			within a certain range of neighbours
	*/
	private double iterateOverNucleus() throws Exception{

		SegmentedProfile refoldProfile = refoldNucleus.getAngleProfile(BorderTag.ORIENTATION_POINT);

		double similarityScore = refoldProfile.absoluteSquareDifference(targetCurve);

		double medianDistanceBetweenPoints = refoldNucleus.getMedianDistanceBetweenPoints();
		double minDistance = medianDistanceBetweenPoints * 0.5;
		double maxDistance = medianDistanceBetweenPoints * 1.2;

		// make all changes to a fresh nucleus before buggering up the real one
//		testNucleus = new Nucleus( (Nucleus)refoldNucleus);

		
		for(int i=0; i<refoldNucleus.getLength(); i++){

			RoundNucleus testNucleus = new RoundNucleus( (RoundNucleus)refoldNucleus);

			double score = testNucleus.getAngleProfile(BorderTag.ORIENTATION_POINT).absoluteSquareDifference(targetCurve);
			// IJ.log("    Internal score: "+(int)score);

			NucleusBorderPoint p = testNucleus.getPoint(i);


			double oldX = p.getX();
			double oldY = p.getY();

			double xDelta =  0.5 - Math.min( Math.random() * (similarityScore/1000), 1); // when score is 1000, change by up to 1. When score is 300, change byup to 0.33
			double yDelta =  0.5 - Math.min( Math.random() * (similarityScore/1000), 1); // when score is 1000, change by up to 1. When score is 300, change byup to 0.33


			double newX = oldX + xDelta;
			double newY = oldY + yDelta;

			try{
				testNucleus.updatePoint(i, newX, newY);
			} catch(Exception e){
				throw new Exception("Cannot update point "+i+" to "+newX+", "+newY+": "+e);
			}


			// measure the new profile & compare
			try{
				testNucleus.calculateAngleProfile(refoldNucleus.getAngleProfileWindowSize());
			} catch(Exception e){
				throw new Exception("Cannot calculate angle profile: "+e);
			}

			score = testNucleus.getAngleProfile(BorderTag.ORIENTATION_POINT).absoluteSquareDifference(targetCurve);
			// IJ.log("    Internal score: "+(int)score);

			// do not apply change  if the distance from teh surrounding points changes too much
			double distanceToPrev = p.getLengthTo( testNucleus.getPoint( Utils.wrapIndex(i-1, testNucleus.getLength()) ) );
			double distanceToNext = p.getLengthTo( testNucleus.getPoint( Utils.wrapIndex(i+1, testNucleus.getLength()) ) );

			// apply the change if better fit
			if(score < 	similarityScore && 
									distanceToNext < maxDistance && distanceToNext > minDistance &&
									distanceToPrev < maxDistance && distanceToPrev > minDistance) {
				refoldNucleus.updatePoint(i, newX, newY);
				refoldNucleus.calculateAngleProfile(refoldNucleus.getAngleProfileWindowSize());
				similarityScore = score;
			}
		}
		return similarityScore;
	}

	public static double getDistanceFromAngle(double angle, Nucleus n){

		// go through the nucleus outline
		// measure the angle to the tail and the distance to the CoM
		// if closest to target angle, return distance
//		double bestAngle = 180;
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
//				bestAngle = pAngle;
				bestDiff = Math.abs(angle-pAngle);
				bestDistance = distance;
			}
		}
		return bestDistance;
	}

}