package com.bmskinner.nuclear_morphology.gui.dialogs.prober.workers;

import java.awt.Dimension;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.ImageIcon;
import javax.swing.SwingWorker;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.ImageProberTableCell;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.ImageSet;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.ImageType;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import ij.process.ImageProcessor;

/**
 * The templae for image prober workers. These calculate the steps in object
 * detection, and display them in a TableModel.
 * @author ben
 * @since 1.13.4
 *
 */
public abstract class ImageProberWorker extends SwingWorker<Boolean, ImageProberTableCell> implements Loggable{
	
	protected File file;
	protected IDetectionOptions options;
	
	protected TableModel model; // model for a table with an IconCellRenderer
	
	private int columnCount; // the number of columns the table has;
	
	private int progress = 0; // the number of images processed
		
	protected ImageSet imageSet; // the set of images to be created
	
	protected Dimension smallDimension; // the max size of the small icons
		
	public ImageProberWorker(final File f, final IDetectionOptions options, final ImageSet type, final TableModel model){
		this.file = f;
		this.options = options;
		this.columnCount = model.getColumnCount();
		this.model = model;
		this.imageSet = type;
	}
	
	public void setSmallIconSize(Dimension d){
		this.smallDimension = d;
	}
	
	@Override
	protected Boolean doInBackground() throws Exception {
		try {
			analyseImages();
		} catch(Exception e){
			warn("Error in image probing");
			stack("Error in signal image probing", e);
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
        		
		progress+= chunks.size();
       
		for(ImageProberTableCell im : chunks){
        	
        	int pos = im.getPosition();
        	
        	int col = pos % columnCount;
        	
        	int row = pos / columnCount;
            
    		model.setValueAt(im, row, col);
        }
				
        int percent = (int) ( (double) progress / (double) imageSet.size() * 100);
//        log("Firing percent is: "+percent);
        if(percent >= 0 && percent <=100){
        	setProgress(percent); // the integer representation of the percent
        }
    }
	
	/**
	 * Create a table cell from the given image, specifying the image type and enabled
	 * @param ip
	 * @param enabled
	 * @param type
	 * @return
	 */
	protected ImageProberTableCell makeIconCell(ImageProcessor ip, boolean enabled, ImageType type){
		
		ImageFilterer filt = new ImageFilterer(ip);
//		ImageIcon ic = filt.fitToScreen().toImageIcon(); // This causes problems when drawing overlay nuclei based on original image size
		ImageIcon ic = filt.toImageIcon();
		ImageProberTableCell iconCell = new ImageProberTableCell( ic, type, enabled, imageSet.getPosition(type));
		
		ImageIcon small = filt.resize( (int) smallDimension.getWidth(), (int) smallDimension.getHeight())
				.toImageIcon();
						
		iconCell.setSmallIcon( small );
		return iconCell;
	}
	
	
	@Override
    public void done() {
    	
    	finest("Worker completed task");

    	 try {
            if(this.get()){
            	finest("Firing trigger for sucessful task");
                firePropertyChange("Finished", getProgress(), IAnalysisWorker.FINISHED);            

            } else {
            	finest("Firing trigger for failed task");
                firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
            }
        } catch (InterruptedException e) {
        	warn("Interruption to worker: "+e.getMessage());
        	stack("Interruption error in worker", e);
        	firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
        } catch (ExecutionException e) {
        	warn("Execution error in worker: "+e.getMessage());
        	stack("Execution error in worker", e);
        	firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
       } catch(Error e){
    	   warn("Unexpected error in worker: "+e.getMessage());
    	   stack(e.getMessage(), e);
    	   firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
       }

    } 

}
