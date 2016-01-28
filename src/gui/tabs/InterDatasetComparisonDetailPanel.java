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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import analysis.AnalysisDataset;

@SuppressWarnings("serial")
public class InterDatasetComparisonDetailPanel extends DetailPanel {
	
	private VennDetailPanel 	    vennPanel;
	private PairwiseVennDetailPanel pairwiseVennPanel;
	private KruskalDetailPanel 	    kruskalPanel;

	public InterDatasetComparisonDetailPanel(Logger programLogger) {
		super(programLogger);
		
		try {
			
			createUI();
			
		} catch (Exception e) {
			programLogger.log(Level.SEVERE, "Error creating inter-dataset panel", e);
		}
		
	}
	
	private void createUI() throws Exception {
		this.setLayout(new BorderLayout());
		JTabbedPane tabPanel = new JTabbedPane(JTabbedPane.TOP);

		vennPanel 		  = new VennDetailPanel(programLogger); 
		pairwiseVennPanel = new PairwiseVennDetailPanel(programLogger);
		kruskalPanel	  = new KruskalDetailPanel(programLogger);
		

		// Add to the tabbed panel
		// Title, icon, component, tooltip
		tabPanel.addTab("Venn"         , null, vennPanel        , null);
		tabPanel.addTab("Pairwise Venn", null, pairwiseVennPanel, null);
		tabPanel.addTab("Kruskal"      , null, kruskalPanel     , null);

		this.add(tabPanel, BorderLayout.CENTER);
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
		vennPanel.update(getDatasets());
		programLogger.log(Level.FINEST, "Updating Venn panel");
		
		pairwiseVennPanel.update(getDatasets());
		programLogger.log(Level.FINEST, "Updating pairwise Venn panel");
		
		kruskalPanel.update(getDatasets());
		programLogger.log(Level.FINEST, "Updating Kruskal panel");
	}
	
	/**
	 * This method must be overridden by the extending class
	 * to perform the actual update when a no datasets are selected
	 */
	protected void updateNull() throws Exception {
		updateMultiple();
	}
	
//	@Override
//	protected void updateDetail(){
//
//		SwingUtilities.invokeLater(new Runnable(){
//			public void run(){
//				try {
//
//					vennPanel.update(getDatasets());
//					programLogger.log(Level.FINEST, "Updating Venn panel");
//					
//					pairwiseVennPanel.update(getDatasets());
//					programLogger.log(Level.FINEST, "Updating pairwise Venn panel");
//					
//					kruskalPanel.update(getDatasets());
//					programLogger.log(Level.FINEST, "Updating Kruskal panel");
//
//				} catch  (Exception e){
//					InterDatasetComparisonDetailPanel.this.update( (List<AnalysisDataset>) null);
//					programLogger.log(Level.SEVERE, "Error updating inter-dataset panels", e);
//
//				} finally {
//					setUpdating(false);
//				}
//			}});
//	}

}
