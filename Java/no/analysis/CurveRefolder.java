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

import no.nuclei.Nucleus;
import no.collections.NucleusCollection;
import no.utility.*;
import no.components.*;


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
		this.initialCurve = n.getInteriorAngles();
	}

	/*
		The main function to be called externally;
		all other functions will hang off this
	*/
	public void refoldCurve(){

		this.moveCoMtoZero();
		this.preparePlots();

		double score = compareProfiles(targetCurve, initialCurve);
		
		IJ.log("    Refolding curve: initial score: "+(int)score);

		double originalScore = score;
		double prevScore = score*2;
		int i=0;
		// while( (prevScore - score)/prevScore > 0.01 || i<50 ){ // iterate until converging on a better curve  score >= originalScore
		// 	prevScore = score;
		// 	score = this.iterateOverNucleus();
		// 	i++;
		// 	if(i%50==0){
		// 		IJ.log("    Iteration "+i+": "+(int)score);
		// 	}
		// }
		IJ.log("    Refolded curve: final score: "+(int)score);
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

		for(int i=0; i<initialNucleus.getLength(); i++){
			XYPoint p = initialNucleus.getBorderPoint(i);

			double x = p.getX() - xOffset;
			double y = p.getY() - yOffset;
			offsetPolygon.addPoint(x, y);

			initialNucleus.getBorderPoint(i).setX( x );
			initialNucleus.getBorderPoint(i).setY( y );
			
		}
		initialNucleus.setPolygon(offsetPolygon);

		this.targetNucleus = initialNucleus;
	}

	/*
		Create the plots that will be needed to display the 
		intiial and target nuclear shapes, plus the angle profiles
	*/
	private void preparePlots(){

		double[] xPoints = new double[initialNucleus.getLength()];
		double[] yPoints = new double[initialNucleus.getLength()];
		double[] aPoints = new double[initialNucleus.getLength()]; // angles
		double[] pPoints = new double[initialNucleus.getLength()]; // positions along array

		for(int i=0; i<targetNucleus.getLength(); i++){
			XYPoint p = targetNucleus.getBorderPoint(i);
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
	}


	/*
		Draw the current state of the target nucleus
	*/
	private void plotTargetNucleus(){

		double[] xPoints = new double[targetNucleus.getLength()+1];
		double[] yPoints = new double[targetNucleus.getLength()+1];

		for(int i=0; i<targetNucleus.getLength(); i++){
			XYPoint p = targetNucleus.getBorderPoint(i);
			xPoints[i] = p.getX();
			yPoints[i] = p.getY();
		}

	  // ensure nucleus outline joins up at tip
	  XYPoint p = targetNucleus.getBorderPoint(0);
	  xPoints[targetNucleus.getLength()] = p.getX();
	  yPoints[targetNucleus.getLength()] = p.getY();

		nucleusPlot.setColor(Color.DARK_GRAY);
		nucleusPlot.addPoints(xPoints, yPoints, Plot.LINE);
		// nucleusPlotWindow.drawPlot(nucleusPlot);
	}

	/*
		Go over the target nucleus, adjusting each point.
		Keep the change if it helps get closer to the target profile

		Changes to make:
			Random mutation to the distance of a point from the CoM
			Random mutation to the angle of a point from the CoM 
			Together these affect the XY position of the point
	*/
	private double iterateOverNucleus(){

		double similarityScore = compareProfiles(targetCurve, targetNucleus.getInteriorAngles());

		double medianDistanceBetweenPoints = this.initialNucleus.getAngleProfile().getMedianDistanceBetweenPoints();
		
		for(int i=0; i<targetNucleus.getLength(); i++){

			NucleusBorderPoint p = targetNucleus.getBorderPoint(i);
			
			double currentDistance = p.getLengthTo(new XYPoint(0,0));
			double newDistance = currentDistance; // default no change
			double newAngle = p.getInteriorAngle();

			double oldX = p.getX();
			double oldY = p.getY();

			// make change dependent on score
			double amountToChange = Math.min( Math.random() * (similarityScore/1000), medianDistanceBetweenPoints); // when score is 1000, change by up to 1. When score is 300, change byup to 0.33

			if(p.getInteriorAngle() > targetCurve[i]){
						newDistance = currentDistance + amountToChange;
						// newAngle = p.getInteriorAngle() + (1-Math.random()); 
			}
			if(p.getInteriorAngle() < targetCurve[i]){
						newDistance = currentDistance - amountToChange; //  some change between 0 and 2
						// newAngle = p.getInteriorAngle() + (1-Math.random()); 
			}

			// find the angle the point makes to the x axis
			double angle = Nucleus.findAngleBetweenXYPoints(p, new XYPoint(0,0), new XYPoint(10, 0)); // point, 10,0, p,0
			if(oldY<0){
				angle = 360-angle;
			}
			double newX = NuclearOrganisationUtility.getXComponentOfAngle(newDistance, angle);
			double newY = NuclearOrganisationUtility.getYComponentOfAngle(newDistance, angle);

			p.setX(newX); // the new x position
			p.setY(newY); // the new y position

			// ensure the interior angle calculation works with the current points
			targetNucleus.setPolygon(createPolygon()); 

			// measure the new profile & compare
			targetNucleus.getAngleProfile().updateAngleCalculations();
			double[] newProfile = targetNucleus.getInteriorAngles();
			double score = compareProfiles(targetCurve, newProfile);

			// do not apply change  if the distance from teh surrounding points changes too much
			double distanceToPrev = p.getLengthTo( targetNucleus.getBorderPoint( NuclearOrganisationUtility.wrapIndex(i-1, targetNucleus.getLength()) ) );
			double distanceToNext = p.getLengthTo( targetNucleus.getBorderPoint( NuclearOrganisationUtility.wrapIndex(i+1, targetNucleus.getLength()) ) );

			// reset if worse fit or distances are too high
			if(score > similarityScore  || distanceToNext > medianDistanceBetweenPoints*1.2 || distanceToPrev > medianDistanceBetweenPoints*1.2 ){
				p.setX(oldX);
				p.setY(oldY);
				targetNucleus.getAngleProfile().updateAngleCalculations();
				targetNucleus.setPolygon(createPolygon());
			} else {
				similarityScore = score;
			}
		}
		return similarityScore;
	}

	private FloatPolygon createPolygon(){
		FloatPolygon offsetPolygon = new FloatPolygon();

		for(int i=0; i<targetNucleus.getLength(); i++){

			NucleusBorderPoint p = targetNucleus.getBorderPoint(i);
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
	  a given point at the bottom. Assumes CoM is at 0,0.
	  Find the angle of rotation needed to put the point atthe bottom
	*/
	private double findRotationAngle(NucleusBorderPoint bottomPoint){
		// find the angle to rotate
		double angleToRotate = 0;
		double distanceFromZero = 180;

		// get the angle from the tail to the vertical axis line
		double tailAngle = Nucleus.findAngleBetweenXYPoints( bottomPoint, targetNucleus.getCentreOfMass(), new XYPoint(0,-10));
			if(bottomPoint.getX()<0){
				tailAngle = 360-tailAngle; // correct for measuring the smallest angle
			}

		for(int i=0;i<360;i++){

			// get a copy of the bottom point
			XYPoint p = new XYPoint( bottomPoint.getX(), bottomPoint.getY() );
			
			// get the distance from tail to CoM
			double distance = p.getLengthTo(targetNucleus.getCentreOfMass());

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

		for(int i=0;i<targetNucleus.getLength();i++){

			XYPoint p = targetNucleus.getBorderPoint(i);
			double distance = p.getLengthTo(targetNucleus.getCentreOfMass());
			double oldAngle = Nucleus.findAngleBetweenXYPoints( p, targetNucleus.getCentreOfMass(), new XYPoint(0,-10));
			if(p.getX()<0){
				oldAngle = 360-oldAngle;
			}

			double newAngle = oldAngle + angleToRotate;
			double newX = NuclearOrganisationUtility.getXComponentOfAngle(distance, newAngle);
			double newY = NuclearOrganisationUtility.getYComponentOfAngle(distance, newAngle);

				p.setX(newX); // the new x position
				p.setY(newY); // the new y position
		}
		plotTargetNucleus();
	}

	

	/*
	  Using a list of signal locations, draw on
	  the consensus plot.
	*/
	public void addSignalsToConsensus(NucleusCollection collection){

		for(int i= 0; i<collection.getNuclei().size();i++){ // for each roi

			Nucleus n = collection.getNuclei().get(i);

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
					double fractionalDistance = signalGroup.get(j).getFractionalDistanceFromCoM();

					// determine the total distance to the border at this angle
					double distanceToBorder = getDistanceFromAngle(angle);

					// convert to fractional distance to signal
					double signalDistance = distanceToBorder * fractionalDistance;
				  
				  // adjust X and Y because we are now counting angles from the vertical axis
					double signalX = NuclearOrganisationUtility.getXComponentOfAngle(signalDistance, angle-90);
					double signalY = NuclearOrganisationUtility.getYComponentOfAngle(signalDistance, angle-90);

				  // add to array
				  xPoints.add( signalX );
				  yPoints.add( signalY ); 
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
	}

	private double getDistanceFromAngle(double angle){

		// go through the nucleus outline
		// measure the angle to the tail and the distance to the CoM
		// if closest to target angle, return distance
		double bestAngle = 180;
		double bestDiff = 180;
		double bestDistance = 180;

		for(int i=0;i<targetNucleus.getLength();i++){
			XYPoint p = targetNucleus.getBorderPoint(i);
			double distance = p.getLengthTo(targetNucleus.getCentreOfMass());
			double pAngle = Nucleus.findAngleBetweenXYPoints( p, targetNucleus.getCentreOfMass(), new XYPoint(0,-10));
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

	/*
		-----------------------
		Export data
		-----------------------
	*/

	public void exportProfileOfRefoldedImage(NucleusCollection collection){
		// targetNucleus.setPath(targetNucleus.getDirectory()+File.separator+"logConsensusNucleus."+collection.getType()+".txt");
	  // IJ.log("Exporting to: "+targetNucleus.getPath());
	  // targetNucleus.printLogFile(targetNucleus.getPath());
	 
	  File f = new File(targetNucleus.getDirectory()+File.separator+"logConsensusNucleus."+collection.getType()+".txt");
	if(f.exists()){
	  f.delete();
	}

	String outLine =  "X_INT\t"+
					  "Y_INT\t"+
					  "X_DOUBLE\t"+
					  "Y_DOUBLE\t"+
					  "INTERIOR_ANGLE\t"+
					  "NORMALISED_PROFILE_X\t"+
					  "DISTANCE_PROFILE\n";

	for(int i=0;i<targetNucleus.getLength();i++){

	  double normalisedX = ((double)i/(double)targetNucleus.getLength())*100; // normalise to 100 length
	  
	  outLine +=  targetNucleus.getBorderPoint(i).getXAsInt()             +"\t"+
				  targetNucleus.getBorderPoint(i).getYAsInt()             +"\t"+
				  targetNucleus.getBorderPoint(i).getX()                  +"\t"+
				  targetNucleus.getBorderPoint(i).getY()                  +"\t"+
				  targetNucleus.getBorderPoint(i).getInteriorAngle()      +"\t"+
				  normalisedX                                             +"\t"+
				  targetNucleus.getBorderPoint(i).getDistanceAcrossCoM()  +"\n";
	}
	IJ.append( outLine, f.getAbsolutePath());
	}

	public void exportImage(NucleusCollection collection){
		ImagePlus plot = nucleusPlot.getImagePlus();
	  IJ.saveAsTiff(plot, targetNucleus.getDirectory()+File.separator+"plotConsensus."+collection.getType()+".tiff");
	}

}