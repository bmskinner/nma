package gui.components;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;

@SuppressWarnings("serial")
public class ProfileAlignmentOptionsPanel extends EnumeratedOptionsPanel {

	private Map<ProfileAlignment, JRadioButton> map  = new  HashMap<ProfileAlignment, JRadioButton>();
	private JCheckBox    normCheckBox 	= new JCheckBox("Normalised");	// to toggle raw or normalised segment profiles in segmentsProfileChartPanel
	
	public ProfileAlignmentOptionsPanel(){
		super();
		
		
		// checkbox to select raw or normalised profiles
		normCheckBox.setSelected(true);
		normCheckBox.addActionListener(this);
		this.add(normCheckBox);
		
		
		final ButtonGroup group = new ButtonGroup();

		for(ProfileAlignment type : ProfileAlignment.values()){
			JRadioButton button = new JRadioButton(type.toString());
			button.setActionCommand(type.toString());
			button.addActionListener(this);
			this.add(button);
			group.add(button);
			map.put(type, button);
		}
		// Set the default
		map.get(ProfileAlignment.LEFT).setSelected(true);		
	}
		
	public ProfileAlignment getSelected(){
		for(ProfileAlignment type : ProfileAlignment.values()){
			JRadioButton button = map.get(type);
			if(button.isSelected()){
				return type;
			}
		}
		return null;
	}
	
	public boolean isNormalised(){
		return this.normCheckBox.isSelected();
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		
		if(  normCheckBox.isSelected()){
			
			for(ProfileAlignment type : ProfileAlignment.values()){
				map.get(type).setEnabled(false);
			}

		} else {
			for(ProfileAlignment type : ProfileAlignment.values()){
				map.get(type).setEnabled(true);
			}
		}
	}

	public enum ProfileAlignment {

		LEFT  ("Left"),
		RIGHT ("Right");

		private String name;

		ProfileAlignment(String name){
			this.name = name;
		}

		public String toString(){
			return this.name;
		}
	}

}
