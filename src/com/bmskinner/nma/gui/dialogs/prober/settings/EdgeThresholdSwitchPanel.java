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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import com.bmskinner.nma.components.options.HashOptions;

/**
 * Holds a canny settings panel and a threshold settings panel. Uses a card
 * layout to switch between them
 * 
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class EdgeThresholdSwitchPanel extends DetectionSettingsPanel implements ActionListener {

	private static final Logger LOGGER = Logger.getLogger(EdgeThresholdSwitchPanel.class.getName());

	private static final String THRESHOLD_LBL = "Threshold";
	private static final String EDGE_LBL = "Edge detection";

	private JPanel cardPanel;

	private JRadioButton thresholdBtn = new JRadioButton(THRESHOLD_LBL);
	private JRadioButton edgeBtn = new JRadioButton(EDGE_LBL);
	private ButtonGroup group = new ButtonGroup();

	public EdgeThresholdSwitchPanel(final HashOptions options) {
		super(options);
		this.add(createPanel(), BorderLayout.CENTER);

	}

	private JPanel createPanel() {

		JPanel panel = new JPanel();

		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		JPanel switchPanel = makeSwitchPanel();
		cardPanel = makeCardPanel();

		panel.add(switchPanel);
		panel.add(cardPanel);

		return panel;
	}

	private JPanel makeCardPanel() {
		JPanel cardPanel = new JPanel(new CardLayout());

		SettingsPanel cannyPanel = new CannySettingsPanel(options);
		SettingsPanel thresholdPanel = new ThresholdSettingsPanel(options);

		this.addSubPanel(cannyPanel);
		this.addSubPanel(thresholdPanel);

		cardPanel.add(cannyPanel, EDGE_LBL);
		cardPanel.add(thresholdPanel, THRESHOLD_LBL);
		CardLayout cl = (CardLayout) (cardPanel.getLayout());
		cl.show(cardPanel, EDGE_LBL);

		return cardPanel;
	}

	/**
	 * A panel with the radio buttons to choose edge detection or threshold for the
	 * nucleus
	 * 
	 * @return
	 */
	private JPanel makeSwitchPanel() {
		JPanel panel = new JPanel(new FlowLayout());

		thresholdBtn.setSelected(false);
		edgeBtn.setSelected(true);
		thresholdBtn.setActionCommand(THRESHOLD_LBL);
		edgeBtn.setActionCommand(EDGE_LBL);

		// Group the radio buttons.
		group.add(thresholdBtn);
		group.add(edgeBtn);

		thresholdBtn.addActionListener(this);
		edgeBtn.addActionListener(this);

		panel.add(thresholdBtn);
		panel.add(edgeBtn);

		return panel;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(THRESHOLD_LBL)) {
			options.setBoolean(HashOptions.IS_USE_CANNY, false);

			CardLayout cl = (CardLayout) (cardPanel.getLayout());
			cl.show(cardPanel, THRESHOLD_LBL);

		}

		if (e.getActionCommand().equals(EDGE_LBL)) {
			options.setBoolean(HashOptions.IS_USE_CANNY, true);
			CardLayout cl = (CardLayout) (cardPanel.getLayout());
			cl.show(cardPanel, EDGE_LBL);
		}
		fireOptionsChangeEvent();

	}

	@Override
	public void update() {
		super.update();
		boolean showCanny = options.getBoolean(HashOptions.IS_USE_CANNY);

		CardLayout cl = (CardLayout) (cardPanel.getLayout());

		if (showCanny) {
			edgeBtn.setSelected(true);
			cl.show(cardPanel, EDGE_LBL);
		} else {
			thresholdBtn.setSelected(true);
			cl.show(cardPanel, THRESHOLD_LBL);
		}
	}

	@Override
	public void setEnabled(boolean b) {
		super.setEnabled(b);
		thresholdBtn.setEnabled(b);
		edgeBtn.setEnabled(b);

	}
}
