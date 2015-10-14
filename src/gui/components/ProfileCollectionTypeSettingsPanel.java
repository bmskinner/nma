package gui.components;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import components.CellCollection.ProfileCollectionType;

@SuppressWarnings("serial")
public class ProfileCollectionTypeSettingsPanel extends JPanel implements ActionListener {
	
	private Map<ProfileCollectionType, JRadioButton> map  = new  HashMap<ProfileCollectionType, JRadioButton>();
	private List<ActionListener> listeners = new ArrayList<ActionListener>();
	
	public ProfileCollectionTypeSettingsPanel(){
		
		this.setLayout(new FlowLayout());
		final ButtonGroup group = new ButtonGroup();
		
		for(ProfileCollectionType type : ProfileCollectionType.values()){
			JRadioButton button = new JRadioButton(type.toString());
			button.setActionCommand(type.toString());
			button.addActionListener(this);
			this.add(button);
			group.add(button);
			map.put(type, button);
		}
		// Set the default
		map.get(ProfileCollectionType.FRANKEN).setSelected(true);
		
	}
	
	/**
	 * Get the selected profile type, or null
	 * @return
	 */
	public ProfileCollectionType getSelected(){
		for(ProfileCollectionType type : ProfileCollectionType.values()){
			JRadioButton button = map.get(type);
			if(button.isSelected()){
				return type;
			}
		}
		return null;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		for(ActionListener a: listeners) {
			a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, e.getActionCommand()) {
			});
		}
	}
	
	public void addActionListener(ActionListener a){
		this.listeners.add(a);
	}


}
