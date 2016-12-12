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
package com.bmskinner.nuclear_morphology.gui.tabs;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.charting.datasets.AnalysisDatasetTableCreator;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.IClusterGroup;
import com.bmskinner.nuclear_morphology.gui.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.DatasetEventListener;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.dialogs.ClusterTreeDialog;

/**
 * This panel shows any cluster groups that have been created, and the 
 * clustering options that were used to create them. 
 * @author bms41
 * @since 1.9.0
 *
 */
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
		finest("Updated cluster panel");
		
	}

	@Override
	protected void updateNull() {
		updateMultiple();
		
	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) {
		return null;
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options) {
		return new AnalysisDatasetTableCreator(options).createClusterOptionsTable();
	}
	
	@Override
	public void datasetEventReceived(DatasetEvent event){
		super.datasetEventReceived(event);
		
		if( event.getSource() instanceof ClusterTreeDialog){
			fireDatasetEvent(event);
		}
	}
	
	public void interfaceEventReceived(InterfaceEvent event){
    	super.interfaceEventReceived(event);
    	if( event.getSource() instanceof ClusterTreeDialog){
			fireInterfaceEvent(event);
		}
    }
				
	private class ClustersPanel extends JPanel {
		
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
			TableModel optionsModel = AnalysisDatasetTableCreator.createBlankTable();
			clusterDetailsTable = new ExportableTable(optionsModel){
				@Override
				public boolean isCellEditable(int rowIndex, int columnIndex) {
				    return false;
				}
			};

			setRenderer(clusterDetailsTable, new ClusterTableCellRenderer());
			
			
			JScrollPane detailScrollPanel = new JScrollPane(clusterDetailsTable);
			clusterDetailPanel.add(detailScrollPanel, BorderLayout.CENTER);
			clusterDetailPanel.add(clusterDetailsTable.getTableHeader(), BorderLayout.NORTH);
			
			tablesPanel.add(clusterDetailPanel);
			
			showTreeButtonPanel = createTreeButtonPanel(null);
			tablesPanel.add(showTreeButtonPanel);
			
				
			this.add(tablesPanel, BorderLayout.CENTER);
			statusPanel = makeStatusPanel();
			setEnabled(false);
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
			
			for(final IAnalysisDataset d : getDatasets()){
				
				for(final IClusterGroup g : d.getClusterGroups()){
					
					if(g.hasTree()){
						JButton button = new JButton("Show tree");
						button.addActionListener( e ->{
								Thread thr = new Thread(){
									public void run(){
										ClusterTreeDialog clusterPanel = new ClusterTreeDialog( d, g);
										clusterPanel.addDatasetEventListener(ClusterDetailPanel.this);
										clusterPanel.addInterfaceEventListener(ClusterDetailPanel.this);
									}};
									thr.start();
							}
						);    
						result.add(button);
					} else {
						result.add(new Box.Filler(fillerSize, fillerSize, fillerSize));
					}
					
				}
			}
			
			return result;
		}
		
				
		@Override
		public void setEnabled(boolean b){
			super.setEnabled(b);
			clusterButton.setEnabled(b);
			buildTreeButton.setEnabled(b);
//			saveClassifierButton.setEnabled(b); // not yet enabled
		}
		/**
		 * This panel shows the status of the dataset, 
		 * and holds the clustering button
		 * @return
		 */
		private JPanel makeStatusPanel(){
			
			JPanel panel = new JPanel(new BorderLayout());
			
			JPanel buttonPanel = new JPanel(new FlowLayout());
			clusterButton.addActionListener( e -> {
					fireDatasetEvent(DatasetEvent.CLUSTER, getDatasets()); 
			});
			
			buildTreeButton.addActionListener( e -> {
					fireDatasetEvent(DatasetEvent.BUILD_TREE, getDatasets());
			});
			
			saveClassifierButton.addActionListener( e -> {
					fireDatasetEvent(DatasetEvent.TRAIN_CLASSIFIER, getDatasets());
			});
			
			saveClassifierButton.setEnabled(false);
			buildTreeButton.setEnabled(true);
			buttonPanel.add(buildTreeButton);
			buttonPanel.add(clusterButton);
//			buttonPanel.add(saveClassifierButton);
			
			panel.add(buttonPanel, BorderLayout.SOUTH);
			panel.add(statusLabel, BorderLayout.CENTER);
					
			return panel;
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

		public void update(List<IAnalysisDataset> list){

			setEnabled(true);
			
			TableOptions options = new TableOptionsBuilder()
				.setDatasets(getDatasets())
				.setTarget(clusterDetailsTable)
				.setRenderer(TableOptions.ALL_EXCEPT_FIRST_COLUMN, new ClusterTableCellRenderer())
				.build();
			
			setTable(options);
			

			updateTreeButtonsPanel();
			
			if( ! hasDatasets()){
				statusLabel.setText(Labels.NULL_DATASETS);
				setEnabled(false);
			} else {
				
				if(isSingleDataset()){
					
					setEnabled(true);

					if(!activeDataset().hasClusters()){

						statusLabel.setText("Dataset contains no clusters");

					} else {
						statusLabel.setText("Dataset has "+activeDataset().getClusterGroups().size()+" cluster groups");						
					}
				} else { // more than one dataset selected
					statusLabel.setText(Labels.MULTIPLE_DATASETS);
					setEnabled(false);
				}
			}
		}

	}
	
	

	/**
	 * Colour analysis parameter table cell background. If parameters are false or N/A, 
	 * make the text colour grey
	 */
	public class ClusterTableCellRenderer extends DefaultTableCellRenderer {

		public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {

			
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	        
			Color colour = Color.BLACK;

			if(value !=null && !value.toString().equals("")){
			
				if(value.toString().equals("false") || value.toString().equals("N/A")){
					colour = Color.GRAY;
				}
			}


			setForeground(colour);
			return this;
		}

	}

}
