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


package com.bmskinner.nuclear_morphology.gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.charts.AbstractChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.ConsensusNucleusChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ConsensusNucleusChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.events.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;
import com.bmskinner.nuclear_morphology.io.Io;
import com.bmskinner.nuclear_morphology.io.SVGWriter;

@SuppressWarnings("serial")
public class ConsensusNucleusPanel extends DetailPanel implements ChangeListener {

    private ConsensusNucleusChartPanel consensusChartPanel;
    private JButton                    runRefoldingButton;

    private JPanel offsetsPanel; // store controls for rotating and translating

    // Debugging tools for the nucleus mesh - not visible in the final panel
    private JCheckBox showMeshBox;
    private JCheckBox showMeshEdgesBox;
    private JCheckBox showMeshFacesBox;
    private JCheckBox straightenMeshBox;
    private JSpinner  meshSizeSpinner;

    public ConsensusNucleusPanel(@NonNull InputSupplier context) {
        super(context);
        this.setLayout(new BorderLayout());
        JFreeChart consensusChart = ConsensusNucleusChartFactory.makeEmptyChart();
        consensusChartPanel = new ConsensusNucleusChartPanel(consensusChart);
        consensusChartPanel.addSignalChangeListener(this);

        runRefoldingButton = new JButton(Labels.Consensus.REFOLD_BTN_LBL);

        runRefoldingButton.addActionListener(e -> {
            this.getDatasetEventHandler().fireDatasetEvent(DatasetEvent.REFOLD_CONSENSUS, getDatasets());
            runRefoldingButton.setVisible(false);
        });

        runRefoldingButton.setVisible(false);

        consensusChartPanel.add(runRefoldingButton);

        add(consensusChartPanel, BorderLayout.CENTER);

        offsetsPanel = createOffsetsPanel();
        add(offsetsPanel, BorderLayout.EAST);
        offsetsPanel.setVisible(false);
    }
    
    @Override
    public String getPanelTitle(){
        return "Consensus panel";
    }

    @Override
    public void setChartsAndTablesLoading() {
        consensusChartPanel.setChart(AbstractChartFactory.createLoadingChart());
    }

    /**
     * Force the chart panel to restore bounds
     */
    public void restoreAutoBounds() {
        consensusChartPanel.restoreAutoBounds();
    }

    private JPanel createOffsetsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel rotatePanel = createRotationPanel();
        panel.add(rotatePanel, BorderLayout.NORTH);

        /*
         * Used for debugging only - do not include in releases
         */
        JPanel meshPanel = createMeshPanel();
//        panel.add(meshPanel, BorderLayout.CENTER);

        JPanel offsetPanel = createTranslatePanel();
        panel.add(offsetPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createMeshPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        showMeshBox = new JCheckBox("Show mesh");
        showMeshBox.setSelected(false);
        showMeshBox.addChangeListener(this);

        SpinnerNumberModel meshSizeModel = new SpinnerNumberModel(10, 2, 100, 1);
        meshSizeSpinner = new JSpinner(meshSizeModel);
        meshSizeSpinner.addChangeListener(this);
        meshSizeSpinner.setToolTipText("Mesh size");
        JSpinner.NumberEditor meshNumberEditor = new JSpinner.NumberEditor(meshSizeSpinner, "0");
        meshSizeSpinner.setEditor(meshNumberEditor);

        showMeshEdgesBox = new JCheckBox("Mesh edges");
        showMeshEdgesBox.setSelected(true);
        showMeshEdgesBox.setEnabled(false);
        showMeshEdgesBox.addChangeListener(this);

        showMeshFacesBox = new JCheckBox("Mesh faces");
        showMeshFacesBox.setSelected(false);
        showMeshFacesBox.setEnabled(false);
        showMeshFacesBox.addChangeListener(this);

        straightenMeshBox = new JCheckBox("Straighten mesh");
        straightenMeshBox.setSelected(false);
        straightenMeshBox.setEnabled(false);
        straightenMeshBox.addChangeListener(this);

        panel.add(showMeshBox);
        panel.add(meshSizeSpinner);
        panel.add(showMeshEdgesBox);
        panel.add(showMeshFacesBox);
        panel.add(straightenMeshBox);

        return panel;

    }

    private JPanel createTranslatePanel() {
        JPanel panel = new JPanel(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.CENTER;

        JButton moveUp = new JButton(Labels.Consensus.INCREASE_Y_LBL);
        moveUp.setToolTipText(Labels.Consensus.INCREASE_Y_TOOLTIP);

        moveUp.addActionListener(e -> {
            if (activeDataset().getCollection().hasConsensus()) {
                activeDataset().getCollection().getConsensus().offset(0, 1);
                refreshChartCache(getDatasets());
            }
        });
        panel.add(moveUp, constraints);

        JButton moveDown = new JButton(Labels.Consensus.DECREASE_Y_LBL);
        moveDown.setToolTipText(Labels.Consensus.DECREASE_Y_TOOLTIP);
        moveDown.addActionListener(e -> {
            if (activeDataset().getCollection().hasConsensus()) {
                activeDataset().getCollection().getConsensus().offset(0, -1);
                refreshChartCache(getDatasets());
            }
        });

        constraints.gridx = 1;
        constraints.gridy = 2;
        panel.add(moveDown, constraints);

        JButton moveLeft = new JButton(Labels.Consensus.DECREASE_X_LBL);
        moveLeft.setToolTipText(Labels.Consensus.DECREASE_X_TOOLTIP);
        moveLeft.addActionListener(e -> {
            if (activeDataset().getCollection().hasConsensus()) {
                activeDataset().getCollection().getConsensus().offset(-1, 0);
                refreshChartCache(getDatasets());
            }
        });

        constraints.gridx = 0;
        constraints.gridy = 1;
        panel.add(moveLeft, constraints);

        JButton moveRight = new JButton(Labels.Consensus.INCREASE_X_LBL);
        moveRight.setToolTipText(Labels.Consensus.INCREASE_X_TOOLTIP);
        moveRight.addActionListener(e -> {
            if (activeDataset().getCollection().hasConsensus()) {
                activeDataset().getCollection().getConsensus().offset(1, 0);
                refreshChartCache(getDatasets());
            }
        });

        constraints.gridx = 2;
        constraints.gridy = 1;
        panel.add(moveRight, constraints);

        JButton moveRst = new JButton(Labels.Consensus.RESET_LBL);
        moveRst.setToolTipText(Labels.Consensus.RESET_COM_TOOLTIP);
        moveRst.addActionListener(e -> {
            if (activeDataset().getCollection().hasConsensus()) {
                double x = 0;
                double y = 0;
                IPoint point = IPoint.makeNew(x, y);

                activeDataset().getCollection().getConsensus().moveCentreOfMass(point);
                refreshChartCache(getDatasets());
            }
        });

        constraints.gridx = 1;
        constraints.gridy = 1;
        panel.add(moveRst, constraints);

        return panel;
    }

    private JPanel createRotationPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.CENTER;

        JButton rotateFwd = new JButton(Labels.Consensus.DECREASE_ROTATION_LBL);
        rotateFwd.setToolTipText(Labels.Consensus.DECREASE_ROTATION_TOOLTIP);
        rotateFwd.addActionListener(e -> {
            if (activeDataset().getCollection().hasConsensus()) {
                activeDataset().getCollection().getConsensus().rotate(-89);
                refreshChartCache(getDatasets());

            }
        });

        panel.add(rotateFwd, constraints);

        JButton rotateBck = new JButton(Labels.Consensus.INCREASE_ROTATION_LBL);
        rotateBck.setToolTipText(Labels.Consensus.INCREASE_ROTATION_TOOLTIP);

        rotateBck.addActionListener(e -> {
            if (activeDataset().getCollection().hasConsensus()) {
                activeDataset().getCollection().getConsensus().rotate(-91);
                refreshChartCache(getDatasets());
            }
        });

        constraints.gridx = 2;
        constraints.gridy = 0;
        panel.add(rotateBck, constraints);

        JButton rotateRst = new JButton(Labels.Consensus.RESET_LBL);
        rotateRst.setToolTipText(Labels.Consensus.RESET_ROTATION_TOOLTIP);

        rotateRst.addActionListener(e -> {
            if (activeDataset().getCollection().hasConsensus()) {
                activeDataset().getCollection().getConsensus().alignVertically();
                refreshChartCache(getDatasets());
            }
        });

        constraints.gridx = 1;
        constraints.gridy = 0;
        panel.add(rotateRst, constraints);

        JButton refoldBtn = new JButton(Labels.Consensus.RE_REFOLD_LBL);
        refoldBtn.addActionListener(e -> {
            getDatasetEventHandler().fireDatasetEvent(DatasetEvent.REFOLD_CONSENSUS, activeDataset());
        });

        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        constraints.gridx = 0;
        constraints.gridy = 2;
        panel.add(refoldBtn, constraints);

        return panel;
    }

    @Override
    protected synchronized JFreeChart createPanelChartType(@NonNull ChartOptions options) {
        return new ConsensusNucleusChartFactory(options).makeConsensusChart();
    }

    @Override
    protected synchronized void updateSingle() {
        super.updateSingle();


        showMeshEdgesBox.setEnabled(showMeshBox.isSelected());
        showMeshFacesBox.setEnabled(showMeshBox.isSelected());
        straightenMeshBox.setEnabled(showMeshBox.isSelected());

        ChartOptions options = new ChartOptionsBuilder().setDatasets(getDatasets())
                .setScale(GlobalOptions.getInstance().getScale())
                .setSwatch(GlobalOptions.getInstance().getSwatch())
                .setShowMesh(showMeshBox.isSelected())
                .setShowMeshEdges(showMeshEdgesBox.isSelected())
                .setShowMeshFaces(showMeshFacesBox.isSelected())
                .setStraightenMesh(straightenMeshBox.isSelected())
                .setShowAnnotations(false)
                .setShowXAxis(false).setShowYAxis(false)
                .setTarget(consensusChartPanel)
                .build();

        setChart(options);
        
        if(activeDataset()==null) {
        	runRefoldingButton.setVisible(false);
        	offsetsPanel.setVisible(false);
        	return;
        }
        ICellCollection collection = activeDataset().getCollection();
        // hide the refold button when not needed
        runRefoldingButton.setVisible(!collection.hasConsensus());
        offsetsPanel.setVisible(collection.hasConsensus());

        consensusChartPanel.restoreAutoBounds();
    }

    @Override
    protected synchronized void updateMultiple() {

        ChartOptions options = new ChartOptionsBuilder().setDatasets(getDatasets())
                .setScale(GlobalOptions.getInstance().getScale()).setSwatch(GlobalOptions.getInstance().getSwatch())
                .setTarget(consensusChartPanel).build();

        setChart(options);

        // Only show the refold button if no selected datasets have a consensus
        runRefoldingButton.setVisible(getDatasets().stream().noneMatch(d->d.getCollection().hasConsensus()));
        offsetsPanel.setVisible(false);
    }

    @Override
    protected synchronized void updateNull() {

        consensusChartPanel.setChart(ConsensusNucleusChartFactory.makeEmptyChart());
        runRefoldingButton.setVisible(false);
        offsetsPanel.setVisible(false);
        consensusChartPanel.restoreAutoBounds();
    }

    private void rotateConsensusNucleus() {
        if (activeDataset()== null)
        	return;
        if (!activeDataset().getCollection().hasConsensus())
    		return;

        try {
        	double angle = getInputSupplier().requestDouble("Choose the amount to rotate", 0, -360, 360, 1.0);
        	activeDataset().getCollection().getConsensus().rotate(angle - 90);
        	update(activeDataset());
        	
		} catch (RequestCancelledException e) {
			return;
		}
    }

    private void resetConsensusNucleusRotation() {
    	if (activeDataset()==null) 
    		return;
    	if (!activeDataset().getCollection().hasConsensus())
    		return;
    	Nucleus nucleus = activeDataset().getCollection().getConsensus();
    	nucleus.alignVertically();
    	update(activeDataset());
    }

    private void offsetConsensusNucleus() {
    	if (activeDataset()==null) 
    		return;
    	if (!activeDataset().getCollection().hasConsensus())
    		return;


    	// get the x and y offset
    	SpinnerNumberModel xModel = new SpinnerNumberModel(0, -100, 100, 0.1);
    	SpinnerNumberModel yModel = new SpinnerNumberModel(0, -100, 100, 0.1);

    	JSpinner xSpinner = new JSpinner(xModel);
    	JSpinner ySpinner = new JSpinner(yModel);

    	JSpinner[] spinners = { xSpinner, ySpinner };

    	int option = JOptionPane.showOptionDialog(null, spinners, "Choose the amount to offset x and y",
    			JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
    	if (option == JOptionPane.CANCEL_OPTION) {
    		// user hit cancel
    	} else if (option == JOptionPane.OK_OPTION) {

    		// offset by 90 because reasons?
    		double x = (Double) xSpinner.getModel().getValue();
    		double y = (Double) ySpinner.getModel().getValue();

    		activeDataset().getCollection().getConsensus().offset(x, y);

    		this.update(activeDataset());
    	}
    }

    private void resetConsensusNucleusOffset() {
    	if (activeDataset()==null) 
    		return;
    	if (!activeDataset().getCollection().hasConsensus())
    		return;

    	double x = 0;
    	double y = 0;
    	IPoint point = IPoint.makeNew(x, y);
    	activeDataset().getCollection().getConsensus().moveCentreOfMass(point);
    	update(activeDataset());
    }

    private void exportConsensusNuclei() {
    	
        String defaultFileName = this.isMultipleDatasets() ? "Outlines.svg" : activeDataset().getName()+".svg";
        
        try {
        	File exportFile = getInputSupplier().requestFileSave(null, defaultFileName, "svg");
        	
        	if(!exportFile.getName().endsWith(Io.SVG_FILE_EXTENSION))
        		exportFile = new File(exportFile.getParentFile(), exportFile.getName()+Io.SVG_FILE_EXTENSION);

        	if(exportFile!=null){
        		SVGWriter wr = new SVGWriter(exportFile);
        		wr.exportConsensusOutlines(getDatasets()); 
        	}
		} catch (RequestCancelledException e) {
			return;
		}
    }

    @Override
    public void eventReceived(SignalChangeEvent event) {

        // pass on log messages back to the main window
        if (event.sourceName().equals(ConsensusNucleusChartPanel.SOURCE_COMPONENT)) {

            this.clearChartCache(getDatasets());

            if (event.type().startsWith("Log_")) {
                getSignalChangeEventHandler().fireSignalChangeEvent(event.type());
            }

            if (event.type().equals("RotateConsensus"))
                rotateConsensusNucleus();

            if (event.type().equals("RotateReset"))
                resetConsensusNucleusRotation();

            if (event.type().equals("OffsetAction"))
                offsetConsensusNucleus();

            if (event.type().equals("OffsetReset"))
                resetConsensusNucleusOffset();
            
            if (event.type().equals(ConsensusNucleusChartPanel.EXPORT_SVG_LBL)) {
                exportConsensusNuclei();
            }

            this.update(getDatasets());

        }

    }

    @Override
    public void stateChanged(ChangeEvent arg0) {

        this.update(getDatasets());
    }

}
