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
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.bmskinner.nma.components.options.HashOptions;

/**
 * Provides basic settings parameters for detection of components. Values such
 * as size, circularity and scale.
 * 
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class ComponentSizeSettingsPanel extends DetectionSettingsPanel {

	private static final Logger LOGGER = Logger
			.getLogger(ComponentSizeSettingsPanel.class.getName());

	private static final String MIN_SIZE_LBL = "Min area (pixels)";
	private static final String MAX_SIZE_LBL = "Max area (pixels)";
	private static final String MIN_CIRC_LBL = "Min circ";
	private static final String MAX_CIRC_LBL = "Max circ";

	private static final Integer MIN_RANGE_SIZE = 5;
	private static final double MIN_RANGE_CIRC = 0;
	private static final double MAX_RANGE_CIRC = 1;

	private static final Integer SIZE_STEP_SIZE = 1;
	private static final double CIRC_STEP_SIZE = 0.05;

	private JSpinner minSizeSpinner;
	private JSpinner maxSizeSpinner;
	private JSpinner minCircSpinner;
	private JSpinner maxCircSpinner;

	public ComponentSizeSettingsPanel(final HashOptions options) {
		super(options);
		this.add(createPanel(), BorderLayout.CENTER);

	}

	/**
	 * Create the settings spinners based on the input options
	 */
	private void createSpinners() {
		minSizeSpinner = new JSpinner(
				new SpinnerNumberModel(Integer.valueOf(options.getInt(HashOptions.MIN_SIZE_PIXELS)),
						MIN_RANGE_SIZE, null, SIZE_STEP_SIZE));

		maxSizeSpinner = new JSpinner(
				new SpinnerNumberModel(Integer.valueOf(options.getInt(HashOptions.MAX_SIZE_PIXELS)),
						MIN_RANGE_SIZE, null, SIZE_STEP_SIZE));

		minCircSpinner = new JSpinner(
				new SpinnerNumberModel(options.getDouble(HashOptions.MIN_CIRC), MIN_RANGE_CIRC,
						MAX_RANGE_CIRC, CIRC_STEP_SIZE));

		maxCircSpinner = new JSpinner(
				new SpinnerNumberModel(options.getDouble(HashOptions.MAX_CIRC), MIN_RANGE_CIRC,
						MAX_RANGE_CIRC, CIRC_STEP_SIZE));

		Dimension dim = new Dimension(BOX_WIDTH, BOX_HEIGHT);
		minSizeSpinner.setPreferredSize(dim);
		maxSizeSpinner.setPreferredSize(dim);
		minCircSpinner.setPreferredSize(dim);
		maxCircSpinner.setPreferredSize(dim);

		minSizeSpinner.addChangeListener(e -> {
			updateOptions(HashOptions.MIN_SIZE_PIXELS, (Integer) minSizeSpinner.getValue());
		});

		maxSizeSpinner.addChangeListener(e -> {
			updateOptions(HashOptions.MAX_SIZE_PIXELS, (Integer) maxSizeSpinner.getValue());
		});

		minCircSpinner.addChangeListener(e -> {
			updateOptions(HashOptions.MIN_CIRC, (Double) minCircSpinner.getValue());
		});

		maxCircSpinner.addChangeListener(e -> {
			updateOptions(HashOptions.MAX_CIRC, (Double) maxCircSpinner.getValue());
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

		labels.add(new JLabel(MIN_SIZE_LBL));
		labels.add(new JLabel(MAX_SIZE_LBL));
		labels.add(new JLabel(MIN_CIRC_LBL));
		labels.add(new JLabel(MAX_CIRC_LBL));

		List<Component> fields = new ArrayList<>();

		fields.add(minSizeSpinner);
		fields.add(maxSizeSpinner);
		fields.add(minCircSpinner);
		fields.add(maxCircSpinner);

		addLabelTextRows(labels, fields, panel);

		return panel;
	}

	/**
	 * Update the spinners to current options values
	 */
	@Override
	protected void update() {
		super.update();
		minSizeSpinner.setValue(options.getInt(HashOptions.MIN_SIZE_PIXELS));
		maxSizeSpinner.setValue(options.getInt(HashOptions.MAX_SIZE_PIXELS));
		minCircSpinner.setValue(options.getDouble(HashOptions.MIN_CIRC));
		maxCircSpinner.setValue(options.getDouble(HashOptions.MAX_CIRC));
	}

	@Override
	public void setEnabled(boolean b) {
		super.setEnabled(b);
		minSizeSpinner.setEnabled(b);
		maxSizeSpinner.setEnabled(b);
		minCircSpinner.setEnabled(b);
		maxCircSpinner.setEnabled(b);

	}
}
