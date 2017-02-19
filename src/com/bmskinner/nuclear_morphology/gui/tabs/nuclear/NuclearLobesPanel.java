package com.bmskinner.nuclear_morphology.gui.tabs.nuclear;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.gui.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;

@SuppressWarnings("serial")
public class NuclearLobesPanel extends DetailPanel {
	
	
	private static final String RUN_LOBE_DETECTION_LBL = "Run lobe detection";
	private JButton runLobeDetectionBtn;
	
	public NuclearLobesPanel(){
		
		this.setLayout(new BorderLayout());
		
		JPanel header = createHeader();
		this.add(header, BorderLayout.NORTH);
				
	}
	
	private JPanel createHeader(){
		JPanel panel = new JPanel(new FlowLayout());
				
		runLobeDetectionBtn = new JButton(RUN_LOBE_DETECTION_LBL);
		runLobeDetectionBtn.addActionListener( a -> {
			fireSignalChangeEvent(SignalChangeEvent.LOBE_DETECTION);
		}  );
		runLobeDetectionBtn.setEnabled(false);
		
		panel.add(runLobeDetectionBtn);
		return panel;
	}
	
	@Override
	protected void updateSingle() {
		runLobeDetectionBtn.setEnabled(true);		
	}
	
}
