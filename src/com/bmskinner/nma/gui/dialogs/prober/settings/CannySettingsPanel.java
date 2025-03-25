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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.bmskinner.nma.components.options.HashOptions;


/**
 * A panel that allows changes to be made to a CannyOptions
 * 
 * @author Ben Skinner
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class CannySettingsPanel extends SettingsPanel implements ActionListener {

	private static final Logger LOGGER = Logger.getLogger(CannySettingsPanel.class.getName());

	public static final double THRESHOLD_STEP_SIZE = 0.05;

	public static final double THRESHOLD_MIN = 0;
	public static final double KERNEL_RADIUS_MIN = 0;

	public static final double LOW_THRESHOLD_MAX = 10;
	public static final double HIGH_THRESHOLD_MAX = 20;
	public static final double KERNEL_RADIUS_MAX = 20;

	public static final Integer CANNY_KERNEL_WIDTH_MIN = Integer.valueOf(1);
	public static final Integer CANNY_KERNEL_WIDTH_MAX = Integer.valueOf(50);
	public static final Integer CANNY_KERNEL_WIDTH_STEP = Integer.valueOf(1);

	public static final Integer CLOSING_RADIUS_MIN = Integer.valueOf(1);
	public static final Integer CLOSING_RADIUS_MAX = Integer.valueOf(100);
	public static final Integer CLOSING_RADIUS_STEP = Integer.valueOf(1);

	private static final String LOW_THRESHOLD_LBL = "Canny low threshold";
	private static final String HIGH_THRESHOLD_LBL = "Canny high threshold";
	private static final String KERNEL_RADIUS_LBL = "Canny kernel radius";
	private static final String KERNEL_WIDTH_LBL = "Canny kernel width";
	private static final String CLOSING_RADIUS_LBL = "Gap closing radius";
	private static final String WATERSHED_LBL = "Watershed";
	private static final String EDGE_FILTER_LBL = "Filter poor edge detection";

	private JSpinner cannyLowThreshold;
	private JSpinner cannyHighThreshold;
	private JSpinner cannyKernelRadius;
	private JSpinner cannyKernelWidth;
	private JSpinner closingObjectRadiusSpinner;
	private JCheckBox watershedBtn = new JCheckBox();

	/**
	 * Should poor edge detection be filtered out?
	 */
	private JCheckBox removePoorEdges = new JCheckBox();

	private HashOptions options;

	public CannySettingsPanel(final HashOptions options) {
		this.options = options;
		createSpinners();
		createPanel();
	}

	/**
	 * Update the display to the options
	 * 
	 */
	@Override
	protected void update() {
		super.update();
		isUpdating = true;
		cannyLowThreshold.setValue((double) options.getFloat(HashOptions.CANNY_LOW_THRESHOLD_FLT));
		cannyHighThreshold
				.setValue((double) options.getFloat(HashOptions.CANNY_HIGH_THRESHOLD_FLT));
		cannyKernelRadius.setValue((double) options.getFloat(HashOptions.CANNY_KERNEL_RADIUS_FLT));
		cannyKernelWidth.setValue(options.getInt(HashOptions.CANNY_KERNEL_WIDTH_INT));
		closingObjectRadiusSpinner.setValue(options.getInt(HashOptions.GAP_CLOSING_RADIUS_INT));

		watershedBtn.setSelected(options.getBoolean(HashOptions.IS_USE_WATERSHED));

		removePoorEdges.setSelected(options.getBoolean(HashOptions.IS_RULESET_EDGE_FILTER));

		isUpdating = false;
	}

	public void set(final HashOptions options) {
		this.options.set(options);
		update();

	}

	/**
	 * Create the spinners with the default options in the CannyOptions CannyOptions
	 * must therefore have been assigned defaults
	 */
	private void createSpinners() {

		cannyLowThreshold = new JSpinner(new SpinnerNumberModel(
				options.getFloat(HashOptions.CANNY_LOW_THRESHOLD_FLT), THRESHOLD_MIN,
				LOW_THRESHOLD_MAX, THRESHOLD_STEP_SIZE));

		cannyHighThreshold = new JSpinner(new SpinnerNumberModel(
				options.getFloat(HashOptions.CANNY_HIGH_THRESHOLD_FLT), THRESHOLD_MIN,
				HIGH_THRESHOLD_MAX, THRESHOLD_STEP_SIZE));

		cannyKernelRadius = new JSpinner(new SpinnerNumberModel(
				options.getFloat(HashOptions.CANNY_KERNEL_RADIUS_FLT), KERNEL_RADIUS_MIN,
				KERNEL_RADIUS_MAX, THRESHOLD_STEP_SIZE));

		cannyKernelWidth = new JSpinner(new SpinnerNumberModel(
				Integer.valueOf(options.getInt(HashOptions.CANNY_KERNEL_WIDTH_INT)),
				CANNY_KERNEL_WIDTH_MIN, CANNY_KERNEL_WIDTH_MAX, CANNY_KERNEL_WIDTH_STEP));

		closingObjectRadiusSpinner = new JSpinner(
				new SpinnerNumberModel(
						Integer.valueOf(options.getInt(HashOptions.GAP_CLOSING_RADIUS_INT)),
						CLOSING_RADIUS_MIN,
						CLOSING_RADIUS_MAX, CLOSING_RADIUS_STEP));

		// add the change listeners
		cannyLowThreshold.addChangeListener(e -> {
			try {
				JSpinner j = (JSpinner) e.getSource();
				cannyLowThreshold.commitEdit();

				if ((Double) j.getValue() > (Double) cannyHighThreshold.getValue()) {
					cannyLowThreshold.setValue(cannyHighThreshold.getValue());
				}
				Double doubleValue = (Double) j.getValue();
				options.setFloat(HashOptions.CANNY_LOW_THRESHOLD_FLT, doubleValue.floatValue());
				fireOptionsChangeEvent();
			} catch (ParseException e1) {
				LOGGER.warning("Parsing exception");
				LOGGER.log(Level.SEVERE, "Parsing error in JSpinner", e1);
			}

		});

		// add the change listeners
		cannyHighThreshold.addChangeListener(e -> {
			try {
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();

				if ((Double) j.getValue() < (Double) cannyLowThreshold.getValue()) {
					j.setValue(cannyLowThreshold.getValue());
				}
				Double doubleValue = (Double) j.getValue();
				options.setFloat(HashOptions.CANNY_HIGH_THRESHOLD_FLT, doubleValue.floatValue());
				fireOptionsChangeEvent();
			} catch (ParseException e1) {
				LOGGER.warning("Parsing exception");
				LOGGER.log(Level.SEVERE, "Parsing error in JSpinner", e1);
			}

		});

		cannyKernelRadius.addChangeListener(e -> {
			try {
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				Double doubleValue = (Double) j.getValue();
				options.setFloat(HashOptions.CANNY_KERNEL_RADIUS_FLT, doubleValue.floatValue());
				fireOptionsChangeEvent();
			} catch (ParseException e1) {
				LOGGER.warning("Parsing exception");
				LOGGER.log(Level.SEVERE, "Parsing error in JSpinner", e1);
			}

		});

		cannyKernelWidth.addChangeListener(e -> {
			try {
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				Integer value = (Integer) j.getValue();
				options.setInt(HashOptions.CANNY_KERNEL_WIDTH_INT, value.intValue());
				fireOptionsChangeEvent();
			} catch (ParseException e1) {
				LOGGER.warning("Parsing exception");
				LOGGER.log(Level.SEVERE, "Parsing error in JSpinner", e1);
			}

		});

		closingObjectRadiusSpinner.addChangeListener(e -> {
			try {
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				options.setInt(HashOptions.GAP_CLOSING_RADIUS_INT, (int) j.getValue());
				fireOptionsChangeEvent();
			} catch (ParseException e1) {
				LOGGER.warning("Parsing exception");
				LOGGER.log(Level.SEVERE, "Parsing error in JSpinner", e1);
			}

		});

		watershedBtn.addActionListener(e -> {
			options.setBoolean(HashOptions.IS_USE_WATERSHED, watershedBtn.isSelected());
			fireOptionsChangeEvent();
		});

		removePoorEdges.addActionListener(e -> {
			options.setBoolean(HashOptions.IS_RULESET_EDGE_FILTER, removePoorEdges.isSelected());
			fireOptionsChangeEvent();
		});

	}

	private void createPanel() {

		this.setLayout(new GridBagLayout());

		List<JLabel> labelList = new ArrayList<>();
		List<JComponent> fieldList = new ArrayList<>();

		labelList.add(new JLabel(LOW_THRESHOLD_LBL));
		labelList.add(new JLabel(HIGH_THRESHOLD_LBL));
		labelList.add(new JLabel(KERNEL_RADIUS_LBL));
		labelList.add(new JLabel(KERNEL_WIDTH_LBL));
		labelList.add(new JLabel(CLOSING_RADIUS_LBL));
		labelList.add(new JLabel(EDGE_FILTER_LBL));
		labelList.add(new JLabel(WATERSHED_LBL));

		JLabel[] labels = labelList.toArray(new JLabel[0]);

		fieldList.add(cannyLowThreshold);
		fieldList.add(cannyHighThreshold);
		fieldList.add(cannyKernelRadius);
		fieldList.add(cannyKernelWidth);
		fieldList.add(closingObjectRadiusSpinner);
		fieldList.add(removePoorEdges);
		fieldList.add(watershedBtn);

		JComponent[] fields = fieldList.toArray(new JComponent[0]);

		addLabelTextRows(labels, fields, this);

	}

	@Override
	public void setEnabled(boolean b) {
		super.setEnabled(b);

		cannyKernelRadius.setEnabled(b);
		cannyKernelWidth.setEnabled(b);
		closingObjectRadiusSpinner.setEnabled(b);

		watershedBtn.setEnabled(b);
		removePoorEdges.setEnabled(b);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		fireOptionsChangeEvent();
	}
}
