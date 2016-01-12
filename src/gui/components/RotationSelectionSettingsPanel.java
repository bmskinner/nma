package gui.components;


import gui.RotationMode;

import java.util.HashMap;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

@SuppressWarnings("serial")
public class RotationSelectionSettingsPanel extends EnumeratedOptionsPanel {
		
		private Map<RotationMode, JRadioButton> map  = new  HashMap<RotationMode, JRadioButton>();
		
		public RotationSelectionSettingsPanel(){
			super();
			
			final ButtonGroup group = new ButtonGroup();

			for(RotationMode type : RotationMode.values()){
				JRadioButton button = new JRadioButton(type.toString());
				button.setActionCommand(type.toString());
				button.addActionListener(this);
				this.add(button);
				group.add(button);
				map.put(type, button);
			}
			// Set the default
			map.get(RotationMode.ACTUAL).setSelected(true);		
		}
			
		public RotationMode getSelected(){
			for(RotationMode type : RotationMode.values()){
				JRadioButton button = map.get(type);
				if(button.isSelected()){
					return type;
				}
			}
			return null;
		}
		
		public void setEnabled(boolean b){

			for(RotationMode type : RotationMode.values()){
				map.get(type).setEnabled(b);
			}
		}
	}
