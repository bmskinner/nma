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
package gui.tabs;


import gui.DatasetEvent.DatasetMethod;
import jebl.evolution.io.ImportException;
import jebl.evolution.io.NewickImporter;
import jebl.evolution.trees.RootedTree;
import jebl.evolution.trees.Tree;
import jebl.gui.trees.treeviewer.TreePane;
import jebl.gui.trees.treeviewer.TreeViewer;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;

import analysis.AnalysisDataset;
import charting.datasets.NucleusTableDatasetCreator;
import components.ClusterGroup;

@SuppressWarnings("serial")
public class ClusterDetailPanel extends DetailPanel {
			
	private ClustersPanel clusterPanel;

	public ClusterDetailPanel(Logger programLogger) {
		super(programLogger);
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		
		clusterPanel = new ClustersPanel();
		this.add(clusterPanel);

	}
		
	public void update(final List<AnalysisDataset> list){
		programLogger.log(Level.FINE, "Updating cluster panel");
		this.list = list;
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				
				clusterPanel.update(list);		
				programLogger.log(Level.FINEST, "Updated cluster panel");
			}
		});
		
	}
		
	private class ClustersPanel extends JPanel {
		
		private JButton 	clusterButton	= new JButton("Cluster population");
		private JLabel		statusLabel 	= new JLabel("No clusters present", SwingConstants.CENTER);
		private JPanel		statusPanel		= new JPanel(new BorderLayout());
		
//		private TreePane viewer = new TreePane();

		
		private JPanel tablesPanel;
		private JTable clusterDetailsTable; 
		
		public ClustersPanel(){
			this.setLayout(new BorderLayout());
		
			
			tablesPanel = new JPanel();
			tablesPanel.setLayout(new BoxLayout(tablesPanel, BoxLayout.Y_AXIS));
					
			
			JPanel clusterDetailPanel = new JPanel(new BorderLayout());
			TableModel optionsModel = NucleusTableDatasetCreator.createClusterOptionsTable(null);
			clusterDetailsTable = new JTable(optionsModel){
				@Override
				public boolean isCellEditable(int rowIndex, int columnIndex) {
				    return false;
				}
			};
			JScrollPane detailScrollPanel = new JScrollPane(clusterDetailsTable);
			clusterDetailPanel.add(detailScrollPanel, BorderLayout.CENTER);
			clusterDetailPanel.add(clusterDetailsTable.getTableHeader(), BorderLayout.NORTH);
			
			tablesPanel.add(clusterDetailPanel);
				
			this.add(tablesPanel, BorderLayout.CENTER);
			statusPanel = makeStatusPanel();
			this.add(statusPanel, BorderLayout.NORTH);
			
//			this.add(viewer, BorderLayout.SOUTH);


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
					fireDatasetEvent(DatasetMethod.CLUSTER, list);
					

				}
			});
			clusterButton.setVisible(false);
			
			panel.add(clusterButton, BorderLayout.SOUTH);
			panel.add(statusLabel, BorderLayout.CENTER);
					
			return panel;
		}

		public void update(List<AnalysisDataset> list){
			clusterButton.setVisible(true);
//			treeViewer.setVisible(false);
			
			TableModel optionsModel = NucleusTableDatasetCreator.createClusterOptionsTable(list);
			clusterDetailsTable.setModel(optionsModel);
			
			if(list.isEmpty() || list==null){
				statusLabel.setText("No datasets selected");
			} else {

				if(list.size()==1){
					AnalysisDataset dataset = list.get(0);

					if(!dataset.hasClusters()){

						statusLabel.setText("Dataset contains no clusters");

					} else {
						statusLabel.setText("Dataset has "+dataset.getClusterGroups().size()+" cluster groups");
						
						for(ClusterGroup g : dataset.getClusterGroups()){
							
							String newickTree = g.getTree();

							if(newickTree!=null){
								
//								treeLabel.setText(newickTree);
//								viewer.setVisible(true);
//								programLogger.log(Level.FINE, "Reading tree");
//								StringReader reader = new StringReader(newickTree);
//
//								boolean readUnquotedLabels = true;
//								NewickImporter imp = new NewickImporter(reader, readUnquotedLabels);

//								try {
//									List<Tree> trees =  imp.importTrees();
//									final RootedTree topTree = (RootedTree) trees.get(0);

//									programLogger.log(Level.FINEST, topTree.toString());
//									Thread thr = new Thread(){
//										public void run(){
//											viewer.setTree( topTree, topTree.getNodes() );
//										}
//									};
//									thr.start();
									

//								} catch (IOException e) {
//									programLogger.log(Level.SEVERE, "Error reading tree", e);
//								} catch (ImportException e) {
//									programLogger.log(Level.SEVERE, "Error in tree IO", e);
//								}
							} 
						}
						
					}
				} else { // more than one dataset selected
					statusLabel.setText("Multiple datasets selected");
					clusterButton.setVisible(false);
				}
			}
		}

	}
}
