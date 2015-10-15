package gui.components;

import javax.swing.JCheckBox;


@SuppressWarnings("serial")
public class ProfileMarkersOptionsPanel extends EnumeratedOptionsPanel {
	
	private JCheckBox   checkBox 	= new JCheckBox("Show markers");	
	
	public ProfileMarkersOptionsPanel(){
		super();

		// checkbox to select raw or normalised profiles
		checkBox.setSelected(true);
		checkBox.addActionListener(this);
		this.add(checkBox);
	
	}
	
	public boolean showMarkers(){
		return this.checkBox.isSelected();
	}

}
