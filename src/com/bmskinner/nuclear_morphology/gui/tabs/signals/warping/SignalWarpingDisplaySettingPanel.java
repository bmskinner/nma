package com.bmskinner.nuclear_morphology.gui.tabs.signals.warping;

import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
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
public class SignalWarpingDisplaySettingPanel extends JPanel 
implements SignalWarpingDisplayListener {

	private static final long serialVersionUID = 1L;
	private static final String PSEUDOCOLOUR_LBL = "Pseudocolour signals";
	private static final String THRESHOLD_LBL    = "Threshold";

	private JCheckBox isPseudocolourBox;
    private JSlider   thresholdSlider;
    
    final private List<SignalWarpingDisplayListener> listeners = new ArrayList<>();
	
	public SignalWarpingDisplaySettingPanel(){
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    	JPanel displayPanel = createDisplaySettingsPanel();
    	add(displayPanel);
	}
	
	/**
	 * Add a listener for changes to the display settings
	 * @param l
	 */
	public void addSignalWarpingDisplayListener(SignalWarpingDisplayListener l) {
		listeners.add(l);
	}
	
	private JPanel createDisplaySettingsPanel() {
    	JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    	
    	isPseudocolourBox = new JCheckBox(PSEUDOCOLOUR_LBL, true);
    	
    	isPseudocolourBox.addActionListener(e->fireDisplaySettingsChanged());
    	panel.add(isPseudocolourBox);
    	
    	
    	panel.add(new JLabel(THRESHOLD_LBL));
    	
    	thresholdSlider = new JSlider(0, SignalWarpingModel.THRESHOLD_ALL_VISIBLE);
    	thresholdSlider.setVisible(true);
    	thresholdSlider.addChangeListener(e->fireDisplaySettingsChanged());
    	panel.add(thresholdSlider);    	    	    	
    	return panel;
    }
	
	private void fireDisplaySettingsChanged(){
		
		SignalWarpingDisplaySettings settings = new SignalWarpingDisplaySettings();
		settings.setBoolean(SignalWarpingDisplaySettings.PSEUDOCOLOUR_KEY, 
				isPseudocolourBox.isSelected());
		
		int value = SignalWarpingModel.THRESHOLD_ALL_VISIBLE - thresholdSlider.getValue();
		settings.setInt(SignalWarpingDisplaySettings.THRESHOLD_KEY, 
				value);
		
		for(SignalWarpingDisplayListener l :listeners) {
			l.signalWarpingDisplayChanged(settings);
		}
	}

	@Override
	public void signalWarpingDisplayChanged(@NonNull SignalWarpingDisplaySettings settings) {
		
		if(settings.getBooleanKeys().contains(SignalWarpingDisplaySettings.THRESHOLD_KEY)) {
			thresholdSlider.setValue(UNDEFINED_CONDITION);
		}
	}
}
