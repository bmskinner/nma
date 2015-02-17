 /*
	-----------------------
    CURVE REFOLDER CLASS
    -----------------------
    Contains the code for taking a profile, and an ideal profile, and
		making the profile fit
 */
package nucleusAnalysis;

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

public class CurveRefolder {

	private double[] targetCurve;
	private double[] initialCurve;

	private Nucleus initialNucleus;
	private Nucleus targetNucleus;

	private Plot nucleusPlot;
	private PlotWindow nucleusPlotWindow;

	private Plot anglePlot;
	private PlotWindow anglePlotWindow;

	public CurveRefolder(double[] target, Nucleus n){
		this.targetCurve = target;
		this.initialNucleus = n;
		this.initialCurve = n.getProfileAngles();
	}

	/*
		The main function to be called externally;
		all other functions will hang off this
	*/
	public void refoldCurve(){

		this.moveCoMtoZero();
		this.preparePlots();

		double score = compareProfiles(targetCurve, initialCurve);
		
		IJ.log("Refolding curve: initial score: "+(int)score);

		double prevScore = score*2;
		int i=0;
		while(prevScore - score >0.0001 || i<100){
			prevScore = score;
			score = this.iterateOverNucleus();
			// IJ.log("Iteration "+i+": "+score);
			i++;
		}
		IJ.log("Refolded curve: final score: "+(int)score);
		// this.plotTargetNucleus();
		// return targetNucleus;
	}

	/*
		Translate the XY coordinates of each point so that
		the nuclear centre of mass is at 0,0.
		Then set the target nucleus as a copy.
	*/
	private void moveCoMtoZero(){

		XYPoint centreOfMass = initialNucleus.getCentreOfMass();
		double xOffset = centreOfMass.getX();
		double yOffset = centreOfMass.getY();

		initialNucleus.setCentreOfMass(new XYPoint(0,0));

		FloatPolygon offsetPolygon = new FloatPolygon();

		for(int i=0; i<initialNucleus.smoothLength; i++){
			XYPoint p = initialNucleus.smoothedArray[i];

			double x = p.getX() - xOffset;
			double y = p.getY() - yOffset;
			offsetPolygon.addPoint(x, y);

			initialNucleus.smoothedArray[i].setX( x );
			initialNucleus.smoothedArray[i].setY( y );
			
		}
		initialNucleus.smoothedPolygon = offsetPolygon;

		this.targetNucleus = initialNucleus;
	}

	/*
		Create the plots that will be needed to display the 
		intiial and target nuclear shapes, plus the angle profiles
	*/
	private void preparePlots(){

		double[] xPoints = new double[initialNucleus.smoothLength];
		double[] yPoints = new double[initialNucleus.smoothLength];
		double[] aPoints = new double[initialNucleus.smoothLength]; // angles
		double[] pPoints = new double[initialNucleus.smoothLength]; // positions along array

		for(int i=0; i<targetNucleus.smoothLength; i++){
			XYPoint p = targetNucleus.smoothedArray[i];
			xPoints[i] = p.getX();
			yPoints[i] = p.getY();
			aPoints[i] = targetCurve[i];
			pPoints[i] = i;
		}
		
		nucleusPlot = new Plot( "Nucleus shape",
                              "",
                              "");
	
		// get the limits  for the plot  	
			double minX = targetNucleus.getMinX();
	  double maxX = targetNucleus.getMaxX();
	  double minY = targetNucleus.getMinY();
	  double maxY = targetNucleus.getMaxY();

	  // ensure that the scales for each axis are the same
	  double min = Math.min(minX, minY);
	  double max = Math.max(maxX, maxY);

	  // ensure there is room for expansion of the target nucleus
	  min = Math.floor(min - Math.abs(min));
	  max = Math.ceil(max * 2);

	  nucleusPlot.setLimits(min, Math.abs(min), min, Math.abs(min));

	  nucleusPlot.setSize(400,400);
	  nucleusPlot.setYTicks(true);
		nucleusPlot.setColor(Color.LIGHT_GRAY);
	  nucleusPlot.drawLine(min, 0, Math.abs(min), 0);
	  nucleusPlot.drawLine(0, min, 0, Math.abs(min));

	  anglePlot = new Plot( "Angles",
	                      "Position",
	                      "Angle");

	  anglePlot.setLimits(0,targetCurve.length,-50,360);
	  anglePlot.setSize(300,300);
	  anglePlot.setYTicks(true);

  	//   nucleusPlot.setColor(Color.LIGHT_GRAY);
		// nucleusPlot.addPoints(xPoints, yPoints, Plot.LINE);
		// anglePlot.setColor(Color.LIGHT_GRAY);
		// anglePlot.addPoints(pPoints, aPoints, Plot.LINE);
		nucleusPlotWindow = nucleusPlot.show();
		// anglePlotWindow = anglePlot.show();
	}

	/*
		Draw the current state of the target nucleus
	*/
	private void plotTargetNucleus(){

		double[] xPoints = new double[targetNucleus.smoothLength+1];
		double[] yPoints = new double[targetNucleus.smoothLength+1];
		// double[] aPoints = new double[targetNucleus.smoothLength+1]; // angles
		// double[] pPoints = new double[targetNucleus.smoothLength+1]; // positions along array

		for(int i=0; i<targetNucleus.smoothLength; i++){
			XYPoint p = targetNucleus.smoothedArray[i];
			xPoints[i] = p.getX();
			yPoints[i] = p.getY();
			// aPoints[i] = p.getInteriorAngle();
			// pPoints[i] = i;
		}

	  // ensure nucleus outline joins up at tip
	  XYPoint p = targetNucleus.smoothedArray[0];
	  xPoints[targetNucleus.smoothLength] = p.getX();
	  yPoints[targetNucleus.smoothLength] = p.getY();

		nucleusPlot.setColor(Color.DARK_GRAY);
		nucleusPlot.addPoints(xPoints, yPoints, Plot.LINE);
		// anglePlot.setColor(Color.DARK_GRAY);
		// anglePlot.addPoints(pPoints, aPoints, Plot.LINE);
		nucleusPlotWindow.drawPlot(nucleusPlot);
		// anglePlotWindow.drawPlot(anglePlot);
	}

	/*
		Go over the target nucleus, adjusting each point.
		Keep the change if it helps get closer to the target profile
	*/
	private double iterateOverNucleus(){

		double similarityScore = compareProfiles(targetCurve, targetNucleus.getProfileAngles());
		
		for(int i=0; i<targetNucleus.smoothLength; i++){

			XYPoint p = targetNucleus.smoothedArray[i];
    		
		double currentDistance = p.getLengthTo(new XYPoint(0,0));
		double newDistance = currentDistance; // default no change

		double oldX = p.getX();
		double oldY = p.getY();

		// make change dependent on score
		double amountToChange = Math.random() * (similarityScore/1000); // when score is 1000, change by up to 1. When score is 300, change byup to 0.33

		if(p.getInteriorAngle() > targetCurve[i]){
					newDistance = currentDistance + amountToChange; 
		}
		if(p.getInteriorAngle() < targetCurve[i]){
					newDistance = currentDistance - amountToChange; //  some change between 0 and 2
		}

		// find the angle the point makes to the x axis
		double angle = findAngleBetweenXYPoints(p, new XYPoint(0,0), new XYPoint(10, 0)); // point, 10,0, p,0
		if(oldY<0){
			angle = 360-angle;
		}
		double newX = getXComponentOfAngle(newDistance, angle);
			double newY = getYComponentOfAngle(newDistance, angle);

			// IJ.log("Old: X:"+(int)oldX+" Y:"+(int)oldY+" Distance: "+(int)currentDistance+" Angle: "+(int)angle);
			// IJ.log("New: X:"+(int)newX+" Y:"+(int)newY+" Distance: "+(int)newDistance+" Angle: "+(int)angle);

			p.setX(newX); // the new x position
			p.setY(newY); // the new y position

			// ensure the interior angle calculation works with the current points
			targetNucleus.smoothedPolygon = createPolygon(); 

			// measure the new profile & compare
			targetNucleus.makeAngleProfile();
			double[] newProfile = targetNucleus.getProfileAngles();
			double score = compareProfiles(targetCurve, newProfile);

			// IJ.log("Score: "+score);
			// do not apply change  if the distance from teh surrounding points changes too much
			double distanceToPrev = p.getLengthTo( targetNucleus.smoothedArray[ wrapIndex(i-1, targetNucleus.smoothLength) ] );
			double distanceToNext = p.getLengthTo( targetNucleus.smoothedArray[ wrapIndex(i+1, targetNucleus.smoothLength) ] );

			// reset if worse fit or distances are too high
			if(score > similarityScore  || distanceToNext > 1.2 || distanceToPrev > 1.2 ){
				p.setX(oldX);
				p.setY(oldY);
				targetNucleus.makeAngleProfile();
				targetNucleus.smoothedPolygon = createPolygon();
				// IJ.log("Rejecting change");
			} else {
				similarityScore = score;
				// IJ.log("Keeping change");
			}
		}
		return similarityScore;
	}

	private FloatPolygon createPolygon(){
		FloatPolygon offsetPolygon = new FloatPolygon();

		for(int i=0; i<targetNucleus.smoothLength; i++){

			XYPoint p = targetNucleus.smoothedArray[i];
			double x = p.getX();
			double y = p.getY();
			offsetPolygon.addPoint(x, y);
    }
    return offsetPolygon;
	}

	/*
		Find the total difference between two angle profiles
	*/
	private double compareProfiles(double[] profile1, double[] profile2){

		double d = 0;
		for(int i=0; i<profile1.length; i++){
			d += Math.abs(profile1[i] - profile2[i]);
		}
		return d;
	}

	/*
	  This is used only for the consensus image.
	  The consenus nucleus needs to be oriented with
	  the tail at the bottom. Assumes CoM is at 0,0
	*/
	private void putTailAtBottom(){

		// find the angle to rotate
		double angleToRotate = 0;
		double distanceFromZero = 180;

		// get the angle from the tail to the vertical axis line
		double tailAngle = findAngleBetweenXYPoints( targetNucleus.getSpermTail(), targetNucleus.getCentreOfMass(), new XYPoint(0,-10));
			if(targetNucleus.getSpermTail().getX()<0){
				tailAngle = 360-tailAngle; // correct for measuring the smallest angle
			}

		for(int i=0;i<360;i++){

			// get a copy of the sperm tail
			XYPoint p = new XYPoint( targetNucleus.getSpermTail().getX(), targetNucleus.getSpermTail().getY() );
			
			// get the distance from tail to CoM
			double distance = p.getLengthTo(targetNucleus.getCentreOfMass());

			// add the rotation amount
			double newAngle = tailAngle + i;

			double newX = getXComponentOfAngle(distance, newAngle);
				double newY = getYComponentOfAngle(distance, newAngle);

				if(Math.abs(newX) < distanceFromZero && newY < 0){
					angleToRotate = i;
					distanceFromZero = Math.abs(newX);
				}
		}

		// if(targetNucleus.getSpermTail().getX()<0){
		// 	angleToRotate = 360-angleToRotate;
		// }
		IJ.log("Rotating by "+(int)angleToRotate);

		for(int i=0;i<targetNucleus.smoothLength;i++){

			XYPoint p = targetNucleus.smoothedArray[i];
			double distance = p.getLengthTo(targetNucleus.getCentreOfMass());
			double oldAngle = findAngleBetweenXYPoints( p, targetNucleus.getCentreOfMass(), new XYPoint(0,-10));
			if(p.getX()<0){
				oldAngle = 360-oldAngle;
			}

			double newAngle = oldAngle + angleToRotate;
			double newX = getXComponentOfAngle(distance, newAngle);
				double newY = getYComponentOfAngle(distance, newAngle);

				p.setX(newX); // the new x position
				p.setY(newY); // the new y position
		}

		// also flip if tip X is >0
		if(targetNucleus.getSpermTip().getX() > 0){
			IJ.log("  Flipping");
			targetNucleus.flipXAroundPoint(targetNucleus.getCentreOfMass());
		}

		plotTargetNucleus();
	}

	private void exportImage(NucleusCollection collection){
		ImagePlus plot = nucleusPlot.getImagePlus();
	  IJ.saveAsTiff(plot, targetNucleus.getDirectory()+"\\plotConsensus."+collection.collectionType+".tiff");

	  targetNucleus.setPath(targetNucleus.getDirectory()+"\\logConsensusNucleus."+collection.collectionType+".txt");
	  IJ.log("Exporting to: "+targetNucleus.getPath());
	  targetNucleus.printLogFile(targetNucleus.getPath());
	}

	/*
	  Using a list of signal locations, draw on
	  the consensus plot.
	*/
	public void addSignalsToConsensus(NucleusCollection collection){

		for(int i= 0; i<collection.nucleiCollection.size();i++){ // for each roi

	    Nucleus n = collection.nucleiCollection.get(i);

	    ArrayList<ArrayList<NuclearSignal>> signals = new ArrayList<ArrayList<NuclearSignal>>(0);
	    signals.add(n.getRedSignals());
	    signals.add(n.getGreenSignals());

	    int signalCount = 0;
	    for( ArrayList<NuclearSignal> signalGroup : signals ){

	      if(signalGroup.size()>0){

	        ArrayList<Double> xPoints = new ArrayList<Double>(0);
	        ArrayList<Double> yPoints = new ArrayList<Double>(0);

	        for(int j=0; j<signalGroup.size();j++){

	        	double angle = signalGroup.get(j).getAngle();
	        	double fractionalDistance = signalGroup.get(j).getFractionalDistance();

	        	// determine the total distance to the border at this angle
	        	double distanceToBorder = getDistanceFromAngle(angle);

	        	// convert to fractional distance to signal
	        	double signalDistance = distanceToBorder * fractionalDistance;
	          
	          // adjust X and Y because we are now counting angles from the vertical axis
	        	double signalX = getXComponentOfAngle(signalDistance, angle-90);
	        	double signalY = getYComponentOfAngle(signalDistance, angle-90);

	          // add to array
	          xPoints.add( signalX );
	          yPoints.add( signalY );
	         // IJ.log("Signal "+j+": Fdist: "+fractionalDistance+" Dist: "+signalDistance+" X: "+signalX+" Y: "+signalY);
	          
	        }
	        if(signalCount==0)
	          nucleusPlot.setColor(Color.RED);
	        else
	          nucleusPlot.setColor(Color.GREEN);

	        nucleusPlot.setLineWidth(2);
	        nucleusPlot.addPoints(xPoints, yPoints, Plot.DOT);
	      }
	      signalCount++;
	    }
	  }
	  nucleusPlotWindow.drawPlot(nucleusPlot);

	}

	private double getDistanceFromAngle(double angle){

		// go through the nucleus outline
		// measure the angle to the tail and the distance to the CoM
		// if closest to target angle, return distance
		double bestAngle = 180;
		double bestDiff = 180;
		double bestDistance = 180;

		for(int i=0;i<targetNucleus.smoothLength;i++){
			XYPoint p = targetNucleus.smoothedArray[i];
			double distance = p.getLengthTo(targetNucleus.getCentreOfMass());
			double pAngle = findAngleBetweenXYPoints( p, targetNucleus.getCentreOfMass(), new XYPoint(0,-10));
			if(p.getX()<0){
				pAngle = 360-pAngle;
			}

			if(Math.abs(angle-pAngle) < bestDiff){
				bestAngle = pAngle;
				bestDiff = Math.abs(angle-pAngle);
				bestDistance = distance;
			}
		}
		// IJ.log("Target angle: "+angle+": Best angel: "+bestAngle+" Distance: "+bestDistance);
		return bestDistance;
	}

}