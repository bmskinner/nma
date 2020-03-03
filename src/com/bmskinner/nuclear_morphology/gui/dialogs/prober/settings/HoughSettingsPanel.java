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
package com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings;

import java.awt.GridBagLayout;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.options.IHoughDetectionOptions;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Set parameters for Hough circle detection
 * 
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class HoughSettingsPanel extends SettingsPanel {
	
	private static final Logger LOGGER = Logger.getLogger(HoughSettingsPanel.class.getName());

    public static final Integer HOUGH_MIN_RADIUS_MIN  = Integer.valueOf(5);
    public static final Integer HOUGH_MIN_RADIUS_MAX  = Integer.valueOf(100);
    public static final Integer HOUGH_MIN_RADIUS_STEP = Integer.valueOf(1);

    public static final Integer HOUGH_MAX_RADIUS_MIN  = Integer.valueOf(5);
    public static final Integer HOUGH_MAX_RADIUS_MAX  = Integer.valueOf(100);
    public static final Integer HOUGH_MAX_RADIUS_STEP = Integer.valueOf(1);

    public static final Integer NUMBER_OF_CIRCLES_MIN  = Integer.valueOf(0);
    public static final Integer NUMBER_OF_CIRCLES_MAX  = Integer.valueOf(10);
    public static final Integer NUMBER_OF_CIRCLES_STEP = Integer.valueOf(1);

    public static final Integer THRESHOLD_MIN  = Integer.valueOf(-1);
    public static final Integer THRESHOLD_MAX  = Integer.valueOf(255);
    public static final Integer THRESHOLD_STEP = Integer.valueOf(1);

    private static final String MIN_RADIUS_LBL        = "Min radius";
    private static final String MAX_RADIUS_LBL        = "Max radius";
    private static final String NUMBER_OF_CIRCLES_LBL = "Number of circles";
    private static final String THRESHOLD_LBL         = "Threshold";

    private JSpinner minRadiusSpinner;
    private JSpinner maxRadiusSpinner;
    private JSpinner numCirclesSpinner;
    private JSpinner thresholdSpinner;

    private IHoughDetectionOptions options;

    public HoughSettingsPanel(@NonNull final IHoughDetectionOptions options) {
        this.options = options;
        createSpinners();
        createPanel();
    }

    /**
     * Create the spinners with the default options in the CannyOptions
     * CannyOptions must therefore have been assigned defaults
     */
    private void createSpinners() {

        minRadiusSpinner = new JSpinner(new SpinnerNumberModel(Integer.valueOf(options.getMinRadius()),
                HOUGH_MIN_RADIUS_MIN, HOUGH_MIN_RADIUS_MAX, HOUGH_MIN_RADIUS_STEP));

        maxRadiusSpinner = new JSpinner(new SpinnerNumberModel(Integer.valueOf(options.getMaxRadius()),
                HOUGH_MAX_RADIUS_MIN, HOUGH_MAX_RADIUS_MAX, HOUGH_MAX_RADIUS_STEP));

        numCirclesSpinner = new JSpinner(new SpinnerNumberModel(Integer.valueOf(options.getNumberOfCircles()),
                NUMBER_OF_CIRCLES_MIN, NUMBER_OF_CIRCLES_MAX, NUMBER_OF_CIRCLES_STEP));

        thresholdSpinner = new JSpinner(new SpinnerNumberModel(Integer.valueOf(options.getHoughThreshold()),
                THRESHOLD_MIN, THRESHOLD_MAX, THRESHOLD_STEP));

        // add the change listeners
        minRadiusSpinner.addChangeListener(e -> {
            try {
                JSpinner j = (JSpinner) e.getSource();
                minRadiusSpinner.commitEdit();

                Integer value = (Integer) j.getValue();
                options.setMinRadius(value.intValue());
                fireOptionsChangeEvent();
            } catch (ParseException e1) {
                LOGGER.warning("Parsing exception");
                LOGGER.log(Loggable.STACK, "Parsing error in JSpinner", e1);
            }

        });

        // add the change listeners
        maxRadiusSpinner.addChangeListener(e -> {
            try {
                JSpinner j = (JSpinner) e.getSource();
                j.commitEdit();
                Integer value = (Integer) j.getValue();
                options.setMaxRadius(value.intValue());
                fireOptionsChangeEvent();
            } catch (ParseException e1) {
                LOGGER.warning("Parsing exception");
                LOGGER.log(Loggable.STACK, "Parsing error in JSpinner", e1);
            }

        });

        numCirclesSpinner.addChangeListener(e -> {
            try {
                JSpinner j = (JSpinner) e.getSource();
                j.commitEdit();
                Integer value = (Integer) j.getValue();
                options.setNumberOfCircles(value.intValue());
                thresholdSpinner.setEnabled(value==0);
                fireOptionsChangeEvent();
            } catch (ParseException e1) {
                LOGGER.warning("Parsing exception");
                LOGGER.log(Loggable.STACK, "Parsing error in JSpinner", e1);
            }

        });

        thresholdSpinner.addChangeListener(e -> {
            try {
                JSpinner j = (JSpinner) e.getSource();
                j.commitEdit();
                Integer value = (Integer) j.getValue();
                options.setHoughThreshold(value.intValue());

                if (value > -1) {
                    numCirclesSpinner.setEnabled(false);
                } else {
                    numCirclesSpinner.setEnabled(true);
                }

                fireOptionsChangeEvent();
            } catch (ParseException e1) {
                LOGGER.warning("Parsing exception");
                LOGGER.log(Loggable.STACK, "Parsing error in JSpinner", e1);
            }

        });
    }

    private void createPanel() {

        try {

            this.setLayout(new GridBagLayout());

            List<JLabel> labelList = new ArrayList<JLabel>();
            List<JComponent> fieldList = new ArrayList<JComponent>();

            labelList.add(new JLabel(MIN_RADIUS_LBL));
            labelList.add(new JLabel(MAX_RADIUS_LBL));
            labelList.add(new JLabel(NUMBER_OF_CIRCLES_LBL));
            labelList.add(new JLabel(THRESHOLD_LBL));

            JLabel[] labels = labelList.toArray(new JLabel[0]);

            fieldList.add(minRadiusSpinner);
            fieldList.add(maxRadiusSpinner);
            fieldList.add(numCirclesSpinner);
            fieldList.add(thresholdSpinner);

            JComponent[] fields = fieldList.toArray(new JComponent[0]);

            addLabelTextRows(labels, fields, this);
        } catch (Exception e) {
            LOGGER.log(Loggable.STACK, e.getMessage(), e);
        }

    }

    /**
     * Update the display to the given options
     * 
     * @param options the options values to be used
     */
    @Override
	protected void update() {
        super.update();

        minRadiusSpinner.setValue(Double.valueOf(options.getMinRadius()));
        maxRadiusSpinner.setValue(Double.valueOf(options.getMaxRadius()));
        numCirclesSpinner.setValue(Integer.valueOf(options.getNumberOfCircles()));
        thresholdSpinner.setValue(Integer.valueOf(options.getHoughThreshold()));
    }
}
