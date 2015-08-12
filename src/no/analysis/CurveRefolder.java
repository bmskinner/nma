 /*
	-----------------------
	CURVE REFOLDER CLASS
	-----------------------
	Contains the code for taking a profile, and an ideal profile, and
		making the profile fit
 */
package no.analysis;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import utility.Constants;
import utility.Logger;
import utility.Utils;
import no.nuclei.Nucleus;
import no.nuclei.RoundNucleus;
import no.nuclei.sperm.RodentSpermNucleus;
import no.collections.CellCollection;
import no.components.*;


public class CurveRefolder extends SwingWorker<Boolean, Integer>{

	private Profile targetCurve;

	private Nucleus refoldNucleus;

	private CellCollection collection;
	
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
	
	@Override
	protected Boolean doInBackground() {
		
		logger = new Logger(collection.getDebugFile(), "CurveRefolder");
		try{ 

			this.refoldCurve();
			
			firePropertyChange("Cooldown", getProgress(), Constants.Progress.COOLDOWN.code());

			// orient refolded nucleus to put tail at the bottom
			this.putPointAtBottom(refoldNucleus.getBorderTag("tail"));

			// if rodent sperm, put tip on left if needed
			if(refoldNucleus.getClass().equals(RodentSpermNucleus.class)){

				if(refoldNucleus.getBorderTag("tip").getX()>0){
					refoldNucleus.flipXAroundPoint(refoldNucleus.getCentreOfMass());
				}
			}

			collection.addConsensusNucleus(refoldNucleus);

		} catch(Exception e){
			logger.log("Unable to refold nucleus: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
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
				firePropertyChange("Finished", getProgress(), Constants.Progress.FINISHED.code());
			} else {
				firePropertyChange("Error", getProgress(), Constants.Progress.ERROR.code());
			}
		} catch (InterruptedException e) {
			logger.log("Unable to refold nucleus: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
		} catch (ExecutionException e) {
			logger.log("Unable to refold nucleus: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
		}
		
	} 
		
	public CurveRefolder(CellCollection collection, Class<?> nucleusClass, String refoldMode) throws Exception{
		
		
		logger = new Logger(collection.getDebugFile(), "CurveRefolder");

		// make an entirely new nucleus to play with
		Nucleus n = (Nucleus)collection.getNucleusMostSimilarToMedian("tail");
		Constructor<?> nucleusConstructor = nucleusClass.getConstructor(new Class[]{RoundNucleus.class});
		Nucleus refoldCandidate  = (Nucleus) nucleusConstructor.newInstance(n);

		if(refoldCandidate==null){
			throw new Exception("Null reference to nucleus refold candidate");
		}

		logger.log("Refolding nucleus of class: "+refoldCandidate.getClass().getSimpleName());
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

	/*
		The main function to be called externally;
		all other functions will hang off this
	*/
	public void refoldCurve() throws Exception {

		try{
			this.moveCoMtoZero();
		} catch(Exception e){
			throw new Exception("Unable to move centre of mass");
		}

		try{
			double score = refoldNucleus.getAngleProfile("tail").differenceToProfile(targetCurve);
			
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
		Translate the XY coordinates of each point so that
		the nuclear centre of mass is at 0,0.
		Affects refoldNucleus, which is only a copy of the median nucleus
	*/
	private void moveCoMtoZero(){

		XYPoint centreOfMass = refoldNucleus.getCentreOfMass();
		double xOffset = centreOfMass.getX();
		double yOffset = centreOfMass.getY();

		refoldNucleus.setCentreOfMass(new XYPoint(0,0));

		for(int i=0; i<refoldNucleus.getLength(); i++){
			XYPoint p = refoldNucleus.getBorderPoint(i);

			double x = p.getX() - xOffset;
			double y = p.getY() - yOffset;

			refoldNucleus.updatePoint(i, x, y );
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

		Profile refoldProfile = refoldNucleus.getAngleProfile("tail");
//		Profile interpolatedTargetCurve = targetCurve.interpolate(refoldProfile.size());

		double similarityScore = refoldProfile.differenceToProfile(targetCurve);

		double medianDistanceBetweenPoints = refoldNucleus.getMedianDistanceBetweenPoints();
		double minDistance = medianDistanceBetweenPoints * 0.5;
		double maxDistance = medianDistanceBetweenPoints * 1.2;

		// make all changes to a fresh nucleus before buggering up the real one
//		testNucleus = new Nucleus( (Nucleus)refoldNucleus);

		
		for(int i=0; i<refoldNucleus.getLength(); i++){

			RoundNucleus testNucleus = new RoundNucleus( (RoundNucleus)refoldNucleus);

			double score = testNucleus.getAngleProfile("tail").differenceToProfile(targetCurve);
			// IJ.log("    Internal score: "+(int)score);

			NucleusBorderPoint p = testNucleus.getPoint(i);
			
//			double currentDistance = p.getLengthTo(new XYPoint(0,0));
//			double newDistance = currentDistance; // default no change
			// double newAngle = p.getAngle();

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

			score = testNucleus.getAngleProfile("tail").differenceToProfile(targetCurve);
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

	/*
		This is used only for the consensus image.
		The consenus nucleus needs to be oriented with
		a given point at the bottom. Assumes CoM is at 0,0.
		Find the angle of rotation needed to put the point atthe bottom
	*/
	private double findRotationAngle(NucleusBorderPoint bottomPoint){
		// find the angle to rotate
		double angleToRotate = 0;
		double distanceFromZero = 180;

		// get the angle from the tail to the vertical axis line
		double tailAngle = RoundNucleus.findAngleBetweenXYPoints( bottomPoint, refoldNucleus.getCentreOfMass(), new XYPoint(0,-10));
			if(bottomPoint.getX()<0){
				tailAngle = 360-tailAngle; // correct for measuring the smallest angle
			}

		for(int i=0;i<360;i++){

			// get a copy of the bottom point
			XYPoint p = new XYPoint( bottomPoint.getX(), bottomPoint.getY() );
			
			// get the distance from tail to CoM
			double distance = p.getLengthTo(refoldNucleus.getCentreOfMass());

			// add the rotation amount
			double newAngle = tailAngle + i;

			double newX = Utils.getXComponentOfAngle(distance, newAngle);
			double newY = Utils.getYComponentOfAngle(distance, newAngle);

			if(Math.abs(newX) < distanceFromZero && newY < 0){
				angleToRotate = i;
				distanceFromZero = Math.abs(newX);
			}
		}

		logger.log("Rotating by "+(int)angleToRotate,Logger.DEBUG);
		return angleToRotate;
	}

	/*
		Given a point, rotate the roi around the CoM so that  the point
		is at the bottom
	*/
	public void putPointAtBottom(NucleusBorderPoint bottomPoint){

		double angleToRotate = findRotationAngle(bottomPoint);

		for(int i=0;i<refoldNucleus.getLength();i++){

			XYPoint p = refoldNucleus.getPoint(i);
			double distance = p.getLengthTo(refoldNucleus.getCentreOfMass());
			double oldAngle = RoundNucleus.findAngleBetweenXYPoints( p, refoldNucleus.getCentreOfMass(), new XYPoint(0,-10));
			if(p.getX()<0){
				oldAngle = 360-oldAngle;
			}

			double newAngle = oldAngle + angleToRotate;
			double newX = Utils.getXComponentOfAngle(distance, newAngle);
			double newY = Utils.getYComponentOfAngle(distance, newAngle);

			refoldNucleus.updatePoint(i, newX, newY);
		}
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