/*******************************************************************************
 *  	Copyright (C) 2015, 2016 Ben Skinner
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
 *     GNU General Public License for more details. Gluten-free. May contain 
 *     traces of LDL asbestos. Avoid children using heavy machinery while under the
 *     influence of alcohol.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package gui.tabs.segments;

import gui.Labels;
import gui.components.ExportableTable;
import gui.components.WilcoxonTableCellRenderer;
import gui.tabs.AbstractPairwiseDetailPanel;

import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.TableModel;

import stats.Quartile;
import stats.SegmentStatistic;
import charting.datasets.AnalysisDatasetTableCreator;
import charting.options.DefaultTableOptions;
import charting.options.TableOptions;
import charting.options.TableOptionsBuilder;
import components.generic.ProfileType;
import components.generic.Tag;
import components.nuclear.IBorderSegment;
import components.nuclear.NucleusBorderSegment;

@SuppressWarnings("serial")
public class SegmentWilcoxonPanel extends AbstractPairwiseDetailPanel  {
					
	public SegmentWilcoxonPanel(){
		super();
	}

	@Override
	protected void updateSingle() {
		tablePanel = createTablePanel();
		scrollPane.setColumnHeaderView(null);
		
		JPanel labelPanel = new JPanel();
		labelPanel.add(new JLabel(Labels.SINGLE_DATASET, JLabel.CENTER));
		tablePanel.add(labelPanel);

		scrollPane.setViewportView(tablePanel);;
		tablePanel.repaint();
		
	}

	@Override
	protected void updateMultiple() {
		tablePanel = createTablePanel();
		scrollPane.setColumnHeaderView(null);
		
		if(IBorderSegment.segmentCountsMatch(getDatasets())){

			List<IBorderSegment> segments = activeDataset()
					.getCollection()
					.getProfileCollection()
					.getSegments(Tag.REFERENCE_POINT);

			for(SegmentStatistic stat : SegmentStatistic.values()){

				// Get each segment as a boxplot
				for(IBorderSegment seg : segments){

					String segName = seg.getName();

					
					ExportableTable table = new ExportableTable();
					
					TableOptions options = new TableOptionsBuilder()
						.setDatasets(getDatasets())
						.addStatistic(stat)
						.setSegPosition(seg.getPosition())
						.setTarget(table)
						.setRenderer(TableOptions.ALL_EXCEPT_FIRST_COLUMN, new WilcoxonTableCellRenderer())
						.build();

					
//					TableModel model = getTable(options);
					
//					ExportableTable table = new ExportableTable(model);
//					setRenderer(table, new WilcoxonTableCellRenderer());
					addWilconxonTable(tablePanel, table, stat.toString() + " - " + segName);
					scrollPane.setColumnHeaderView(table.getTableHeader());
					setTable(options);
				}

			}
			tablePanel.revalidate();

		} else {
			JPanel labelPanel = new JPanel();
			// Separate so we can use a flow layout for the label
			labelPanel.add(new JLabel(Labels.INCONSISTENT_SEGMENT_NUMBER, JLabel.CENTER));
			tablePanel.add(labelPanel);
		} 
		
		
		
		scrollPane.setViewportView(tablePanel);;
		tablePanel.repaint();
		
	}

	@Override
	protected void updateNull() {
		tablePanel = createTablePanel();
		scrollPane.setColumnHeaderView(null);
		JPanel labelPanel = new JPanel();
		// Separate so we can use a flow layout for the label
		labelPanel.add(new JLabel(Labels.NO_DATA_LOADED, JLabel.CENTER));
		tablePanel.add(labelPanel);
		scrollPane.setViewportView(tablePanel);;
		tablePanel.repaint();
		
	}
	
	protected TableModel createPanelTableType(DefaultTableOptions options) throws Exception{
		return new AnalysisDatasetTableCreator(options).createWilcoxonStatisticTable();
	}
			
}
