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
package com.bmskinner.nuclear_morphology.gui.tabs.cells_detail;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.charting.charts.MorphologyChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.ProfileChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileTypeOptionsPanel;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Editing panel for the border tags of a single cell.
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class CellProfilesPanel extends AbstractCellDetailPanel {
	
	private static final Logger LOGGER = Logger.getLogger(CellProfilesPanel.class.getName());

    private ExportableChartPanel chartPanel;

    private ProfileTypeOptionsPanel profileOptions = new ProfileTypeOptionsPanel();

    private JPanel buttonsPanel;
    private JButton reverseProfileBtn = new JButton(Labels.Cells.REVERSE_PROFILE_BTN_LBL);

    public CellProfilesPanel(@NonNull InputSupplier context, CellViewModel model) {
        super(context, model, Labels.Cells.PROFILES_PANEL_TITLE_LBL);

        this.setLayout(new BorderLayout());

        buttonsPanel = makeButtonPanel();
        this.add(buttonsPanel, BorderLayout.NORTH);
        setButtonsEnabled(false);
        
        chartPanel = new ExportableChartPanel(ProfileChartFactory.createEmptyChart());
        this.add(chartPanel, BorderLayout.CENTER);

        this.setBorder(null);

        setButtonsEnabled(false);
    }
    
    private JPanel makeButtonPanel() {

        JPanel panel = new JPanel(new FlowLayout()) {
            @Override
            public void setEnabled(boolean b) {
                super.setEnabled(b);
                for (Component c : this.getComponents()) {
                    c.setEnabled(b);
                }
            }
        };

        panel.add(profileOptions);
        profileOptions.addActionListener(e -> update());
        
        reverseProfileBtn.addActionListener(e -> reverseProfileAction());
        panel.add(reverseProfileBtn);
        
        return panel;

    }
    
    /**
     * Reverse the profiles for the active cell. Tags and 
     * segments are also reversed.
     */
    private void reverseProfileAction() {
    	if(this.getCellModel().hasCell()) {
    		for(Nucleus n : this.getCellModel().getCell().getNuclei()) {
    			n.reverse();
    		}

    		this.getCellModel().updateViews();
    		try {
    			// Trigger refresh of dataset median profile and charts
				activeDataset().getCollection().getProfileManager().recalculateProfileAggregates();
				this.getDatasetEventHandler().fireDatasetEvent(DatasetEvent.RECACHE_CHARTS, activeDataset());
			} catch (ProfileException e) {
				LOGGER.log(Loggable.STACK, "Error recalculating profile aggregate", e);
			}
    	}
    	
    	
    }

    public void setButtonsEnabled(boolean b) {
    	buttonsPanel.setEnabled(b);
    }

    @Override
	public void update() {

        try {

            ProfileType type = profileOptions.getSelected();

            if (!this.getCellModel().hasCell()) {
            	chartPanel.setChart(MorphologyChartFactory.createEmptyChart());
            	buttonsPanel.setEnabled(false);

            } else {

                ChartOptions options = new ChartOptionsBuilder()
                		.setDatasets(getDatasets())
                        .setCell(this.getCellModel().getCell())
                        .setNormalised(false)
                        .setAlignment(ProfileAlignment.LEFT)
                        .setTag(Landmark.REFERENCE_POINT)
                        .setShowMarkers(true).setProfileType(type)
                        .setSwatch(GlobalOptions.getInstance().getSwatch())
                        .setShowAnnotations(false)
                        .setShowPoints(true)
                        .setShowXAxis(false)
                        .setShowYAxis(false)
                        .setTarget(chartPanel)
                        .build();

                setChart(options);
                buttonsPanel.setEnabled(true);

            }

        } catch (Exception e) {
            LOGGER.log(Loggable.STACK, "Error updating cell panel", e);
            chartPanel.setChart(MorphologyChartFactory.createErrorChart());
            buttonsPanel.setEnabled(false);
        }

    }

    @Override
    public void setChartsAndTablesLoading() {
        super.setChartsAndTablesLoading();
        chartPanel.setChart(MorphologyChartFactory.createLoadingChart());
    }

    @Override
    protected JFreeChart createPanelChartType(@NonNull ChartOptions options) {
        return new ProfileChartFactory(options).createProfileChart();
    }

    @Override
    public void refreshChartCache() {
        clearChartCache();
        this.update();
    }
}
