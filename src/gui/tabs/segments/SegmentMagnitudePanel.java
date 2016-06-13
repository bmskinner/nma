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
import gui.components.PairwiseTableCellRenderer;
import gui.tabs.AbstractPairwiseDetailPanel;

import java.util.List;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.TableModel;

import analysis.profiles.ProfileManager;
import stats.SegmentStatistic;
import charting.datasets.NucleusTableDatasetCreator;
import charting.options.TableOptions;
import charting.options.TableOptionsBuilder;
import components.generic.BorderTag;
import components.generic.ProfileType;
import components.nuclear.NucleusBorderSegment;

@SuppressWarnings("serial")
public class SegmentMagnitudePanel extends AbstractPairwiseDetailPanel  {
					
	public SegmentMagnitudePanel(){
		super();
	}
	
	/**
	 * Create the info panel
	 * @return
	 */
	@Override
	protected JPanel createInfoPanel(){
		JPanel infoPanel = new JPanel();
		infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
		infoPanel.add(new JLabel("Pairwise magnitude comparisons between populations"));
		infoPanel.add(new JLabel("Row median value as a proportion of column median value"));
		return infoPanel;
	}

	@Override
	protected void updateSingle() {
		tablePanel = createTablePanel();
		scrollPane.setColumnHeaderView(null);
		tablePanel.add(new JLabel("Single dataset selected", JLabel.CENTER));
		scrollPane.setViewportView(tablePanel);;
		tablePanel.repaint();
		
	}

	@Override
	protected void updateMultiple() {
		tablePanel = createTablePanel();
		scrollPane.setColumnHeaderView(null);
		
		if(ProfileManager.segmentCountsMatch(getDatasets())){

			List<NucleusBorderSegment> segments = activeDataset()
					.getCollection()
					.getProfileCollection(ProfileType.REGULAR)
					.getSegmentedProfile(BorderTag.REFERENCE_POINT)
					.getOrderedSegments();

			for(SegmentStatistic stat : SegmentStatistic.values()){

				// Get each segment as a boxplot
				for(NucleusBorderSegment seg : segments){
					String segName = seg.getName();

					
					
					TableOptions options = new TableOptionsBuilder()
					.setDatasets(getDatasets())
					.addStatistic(stat)
					.setSegPosition(seg.getPosition())
					.build();
					
					TableModel model = getTable(options);
					
					ExportableTable table = new ExportableTable(model);
					setRenderer(table, new PairwiseTableCellRenderer());
					addWilconxonTable(tablePanel, table, stat.toString() + " - " + segName);
					scrollPane.setColumnHeaderView(table.getTableHeader());
				}

			}
			tablePanel.revalidate();

		} else {
			tablePanel.add(new JLabel(Labels.INCONSISTENT_SEGMENT_NUMBER, JLabel.CENTER));
		} 
		
		scrollPane.setViewportView(tablePanel);;
		tablePanel.repaint();
		
	}

	@Override
	protected void updateNull() {
		tablePanel = createTablePanel();
		scrollPane.setColumnHeaderView(null);
		tablePanel.add(new JLabel("No datasets selected", JLabel.CENTER));
		scrollPane.setViewportView(tablePanel);;
		tablePanel.repaint();
		
	}
	
	protected TableModel createPanelTableType(TableOptions options) throws Exception{
		return NucleusTableDatasetCreator.getInstance().createMagnitudeStatisticTable(options);
	}
			
}	
