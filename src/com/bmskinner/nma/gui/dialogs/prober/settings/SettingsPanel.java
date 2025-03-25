/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nma.gui.dialogs.prober.settings;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.bmskinner.nma.gui.dialogs.prober.GenericImageProberPanel.PanelUpdatingEvent;
import com.bmskinner.nma.gui.dialogs.prober.GenericImageProberPanel.PanelUpdatingEventListener;
import com.bmskinner.nma.gui.dialogs.prober.OptionsChangeEvent;
import com.bmskinner.nma.gui.dialogs.prober.OptionsChangeListener;
import com.bmskinner.nma.gui.dialogs.prober.ProberReloadEvent;
import com.bmskinner.nma.gui.dialogs.prober.ProberReloadEventListener;

/**
 * The class from which all detection settings panels will derive
 * 
 * @author Ben Skinner
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public abstract class SettingsPanel extends JPanel
		implements OptionsChangeListener, PanelUpdatingEventListener {

	private static final Logger LOGGER = Logger.getLogger(SettingsPanel.class.getName());

	protected static final int BOX_WIDTH = 80;
	protected static final int BOX_HEIGHT = 20;

	private List<SettingsPanel> subPanels = new ArrayList<>();
	private List<OptionsChangeListener> optionsListeners = new ArrayList<>();
	private List<ProberReloadEventListener> proberListeners = new ArrayList<>();

	protected String[] channelOptionStrings = { "Greyscale", "Red", "Green", "Blue" };

	/**
	 * Used to prevent OptionsChangeEvents firing while panel is updating settings
	 * to match attached options
	 */
	protected boolean isUpdating = false;

	public SettingsPanel() {
		super();
	}

	/**
	 * Add the given panel as a subpanel. Updates to this panel will cause the sub
	 * panel to update also. OptionsChangeEvents from subpanels will be passed
	 * upwards.
	 * 
	 * @param panel
	 */
	protected void addSubPanel(SettingsPanel panel) {
		subPanels.add(panel);
		panel.addOptionsChangeListener(this);
	}

	/**
	 * Remove the given sub panel if present
	 * 
	 * @param panel
	 */
	protected void removeSubPanel(SettingsPanel panel) {
		subPanels.remove(panel);
		panel.removeOptionsChangeListener(this);
	}

	/**
	 * Test if the given panel is a sub panel of this or one of the sub-panels of
	 * this panel.
	 * 
	 * @param panel the panel to test
	 * @return
	 */
	protected boolean hasSubPanel(SettingsPanel panel) {

		if (subPanels.contains(panel))
			return true;
		for (SettingsPanel p : subPanels) {
			if (p.hasSubPanel(panel))
				return true;
		}
		return false;
	}

	/**
	 * Get the sub panels
	 * 
	 * @return
	 */
	protected List<SettingsPanel> getSubPanels() {
		return subPanels;
	}

	protected void update() {

		for (SettingsPanel p : subPanels) {
			p.removeOptionsChangeListener(this); // Don't trigger reload events
			p.update();
			p.addOptionsChangeListener(this);
		}
	}

	/**
	 * Add components to a container via a list
	 * 
	 * @param labels    the list of labels
	 * @param fields    the list of components
	 * @param gridbag   the layout
	 * @param container the container to add the labels and fields to
	 */
	protected void addLabelTextRows(List<JLabel> labels, List<? extends Component> fields,
			Container container) {

		JLabel[] labelArray = labels.toArray(new JLabel[0]);
		Component[] fieldArray = fields.toArray(new Component[0]);

		addLabelTextRows(labelArray, fieldArray, container);

	}

	/**
	 * Add components to a container via arrays
	 * 
	 * @param labels    the list of labels
	 * @param fields    the list of components
	 * @param gridbag   the layout
	 * @param container the container to add the labels and fields to
	 */
	protected void addLabelTextRows(JLabel[] labels, Component[] fields,

			Container container) {
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.EAST;
		int numLabels = labels.length;

		for (int i = 0; i < numLabels; i++) {
			c.gridwidth = 1; // next-to-last
			c.fill = GridBagConstraints.NONE; // reset to default
			c.weightx = 0.0; // reset to default
			container.add(labels[i], c);

			Dimension minSize = new Dimension(10, 5);
			Dimension prefSize = new Dimension(10, 5);
			Dimension maxSize = new Dimension(Short.MAX_VALUE, 5);
			c.gridwidth = GridBagConstraints.RELATIVE; // next-to-last
			c.fill = GridBagConstraints.NONE; // reset to default
			c.weightx = 0.0; // reset to default
			container.add(new Box.Filler(minSize, prefSize, maxSize), c);

			c.gridwidth = GridBagConstraints.REMAINDER; // end row
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
			container.add(fields[i], c);
		}
	}

	public void addOptionsChangeListener(OptionsChangeListener l) {
		optionsListeners.add(l);
	}

	public void removeOptionsChangeListener(OptionsChangeListener l) {
		optionsListeners.remove(l);
	}

	/**
	 * Fire an event to all listeners that the options in this panel have been
	 * changed via the GUI
	 */
	protected void fireOptionsChangeEvent() {
		if (!isUpdating) {
			OptionsChangeEvent e = new OptionsChangeEvent(this);

			Iterator<OptionsChangeListener> it = optionsListeners.iterator();

			while (it.hasNext()) {
				OptionsChangeListener l = it.next();
				l.optionsChangeEventReceived(e);
			}
		}
	}

	public void addProberReloadEventListener(ProberReloadEventListener l) {
		proberListeners.add(l);
	}

	public void removeProberReloadEventListener(ProberReloadEventListener l) {
		proberListeners.remove(l);
	}

	/**
	 * Fire an event to all listeners that the image prober should reload all images
	 */
	protected void fireProberReloadEvent() {

		ProberReloadEvent e = new ProberReloadEvent(this);

		Iterator<ProberReloadEventListener> it = proberListeners.iterator();

		while (it.hasNext()) {
			ProberReloadEventListener l = it.next();
			l.proberReloadEventReceived(e);
		}
	}

	@Override
	public void optionsChangeEventReceived(OptionsChangeEvent e) {

		// Pass upwards
		if (this.hasSubPanel((SettingsPanel) e.getSource())) {
			update();
			fireOptionsChangeEvent();
		}

	}

	@Override
	public void setEnabled(boolean b) {
		LOGGER.finest(this.getClass().getSimpleName() + ": Setting updating " + b);
		for (Component c : this.getComponents()) {
			c.setEnabled(b);
		}
		for (SettingsPanel p : subPanels) {
			p.setEnabled(b);
		}
	}

	@Override
	public void panelUpdatingEventReceived(PanelUpdatingEvent e) {
		if (e.getType() == PanelUpdatingEvent.UPDATING)
			this.setEnabled(false);

		if (e.getType() == PanelUpdatingEvent.COMPLETE)
			this.setEnabled(true);

		for (SettingsPanel s : this.getSubPanels()) {
			s.panelUpdatingEventReceived(e);
		}
	}

}
