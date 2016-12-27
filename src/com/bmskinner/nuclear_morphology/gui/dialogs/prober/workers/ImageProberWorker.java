package com.bmskinner.nuclear_morphology.gui.dialogs.prober.workers;

import java.awt.Dimension;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.ImageIcon;
import javax.swing.SwingWorker;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.analysis.detection.IconCell;
import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.ImageProberTableCell;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.ImageType;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.utility.Constants;

import ij.process.ImageProcessor;

public abstract class ImageProberWorker extends SwingWorker<Boolean, ImageProberTableCell> implements Loggable{
	
	protected File file;
	protected IAnalysisOptions options;
	
	protected TableModel model; // model for a table with an IconCellRenderer
	
	private int columnCount; // the number of columns the table has;
	
	private int progress = 0; // the number of images processed
	
	protected ImageType type; // the type of analysis images to be generated
	
	protected Dimension smallDimension; // the max size of the small icons
	
	public ImageProberWorker(File f, IAnalysisOptions options, ImageType type, TableModel model){
		this.file = f;
		this.options = options;
		this.columnCount = model.getColumnCount();
		this.model = model;
		this.type = type;
	}
	
	public void setSmallIconSize(Dimension d){
		this.smallDimension = d;
	}
	
	@Override
	protected Boolean doInBackground() throws Exception {
		try {
			analyseImages();
		} catch(Exception e){
			error("Error in signal image probing", e);
			return false;
		}

		return true;
	}
	
	/**
	 * Carry out the analysis
	 */
	protected abstract void analyseImages() throws Exception;
	
	@Override
    protected void process( List<ImageProberTableCell> chunks ) {
        
//		log("Processing "+chunks.size()+" chunks");
		
		progress+= chunks.size();
       
		for(ImageProberTableCell im : chunks){
        	
        	int pos = im.getType().getPosition();
        	
        	int col = pos % columnCount;
        	
        	int row = pos / columnCount;

//        	log("Processing chunk: Type is "+im.toString());
            
    		model.setValueAt(im, row, col);
        }
				
        int percent = (int) ( (double) progress / (double) type.getValues().length * 100);
//        log("Firing percent is: "+percent);
        if(percent >= 0 && percent <=100){
        	setProgress(percent); // the integer representation of the percent
        }
    }
	
	protected ImageProberTableCell makeIconCell(ImageProcessor ip, ImageType type){
		
		ImageFilterer filt = new ImageFilterer(ip);
		ImageIcon ic = filt.fitToScreen().toImageIcon();
		ImageProberTableCell iconCell = new ImageProberTableCell(ic, type);
		
		ImageIcon small = filt.resize( (int) smallDimension.getWidth(), (int) smallDimension.getHeight())
				.toImageIcon();
						
		iconCell.setSmallIcon(small );
		return iconCell;
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
       } catch(Error e){
    	   
    	   error("Something went really wrong in the image prober", e);
    	   firePropertyChange("Error", getProgress(), Constants.Progress.ERROR.code());
       }

    } 

}
