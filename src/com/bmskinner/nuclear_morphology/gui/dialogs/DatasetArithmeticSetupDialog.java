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
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.components.panels.DatasetSelectionPanel;
import com.bmskinner.nuclear_morphology.gui.main.MainWindow;

@SuppressWarnings("serial")
public class DatasetArithmeticSetupDialog extends SettingsDialog implements ActionListener {

    private DatasetSelectionPanel                 boxOne;
    private DatasetSelectionPanel                 boxTwo;
    private JComboBox<DatasetArithmeticOperation> operatorBox;
    private JLabel                                operatorDescription = new JLabel(
            DatasetArithmeticOperation.AND.getDescription());

    public enum DatasetArithmeticOperation {
        AND("Cells are present in both datasets"), 
        OR("Cells are in either dataset (this merges the datasets)"), 
        NOT("Cells are in dataset one, but not dataset two"), 
        XOR("Cells are in one or other dataset, but not both datasets");

        private String description;

        private DatasetArithmeticOperation(String description) {
            this.description = description;
        }

        public String getDescription() {
            return this.description;
        }
    }

    public DatasetArithmeticSetupDialog(List<IAnalysisDataset> list) {
        super(true);

        this.setTitle("Dataset boolean options");
        setSize(450, 300);
        this.setLocationRelativeTo(null);
        createGUI(list);
        this.setVisible(true);
    }

    public IAnalysisDataset getDatasetOne() {
        return boxOne.getSelectedDataset();
    }

    public IAnalysisDataset getDatasetTwo() {
        return boxTwo.getSelectedDataset();
    }

    public DatasetArithmeticOperation getOperation() {
        return (DatasetArithmeticOperation) operatorBox.getSelectedItem();
    }

    private void createGUI(List<IAnalysisDataset> list) {

        setLayout(new BorderLayout());
    	

        JPanel panel = new JPanel();
        GridBagLayout layout = new GridBagLayout();
        panel.setLayout(layout);
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));

        List<JLabel> labels = new ArrayList<JLabel>();
        List<Component> fields = new ArrayList<Component>();

        boxOne = new DatasetSelectionPanel(list);
        boxTwo = new DatasetSelectionPanel(list);

        boxOne.setSelectedDataset(list.get(0));
        boxTwo.setSelectedDataset(list.get(0));

        if (list.size() == 2) {
            boxTwo.setSelectedDataset(list.get(1));
        }

        operatorBox = new JComboBox<DatasetArithmeticOperation>(DatasetArithmeticOperation.values());
        operatorBox.setSelectedItem(DatasetArithmeticOperation.AND);
        operatorBox.setPreferredSize(boxOne.getPreferredSize());
        operatorBox.addActionListener(this);

        labels.add(new JLabel("Dataset one"));
        fields.add(boxOne);

        JPanel operatorPanel = new JPanel(new FlowLayout());
        operatorPanel.add(operatorBox);
        labels.add(new JLabel("Operation"));
        fields.add(operatorPanel);

        JPanel descPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        descPanel.add(operatorDescription);
        labels.add(new JLabel("Description"));
        fields.add(descPanel);

        labels.add(new JLabel("Dataset two"));
        fields.add(boxTwo);

        this.addLabelTextRows(labels, fields, layout, panel);

        JPanel header = new JPanel(new FlowLayout());
        header.add(new JLabel("Create a new dataset using the following rule:"));

        this.add(header, BorderLayout.NORTH);
        this.add(panel, BorderLayout.CENTER);

        this.add(createFooter(), BorderLayout.SOUTH);

    }

    @Override
    public void actionPerformed(ActionEvent e) {

        operatorDescription.setText(getOperation().getDescription());

    }

}
