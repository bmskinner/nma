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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.analysis.image.AbstractImageFilterer;
import com.bmskinner.nuclear_morphology.analysis.image.ImageAnnotator;
import com.bmskinner.nuclear_morphology.analysis.image.ImageConverter;
import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.charting.charts.ConsensusNucleusChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.MorphologyChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.OutlineChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.Imageable;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.gui.ChartOptionsRenderedEvent;
import com.bmskinner.nuclear_morphology.gui.ChartSetEvent;
import com.bmskinner.nuclear_morphology.gui.ChartSetEventListener;
import com.bmskinner.nuclear_morphology.gui.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.RotationMode;
import com.bmskinner.nuclear_morphology.gui.components.panels.GenericCheckboxPanel;
import com.bmskinner.nuclear_morphology.gui.components.panels.RotationSelectionSettingsPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.collections.CellCollectionOverviewDialog;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;
import com.bmskinner.nuclear_morphology.main.ThreadManager;

import ij.process.ImageProcessor;

@SuppressWarnings("serial")
public class CellOutlinePanel extends AbstractCellDetailPanel implements ActionListener, ChartSetEventListener {

    private static final String PANEL_TITLE_LBL = "Outline";
            
    private RotationSelectionSettingsPanel rotationPanel;
    
    private JPanel imagePanel;
    private JLabel imageLabel;

    private ExportableChartPanel panel;

    private GenericCheckboxPanel makeMeshPanel = new GenericCheckboxPanel("Compare to consensus");
    private GenericCheckboxPanel warpMeshPanel = new GenericCheckboxPanel("Warp to consensus");

    private JButton redrawBorderBtn = new JButton("Redraw outline");

    private CellBorderAdjustmentDialog cellBorderAdjustmentDialog;

    public CellOutlinePanel(CellViewModel model) {
        super(model, PANEL_TITLE_LBL);
        // make the chart for each nucleus
        this.setLayout(new BorderLayout());
        JFreeChart chart = ConsensusNucleusChartFactory.makeEmptyChart();

        JPanel settingsPanel = new JPanel(new FlowLayout());

        rotationPanel = new RotationSelectionSettingsPanel();
        rotationPanel.setEnabled(false);
        rotationPanel.addActionListener(this);

        makeMeshPanel.addActionListener(this);
        makeMeshPanel.setEnabled(false);

        warpMeshPanel.addActionListener(this);
        warpMeshPanel.setEnabled(false);

        settingsPanel.add(rotationPanel);
        settingsPanel.add(makeMeshPanel);
        settingsPanel.add(warpMeshPanel);

        cellBorderAdjustmentDialog = new CellBorderAdjustmentDialog(model);

        redrawBorderBtn.addActionListener(e -> {
            cellBorderAdjustmentDialog.load(model.getCell(), activeDataset());
        });
        redrawBorderBtn.setEnabled(false);
        settingsPanel.add(redrawBorderBtn);

        this.add(settingsPanel, BorderLayout.NORTH);

        imagePanel = new JPanel(new BorderLayout());
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setVerticalAlignment(JLabel.CENTER);
        imageLabel.setHorizontalTextPosition(JLabel.CENTER);
        imageLabel.setVerticalTextPosition(JLabel.CENTER);
        imagePanel.add(imageLabel, BorderLayout.CENTER);
        add(imagePanel, BorderLayout.CENTER);
//        panel = new ExportableChartPanel(chart);
//        panel.setFixedAspectRatio(true);
//
//        panel.addChartSetEventListener(this);
//        this.add(panel, BorderLayout.CENTER);

    }
    
    private synchronized void updateSettingsPanels() {

        if (this.isMultipleDatasets() || !this.hasDatasets()) {
            rotationPanel.setEnabled(false);
            makeMeshPanel.setEnabled(false);
            warpMeshPanel.setEnabled(false);
            redrawBorderBtn.setEnabled(false);
            return;
        }

        if (!this.getCellModel().hasCell()) {
            rotationPanel.setEnabled(false);
            makeMeshPanel.setEnabled(false);
            warpMeshPanel.setEnabled(false);
            redrawBorderBtn.setEnabled(false);
        } else {

            redrawBorderBtn.setEnabled(true);

            // Only allow one mesh activity to be active
            rotationPanel.setEnabled(!warpMeshPanel.isSelected());
            makeMeshPanel.setEnabled(!warpMeshPanel.isSelected());
            warpMeshPanel.setEnabled(!makeMeshPanel.isSelected());

            if (!activeDataset().getCollection().hasConsensus()) {
                makeMeshPanel.setEnabled(false);
                warpMeshPanel.setEnabled(false);
            }
        }
    }

    public synchronized void update() {

        if (this.isMultipleDatasets() || !this.hasDatasets()) {
            imageLabel.setIcon(null);
            return;
        }
        
        
        if(!getCellModel().hasCell()){
            imageLabel.setIcon(null);
            return;
        }
        
        final ICell cell = getCellModel().getCell();

        CellularComponent component = getCellModel().getComponent();

        updateSettingsPanels();

        if (component==null) {
            imageLabel.setIcon(null);
            return;
        }

        Runnable r = () -> {
                
                ImageProcessor ip;
                try{
                    ip = component.getImage();
                } catch(UnloadableImageException e){
                    fine("Making blank image", e);
                    ip = AbstractImageFilterer.createBlankColorProcessor( 1500, 1500); //TODO make based on cell location
                }
                
                
                ImageAnnotator an = new ImageAnnotator(ip);
                
                if(cell.hasCytoplasm()){
                    an.crop(cell.getCytoplasm());
                } else{
                    an.crop(cell.getNuclei().get(0));
                }
                ImageAnnotator an2 = new ImageAnnotator(an.toProcessor(), imagePanel.getWidth(), imagePanel.getHeight());
                
                for(Nucleus n : cell.getNuclei()){
                    an2.annotateCroppedNucleus(n.duplicate());
                }

                imageLabel.setIcon(an2.toImageIcon());

        };

        ThreadManager.getInstance().submit(r);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        update();
    }

    @Override
    protected void updateSingle() {
        update();
    }

    @Override
    protected void updateMultiple() {
        updateNull();
    }

    @Override
    protected void updateNull() {
        imageLabel.setIcon(null);
        updateSettingsPanels();

    }

    @Override
    public void datasetEventReceived(DatasetEvent event) {
        super.datasetEventReceived(event);
        // Pass messages upwards
        if (event.getSource() instanceof CellCollectionOverviewDialog) {
            this.getDatasetEventHandler().fireDatasetEvent(new DatasetEvent(this, event));
        }
    }

    @Override
    public void chartOptionsRenderedEventReceived(ChartOptionsRenderedEvent e) {
        update();
    }

    @Override
    public void chartSetEventReceived(ChartSetEvent e) { }
}
