package com.bmskinner.nuclear_morphology.gui.components.panels;

import java.util.List;

import javax.swing.JComboBox;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;

/**
 * This creates a panel with a drop down list of datasets, specified in the
 * constructor. Add an actionlistener to the panel, and access the selected
 * dataset via the getSelectedDataset() method.
 *
 */
@SuppressWarnings("serial")
public class DatasetSelectionPanel extends EnumeratedOptionsPanel {

    JComboBox<IAnalysisDataset> box;

    public DatasetSelectionPanel(List<IAnalysisDataset> datasets) {
        box = new JComboBox<IAnalysisDataset>();
        for (IAnalysisDataset d : datasets) {
            box.addItem(d);
        }

        box.setSelectedItem(datasets.get(0));

        box.addActionListener(this);

        this.add(box);
    }

    public IAnalysisDataset getSelectedDataset() {
        return (IAnalysisDataset) box.getSelectedItem();
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        box.setEnabled(b);
    }

    public void setSelectionIndex(int i) {

        if (i > box.getItemCount() - 1) {
            return;
        }
        if (i < 0) {
            return;
        }
        box.setSelectedItem(i);
    }

    public void setSelectedDataset(IAnalysisDataset d) {

        box.setSelectedItem(d);
    }

}
