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
	
	private static final long serialVersionUID = 1L;
	
	protected UUID uuid;
	
	protected File sourceFile;    // the image from which the tail came
	protected int sourceChannel; // the channel in the source image
	
	protected double length; // the length of the skeleton
	
	protected List<XYPoint> skeletonPoints = new ArrayList<XYPoint>(0); 
	protected List<XYPoint> borderPoints   = new ArrayList<XYPoint>(0); 
	
	public Flagellum(File source, int channel, Roi skeleton, Roi border){
		this.uuid = java.util.UUID.randomUUID();
		this.sourceFile = source;
		this.sourceChannel = channel;
		
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
	
	public List<XYPoint> getBorder(){
		return this.borderPoints;
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
	
	

}
