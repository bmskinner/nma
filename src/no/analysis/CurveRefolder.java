 /*
	-----------------------
	CURVE REFOLDER CLASS
	-----------------------
	Contains the code for taking a profile, and an ideal profile, and
		making the profile fit
 */
package no.analysis;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import utility.Constants;
import utility.Logger;
import utility.Utils;
import no.nuclei.ConsensusNucleus;
import no.nuclei.Nucleus;
import no.nuclei.RoundNucleus;
import no.nuclei.sperm.RodentSpermNucleus;
import no.collections.CellCollection;
import no.components.*;


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
		Nucleus n = (Nucleus)collection.getNucleusMostSimilarToMedian(collection.getOrientationPoint());	
		
		logger.log("Creating consensus nucleus template", Logger.DEBUG);
		ConsensusNucleus refoldCandidate = new ConsensusNucleus(n, collection.getNucleusClass());

		logger.log("Refolding nucleus of class: "+collection.getNucleusClass().getSimpleName());
		logger.log("Subject: "+refoldCandidate.getImageName()+"-"+refoldCandidate.getNucleusNumber(), Logger.DEBUG);

		Profile targetProfile 	= collection.getProfileCollection().getProfile("tail");
		Profile q25 			= collection.getProfileCollection().getProfile("tail25");
		Profile q75 			= collection.getProfileCollection().getProfile("tail75");

		if(targetProfile==null){
			throw new Exception("Null reference to target profile");
		}
		if(q25==null || q75==null){
			throw new Exception("Null reference to q25 or q75 profile");
		}


		this.targetCurve 	= targetProfile;
		this.refoldNucleus 	= refoldCandidate;
		this.collection 	= collection;
		this.setMode(refoldMode);
	}
	
	@Override
	protected Boolean doInBackground() {
		
		logger = new Logger(collection.getDebugFile(), "CurveRefolder");
		try{ 

			this.refoldCurve();
			
			firePropertyChange("Cooldown", getProgress(), Constants.Progress.COOLDOWN.code());

			// orient refolded nucleus to put tail at the bottom
			refoldNucleus.rotatePointToBottom(refoldNucleus.getBorderTag(collection.getOrientationPoint()));

			// if rodent sperm, put tip on left if needed
			if(collection.getNucleusClass().equals(RodentSpermNucleus.class)){
				if(refoldNucleus.getBorderTag(collection.getReferencePoint()).getX()>0){
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
			double score = refoldNucleus.getAngleProfile("tail").absoluteSquareDifference(targetCurve);
			
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
			throw new Exception("Cannot calculate scores: "+e);
		}
	}

	public void setMode(String s){
		this.mode = CurveRefolder.MODES.get(s);
	}


	/*
		Go over the target nucleus, adjusting each point.
		Keep the change if it helps get closer to the target profile

		Changes to make:
			Random mutation to the X and Y position. Must remain
			within a certain range of neighbours
	*/
	private double iterateOverNucleus() throws Exception{

		SegmentedProfile refoldProfile = refoldNucleus.getAngleProfile("tail");

		double similarityScore = refoldProfile.absoluteSquareDifference(targetCurve);

		double medianDistanceBetweenPoints = refoldNucleus.getMedianDistanceBetweenPoints();
		double minDistance = medianDistanceBetweenPoints * 0.5;
		double maxDistance = medianDistanceBetweenPoints * 1.2;

		// make all changes to a fresh nucleus before buggering up the real one
//		testNucleus = new Nucleus( (Nucleus)refoldNucleus);

		
		for(int i=0; i<refoldNucleus.getLength(); i++){

			RoundNucleus testNucleus = new RoundNucleus( (RoundNucleus)refoldNucleus);

			double score = testNucleus.getAngleProfile("tail").absoluteSquareDifference(targetCurve);
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

			score = testNucleus.getAngleProfile("tail").absoluteSquareDifference(targetCurve);
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