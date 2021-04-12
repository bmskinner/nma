package com.bmskinner.nuclear_morphology.gui.tabs.signals.warping;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import com.bmskinner.nuclear_morphology.analysis.signals.SignalManager;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.components.panels.DatasetSelectionPanel;
import com.bmskinner.nuclear_morphology.gui.components.panels.SignalGroupSelectionPanel;

public class SignalWarpingRunSettingsPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private static final String SOURCE_DATASET_LBL  = "Source dataset";
    private static final String TARGET_DATASET_LBL  = "Target dataset";
    private static final String SIGNAL_GROUP_LBL    = "Signal group";
    private static final String INCLUDE_CELLS_LBL   = "Only include cells with signals";
    private static final String RUN_LBL             = "Run";
    private static final String MIN_THRESHOLD_LBL   = "Min threshold";
    private static final String BINARISE_LBL        = "Binarise";
    
	private DatasetSelectionPanel datasetBoxOne;
    private DatasetSelectionPanel datasetBoxTwo;

    private SignalGroupSelectionPanel signalBox;

    private JButton   runButton;
    private JCheckBox cellsWithSignalsBox;
    private JSpinner minThresholdSpinner;
    private JCheckBox binariseBox;
    
	private SignalWarpingDialogControllerRevamp controller;
	private SignalWarpingModelRevamp model;
	
	private List<SignalWarpingRunEventListener> runListeners = new ArrayList<>();
	
	public SignalWarpingRunSettingsPanel(SignalWarpingDialogControllerRevamp controller2,
			SignalWarpingModelRevamp model) {
		controller = controller2;
		this.model = model;
		this.setLayout(new BorderLayout());
		this.add(createSettingsPanel(), BorderLayout.CENTER);
	}
	
	public void addSignalWarpingRunEventListener(SignalWarpingRunEventListener l) {
		runListeners.add(l);
	}
		
	private void fireSignalWarpingRunEvent() {
		
		SignalWarpingRunSettings settings = new SignalWarpingRunSettings(datasetBoxOne.getSelectedDataset(),
				datasetBoxTwo.getSelectedDataset(),
				signalBox.getSelectedID());
		settings.setBoolean(SignalWarpingRunSettings.IS_BINARISE_SIGNALS_KEY, binariseBox.isSelected());
		settings.setBoolean(SignalWarpingRunSettings.IS_ONLY_CELLS_WITH_SIGNALS_KEY, cellsWithSignalsBox.isSelected());
		settings.setInt(SignalWarpingRunSettings.MIN_THRESHOLD_KEY, (int)minThresholdSpinner.getValue());
		
		for(SignalWarpingRunEventListener l : runListeners) {
			l.runEventReceived(settings);
		}
	}
	
	private JPanel createSettingsPanel() {
    	JPanel panel = new JPanel();
    	panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    	
    	JPanel upperPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    	JPanel midPanel   = new JPanel(new FlowLayout(FlowLayout.LEFT));
    	JPanel lowerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

    	datasetBoxOne = new DatasetSelectionPanel(model.getDatasets());
    	datasetBoxTwo = new DatasetSelectionPanel(model.getDatasets());

    	datasetBoxOne.setSelectedDataset(model.getDatasets().get(0));
    	datasetBoxTwo.setSelectedDataset(model.getDatasets().get(0));
    	datasetBoxOne.addActionListener(e -> {
    		if (datasetBoxOne.getSelectedDataset().getCollection().getSignalManager().hasSignals()) {
    			signalBox.setDataset(datasetBoxOne.getSelectedDataset());
    			
    			int threshold = datasetBoxOne.getSelectedDataset().getAnalysisOptions().get()
    					.getNuclearSignalOptions(signalBox.getSelectedID()).getThreshold();
    			minThresholdSpinner.setValue(threshold);
    		}
    		
    	});
    	datasetBoxTwo.addActionListener(e -> controller.displayBlankChart() );

    	signalBox = new SignalGroupSelectionPanel(datasetBoxOne.getSelectedDataset());
    	if (!signalBox.hasSelection())
    		signalBox.setEnabled(false);

    	signalBox.addActionListener(e->{
    		IAnalysisDataset sourceDataset = datasetBoxOne.getSelectedDataset();

	        SignalManager m = sourceDataset.getCollection().getSignalManager();
	        if (!m.hasSignals()) {
	        	setSignalSettingsEnabled(false);
	        } else {
	        	setSignalSettingsEnabled(true);
	            int threshold = datasetBoxOne.getSelectedDataset().getAnalysisOptions().get()
    					.getNuclearSignalOptions(signalBox.getSelectedID()).getThreshold();
    			minThresholdSpinner.setValue(threshold);
	        }
    	});

    	cellsWithSignalsBox = new JCheckBox(INCLUDE_CELLS_LBL, true);
    	
    	// Set the initial value to the signal detection threshold of the initial selected signal group
    	int threshold = datasetBoxOne.getSelectedDataset().getAnalysisOptions().get()
				.getNuclearSignalOptions(signalBox.getSelectedID()).getThreshold();
    	SpinnerModel minThresholdModel = new SpinnerNumberModel(threshold, 0, 255, 1);
    	minThresholdSpinner = new JSpinner(minThresholdModel);

    	binariseBox = new JCheckBox(BINARISE_LBL, true);   	
    	
    	runButton = new JButton(RUN_LBL);
    	runButton.addActionListener(e ->  fireSignalWarpingRunEvent() );
    	if(!signalBox.hasSelection()) 
    		runButton.setEnabled(false);
    	
    	
    	upperPanel.add(new JLabel(SOURCE_DATASET_LBL));
    	upperPanel.add(datasetBoxOne);
    	upperPanel.add(new JLabel(SIGNAL_GROUP_LBL));
    	upperPanel.add(signalBox);

    	midPanel.add(new JLabel(MIN_THRESHOLD_LBL));
    	midPanel.add(minThresholdSpinner);
    	midPanel.add(binariseBox);
    	midPanel.add(cellsWithSignalsBox);

    	lowerPanel.add(new JLabel(TARGET_DATASET_LBL));
    	lowerPanel.add(datasetBoxTwo);
    	lowerPanel.add(runButton);
    	
    	panel.add(upperPanel);
    	panel.add(midPanel);
    	panel.add(lowerPanel);
    	
    	return panel;
    }
	
	private void setSignalSettingsEnabled(boolean b) {
		minThresholdSpinner.setEnabled(b);
		cellsWithSignalsBox.setEnabled(b);
	}

}
