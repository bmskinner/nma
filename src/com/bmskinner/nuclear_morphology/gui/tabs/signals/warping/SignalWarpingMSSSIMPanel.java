package com.bmskinner.nuclear_morphology.gui.tabs.signals.warping;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Display basic MS-SSIM* scores, and hold buttons for
 * detailed MS-SSIM* values
 * @author ben
 * @since 1.19.4
 *
 */
public class SignalWarpingMSSSIMPanel 
	extends JPanel 
	implements SignalWarpingMSSSIMUpdateListener {

	private static final long serialVersionUID = 1L;
	private final JLabel messageLabel = new JLabel();
	private final SignalWarpingModelRevamp model;
	
	public SignalWarpingMSSSIMPanel(SignalWarpingModelRevamp model) {
		this.model = model;
		JPanel panel = new JPanel(new FlowLayout());
		
		panel.add(new JLabel("MS-SSIM*: "));
		panel.add(messageLabel);
		
		JButton comparisonBtn = new JButton("Detailed MS-SSIM*");
		comparisonBtn.addActionListener(e->new StructuralSimilarityComparisonDialog(model));
    	
		panel.add(comparisonBtn);
		this.add(panel, BorderLayout.CENTER);
	}

	@Override
	public void MSSSIMUpdated(String message) {
		messageLabel.setText(message);	
	}
	
}
