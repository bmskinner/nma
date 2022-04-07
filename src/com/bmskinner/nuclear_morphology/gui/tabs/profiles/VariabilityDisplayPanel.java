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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.text.ParseException;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileMarkersOptionsPanel;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileTypeOptionsPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.SignificanceTest;
import com.bmskinner.nuclear_morphology.visualisation.charts.AbstractChartFactory;
import com.bmskinner.nuclear_morphology.visualisation.charts.ProfileChartFactory;
import com.bmskinner.nuclear_morphology.visualisation.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptions;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptionsBuilder;

/**
 * Display variability information about profiles
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class VariabilityDisplayPanel extends DetailPanel {
	
	private static final Logger LOGGER = Logger.getLogger(VariabilityDisplayPanel.class.getName());

    private static final String PANEL_TITLE_LBL = "Variability";
    private JPanel                 buttonPanel = new JPanel(new FlowLayout());
    protected ExportableChartPanel chartPanel;
    private JSpinner               pvalueSpinner;

    private ProfileTypeOptionsPanel profileCollectionTypeSettingsPanel = new ProfileTypeOptionsPanel();

    private ProfileMarkersOptionsPanel profileMarkersOptionsPanel = new ProfileMarkersOptionsPanel();

    public VariabilityDisplayPanel(@NonNull InputSupplier context) {
        super(context);
        this.setLayout(new BorderLayout());

        ChartOptions options = new ChartOptionsBuilder().setProfileType(ProfileType.ANGLE).build();

        JFreeChart chart = new ProfileChartFactory(options).createVariabilityChart();

        chartPanel = new ExportableChartPanel(chart);
        chartPanel.getChartRenderingInfo().setEntityCollection(null);
        this.add(chartPanel, BorderLayout.CENTER);

        pvalueSpinner = new JSpinner(
                new SpinnerNumberModel(SignificanceTest.FIVE_PERCENT_SIGNIFICANCE_LEVEL, 0d, 1d, 0.001d));
        pvalueSpinner.setEnabled(false);
        pvalueSpinner.addChangeListener(e->{
             try {
            	 pvalueSpinner.commitEdit();
            	 update(getDatasets());
             } catch (ParseException e1) {
                 LOGGER.fine("Error setting p-value spinner: "+e1.getMessage());
             }
        });
        JComponent field = ((JSpinner.DefaultEditor) pvalueSpinner.getEditor());
        Dimension prefSize = field.getPreferredSize();
        prefSize = new Dimension(50, prefSize.height);
        field.setPreferredSize(prefSize);

        // add extra fields to the header panel
        buttonPanel.add(new JLabel("Dip test p-value:"));
        buttonPanel.add(pvalueSpinner);

        profileCollectionTypeSettingsPanel.addActionListener(e-> update(getDatasets()));
        profileCollectionTypeSettingsPanel.setEnabled(false);
        buttonPanel.add(profileCollectionTypeSettingsPanel);

        buttonPanel.revalidate();

        this.add(buttonPanel, BorderLayout.NORTH);
    }

    
    @Override
    public String getPanelTitle(){
        return PANEL_TITLE_LBL;
    }
    
    @Override
	public void setEnabled(boolean b) {
        // borderTagOptionsPanel.setEnabled(b);
        profileCollectionTypeSettingsPanel.setEnabled(b);
        profileMarkersOptionsPanel.setEnabled(b);
        pvalueSpinner.setEnabled(b);
    }

    /**
     * Update the profile panel with data from the given options
     */
    private void updateProfiles(ChartOptions options) {
        try {
            setChart(options);
        } catch (Exception e) {
           LOGGER.log(Loggable.STACK, "Error in plotting variability chart", e);
        }
    }

    @Override
    protected void updateSingle() {

        this.setEnabled(true);
        boolean showMarkers = profileMarkersOptionsPanel.showMarkers();
        ProfileType type = profileCollectionTypeSettingsPanel.getSelected();

        ChartOptions options = new ChartOptionsBuilder().setDatasets(getDatasets()).setNormalised(true)
                .setAlignment(ProfileAlignment.LEFT).setTag(Landmark.REFERENCE_POINT).setShowMarkers(showMarkers)
                .setModalityPosition((Double) pvalueSpinner.getValue())
                .setSwatch(GlobalOptions.getInstance().getSwatch()).setProfileType(type).setTarget(chartPanel).build();

        updateProfiles(options);

    }

    @Override
    protected void updateMultiple() {
        updateSingle();
        // Don't allow marker selection for multiple datasets
        profileMarkersOptionsPanel.setEnabled(false);
        pvalueSpinner.setEnabled(false);
    }

    @Override
    protected void updateNull() {
        updateSingle();
        this.setEnabled(false);
    }

    @Override
    public void setChartsAndTablesLoading() {
        super.setChartsAndTablesLoading();
        chartPanel.setChart(AbstractChartFactory.createLoadingChart());
    }

    @Override
    protected JFreeChart createPanelChartType(ChartOptions options) {
        return new ProfileChartFactory(options).createVariabilityChart();
    }

}
