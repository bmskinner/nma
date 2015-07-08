package components;

import ij.gui.Roi;
import ij.process.FloatPolygon;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import no.components.NucleusBorderPoint;
import no.components.XYPoint;

/**
 * There can be many types of flagellum; the type of interest mainly
 * is the sperm tail.
 * @author bms41
 *
 */
public class Flagellum {
	
	// indices in  the originalPositions array
	public static final int X_BASE 	= 0;
	public static final int Y_BASE 	= 1;
	public static final int WIDTH 	= 2;
	public static final int HEIGHT 	= 3;
	
	private static final long serialVersionUID = 1L;
	
	protected UUID uuid;
	
	protected File sourceFile;    // the image from which the tail came
	protected int sourceChannel; // the channel in the source image
	
	protected double length; // the length of the skeleton
	protected double[] orignalPosition; // the xbase, ybase, width and height of the original bounding rectangle
	
	protected List<XYPoint> skeletonPoints = new ArrayList<XYPoint>(0); 
	protected List<XYPoint> borderPoints   = new ArrayList<XYPoint>(0); 
	
	public Flagellum(File source, int channel, Roi skeleton, Roi border){
		this.uuid = java.util.UUID.randomUUID();
		this.sourceFile = source;
		this.sourceChannel = channel;
		
		this.orignalPosition = new double[] { border.getPolygon().getBounds().getMinX(),
				 border.getPolygon().getBounds().getMinY(),
				 border.getPolygon().getBounds().getWidth(),
				 border.getPolygon().getBounds().getHeight()};
		
		
		FloatPolygon polygon = skeleton.getFloatPolygon();
		for(int i=0; i<polygon.npoints; i++){
			skeletonPoints.add(new XYPoint( polygon.xpoints[i], polygon.ypoints[i]));
		}
		
		FloatPolygon borderPolygon = border.getFloatPolygon();
		for(int i=0; i<polygon.npoints; i++){
			borderPoints.add(new XYPoint( borderPolygon.xpoints[i], borderPolygon.ypoints[i]));
		}
		
		 
		
		this.length = skeleton.getLength();
		
		
	}
	
	public UUID getID(){
		return this.uuid;
	}
	
	public List<XYPoint> getSkeleton(){
		return this.skeletonPoints;
	}
	
	public List<XYPoint> getOffsetSkeleton(){
		List<XYPoint> result = new ArrayList<XYPoint>(0);
		for(XYPoint p : skeletonPoints){
			result.add(new XYPoint( p.getX() - orignalPosition[X_BASE], p.getY() - orignalPosition[Y_BASE]));
		}
		return result;
	}
	
	public List<XYPoint> getBorder(){
		return this.borderPoints;
	}
	
	// positions are offset by the bounding rectangle for easier plotting
	public List<XYPoint> getOffsetBorder(){
		List<XYPoint> result = new ArrayList<XYPoint>(0);
		for(XYPoint p : borderPoints){
			result.add(new XYPoint( p.getX() - orignalPosition[X_BASE], p.getY() - orignalPosition[Y_BASE]));
		}
		return result;
	}
	
	public double getLength(){
		return this.length;
	}
	
	public File getSourceFile(){
		return this.sourceFile;
	}
	
	public int getSourceChannel(){
		return this.sourceChannel;
	}
	
	public double[] getPosition(){
		return this.orignalPosition;
	}
	
	

}
