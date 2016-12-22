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
import javax.swing.JTabbedPane;

@SuppressWarnings("serial")
public class InterDatasetComparisonDetailPanel extends DetailPanel {
	
	private static final String BASIC_VENN_TAB_LBL  = "Venn";
	private static final String DETAIL_VENN_TAB_LBL = "Detailed Venn";
	private static final String KRUSKAL_TAB_LBL     = "Kruskal";

	public InterDatasetComparisonDetailPanel() {
		super();
		
		this.setLayout(new BorderLayout());
		
		JTabbedPane tabPanel = new JTabbedPane(JTabbedPane.TOP);

		DetailPanel vennPanel 		  = new VennDetailPanel(); 
		DetailPanel pairwiseVennPanel = new PairwiseVennDetailPanel();
		DetailPanel kruskalPanel	  = new KruskalDetailPanel();
		
		this.addSubPanel(vennPanel);
		this.addSubPanel(pairwiseVennPanel);
		this.addSubPanel(kruskalPanel);

		tabPanel.addTab(BASIC_VENN_TAB_LBL , vennPanel        );
		tabPanel.addTab(DETAIL_VENN_TAB_LBL, pairwiseVennPanel);
		tabPanel.addTab(KRUSKAL_TAB_LBL    , kruskalPanel     );

		this.add(tabPanel, BorderLayout.CENTER);
	}
}
