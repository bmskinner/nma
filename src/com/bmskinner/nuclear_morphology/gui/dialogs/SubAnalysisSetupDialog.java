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
package com.bmskinner.nuclear_morphology.gui.dialogs;

import java.awt.Frame;

import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.main.AbstractMainWindow;
import com.bmskinner.nuclear_morphology.gui.main.MainWindow;

/**
 * A base class for the sub analyses setup options. It contains a reference to
 * the main analysis window to make the dialog modal It can return an
 * IAnalysisMethod preconfigured for running
 * 
 * @author bms41
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public abstract class SubAnalysisSetupDialog extends SettingsDialog {

    protected final JPanel contentPanel = new JPanel();

    protected final IAnalysisDataset dataset;

    /**
     * Construct with a program window to listen for actions, and a dataset
     * to operate on
     * 
     * @param mw
     * @param dataset
     */
    public SubAnalysisSetupDialog(final Frame owner, final IAnalysisDataset dataset, final String title) {
        super(owner, true);
        this.dataset = dataset;
        this.setTitle(title);
        this.setModal(true);
    }
    
    /**
     * Construct with a main program window to listen for actions, and a dataset
     * to operate on
     * 
     * @param mw
     * @param dataset
     */
    public SubAnalysisSetupDialog(final IAnalysisDataset dataset, final String title) {
        super(true);
        this.dataset = dataset;
        this.setTitle(title);
    }

    protected void packAndDisplay() {
        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    /**
     * Get the method for the analysis to be run
     * 
     * @return
     */
    public abstract IAnalysisMethod getMethod();

    /**
     * Make the UI for the dialog
     */
    protected abstract void createUI();

    /**
     * Set the default options for the dialog
     */
    protected abstract void setDefaults();

}
