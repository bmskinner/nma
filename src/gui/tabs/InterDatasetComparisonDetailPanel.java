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

import java.awt.BorderLayout;
import java.util.logging.Level;

import javax.swing.JTabbedPane;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import charting.options.ChartOptions;
import charting.options.TableOptions;

@SuppressWarnings("serial")
public class InterDatasetComparisonDetailPanel extends DetailPanel {
	
	private VennDetailPanel 	    vennPanel;
	private PairwiseVennDetailPanel pairwiseVennPanel;
	private KruskalDetailPanel 	    kruskalPanel;

	public InterDatasetComparisonDetailPanel() {
		super();
		
		try {
			
			createUI();
			
		} catch (Exception e) {
			log(Level.SEVERE, "Error creating inter-dataset panel", e);
		}
		
	}
	
	private void createUI() throws Exception {
		this.setLayout(new BorderLayout());
		JTabbedPane tabPanel = new JTabbedPane(JTabbedPane.TOP);

		vennPanel 		  = new VennDetailPanel(); 
		pairwiseVennPanel = new PairwiseVennDetailPanel();
		kruskalPanel	  = new KruskalDetailPanel();
		
		this.addSubPanel(vennPanel);
		this.addSubPanel(pairwiseVennPanel);
		this.addSubPanel(kruskalPanel);
		

		// Add to the tabbed panel
		// Title, icon, component, tooltip
		tabPanel.addTab("Venn"         , null, vennPanel        , null);
		tabPanel.addTab("Detailed Venn", null, pairwiseVennPanel, null);
		tabPanel.addTab("Kruskal"      , null, kruskalPanel     , null);

		this.add(tabPanel, BorderLayout.CENTER);
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
		vennPanel.update(getDatasets());
		log(Level.FINEST, "Updating Venn panel");
		
		pairwiseVennPanel.update(getDatasets());
		log(Level.FINEST, "Updating pairwise Venn panel");
		
		kruskalPanel.update(getDatasets());
		log(Level.FINEST, "Updating Kruskal panel");
	}
	
	/**
	 * This method must be overridden by the extending class
	 * to perform the actual update when a no datasets are selected
	 */
	protected void updateNull() {
		updateMultiple();
	}
		
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options){
		return null;
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options){
		return null;
	}
	

}
