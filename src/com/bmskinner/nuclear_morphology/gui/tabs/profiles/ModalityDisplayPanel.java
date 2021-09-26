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
package com.bmskinner.nuclear_morphology.gui.tabs.profiles;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.charts.AbstractChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.MorphologyChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.profiles.Tag;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileTypeOptionsPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;

/**
 * Display modality information about profiles
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class ModalityDisplayPanel extends DetailPanel implements ActionListener, ListSelectionListener {

    private static final String PANEL_TITLE_LBL = "Modality";
    
    private JPanel               mainPanel = new JPanel(new BorderLayout());
    private JList<Double>        pointList;
    private ExportableChartPanel angleDistributionPanel;
    private ExportableChartPanel pValueChartPanel; // p-value across the profile

    private ProfileTypeOptionsPanel profileCollectionTypeSettingsPanel = new ProfileTypeOptionsPanel();

    public ModalityDisplayPanel(@NonNull InputSupplier context) {
        super(context);
        createUI();

    }
    
    @Override
    public String getPanelTitle(){
        return PANEL_TITLE_LBL;
    }

    private void createUI() {
        this.setLayout(new BorderLayout());

        /*
         * Make the settings panel
         */
        JPanel headerPanel = createHeader();
        this.add(headerPanel, BorderLayout.NORTH);

        /*
         * Make the main chart panels
         */
        angleDistributionPanel = new ExportableChartPanel(MorphologyChartFactory.createEmptyChart());
        pValueChartPanel = new ExportableChartPanel(MorphologyChartFactory.createEmptyChart());

        angleDistributionPanel.getChartRenderingInfo().setEntityCollection(null);
        pValueChartPanel.getChartRenderingInfo().setEntityCollection(null);
        mainPanel.add(angleDistributionPanel, BorderLayout.WEST);
        mainPanel.add(pValueChartPanel, BorderLayout.CENTER);

        /*
         * Make the list of positions along the profile
         */
        JScrollPane listPanel = createListPanel();
        this.add(listPanel, BorderLayout.WEST);

        this.add(mainPanel, BorderLayout.CENTER);

    }

    private JPanel createHeader() {
        JPanel panel = new JPanel();
        profileCollectionTypeSettingsPanel.addActionListener(this);
        profileCollectionTypeSettingsPanel.setEnabled(false);
        panel.add(profileCollectionTypeSettingsPanel);
        return panel;
    }

    private JScrollPane createListPanel() {

        pointList = new JList<Double>();

        pointList.setModel(createEmptyListModel());
        JScrollPane listPanel = new JScrollPane(pointList);

        pointList.addListSelectionListener(this);
        pointList.setEnabled(false);

        pointList.setCellRenderer(new ModalityListCellRenderer());

        return listPanel;
    }

    private ListModel<Double> createEmptyListModel() {
        DefaultListModel<Double> model = new DefaultListModel<Double>();
        for (Double d = 0.0; d <= 100; d += 0.5) {
            model.addElement(d);
        }
        return model;
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        profileCollectionTypeSettingsPanel.setEnabled(b);
        pointList.setEnabled(b);
    }

    @Override
    protected void updateSingle() {
        this.setEnabled(true);

        pointList.setModel(createEmptyListModel());
        pointList.setCellRenderer(new ModalityListCellRenderer());
        pointList.setSelectedIndex(0);

        updateModalityProfileChart();
    }

    @Override
    protected void updateMultiple() {
        this.setEnabled(true);

        pointList.setModel(createEmptyListModel());
        pointList.setCellRenderer(new ModalityListCellRenderer());
        pointList.setSelectedIndex(0);

        updateModalityProfileChart();
    }

    @Override
    protected void updateNull() {
        this.setEnabled(false);

        updateModalityProfileChart();
        updatePositionChart(0);
    }

    @Override
    public void setChartsAndTablesLoading() {
        super.setChartsAndTablesLoading();
        angleDistributionPanel.setChart(AbstractChartFactory.createLoadingChart());
        pValueChartPanel.setChart(AbstractChartFactory.createLoadingChart());

    }

    @Override
    protected JFreeChart createPanelChartType(ChartOptions options) {
        if (options.isNormalised()) {
            return new MorphologyChartFactory(options).createModalityProfileChart();
        } else {
            return new MorphologyChartFactory(options).createModalityPositionChart();
        }
    }

    public void updateModalityProfileChart() {

        ProfileType type = profileCollectionTypeSettingsPanel.getSelected();

        
        ChartOptions options = new ChartOptionsBuilder()
        		.setDatasets(getDatasets())
        		.setNormalised(true) // setNormalised is used to indicate use of p-value chart here
                .setAlignment(ProfileAlignment.LEFT)
                .setTag(Tag.REFERENCE_POINT)
                .setShowMarkers(false)
                .setProfileType(type)
                .setSwatch(GlobalOptions.getInstance().getSwatch())
                .setTarget(pValueChartPanel)
                .build();

        setChart(options);
    }

    /**
     * Update the chart to display values at the given percentage along the
     * profile.
     * 
     * @param xvalue the percentage between zero and one hundred
     */
    public void updatePositionChart(double xvalue) {

        ProfileType type = profileCollectionTypeSettingsPanel.getSelected();

        double fraction = xvalue / 100d;

        ChartOptions options = new ChartOptionsBuilder()
        		.setDatasets(getDatasets())
        		.setNormalised(false) // setNormalised is used to indicate use of main chart here
                .setAlignment(ProfileAlignment.RIGHT)
                .setTag(Tag.REFERENCE_POINT)
                .setShowMarkers(false)
                .setProfileType(type)
                .setModalityPosition(fraction)
                .setSwatch(GlobalOptions.getInstance().getSwatch())
                .setTarget(angleDistributionPanel)
                .build();
        setChart(options);
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        updatePointSelection();
        updateModalityProfileChart();
    }

    private void updatePointSelection() {

        if (pointList.getSelectedValue() != null) {

            double xvalue = pointList.getSelectedValue().doubleValue();

            int lastRow = pointList.getModel().getSize() - 1;

            if (xvalue == 100 || pointList.getSelectedIndex() == lastRow)
                xvalue = 0; // wrap arrays

            updatePositionChart(xvalue);
        }
    }

    public void valueChanged(ListSelectionEvent e) {
        updatePointSelection();
    }

    /**
     * Render the posiitons along a profile with appropriate formatting
     * 
     * @author ben
     *
     */
    private class ModalityListCellRenderer extends DefaultListCellRenderer {

        DecimalFormat df = new DecimalFormat("#0.00");

        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {

            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            this.setText(df.format(value));
            return this;
        }
    }

}
