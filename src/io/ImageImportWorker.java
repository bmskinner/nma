package io;

import java.awt.Color;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.SwingWorker;
import javax.swing.table.TableModel;

import charting.charts.AbstractChartFactory;
import analysis.AnalysisDataset;
import components.AbstractCellularComponent;
import components.Cell;
import components.CellularComponent;
import components.generic.ProfileType;
import components.nuclear.BorderPoint;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;
import gui.components.ColourSelecter;
import gui.dialogs.CellCollectionOverviewDialog;
import gui.tabs.cells.LabelInfo;
import ij.gui.PolygonRoi;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import logging.Loggable;

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
	
	public ImageImportWorker(AnalysisDataset dataset, TableModel model) {
		super();
		this.dataset = dataset;
		this.model = model;
		
	}

	@Override
	protected Boolean doInBackground() throws Exception {
		
		for(Cell c : dataset.getCollection().getCells()){
			
			try {

				ImageIcon ic = importCellImage(c);
				
				LabelInfo inf = new LabelInfo(ic, c.getNucleus().getNameAndNumber());

				publish(inf);
			} catch(Exception e){
				error("Error opening cell image", e);
			}
			
		}
		
		return true;
	}
	
	private ImageIcon importCellImage(Cell c){
		ImageProcessor ip = c.getNucleus().getComponentImage();
		drawNucleus(c, ip);
		
		
		double aspect =  (double) ip.getWidth() / (double) ip.getHeight();
		double newWidth = 150 * aspect; // fix height
		newWidth = newWidth > 150 ? 150 : newWidth; // but constrain width too
		
		ip = ip.resize( (int) newWidth); 
		
		ImageIcon ic = new ImageIcon(ip.getBufferedImage());
		return ic;
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
		double[] positions = n.getPosition();

		
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
                
        int percent = (int) ( (double) loaded / (double) dataset.getCollection().cellCount() * 100);
        
        if(percent >= 0 && percent <=100){
        	setProgress(percent); // the integer representation of the percent
        }
    }
	

	

}
