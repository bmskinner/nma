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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsBuilder;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.components.panels.GenericCheckboxPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.collections.ManualCurationDialog;
import com.bmskinner.nuclear_morphology.gui.events.CellUpdatedEventListener;
import com.bmskinner.nuclear_morphology.gui.events.ChartOptionsRenderedEvent;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.events.SegmentEvent;
import com.bmskinner.nuclear_morphology.gui.events.SegmentEvent.SegmentUpdateType;
import com.bmskinner.nuclear_morphology.gui.tabs.cells_detail.InteractiveCellPanel.CellDisplayOptions;
import com.bmskinner.nuclear_morphology.logging.Loggable;


/**
 * Display panel for cell outlines, including segments and landmarks
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class CellOutlinePanel extends AbstractCellDetailPanel implements ActionListener, CellUpdatedEventListener {
	
	private static final Logger LOGGER = Logger.getLogger(CellOutlinePanel.class.getName());

    private static final String PANEL_TITLE_LBL = "Outline";
  
    private InteractiveCellPanel imagePanel;

    private GenericCheckboxPanel rotatePanel   = new GenericCheckboxPanel("Rotate vertical");
    private GenericCheckboxPanel warpMeshPanel = new GenericCheckboxPanel("Warp image to consensus shape");
    
    // A JDialog is a top level container, and these are not subject to GC on
    // disposal according to
    // https://stackoverflow.com/questions/15863178/memory-leaking-on-jdialog-closing
    // Hence, only keep one dialog, and prevent multiple copies spawning by
    // loading the active cell in when needed
//    private final CellResegmentationDialog resegDialog; // not in use while debugging
    
    private final CellBorderAdjustmentDialog cellBorderAdjustmentDialog;
    
    public CellOutlinePanel(@NonNull InputSupplier context, CellViewModel model) {
        super(context, model, PANEL_TITLE_LBL);
        // make the chart for each nucleus
        this.setLayout(new BorderLayout());

        
        JPanel header = makeHeader();
        add(header, BorderLayout.NORTH);

        imagePanel = new InteractiveCellPanel(this);
        imagePanel.addSegmentEventListener(this);

        add(imagePanel, BorderLayout.CENTER);
        
        cellBorderAdjustmentDialog = new CellBorderAdjustmentDialog(model);
    }
    
    private JPanel makeHeader() {
    	JPanel panel = new JPanel(new FlowLayout());
    	
        JLabel headerLabel = new JLabel("<html><body style='width: 90%'>"
        		+ "Click a border point to update segments or landmarks.</html>");
        panel.add(headerLabel);

    	rotatePanel.setEnabled(false);
    	rotatePanel.addActionListener(this);

        warpMeshPanel.addActionListener(this);
        warpMeshPanel.setEnabled(false);
         
        JButton adjustBtn = new JButton("Adjust border");
        adjustBtn.addActionListener(e-> cellBorderAdjustmentDialog.load(getCellModel().getCell(), activeDataset()));

        panel.add(rotatePanel);
        panel.add(warpMeshPanel);
        
        return panel;
    }
    
    private synchronized void updateSettingsPanels() {

        if (this.isMultipleDatasets() || !this.hasDatasets()) {
        	rotatePanel.setEnabled(false);
            warpMeshPanel.setEnabled(false);
            return;
        }

        if (!this.getCellModel().hasCell()) {
        	rotatePanel.setEnabled(false);
            warpMeshPanel.setEnabled(false);
        } else {
            // Only allow one mesh activity to be active
        	rotatePanel.setEnabled(!warpMeshPanel.isSelected());
        	 warpMeshPanel.setEnabled(!rotatePanel.isSelected());

            if (!activeDataset().getCollection().hasConsensus()) {
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
        
        HashOptions displayOptions = new OptionsBuilder()
        		.withValue(CellDisplayOptions.WARP_IMAGE, warpMeshPanel.isSelected())
        		.withValue(CellDisplayOptions.ROTATE_VERTICAL, rotatePanel.isSelected())
        		.build();
        
        imagePanel.setCell(activeDataset(), cell, component, displayOptions);

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
    public void refreshChartCache() {
        clearChartCache();
        imagePanel.createImage();
        this.update();
    }

    @Override
    public void eventReceived(DatasetEvent event) {
        super.eventReceived(event);
        // Pass messages upwards
        if (event.getSource() instanceof ManualCurationDialog)
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

    @Override
    public void segmentEventReceived(SegmentEvent event) {

        if (event.type.equals(SegmentUpdateType.MOVE_START_INDEX)) {
        	
        	// Wrap in a runnable to avoid occasional hanging. Did it help?
        	Runnable r = () ->{
        		try {

        			LOGGER.fine("Updating segment start index to "+event.index);
        			// This is a manual change, so disable any lock
        			getCellModel().getCell().getPrimaryNucleus().setLocked(false);

        			// Carry out the update
        			activeDataset().getCollection().getProfileManager()
        			.updateCellSegmentStartIndex(getCellModel().getCell(), event.id, event.index);

        			// even if no lock was previously set, there should be one now a manual adjustment was made
        			getCellModel().getCell().getPrimaryNucleus().setLocked(true);

        			// Recache necessary charts within this panel at once
        			refreshChartCache();

        			// Request a refresh of other panels
        			getDatasetEventHandler().fireDatasetEvent(DatasetEvent.RECACHE_CHARTS, getDatasets());
        		} catch (Exception e) {
        			LOGGER.log(Loggable.STACK, "Error updating segment", e);
        		}
        	};
        	new Thread(r).start();
        }
    }
}
