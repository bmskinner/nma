package com.bmskinner.nuclear_morphology.gui.tabs.signals.warping;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.image.MultiScaleStructuralSimilarityIndex;
import com.bmskinner.nuclear_morphology.analysis.image.MultiScaleStructuralSimilarityIndex.MSSIMScore;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalWarper;
import com.bmskinner.nuclear_morphology.charting.charts.ConsensusNucleusChartFactory;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.IWarpedSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.ShortWarpedSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.WarpedSignalKey;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.DefaultInputSupplier;
import com.bmskinner.nuclear_morphology.gui.tabs.signals.warping.SignalWarpingModelRevamp.ImageCache.WarpedImageKey;
import com.bmskinner.nuclear_morphology.io.Io;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 * Control interactions of the signal warping chart, table and settings
 * @author bms41
 * @since 1.19.4
 *
 */
public class SignalWarpingDialogControllerRevamp 
implements SignalWarpingDisplayListener, 
	SignalWarpingRunEventListener, 
	PropertyChangeListener {
	
	private static final Logger LOGGER = Logger.getLogger(SignalWarpingDialogControllerRevamp.class.getName());

    final private List<SignalWarpingDisplayListener> displayListeners = new ArrayList<>();
    final private List<SignalWarpingProgressEventListener> progressListeners = new ArrayList<>();
    final private List<SignalWarpingMSSSIMUpdateListener> msssimListeners = new ArrayList<>();
    
	private SignalWarpingModelRevamp model;
    private SignalWarper warper;
	private ChartPanel chart;
	private JTable table;
	private SignalWarpingDisplaySettings displayOptions;
	
	public SignalWarpingDialogControllerRevamp(SignalWarpingModelRevamp model, 
			ChartPanel chart, 
			JTable table, 
			SignalWarpingDisplaySettings displayOptions) {
		this.chart = chart;
		this.table = table;
		this.model = model;
		this.displayOptions = displayOptions;


		ListSelectionModel cellSelectionModel = table.getSelectionModel();
		cellSelectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		cellSelectionModel.addListSelectionListener(e->{
			if(e.getValueIsAdjusting())
				return;

			model.clearSelection();
			List<WarpedImageKey> keys = new ArrayList<>();
			int[] selectedRow = table.getSelectedRows();
			for (int i = 0; i < selectedRow.length; i++) {
				LOGGER.fine("Selecting table row "+selectedRow[i]);
				model.addSelection(selectedRow[i]);
				
				// Send a message with the changed display settings
				SignalWarpingDisplaySettings displaySettings = new SignalWarpingDisplaySettings();
				displaySettings.setInt(SignalWarpingDisplaySettings.THRESHOLD_KEY, 
						SignalWarpingModel.THRESHOLD_ALL_VISIBLE-model.getThreshold(selectedRow[i]));
				fireDisplaySettingsChanged(displaySettings);
				keys.add(model.getKey(selectedRow[i]));
			}
			
			MSSIMScore values = null;
			if(keys.size()==2 && keys.get(0).getTarget().getID()
					.equals(keys.get(1).getTarget().getID())) {
				MultiScaleStructuralSimilarityIndex msi = new MultiScaleStructuralSimilarityIndex();
				values = msi.calculateMSSIM(model.getImage(keys.get(0)), 
											model.getImage(keys.get(1)));
				
				fireMSSSIMUpdated(String.format("%4.3f", values.msSsimIndex)+" between selected images");
			} else {
				if(keys.size()==2)
					fireMSSSIMUpdated("Selected images must have the same target shape");
				else
					fireMSSSIMUpdated("Can only calculate MS-SSIM* between two images");
			}
			updateChart();
		});
	}
	
	/**
	 * Add a listener for changes to the display settings
	 * @param l
	 */
	public void addSignalWarpingDisplayListener(SignalWarpingDisplayListener l) {
		displayListeners.add(l);
	}
	
	/**
	 * Add a listener for changes to warping progress
	 * @param l
	 */
	public void addSignalWarpingProgressEventListener(SignalWarpingProgressEventListener l) {
		progressListeners.add(l);
	}
	
	/**
	 * Add a listener for changes to MS-SSIM* values
	 * @param l
	 */
	public void addSignalWarpingMSSSIMUpdateListener(SignalWarpingMSSSIMUpdateListener l) {
		msssimListeners.add(l);
	}
	
	/**
	 * Inform listeners of changes to MS-SSIM* values
	 * @param value
	 * @param message
	 */
	private void fireMSSSIMUpdated(String message){		
		for(SignalWarpingMSSSIMUpdateListener l : msssimListeners) {
			l.MSSSIMUpdated(message);
		}
	}
	
	private void fireDisplaySettingsChanged(SignalWarpingDisplaySettings settings){		
		for(SignalWarpingDisplayListener l : displayListeners) {
			l.signalWarpingDisplayChanged(settings);
		}
	}

	/**
     * Run the warper with the currently selected settings
     */
    private void runWarping(SignalWarpingRunSettings runOptions) {

        try {
            warper = new SignalWarper(runOptions);
            warper.addPropertyChangeListener(this);
            
            ThreadManager.getInstance().execute(warper);

        } catch (Exception e) {
        	LOGGER.warning("Error running warping");
            LOGGER.log(Loggable.STACK, "Error running warping", e);
            JFreeChart ch = ConsensusNucleusChartFactory.createErrorChart();
            chart.setChart(ch);
        }
    }
    
	/**
	 * Update the chart to display the given image over the nucleus outline for
	 * dataset two
	 * 
	 * @param image
	 */
	public void updateChart() {

		Runnable task = () -> {
			chart.setChart(model.getChart(displayOptions));
			chart.restoreAutoBounds();
		};
		ThreadManager.getInstance().submit(task);
	}
	
	public void deleteWarpedSignal(WarpedImageKey selectedKey) {
				
		ISignalGroup sg  = selectedKey.getTemplate().getCollection().getSignalGroup(selectedKey.getSignalGroupId()).get();
		
		WarpedSignalKey k = new WarpedSignalKey(selectedKey.getTemplate().getCollection().getConsensus(), selectedKey.getTemplate().getId(), selectedKey.isOnlyCellsWithSignals(), selectedKey.getThreshold() );
		sg.getWarpedSignals().ifPresent(e->e.removeWarpedImage(k));
		model.removeRow(selectedKey);
	}
	
	public void deleteWarpedSignal(int row) {
		WarpedImageKey selectedKey = model.getKey(row);
		deleteWarpedSignal(selectedKey);		
	}

	/**
	 * Run when the warper is finished. Create the final image for display and
	 * set the chart
	 */
	public void warpingComplete() {
		try {

			ImageProcessor image = warper.get(); // get the 16-bit result of warping
			SignalWarpingRunSettings runSettings = warper.getOptions();

			ISignalGroup sg  = runSettings.templateSignalGroup();
			IWarpedSignal ws = sg.getWarpedSignals()
					.orElse(new ShortWarpedSignal(runSettings.signalId()));

			ws.addWarpedImage(runSettings.targetShape(), 
					runSettings.signalId(), 
					runSettings.targetDataset().getName(), 
					runSettings.getBoolean(SignalWarpingRunSettings.IS_ONLY_CELLS_WITH_SIGNALS_KEY), 
					runSettings.getInt(SignalWarpingRunSettings.MIN_THRESHOLD_KEY), 
					image);
			sg.setWarpedSignals(ws);

			model.clearSelection();
			model.addImage(runSettings.targetShape(), 
					runSettings.targetDataset().getName(), 
					runSettings.templateDataset(), 
					runSettings.signalId(), 
					runSettings.getBoolean(SignalWarpingRunSettings.IS_ONLY_CELLS_WITH_SIGNALS_KEY),
					runSettings.getBoolean(SignalWarpingRunSettings.IS_BINARISE_SIGNALS_KEY),
					runSettings.getInt(SignalWarpingRunSettings.MIN_THRESHOLD_KEY), 
					image);

			updateChart();
			fireSignalWarpingProgressEvent(-1);
		} catch (Exception e) {
			LOGGER.log(Loggable.STACK, "Error getting warp results", e);
		}
	}
	
	/**
	 * Set the display threshold to the given value
	 * @param threshold the threshold value to set. Has no effect if not in range 0-255
	 */
	public void setThresholdOfSelected(int threshold) {
		if(threshold<0 || threshold >255)
			return;
		model.setThresholdOfSelected(threshold);
	}

	/**
	 * Update the chart to display to an empty chart
	 * 
	 */
	public void displayBlankChart() {

		LOGGER.fine("Updating blank chart");
		chart.setChart(ConsensusNucleusChartFactory.createEmptyChart());
//		JFreeChart ch = null;
//
//		ChartOptions options = new ChartOptionsBuilder()
//				.setDatasets(model.getTemplates()).build();
//
//		ch = new ConsensusNucleusChartFactory(options)
//				.makeNucleusOutlineChart();
//		chart.setChart(ch);
	}

	/**
	 * Export the selected warped image with no chart decorations. The image is pseudocoloured,
	 * enhanced and thresholded according to the current display settings.
	 */
	public void exportImage() {
		
		ImageProcessor ip = model.getDisplayImage(displayOptions);
		ip.flipVertical();
		
		int[] selectedRow = table.getSelectedRows();
		
		File defaultFolder = null;
		String imageName = "Image";
		if(selectedRow.length==1) {
			WarpedImageKey k = model.getKey(selectedRow[0]);
			defaultFolder = k.getTemplate().getSavePath().getParentFile();
			imageName = k.getTargetName()+"_"
					+k.getTemplate().getName()+"-"
					+k.getTemplate().getCollection()
						.getSignalGroup(k.getSignalGroupId()).get()
						.getGroupName();
		}

		ImagePlus imp = new ImagePlus(imageName,ip);
		try {
			File saveFile = new DefaultInputSupplier()
					.requestFileSave(defaultFolder, imageName, Io.TIFF_FILE_EXTENSION_NODOT);
			IJ.saveAsTiff(imp, saveFile.getAbsolutePath());
		} catch (RequestCancelledException e) {}
	}	
	
	@Override
	public void signalWarpingDisplayChanged(@NonNull SignalWarpingDisplaySettings settings) {
		displayOptions.set(settings);
		setThresholdOfSelected(displayOptions.getInt(SignalWarpingDisplaySettings.THRESHOLD_KEY));
		updateChart();
		
	}

	@Override
	public void runEventReceived(SignalWarpingRunSettings settings) {
		ThreadManager.getInstance().submit( () -> runWarping(settings));		
	}
	
	private void fireSignalWarpingProgressEvent(int progress){
		for(SignalWarpingProgressEventListener l : progressListeners) {
			l.warpingProgressed(progress);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (IAnalysisWorker.FINISHED_MSG.equals(evt.getPropertyName())) {
			warpingComplete();
		} else {
			// Fire progress to listening UI elements
	        Object newValue = evt.getNewValue();
	        if (newValue instanceof Integer) {
	        	fireSignalWarpingProgressEvent(((Integer) newValue).intValue());
	        }
		}
		
	}
}
