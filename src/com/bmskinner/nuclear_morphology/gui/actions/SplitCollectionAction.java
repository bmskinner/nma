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


package com.bmskinner.nuclear_morphology.gui.actions;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

import javax.swing.JOptionPane;

import com.bmskinner.nuclear_morphology.components.DefaultCell;
import com.bmskinner.nuclear_morphology.components.DefaultCellCollection;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.gui.MainWindow;

public class SplitCollectionAction extends SingleDatasetResultAction {

    public SplitCollectionAction(IAnalysisDataset dataset, MainWindow mw) {
        super(dataset, "Splitting collection", mw);
    }

    @Override
    public void run() {
        try {

            if (dataset.hasChildren()) {
                log("Splitting collection...");

                IAnalysisDataset[] names = dataset.getAllChildDatasets().toArray(new IAnalysisDataset[0]);

                IAnalysisDataset negative = (IAnalysisDataset) JOptionPane.showInputDialog(null,
                        "Give me nuclei that are NOT present within the following population", "Split population",
                        JOptionPane.PLAIN_MESSAGE, null, names, names[0]);

                if (negative != null) {

                    // prepare a new collection
                    ICellCollection collection = dataset.getCollection();

                    ICellCollection newCollection = new DefaultCellCollection(dataset, "Subtraction");
                    
                    
                    collection.streamCells()
                        .filter(c->!negative.getCollection().contains(c))
                        .forEach(c->newCollection.addCell(new DefaultCell(c)));

                    newCollection.setName("Not_in_" + negative.getName());

                    dataset.addChildCollection(newCollection);

                    if (newCollection.size() > 0) {

                        log(Level.INFO, "Reapplying morphology...");

                        int flag = 0;
                        IAnalysisDataset newDataset = dataset.getChildDataset(newCollection.getID());
                        final CountDownLatch latch = new CountDownLatch(1);
                        new RunSegmentationAction(newDataset, dataset, flag, mw, latch);
                    }
                } else {
                    fine("User cancelled split");
                }

            } else {
                log("Cannot split; no children in dataset");
            }

        } catch (Exception e1) {
            error("Error splitting collection", e1);
        } finally {
            cancel();
        }
    }

}
