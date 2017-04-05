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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.gui.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.gui.SignalChangeListener;
import com.bmskinner.nuclear_morphology.gui.tabs.cells_detail.CellBorderTagPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.cells_detail.CellOutlinePanel;
import com.bmskinner.nuclear_morphology.gui.tabs.cells_detail.CellProfilePanel;
import com.bmskinner.nuclear_morphology.gui.tabs.cells_detail.CellSegTablePanel;
import com.bmskinner.nuclear_morphology.gui.tabs.cells_detail.CellSignalStatsPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.cells_detail.CellStatsPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.cells_detail.CellViewModel;
import com.bmskinner.nuclear_morphology.gui.tabs.cells_detail.CellsListPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.cells_detail.ComponentListPanel;

@SuppressWarnings("serial")
public class IndividualCellDetailPanel extends DetailPanel implements SignalChangeListener {
		
	private JTabbedPane tabPane; 
	
	private static final String CELL_INFO_LBL    = "Info";
	private static final String CELL_SEGS_LBL    = "Segments";
	private static final String CELL_TAGS_LBL    = "Tags";
	private static final String CELL_OUTLINE_LBL = "Outline";
	private static final String CELL_SIGNALS_LBL = "Signals";
	private static final String CELL_SEGTABLE_LBL  = "Segtable";
	
	protected CellsListPanel	 cellsListPanel;		// the list of cells in the active dataset
	protected CellProfilePanel	 segmentProfilePanel;// = new CellProfilePanel(); 		// the nucleus angle profile
	protected CellBorderTagPanel cellBorderTagPanel;//  = new CellBorderTagPanel();
	protected CellOutlinePanel 	 outlinePanel  ;//      = new CellOutlinePanel(); 		// the outline of the cell and detected objects
	protected CellStatsPanel 	 cellStatsPanel ;//     = new CellStatsPanel();		// the stats table
	protected ComponentListPanel signalListPanel;	// choose which background image to display
	protected CellSignalStatsPanel 	 cellsignalStatsPanel ; // show pairwise distances for signals
	protected CellSegTablePanel 	 cellSegTablePanel ; // show segments in cell profiles
	
	
	private CellViewModel model   = new CellViewModel(null, null);
	
	public IndividualCellDetailPanel() {

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
			this.addSubPanel(cellsignalStatsPanel);
			this.addSubPanel(cellSegTablePanel);
			
			tabPane = new JTabbedPane(JTabbedPane.LEFT);
			this.add(tabPane, BorderLayout.CENTER);
			
			tabPane.add(CELL_INFO_LBL, cellStatsPanel);
			tabPane.add(CELL_SEGS_LBL, segmentProfilePanel);
			tabPane.add(CELL_TAGS_LBL, cellBorderTagPanel);
			tabPane.add(CELL_OUTLINE_LBL, outlinePanel);
			tabPane.add(CELL_SIGNALS_LBL, cellsignalStatsPanel);
			tabPane.add(CELL_SEGTABLE_LBL, cellSegTablePanel);
		
			this.validate();
		} catch(Exception e){
			warn("Error creating cell detail panel");
			stack("Error creating cell detail panel", e);
		}

	}
		
	private void createSubPanels(){
		segmentProfilePanel = new CellProfilePanel(model); 		// the nucleus angle profile
		cellBorderTagPanel  = new CellBorderTagPanel(model);
		outlinePanel        = new CellOutlinePanel(model); 		// the outline of the cell and detected objects
		cellStatsPanel      = new CellStatsPanel(model);		// the stats table
		cellsignalStatsPanel= new CellSignalStatsPanel(model);
		cellSegTablePanel   = new CellSegTablePanel(model);
		
		model.addView(segmentProfilePanel);
		model.addView(cellBorderTagPanel);
		model.addView(outlinePanel);
		model.addView(cellStatsPanel);
		model.addView(cellsignalStatsPanel);
		model.addView(cellSegTablePanel);
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
		
		if( model.hasCell() && ! activeDataset().getCollection().containsExact(model.getCell())){
			model.setCell(null);
		}
	}
	
	@Override
	protected void updateMultiple() {
		updateNull();
	}
	
	@Override
	protected void updateNull() {
		model.setCell(null);
		model.setComponent(null);
	}
		
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) {
		return null;
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options){
		return null;
	}
	

	@Override
	public void signalChangeReceived(SignalChangeEvent event) {
		if(event.type().equals(SignalChangeEvent.SIGNAL_COLOUR_CHANGE)){
			model.updateViews();
		}

	}
	
}
