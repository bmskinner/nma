package com.bmskinner.nuclear_morphology.gui.tabs.signals.warping;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeListener;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.analysis.image.MultiScaleStructuralSimilarityIndex;
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
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.DefaultInputSupplier;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.tabs.signals.warping.SignalWarpingDialog.WarpingSettingsPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.signals.warping.SignalWarpingModel.ImageCache.WarpedImageKey;
import com.bmskinner.nuclear_morphology.io.Io;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 * Control interactions of the signal warping chart, table and settings
 * @author bms41
 * @since 1.15.0
 *
 */
public class SignalWarpingDialogController implements Loggable {

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
			List<WarpedImageKey> keys = new ArrayList<>();
			int[] selectedRow = table.getSelectedRows();
			for (int i = 0; i < selectedRow.length; i++) {
				fine("Selecting table row "+selectedRow[i]);
				model.addSelection(selectedRow[i]);
				settingsPanel.setDisplayThreshold(SignalWarpingModel.THRESHOLD_ALL_VISIBLE-model.getThreshold(selectedRow[i]));
				keys.add(model.getKey(selectedRow[i]));
			}
			
			if(keys.size()==2 &&keys.get(0).getTarget().equals(keys.get(1).getTarget())) {
				double value = MultiScaleStructuralSimilarityIndex.calculateMSSIM(model.getImage(keys.get(0)), model.getImage(keys.get(1)));
				settingsPanel.setMSSIM(String.valueOf(value));
			} else {
				settingsPanel.setMSSIM("");
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
		
		WarpedImageKey selectedKey = model.getKey(row);
		
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
			settingsPanel.setSettingsEnabled(true);

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
		
		
		int[] selectedRow = table.getSelectedRows();
		
		File defaultFolder = null;
		String imageName = "Image";
		if(selectedRow.length==1) {
			WarpedImageKey k = model.getKey(selectedRow[0]);
			defaultFolder = k.getTemplate().getSavePath().getParentFile();
			imageName = k.getTargetName()+"_"+k.getTemplate().getName()+"-"+k.getTemplate().getCollection().getSignalGroup(k.getSignalGroupId()).get().getGroupName();
		}
		
		ImagePlus imp = new ImagePlus(imageName,ip);
		try {
			File saveFile = new DefaultInputSupplier().requestFileSave(defaultFolder, imageName, "tiff");
			IJ.saveAsTiff(imp, saveFile.getAbsolutePath());
		} catch (RequestCancelledException e) {}
	}
	
	/**
	 * Calculate MS-SSIM for all pairwise combinations of signals in each target shape.
	 */
	public void calculateSimilarities() {
		for(CellularComponent c : model.getTargets()) {
			for(WarpedImageKey k1 : model.getKeys(c)) {
				for(WarpedImageKey k2 : model.getKeys(c)) {
					if(k1==k2)
						continue;
					ImageProcessor ip1 = model.getImage(k1);
					ImageProcessor ip2 = model.getImage(k2);
					System.out.println(k1+" vs "+k2);
					double value = MultiScaleStructuralSimilarityIndex.calculateMSSIM(ip1, ip2);
					
				}
				
			}
		}
		
	}

}
