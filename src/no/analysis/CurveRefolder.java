 /*
	-----------------------
	CURVE REFOLDER CLASS
	-----------------------
	Contains the code for taking a profile, and an ideal profile, and
		making the profile fit
 */
package no.analysis;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Plot;
import ij.measure.Calibration;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.*;

import utility.Constants;
import utility.Equation;
import utility.Logger;
import utility.Utils;
import no.nuclei.Nucleus;
import no.nuclei.RoundNucleus;
import no.nuclei.sperm.RodentSpermNucleus;
import no.collections.CellCollection;
import no.gui.ColourSelecter;
import no.components.*;


public class CurveRefolder{

	private Profile targetCurve;
	private Profile q25;
	private Profile q75;

	private Nucleus refoldNucleus;
//	private Nucleus testNucleus;

	private Plot nucleusPlot;
//	private PlotWindow nucleusPlotWindow;
	
	private static Logger logger;


	public static final int FAST_MODE = 0; // default; iterate until convergence
	public static final int INTENSIVE_MODE = 1; // iterate until value
	public static final int BRUTAL_MODE = 2; // iterate until value
	private int mode = FAST_MODE;

	public static Map<String, Integer> MODES = new LinkedHashMap<String, Integer>();

	static {
		MODES.put("Fast", FAST_MODE);
		MODES.put("Intensive", INTENSIVE_MODE);
		MODES.put("Brutal", BRUTAL_MODE);
	}

	private double plotLimit;
	
	public static boolean run(CellCollection collection, Class<?> nucleusClass, String refoldMode){

		logger = new Logger(collection.getDebugFile(), "CurveRefolder");
		try{ 

			// make an entirely new nucleus to play with
			Nucleus n = (Nucleus)collection.getNucleusMostSimilarToMedian("tail");
			Constructor<?> nucleusConstructor = nucleusClass.getConstructor(new Class[]{RoundNucleus.class});
			Nucleus refoldCandidate  = (Nucleus) nucleusConstructor.newInstance(n);

			if(refoldCandidate==null){
				throw new Exception("Null reference to nucleus refold candidate");
			}

			logger.log("Refolding nucleus of class: "+refoldCandidate.getClass().getSimpleName());
			logger.log("Subject: "+refoldCandidate.getImageName()+"-"+refoldCandidate.getNucleusNumber(), Logger.DEBUG);

			Profile targetProfile = collection.getProfileCollection().getProfile("tail");
			Profile q25 = collection.getProfileCollection().getProfile("tail25");
			Profile q75 = collection.getProfileCollection().getProfile("tail75");

			if(targetProfile==null){
				throw new Exception("Null reference to target profile");
			}
			if(q25==null || q75==null){
				throw new Exception("Null reference to q25 or q75 profile");
			}

			CurveRefolder refolder = new CurveRefolder(targetProfile, q25, q75, refoldCandidate);
			refolder.setMode(refoldMode);
			refolder.refoldCurve();

			// orient refolded nucleus to put tail at the bottom
			refolder.putPointAtBottom(refoldCandidate.getBorderTag("tail"));

			// if rodent sperm, put tip on left if needed
			if(refoldCandidate.getClass().equals(RodentSpermNucleus.class)){

				if(refoldCandidate.getBorderTag("tip").getX()>0){
					refoldCandidate.flipXAroundPoint(refoldCandidate.getCentreOfMass());
				}
			}

			refolder.plotNucleus();

			// draw signals on the refolded nucleus
			refolder.addSignalsToConsensus(collection);
			refolder.exportImage(collection);
			refolder.exportProfileOfRefoldedImage(collection);
			collection.addConsensusNucleus(refoldCandidate);

		} catch(Exception e){
			logger.log("Unable to refold nucleus: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
			return false;
		} 
		return true;
	}

	public CurveRefolder(Profile target, Profile q25, Profile q75, Nucleus n){
		this.targetCurve = target;
		this.q25 = q25.interpolate(n.getLength());
		this.q75 = q75.interpolate(n.getLength());
		this.refoldNucleus = n;
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
			this.preparePlots();
		} catch(Exception e){
			throw new Exception("Unable to prepare plots");
		}

		try{
			double score = refoldNucleus.getAngleProfile("tail").differenceToProfile(targetCurve);
			
			logger.log("Refolding curve: initial score: "+(int)score, Logger.INFO);

			double originalScore = score;
			double prevScore = score*2;
			int i=0;

			if(this.mode==FAST_MODE){
				while( (prevScore - score)/prevScore > 0.001 || i<50){ // iterate until converging
					prevScore = score;
					score = this.iterateOverNucleus();
					i++;
					logger.log("Iteration "+i+": "+(int)score, Logger.DEBUG);
				}
			}

			if(this.mode==INTENSIVE_MODE){

				while(score > (originalScore*0.1) && i<1000){ // iterate until 0.6 original score, or 1000 iterations
					prevScore = score;
					score = this.iterateOverNucleus();
					i++;
					if(i%50==0){
						logger.log("Iteration "+i+": "+(int)score, Logger.DEBUG);
					}
				}
			}

			if(this.mode==BRUTAL_MODE){

				while(score > (originalScore*0.1) && i<10000){ // iterate until 0.1 original score, or 10000 iterations
					prevScore = score;
					score = this.iterateOverNucleus();
					i++;
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
//		refoldNucleus.updatePolygon();
	}

	/*
		Create the plots that will be needed to display the 
		intiial and target nuclear shapes, plus the angle profiles
	*/
	private void preparePlots(){
		
		nucleusPlot = new Plot( "Nucleus shape",
								"",
								"");

		// get the limits  for the plot  	
		double minX = refoldNucleus.getMinX();
		double maxX = refoldNucleus.getMaxX();
		double minY = refoldNucleus.getMinY();
		double maxY = refoldNucleus.getMaxY();

		// ensure that the scales for each axis are the same
		double min = Math.min(minX, minY);
		double max = Math.max(maxX, maxY);

		// ensure there is room for expansion of the target nucleus
		min = Math.floor(min - Math.abs(min));
		max = Math.ceil(max * 2);

		this.plotLimit = Math.abs(min);

		nucleusPlot.setLimits(min, Math.abs(min), min, Math.abs(min));

		nucleusPlot.setSize(400,400);
		nucleusPlot.setYTicks(false);
		nucleusPlot.setXTicks(false);
		nucleusPlot.setColor(Color.LIGHT_GRAY);
		nucleusPlot.drawLine(min, 0, Math.abs(min), 0);
		nucleusPlot.drawLine(0, min, 0, Math.abs(min));
	}

	/*
		Draw the current state of the target nucleus
	*/
	public void plotNucleus(){
		
		// Add lines to show the IQR of the angle profile at each point
		double[] innerIQRX = new double[refoldNucleus.getLength()+1];
		double[] innerIQRY = new double[refoldNucleus.getLength()+1];
		double[] outerIQRX = new double[refoldNucleus.getLength()+1];
		double[] outerIQRY = new double[refoldNucleus.getLength()+1];

		// find the maximum difference between IQRs
		double maxIQR = 0;
		for(int i=0; i<refoldNucleus.getLength(); i++){
			if(this.q75.get(i) - this.q25.get(i)>maxIQR){
				maxIQR = this.q75.get(i) - this.q25.get(i);
			}
		}


		// get the maximum values from nuclear diameters
		// get the limits  for the plot  	
		double min = Math.min(refoldNucleus.getMinX(), refoldNucleus.getMinY());
		double max = Math.max(refoldNucleus.getMaxX(), refoldNucleus.getMaxY());
		double scale = Math.min(Math.abs(min), Math.abs(max));

		// iterate from tail point
		int tailIndex = refoldNucleus.getBorderIndex("tail");

		for(int i=0; i<refoldNucleus.getLength(); i++){

			int index = Utils.wrapIndex(i + tailIndex, refoldNucleus.getLength());

			int prevIndex = Utils.wrapIndex(i-3 + tailIndex, refoldNucleus.getLength());
			int nextIndex = Utils.wrapIndex(i+3 + tailIndex, refoldNucleus.getLength());

			// IJ.log("Getting point: "+index);
			XYPoint n = refoldNucleus.getPoint( index  );

			double distance = ((this.q75.get(index) - this.q25.get(index))/maxIQR)*(scale/10); // scale to maximum of 10% the minimum diameter 
			// use scaling factor
			// IJ.log("    Distance: "+distance);
			// normalise distances to the plot

			Equation eq = new Equation(refoldNucleus.getPoint( prevIndex  ), refoldNucleus.getPoint( nextIndex  ));
			// move the line to the index point, and find the orthogonal line
			Equation perp = eq.translate(n).getPerpendicular(n);

			XYPoint aPoint = perp.getPointOnLine(n, (0-distance));
			XYPoint bPoint = perp.getPointOnLine(n, distance);
			// IJ.log("    Eq: "+eq.print());
			// IJ.log("    Perp: "+perp.print());
			// IJ.log("    Position: n: "+n.toString()+"   A: "+aPoint.toString()+"   B: "+bPoint.toString());

			XYPoint innerPoint = Utils.createPolygon(refoldNucleus).contains(  (float) aPoint.getX(), (float) aPoint.getY() ) ? aPoint : bPoint;
			XYPoint outerPoint = Utils.createPolygon(refoldNucleus).contains(  (float) bPoint.getX(), (float) bPoint.getY() ) ? aPoint : bPoint;

			innerIQRX[i] = innerPoint.getX();
			innerIQRY[i] = innerPoint.getY();
			outerIQRX[i] = outerPoint.getX();
			outerIQRY[i] = outerPoint.getY();

		}
		innerIQRX[refoldNucleus.getLength()] = innerIQRX[0];
		innerIQRY[refoldNucleus.getLength()] = innerIQRY[0];
		outerIQRX[refoldNucleus.getLength()] = outerIQRX[0];
		outerIQRY[refoldNucleus.getLength()] = outerIQRY[0];

		nucleusPlot.setColor(Color.DARK_GRAY);
		nucleusPlot.addPoints(innerIQRX, innerIQRY, Plot.LINE);
		nucleusPlot.setColor(Color.DARK_GRAY);
		nucleusPlot.addPoints(outerIQRX, outerIQRY, Plot.LINE);


		// draw the segments on top of the IQR lines
		List<NucleusBorderSegment> segmentList = refoldNucleus.getSegments();
		if(!segmentList.isEmpty()){ // only draw if there are segments
			for(int i=0;i<segmentList.size();i++){

				NucleusBorderSegment seg = refoldNucleus.getSegmentTag("Seg_"+i);

				float[] xpoints = new float[seg.length(refoldNucleus.getLength())+1];
				float[] ypoints = new float[seg.length(refoldNucleus.getLength())+1];
				for(int j=0; j<=seg.length(refoldNucleus.getLength());j++){
					int k = Utils.wrapIndex(seg.getStartIndex()+j, refoldNucleus.getLength());
					NucleusBorderPoint p = refoldNucleus.getBorderPoint(k); // get the border points in the segment
					xpoints[j] = (float) p.getX();
					ypoints[j] = (float) p.getY();
				}

				// avoid colour wrapping when segment number is 1 more than the colour list
				Color color = i==0 && segmentList.size()==9 ? Color.MAGENTA : ColourSelecter.getSegmentColor(i);

				nucleusPlot.setColor(color);
				nucleusPlot.setLineWidth(3);
				nucleusPlot.addPoints(xpoints, ypoints, Plot.LINE);
			}
		} else { // segment list was empty, fall back on black and white
			logger.log("Cannot add segments to consensus",Logger.ERROR);
			
			double[] xPoints = new double[refoldNucleus.getLength()+1];
			double[] yPoints = new double[refoldNucleus.getLength()+1];

			for(int i=0; i<refoldNucleus.getLength(); i++){
				XYPoint p = refoldNucleus.getPoint(i);
				xPoints[i] = p.getX();
				yPoints[i] = p.getY();
			}

			// ensure nucleus outline joins up at tip
			XYPoint p = refoldNucleus.getPoint(0);
			xPoints[refoldNucleus.getLength()] = p.getX();
			yPoints[refoldNucleus.getLength()] = p.getY();
			
			nucleusPlot.setColor(Color.DARK_GRAY);
			nucleusPlot.addPoints(Utils.createPolygon(refoldNucleus).xpoints, Utils.createPolygon(refoldNucleus).ypoints, Plot.LINE);

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

	/*
		Using a list of signal locations, draw on
		the consensus plot.
	*/
	public void addSignalsToConsensus(CellCollection collection){

		for(int i= 0; i<collection.getNuclei().size();i++){ // for each roi

			Nucleus n = collection.getNuclei().get(i);

			ImageProcessor plotIP = nucleusPlot.getImagePlus().getProcessor();
			Calibration cal = nucleusPlot.getImagePlus().getCalibration();
			cal.setUnit("pixels");
			cal.pixelWidth = 1;
			cal.pixelHeight = 1;

			int signalCount = 0;
			for( int j : n.getSignalCollection().getChannels()){
				List<NuclearSignal> signals = n.getSignalCollection().getSignals(j);
				
				Color colour = signalCount== 0 ? new Color(255,0,0,50) : new Color(0,255,0,50);

				if(!signals.isEmpty()){

					ArrayList<Double> xPoints = new ArrayList<Double>(0);
					ArrayList<Double> yPoints = new ArrayList<Double>(0);


					for(NuclearSignal s : signals){


						double angle = s.getAngle();

						double fractionalDistance = s.getFractionalDistanceFromCoM();
						double diameter = s.getRadius() * 2;

						// determine the total distance to the border at this angle
						double distanceToBorder = getDistanceFromAngle(angle, refoldNucleus);

						// convert to fractional distance to signal
						double signalDistance = distanceToBorder * fractionalDistance;

						if(angle==0){ // no angle was calculated, so spread the points based on distance from CoM
							angle = signalCount == Constants.RGB_RED ? 360 * fractionalDistance : 360 * fractionalDistance + 180;
						}

						// adjust X and Y because we are now counting angles from the vertical axis
						double signalX = Utils.getXComponentOfAngle(signalDistance, angle-90);
						double signalY = Utils.getYComponentOfAngle(signalDistance, angle-90);

						/* draw the circles on the plot
							 An ImageJ Plot cannot draw circles by itself. We therefore need to get the
							 underlying ImageProcessor, and draw the circles on this. To do this correctly,
							 the signal positions within the plot must be translated into pixel positions
							 on the ImageProcessor. 
							 The image is 400*400 pixels. 
							 60 pixels used for the left border, 18 pixels for the right, leaving 322 horizontal pixels
							 17 used for the top border, 40 for the bottom border, leaving 343 vertical pixels.
							 The plot 0,0 is therefore at x:161+60 = 221 y:172+17= 189  :  221,189
							 Positive Y values must be subtracted from this
							 Negative X values must be subtracted from this
						 */
						double xRatio = signalX / this.plotLimit; // the ratio of the signal from the centre to the plot edge
						double yRatio = signalY / this.plotLimit;

						double xCorrected = 222 + (  161 * xRatio ) -2; // 9 is arbirtrary offset for now
						double yCorrected = 188 - (  172 * yRatio ) -2;

						// double xCorrected = 221 + ( xRatio + signalX );
						// double yCorrected = 189 - ( yRatio + signalY );

						// IJ.log("X: "+signalX+"  Y: "+signalY+" Xc: "+xCorrected+"  Yc: "+yCorrected);

						plotIP.setColor(colour);
						plotIP.drawOval((int) xCorrected, (int)yCorrected, (int)diameter, (int)diameter);

						// add to array
						xPoints.add( signalX );
						yPoints.add( signalY ); 
					}

					nucleusPlot.setColor(colour);
					nucleusPlot.setLineWidth(2);
					nucleusPlot.addPoints(xPoints, yPoints, Plot.DOT);
				}
				signalCount++;
			}
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

	/*
		-----------------------
		Export data
		-----------------------
	*/

	public void exportProfileOfRefoldedImage(CellCollection collection){

		String logFile = collection.getLogFileName("log.consensusNucleus");

		StringBuilder outLine = new StringBuilder();

		outLine.append(	"X_INT\t"+
				"Y_INT\t"+
				"X_DOUBLE\t"+
				"Y_DOUBLE\t"+
				"INTERIOR_ANGLE\t"+
				"NORMALISED_PROFILE_X\t"+
				"DISTANCE_PROFILE\r\n");

		for(int i=0;i<refoldNucleus.getLength();i++){

			double normalisedX = ((double)i/(double)refoldNucleus.getLength())*100; // normalise to 100 length

			outLine.append( refoldNucleus.getPoint(i).getXAsInt()        				    +"\t"+
					refoldNucleus.getPoint(i).getYAsInt()            				+"\t"+
					refoldNucleus.getPoint(i).getX()                  			+"\t"+
					refoldNucleus.getPoint(i).getY()                  			+"\t"+
					refoldNucleus.getAngle(i)													      +"\t"+
					normalisedX                                             +"\t"+
					refoldNucleus.getPoint(i).getDistanceAcrossCoM()  			+"\r\n");
		}
		IJ.append( outLine.toString(), logFile);
	}

	public void exportImage(CellCollection collection){
		ImagePlus plot = nucleusPlot.getImagePlus();
		Calibration cal = plot.getCalibration();
		cal.setUnit("pixels");
		cal.pixelWidth = 1;
		cal.pixelHeight = 1;
		IJ.saveAsTiff(plot, refoldNucleus.getOutputFolder()+File.separator+"plotConsensus."+collection.getType()+".tiff");
	}

}