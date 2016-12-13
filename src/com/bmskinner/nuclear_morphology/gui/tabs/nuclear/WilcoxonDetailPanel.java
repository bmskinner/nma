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
package com.bmskinner.nuclear_morphology.gui.tabs.nuclear;

import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.charting.datasets.AbstractDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.AnalysisDatasetTableCreator;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.stats.NucleusStatistic;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.components.WilcoxonTableCellRenderer;
import com.bmskinner.nuclear_morphology.gui.tabs.AbstractPairwiseDetailPanel;

@SuppressWarnings("serial")
public class WilcoxonDetailPanel extends AbstractPairwiseDetailPanel {
		
	public WilcoxonDetailPanel() throws Exception {
		super();
	}
	
	@Override
	protected void updateSingle() {
		scrollPane.setColumnHeaderView(null);
		tablePanel = createTablePanel();
		
		JPanel panel = new JPanel(new FlowLayout());
		panel.add(new JLabel(Labels.SINGLE_DATASET, JLabel.CENTER));
		tablePanel.add(panel);

		scrollPane.setViewportView(tablePanel);;
		tablePanel.repaint();
	}
	

	@Override
	protected void updateMultiple() {
		scrollPane.setColumnHeaderView(null);
		tablePanel = createTablePanel();
		for(NucleusStatistic stat : NucleusStatistic.values()){

			ExportableTable table = new ExportableTable(AbstractDatasetCreator.createLoadingTable());
			
			TableOptions options = new TableOptionsBuilder()
				.setDatasets(getDatasets())
				.addStatistic(stat)
				.setTarget(table)
				.setRenderer(TableOptions.ALL_EXCEPT_FIRST_COLUMN, new WilcoxonTableCellRenderer())
				.build();

			addWilconxonTable(tablePanel, table, stat.toString());
			scrollPane.setColumnHeaderView(table.getTableHeader());
			setTable(options);


		}
		tablePanel.revalidate();
		scrollPane.setViewportView(tablePanel);;
		tablePanel.repaint();
		
	}
	
	@Override
	protected void updateNull() {		
		scrollPane.setColumnHeaderView(null);
		tablePanel = createTablePanel();
		
		JPanel panel = new JPanel(new FlowLayout());
		panel.add(new JLabel(Labels.NO_DATA_LOADED, JLabel.CENTER));
		tablePanel.add(panel);
		scrollPane.setViewportView(tablePanel);;
		tablePanel.repaint();
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options){
		return new AnalysisDatasetTableCreator(options).createWilcoxonStatisticTable();
	}
}
