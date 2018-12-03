package com.bmskinner.nuclear_morphology.gui.tabs.signals.warping;

import java.util.UUID;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeListener;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.analysis.signals.SignalWarper;
import com.bmskinner.nuclear_morphology.charting.charts.ConsensusNucleusChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclear.DefaultWarpedSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.IWarpedSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.WarpedSignalKey;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.DefaultOptions;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.tabs.signals.warping.SignalWarpingDialog.WarpingSettingsPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.signals.warping.SignalWarpingModel.ImageCache.WarpedImageKey;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 * Control interactions of the signal warping chart, table and settings
 * @author bms41
 * @since 1.15.0
 *
 */
public class SignalWarpingDialogController implements Loggable {

	private static final int KEY_COLUMN_INDEX = 4;

	private SignalWarpingModel model;
	private ChartPanel chart;
	private JTable table;
	private WarpingSettingsPanel settingsPanel;

	public SignalWarpingDialogController(SignalWarpingModel model, ChartPanel chart, JTable table, WarpingSettingsPanel settingsPanel) {
		this.chart = chart;
		this.table = table;
		this.model = model;
		this.settingsPanel = settingsPanel;


		ListSelectionModel cellSelectionModel = table.getSelectionModel();
		cellSelectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		cellSelectionModel.addListSelectionListener(e->{
			if(e.getValueIsAdjusting())
				return;

			model.clearSelection();
			int[] selectedRow = table.getSelectedRows();
			for (int i = 0; i < selectedRow.length; i++) {
				WarpedImageKey selectedKey = (WarpedImageKey) model.getValueAt(selectedRow[i], KEY_COLUMN_INDEX);
				model.addSelection(selectedKey);
				settingsPanel.setDisplayThreshold(SignalWarpingModel.THRESHOLD_ALL_VISIBLE-model.getThreshold(selectedKey));

			}
			updateChart();
		});
	}

	/**
	 * Update the chart to display the given image over the nucleus outline for
	 * dataset two
	 * 
	 * @param image
	 */
	public void updateChart() {

		Runnable task = () -> {
			chart.setChart(model.getChart(settingsPanel.isPseudocolour(), settingsPanel.isEnhance()));
			chart.restoreAutoBounds();
		};
		ThreadManager.getInstance().submit(task);
	}
	
	public void deleteWarpedSignal(int row) {
		
		WarpedImageKey selectedKey = (WarpedImageKey) model.getValueAt(row, KEY_COLUMN_INDEX);
		
		ISignalGroup sg  = selectedKey.getTemplate().getCollection().getSignalGroup(selectedKey.getSignalGroupId()).get();
		
		WarpedSignalKey k = new WarpedSignalKey(selectedKey.getTemplate().getCollection().getConsensus(), selectedKey.isOnlyCellsWithSignals());
		sg.getWarpedSignals().ifPresent(e->e.removeWarpedImage(k));
		
		model.removeRow(row);
		
	}

	/**
	 * Run when the warper is finished. Create the final image for display and
	 * set the chart
	 */
	public void warpingComplete(SignalWarper warper) {
		try {

			ImageProcessor image = warper.get();
			IAnalysisDataset targetDataset = settingsPanel.getTarget();
			CellularComponent consensusTemplate = targetDataset.getCollection().getConsensus();
			IAnalysisDataset signalSource = settingsPanel.getSource();
			UUID signalGroupId = settingsPanel.getSignalId();

			boolean isCellsWithSignals = settingsPanel.isCellsWithSignals();
			
			boolean isBinarise = settingsPanel.isBinarise();
			
			int minThreshold = settingsPanel.getMinThreshold();

			ISignalGroup sg  = signalSource.getCollection().getSignalGroup(signalGroupId).get();
			IWarpedSignal ws = sg.getWarpedSignals().orElse(new DefaultWarpedSignal(signalGroupId));

//			ws.addWarpedImage(consensusTemplate, targetDataset.getName(), isCellsWithSignals, image.convertToByteProcessor());
//			sg.setWarpedSignals(ws);

			model.clearSelection();
			model.addImage(consensusTemplate, targetDataset.getName(), signalSource, signalGroupId, isCellsWithSignals, isBinarise, minThreshold, image);

			updateChart();
			settingsPanel.setEnabled(true);

		} catch (Exception e) {
			error("Error getting warp results", e);
		}
	}

	/**
	 * Display the nucleus outline for dataset two
	 * 
	 */
	public void updateBlankChart() {

		fine("Updating blank chart");
		JFreeChart ch = null;

		ChartOptions options = new ChartOptionsBuilder().setDatasets(settingsPanel.getTarget()).build();

		ch = new ConsensusNucleusChartFactory(options).makeNucleusOutlineChart();
		chart.setChart(ch);
	}

	public void exportImage() {
		ImageProcessor ip = model.getDisplayImage(settingsPanel.isPseudocolour(), settingsPanel.isEnhance());
		ip.flipVertical();
		new ImagePlus("Image",ip).show();
	}

}
