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
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.io.ConfigFileReader;
import com.bmskinner.nma.io.ConfigFileReader.RulesetEntry;
import com.bmskinner.nma.io.Io;
import com.bmskinner.nma.io.XMLReader;
import com.bmskinner.nma.io.XMLReader.XMLReadingException;


/**
 * Holds other nucleus detection options. E.g. profile window
 * 
 * @author Ben Skinner
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class NucleusProfileSettingsPanel extends SettingsPanel {

	private static final Logger LOGGER = Logger
			.getLogger(NucleusProfileSettingsPanel.class.getName());

	private static final double MIN_PROFILE_PROP = 0;
	private static final double MAX_PROFILE_PROP = 1;
	private static final double STEP_PROFILE_PROP = 0.01;

	private static final String TYPE_LBL = "Ruleset";
	private static final String PROFILE_WINDOW_LBL = "Profile window";
	private static final String IS_SEGMENT_LBL = "Segment profiles";

	private IAnalysisOptions options;

	private JSpinner profileWindow;

	private JComboBox<RulesetEntry> typeBox;

	private JCheckBox segmentBox;

	private RulesetEntry[] availableRules = ConfigFileReader.getAvailableRulesets();


	public NucleusProfileSettingsPanel(final IAnalysisOptions op) {
		super();
		options = op;
		this.add(createPanel(), BorderLayout.CENTER);
	}


	private void setRuleset(RulesetEntry f) {
		options.setRuleSetCollection(f.rsc());
		fireOptionsChangeEvent();
		fireProberReloadEvent();
	}

	/**
	 * Create the settings spinners based on the input options
	 */
	private void createSpinners() {

		// Name of the default ruleset
		String defaultRulesetName = GlobalOptions.getInstance()
				.getString(GlobalOptions.DEFAULT_RULESET_KEY);

		LOGGER.fine(() -> "Default ruleset is '%s': ".formatted(defaultRulesetName));

		typeBox = new JComboBox<>(availableRules);
		typeBox.addActionListener(e -> {

			Optional<HashOptions> nOptions = options.getDetectionOptions(CellularComponent.NUCLEUS);
			if (!nOptions.isPresent())
				return;

			RulesetEntry selected = (RulesetEntry) typeBox.getSelectedItem();
			LOGGER.fine(() -> "Selected ruleset %s".formatted(selected.toString()));
			setRuleset(selected);
		});

		// Check if the default ruleset name from the config file is present
		// If there are multiple matching names, choose the most recent
		// version
		Optional<RulesetEntry> defaultEntry = Arrays.stream(availableRules)
				.filter(r -> r.rsc().getName().equals(defaultRulesetName))
				.sorted((r1, r2) -> r1.rsc().getRulesetVersion()
						.isNewerThan(r2.rsc().getRulesetVersion())
								? -1
								: 1)
				.findFirst();

		// If a default ruleset is in the config options, set it, otherwise
		// use the first entry
		if (defaultEntry.isPresent()) {
			LOGGER.fine(() -> "Default ruleset present '%s': ".formatted(defaultRulesetName));
			typeBox.setSelectedItem(defaultEntry.get());
			setRuleset(defaultEntry.get());
		} else {
			typeBox.setSelectedIndex(0);
			setRuleset((RulesetEntry) typeBox.getSelectedItem());
		}

		profileWindow = new JSpinner(
				new SpinnerNumberModel(options.getProfileWindowProportion(), MIN_PROFILE_PROP,
						MAX_PROFILE_PROP, STEP_PROFILE_PROP));

		Dimension dim = new Dimension(BOX_WIDTH, BOX_HEIGHT);
		profileWindow.setPreferredSize(dim);

		profileWindow.addChangeListener(e -> {
			JSpinner j = (JSpinner) e.getSource();
			try {
				j.commitEdit();
				options.setAngleWindowProportion((Double) j.getValue());
			} catch (Exception e1) {
				LOGGER.warning("Parsing error in spinner");
				LOGGER.log(Level.SEVERE, "Parsing error in JSpinner", e1);
			}

		});

		segmentBox = new JCheckBox("", options.getProfilingOptions().getBoolean(HashOptions.IS_SEGMENT_PROFILES));
		segmentBox.addChangeListener(e -> {
			options.getProfilingOptions().setBoolean(HashOptions.IS_SEGMENT_PROFILES,
					segmentBox.isSelected());
		});
	}

	private JPanel createPanel() {

		this.createSpinners();

		JPanel panel = new JPanel(new GridBagLayout());

		List<JLabel> labels = new ArrayList<>();
		labels.add(new JLabel(TYPE_LBL));
		labels.add(new JLabel(PROFILE_WINDOW_LBL));
		labels.add(new JLabel(IS_SEGMENT_LBL));

		List<Component> fields = new ArrayList<>();

		fields.add(typeBox);
		fields.add(profileWindow);
		fields.add(segmentBox);

		addLabelTextRows(labels, fields, panel);

		return panel;
	}

	/**
	 * Update the spinners to current options values
	 */
	@Override
	protected void update() {
		super.update();

		availableRules = ConfigFileReader.getAvailableRulesets();
		profileWindow.setValue(options.getProfileWindowProportion());
		RuleSetCollection rsc = options.getRuleSetCollection();

		Arrays.stream(availableRules)
				.filter(r -> r.rsc().equals(rsc))
				.findFirst().ifPresent(r -> typeBox.setSelectedItem(r));
	}

	@Override
	public void setEnabled(boolean b) {
		super.setEnabled(b);
		profileWindow.setEnabled(b);
		typeBox.setEnabled(b);
	}
}
