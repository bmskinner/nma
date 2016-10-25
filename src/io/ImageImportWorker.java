package io;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.ImageIcon;
import javax.swing.SwingWorker;
import javax.swing.table.TableModel;

import charting.charts.AbstractChartFactory;
import analysis.AnalysisDataset;
import components.AbstractCellularComponent;
import components.Cell;
import components.CellularComponent;
import components.generic.BorderTagObject;
import components.generic.ProfileType;
import components.generic.XYPoint;
import components.nuclear.BorderPoint;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;
import gui.components.ColourSelecter;
import gui.dialogs.CellCollectionOverviewDialog;
import gui.tabs.cells.LabelInfo;
import ij.gui.PolygonRoi;
import ij.process.Blitter;
import ij.process.ColorProcessor;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import logging.Loggable;
import utility.Constants;

/**
 * Handles the import of all images within a given AnalysisDataset
 * and sizing for display
 * @author ben
 *
 */
public class ImageImportWorker extends SwingWorker<Boolean, LabelInfo> implements Loggable{
	
	
	private final AnalysisDataset dataset;
	private final TableModel model;
	private final static int COLUMN_COUNT = CellCollectionOverviewDialog.COLUMN_COUNT;
	private int loaded = 0;
	private boolean rotate;
	
	public ImageImportWorker(AnalysisDataset dataset, TableModel model, boolean rotate) {
		super();
		this.dataset = dataset;
		this.model = model;
		this.rotate = rotate;
	}

	@Override
	protected Boolean doInBackground() throws Exception {
		
		for(Cell c : dataset.getCollection().getCells()){
			
			try {

				ImageIcon ic = importCellImage(c);
				
				LabelInfo inf = new LabelInfo(ic, c);

				publish(inf);
			} catch(Exception e){
				error("Error opening cell image", e);
			}
			
		}
		
		return true;
	}
	
	@Override
    public void done() {
    	
    	finest("Worker completed task");

    	 try {
            if(this.get()){
            	finest("Firing trigger for sucessful task");
                firePropertyChange("Finished", getProgress(), Constants.Progress.FINISHED.code());            

            } else {
            	finest("Firing trigger for failed task");
                firePropertyChange("Error", getProgress(), Constants.Progress.ERROR.code());
            }
        } catch (InterruptedException e) {
        	error("Interruption error in worker", e);
        	firePropertyChange("Error", getProgress(), Constants.Progress.ERROR.code());
        } catch (ExecutionException e) {
        	error("Execution error in worker", e);
        	firePropertyChange("Error", getProgress(), Constants.Progress.ERROR.code());
       }

    } 
	
	private ImageIcon importCellImage(Cell c){
		Nucleus n = c.getNucleus();
		ImageProcessor ip;
		try {
			ip = n.getComponentImage();
		} catch (UnloadableImageException e) {
			fine("Cannot load image for component");
			return new ImageIcon();
		}
		drawNucleus(c, ip);

		if(rotate){
			ip = rotateToVertical(c, ip);
			ip.flipVertical(); // Y axis needs inverting
		}
		// Rescale the resulting image	
		ip = scaleImage(ip);
			
		ImageIcon ic = new ImageIcon(ip.getBufferedImage());
		return ic;
	}
	
	private ImageProcessor rotateToVertical(Cell c, ImageProcessor ip){
		// Calculate angle for vertical rotation
		Nucleus n = c.getNucleus();
		
		XYPoint topPoint;
		XYPoint btmPoint;
		
		if( ! n.hasBorderTag(BorderTagObject.TOP_VERTICAL) || ! n.hasBorderTag(BorderTagObject.BOTTOM_VERTICAL)){
			topPoint = n.getCentreOfMass();
			btmPoint = n.getBorderPoint(BorderTagObject.ORIENTATION_POINT);
			
		} else {
			
			topPoint = n.getBorderPoint(BorderTagObject.TOP_VERTICAL);
			btmPoint = n.getBorderPoint(BorderTagObject.BOTTOM_VERTICAL);
			
			// Sometimes the points have been set to overlap in older datasets
			if(topPoint.overlapsPerfectly(btmPoint)){
				topPoint = n.getCentreOfMass();
				btmPoint = n.getBorderPoint(BorderTagObject.ORIENTATION_POINT);
			}
		}
		
		

		// Find which point is higher in the image
		XYPoint upperPoint = topPoint.getY()>btmPoint.getY()? topPoint : btmPoint;
		XYPoint lowerPoint = upperPoint==topPoint ? btmPoint : topPoint;

		XYPoint comp = new XYPoint(lowerPoint.getX(),upperPoint.getY());

		/*
		 *      LA             RA        RB         LB         
		 * 
		 *      T  C          C  T      B  C       C  B
		 *       \ |          | /        \ |       | /
		 *         B          B            T       T
		 * 
		 * When Ux<Lx, angle describes the clockwise rotation around L needed to move U above it.
		 * When Ux>Lx, angle describes the anticlockwise rotation needed to move U above it.
		 * 
		 * If L is supposed to be on top, the clockwise rotation must be 180+a
		 * 
		 * However, the image coordinates have a reversed Y axis
		 */
		
		double angleFromVertical = lowerPoint.findAngle( upperPoint, comp);

		double angle = 0;
		if(topPoint.isLeftOf(btmPoint) && topPoint.isAbove(btmPoint)){		
			angle = 360-angleFromVertical;
			//					log("LA: "+angleFromVertical+" to "+angle); // Tested working
		}

		if(topPoint.isRightOf(btmPoint) && topPoint.isAbove(btmPoint)){
			angle = angleFromVertical;
			//					log("RA: "+angleFromVertical+" to "+angle); // Tested working
		}

		if(topPoint.isLeftOf(btmPoint) && topPoint.isBelow(btmPoint)){
			angle = angleFromVertical+180;
			//					angle = 180-angleFromVertical;
			//					log("LB: "+angleFromVertical+" to "+angle); // Tested working
		}

		if(topPoint.isRightOf(btmPoint) && topPoint.isBelow(btmPoint)){
			//					angle = angleFromVertical+180;
			angle = 180-angleFromVertical;
			//					log("RB: "+angleFromVertical+" to "+angle); // Tested working
		}

		// Increase the canvas size so rotation does not crop the nucleus
		finer("Input: "+n.getNameAndNumber()+" - "+ip.getWidth()+" x "+ip.getHeight());
		ImageProcessor newIp = createEnlargedProcessor(ip, angle);

		newIp.rotate(angle);
		return newIp;
	}
	
	private ImageProcessor createEnlargedProcessor(ImageProcessor ip, double degrees){
		

		double rad = Math.toRadians(degrees);
		
		// Calculate the new width and height of the canvas
		// new width is h sin(a) + w cos(a) and vice versa for height
		double newWidth  = Math.abs(   Math.sin(rad) * ip.getHeight() )  +   Math.abs(   Math.cos(rad)* ip.getWidth()     );
		double newHeight = Math.abs(   Math.sin(rad) * ip.getWidth()  )  +   Math.abs(   Math.cos(rad)* ip.getHeight()    );
		
		int w = (int) Math.ceil(newWidth);
		int h = (int) Math.ceil(newHeight);
		
		
		// The new image may be narrower or shorter following rotation.
		// To avoid clipping, ensure the image never gets smaller in either dimension.
		w = w < ip.getWidth()  ? ip.getWidth()  : w;
		h = h < ip.getHeight() ? ip.getHeight() : h;
		
		// paste old image to centre of enlarged canvas
		int xBase = (w-ip.getWidth())  >> 1; 
		int yBase = (h-ip.getHeight()) >> 1;
		
		finer("New image "+w+" x "+h+" from "+ip.getWidth()+" x "+ip.getHeight()+" : Rot: "+degrees);
		
		finest("Copy starting at "+xBase+", "+yBase);
		
		ImageProcessor newIp= new ColorProcessor(w,h);

		newIp.setColor(Color.WHITE); // fill current space with white
		newIp.fill();
		
		newIp.setBackgroundValue(16777215); // fill on rotate is RGB int white 
		newIp.copyBits(ip, xBase, yBase, Blitter.COPY);
		return newIp;
	}
	
	private ImageProcessor scaleImage(ImageProcessor ip){
		double aspect =  (double) ip.getWidth() / (double) ip.getHeight();
		double finalWidth = 150 * aspect; // fix height
		finalWidth = finalWidth > 150 ? 150 : finalWidth; // but constrain width too
		
		ip = ip.resize( (int) finalWidth); 
		return ip;
	}
	
	/**
	 * Draw the outline of a nucleus on the given processor
	 * @param cell
	 * @param ip
	 */
	private void drawNucleus(Cell cell, ImageProcessor ip) {
		if(cell==null){
			throw new IllegalArgumentException("Input cell is null");
		}
		
		Nucleus n = cell.getNucleus();
//		double[] positions = n.getPosition();

		
		// annotate the image processor with the nucleus outline
		List<NucleusBorderSegment> segmentList = n.getProfile(ProfileType.ANGLE).getSegments();
		
		ip.setLineWidth(2);
		if(!segmentList.isEmpty()){ // only draw if there are segments
			
			for(NucleusBorderSegment seg  : segmentList){
				
				float[] x = new float[seg.length()+1];
				float[] y = new float[seg.length()+1];
				
				
				for(int j=0; j<=seg.length();j++){
					int k = AbstractCellularComponent.wrapIndex(seg.getStartIndex()+j, n.getBorderLength());
					BorderPoint p = n.getBorderPoint(k); // get the border points in the segment
					x[j] = (float) p.getX();
					y[j] = (float) p.getY();
				}
				
				int segIndex = AbstractChartFactory.getIndexFromLabel (seg.getName());
				ip.setColor(ColourSelecter.getColor(segIndex));
				
				PolygonRoi segRoi = new PolygonRoi(x, y, PolygonRoi.POLYLINE);
				
				segRoi.setLocation(segRoi.getBounds().getMinX()+CellularComponent.COMPONENT_BUFFER, segRoi.getBounds().getMinY()+CellularComponent.COMPONENT_BUFFER);
				
				ip.draw(segRoi);

			}
		} else {

			ip.setColor(Color.ORANGE);
			FloatPolygon polygon = n.createPolygon();
			PolygonRoi roi = new PolygonRoi(polygon, PolygonRoi.POLYGON);
			roi.setLocation(CellularComponent.COMPONENT_BUFFER, CellularComponent.COMPONENT_BUFFER);
			ip.draw(roi);
		}

	}
	
	@Override
    protected void process( List<LabelInfo> chunks ) {
        
        for(LabelInfo im : chunks){
        	
        	int row = loaded / COLUMN_COUNT;
            int col = loaded % COLUMN_COUNT;
//            log("Image: "+loaded+" - Row "+row+" col "+col);
            
            
            
    		model.setValueAt(im, row, col);
    		
    		
    		loaded++;
        }
                
        int percent = (int) ( (double) loaded / (double) dataset.getCollection().size() * 100);
        
        if(percent >= 0 && percent <=100){
        	setProgress(percent); // the integer representation of the percent
        }
    }
	

	

}
