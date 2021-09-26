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
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
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
public class DatasetMergingDialog extends LoadingIconDialog {
	
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
    
    private JButton inferButton;

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
        mergeButton.addActionListener(e->{
        	LOGGER.info("Signal pairing complete");
            LOGGER.fine("Merged pairs: "+pairedSignalGroups.toString());
            this.setVisible(false);
        });

        panel.add(mergeButton);
        return panel;
    }

    private JPanel createHeader() {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JPanel upperPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel middlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        upperPanel.add(new JLabel("Datasets have signals. Choose which signal groups (if any) to merge"));

        datasetBoxOne = new DatasetSelectionPanel(datasets);
        datasetBoxTwo = new DatasetSelectionPanel(datasets);
        
        IAnalysisDataset d0 = datasets.get(0);
        IAnalysisDataset d1 = datasets.size()>1 ? datasets.get(1) : datasets.get(0);

        datasetBoxOne.setSelectedDataset(d0);
        datasetBoxTwo.setSelectedDataset(d1);

        datasetBoxOne.addActionListener(e->signalBoxOne.setDataset(datasetBoxOne.getSelectedDataset()));
        datasetBoxTwo.addActionListener(e->signalBoxTwo.setDataset(datasetBoxTwo.getSelectedDataset()));

        signalBoxOne = new SignalGroupSelectionPanel(d0);
        signalBoxTwo = new SignalGroupSelectionPanel(d1);

        setEqualButton = new JButton("Set equal");
        setEqualButton.addActionListener(e->{
        	pairedSignalGroups.add(datasetBoxOne.getSelectedDataset().getId(), 
        			signalBoxOne.getSelectedID(), 
        			datasetBoxTwo.getSelectedDataset().getId(), 
        			signalBoxTwo.getSelectedID());
            updateTable();
        });
        
        inferButton = new JButton("Infer from group names");
        inferButton.addActionListener(e->{
        	LOGGER.info("Inferring pairs from signal group names");
        	inferPairs();
        	updateTable();
        });
        
        bottomPanel.add(inferButton);
        
        middlePanel.add(datasetBoxOne);
        middlePanel.add(signalBoxOne);
        middlePanel.add(setEqualButton);
        middlePanel.add(signalBoxTwo);
        middlePanel.add(datasetBoxTwo);

        headerPanel.add(upperPanel);
        headerPanel.add(middlePanel);
        headerPanel.add(bottomPanel);
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
    
    /**
     * Infer which signal groups should be merged based on their names.
     * Signal groups with identical names will be added to the paired list.
     * 
     */
    private void inferPairs() {
    	
    	List<String> signalGroupNames = datasets.stream()
    			.flatMap(d->d.getCollection().getSignalGroups().stream())
    			.map(g->g.getGroupName())
    			.distinct()
    			.collect(Collectors.toList());
    	
    	
    	// For each name, find matching datasets
    	for(String groupName : signalGroupNames) {
    		
    		List<IAnalysisDataset> matchingDatasets = datasets.stream().filter(d->d.getCollection()
    				.getSignalGroups()
    				.stream().anyMatch(g->g.getGroupName()
    						.equals(groupName))).collect(Collectors.toList());
    		
    		IAnalysisDataset d1 = matchingDatasets.get(0);
    		
    		// Get the id from d1
    		UUID u1 = null;
    		for(UUID u : d1.getCollection().getSignalGroupIDs()) {
    			if(d1.getCollection().getSignalGroup(u).get().getGroupName().equals(groupName))
    				u1 = u;
    		}
    		
    		
    		for(IAnalysisDataset d2 : matchingDatasets) {
    			if(d1==d2) continue;

    			// Get the id from d2
        		for(UUID u2 : d2.getCollection().getSignalGroupIDs()) {
        			if(d2.getCollection().getSignalGroup(u2).get().getGroupName().equals(groupName))
        				pairedSignalGroups.add(d1.getId(), u1, d2.getId(), u2);
        		}
    		}
    	}   
    }
}
