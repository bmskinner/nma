/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nma.gui.tabs.segments;

import java.util.List;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.TableModel;

import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.gui.Labels;
import com.bmskinner.nma.gui.components.ExportableTable;
import com.bmskinner.nma.gui.components.renderers.WilcoxonTableCellRenderer;
import com.bmskinner.nma.gui.tabs.AbstractPairwiseDetailPanel;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.visualisation.options.TableOptions;
import com.bmskinner.nma.visualisation.options.TableOptionsBuilder;
import com.bmskinner.nma.visualisation.tables.AbstractTableCreator;
import com.bmskinner.nma.visualisation.tables.AnalysisDatasetTableCreator;

@SuppressWarnings("serial")
public class SegmentWilcoxonPanel extends AbstractPairwiseDetailPanel {

	private static final Logger LOGGER = Logger.getLogger(SegmentWilcoxonPanel.class.getName());

	private static final String PANEL_TITLE_LBL = "Wilcoxon";
	private static final String PANEL_DESC_LBL = "Pairwise detection of significant differences in measured values";

	public SegmentWilcoxonPanel() {
		super(PANEL_TITLE_LBL, PANEL_DESC_LBL);
	}

	@Override
	protected synchronized void updateSingle() {
		tablePanel = createTablePanel();
		scrollPane.setColumnHeaderView(null);

		JPanel labelPanel = new JPanel();
		labelPanel.add(new JLabel(Labels.SINGLE_DATASET, JLabel.CENTER));
		tablePanel.add(labelPanel);

		scrollPane.setViewportView(tablePanel);
		tablePanel.repaint();

	}

	@Override
	protected synchronized void updateMultiple() {
		tablePanel = createTablePanel();
		scrollPane.setColumnHeaderView(null);

		if (IProfileSegment.segmentCountsMatch(getDatasets())) {

			List<IProfileSegment> segments;
			try {
				segments = activeDataset().getCollection().getProfileCollection()
						.getSegments(OrientationMark.REFERENCE);
			} catch (MissingLandmarkException | SegmentUpdateException e) {
				LOGGER.warning("Cannot get segments");
				LOGGER.log(Loggable.STACK, "Cannot get segments", e);
				return;
			}

			for (Measurement stat : Measurement.getSegmentStats()) {

				// Get each segment as a boxplot
				for (IProfileSegment seg : segments) {

					String segName = seg.getName();

					ExportableTable table = new ExportableTable(
							AbstractTableCreator.createLoadingTable());

					TableOptions options = new TableOptionsBuilder().setDatasets(getDatasets())
							.addStatistic(stat)
							.setSegPosition(seg.getPosition()).setTarget(table)
							.setColumnRenderer(TableOptions.ALL_EXCEPT_FIRST_COLUMN,
									new WilcoxonTableCellRenderer())
							.build();

					addWilconxonTable(tablePanel, table, stat.toString() + " - " + segName);
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

		scrollPane.setViewportView(tablePanel);
		tablePanel.repaint();
	}

	@Override
	protected synchronized void updateNull() {
		tablePanel = createTablePanel();
		scrollPane.setColumnHeaderView(null);
		JPanel labelPanel = new JPanel();
		// Separate so we can use a flow layout for the label
		labelPanel.add(new JLabel(Labels.NO_DATA_LOADED, JLabel.CENTER));
		tablePanel.add(labelPanel);
		scrollPane.setViewportView(tablePanel);
		tablePanel.repaint();

	}

	@Override
	protected TableModel createPanelTableType(TableOptions options) {
		return new AnalysisDatasetTableCreator(options)
				.createWilcoxonStatisticTable(CellularComponent.NUCLEAR_BORDER_SEGMENT);
	}

}
