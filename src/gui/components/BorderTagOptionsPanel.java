package gui.components;

import java.util.HashMap;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

import components.generic.BorderTag;
import components.generic.BorderTag.BorderTagType;

@SuppressWarnings("serial")
public class BorderTagOptionsPanel extends EnumeratedOptionsPanel {
	
	private Map<BorderTag, JRadioButton> map  = new  HashMap<BorderTag, JRadioButton>();
	
	public BorderTagOptionsPanel(){
		
		super();
		final ButtonGroup group = new ButtonGroup();
		
		for(BorderTag type : BorderTag.values(BorderTagType.CORE)){
			JRadioButton button = new JRadioButton(type.toString());
			button.setActionCommand(type.toString());
			button.addActionListener(this);
			this.add(button);
			group.add(button);
			map.put(type, button);
		}
		// Set the default
		map.get(BorderTag.REFERENCE_POINT).setSelected(true);
		
	}
	
	public void setEnabled(boolean b){
		for(BorderTag type : BorderTag.values(BorderTagType.CORE)){
			map.get(type).setEnabled(b);
		}
	}

	/**
	 * Get the selected profile type, or null
	 * @return
	 */
	public BorderTag getSelected(){
		for(BorderTag type : BorderTag.values(BorderTagType.CORE)){
			JRadioButton button = map.get(type);
			if(button.isSelected()){
				return type;
			}
		}
		return null;
	}

}
