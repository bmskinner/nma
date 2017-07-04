package com.bmskinner.nuclear_morphology.gui.dialogs.collections;

import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTable;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.dialogs.LoadingIconDialog;
import com.bmskinner.nuclear_morphology.io.ImageImportWorker;

/**
 * Display collections of images from a dataset. Uses a SwingWorker
 * to import the images.
 * @author ben
 * @since 1.13.7
 *
 */
public abstract class CollectionOverviewDialog extends LoadingIconDialog implements PropertyChangeListener {

    public static final int COLUMN_COUNT = 3;
    
    protected static final String LOADING_LBL = "Loading";

    protected IAnalysisDataset dataset;
    protected JTable           table;
    protected JProgressBar     progressBar;
    protected ImageImportWorker worker;

    /**
     * Construct with a dataset to display
     * @param dataset
     */
    public CollectionOverviewDialog(IAnalysisDataset dataset) {
        super();
        this.dataset = dataset;

        createUI();
        createWorker();

        this.setModal(false);
        this.pack();
        this.setVisible(true);
    }
    
    /**
     * Create the image import worker needed to import and
     * annotate images.
     */
    protected abstract void createWorker();
    
    /**
     * Create the UI of the dialog
     */
    protected abstract void createUI();
    
    protected abstract JPanel createHeader();

}
