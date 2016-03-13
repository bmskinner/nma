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
import java.awt.BorderLayout;
import java.util.logging.Level;
import java.util.logging.Logger;

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

		
	}
	
	
	/**
	 * This method must be overridden by the extending class
	 * to perform the actual update when a single dataset is selected
	 */
	protected void updateSingle() throws Exception {
		updateMultiple();
	}
	
	/**
	 * This method must be overridden by the extending class
	 * to perform the actual update when a multiple datasets are selected
	 */
	protected void updateMultiple() throws Exception {
		cellDetailPanel.setEnabled(true);
		segmentsEditingPanel.setEnabled(true);

		cellDetailPanel.update(getDatasets()); 
		programLogger.log(Level.FINEST, "Updated segments boxplot panel");

		segmentsEditingPanel.update(getDatasets()); 
		programLogger.log(Level.FINEST, "Updated segments histogram panel");
	}
	
	/**
	 * This method must be overridden by the extending class
	 * to perform the actual update when a no datasets are selected
	 */
	protected void updateNull() throws Exception {
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
	
	@Override
	public void signalChangeReceived(SignalChangeEvent event) {
		
		programLogger.log(Level.FINER, "Editing panel heard signal: "+event.type());
		
		if(event.sourceName().equals("CellDetailPanel") || event.sourceName().equals("SegmentsEditingPanel")){
			fireSignalChangeEvent(event.type());			
		} 
			
		cellDetailPanel.signalChangeReceived(event);
		segmentsEditingPanel.signalChangeReceived(event);

		
	}
}
