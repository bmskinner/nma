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
package gui.tabs.nuclear;

import javax.swing.JLabel;
import javax.swing.table.TableModel;

import charting.datasets.AnalysisDatasetTableCreator;
import charting.options.TableOptions;
import charting.options.TableOptionsBuilder;
import gui.components.ExportableTable;
import gui.components.WilcoxonTableCellRenderer;
import gui.tabs.AbstractPairwiseDetailPanel;
import stats.NucleusStatistic;

@SuppressWarnings("serial")
public class WilcoxonDetailPanel extends AbstractPairwiseDetailPanel {
		
	public WilcoxonDetailPanel() throws Exception {
		super();
	}
	
	@Override
	protected void updateSingle() {
		scrollPane.setColumnHeaderView(null);
		tablePanel = createTablePanel();
		tablePanel.add(new JLabel("Single dataset selected", JLabel.CENTER));
		scrollPane.setViewportView(tablePanel);;
		tablePanel.repaint();
	}
	

	@Override
	protected void updateMultiple() {
		scrollPane.setColumnHeaderView(null);
		tablePanel = createTablePanel();
		for(NucleusStatistic stat : NucleusStatistic.values()){

			ExportableTable table = new ExportableTable();
			
			TableOptions options = new TableOptionsBuilder()
				.setDatasets(getDatasets())
				.addStatistic(stat)
				.setTarget(table)
				.setRenderer(new WilcoxonTableCellRenderer())
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
		tablePanel.add(new JLabel("No datasets selected", JLabel.CENTER));
		scrollPane.setViewportView(tablePanel);;
		tablePanel.repaint();
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception{
		return new AnalysisDatasetTableCreator(options).createWilcoxonStatisticTable();
	}
}
