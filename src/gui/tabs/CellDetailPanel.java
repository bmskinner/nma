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
import gui.DatasetEvent;
import gui.SignalChangeEvent;
import gui.SignalChangeListener;
import gui.tabs.cells.AbstractCellDetailPanel;
import gui.tabs.cells.CellBorderTagPanel;
import gui.tabs.cells.CellOutlinePanel;
import gui.tabs.cells.CellProfilePanel;
import gui.tabs.cells.CellStatsPanel;
import gui.tabs.cells.CellViewModel;
import gui.tabs.cells.CellsListPanel;
import gui.tabs.cells.ComponentListPanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.ListModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import org.jfree.chart.JFreeChart;

import analysis.AnalysisDataset;
import charting.datasets.SignalTableCell;
import charting.options.ChartOptions;
import charting.options.TableOptions;
import components.Cell;
import components.CellularComponent;
import components.nuclear.NuclearSignal;

@SuppressWarnings("serial")
public class CellDetailPanel extends DetailPanel implements SignalChangeListener {
		
	private JTabbedPane tabPane; 
	
	protected CellsListPanel	 cellsListPanel;		// the list of cells in the active dataset
	protected CellProfilePanel	 segmentProfilePanel;// = new CellProfilePanel(); 		// the nucleus angle profile
	protected CellBorderTagPanel cellBorderTagPanel;//  = new CellBorderTagPanel();
	protected CellOutlinePanel 	 outlinePanel  ;//      = new CellOutlinePanel(); 		// the outline of the cell and detected objects
	protected CellStatsPanel 	 cellStatsPanel ;//     = new CellStatsPanel();		// the stats table
	protected ComponentListPanel signalListPanel;	// choose which background image to display

	private CellViewModel model                      = new CellViewModel(null, null);
	
	public CellDetailPanel() {

		super();

		try{
			
			createSubPanels();
			
			this.setLayout(new BorderLayout());
			
			this.add(createCellandSignalListPanels(), BorderLayout.WEST);
			
			
			this.addSubPanel(cellStatsPanel);
			this.addSubPanel(segmentProfilePanel);
			this.addSubPanel(cellBorderTagPanel);
			this.addSubPanel(outlinePanel);
			this.addSubPanel(cellsListPanel);
			this.addSubPanel(signalListPanel);
			
			
			
//			cellStatsPanel.setParent(this);
//			segmentProfilePanel.setParent(this);
//			cellBorderTagPanel.setParent(this);
//			outlinePanel.setParent(this);
			
			tabPane = new JTabbedPane(JTabbedPane.LEFT);
			this.add(tabPane, BorderLayout.CENTER);
			
			tabPane.add("Info", cellStatsPanel);
			
			tabPane.add("Segments", segmentProfilePanel);
			
			tabPane.add("Tags", cellBorderTagPanel);
			
			tabPane.add("Outline", outlinePanel);

			

			this.validate();
		} catch(Exception e){
			error("Error creating cell detail panel", e);
		}

	}
	
	private void createSubPanels(){
		segmentProfilePanel = new CellProfilePanel(model); 		// the nucleus angle profile
		cellBorderTagPanel  = new CellBorderTagPanel(model);
		outlinePanel        = new CellOutlinePanel(model); 		// the outline of the cell and detected objects
		cellStatsPanel      = new CellStatsPanel(model);		// the stats table
		
		model.addView(segmentProfilePanel);
		model.addView(cellBorderTagPanel);
		model.addView(outlinePanel);
		model.addView(cellStatsPanel);
	}
	
	private JPanel createCellandSignalListPanels(){
		JPanel panel = new JPanel(new GridBagLayout());
		
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.BOTH;
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.gridheight = 2;
		constraints.gridwidth = 1;
		constraints.weightx = 0.5;
		constraints.weighty = 0.6;
		constraints.anchor = GridBagConstraints.CENTER;

		cellsListPanel = new CellsListPanel(model);
		model.addView(cellsListPanel);
		cellsListPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		panel.add(cellsListPanel, constraints);
		
		constraints.gridx = 0;
		constraints.gridy = 2;
		constraints.gridheight = 2;
		constraints.gridwidth = 1;
		constraints.weightx = 0.5;
		constraints.weighty = 0.4;
		signalListPanel = new ComponentListPanel(model);
		model.addView(signalListPanel);
		signalListPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		panel.add(signalListPanel, constraints);
		
		return panel;
	}
				
	@Override
	protected void updateSingle() {
		cellsListPanel.update(getDatasets());
		outlinePanel.update(getDatasets());
		cellStatsPanel.update(getDatasets());
		segmentProfilePanel.update(getDatasets());
		cellBorderTagPanel.update(getDatasets());
		signalListPanel.update(getDatasets());
		
		if( model.hasCell() && ! activeDataset().getCollection().contains(model.getCell())){
			model.setCell(null);
		}
		
		
		finest("Updated cell list panel");
//		updateCell(activeCell);
		finest("Updated active cell panel");
	}
	
	@Override
	protected void updateMultiple() {
		updateNull();
	}
	
	@Override
	protected void updateNull() {
//		activeCell=null;
		cellsListPanel.update(getDatasets());
		outlinePanel.update(getDatasets());
		cellStatsPanel.update(getDatasets());
		segmentProfilePanel.update(getDatasets());
		cellBorderTagPanel.update(getDatasets());
		signalListPanel.update(getDatasets());
		model.setCell(null);
		model.setComponent(null);
		
		finest("Updated cell list panel");
	}
		
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return null;
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception{
		return null;
	}
	

	@Override
	public void signalChangeReceived(SignalChangeEvent event) {
		if(event.type().equals("SignalColourUpdate")){
			model.updateViews();
		}

	}
	
}
