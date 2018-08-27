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


package com.bmskinner.nuclear_morphology.gui.tabs.cells_detail;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.charting.charts.MorphologyChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.ProfileChartFactory;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileTypeOptionsPanel;
import com.bmskinner.nuclear_morphology.gui.components.panels.SegmentationDualChartPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.CellResegmentationDialog;
import com.bmskinner.nuclear_morphology.gui.events.ChartSetEventListener;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.events.SegmentEvent;

@SuppressWarnings("serial")
public class CellSegmentsPanel extends AbstractCellDetailPanel implements ChartSetEventListener {

    private static final String PANEL_TITLE_LBL = "Segments";
    
//    private SegmentationDualChartPanel dualPanel;

    private ProfileTypeOptionsPanel profileOptions = new ProfileTypeOptionsPanel();

    private JPanel buttonsPanel;

    private JButton resegmentButton;
    
    private InteractiveSegmentCellPanel imagePanel;

    // A JDialog is a top level container, and these are not subject to GC on
    // disposal according to
    // https://stackoverflow.com/questions/15863178/memory-leaking-on-jdialog-closing
    // Hence, only keep one dialog, and prevent multiple copies spawning by
    // loading the active cell in when needed
//    private final CellResegmentationDialog resegDialog;

    public CellSegmentsPanel(@NonNull InputSupplier context, final CellViewModel model) {
        super(context, model, PANEL_TITLE_LBL);

//        resegDialog = new CellResegmentationDialog(model);

        this.setLayout(new BorderLayout());
        this.setBorder(null);
        
        imagePanel = new InteractiveSegmentCellPanel(this);
        imagePanel.addSegmentEventListener(this);
        
        add(imagePanel, BorderLayout.CENTER);

//        dualPanel = new SegmentationDualChartPanel();
//        dualPanel.addSegmentEventListener(this);
//        dualPanel.getMainPanel().addChartSetEventListener(this);

//        JPanel chartPanel = new JPanel();
//        chartPanel.setLayout(new GridBagLayout());
//
//        GridBagConstraints c = new GridBagConstraints();
//        c.anchor = GridBagConstraints.EAST;
//        c.gridx = 0;
//        c.gridy = 0;
//        c.gridwidth = 1;
//        c.gridheight = 1;
//        c.fill = GridBagConstraints.BOTH; // reset to default
//        c.weightx = 1.0;
//        c.weighty = 0.7;
//
//        chartPanel.add(dualPanel.getMainPanel(), c);
//        c.weighty = 0.3;
//        c.gridx = 0;
//        c.gridy = 1;
//        chartPanel.add(dualPanel.getRangePanel(), c);

//        this.add(chartPanel, BorderLayout.CENTER);

        buttonsPanel = makeButtonPanel();
        add(buttonsPanel, BorderLayout.NORTH);

//        resegDialog.addDatasetEventListener(this);

        setEnabled(false);
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

//        resegmentButton = new JButton("Resegment");
//        panel.add(resegmentButton);
//        resegmentButton.setEnabled(false);
//
//        resegmentButton.addActionListener(e -> {
//            CellResegmentationDialog d = new CellResegmentationDialog(getCellModel());
//            d.load(getCellModel().getCell(), activeDataset());
//        });

        return panel;

    }

    @Override
	public void setEnabled(boolean b) {
        super.setEnabled(b);
        profileOptions.setEnabled(b);
//        resegmentButton.setEnabled(b);
    }

    @Override
	public synchronized void update() {

        try {
        	
        	 final ICell cell = getCellModel().getCell();
             final CellularComponent component = getCellModel().getComponent();
        	
        	imagePanel.setCell(activeDataset(), cell, component, false, false);

//            ProfileType type = profileOptions.getSelected();
//
//            if (this.getCellModel().hasCell()) {
//
//                ISegmentedProfile profile = this.getCellModel().getCell().getNucleus().getProfile(type,
//                        Tag.REFERENCE_POINT);
//
//                ChartOptions options = new ChartOptionsBuilder().setDatasets(getDatasets())
//                        .setCell(this.getCellModel().getCell()).setNormalised(false).setAlignment(ProfileAlignment.LEFT)
//                        .setTag(Tag.REFERENCE_POINT).setShowMarkers(false).setProfileType(type)
//                        .setSwatch(GlobalOptions.getInstance().getSwatch()).setShowPoints(true).setShowXAxis(false)
//                        .setShowYAxis(false).setTarget(dualPanel.getMainPanel()).build();
//
//                setChart(options);
//
//                /*
//                 * Create the chart for the range panel
//                 */
//
//                ChartOptions rangeOptions = new ChartOptionsBuilder().setDatasets(getDatasets())
//                        .setCell(this.getCellModel().getCell()).setNormalised(false).setAlignment(ProfileAlignment.LEFT)
//                        .setTag(Tag.REFERENCE_POINT).setShowMarkers(false).setProfileType(type)
//                        .setSwatch(GlobalOptions.getInstance().getSwatch()).setShowPoints(false).setShowXAxis(false)
//                        .setShowYAxis(false).setTarget(dualPanel.getRangePanel()).build();
//
//                setChart(rangeOptions);
//
//                setEnabled(true);
//
//            } else {
//                JFreeChart chart1 = MorphologyChartFactory.createEmptyChart();
//                JFreeChart chart2 = MorphologyChartFactory.createEmptyChart();
//
//                dualPanel.setCharts(chart1, chart2);
//                setEnabled(false);
//
//            }

        } catch (Exception e) {
            error("Error updating cell panel", e);
            JFreeChart chart1 = MorphologyChartFactory.createEmptyChart();
            JFreeChart chart2 = MorphologyChartFactory.createEmptyChart();
//            dualPanel.setCharts(chart1, chart2);
            setEnabled(false);
        }

    }

    @Override
    public void setChartsAndTablesLoading() {
        super.setChartsAndTablesLoading();
        JFreeChart chart1 = MorphologyChartFactory.createLoadingChart();
        JFreeChart chart2 = MorphologyChartFactory.createLoadingChart();

//        dualPanel.setCharts(chart1, chart2);
    }

    @Override
    public void chartSetEventReceived(ChartSetEvent e) {
        ISegmentedProfile profile;
        try {
            profile = this.getCellModel().getCell().getNucleus().getProfile(profileOptions.getSelected(),
                    Tag.REFERENCE_POINT);
//            dualPanel.setProfile(profile, false);
        } catch (ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException e1) {
            fine("Error getting profile", e1);
        }

    }

    @Override
    protected JFreeChart createPanelChartType(ChartOptions options) {
    	return new ProfileChartFactory(options).createProfileChart();
    }

    @Override
    public void eventReceived(DatasetEvent event) {
        if (event.getSource() instanceof CellResegmentationDialog) { // Pass
                                                                     // upwards
            this.getDatasetEventHandler().fireDatasetEvent(event.method(), event.getDatasets());
        }
    }

    @Override
    public void refreshChartCache() {
        clearChartCache();
        finest("Updating chart after clear");
        this.update();
    }

    @Override
    public void segmentEventReceived(SegmentEvent event) {

        if (event.getType() == SegmentEvent.MOVE_START_INDEX) {
            try {

            	System.out.println("Updating segment start index");
                // This is a manual change, so disable any lock
                this.getCellModel().getCell().getNucleus().setLocked(false);

                // Carry out the update
                activeDataset().getCollection().getProfileManager()
                        .updateCellSegmentStartIndex(getCellModel().getCell(), event.getId(), event.getIndex());

                // even if no lock was previously set, there should be one now a
                // manual adjustment was made
                this.getCellModel().getCell().getNucleus().setLocked(true);

                // Recache necessary charts
                refreshChartCache();

                this.getDatasetEventHandler().fireDatasetEvent(DatasetEvent.REFRESH_CACHE, getDatasets());
            } catch (Exception e) {
                error("Error updating segment", e);
            }
        }
    }

}
