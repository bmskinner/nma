/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
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
import gui.DatasetEventListener;
import gui.components.ExportableTable;
import gui.dialogs.ClusterTreeDialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import analysis.AnalysisDataset;
import charting.datasets.NucleusTableDatasetCreator;
import charting.options.ChartOptions;
import charting.options.TableOptions;
import components.ClusterGroup;

@SuppressWarnings("serial")
public class ClusterDetailPanel extends DetailPanel implements DatasetEventListener {
			
	private ClustersPanel clusterPanel;

	public ClusterDetailPanel() {
		super();
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		
		clusterPanel = new ClustersPanel();
		this.add(clusterPanel);

	}
	
	@Override
	protected void updateSingle() {
		updateMultiple();
		
	}

	@Override
	protected void updateMultiple() {
		clusterPanel.update(getDatasets());		
		log(Level.FINEST, "Updated cluster panel");
		
	}

	@Override
	protected void updateNull() {
		updateMultiple();
		
	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return null;
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception{
		return null;
	}
				
	private class ClustersPanel extends JPanel implements MouseListener {
		
		private JButton 	clusterButton	= new JButton("Cluster population");
		private JButton 	buildTreeButton	= new JButton("Create tree");
		private JButton 	saveClassifierButton	= new JButton("Create classifier");
		
		private JLabel		statusLabel 	= new JLabel("No clusters present", SwingConstants.CENTER);
		private JPanel		statusPanel		= new JPanel(new BorderLayout());
		
		private JPanel		showTreeButtonPanel;
		
		private JPanel tablesPanel;
		private ExportableTable clusterDetailsTable; 
		
		public ClustersPanel(){
			this.setLayout(new BorderLayout());
		
			
			tablesPanel = new JPanel();
			tablesPanel.setLayout(new BoxLayout(tablesPanel, BoxLayout.Y_AXIS));
					
			
			JPanel clusterDetailPanel = new JPanel(new BorderLayout());
			TableModel optionsModel = NucleusTableDatasetCreator.getInstance().createClusterOptionsTable(null);
			clusterDetailsTable = new ExportableTable(optionsModel){
				@Override
				public boolean isCellEditable(int rowIndex, int columnIndex) {
				    return false;
				}
			};
			clusterDetailsTable.addMouseListener(this);
			setRenderer(clusterDetailsTable, new ClusterTableCellRenderer());
			
			
			JScrollPane detailScrollPanel = new JScrollPane(clusterDetailsTable);
			clusterDetailPanel.add(detailScrollPanel, BorderLayout.CENTER);
			clusterDetailPanel.add(clusterDetailsTable.getTableHeader(), BorderLayout.NORTH);
			
			tablesPanel.add(clusterDetailPanel);
			
			showTreeButtonPanel = createTreeButtonPanel(null);
			tablesPanel.add(showTreeButtonPanel);
			
				
			this.add(tablesPanel, BorderLayout.CENTER);
			statusPanel = makeStatusPanel();
			setButtonsEnabled(false);
			this.add(statusPanel, BorderLayout.NORTH);


		}
		
		private JPanel createTreeButtonPanel(List<JComponent> buttons){
			JPanel panel = new JPanel();
			
			GridBagLayout gbl = new GridBagLayout();
			panel.setLayout(gbl);
			
			GridBagConstraints c = new GridBagConstraints();
			
			c.anchor = GridBagConstraints.CENTER; // place the buttons in the middle of their grid
			c.gridwidth = buttons==null ? 1 : buttons.size()+1; // one button per column, plus a blank
			c.gridheight = 1;
			c.fill = GridBagConstraints.NONE;      // don't resize the buttons
			c.weightx = 1.0; 						// buttons have padding between them
			
			Dimension fillerSize = new Dimension(10, 5);
			panel.add(new Box.Filler(fillerSize, fillerSize, fillerSize), c);
			if(buttons!=null){
				for(JComponent button : buttons){
					panel.add(button, c);
				}
			}

			return panel;
		}
		
		private List<JComponent> createShowTreeButtons(){
			 
			if(!hasDatasets()){
				return null;
			}
			
			List<JComponent> result = new  ArrayList<JComponent>(); 
			Dimension fillerSize = new Dimension(10, 5);
			
			for(final AnalysisDataset d : getDatasets()){
				
				for(final ClusterGroup g : d.getClusterGroups()){
					
					if(g.hasTree()){
						JButton button = new JButton("Show tree");
						button.addActionListener( new ActionListener() {

							public void actionPerformed(ActionEvent e) {
								Thread thr = new Thread(){
									public void run(){
										ClusterTreeDialog clusterPanel = new ClusterTreeDialog( d, g);
										clusterPanel.addDatasetEventListener(ClusterDetailPanel.this);
										clusterPanel.addInterfaceEventListener(ClusterDetailPanel.this);
									}};
									thr.start();
							}
						});    
						result.add(button);
					} else {
						result.add(new Box.Filler(fillerSize, fillerSize, fillerSize));
					}
					
				}
			}
			
			return result;
		}
				
		
		private void setButtonsEnabled(boolean b){
			clusterButton.setEnabled(b);
			buildTreeButton.setEnabled(b);
			saveClassifierButton.setEnabled(false); // not yet enabled
		}
		/**
		 * This panel shows the status of the dataset, 
		 * and holds the clustering button
		 * @return
		 */
		private JPanel makeStatusPanel(){
			
			JPanel panel = new JPanel(new BorderLayout());
			
			JPanel buttonPanel = new JPanel(new FlowLayout());
			clusterButton.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent arg0) {
					fireDatasetEvent(DatasetMethod.CLUSTER, getDatasets());
					

				}
			});
			clusterButton.setVisible(false);
			
			buildTreeButton.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent arg0) {
					fireDatasetEvent(DatasetMethod.BUILD_TREE, getDatasets());
					

				}
			});
			buildTreeButton.setVisible(false);
			
			saveClassifierButton.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent arg0) {
					fireDatasetEvent(DatasetMethod.TRAIN_CLASSIFIER, getDatasets());
					

				}
			});
			saveClassifierButton.setVisible(false);
			
			saveClassifierButton.setEnabled(false);
			buildTreeButton.setEnabled(true);
			buttonPanel.add(buildTreeButton);
			buttonPanel.add(clusterButton);
			buttonPanel.add(saveClassifierButton);
			
			panel.add(buttonPanel, BorderLayout.SOUTH);
			panel.add(statusLabel, BorderLayout.CENTER);
					
			return panel;
		}
		
		private void setButtonsVisible(boolean b){
			clusterButton.setVisible(b);
			saveClassifierButton.setVisible(false);
			buildTreeButton.setVisible(b);
			
		}
		
		private void updateTreeButtonsPanel(){
			
			tablesPanel.remove(showTreeButtonPanel);
			
			List<JComponent> buttons = createShowTreeButtons();
			
			
			showTreeButtonPanel = createTreeButtonPanel(buttons);

			// add this new panel
			tablesPanel.add(showTreeButtonPanel);
			tablesPanel.revalidate();
			tablesPanel.repaint();
			tablesPanel.setVisible(true);
		}

		public void update(List<AnalysisDataset> list){
			setButtonsVisible(true);
			setButtonsEnabled(true);
			
			TableModel optionsModel = NucleusTableDatasetCreator.getInstance().createClusterOptionsTable(list);
			clusterDetailsTable.setModel(optionsModel);
			setRenderer(clusterDetailsTable, new ClusterTableCellRenderer());

			updateTreeButtonsPanel();
			
			if( ! hasDatasets()){
				statusLabel.setText("No datasets selected");
				setButtonsEnabled(false);
			} else {
				
				if(isSingleDataset()){
					
					setButtonsEnabled(true);

					if(!activeDataset().hasClusters()){

						statusLabel.setText("Dataset contains no clusters");

					} else {
						statusLabel.setText("Dataset has "+activeDataset().getClusterGroups().size()+" cluster groups");						
					}
				} else { // more than one dataset selected
					statusLabel.setText("Multiple datasets selected");
//					setButtonsVisible(false);
					setButtonsEnabled(false);
				}
			}
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			JTable table = (JTable) e.getSource();
			int row = table.rowAtPoint((e.getPoint()));
			int column = table.columnAtPoint((e.getPoint()));
			String rowName = table.getModel().getValueAt(row, 0).toString();
			String colName = table.getColumnName(column);
			String groupName = table.getModel().getValueAt(0, column).toString();
			
			// double click
			if (e.getClickCount() == 2) {
				
				if(rowName.equals("Tree") && column > 0 ){
					
					String tree = table.getModel().getValueAt(row, column).toString();
					if(!tree.equals("N/A")){
						
						AnalysisDataset dataset = null;
						for(AnalysisDataset d: getDatasets()){
							if(d.getName().equals(colName)){
								dataset = d;
							}
						}
						
						ClusterGroup group = null;
						for(ClusterGroup g : dataset.getClusterGroups()){
							if(g.getName().equals(groupName)){
								group = g;
							}
						}
						ClusterTreeDialog clusterPanel = new ClusterTreeDialog( dataset, group);
						clusterPanel.addDatasetEventListener(ClusterDetailPanel.this);
					}
				}
				
			}
			
		}

		@Override
		public void mouseEntered(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseExited(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mousePressed(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseReleased(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}

	}
	
	

	/**
	 * Colour analysis parameter table cell background. If all the datasets selected
	 * have the same value, colour them light green
	 */
	@SuppressWarnings("serial")
	public class ClusterTableCellRenderer extends DefaultTableCellRenderer {

		public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {


			Color colour = Color.BLACK;
			JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			if(value !=null && !value.toString().equals("")){
			
				if(value.toString().equals("false") || value.toString().equals("N/A")){
					colour = Color.GRAY;
				}
			}


			l.setForeground(colour);

			return l;
		}

	}

}
