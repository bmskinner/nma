package com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.components.options.IMutableAnalysisOptions;
@SuppressWarnings("serial")
public class MiscNucleusSettingsPanel extends SettingsPanel {

	private static final String KEEP_FAILED_LBL    = "Keep filtered nuclei";
	

	private IMutableAnalysisOptions options;

	private JCheckBox keepFailedheckBox = new JCheckBox("", false);

	public MiscNucleusSettingsPanel(final IMutableAnalysisOptions op){
		super();
		options = op;
		this.add(createPanel(), BorderLayout.CENTER);
	}

	/**
	 * Create the settings spinners based on the input options
	 */
	private void createSpinners(){

		Dimension dim = new Dimension(BOX_WIDTH, BOX_HEIGHT);


		keepFailedheckBox.addActionListener( e -> {
			options.setKeepFailedCollections(keepFailedheckBox.isSelected());
			fireOptionsChangeEvent();
		});
		keepFailedheckBox.setPreferredSize(dim);
		
		
		
		
		
		
	}

	private JPanel createPanel(){

		this.createSpinners();

		JPanel panel = new JPanel();

		panel.setLayout(new GridBagLayout());

		List<JLabel> labels = new ArrayList<JLabel>();
		labels.add(new JLabel(KEEP_FAILED_LBL));


		List<Component> fields = new ArrayList<Component>();

		fields.add(keepFailedheckBox);

		addLabelTextRows(labels, fields, panel );

		return panel;
	}

	/**
	 * Update the spinners to current options values 
	 */
	@Override
	protected void update(){
		super.update();
		keepFailedheckBox.setSelected(options.isKeepFailedCollections());
	}
	
	@Override
	public void setEnabled(boolean b){
		super.setEnabled(b);
		keepFailedheckBox.setEnabled(b);

	}

}
