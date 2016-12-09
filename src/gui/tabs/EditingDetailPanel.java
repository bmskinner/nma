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

import gui.DatasetEventListener;
import gui.InterfaceEventListener;
import gui.SignalChangeEvent;
import gui.SignalChangeListener;
import gui.tabs.editing.BorderTagEditingPanel;
import gui.tabs.editing.SegmentsEditingPanel;

import java.awt.BorderLayout;

import javax.swing.JTabbedPane;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import charting.options.ChartOptions;
import charting.options.TableOptions;

@SuppressWarnings("serial")
public class EditingDetailPanel extends DetailPanel implements SignalChangeListener, DatasetEventListener, InterfaceEventListener {
	
	private JTabbedPane tabPane;
	protected CellDetailPanel		cellDetailPanel;
	protected SegmentsEditingPanel segmentsEditingPanel;
	protected BorderTagEditingPanel borderTagEditingPanel;
	
	
	
	public EditingDetailPanel(){
		
		super();
		
		this.setLayout(new BorderLayout());
		tabPane = new JTabbedPane();
		this.add(tabPane, BorderLayout.CENTER);
		
		cellDetailPanel = new CellDetailPanel();
		this.addSubPanel(cellDetailPanel);
		this.addSignalChangeListener(cellDetailPanel);
		tabPane.addTab("Cells", cellDetailPanel);
		
		/*
		 * Signals come from the segment panel to this container
		 * Signals can be sent to the segment panel
		 * Events come from the panel only
		 */
		segmentsEditingPanel = new SegmentsEditingPanel();
		segmentsEditingPanel.addSignalChangeListener(this);
		this.addSignalChangeListener(segmentsEditingPanel);
		this.addSubPanel(segmentsEditingPanel);
		tabPane.addTab("Segmentation", segmentsEditingPanel);
		
		/*
		 * Edit the border tag locations on the median profile
		 */
		borderTagEditingPanel = new BorderTagEditingPanel();
		borderTagEditingPanel.addSignalChangeListener(this);
		this.addSignalChangeListener(borderTagEditingPanel);
		this.addSubPanel(borderTagEditingPanel);
		tabPane.addTab("Border tags", borderTagEditingPanel);

		
	}
	
	
	/**
	 * This method must be overridden by the extending class
	 * to perform the actual update when a single dataset is selected
	 */
	protected void updateSingle() {
		updateMultiple();
	}
	
	/**
	 * This method must be overridden by the extending class
	 * to perform the actual update when a multiple datasets are selected
	 */
	protected void updateMultiple() {
		cellDetailPanel.setEnabled(true);
		segmentsEditingPanel.setEnabled(true);

//		cellDetailPanel.update(getDatasets()); 
//		finest("Updated segments boxplot panel");
//
//		segmentsEditingPanel.update(getDatasets()); 
//		finest("Updated segments editing panel");
//		
//		borderTagEditingPanel.update(getDatasets()); 
//		finest("Updated border tag editing panel");
	}
	
	/**
	 * This method must be overridden by the extending class
	 * to perform the actual update when a no datasets are selected
	 */
	protected void updateNull() {
		updateMultiple();
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
		
		finer("Editing panel heard signal: "+event.type());
		
		if(event.sourceName().equals("CellDetailPanel") 
				|| event.sourceName().equals("SegmentsEditingPanel")
				|| event.sourceName().equals("BorderTagEditingPanel")){
			fireSignalChangeEvent(event.type());			
		} 
			
		cellDetailPanel.signalChangeReceived(event);
		segmentsEditingPanel.signalChangeReceived(event);
		borderTagEditingPanel.signalChangeReceived(event);

		
	}
}
