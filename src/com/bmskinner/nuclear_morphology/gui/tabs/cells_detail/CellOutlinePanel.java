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

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.components.panels.GenericCheckboxPanel;
import com.bmskinner.nuclear_morphology.gui.components.panels.RotationSelectionSettingsPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.collections.CellCollectionOverviewDialog;
import com.bmskinner.nuclear_morphology.gui.events.CellUpdatedEventListener;
import com.bmskinner.nuclear_morphology.gui.events.ChartOptionsRenderedEvent;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;

@SuppressWarnings("serial")
public class CellOutlinePanel extends AbstractCellDetailPanel implements ActionListener, CellUpdatedEventListener {

    private static final String PANEL_TITLE_LBL = "Outline";
            
    private RotationSelectionSettingsPanel rotationPanel;
    
    private InteractiveAnnotatedCellPanel imagePanel;

    private GenericCheckboxPanel makeMeshPanel = new GenericCheckboxPanel("Compare to consensus");
    private GenericCheckboxPanel warpMeshPanel = new GenericCheckboxPanel("Warp to consensus");
    
    public CellOutlinePanel(@NonNull InputSupplier context, CellViewModel model) {
        super(context, model, PANEL_TITLE_LBL);
        // make the chart for each nucleus
        this.setLayout(new BorderLayout());

        JPanel settingsPanel = new JPanel(new FlowLayout());

        rotationPanel = new RotationSelectionSettingsPanel();
        rotationPanel.setEnabled(false);
        rotationPanel.addActionListener(this);

        makeMeshPanel.addActionListener(this);
        makeMeshPanel.setEnabled(false);

        warpMeshPanel.addActionListener(this);
        warpMeshPanel.setEnabled(false);

        settingsPanel.add(makeMeshPanel);
        settingsPanel.add(warpMeshPanel);
        settingsPanel.add(new JLabel("Click a border point to reassign tags"));
        
        add(settingsPanel, BorderLayout.NORTH);

        imagePanel = new InteractiveAnnotatedCellPanel(this);

        add(imagePanel, BorderLayout.CENTER);
    }
    
    private synchronized void updateSettingsPanels() {

        if (this.isMultipleDatasets() || !this.hasDatasets()) {
            rotationPanel.setEnabled(false);
            makeMeshPanel.setEnabled(false);
            warpMeshPanel.setEnabled(false);
            return;
        }

        if (!this.getCellModel().hasCell()) {
            rotationPanel.setEnabled(false);
            makeMeshPanel.setEnabled(false);
            warpMeshPanel.setEnabled(false);
        } else {
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

    @Override
	public synchronized void update() {

        if (this.isMultipleDatasets() || !this.hasDatasets()) {
            imagePanel.setNull();
            return;
        }
        
        final ICell cell = getCellModel().getCell();
        final CellularComponent component = getCellModel().getComponent();
        
        boolean isShowMesh  = makeMeshPanel.isSelected();
        boolean isWarpImage = warpMeshPanel.isSelected();        
        imagePanel.setCell(activeDataset(), cell, component, isShowMesh, isWarpImage);

        updateSettingsPanels();
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
    	imagePanel.setNull();
        updateSettingsPanels();

    }

    @Override
    public void eventReceived(DatasetEvent event) {
        super.eventReceived(event);
        // Pass messages upwards
        if (event.getSource() instanceof CellCollectionOverviewDialog)
            this.getDatasetEventHandler().fireDatasetEvent(new DatasetEvent(this, event));
        
        if(event.getSource()==imagePanel) {
        	 refreshChartCache();
        	 getDatasetEventHandler().fireDatasetEvent(new DatasetEvent(this, event));
        }
       
    }

    @Override
    public void eventReceived(ChartOptionsRenderedEvent e) {
        update();
    }

}
