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
package com.bmskinner.nuclear_morphology.gui.tabs.segments;

import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.charting.datasets.AnalysisDatasetTableCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.tables.AbstractTableCreator;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.components.renderers.PairwiseTableCellRenderer;
import com.bmskinner.nuclear_morphology.gui.tabs.AbstractPairwiseDetailPanel;

@SuppressWarnings("serial")
public class SegmentMagnitudePanel extends AbstractPairwiseDetailPanel {

    private static final String PANEL_TITLE_LBL = "Magnitude";
    
    public SegmentMagnitudePanel(@NonNull InputSupplier context) {
        super(context);
    }
    
    @Override
    public String getPanelTitle(){
        return PANEL_TITLE_LBL;
    }

    /**
     * Create the info panel
     * 
     * @return
     */
    @Override
    protected JPanel createInfoPanel() {
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

        JPanel labelPanel = new JPanel();
        labelPanel.add(new JLabel(Labels.SINGLE_DATASET, JLabel.CENTER));
        tablePanel.add(labelPanel);
        scrollPane.setViewportView(tablePanel);
        ;
        tablePanel.repaint();

    }

    @Override
    protected void updateMultiple() {
        tablePanel = createTablePanel();
        scrollPane.setColumnHeaderView(null);

        if (IBorderSegment.segmentCountsMatch(getDatasets())) {

            List<IBorderSegment> segments;
            try {
                segments = activeDataset().getCollection().getProfileCollection().getSegments(Tag.REFERENCE_POINT);
            } catch (UnavailableBorderTagException | ProfileException e) {
                warn("Cannot get segments");
                fine("Cannot get segments", e);
                return;
            }

            for (PlottableStatistic stat : PlottableStatistic.getSegmentStats()) {

                // Get each segment as a boxplot
                for (IBorderSegment seg : segments) {
                    String segName = seg.getName();

                    ExportableTable table = new ExportableTable(AbstractTableCreator.createLoadingTable());

                    TableOptions options = new TableOptionsBuilder().setDatasets(getDatasets()).addStatistic(stat)
                            .setSegPosition(seg.getPosition()).setTarget(table)
                            .setColumnRenderer(TableOptions.ALL_EXCEPT_FIRST_COLUMN, new PairwiseTableCellRenderer()).build();

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

        scrollPane.setViewportView(tablePanel);
        ;
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

        scrollPane.setViewportView(tablePanel);
        ;
        tablePanel.repaint();

    }

    @Override
    protected TableModel createPanelTableType(TableOptions options) {
        return new AnalysisDatasetTableCreator(options)
                .createMagnitudeStatisticTable(CellularComponent.NUCLEAR_BORDER_SEGMENT);
    }

}
