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
package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.awt.BorderLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.Finder;
import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.FishRemappingFinder;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;

@SuppressWarnings("serial")
public class FishRemappingProber extends IntegratedImageProber {

    private static final String DIALOG_TITLE_BAR_LBL = "Post-FISH mapping";
    private static final String PROCEED_LBL          = "Finished selection";

    final IAnalysisDataset       dataset;
    final List<IAnalysisDataset> newList = new ArrayList<>();

    /**
     * Create with a dataset (from which nuclei will be drawn) and a folder of
     * images to be analysed
     * 
     * @param dataset the analysis dataset
     * @param folder the folder of images
     */
    public FishRemappingProber(@NonNull final IAnalysisDataset dataset, @NonNull final File fishImageDir) {
        this.dataset = dataset;

        try {

            // make the panel
            Finder<?> finder = new FishRemappingFinder(dataset.getAnalysisOptions().get(), fishImageDir);

            imageProberPanel = new FishRemappingProberPanel(dataset, finder, this);
            
            imageProberPanel.setSize(imageProberPanel.getPreferredSize());

            JPanel footerPanel = createFooter();
            this.setOkButtonText(PROCEED_LBL);

            this.add(imageProberPanel, BorderLayout.CENTER);
            this.add(footerPanel, BorderLayout.SOUTH);

            this.setTitle(DIALOG_TITLE_BAR_LBL);

        } catch (Exception e) {
            warn("Error launching FISH remapping window");
            stack(e.getMessage(), e);
            this.dispose();
        }

        this.pack();
        this.setModal(true);
        this.setLocationRelativeTo(null); // centre on screen
        this.setVisible(true);
    }

    @Override
    protected void okButtonClicked() {

        List<ICellCollection> subs = ((FishRemappingProberPanel) imageProberPanel).getSubCollections();

        if (subs.isEmpty()) {

            return;
        }

        for (ICellCollection sub : subs) {

            if (sub.hasCells()) {

                dataset.addChildCollection(sub);

                final IAnalysisDataset subDataset = dataset.getChildDataset(sub.getID());
                newList.add(subDataset);
            }
        }
    }

    public List<IAnalysisDataset> getNewDatasets() {
        return newList;
    }

}
