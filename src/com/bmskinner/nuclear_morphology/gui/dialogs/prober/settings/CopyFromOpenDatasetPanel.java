/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/

package com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableDetectionOptions;
import com.bmskinner.nuclear_morphology.main.DatasetListManager;

/**
 * A copy button that allows nuclear detection options to be copied from an open
 * dataset
 * 
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class CopyFromOpenDatasetPanel extends DetectionSettingsPanel {

    private static final String COPY_FROM_OPEN_LBL     = "Copy from open dataset";
    private static final String COPY_FROM_OPEN_TOOLTIP = "Copy from existing open dataset";
    private static final String CHOOSE_DATASET_MSG_LBL = "Choose source dataset";
    private static final String CHOOSE_DATASET_TTL_LBL = "Source dataset";

    private JButton copyBtn;

    public CopyFromOpenDatasetPanel(IMutableDetectionOptions op) {
        super(op);
        this.add(createPanel(), BorderLayout.CENTER);
    }

    /**
     * Create the settings spinners based on the input options
     */
    private void createSpinners() {

        // Button to copy existing dataset options
        copyBtn = new JButton(COPY_FROM_OPEN_LBL);
        copyBtn.addActionListener(e -> {
            IAnalysisDataset[] nameArray = DatasetListManager.getInstance().getAllDatasets()
                    .toArray(new IAnalysisDataset[0]);

            IAnalysisDataset sourceDataset = (IAnalysisDataset) JOptionPane.showInputDialog(null,
                    CHOOSE_DATASET_MSG_LBL, CHOOSE_DATASET_TTL_LBL, JOptionPane.QUESTION_MESSAGE, null, nameArray,
                    nameArray[0]);

            if (sourceDataset != null) {

            	fine("Copying options from dataset: " + sourceDataset.getName());

            	// Ensure the folder is not overwritten by the new options
            	File folder = options.getFolder();
            	Optional<IMutableAnalysisOptions> op = sourceDataset.getAnalysisOptions();
            	if(op.isPresent()){
            		Optional<IMutableDetectionOptions> srcOptions = op.get().getDetectionOptions(IAnalysisOptions.NUCLEUS);
            		options.set(srcOptions.get());
            		options.setFolder(folder);
            	}

                fireOptionsChangeEvent();
            }
        });

        copyBtn.setEnabled(DatasetListManager.getInstance().hasDatasets());

        copyBtn.setToolTipText(COPY_FROM_OPEN_TOOLTIP);

    }

    private JPanel createPanel() {

        this.createSpinners();

        JPanel panel = new JPanel();

        panel.setLayout(new FlowLayout(FlowLayout.CENTER));

        panel.add(copyBtn);

        return panel;
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);

        if (b) {
            copyBtn.setEnabled(DatasetListManager.getInstance().hasDatasets());
        } else {
            copyBtn.setEnabled(false);
        }

    }

    @Override
    public void set(IDetectionOptions options) {
    }

}
