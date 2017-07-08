/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.bmskinner.nuclear_morphology.gui.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter.ColourSwatch;
import com.bmskinner.nuclear_morphology.logging.Loggable;

@SuppressWarnings("serial")
public class MainOptionsDialog extends SettingsDialog implements ActionListener {

    private static final String DIALOG_TITLE = "Options";

    private JComboBox<Level>        levelBox;
    private JComboBox<ColourSwatch> colourBox;
    private JCheckBox               violinBox;
    private JCheckBox               fillConsensusBox;
    private JCheckBox               refoldOverrideBox;
    private JCheckBox               antiAliasBox;
    private JCheckBox               convertDatasetsBox; // should opened
                                                        // datasets be converted
                                                        // to the latest version

    public MainOptionsDialog(final MainWindow mw) {
        super(mw, false);

        // this.mw = mw;
        this.setLayout(new BorderLayout());
        this.setTitle(DIALOG_TITLE);

        // The date and time the program was built
        // this.add( new JLabel(Constants.BUILD), BorderLayout.NORTH);

        this.add(createMainPanel(), BorderLayout.CENTER);
        this.add(createFooter(), BorderLayout.SOUTH);

        this.setLocationRelativeTo(null);
        this.setMinimumSize(new Dimension(100, 70));
        this.pack();
        this.setVisible(true);

    }

    /**
     * Create the panel footer, with just a close button buttons
     * 
     * @return
     */
    @Override
    protected JPanel createFooter() {

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JButton okButton = new JButton("Close");
        okButton.addActionListener(e -> {
            setVisible(false);
            dispose();
        });
        panel.add(okButton);

        return panel;
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel();
        GridBagLayout layout = new GridBagLayout();
        panel.setLayout(layout);
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));

        List<JLabel> labels = new ArrayList<JLabel>();
        List<Component> fields = new ArrayList<Component>();

        JLabel logLabel = new JLabel("Logging level");
        Level[] levelArray = { Level.INFO, Loggable.TRACE, Level.FINE, Level.FINER, Level.FINEST };
        levelBox = new JComboBox<Level>(levelArray);
        levelBox.setSelectedItem(Logger.getLogger(PROGRAM_LOGGER).getLevel());
        levelBox.addActionListener(this);

        labels.add(logLabel);
        fields.add(levelBox);

        JLabel swatchLabel = new JLabel("Colour swatch");
        colourBox = new JComboBox<ColourSwatch>(ColourSwatch.values());
        colourBox.setSelectedItem(GlobalOptions.getInstance().getSwatch());
        colourBox.addActionListener(this);

        labels.add(swatchLabel);
        fields.add(colourBox);

        JLabel violinLabel = new JLabel("Violin plots");
        violinBox = new JCheckBox((String) null, GlobalOptions.getInstance().isViolinPlots());
        violinBox.addActionListener(this);

        labels.add(violinLabel);
        fields.add(violinBox);

        JLabel fillConsensusLabel = new JLabel("Fill consensus");
        fillConsensusBox = new JCheckBox((String) null, GlobalOptions.getInstance().isFillConsensus());
        fillConsensusBox.addActionListener(this);

        labels.add(fillConsensusLabel);
        fields.add(fillConsensusBox);
        
        JLabel overrideRefoldLabel = new JLabel("Refold override");
        refoldOverrideBox = new JCheckBox((String) null, GlobalOptions.getInstance().isOverrideRefold());
        refoldOverrideBox.addActionListener(this);

        labels.add(overrideRefoldLabel);
        fields.add(refoldOverrideBox);

        JLabel antiAliasLabel = new JLabel("Antialiasing");
        antiAliasBox = new JCheckBox((String) null, GlobalOptions.getInstance().isAntiAlias());
        antiAliasBox.addActionListener(this);

        labels.add(antiAliasLabel);
        fields.add(antiAliasBox);

        JLabel convertDatasetsLabel = new JLabel("Convert datasets");
        convertDatasetsBox = new JCheckBox((String) null, GlobalOptions.getInstance().isConvertDatasets());
        convertDatasetsBox.addActionListener(this);

        labels.add(convertDatasetsLabel);
        fields.add(convertDatasetsBox);

        this.addLabelTextRows(labels, fields, layout, panel);
        return panel;
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {

        Level level = (Level) levelBox.getSelectedItem();
        if (!level.equals(Logger.getLogger(PROGRAM_LOGGER).getLevel())) {
            Logger.getLogger(PROGRAM_LOGGER).setLevel(level);
            GlobalOptions.getInstance().setLogLevel(level);
        }

        ColourSwatch swatch = (ColourSwatch) colourBox.getSelectedItem();

        if (!swatch.equals(GlobalOptions.getInstance().getSwatch())) {

            GlobalOptions.getInstance().setSwatch(swatch);
            fireInterfaceEvent(InterfaceMethod.UPDATE_PANELS);
        }

        boolean useViolins = violinBox.isSelected();
        if (GlobalOptions.getInstance().isViolinPlots() != useViolins) {
            GlobalOptions.getInstance().setViolinPlots(useViolins);
            fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
        }

        boolean fillConsensus = fillConsensusBox.isSelected();
        if (GlobalOptions.getInstance().isFillConsensus() != fillConsensus) {
            GlobalOptions.getInstance().setFillConsensus(fillConsensus);
            fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
        }

        boolean antiAlias = antiAliasBox.isSelected();
        if (GlobalOptions.getInstance().isAntiAlias() != antiAlias) {
            GlobalOptions.getInstance().setAntiAlias(antiAlias);
            fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
        }

        boolean convertDatasets = convertDatasetsBox.isSelected();
        if (GlobalOptions.getInstance().isConvertDatasets() != convertDatasets) {
            GlobalOptions.getInstance().setConvertDatasets(convertDatasets);
        }
        
        boolean overrideRefold = refoldOverrideBox.isSelected();
        if (GlobalOptions.getInstance().isOverrideRefold() != overrideRefold) {
            GlobalOptions.getInstance().setOverrideRefold(overrideRefold);
        }


    }

}
