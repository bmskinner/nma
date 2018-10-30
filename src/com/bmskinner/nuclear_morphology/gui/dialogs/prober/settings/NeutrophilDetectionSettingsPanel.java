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

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.GenericImageProberPanel.PanelUpdatingEventListener;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.OptionsChangeEvent;

/**
 * A combined settings panel allowing setup for neutrophil detection using
 * cytoplasm and nucleus settings.
 * 
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class NeutrophilDetectionSettingsPanel extends SettingsPanel implements PanelUpdatingEventListener {

    private IAnalysisOptions options;

    private static final String CYTO_SETTINGS_LBL = "Cytoplasm";
    private static final String NUCL_SETTINGS_LBL = "Nucleus";
    private static final String RELOAD_LBL        = "Reload";

    private JButton reloadBtn;

    public NeutrophilDetectionSettingsPanel(IAnalysisOptions options) {
        this.options = options;

        this.setLayout(new BorderLayout());
        this.add(createPanel(), BorderLayout.CENTER);
        this.add(createFooter(), BorderLayout.SOUTH);
    }

    private JPanel createPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        try {
            SettingsPanel cytoPanel = new ConstructableSettingsPanel(options)
                    // .addImageChannelPanel(IAnalysisOptions.CYTOPLASM,
                    // ConstructableSettingsPanel.CHANNEL_LBL)
                    .addColourThresholdWatershedSwitchPanel(IAnalysisOptions.CYTOPLASM,
                            ConstructableSettingsPanel.THRESHOLDING_LBL)
                    // .addColorThresholdPanel(IAnalysisOptions.CYTOPLASM,
                    // ConstructableSettingsPanel.THRESHOLDING_LBL)
                    .addSizePanel(IAnalysisOptions.CYTOPLASM, "Cytoplasm filtering")
                    .addTopHatPanel(IAnalysisOptions.NUCLEUS, "Nucleus detection")
                    .addSizePanel(IAnalysisOptions.NUCLEUS, "Nucleus filtering")
                    .addNucleusProfilePanel(IAnalysisOptions.NUCLEUS, ConstructableSettingsPanel.PROFILING_LBL).build();

            SettingsPanel nuclPanel = new ConstructableSettingsPanel(options).addTopHatPanel(IAnalysisOptions.NUCLEUS)
                    // .addImageChannelPanel(IAnalysisOptions.NUCLEUS,
                    // ConstructableSettingsPanel.CHANNEL_LBL)
                    // .addColorThresholdPanel(IAnalysisOptions.NUCLEUS,
                    // ConstructableSettingsPanel.THRESHOLDING_LBL)
                    .addSizePanel(IAnalysisOptions.NUCLEUS, ConstructableSettingsPanel.SIZE_SETTINGS_LBL)
                    .addNucleusProfilePanel(IAnalysisOptions.NUCLEUS, ConstructableSettingsPanel.PROFILING_LBL).build();

            cytoPanel.setBorder(BorderFactory.createTitledBorder(CYTO_SETTINGS_LBL));
            nuclPanel.setBorder(BorderFactory.createTitledBorder(NUCL_SETTINGS_LBL));

            this.addSubPanel(cytoPanel);
            this.addSubPanel(nuclPanel);

            panel.add(cytoPanel);
            panel.add(nuclPanel);

        } catch (MissingOptionException e) {
            warn("Cannot make panels; missing options");
            stack(e.getMessage(), e);
        }

        return panel;
    }

    private JPanel createFooter() {
        JPanel panel = new JPanel();
        reloadBtn = new JButton(RELOAD_LBL);
        reloadBtn.addActionListener(e -> {
            fireProberReloadEvent();
        });

        panel.add(reloadBtn);
        return panel;

    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        reloadBtn.setEnabled(b);

    }

    @Override
    public void optionsChangeEventReceived(OptionsChangeEvent e) {

        if (this.hasSubPanel((SettingsPanel) e.getSource())) {
            update();

            if (e.getSource() instanceof EdgeThresholdSwitchPanel
                    || e.getSource() instanceof ColourThresholdWatershedSwitchPanel
                    || e.getSource() instanceof ImagePreprocessingSettingsPanel
                    || e.getSource() instanceof ComponentSizeSettingsPanel
                    || e.getSource() instanceof ImageChannelSettingsPanel) {
                fireProberReloadEvent(); // don't fire an update for values that
                                         // have no effect on a prober
            }
        }

    }

}
