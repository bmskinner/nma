package gui.components;

import javax.swing.JCheckBox;

@SuppressWarnings("serial")
public class ProbabilityDensityCheckboxPanel extends EnumeratedOptionsPanel {

	
	private JCheckBox    checkBox 	= new JCheckBox("Probability density function");

	public ProbabilityDensityCheckboxPanel(){
		super();


		// checkbox to select raw or normalised profiles
		checkBox.setSelected(false);
		checkBox.addActionListener(this);
		this.add(checkBox);

	}


	public boolean isSelected(){
		return this.checkBox.isSelected();
	}

	public void setEnabled(boolean b){

		checkBox.setEnabled(b);
	}

}
