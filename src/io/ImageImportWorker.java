package io;

import java.awt.Image;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.SwingWorker;
import javax.swing.table.TableModel;

import analysis.AnalysisDataset;
import components.Cell;
import gui.dialogs.CellCollectionOverviewDialog;
import ij.process.ImageProcessor;
import logging.Loggable;

/**
 * Handles the import of all images within a given AnalysisDataset
 * and sizing for display
 * @author ben
 *
 */
public class ImageImportWorker extends SwingWorker<Boolean, ImageIcon> implements Loggable{
	
	
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

				publish(ic);
			} catch(Exception e){
				error("Error opening cell image", e);
			}
			
		}
		
		return true;
	}
	
	private ImageIcon importCellImage(Cell c){
		ImageProcessor ip = c.getNucleus().getComponentImage();
		
		ip = ip.resize(150); // fixed width for now
		
		ImageIcon ic = new ImageIcon(ip.getBufferedImage());
		return ic;
	}
	
	@Override
    protected void process( List<ImageIcon> chunks ) {
		
		
		
        int amount  = chunks.size();
        
        for(ImageIcon im : chunks){
        	
        	int row = loaded / COLUMN_COUNT;
            int col = loaded % COLUMN_COUNT;
            log("Image: "+loaded+" - Row "+row+" col "+col);
            
    		model.setValueAt(im, row, col);
    		loaded++;
        }
                
        int percent = (int) ( (double) loaded / (double) dataset.getCollection().cellCount() * 100);
        
        if(percent >= 0 && percent <=100){
        	setProgress(percent); // the integer representation of the percent
        }
    }
	
	

}
