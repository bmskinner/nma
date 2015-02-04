import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.DirectoryChooser;
import ij.io.Opener;
import ij.io.OpenDialog;
import ij.io.RandomAccessStream;
import ij.measure.ResultsTable;
import ij.measure.SplineFitter;
import ij.plugin.ChannelSplitter;
import ij.plugin.PlugIn;
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
import java.awt.Polygon;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.*;

public class Curve_Refolder
  extends ImagePlus
  implements PlugIn
{

	// direct copy from sperm analysis - J:\Images\2015-01-20 Sperm WT Rep 2 Orly\. Window size 23
	private static final double[] normalisedMedian = { 64.46088056, 65.67315419, 68.05068779, 72.02194198, 76.65159665, 81.22177907, 86.38662247, 92.18316184, 97.81915879, 105.6787042, 113.669713, 121.3615904, 130.0065953, 139.0295942, 147.5205515, 154.4769017, 161.6613919, 167.4587598, 173.0845645, 178.9422118, 181.931016, 184.0510983, 186.7409952, 188.2976944, 189.6755332, 190.894908, 191.668603, 191.9310335, 190.3321869, 189.3566922, 188.4257547, 186.6413838, 185.2009166, 183.6961036, 181.9945775, 180.2672969, 178.4063472, 176.3740562, 174.3716642, 172.1444771, 169.9000081, 167.3742128, 164.7406591, 163.1634597, 160.6376781, 158.5538233, 156.7084676, 154.8990437, 153.3914834, 152.0143961, 150.3180747, 149.0774399, 147.5033487, 146.3961081, 145.0596834, 143.8567091, 142.3117436, 141.0350867, 139.8664762, 138.1745365, 136.2147766, 135.3400257, 133.9036841, 132.6637229, 131.6061872, 130.0306309, 129.3333795, 128.7495132, 128.1170202, 127.7890886, 127.2667816, 126.2527704, 125.7562909, 123.9434051, 123.0996397, 121.6435263, 120.0821357, 117.7716508, 116.4064533, 114.7067365, 113.5132842, 111.9347533, 110.5604726, 109.6685311, 109.0618532, 108.5704256, 107.8854802, 107.7589289, 108.1292706, 108.5708565, 108.7994193, 108.6965389, 108.9374317, 108.4275348, 109.0631451, 108.9425363, 109.6926176, 110.3717527, 111.1203639, 112.9438281, 113.9777717, 113.5554734, 114.8735152, 116.6787323, 117.8978219, 119.1481332, 120.2887959, 121.6002875, 122.5124478, 124.1823097, 125.2728711, 126.5705012, 127.5806768, 128.6456253, 129.5895798, 130.4629016, 131.4414098, 132.1205527, 132.9117397, 133.6518854, 134.696968, 135.851638, 136.9794387, 137.8610003, 138.7806523, 139.7300728, 140.7603942, 141.7294101, 142.2813272, 143.1653258, 144.2759908, 145.2013095, 145.6643536, 146.5866879, 146.7469399, 147.5754776, 148.0122666, 148.2763765, 148.8304689, 149.1309015, 149.5946889, 149.8625063, 150.2675561, 150.6077154, 150.9907463, 151.3041299, 151.7704546, 151.8451775, 152.1444516, 152.4145882, 152.5269741, 152.5446695, 152.8612893, 152.8993201, 152.7715257, 152.6144594, 152.8965996, 152.679217, 152.6166162, 153.1344745, 153.1678267, 152.8743555, 152.5246895, 152.6123206, 152.4438909, 152.2147374, 152.040507, 151.9394693, 151.8203693, 151.5925648, 151.376504, 151.3442411, 150.8107022, 150.0207697, 149.7315394, 148.8316277, 147.2553505, 145.3280081, 143.677565, 141.3492767, 138.5886859, 135.2134855, 131.8540679, 128.2036237, 123.8127131, 117.9172057, 111.9897667, 106.0883575, 99.56274882, 94.09200714, 87.89874063, 83.03045494, 78.59278525, 74.29778288, 71.34408842, 68.9474264, 66.78672999, 65.12711991, 64.11002971, 63.74447546 }; 
	private static final int windowSize = 23;

	public void run(String paramString)  {

		/*
		the goal is to take the array of angles above, and refold them into a sperm head shape.
		 Approach:
			Take an oval / circle as a polygon with the same number of points as the array
			Measure the angles with the given window size at each point around the perimeter
			If the angle is too high, move the point outwards from CoM by <A NUMBER>
			If the angle is too low, move the point inwards to CoM by <A_NUMBER>
			Otherwise, move on to the next point.

			Carry out successive iterations until all angles are satisfied?
			What does it look like?
			Constraints on distance between positions?
		*/

		Plot circlePlot = new Plot( "Circle",
                                  "X",
                                  "Y");;
    	
	    circlePlot.setLimits(-320,320,-320,320);
	    circlePlot.setSize(300,300);
	    circlePlot.setYTicks(true);

	    Plot anglePlot = new Plot( "Angles",
                                  "Position",
                                  "Angle");;

	    anglePlot.setLimits(0,normalisedMedian.length,-50,360);
	    anglePlot.setSize(300,300);
	    anglePlot.setYTicks(true);



    	ArrayList<XYPoint> circleArray = createCircle(200, 100);

    	double[] xpoints = new double[circleArray.size()];
    	double[] ypoints = new double[circleArray.size()];

    	double[] positionpoints = new double[circleArray.size()];
    	double[] anglepoints = new double[circleArray.size()];
    	double[] targetpoints = new double[circleArray.size()];

    	for(int i=0; i<circleArray.size(); i++){
			xpoints[i] = circleArray.get(i).getX();
			ypoints[i] = circleArray.get(i).getY();

			// do the angle measurements for the first time
			double angle = measureAngleBetweenPoints( circleArray.get(wrapIndex(i-windowSize, circleArray.size())), circleArray.get(i), circleArray.get(wrapIndex(i+windowSize, circleArray.size())));
			circleArray.get(i).setAngle(angle);
			circleArray.get(i).setTargetAngle(normalisedMedian[i]);
			circleArray.get(i).setCreatedAngle( ((double) i / (double) 200) * 360 );

			positionpoints[i] = i;
			anglepoints[i] = circleArray.get(i).getAngle();
			targetpoints[i] = circleArray.get(i).getTargetAngle();

		}

		circlePlot.addPoints(xpoints, ypoints, Plot.DOT);
		anglePlot.addPoints(positionpoints, anglepoints, Plot.DOT);
		anglePlot.addPoints(positionpoints, targetpoints, Plot.LINE);
    	PlotWindow circlePlotWindow = circlePlot.show();
    	PlotWindow anglePlotWindow = anglePlot.show();

    	// iterate over the circle;
    	XYPoint centreOfMass = new XYPoint(0,0);

    	int cycles = 500;
    	for(int j = 0; j<cycles; j++){

    		double[] newXpoints = new double[circleArray.size()];
    		double[] newYpoints = new double[circleArray.size()];
    		double[] newAngle = new double[circleArray.size()];

	    	for(int i=0; i<circleArray.size(); i++){

	    		XYPoint p = circleArray.get(i);
	    		


	    		double currentDistance = p.getLengthTo(centreOfMass);
	    		double newDistance = currentDistance; // default no change
	    		double rand = Math.random();

	    		if(p.getAngle() > p.getTargetAngle()){

	    			// increase distance from centre (0,0)
	    			if(currentDistance < 200){
	    				if(rand < 0.2)
	    					newDistance = currentDistance + 0.1; // 1% change
	    				else
	    					newDistance = currentDistance + 0.5; // 1% change


	    				// newDistance = currentDistance + 0.1; // 1% change
	    			} else {
	    				// if(rand < 0.2)
	    				// 	p.setCreatedAngle( p.getCreatedAngle()+1);
	    				// else
	    				// 	p.setCreatedAngle( p.getCreatedAngle()-1);
	    			}
	    			
	    		}
	    		if(p.getAngle() < p.getTargetAngle()){

	    			if(currentDistance > 100){
	    				if(rand < 0.2)
	    					newDistance = currentDistance - 0.1; // 1% change
	    				else
	    					newDistance = currentDistance - 0.5; // 1% change
	    				// newDistance = currentDistance - 0.1; // 1% change
	    			} else {
	    				// if(rand > 0.2)
	    				// 	p.setCreatedAngle( p.getCreatedAngle()+1);
	    				// else
	    				// 	p.setCreatedAngle( p.getCreatedAngle()-1);
	    			}

	    		}

	    		double x = getXComponentOfAngle(newDistance, p.getCreatedAngle());
				double y = getYComponentOfAngle(newDistance, p.getCreatedAngle());
				p.setX(x); // the new x position
				p.setY(y); // the new y position
				double angle = measureAngleBetweenPoints( 	circleArray.get(wrapIndex(i-windowSize, circleArray.size())), 
															p, 
															circleArray.get(wrapIndex(i+windowSize, circleArray.size())));
				p.setAngle(angle);
				newXpoints[i] = p.getX();
				newYpoints[i] = p.getY();
				newAngle[i] = p.getAngle();



			}
			double difference = getDifferenceBetweenCurves(newAngle, targetpoints);
			IJ.log("Iteration "+j+": "+difference);

			// iterate until differences no longer changing much;
			// then swap to angle changes to pull the tip out

			if(j == cycles-1){
		    	circlePlot.addPoints(newXpoints, newYpoints, Plot.LINE);
				anglePlot.addPoints(positionpoints, newAngle, Plot.DOT);
				circlePlotWindow.drawPlot(circlePlot);
			    anglePlotWindow.drawPlot(anglePlot);
			}
		}


	}

	public ArrayList<XYPoint> createCircle(int numberOfPoints, int radius){

		double[] angles = new double[numberOfPoints];
		for(int i=0; i<angles.length; i++){
			angles[i] = ((double) i / (double) numberOfPoints) * 360;
		}

		ArrayList<XYPoint> circle = new ArrayList<XYPoint>(0);
		// circle.add( new XYPoint(radius, 0) ); // start off at x=radius, y=0

		for(int i=0; i<angles.length; i++){
			
			double x = getXComponentOfAngle(radius, angles[i]);
			double y = getYComponentOfAngle(radius, angles[i]);
			circle.add( new XYPoint(x, y) );
		}
		return circle;
	}

	public double[] findLineEquation(XYPoint a, XYPoint b){

      // y=mx+c
      double deltaX = a.getX() - b.getX();
      double deltaY = a.getY() - b.getY();
        
      double m = deltaY / deltaX;
        
      // y - y1 = m(x - x1)
      double c = a.getY() -  ( m * a.getX() );
        
      // double testY = (m * position_2[0]) + c;
        
      // write("y = "+m+"x + "+c);
      // result=newArray(m, c);
      return new double[] { m, c };
    }

    public double getXFromEquation(double[] eq, double y){
      // x = (y-c)/m
      double x = (y - eq[1]) / eq[0];
      return x;
    }

    public double getYFromEquation(double[] eq, double x){
      // x = (y-c)/m
      double y = (eq[0] * x) + eq[1];
      return y;
    }

    public double getXComponentOfAngle(double length, double angle){
    	// a^2 = b^2+c^2

    	// sin(angle) = y / h
    	// cos(angle) = x / h
    	// x = cos(a)*h
    	double x = length * Math.cos(Math.toRadians(angle));
    	return x;

    }

    public double getYComponentOfAngle(double length, double angle){
    	double y = length * Math.sin(Math.toRadians(angle));
    	return y;
    }

    public double measureAngleBetweenPoints(XYPoint a, XYPoint b, XYPoint c){

    	float[] xpoints = { (float) a.getX(), (float) b.getX(), (float) c.getX()};
	    float[] ypoints = { (float) a.getY(), (float) b.getY(), (float) c.getY()};
	    PolygonRoi roi = new PolygonRoi(xpoints, ypoints, 3, Roi.ANGLE);
	    return roi.getAngle();
    }

    public double getDifferenceBetweenCurves(double[] a, double[] b){
    	// compare the angles of the target curves
    	double difference = 0;

    	for(int i=0; i<a.length; i++){
    		difference += Math.abs(a[i] - b[i]);
    	}
    	return difference;
    }

    public int wrapIndex(int i, int length){
	    if(i<0)
	      i = length + i; // if i = -1, in a 200 length array,  will return 200-1 = 199
	    if(Math.floor(i / length)>0)
	      i = i - ( ((int)Math.floor(i / length) )*length);    
	    return i;
	}

    class XYPoint {
	    private double x;
	    private double y;
	    private double angle;
	    private double targetAngle;
	    private double createdAngle; // the angle from the centre of mass of the point
	  
	    public XYPoint (double x, double y){
	      this.x = x;
	      this.y = y;
	    }

	    public double getX(){
	      return this.x;
	    }
	    public double getY(){
	      return this.y;
	    }

	    public int getXAsInt(){
	      Double obj = new Double(this.x);
	      int i = obj.intValue();
	      return i;
	    }

	    public int getYAsInt(){
	      Double obj = new Double(this.y);
	      int i = obj.intValue();
	      return i;
	    }

	    public void setX(double x){
	      this.x = x;
	    }

	    public void setY(double y){
	      this.y = y;
	    }

	    public double getAngle(){
	      return this.angle;
	    }

	    public void setAngle(double a){
	      this.angle = a;
	    }

	    public double getTargetAngle(){
	    	return this.targetAngle;
	    }

	    public void setTargetAngle(double a){
	      this.targetAngle = a;
	    }

	    public double getCreatedAngle(){
	    	return this.createdAngle;
	    }

	    public void setCreatedAngle(double a){
	      this.createdAngle = a;
	    }

	    public double getLengthTo(XYPoint a){
	      // a2 = b2 + c2
	      double dx = Math.abs(this.getX() - a.getX());
	      double dy = Math.abs(this.getY() - a.getY());
	      double dx2 = dx * dx;
	      double dy2 = dy * dy;
	      double length = Math.sqrt(dx2+dy2);
	      return length;
	    }
	}


}