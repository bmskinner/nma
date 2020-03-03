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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.analysis.signals.PairedSignalGroups;
import com.bmskinner.nuclear_morphology.analysis.signals.PairedSignalGroups.DatasetSignalId;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.components.panels.DatasetSelectionPanel;
import com.bmskinner.nuclear_morphology.gui.components.panels.SignalGroupSelectionPanel;

/**
 * Allows options to be set when merging datasets. E.g. which signal groups are
 * equivalent.
 * 
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class DatasetMergingDialog extends LoadingIconDialog implements ActionListener {
	
	private static final Logger LOGGER = Logger.getLogger(DatasetMergingDialog.class.getName());

    private static final String SIGNAL_GROUP_COL_LBL = "Signal group";

	private List<IAnalysisDataset> datasets;

    private DatasetSelectionPanel datasetBoxOne;
    private DatasetSelectionPanel datasetBoxTwo;

    private SignalGroupSelectionPanel signalBoxOne;
    private SignalGroupSelectionPanel signalBoxTwo;

    private JTable matchTable;

    private JButton mergeButton;
    private JButton setEqualButton;

    // Store the ids of signal groups that should be merged
    private PairedSignalGroups pairedSignalGroups = new PairedSignalGroups();

    public DatasetMergingDialog(List<IAnalysisDataset> datasets) {
        this.datasets = datasets;
        createUI();

        this.setModal(true);
        this.pack();
        centerOnScreen();
        this.setVisible(true);
        LOGGER.finest( "Created dataset merging dialog");
    }

    /**
     * Get any matched signal groups. Can be empty.
     * @return
     */
    public PairedSignalGroups getPairedSignalGroups() {
        return this.pairedSignalGroups;
    }

    private void createUI() {

        this.setLayout(new BorderLayout());
        this.setTitle("Merge datasets");

        JPanel header = createHeader();
        this.add(header, BorderLayout.NORTH);

        JScrollPane scrollPanel = new JScrollPane();
        matchTable = new JTable(createTableModel());
        scrollPanel.setViewportView(matchTable);
        this.add(scrollPanel, BorderLayout.CENTER);

        JPanel footer = createFooter();
        this.add(footer, BorderLayout.SOUTH);
    }

    private JPanel createFooter() {
        JPanel panel = new JPanel(new FlowLayout());

        mergeButton = new JButton("Merge");
        mergeButton.addActionListener(this);

        panel.add(mergeButton);
        return panel;
    }

    private JPanel createHeader() {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JPanel upperPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel lowerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        upperPanel.add(new JLabel("Datasets have signals. Choose which signal groups (if any) to merge"));

        datasetBoxOne = new DatasetSelectionPanel(datasets);
        datasetBoxTwo = new DatasetSelectionPanel(datasets);
        
        IAnalysisDataset d0 = datasets.get(0);
        IAnalysisDataset d1 = datasets.size()>1 ? datasets.get(1) : datasets.get(0);

        datasetBoxOne.setSelectedDataset(d0);
        datasetBoxTwo.setSelectedDataset(d1);

        datasetBoxOne.addActionListener(this);
        datasetBoxTwo.addActionListener(this);

        signalBoxOne = new SignalGroupSelectionPanel(d0);
        signalBoxTwo = new SignalGroupSelectionPanel(d1);

        setEqualButton = new JButton("Set equal");
        setEqualButton.addActionListener(this);

        // upperPanel.add(new JLabel("Source dataset"));
        lowerPanel.add(datasetBoxOne);
        lowerPanel.add(signalBoxOne);
        lowerPanel.add(setEqualButton);
        lowerPanel.add(signalBoxTwo);
        lowerPanel.add(datasetBoxTwo);

        headerPanel.add(upperPanel);
        headerPanel.add(lowerPanel);
        return headerPanel;
    }

    private TableModel createTableModel() {

        DefaultTableModel model = new DefaultTableModel();

        Object[] columns = { SIGNAL_GROUP_COL_LBL, SIGNAL_GROUP_COL_LBL };

        model.setColumnIdentifiers(columns);

        return model;

    }

    private void updateTable() {

        DefaultTableModel model = new DefaultTableModel();

        Object[] columns = { SIGNAL_GROUP_COL_LBL, SIGNAL_GROUP_COL_LBL };

        model.setColumnIdentifiers(columns);

        for (DatasetSignalId id1 : pairedSignalGroups.keySet()) {
		    String col1 = "";
		    for (IAnalysisDataset d : datasets) {
		        if (d.getId().equals(id1.d) && d.getCollection().getSignalManager().hasSignals(id1.s)) {
		            col1 = d.getName() + " : " + d.getCollection().getSignalGroup(id1.s).get().getGroupName();
		        }
		    }
		    Set<DatasetSignalId> idList = pairedSignalGroups.get(id1);
		    for (DatasetSignalId id2 : idList) {
		        String col2 = "";
		        for (IAnalysisDataset d : datasets) {
		        	if(d.getId().equals(id2.d) && d.getCollection().getSignalManager().hasSignals(id2.s)) {
		                col2 = d.getName() + " : " + d.getCollection().getSignalGroup(id2.s).get().getGroupName();
		            }
		        }

		        Object[] row = { col1, col2 };
		        model.addRow(row);
		    }

		}
		matchTable.setModel(model);

    }

    @Override
    public void actionPerformed(ActionEvent e) {

        IAnalysisDataset d1 = datasetBoxOne.getSelectedDataset();
        IAnalysisDataset d2 = datasetBoxTwo.getSelectedDataset();

        UUID s1 = signalBoxOne.getSelectedID();
        UUID s2 = signalBoxTwo.getSelectedID();
        
        if (e.getSource() == datasetBoxOne) {
            signalBoxOne.setDataset(d1);
        }
        if (e.getSource() == datasetBoxTwo) {
            signalBoxTwo.setDataset(d2);
        }

        if (e.getSource() == setEqualButton) {
        	pairedSignalGroups.add(d1.getId(), s1, d2.getId(), s2);
            updateTable();
        }

        if (e.getSource() == mergeButton) {
            LOGGER.info("Signal pairing complete");
            
            LOGGER.fine("Merged pairs: "+pairedSignalGroups.toString());
            this.setVisible(false);
        }

    }

}
