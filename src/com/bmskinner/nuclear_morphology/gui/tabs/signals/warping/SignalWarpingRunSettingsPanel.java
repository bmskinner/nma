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
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import com.bmskinner.nuclear_morphology.analysis.signals.SignalManager;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.components.panels.DatasetSelectionPanel;
import com.bmskinner.nuclear_morphology.gui.components.panels.SignalGroupSelectionPanel;

public class SignalWarpingRunSettingsPanel 
	extends JPanel 
	implements SignalWarpingProgressEventListener {

	private static final long serialVersionUID = 1L;
	private static final String SOURCE_DATASET_LBL  = "Source dataset";
    private static final String TARGET_DATASET_LBL  = "Target dataset";
    private static final String SIGNAL_GROUP_LBL    = "Signal group";
    private static final String INCLUDE_CELLS_LBL   = "Only include cells with signals";
    private static final String RUN_LBL             = "Run";
    private static final String MIN_THRESHOLD_LBL   = "Min threshold";
    private static final String BINARISE_LBL        = "Binarise";
    private static final String NORMALISE_LBL       = "Normalise to counterstain";
    
    
    private static final String SOURCE_DATASET_TOOLTIP = "Which dataset should signals come from?";
    private static final String TARGET_DATASET_TOOLTIP = "Which dataset consensus should we warp onto?";
    private static final String SIGNAL_GROUP_TOOLTIP   = "Which signal group to warp?";
    private static final String INCLUDE_CELLS_TOOLTIP  = "Tick to use only cells with explicit signals detected";
    private static final String RUN_TOOLTIP            = "Run the signal warping";
    private static final String MIN_THRESHOLD_TOOLTIP  = "Threshold images to this value before warping";
    private static final String BINARISE_TOOLTIP       = "Binarise images so intra-image intensities are not included";
    private static final String NORMALISE_TOOLTIP      = "Normalise signal against the counterstain before warping";
    
    private static final String SOURCE_HELP       = "Choose the signals to be warped:";
    private static final String IMAGE_HELP        = "Choose how to pre-process images:";
    private static final String TARGET_HELP       = "Choose the shape to warp images onto:";
    
	private DatasetSelectionPanel datasetBoxOne;
    private DatasetSelectionPanel datasetBoxTwo;

    private SignalGroupSelectionPanel signalBox;

    private JButton   runButton;
    private JCheckBox cellsWithSignalsBox;
    private JSpinner minThresholdSpinner;
    private JCheckBox binariseBox;
    private JCheckBox normaliseBox;
    
	private SignalWarpingDialogController controller;
	private SignalWarpingModel model;
	
    private final JProgressBar progressBar = new JProgressBar(0,100);
    	
	private List<SignalWarpingRunEventListener> runListeners = new ArrayList<>();
	
	public SignalWarpingRunSettingsPanel(SignalWarpingDialogController controller2,
			SignalWarpingModel model) {
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
		settings.setBoolean(SignalWarpingRunSettings.IS_NORMALISE_TO_COUNTERSTAIN_KEY, normaliseBox.isSelected());
		
		for(SignalWarpingRunEventListener l : runListeners) {
			l.runEventReceived(settings);
		}
	}
	
	private JPanel createSettingsPanel() {
    	JPanel panel = new JPanel();
    	panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    	
    	JPanel help1Panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    	JPanel upperPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    	JPanel help2Panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    	JPanel midPanel   = new JPanel(new FlowLayout(FlowLayout.LEFT));
    	JPanel help3Panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    	JPanel lowerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    	JPanel runPanel   = new JPanel(new FlowLayout(FlowLayout.LEFT));

    	datasetBoxOne = new DatasetSelectionPanel(model.getDatasets());
    	datasetBoxOne.setToolTipText(SOURCE_DATASET_TOOLTIP);
    	datasetBoxTwo = new DatasetSelectionPanel(model.getDatasets());
    	datasetBoxTwo.setToolTipText(TARGET_DATASET_TOOLTIP);

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
    	signalBox.setToolTipText(SIGNAL_GROUP_TOOLTIP);
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

    	cellsWithSignalsBox = new JCheckBox(INCLUDE_CELLS_LBL, false);
    	cellsWithSignalsBox.setToolTipText(INCLUDE_CELLS_TOOLTIP);
    	
    	// Set the initial value to the signal detection threshold of the initial selected signal group
    	int threshold = datasetBoxOne.getSelectedDataset().getAnalysisOptions().isPresent() 
    			? datasetBoxOne.getSelectedDataset().getAnalysisOptions().get()
				.getNuclearSignalOptions(signalBox.getSelectedID()).getThreshold() : 0;
    	SpinnerModel minThresholdModel = new SpinnerNumberModel(threshold, 0, 255, 1);
    	minThresholdSpinner = new JSpinner(minThresholdModel);
    	minThresholdSpinner.setToolTipText(MIN_THRESHOLD_TOOLTIP);

    	binariseBox = new JCheckBox(BINARISE_LBL, false);   	
    	binariseBox.setToolTipText(BINARISE_TOOLTIP);
    	
    	normaliseBox = new JCheckBox(NORMALISE_LBL, false);   	
    	normaliseBox.setToolTipText(NORMALISE_TOOLTIP);
    	
    	runButton = new JButton(RUN_LBL);
    	runButton.setToolTipText(RUN_TOOLTIP);
    	runButton.addActionListener(e ->{  
    		setEnabled(false);
    		fireSignalWarpingRunEvent();
    	});
    	if(!signalBox.hasSelection()) 
    		runButton.setEnabled(false);
    	
    	
    	help1Panel.add(new JLabel(SOURCE_HELP));
    	
    	upperPanel.add(new JLabel(SOURCE_DATASET_LBL));
    	upperPanel.add(datasetBoxOne);
    	upperPanel.add(new JLabel(SIGNAL_GROUP_LBL));
    	upperPanel.add(signalBox);
    	
    	help2Panel.add(new JLabel(IMAGE_HELP));

    	midPanel.add(new JLabel(MIN_THRESHOLD_LBL));
    	midPanel.add(minThresholdSpinner);
    	midPanel.add(binariseBox);
    	midPanel.add(cellsWithSignalsBox);
    	midPanel.add(normaliseBox);
    	
    	help3Panel.add(new JLabel(TARGET_HELP));

    	lowerPanel.add(new JLabel(TARGET_DATASET_LBL));
    	lowerPanel.add(datasetBoxTwo);

    	runPanel.add(runButton);
    	progressBar.setStringPainted(true);
    	runPanel.add(progressBar);
    	
    	panel.add(help1Panel);
    	panel.add(upperPanel);
    	panel.add(help2Panel);
    	panel.add(midPanel);
    	panel.add(help3Panel);
    	panel.add(lowerPanel);
    	panel.add(runPanel);
    	
    	return panel;
    }
	
	@Override
	public void setEnabled(boolean b) {
		datasetBoxOne.setEnabled(b);
		datasetBoxTwo.setEnabled(b);
		signalBox.setEnabled(b);
		cellsWithSignalsBox.setEnabled(b);
		minThresholdSpinner.setEnabled(b);
		binariseBox.setEnabled(b);
		runButton.setEnabled(b);
		normaliseBox.setEnabled(b);
	}
	
	/**
	 * Set the signal settings enabled. Use when switching signal
	 * sources
	 * @param b
	 */
	private void setSignalSettingsEnabled(boolean b) {
		minThresholdSpinner.setEnabled(b);
		cellsWithSignalsBox.setEnabled(b);
		binariseBox.setEnabled(b);
		normaliseBox.setEnabled(b);
	}

	@Override
	public void warpingProgressed(int progress) {
		progressBar.setValue(progress);
		if(progress==-1)
			setEnabled(true);
	}

}
