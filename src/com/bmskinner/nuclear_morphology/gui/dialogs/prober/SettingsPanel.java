package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * The class from which all detection settings panels will derive
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public abstract class SettingsPanel extends JPanel implements Loggable, OptionsChangeListener {
	
	private List<SettingsPanel> subPanels = new ArrayList<SettingsPanel>();
	List<OptionsChangeListener> optionsListeners = new ArrayList<OptionsChangeListener>();
	
	protected String[] channelOptionStrings = {"Greyscale", "Red", "Green", "Blue"};
	
	public SettingsPanel(){
		super();
	}
	
	/**
	 * Add the given panel as a subpanel. Updates to this panel
	 * will cause the sub panel to update also. OptionsChangeEvents
	 * from subpanels will be passed upwards.
	 * @param panel
	 */
	protected void addSubPanel(SettingsPanel panel){
		subPanels.add(panel);
	}
	
	/**
	 * Remove the given sub panel if present
	 * @param panel
	 */
	protected void removeSubPanel(SettingsPanel panel){
		subPanels.remove(panel);
	}
	
	/**
	 * Test if the given panel is a sub panel of this
	 * or one of the sub-panels of this panel.
	 * @param panel the panel to test
	 * @return
	 */
	protected boolean hasSubPanel(SettingsPanel panel){
		
		if(subPanels.contains(panel)){
			return true;
		}
		for(SettingsPanel p : subPanels){
			if(p.hasSubPanel(panel)){
				return true;
			}
		}
		return false;
	}
	
	protected void update(){

		for(SettingsPanel p : subPanels){
			p.update();
		}
	}
	
	/**
	 * Add components to a container via a list
	 * @param labels the list of labels
	 * @param fields the list of components
	 * @param gridbag the layout
	 * @param container the container to add the labels and fields to
	 */
	protected void addLabelTextRows(List<JLabel> labels,
			List<Component> fields,
			GridBagLayout gridbag,
			Container container) {
		
		JLabel[] labelArray = labels.toArray(new JLabel[0]);
		Component[] fieldArray = fields.toArray(new Component[0]);
		
		addLabelTextRows(labelArray, fieldArray, gridbag, container);
		
	}
	
	/**
	 * Add components to a container via arrays
	 * @param labels the list of labels
	 * @param fields the list of components
	 * @param gridbag the layout
	 * @param container the container to add the labels and fields to
	 */
	protected void addLabelTextRows(JLabel[] labels,
			Component[] fields,
			GridBagLayout gridbag,
			Container container) {
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.EAST;
		int numLabels = labels.length;

		for (int i = 0; i < numLabels; i++) {
			c.gridwidth = 1; //next-to-last
			c.fill = GridBagConstraints.NONE;      //reset to default
			c.weightx = 0.0;                       //reset to default
			container.add(labels[i], c);

			Dimension minSize = new Dimension(10, 5);
			Dimension prefSize = new Dimension(10, 5);
			Dimension maxSize = new Dimension(Short.MAX_VALUE, 5);
			c.gridwidth = GridBagConstraints.RELATIVE; //next-to-last
			c.fill = GridBagConstraints.NONE;      //reset to default
			c.weightx = 0.0;                       //reset to default
			container.add(new Box.Filler(minSize, prefSize, maxSize),c);

			c.gridwidth = GridBagConstraints.REMAINDER;     //end row
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
			container.add(fields[i], c);
		}
	}
	
	public void addOptionsChangeListener(OptionsChangeListener l){
		optionsListeners.add(l);
	}
	
	public void removeOptionsChangeListener(OptionsChangeListener l){
		optionsListeners.remove(l);
	}
	
	protected void fireOptionsChangeEvent(){
		OptionsChangeEvent e = new OptionsChangeEvent(this);
		
		Iterator<OptionsChangeListener> it = optionsListeners.iterator();
		
		while(it.hasNext()){
			OptionsChangeListener l = it.next();
			l.optionsChangeEventReceived(e);
		}
	}
	
	@Override
	public void optionsChangeEventReceived(OptionsChangeEvent e) {
//		log(this.getClass().getSimpleName()+": Heard options changed");
		if(this.hasSubPanel((SettingsPanel) e.getSource())){
			update();
		} else {
			fireOptionsChangeEvent();
		}
		
		
		
	}
}
