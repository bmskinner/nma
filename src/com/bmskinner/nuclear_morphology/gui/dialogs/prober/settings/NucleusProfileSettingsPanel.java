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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.io.Io;
import com.bmskinner.nuclear_morphology.io.XMLReader;
import com.bmskinner.nuclear_morphology.io.XMLReader.XMLReadingException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Holds other nucleus detection options. E.g. profile window
 * 
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class NucleusProfileSettingsPanel extends SettingsPanel {
	
	private static final Logger LOGGER = Logger.getLogger(NucleusProfileSettingsPanel.class.getName());

    private static final double MIN_PROFILE_PROP  = 0;
    private static final double MAX_PROFILE_PROP  = 1;
    private static final double STEP_PROFILE_PROP = 0.01;

    private static final String TYPE_LBL           = "Nucleus type";
    private static final String PROFILE_WINDOW_LBL = "Profile window";

    private IAnalysisOptions options;

    private JSpinner profileWindow;

    private JComboBox<String> typeBox;
    
    public NucleusProfileSettingsPanel(final IAnalysisOptions op) {
        super();
        options = op;
        this.add(createPanel(), BorderLayout.CENTER);
    }
    
    private String[] getAvailableRulesets(){
    	File[] files = Io.getRulesetDir().listFiles((d, s) -> s.toLowerCase().endsWith(Io.XML_FILE_EXTENSION));
    	return Arrays.stream(files)
    			.map(File::getName)
    			.map(f-> f.replaceAll(Io.XML_FILE_EXTENSION, ""))
    			.toArray(String[]::new);
    }
    
    private void setRuleset(File f) {
    	try {
        	options.setRuleSetCollection(XMLReader.readRulesetCollection(f));
		} catch (XMLReadingException | ComponentCreationException e1) {
			LOGGER.log(Loggable.STACK, e1, () -> "Unable to read XML file: "+f.getAbsolutePath());
		}
    }

    /**
     * Create the settings spinners based on the input options
     */
    private void createSpinners() {

        typeBox = new JComboBox<>(getAvailableRulesets());

        typeBox.addActionListener(e -> {

        	Optional<HashOptions> nOptions = options.getDetectionOptions(CellularComponent.NUCLEUS);
        	if(!nOptions.isPresent())
        		return;

        	// Rebuild the file name from the cleaned names
        	File type = new File(Io.getRulesetDir(), (String)typeBox.getSelectedItem()+Io.XML_FILE_EXTENSION);
        	setRuleset(type);
        });
        
        // Set the default from the order of the dropdown list
        File defaultRuleset = new File(Io.getRulesetDir(), (String)typeBox.getSelectedItem()+Io.XML_FILE_EXTENSION);
        setRuleset(defaultRuleset);

        profileWindow = new JSpinner(new SpinnerNumberModel(options.getProfileWindowProportion(), MIN_PROFILE_PROP,
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
                LOGGER.log(Loggable.STACK, "Parsing error in JSpinner", e1);
            }

        });        
    }

    private JPanel createPanel() {

        this.createSpinners();

        JPanel panel = new JPanel(new GridBagLayout());

        List<JLabel> labels = new ArrayList<>();
        labels.add(new JLabel(TYPE_LBL));
        labels.add(new JLabel(PROFILE_WINDOW_LBL));

        List<Component> fields = new ArrayList<>();

        fields.add(typeBox);
        fields.add(profileWindow);

        addLabelTextRows(labels, fields, panel);

        return panel;
    }

    /**
     * Update the spinners to current options values
     */
    @Override
    protected void update() {
        super.update();
        profileWindow.setValue(options.getProfileWindowProportion());
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        profileWindow.setEnabled(b);
        typeBox.setEnabled(b);
    }
}
