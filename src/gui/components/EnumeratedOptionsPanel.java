package gui.components;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

@SuppressWarnings("serial")
public abstract class EnumeratedOptionsPanel extends JPanel implements ActionListener {

	private List<ActionListener> listeners = new ArrayList<ActionListener>();
	
	public EnumeratedOptionsPanel(){
		this.setLayout(new FlowLayout());
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		
		for(ActionListener a: listeners) {
			a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, e.getActionCommand()) {
			});
		}
	}
	
	public void addActionListener(ActionListener a){
		this.listeners.add(a);
	}
	
}
