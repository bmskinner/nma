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

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
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

public class ClusterDetailPanel extends DetailPanel {

	private static final long serialVersionUID = 1L;
	
	private List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
		
	private ClustersPanel clusterPanel;
	private Logger programLogger;

	public ClusterDetailPanel(Logger programLogger) {
		super(programLogger);
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		
		clusterPanel = new ClustersPanel();
		this.add(clusterPanel);

	}
		
	public void update(final List<AnalysisDataset> list){
		
		this.list = list;
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				programLogger.log(Level.FINEST, "Updating cluster panel");
				clusterPanel.update(list);		
				programLogger.log(Level.FINEST, "Updated cluster panel");
			}
		});
		
	}
		
	@SuppressWarnings("serial")
	private class ClustersPanel extends JPanel {
		
		private JButton 	clusterButton	= new JButton("Cluster population");
		private JLabel		statusLabel 	= new JLabel("No clusters present", SwingConstants.CENTER);
		private JPanel		statusPanel		= new JPanel(new BorderLayout());
//		private TreePane 	treeViewer; // from jebl, extends JPanel
		
		private JPanel tablesPanel;
		private JTable clusterDetailsTable; 
		
		public ClustersPanel(){
			this.setLayout(new BorderLayout());
//			treeView = new JScrollPane(tree);
//			this.add(treeView, BorderLayout.CENTER);
//			treeView.setVisible(false);
			
			
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
//			this.add(clusterDetailsTable, BorderLayout.SOUTH);
				
			this.add(tablesPanel, BorderLayout.CENTER);
			statusPanel = makeStatusPanel();
			this.add(statusPanel, BorderLayout.NORTH);
			
//			treeViewer = new TreePane();
//			this.add(treeViewer, BorderLayout.SOUTH);

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
//					fireSignalChangeEvent("NewClusterAnalysis");
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
						
//						for(ClusterGroup g : dataset.getClusterGroups()){
//							
//							String newickTree = g.getTree();
//
//							if(newickTree!=null){
//								treeViewer.setVisible(true);
//								StringReader reader = new StringReader(newickTree);
//
//								boolean readUnquotedLabels = true;
//								NewickImporter imp = new NewickImporter(reader, readUnquotedLabels);
//
//								try {
//									List<Tree> trees =  imp.importTrees();
//									RootedTree topTree = (RootedTree) trees.get(0);
//
//									treeViewer.setTree( topTree, topTree.getNodes());
//
//								} catch (IOException e) {
//									error("Error in reader io", e);
//								} catch (ImportException e) {
//									error("Error in tree io", e);
//								}
//							}
//						}
						
					}
				} else { // more than one dataset selected
					statusLabel.setText("Multiple datasets selected");
					clusterButton.setVisible(false);
//					treeViewer.);
				}
			}
		}

	}
}
