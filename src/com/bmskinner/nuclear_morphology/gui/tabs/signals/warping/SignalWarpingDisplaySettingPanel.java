package com.bmskinner.nuclear_morphology.gui.tabs.signals.warping;

import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Contains display settings for warped images
 * @author ben
 * @since 1.19.4
 *
 */
public class SignalWarpingDisplaySettingPanel 
	extends JPanel 
	implements SignalWarpingDisplayListener {

	private static final long serialVersionUID = 1L;
	private static final String PSEUDOCOLOUR_LBL = "Pseudocolour signals";
	private static final String THRESHOLD_LBL    = "Threshold";
	private static final String EXPORT_WITH_LBL  = "Export with consensus";
	private static final String EXPORT_WITHOUT_LBL = "Export without consensus";
	
	private static final String PSEUDOCOLOUR_TOOLTIP = "Peudocoloured signals using the signal group colour";
	private static final String THRESHOLD_TOOLTIP    = "Threshold the display to remove fainter signal";
	private static final String EXPORT_TOOLTIP = "Export the image with optimised colours";

	
	private static final boolean WITH_CONSENSUS = true;
	private static final boolean WITHOUT_CONSENSUS = false;
	
	private JCheckBox isPseudocolourBox;
    private JSlider   thresholdSlider;
    private JButton   exportWithButton;
    private JButton   exportWithoutButton;
    private SignalWarpingDialogController controller;
    
    final private List<SignalWarpingDisplayListener> listeners = new ArrayList<>();
	
	public SignalWarpingDisplaySettingPanel(SignalWarpingDialogController controller){
		this.controller = controller;
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    	JPanel displayPanel = createDisplaySettingsPanel();
    	add(displayPanel);
    	this.validate();
	}
	
	/**
	 * Add a listener for changes to the display settings
	 * @param l
	 */
	public void addSignalWarpingDisplayListener(SignalWarpingDisplayListener l) {
		listeners.add(l);
	}
	
	private JPanel createDisplaySettingsPanel() {
    	JPanel panel = new JPanel();
    	panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

    	isPseudocolourBox = new JCheckBox(PSEUDOCOLOUR_LBL, 
    			SignalWarpingDisplaySettings.DEFAULT_IS_PSEUDOCOLOUR);
    	isPseudocolourBox.setToolTipText(PSEUDOCOLOUR_TOOLTIP);
    	isPseudocolourBox.addActionListener(e->fireDisplaySettingsChanged());
    	
    	JPanel pseudoPanel =  new JPanel(new FlowLayout(FlowLayout.LEFT));
    	pseudoPanel.add(isPseudocolourBox);
    	panel.add(pseudoPanel);
    	
    	JPanel thresholdPanel =  new JPanel(new FlowLayout(FlowLayout.LEFT));
    	
    	thresholdPanel.add(new JLabel(THRESHOLD_LBL));
    	
    	thresholdSlider = new JSlider(0, SignalWarpingModel.THRESHOLD_ALL_VISIBLE);
    	thresholdSlider.setToolTipText(THRESHOLD_TOOLTIP);
    	thresholdSlider.setVisible(true);
    	thresholdSlider.setValue(SignalWarpingDisplaySettings.DEFAULT_THRESHOLD);
    	thresholdSlider.addChangeListener(e->fireDisplaySettingsChanged());
    	thresholdPanel.add(thresholdSlider); 
    	
    	panel.add(thresholdPanel);
    	
    	
    	JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    	exportWithButton = new JButton(EXPORT_WITH_LBL);
    	exportWithButton.setToolTipText(EXPORT_TOOLTIP);
    	exportWithButton.addActionListener(e->controller.exportImage(WITH_CONSENSUS));
    	
    	exportWithoutButton = new JButton(EXPORT_WITHOUT_LBL);
    	exportWithoutButton.setToolTipText(EXPORT_TOOLTIP);
    	exportWithoutButton.addActionListener(e->controller.exportImage(WITHOUT_CONSENSUS));
    	
    	buttonPanel.add(exportWithButton);    
    	buttonPanel.add(exportWithoutButton);    
    	
    	panel.add(buttonPanel);    
    	return panel;
    }
	
	private void fireDisplaySettingsChanged(){
		
		SignalWarpingDisplaySettings settings = new SignalWarpingDisplaySettings();
		settings.setBoolean(SignalWarpingDisplaySettings.PSEUDOCOLOUR_KEY, 
				isPseudocolourBox.isSelected());
		
		int value = SignalWarpingModel.THRESHOLD_ALL_VISIBLE - thresholdSlider.getValue();
		settings.setInt(SignalWarpingDisplaySettings.THRESHOLD_KEY, 
				value);
		
		for(SignalWarpingDisplayListener l : listeners) {
			l.signalWarpingDisplayChanged(settings);
		}
	}

	@Override
	public void signalWarpingDisplayChanged(@NonNull SignalWarpingDisplaySettings settings) {
		if(settings.getIntegerKeys().contains(SignalWarpingDisplaySettings.THRESHOLD_KEY)) {
			thresholdSlider.setValue(settings.getInt(SignalWarpingDisplaySettings.THRESHOLD_KEY));
		}
		
		if(settings.getBooleanKeys().contains(SignalWarpingDisplaySettings.PSEUDOCOLOUR_KEY)) {
			isPseudocolourBox.setSelected(settings.getBoolean(SignalWarpingDisplaySettings.PSEUDOCOLOUR_KEY));
		}
	}
}
