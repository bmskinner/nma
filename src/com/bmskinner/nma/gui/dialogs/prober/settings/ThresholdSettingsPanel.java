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

import java.awt.GridBagLayout;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.logging.Loggable;

@SuppressWarnings("serial")
public class ThresholdSettingsPanel extends DetectionSettingsPanel {

	private static final Logger LOGGER = Logger.getLogger(ThresholdSettingsPanel.class.getName());

	private static final Integer MIN_RANGE = Integer.valueOf(0);
	private static final Integer MAX_RANGE = Integer.valueOf(255);
	private static final Integer STEP = Integer.valueOf(1);

	private static final String THRESHOLD_LBL = "Threshold";
	private static final String WATERSHED_LBL = "Watershed";

	private JSpinner thresholdSpinner;
	private JCheckBox watershedBtn = new JCheckBox();

	public ThresholdSettingsPanel(final HashOptions options) {
		super(options);

		createPanel();
	}

	private void createPanel() {
		this.setLayout(new GridBagLayout());

		thresholdSpinner = new JSpinner(
				new SpinnerNumberModel(Integer.valueOf(options.getInt(HashOptions.THRESHOLD)),
						MIN_RANGE, MAX_RANGE, STEP));

		thresholdSpinner.addChangeListener(e -> {
			try {
				thresholdSpinner.commitEdit();
				options.setInt(HashOptions.THRESHOLD,
						((Integer) thresholdSpinner.getValue()).intValue());
				fireOptionsChangeEvent();
			} catch (ParseException e1) {
				LOGGER.warning("Parsing error in JSpinner");
				LOGGER.log(Loggable.STACK, "Parsing error in JSpinner", e1);
			}
		});

		watershedBtn.addActionListener(e -> {
			options.setBoolean(HashOptions.IS_USE_WATERSHED, watershedBtn.isSelected());
			fireOptionsChangeEvent();
		});

		List<JLabel> labelList = new ArrayList<>();
		List<JComponent> fieldList = new ArrayList<>();

		labelList.add(new JLabel(THRESHOLD_LBL));
		labelList.add(new JLabel(WATERSHED_LBL));

		JLabel[] labels = labelList.toArray(new JLabel[0]);

		fieldList.add(thresholdSpinner);
		fieldList.add(watershedBtn);

		JComponent[] fields = fieldList.toArray(new JComponent[0]);

		addLabelTextRows(labels, fields, this);
	}

	@Override
	protected void update() {
		super.update();
		isUpdating = true;
		thresholdSpinner.setValue(options.getInt(HashOptions.THRESHOLD));
		watershedBtn.setSelected(options.getBoolean(HashOptions.IS_USE_WATERSHED));
		isUpdating = false;
	}

	@Override
	public void setEnabled(boolean b) {
		super.setEnabled(b);
		thresholdSpinner.setEnabled(b);
		watershedBtn.setEnabled(b);
	}
}
