/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package no.gui;


import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

import no.analysis.AnalysisDataset;

public class ClusterDetailPanel extends DetailPanel {

	private static final long serialVersionUID = 1L;
	
	private JButton 	clusterButton	= new JButton("Cluster population");
	private JLabel		statusLabel 	= new JLabel("No clusters present", SwingConstants.CENTER);
	private JPanel		statusPanel		= new JPanel(new BorderLayout());
	private JTextArea 	tree			= new JTextArea();
	private JScrollPane treeView;
	
	private JTable		mergeSources;
	private JButton		getSourceButton = new JButton("Recover source");
	
	private JPanel		clusterPanel	= new JPanel(new BorderLayout());
	private JPanel		mergePanel		= new JPanel(new BorderLayout());

	public ClusterDetailPanel() {
		
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		
		clusterPanel = makeClusterPanel();
		mergePanel 	 = makeMergeSourcesPanel();
		this.add(mergePanel);
		this.add(clusterPanel);

	}
	
	/**
	 * This panel shows the status of the dataset, 
	 * and holds the clustering button
	 * @return
	 */
	private JPanel makeStatusPanel(){
		
		JPanel panel = new JPanel(new BorderLayout());
		clusterButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				fireSignalChangeEvent("NewClusterAnalysis");

			}
		});
		clusterButton.setVisible(false);
		
		panel.add(clusterButton, BorderLayout.SOUTH);
		panel.add(statusLabel, BorderLayout.CENTER);
				
		return panel;
	}
	
	private JPanel makeClusterPanel(){
		JPanel panel = new JPanel(new BorderLayout());
		
		treeView = new JScrollPane(tree);
		panel.add(treeView, BorderLayout.CENTER);
		treeView.setVisible(false);
			
		statusPanel = makeStatusPanel();
		panel.add(statusPanel, BorderLayout.NORTH);
		
		return panel;
	}
	
	private JPanel makeMergeSourcesPanel(){
		JPanel panel = new JPanel(new BorderLayout());
		mergeSources = new JTable(makeBlankTable()){
			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {
			    return false;
			}
		};
		mergeSources.setEnabled(true);
		mergeSources.setCellSelectionEnabled(false);
		mergeSources.setColumnSelectionAllowed(false);
		mergeSources.setRowSelectionAllowed(true);
		
		panel.add(mergeSources, BorderLayout.CENTER);
		panel.add(mergeSources.getTableHeader(), BorderLayout.NORTH);
		
		getSourceButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				
				// get the dataset name selected
				String name = (String) mergeSources.getModel().getValueAt(mergeSources.getSelectedRow(), 0);

				fireSignalChangeEvent("ExtractSource_"+name);

			}
		});
		getSourceButton.setVisible(false);
		panel.add(getSourceButton, BorderLayout.SOUTH);
		return panel;
	}
	
	private DefaultTableModel makeBlankTable(){
		DefaultTableModel model = new DefaultTableModel();

		Vector<Object> names 	= new Vector<Object>();
		Vector<Object> nuclei 	= new Vector<Object>();

		names.add("No merge sources");
		nuclei.add("");


		model.addColumn("Merge source", names);
		model.addColumn("Nuclei", nuclei);
		return model;
	}
	
	public void update(List<AnalysisDataset> list){
		
		clusterButton.setVisible(false);
		getSourceButton.setVisible(false);
		
		if(list.size()==1){
			AnalysisDataset dataset = list.get(0);
			
			// If no clusters are present, show the button
			if(!dataset.hasClusters()){
				
				statusLabel.setText("Dataset contains no clusters");
				clusterButton.setVisible(true);
				tree.setText("");
				treeView.setVisible(false);

				
			} else {
				 // clusters present, show the tree if available
				// Show a line indicating clusters are present anyway
				statusLabel.setText("Dataset contains "+dataset.getClusterIDs().size()+" clusters");
				
				String treeString = dataset.getClusterTree();
				if(treeString!=null){
					tree.setText(treeString);
					tree.setLineWrap(true);
					treeView.revalidate();
					treeView.setVisible(true);
				}
				

			}
			if(dataset.hasMergeSources()){
				
				DefaultTableModel model = new DefaultTableModel();

				Vector<Object> names 	= new Vector<Object>();
				Vector<Object> nuclei 	= new Vector<Object>();

				for( UUID id : dataset.getMergeSources()){
					AnalysisDataset mergeSource = dataset.getMergeSource(id);
					names.add(mergeSource.getName());
					nuclei.add(mergeSource.getCollection().getNucleusCount());
				}
				model.addColumn("Merge source", names);
				model.addColumn("Nuclei", nuclei);

				mergeSources.setModel(model);
				getSourceButton.setVisible(true);
				
			} else {
				try{
				mergeSources.setModel(makeBlankTable());
				} catch (Exception e){
//					TODO: fix error
				}
			}
		} else { // more than one dataset selected
			statusLabel.setText("Multiple datasets selected");
			clusterButton.setVisible(false);
			tree.setText("");
			treeView.setVisible(false);
			mergeSources.setModel(makeBlankTable());
			
		}
		
	}
}
