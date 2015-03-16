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
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.TextRoi;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.StackConverter;
import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.Color;
import java.awt.geom.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.Polygon;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.*;

import no.nuclei.INuclearFunctions;
import no.nuclei.Nucleus;
import no.analysis.Analysable;
import no.utility.*;
import no.components.*;


public class CurveRefolder{

	private Profile targetCurve;
	private Profile q25;
	private Profile q75;
	// private double[] initialCurve;

	// private INuclearFunctions refoldNucleus;
	// private INuclearFunctions refoldNucleus;
	private INuclearFunctions refoldNucleus;
	private Nucleus testNucleus;

	private Plot nucleusPlot;
	private PlotWindow nucleusPlotWindow;

	private Plot anglePlot;
	private PlotWindow anglePlotWindow;

	private double plotLimit;

	public CurveRefolder(Profile target, Profile q25, Profile q75, INuclearFunctions n){
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
			
			IJ.log("    Refolding curve: initial score: "+(int)score);

			double originalScore = score;
			double prevScore = score*2;
			int i=0;
			while( (prevScore - score)/prevScore > 0.01 || i<50){ // iterate until converging on a better curve  score >= originalScore
				prevScore = score;
				score = this.iterateOverNucleus();
				i++;
				// if(i%50==0){
					// IJ.log("    Iteration "+i+": "+(int)score);
				// }
			}
			IJ.log("    Refolded curve: final score: "+(int)score);
		} catch(Exception e){
			throw new Exception("Cannot calculate scores: "+e);
		}
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
		refoldNucleus.updatePolygon();
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

		// iterate from tail point
		int tailIndex = refoldNucleus.getBorderIndex("tail");

		for(int i=0; i<refoldNucleus.getLength(); i++){

			int index = NuclearOrganisationUtility.wrapIndex(i + tailIndex, refoldNucleus.getLength());

			int prevIndex = NuclearOrganisationUtility.wrapIndex(i-3 + tailIndex, refoldNucleus.getLength());
			int nextIndex = NuclearOrganisationUtility.wrapIndex(i+3 + tailIndex, refoldNucleus.getLength());

			// IJ.log("Getting point: "+index);
			XYPoint n = refoldNucleus.getPoint( index  );

			double distance = ((this.q75.get(index) - this.q25.get(index))/maxIQR)*5; // scale to maximum of 10 pixels total width 
			// use scaling factor
			// normalise distances to the plot

			Equation eq = new Equation(refoldNucleus.getPoint( prevIndex  ), refoldNucleus.getPoint( nextIndex  ));
			// move the line to the index point, and find the orthogonal line
			Equation perp = eq.translate(n).getPerpendicular(n);

			XYPoint aPoint = perp.getPointOnLine(n, (0-distance));
			XYPoint bPoint = perp.getPointOnLine(n, distance);

			XYPoint innerPoint = refoldNucleus.getPolygon().contains(  (float) aPoint.getX(), (float) aPoint.getY() ) ? aPoint : bPoint;
			XYPoint outerPoint = refoldNucleus.getPolygon().contains(  (float) bPoint.getX(), (float) bPoint.getY() ) ? aPoint : bPoint;

			innerIQRX[i] = innerPoint.getX();
			innerIQRY[i] = innerPoint.getY();
			outerIQRX[i] = outerPoint.getX();
			outerIQRY[i] = outerPoint.getY();
			
		}
		innerIQRX[refoldNucleus.getLength()] = innerIQRX[0];
		innerIQRY[refoldNucleus.getLength()] = innerIQRY[0];
		outerIQRX[refoldNucleus.getLength()] = outerIQRX[0];
		outerIQRY[refoldNucleus.getLength()] = outerIQRY[0];

		nucleusPlot.setColor(Color.LIGHT_GRAY);
		nucleusPlot.addPoints(innerIQRX, innerIQRY, Plot.LINE);
		nucleusPlot.setColor(Color.LIGHT_GRAY);
		nucleusPlot.addPoints(outerIQRX, outerIQRY, Plot.LINE);

		// nucleusPlot.setColor(Color.DARK_GRAY);
		// nucleusPlot.addPoints(xPoints, yPoints, Plot.LINE);
		nucleusPlot.setColor(Color.DARK_GRAY);
		nucleusPlot.addPoints(refoldNucleus.getPolygon().xpoints, refoldNucleus.getPolygon().ypoints, Plot.LINE);

	}

	/*
		Go over the target nucleus, adjusting each point.
		Keep the change if it helps get closer to the target profile

		Changes to make:
			Random mutation to the distance of a point from the CoM
			Random mutation to the angle of a point from the CoM 
			Together these affect the XY position of the point
	*/
	private double iterateOverNucleus() throws Exception{

		Profile refoldProfile = refoldNucleus.getAngleProfile("tail");
		Profile interpolatedTargetCurve = targetCurve.interpolate(refoldProfile.size());

		double similarityScore = refoldProfile.differenceToProfile(targetCurve);
		// IJ.log("    Iteration score: "+(int)similarityScore);

		double medianDistanceBetweenPoints = refoldNucleus.getMedianDistanceBetweenPoints();
		// refoldNucleus.getAngleProfile("tail").print();

		testNucleus = new Nucleus( (Nucleus)refoldNucleus);
		// IJ.log("Before calculating new profile:");
		// testNucleus.dumpInfo(Nucleus.BORDER_POINTS);
		// testNucleus.getAngleProfile().print();
		// IJ.log("");
		
		for(int i=0; i<refoldNucleus.getLength(); i++){

			Nucleus testNucleus = new Nucleus( (Nucleus)refoldNucleus);

			double score = testNucleus.getAngleProfile("tail").differenceToProfile(targetCurve);
			// IJ.log("    Internal score: "+(int)score);

			NucleusBorderPoint p = testNucleus.getPoint(i);
			
			double currentDistance = p.getLengthTo(new XYPoint(0,0));
			double newDistance = currentDistance; // default no change
			// double newAngle = p.getAngle();

			double oldX = p.getX();
			double oldY = p.getY();

			// make change dependent on score
			double amountToChange =  Math.min( Math.random() * (similarityScore/1000), 1); // when score is 1000, change by up to 1. When score is 300, change byup to 0.33

			if(refoldProfile.get(i) > interpolatedTargetCurve.get(i))
				newDistance = currentDistance + amountToChange;
			
			if(refoldProfile.get(i) < interpolatedTargetCurve.get(i))
				newDistance = currentDistance - amountToChange; //  some change between 0 and 2
			

			// find the angle the point makes to the x axis
			double angle = Nucleus.findAngleBetweenXYPoints(p, new XYPoint(0,0), new XYPoint(10, 0)); // point, 10,0, p,0
			if(oldY<0){
				angle = 360-angle;
			}
			double newX = NuclearOrganisationUtility.getXComponentOfAngle(newDistance, angle);
			double newY = NuclearOrganisationUtility.getYComponentOfAngle(newDistance, angle);

			try{
				testNucleus.updatePoint(i, newX, newY);
			} catch(Exception e){
				throw new Exception("Cannot update point "+i+" to "+newX+", "+newY+": "+e);
			}

			// ensure the interior angle calculation works with the current points
			try{
				testNucleus.updatePolygon();
			} catch(Exception e){
				throw new Exception("Cannot set new polygon position "+e);
			}

			// measure the new profile & compare
			try{
				testNucleus.calculateAngleProfile(refoldNucleus.getAngleProfileWindowSize());
				// if(i==0){
				// 	IJ.log("After calculating new profile:");
				// 	testNucleus.getAngleProfile().print();
				// 	testNucleus.dumpInfo(Nucleus.BORDER_POINTS);
				// }
			} catch(Exception e){
				throw new Exception("Cannot calculate angle profile: "+e);
			}

			score = testNucleus.getAngleProfile("tail").differenceToProfile(targetCurve);
			// IJ.log("    Internal score: "+(int)score);

			// do not apply change  if the distance from teh surrounding points changes too much
			double distanceToPrev = p.getLengthTo( testNucleus.getPoint( NuclearOrganisationUtility.wrapIndex(i-1, testNucleus.getLength()) ) );
			double distanceToNext = p.getLengthTo( testNucleus.getPoint( NuclearOrganisationUtility.wrapIndex(i+1, testNucleus.getLength()) ) );

			// apply the change if better fit
			if(score < similarityScore && distanceToNext < medianDistanceBetweenPoints*1.2 && distanceToPrev < medianDistanceBetweenPoints*1.2){
				refoldNucleus.updatePoint(i, newX, newY);
				refoldNucleus.calculateAngleProfile(refoldNucleus.getAngleProfileWindowSize());
				refoldNucleus.updatePolygon();
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
		double tailAngle = Nucleus.findAngleBetweenXYPoints( bottomPoint, refoldNucleus.getCentreOfMass(), new XYPoint(0,-10));
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

			double newX = NuclearOrganisationUtility.getXComponentOfAngle(distance, newAngle);
			double newY = NuclearOrganisationUtility.getYComponentOfAngle(distance, newAngle);

			if(Math.abs(newX) < distanceFromZero && newY < 0){
				angleToRotate = i;
				distanceFromZero = Math.abs(newX);
			}
		}

		IJ.log("    Rotating by "+(int)angleToRotate);
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
			double oldAngle = Nucleus.findAngleBetweenXYPoints( p, refoldNucleus.getCentreOfMass(), new XYPoint(0,-10));
			if(p.getX()<0){
				oldAngle = 360-oldAngle;
			}

			double newAngle = oldAngle + angleToRotate;
			double newX = NuclearOrganisationUtility.getXComponentOfAngle(distance, newAngle);
			double newY = NuclearOrganisationUtility.getYComponentOfAngle(distance, newAngle);

			refoldNucleus.updatePoint(i, newX, newY);
		}
		refoldNucleus.updatePolygon();
	}

	/*
		Using a list of signal locations, draw on
		the consensus plot.
	*/
	public void addSignalsToConsensus(Analysable collection){

		for(int i= 0; i<collection.getNuclei().size();i++){ // for each roi

			INuclearFunctions n = collection.getNuclei().get(i);

			List<List<NuclearSignal>> signals = new ArrayList<List<NuclearSignal>>(0);
			signals.add(n.getRedSignals());
			signals.add(n.getGreenSignals());

			ImageProcessor plotIP = nucleusPlot.getImagePlus().getProcessor();
			Calibration cal = nucleusPlot.getImagePlus().getCalibration();
			cal.setUnit("pixels");
			cal.pixelWidth = 1;
			cal.pixelHeight = 1;


			int signalCount = 0;
			for( List<NuclearSignal> signalGroup : signals ){

				Color colour = signalCount==0 ? new Color(255,0,0,50) : new Color(0,255,0,50);

				if(signalGroup.size()>0){

					ArrayList<Double> xPoints = new ArrayList<Double>(0);
					ArrayList<Double> yPoints = new ArrayList<Double>(0);

					for(int j=0; j<signalGroup.size();j++){

						double angle = signalGroup.get(j).getAngle();
						double fractionalDistance = signalGroup.get(j).getFractionalDistanceFromCoM();
						double diameter = signalGroup.get(j).getRadius() * 2;

						// determine the total distance to the border at this angle
						double distanceToBorder = getDistanceFromAngle(angle);

						// convert to fractional distance to signal
						double signalDistance = distanceToBorder * fractionalDistance;
						
						// adjust X and Y because we are now counting angles from the vertical axis
						double signalX = NuclearOrganisationUtility.getXComponentOfAngle(signalDistance, angle-90);
						double signalY = NuclearOrganisationUtility.getYComponentOfAngle(signalDistance, angle-90);

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

						double xCorrected = 222 + (  161 * xRatio ) -7; // 9 is arbirtrary offset for now
						double yCorrected = 188 - (  172 * yRatio ) -7;

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

	private double getDistanceFromAngle(double angle){

		// go through the nucleus outline
		// measure the angle to the tail and the distance to the CoM
		// if closest to target angle, return distance
		double bestAngle = 180;
		double bestDiff = 180;
		double bestDistance = 180;

		for(int i=0;i<refoldNucleus.getLength();i++){
			XYPoint p = refoldNucleus.getBorderPoint(i);
			double distance = p.getLengthTo(refoldNucleus.getCentreOfMass());
			double pAngle = Nucleus.findAngleBetweenXYPoints( p, refoldNucleus.getCentreOfMass(), new XYPoint(0,-10));
			if(p.getX()<0){
				pAngle = 360-pAngle;
			}

			if(Math.abs(angle-pAngle) < bestDiff){
				bestAngle = pAngle;
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

	public void exportProfileOfRefoldedImage(Analysable collection){
	 
		String logFile = collection.makeGlobalLogFile("logConsensusNucleus");

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

	public void exportImage(Analysable collection){
		ImagePlus plot = nucleusPlot.getImagePlus();
		Calibration cal = plot.getCalibration();
		cal.setUnit("pixels");
		cal.pixelWidth = 1;
		cal.pixelHeight = 1;
		IJ.saveAsTiff(plot, refoldNucleus.getOutputFolder()+File.separator+"plotConsensus."+collection.getType()+".tiff");
	}

}