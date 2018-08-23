/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.gui.tabs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;
import org.jfree.data.Range;

import com.bmskinner.nuclear_morphology.analysis.nucleus.CellCollectionFilterer;
import com.bmskinner.nuclear_morphology.analysis.nucleus.Filterer;
import com.bmskinner.nuclear_morphology.analysis.nucleus.Filterer.CollectionFilteringException;
//import com.bmskinner.nuclear_morphology.analysis.nucleus.CollectionFilterer;
//import com.bmskinner.nuclear_morphology.analysis.nucleus.CollectionFilterer.CollectionFilteringException;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.charting.charts.AbstractChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.ScatterChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.datasets.AnalysisDatasetTableCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.tables.AbstractTableCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.tables.ScatterTableDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.VirtualCellCollection;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent.InterfaceMethod;

/**
 * An abstract class implementing the plottable statistic header on a detail
 * panel for drawing scatter charts
 * 
 * @author ben
 *
 */
@SuppressWarnings("serial")
public abstract class AbstractScatterChartPanel extends DetailPanel  {

    private static final String PANEL_TITLE_LBL = "Scatter";
    private static final String FILTER_BTN_LBL  = "Filter visible";
    private static final String FILTER_BTN_TOOLTIP = "Create a sub-population based on the visible values";
    
    private static final String X_AXIS_LBL = "X axis";
    private static final String Y_AXIS_LBL = "Y axis";
    
    private static final String SPEARMAN_LBL = "Spearman's rank correlation coefficients are shown in the table";
    
    
    protected ExportableChartPanel chartPanel;  // hold the charts
    protected JPanel               headerPanel; // hold buttons

    protected JButton gateButton;

    protected JComboBox<PlottableStatistic> statABox, statBBox;

    protected ExportableTable rhoTable;

    protected String component;

    public AbstractScatterChartPanel(@NonNull InputSupplier context, String component) {
        super(context);
        this.component = component;

        this.setLayout(new BorderLayout());

        headerPanel = createHeader();

        this.add(headerPanel, BorderLayout.NORTH);

        JPanel tablePanel = new JPanel(new BorderLayout());

        TableModel model = AnalysisDatasetTableCreator.createBlankTable();
        rhoTable = new ExportableTable(model);
        rhoTable.setEnabled(false);
        tablePanel.add(rhoTable, BorderLayout.CENTER);

        JFreeChart chart = ScatterChartFactory.makeEmptyChart();

        chartPanel = new ExportableChartPanel(chart);
        chartPanel.getChartRenderingInfo().setEntityCollection(null);
        this.add(chartPanel, BorderLayout.CENTER);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(tablePanel);
        scrollPane.setColumnHeaderView(rhoTable.getTableHeader());
        Dimension size = new Dimension(300, 200);
        scrollPane.setMinimumSize(size);
        scrollPane.setPreferredSize(size);

        this.add(scrollPane, BorderLayout.WEST);
    }

    @Override
    public String getPanelTitle(){
        return PANEL_TITLE_LBL;
    }
    
    private JPanel createHeader() {
        statABox = new JComboBox<>(PlottableStatistic.getStats(component));
        statBBox = new JComboBox<>(PlottableStatistic.getStats(component));
        
        statABox.setSelectedItem(PlottableStatistic.VARIABILITY); // default if present

        statABox.addActionListener(e->update(getDatasets()));
        statBBox.addActionListener(e->update(getDatasets()));

        gateButton = new JButton(FILTER_BTN_LBL);
        gateButton.setToolTipText(FILTER_BTN_TOOLTIP);
        gateButton.addActionListener(e-> gateOnVisible());
        gateButton.setEnabled(false);

        JPanel panel = new JPanel(new FlowLayout());

        panel.add(new JLabel(X_AXIS_LBL));
        panel.add(statABox);
        panel.add(new JLabel(Y_AXIS_LBL));
        panel.add(statBBox);

        panel.add(gateButton);
        panel.add(new JLabel(SPEARMAN_LBL));

        return panel;
    }

    @Override
    protected void updateSingle() {

        PlottableStatistic statA = (PlottableStatistic) statABox.getSelectedItem();
        PlottableStatistic statB = (PlottableStatistic) statBBox.getSelectedItem();

        ChartOptions options = new ChartOptionsBuilder().setDatasets(getDatasets())
        		.addStatistic(statA)
                .addStatistic(statB)
                .setScale(GlobalOptions.getInstance().getScale())
                .setSwatch(GlobalOptions.getInstance()
                .getSwatch())
                .setTarget(chartPanel).build();

        setChart(options);

        TableOptions tableOptions = new TableOptionsBuilder().setDatasets(getDatasets())
        		.addStatistic(statA)
                .addStatistic(statB)
                .setScale(GlobalOptions.getInstance().getScale())
                .setTarget(rhoTable).build();

        setTable(tableOptions);

        gateButton.setEnabled(true);
    }

    @Override
    protected void updateMultiple() {
        updateSingle();
    }

    @Override
    protected void updateNull() {

        chartPanel.setChart(AbstractChartFactory.createEmptyChart());
        rhoTable.setModel(AbstractTableCreator.createBlankTable());
        gateButton.setEnabled(false);
    }

    @Override
    public synchronized void setChartsAndTablesLoading() {
        chartPanel.setChart(AbstractChartFactory.createLoadingChart());
        rhoTable.setModel(AbstractTableCreator.createLoadingTable());
    }

    @Override
    protected TableModel createPanelTableType(TableOptions options) {
        return new ScatterTableDatasetCreator(options).createSpearmanCorrlationTable(component);
    }

    @Override
    protected JFreeChart createPanelChartType(ChartOptions options) {
        return new ScatterChartFactory(options).createScatterChart(component);
    }


    protected void gateOnVisible() {

     	int result;
		try {
			String[] options = { "Filter collection", "Do not filter", };
			result = this.getInputSupplier().requestOption(options, 1, "Filter selected datasets on visible values?");
		} catch (RequestCancelledException e2) {
			return;
		}

        if (result != 0) { // button at index 0 - continue
            return;
        }
        finer("Gating datasets on " + statABox.getSelectedItem().toString() + " and "
                + statBBox.getSelectedItem().toString());

        MeasurementScale scale = GlobalOptions.getInstance().getScale();
        
        Filterer<ICellCollection> f = new CellCollectionFilterer();

        for (IAnalysisDataset d : getDatasets()) {

            Range domain = getDomainBounds();
            Range range = getRangeBounds();

            PlottableStatistic statA = (PlottableStatistic) statABox.getSelectedItem();
            PlottableStatistic statB = (PlottableStatistic) statBBox.getSelectedItem();

            try {

                ICellCollection stat1 = f.filter(d.getCollection(), component, statA, domain.getLowerBound(),
                        domain.getUpperBound(), scale);

                ICellCollection stat2 = f.filter(stat1, component, statB, range.getLowerBound(), range.getUpperBound(), scale);

                ICellCollection virt = new VirtualCellCollection(d, stat2.getName());
                
                stat2.getCells().forEach(c->virt.addCell(c));

                virt.setName("Filtered_" + statA + "_" + statB);

                d.addChildCollection(virt);
                try {

                    d.getCollection().getProfileManager().copyCollectionOffsets(virt);
                } catch (ProfileException e) {
                    warn("Error copying collection offsets for " + d.getName());
                    stack("Error in offsetting", e);
                    continue;
                }

            } catch (CollectionFilteringException e1) {
                stack("Unable to filter collection for " + d.getName(), e1);
                continue;
            }

        }
        log("Filtered datasets");

        finer("Firing population update request");
//        getSignalChangeEventHandler().fireSignalChangeEvent(SignalChangeEvent.UPDATE_POPULATION_PANELS);
        getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.REFRESH_POPULATIONS);
    }

    protected Range getRangeBounds() {
        return chartPanel.getChart().getXYPlot().getRangeAxis().getRange();
    }

    protected Range getDomainBounds() {
        return chartPanel.getChart().getXYPlot().getDomainAxis().getRange();
    }
}
