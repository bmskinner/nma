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


package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.gui.dialogs.LoadingIconDialog;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings.SettingsPanel;

/**
 * Integrates the analysis setup dialog with the image prober.
 * 
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public abstract class IntegratedImageProber extends LoadingIconDialog {

    private static final String PROCEED_LBL = "Proceed with detection";

    protected IAnalysisOptions options; // the active options

    protected SettingsPanel optionsSettingsPanel; // settings

    protected GenericImageProberPanel imageProberPanel; // result

    protected boolean ok = false;

    private JButton okButton = new JButton(PROCEED_LBL);

    /**
     * Make the footer panel, with ok and cancel buttons
     * 
     * @return
     */
    protected JPanel createFooter() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        okButton.addActionListener(e -> {
            okButtonClicked();
            ok = true;
            setVisible(false);
        });
        panel.add(okButton);

        getRootPane().setDefaultButton(okButton);

        return panel;
    }

    /**
     * Allow overriding of the "Proceed" button label
     * 
     * @param s
     */
    protected void setOkButtonText(String s) {
        okButton.setText(s);
    }

    /**
     * Check if the analysis is ready to run
     * 
     * @return
     */
    public boolean isOk() {
        return ok;
    }

    protected abstract void okButtonClicked();

}
