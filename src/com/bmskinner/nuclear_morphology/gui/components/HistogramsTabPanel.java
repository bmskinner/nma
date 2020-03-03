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
package com.bmskinner.nuclear_morphology.gui.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.charts.AbstractChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.HistogramChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.charts.panels.SelectableChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.gui.components.panels.GenericCheckboxPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * This class is extended for making a panel with multiple stats histograms
 * arranged vertically
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public abstract class HistogramsTabPanel extends DetailPanel implements ActionListener {
	
	private static final Logger LOGGER = Logger.getLogger(HistogramsTabPanel.class.getName());

    private static final String PANEL_TITLE_LBL = "Histograms";
    
    protected static final int HISTOGRAM_CHART_WIDTH  = 400;
    protected static final int HISTOGRAM_CHART_HEIGHT = 150;
    
    /** Map statistics to individual charts */
    protected Map<String, SelectableChartPanel> chartPanels = new HashMap<>();

    /** Hold the charts */
    protected JPanel mainPanel; 
    
    /** Holds the buttons */
    protected JPanel headerPanel;
    
    
    protected GenericCheckboxPanel useDensityPanel = new GenericCheckboxPanel("Probability density function");

    protected JScrollPane scrollPane;

    protected String component;

    public HistogramsTabPanel(@NonNull InputSupplier context, String component) {
        super(context);
        this.component = component;
        this.setLayout(new BorderLayout());

        try {
            mainPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

            headerPanel = new JPanel(new FlowLayout());

            headerPanel.add(useDensityPanel);

            useDensityPanel.addActionListener(this);

            this.add(headerPanel, BorderLayout.NORTH);

            // add the scroll pane to the tab
            scrollPane = new JScrollPane(mainPanel);
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Dimension preferredFloatingDimension = new Dimension( (int) (screenSize.getWidth()*0.25), (int) (screenSize.getHeight()*0.25) );
            scrollPane.setPreferredSize(preferredFloatingDimension);
            this.add(scrollPane, BorderLayout.CENTER);

            this.setEnabled(false);
        } catch (Exception e) {
            LOGGER.log(Loggable.STACK, "Error creating panel", e);
        }

    }
    
    @Override
    public String getPanelTitle(){
        return PANEL_TITLE_LBL;
    }

    @Override
    public void setChartsAndTablesLoading() {
        super.setChartsAndTablesLoading();
        for (ExportableChartPanel p : chartPanels.values()) {
            p.setChart(AbstractChartFactory.createLoadingChart());
        }

    }

    @Override
    protected JFreeChart createPanelChartType(ChartOptions options) {
        return new HistogramChartFactory(options).createStatisticHistogram(component);
    }

    @Override
    protected TableModel createPanelTableType(TableOptions options) {
        return null;
    }

    public void setEnabled(boolean b) {
        super.setEnabled(b);
        useDensityPanel.setEnabled(b);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        try {
            LOGGER.finest("Updating abstract histogram tab panel");
            this.update(getDatasets());
        } catch (Exception e1) {
        	LOGGER.log(Loggable.STACK, "Error updating histogram panel from action listener", e1);
        }

    }

    protected Optional<Integer> getFilterDialogResult(double lower, double upper) {
        DecimalFormat df = new DecimalFormat("#.##");
        String[] options = { "Filter collection", "Cancel", };
        
        String title = "Confirm filter";
        String message = String.format("Filter between %s-%s?", df.format(lower), df.format(upper));
        
        try {
        	int selected = getInputSupplier().requestOptionAllVisible(options, message, title);
        	return Optional.of(selected);
        } catch(RequestCancelledException e) {
        	return Optional.empty();
        }
    }

}
