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
import java.awt.Component;
import java.awt.GridBagLayout;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.io.ImageImporter;
import com.bmskinner.nma.logging.Loggable;

/**
 * Panel for image channel settings
 * 
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class ImageChannelSettingsPanel extends DetectionSettingsPanel {

	private static final Logger LOGGER = Logger
			.getLogger(ImageChannelSettingsPanel.class.getName());

	private static final double SCALE_STEP_SIZE = 1;
	private static final double SCALE_MIN = 1;
	private static final double SCALE_MAX = 100000;

	private static final String CHANNEL_LBL = "Channel";
	private static final String SCALE_LBL = "Scale (pixels/micron)";

	private JComboBox<String> channelBox = new JComboBox<>(channelOptionStrings);
	private JSpinner scaleSpinner;

	public ImageChannelSettingsPanel(final HashOptions options) {
		super(options);
		this.add(createPanel(), BorderLayout.CENTER);

	}

	/**
	 * Create the settings spinners based on the input options
	 */
	private void createSpinners() {

		channelBox.setSelectedItem(
				ImageImporter.channelIntToName(options.getInt(HashOptions.CHANNEL)));
		channelBox.addActionListener(e -> {

			int channel = 0;
			switch (channelBox.getSelectedItem().toString()) {
			case "Red":
				channel = ImageImporter.RGB_RED;
				break;
			case "Green":
				channel = ImageImporter.RGB_GREEN;
				break;
			default:
				channel = ImageImporter.RGB_BLUE;
			}
			updateOptions(HashOptions.CHANNEL, channel);
		});

		scaleSpinner = new JSpinner(new SpinnerNumberModel(options.getDouble(HashOptions.SCALE),
				SCALE_MIN, SCALE_MAX, SCALE_STEP_SIZE));

		scaleSpinner.addChangeListener(e -> {

			try {

				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();

				// Note we don't use updateOptions here becuase we don't need to
				// reload all the images when setting scale
				options.setDouble(HashOptions.SCALE, (Double) j.getValue());

			} catch (ParseException e1) {
				LOGGER.log(Loggable.STACK, "Parsing error in JSpinner", e1);
			}
		});
	}

	/**
	 * Create the panel containing the settings spinners
	 * 
	 * @return
	 */
	private JPanel createPanel() {

		this.createSpinners();

		JPanel panel = new JPanel();

		panel.setLayout(new GridBagLayout());

		List<JLabel> labels = new ArrayList<>();

		labels.add(new JLabel(CHANNEL_LBL));
		labels.add(new JLabel(SCALE_LBL));

		List<Component> fields = new ArrayList<>();

		fields.add(channelBox);
		fields.add(scaleSpinner);

		addLabelTextRows(labels, fields, panel);

		return panel;
	}

	/**
	 * Update the spinners to current options values
	 */
	@Override
	protected void update() {
		super.update();
		isUpdating = true;
		channelBox.setSelectedItem(
				ImageImporter.channelIntToName(options.getInt(HashOptions.CHANNEL)));
		scaleSpinner.setValue(options.getDouble(HashOptions.SCALE));
		isUpdating = false;
	}

	@Override
	public void setEnabled(boolean b) {
		super.setEnabled(b);
		scaleSpinner.setEnabled(b);
		channelBox.setEnabled(b);
	}
}
