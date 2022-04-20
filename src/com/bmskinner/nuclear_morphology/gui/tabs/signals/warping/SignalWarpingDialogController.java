package com.bmskinner.nuclear_morphology.gui.tabs.signals.warping;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.image.MultiScaleStructuralSimilarityIndex;
import com.bmskinner.nuclear_morphology.analysis.image.MultiScaleStructuralSimilarityIndex.MSSIMScore;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalWarper;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.cells.Nucleus;
import com.bmskinner.nuclear_morphology.components.generic.FloatPoint;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.signals.DefaultWarpedSignal;
import com.bmskinner.nuclear_morphology.components.signals.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.signals.IWarpedSignal;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.DefaultInputSupplier;
import com.bmskinner.nuclear_morphology.gui.tabs.signals.warping.SignalWarpingModel.ImageCache.WarpedImageKey;
import com.bmskinner.nuclear_morphology.io.Io;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.utility.FileUtils;
import com.bmskinner.nuclear_morphology.visualisation.charts.ConsensusNucleusChartFactory;
import com.bmskinner.nuclear_morphology.visualisation.image.ImageConverter;
import com.bmskinner.nuclear_morphology.visualisation.image.ImageFilterer;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.RGBStackMerge;
import ij.process.ImageProcessor;

/**
 * Control interactions of the signal warping chart, table and settings
 * 
 * @author bms41
 * @since 1.19.4
 *
 */
public class SignalWarpingDialogController
		implements SignalWarpingDisplayListener, SignalWarpingRunEventListener, PropertyChangeListener {

	private static final Logger LOGGER = Logger.getLogger(SignalWarpingDialogController.class.getName());

	final private List<SignalWarpingDisplayListener> displayListeners = new ArrayList<>();
	final private List<SignalWarpingProgressEventListener> progressListeners = new ArrayList<>();
	final private List<SignalWarpingMSSSIMUpdateListener> msssimListeners = new ArrayList<>();

	private SignalWarpingModel model;
	private SignalWarper warper;
	private ChartPanel chart;
	private JTable table;
	private SignalWarpingDisplaySettings displayOptions;

	public SignalWarpingDialogController(SignalWarpingModel model, ChartPanel chart, JTable table,
			SignalWarpingDisplaySettings displayOptions) {
		this.chart = chart;
		this.table = table;
		this.model = model;
		this.displayOptions = displayOptions;

		ListSelectionModel cellSelectionModel = table.getSelectionModel();
		cellSelectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		cellSelectionModel.addListSelectionListener(e -> {
			if (e.getValueIsAdjusting())
				return;

			model.clearSelection();
			List<WarpedImageKey> keys = new ArrayList<>();

			int[] selectedRow = table.getSelectedRows();

			for (int i = 0; i < selectedRow.length; i++) {
				LOGGER.fine("Selecting table row " + selectedRow[i]);
				WarpedImageKey key = model.getKey(selectedRow[i]);
				model.addSelection(selectedRow[i]);

				// Send a message with the changed display settings
				// as long as only one row is selected - otherwise
				// we will overwrite the stored thresholds
				if (selectedRow.length == 1) {
					this.displayOptions.setInt(SignalWarpingDisplaySettings.THRESHOLD_KEY,
							SignalWarpingModel.THRESHOLD_ALL_VISIBLE - model.getThreshold(selectedRow[i]));
					fireDisplaySettingsChanged(this.displayOptions);
				}
				keys.add(key);
			}

			MSSIMScore values = null;
			if (keys.size() == 2 && keys.get(0).getTarget().getID().equals(keys.get(1).getTarget().getID())) {
				MultiScaleStructuralSimilarityIndex msi = new MultiScaleStructuralSimilarityIndex();
				values = msi.calculateMSSIM(model.getImage(keys.get(0)), model.getImage(keys.get(1)));

				fireMSSSIMUpdated(String.format("%4.3f", values.msSsimIndex) + " between selected images");
			} else {
				if (keys.size() == 2)
					fireMSSSIMUpdated("Selected images must have the same target shape");
				else
					fireMSSSIMUpdated("Can only calculate MS-SSIM* between two images");
			}
			updateChart();
		});
	}

	/**
	 * Add a listener for changes to the display settings
	 * 
	 * @param l
	 */
	public void addSignalWarpingDisplayListener(SignalWarpingDisplayListener l) {
		displayListeners.add(l);
	}

	/**
	 * Add a listener for changes to warping progress
	 * 
	 * @param l
	 */
	public void addSignalWarpingProgressEventListener(SignalWarpingProgressEventListener l) {
		progressListeners.add(l);
	}

	/**
	 * Add a listener for changes to MS-SSIM* values
	 * 
	 * @param l
	 */
	public void addSignalWarpingMSSSIMUpdateListener(SignalWarpingMSSSIMUpdateListener l) {
		msssimListeners.add(l);
	}

	/**
	 * Inform listeners of changes to MS-SSIM* values
	 * 
	 * @param value
	 * @param message
	 */
	private void fireMSSSIMUpdated(String message) {
		for (SignalWarpingMSSSIMUpdateListener l : msssimListeners) {
			l.MSSSIMUpdated(message);
		}
	}

	private void fireDisplaySettingsChanged(SignalWarpingDisplaySettings settings) {
		for (SignalWarpingDisplayListener l : displayListeners) {
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

	public void deleteWarpedSignal(WarpedImageKey selectedKey)
			throws MissingLandmarkException, ComponentCreationException {

		ISignalGroup sg = selectedKey.getTemplate().getCollection().getSignalGroup(selectedKey.getSignalGroupId())
				.get();

		DefaultWarpedSignal k = new DefaultWarpedSignal(selectedKey.getTemplate().getCollection().getConsensus(),
				selectedKey.getTemplate().getId(), selectedKey.isOnlyCellsWithSignals(), selectedKey.getThreshold(),
				selectedKey.isBinarised(), selectedKey.isNormalised());
		sg.getWarpedSignals().ifPresent(e -> e.removeWarpedImage(k));
		model.removeRow(selectedKey);
	}

	public void deleteWarpedSignal(int row) throws MissingLandmarkException, ComponentCreationException {
		WarpedImageKey selectedKey = model.getKey(row);
		deleteWarpedSignal(selectedKey);
	}

	/**
	 * Run when the warper is finished. Create the final image for display and set
	 * the chart
	 */
	public void warpingComplete() {
		try {

			ImageProcessor image = warper.get(); // get the 16-bit result of warping
			SignalWarpingRunSettings runSettings = warper.getOptions();
			ISignalGroup sg = runSettings.templateSignalGroup();

			IWarpedSignal ws = new DefaultWarpedSignal(runSettings.targetShape(), runSettings.targetDataset().getId(),
					runSettings.getBoolean(SignalWarpingRunSettings.IS_ONLY_CELLS_WITH_SIGNALS_KEY),
					runSettings.getInt(SignalWarpingRunSettings.MIN_THRESHOLD_KEY),
					runSettings.getBoolean(SignalWarpingRunSettings.IS_BINARISE_SIGNALS_KEY),
					runSettings.getBoolean(SignalWarpingRunSettings.IS_NORMALISE_TO_COUNTERSTAIN_KEY),
					IWarpedSignal.toArray(image), image.getWidth());
			sg.addWarpedSignal(ws);

			updateChart();
			fireSignalWarpingProgressEvent(-1);
		} catch (Exception e) {
			LOGGER.log(Loggable.STACK, "Error getting warp results", e);
		}
	}

	/**
	 * Set the display threshold to the given value
	 * 
	 * @param threshold the threshold value to set. Has no effect if not in range
	 *                  0-255
	 */
	public void setThresholdOfSelected(int threshold) {
		if (threshold < 0 || threshold > 255)
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
	}

	/**
	 * Export the selected warped image with no chart decorations. If one image is
	 * selected, the image is pseudocoloured and thresholded according to the
	 * current display settings. If two images are selected, the images are combined
	 * into an RGB image using green and magenta channels for easily distinguishable
	 * overlaps.
	 */
	public void exportImage(boolean includeConsensus) {
		ImagePlus imp;
		File defaultFolder = FileUtils.extantComponent(model.getSelectedKeys().get(0).getTemplate().getSavePath());

		if (model.selectedImageCount() == 2) {
			try {
				String[] options = { "Red-Green", "Blue-Yellow", "Green-Magenta", "What's on screen" };

				int colourOption = new DefaultInputSupplier().requestOption(options, 2,
						"Select a colour pair for signals; the default\noptions will"
								+ " remain distinct even if the\nsignal pseudocolours were similar",
						"Choose colour pair");

				if (colourOption == 3) {
					// If they want to be foolish and use their own colours...
					imp = createDualChannelDisplayImageForExport(includeConsensus);
				} else {

					int c1 = colourOption == 0 ? 0 : colourOption == 1 ? 2 : 1;

					int c2 = colourOption == 0 ? 1 : colourOption == 1 ? 6 : 5;
					imp = createDualChannelImage(c1, c2, includeConsensus);
				}
			} catch (RequestCancelledException e) {
				return;
			}
		} else {
			imp = createSingleChannelImage(includeConsensus);
		}

		try {
			boolean doSave = true;
			File saveFile = new DefaultInputSupplier().requestFileSave(defaultFolder, imp.getTitle(),
					Io.TIFF_FILE_EXTENSION_NODOT);
			if (saveFile.exists()) {
				doSave = new DefaultInputSupplier().requestApproval("Overwrite existing file?", "Overwrite?");
			}
			if (doSave)
				IJ.saveAsTiff(imp, saveFile.getAbsolutePath());

		} catch (RequestCancelledException e) {
		}
	}

	/**
	 * Add a consensus nucleus to the current display image
	 * 
	 * @return
	 */
	private ImagePlus createSingleChannelImage(boolean includeConsensus) {
		ImageProcessor ip = model.getDisplayImage(displayOptions);
		WarpedImageKey k = model.getSelectedKeys().get(0);
		String imageName = k.getTargetName() + "_" + k.getTemplate().getName() + "-" + k.getSignalGroupName();

		// Add a border so we don't drop outline pixels at the edge
		int buffer = 10;
		ip = ImageConverter.expandCanvas(ip, buffer, Color.white);

		List<Nucleus> targets = model.getSelectedKeys().stream().map(key -> key.getTarget().duplicate()).distinct()
				.collect(Collectors.toList());

		if (includeConsensus)
			return drawConsensusOnImage(ip, targets, Color.black, imageName);
		ip.flipVertical();
		return new ImagePlus(imageName, ip);
	}

	/**
	 * Create an image from two warped selected images, using the ImageJ colour
	 * merge tool
	 * 
	 * @param colour1 the index of the colour for image 1 in { R, G, B, W, C, M, Y }
	 * @param colour2 the index of the colour for image 2 in { R, G, B, W, C, M, Y }
	 * @return
	 */
	private ImagePlus createDualChannelImage(int colour1, int colour2, boolean includeConsensus) {

		List<WarpedImageKey> keys = new ArrayList<>(model.getSelectedKeys());

		WarpedImageKey key0 = keys.get(0);
		WarpedImageKey key1 = keys.get(1);

		// Ensure we keep order of keys and images consistent
		List<ImageProcessor> imageList = ImageFilterer
				.fitToCommonCanvas(keys.stream().map(k -> model.getImage(k)).collect(Collectors.toList()));

		ImageProcessor ip1 = imageList.get(0);
		ImageProcessor ip2 = imageList.get(1);

		// Order of images - RGBWCMY
		ImagePlus[] images = { null, null, null, null, null, null, null };
		images[colour1] = new ImagePlus("1", ip1);
		images[colour2] = new ImagePlus("2", ip2);

		// Merge and flatten to an RGB image
		ImagePlus result = RGBStackMerge.mergeChannels(images, false).flatten();
		LOGGER.fine(result.getProcessor().getClass().getName());
		ImageProcessor ip3 = result.getProcessor();

		// Add a border so we don't drop outline pixels at the edge
		int buffer = 10;
		ip3 = ImageConverter.expandCanvas(ip3, buffer, Color.black);

		List<Nucleus> targets = keys.stream().map(k -> k.getTarget().duplicate()).distinct()
				.collect(Collectors.toList());

		String imageName = key0.getTemplate().getName() + "_" + key0.getSignalGroupName() + "_"
				+ key1.getTemplate().getName() + "_" + key1.getSignalGroupName();

		if (includeConsensus)
			return drawConsensusOnImage(ip3, targets, Color.white, imageName);
		ip3.flipVertical();
		return new ImagePlus(imageName, ip3);
	}

	/**
	 * Create a dual channel image for export with the consensus drawn atop, using
	 * the display image rather than recolouring to sensible values
	 * 
	 * @return
	 */
	private ImagePlus createDualChannelDisplayImageForExport(boolean includeConsensus) {
		ImageProcessor ip = model.getDisplayImage(displayOptions);
		int buffer = 10;
		ip = ImageConverter.expandCanvas(ip, buffer, Color.white);
		List<WarpedImageKey> keys = new ArrayList<>(model.getSelectedKeys());

		WarpedImageKey key0 = keys.get(0);
		WarpedImageKey key1 = keys.get(1);

		List<Nucleus> targets = keys.stream().map(k -> k.getTarget().duplicate()).distinct()
				.collect(Collectors.toList());

		String imageName = key0.getTemplate().getName() + "_" + key0.getSignalGroupName() + "_"
				+ key1.getTemplate().getName() + "_" + key1.getSignalGroupName();

		if (includeConsensus)
			return drawConsensusOnImage(ip, targets, Color.black, imageName);
		ip.flipVertical();
		return new ImagePlus(imageName, ip);
	}

	private ImagePlus drawConsensusOnImage(ImageProcessor ip, List<Nucleus> targets, Color colour, String imageName) {

		try {
			for (Nucleus target : targets) {
				// Don't move the existing template
				target = target.duplicate();
				target.orient(); // ensure rotation is valid

				// Centre the outline on the canvas
				int wBuffer = (int) Math.round(ip.getWidth() - target.getWidth()) / 2;
				int hBuffer = (int) Math.round(ip.getHeight() - target.getHeight()) / 2;
				LOGGER.fine("Buffer: " + wBuffer + "w " + hBuffer + "h");

				// CoM starts at 0, 0; offset to image coordinates
				target.moveCentreOfMass(
						new FloatPoint(Math.abs(target.getMinX()) + wBuffer, Math.abs(target.getMinY()) + hBuffer));
				ip.setColor(colour);

				// Draw the border
				for (IPoint p : target.getBorderList()) {
					ip.drawDot(p.getXAsInt(), p.getYAsInt());
				}
			}
		} catch (MissingLandmarkException e) {
			LOGGER.warning("Error getting consensus nucleus");
		}

		// Y-coordinates in images increase top to bottom
		ip.flipVertical();

		return new ImagePlus(imageName, ip);
	}

	@Override
	public void signalWarpingDisplayChanged(@NonNull SignalWarpingDisplaySettings settings) {
		displayOptions.set(settings);
		setThresholdOfSelected(displayOptions.getInt(SignalWarpingDisplaySettings.THRESHOLD_KEY));
		updateChart();
	}

	@Override
	public void runEventReceived(SignalWarpingRunSettings settings) {
		ThreadManager.getInstance().submit(() -> runWarping(settings));
	}

	private void fireSignalWarpingProgressEvent(int progress) {
		for (SignalWarpingProgressEventListener l : progressListeners) {
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
