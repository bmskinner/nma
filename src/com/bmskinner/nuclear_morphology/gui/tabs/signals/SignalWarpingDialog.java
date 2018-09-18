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


package com.bmskinner.nuclear_morphology.gui.tabs.signals;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalManager;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalWarper;
import com.bmskinner.nuclear_morphology.charting.charts.ConsensusNucleusChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.OutlineChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclear.DefaultWarpedSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.IWarpedSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.UnavailableSignalGroupException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.components.panels.DatasetSelectionPanel;
import com.bmskinner.nuclear_morphology.gui.components.panels.SignalGroupSelectionPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.LoadingIconDialog;
import com.bmskinner.nuclear_morphology.gui.tabs.signals.SignalWarpingModel.ImageCache.Key;

import ij.process.ImageProcessor;

/**
 * Displays signals warped onto the consensus nucleus of a dataset
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class SignalWarpingDialog extends LoadingIconDialog implements PropertyChangeListener, ActionListener {

    private static final String SOURCE_DATASET_LBL  = "Source dataset";
    private static final String TARGET_DATASET_LBL  = "Target dataset";
    private static final String SIGNAL_GROUP_LBL    = "Signal group";
    private static final String INCLUDE_CELLS_LBL   = "Only include cells with signals";
    private static final String STRAIGHTEN_MESH_LBL = "Straighten meshes";
    private static final String RUN_LBL             = "Run";
    private static final String DIALOG_TITLE        = "Signal warping";
    private static final String COW_LBL             = "Co-warpalise";

    private List<IAnalysisDataset> datasets;
    private ExportableChartPanel   chartPanel;

    private DatasetSelectionPanel datasetBoxOne;
    private DatasetSelectionPanel datasetBoxTwo;

    private SignalGroupSelectionPanel signalBox;

    private JButton   runButton;
    private JCheckBox cellsWithSignalsBox;
    private JSlider   thresholdSlider;

    private SignalWarper warper;

    private JProgressBar progressBar = new JProgressBar(0, 100);

    private JTree tree;

    private final SignalWarpingModel model;

    private boolean ctrlPressed = false;

    private boolean isCtrlPressed() {
        synchronized (SignalWarpingDialog.class) {
            return ctrlPressed;
        }
    }
    
    private TreeSelectionListener selectionListener;

    /**
     * Construct with a list of datasets available to warp signals to and from
     * 
     * @param datasets
     */
    public SignalWarpingDialog(final List<IAnalysisDataset> datasets) {
        super();
        this.datasets = datasets;
        model = new SignalWarpingModel(this);
        
        // Add saved warped images to the image cache 
        model.addSavedImages(datasets);
        
        createUI();
        addCtrlPressListener();
        this.setModal(false);
        this.pack();

        chartPanel.restoreAutoBounds();
        this.setVisible(true);
    }

    private void addCtrlPressListener() {
        // Track when the Ctrl key is down
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {

            @Override
            public boolean dispatchKeyEvent(KeyEvent ke) {
                synchronized (SignalWarpingDialog.class) {
                    switch (ke.getID()) {
                    case KeyEvent.KEY_PRESSED:
                        if (ke.getKeyCode() == KeyEvent.VK_CONTROL) {
                            ctrlPressed = true;
                        }
                        break;

                    case KeyEvent.KEY_RELEASED:
                        if (ke.getKeyCode() == KeyEvent.VK_CONTROL) {
                            ctrlPressed = false;
                        }
                        break;
                    }
                    return false;
                }
            }

        });
    }
    
    private void createUI() {
        this.setLayout(new BorderLayout());
        this.setTitle(DIALOG_TITLE);

        JPanel header = createHeader();
        this.add(header, BorderLayout.NORTH);

        ChartOptions options = new ChartOptionsBuilder().setDatasets(datasets.get(0)).build();

        JFreeChart chart = new ConsensusNucleusChartFactory(options).makeNucleusOutlineChart();
        chartPanel = new ExportableChartPanel(chart);
        chartPanel.setFixedAspectRatio(true);
        this.add(chartPanel, BorderLayout.CENTER);

        JPanel westPanel = createWestPanel();        
        this.add(westPanel, BorderLayout.WEST);

    }

    /**
     * Create the settings header panel
     * 
     * @return
     */
    private JPanel createHeader() {

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JPanel upperPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel lowerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        datasetBoxOne = new DatasetSelectionPanel(datasets);
        datasetBoxTwo = new DatasetSelectionPanel(datasets);

        datasetBoxOne.setSelectedDataset(datasets.get(0));
        datasetBoxTwo.setSelectedDataset(datasets.get(0));

        datasetBoxOne.addActionListener(e -> {
            if (datasetBoxOne.getSelectedDataset().getCollection().getSignalManager().hasSignals()) {

                signalBox.setDataset(datasetBoxOne.getSelectedDataset());
            }
        });
        datasetBoxTwo.addActionListener(e -> {
            updateBlankChart();
        });

        upperPanel.add(new JLabel(SOURCE_DATASET_LBL));
        upperPanel.add(datasetBoxOne);

        signalBox = new SignalGroupSelectionPanel(datasetBoxOne.getSelectedDataset());

        if (!signalBox.hasSelection()) {
            signalBox.setEnabled(false);
        }

        upperPanel.add(new JLabel(SIGNAL_GROUP_LBL));
        upperPanel.add(signalBox);

        signalBox.addActionListener(this);

        cellsWithSignalsBox = new JCheckBox(INCLUDE_CELLS_LBL, true);
        cellsWithSignalsBox.addActionListener(this);
        upperPanel.add(cellsWithSignalsBox);

        lowerPanel.add(new JLabel(TARGET_DATASET_LBL));
        lowerPanel.add(datasetBoxTwo);

        runButton = new JButton(RUN_LBL);

        runButton.addActionListener(e -> {

            Runnable task = () -> {
                runWarping();
            };

            ThreadManager.getInstance().submit(task);

        });

        lowerPanel.add(runButton);

        if (!signalBox.hasSelection()) 
            runButton.setEnabled(false);

        
        thresholdSlider = new JSlider(0, model.ALL_VISIBLE);
        lowerPanel.add(thresholdSlider);
        thresholdSlider.setVisible(false);
        thresholdSlider.addChangeListener( e-> {
        	JSlider s = (JSlider) e.getSource();
    		int value = 255 - s.getValue();
        	model.setThreshold(value);
        	chartPanel.setChart(model.getChart());
        });

        lowerPanel.add(progressBar);
        progressBar.setVisible(false);

        lowerPanel.add(this.getLoadingLabel());

        headerPanel.add(upperPanel);
        headerPanel.add(lowerPanel);

        return headerPanel;
    }

    /**
     * Create the list of saved warp images
     * 
     * @return
     */
    private JPanel createWestPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        tree = new JTree();
        selectionListener = createTreeListener();
        updateTree();
        JScrollPane sp = new JScrollPane(tree);
        panel.add(sp, BorderLayout.CENTER);
        return panel;
    }
    
    private TreeSelectionListener createTreeListener() {
    	return e-> {
    		if (!isCtrlPressed()) {
                model.clearSelection();
            }

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();

            Object obj = node.getUserObject();
            if (obj instanceof Key) {

                Key k = (Key) obj;
                
                if (isCtrlPressed()) {
                	model.toggleSelection(k);
                } else {
                	model.addSelection(k);
                }

                thresholdSlider.setValue(255-model.getThreshold());
                updateChart();
            }

            thresholdSlider.setVisible(model.selectedSize() == 1);
    	};
    }
    
    /**
     * Update the chart to display the given image over the nucleus outline for
     * dataset two
     * 
     * @param image
     */
    private void updateChart() {

        Runnable task = () -> {
            fine("Updating chart");
            chartPanel.setChart(model.getChart());
            chartPanel.restoreAutoBounds();
        };
        ThreadManager.getInstance().submit(task);

    }

    private void updateTree() {

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Targets");

        for (CellularComponent d : model.getTargets()) {

            DefaultMutableTreeNode m = new DefaultMutableTreeNode("idToCome");
            root.add(m);
            for (Key k : model.getKeys(d)) {
                m.add(new DefaultMutableTreeNode(k));
            }

        }

        tree.removeTreeSelectionListener(selectionListener);
        
        TreeModel model = new DefaultTreeModel(root);
        
        tree.setModel(model);

        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
        tree.addTreeSelectionListener(selectionListener);
    }

    /**
     * Run the warper with the currently selected settings
     */
    private void runWarping() {

        fine("Running warping");
        try {
        	thresholdSlider.setVisible(false);
        	progressBar.setValue(0);

        	IAnalysisDataset sourceDataset = datasetBoxOne.getSelectedDataset();
        	IAnalysisDataset targetDataset = datasetBoxTwo.getSelectedDataset();

        	boolean cellsWithSignals = cellsWithSignalsBox.isSelected();

        	Nucleus target = targetDataset.getCollection().getConsensus();

        	finest("Signal group: " + signalBox.getSelectedGroup());

            setStatusLoading();
            setEnabled(false);

            progressBar.setStringPainted(true);

            progressBar.setVisible(true);

            warper = new SignalWarper(sourceDataset, target, signalBox.getSelectedID(), cellsWithSignals, SignalWarper.REGULAR_MESH);
            warper.addPropertyChangeListener(this);
            
            fine("Executing warper");
            ThreadManager.getInstance().execute(warper);

        } catch (Exception e) {
        	warn("Error running warping");
            fine("Error running warping", e);
            JFreeChart chart = ConsensusNucleusChartFactory.makeErrorChart();
            chartPanel.setChart(chart);
            setEnabled(true);
        }
    }

    @Override
    public void setEnabled(boolean b) {
        signalBox.setEnabled(b);
        cellsWithSignalsBox.setEnabled(b);
        runButton.setEnabled(b);
        datasetBoxOne.setEnabled(b);
        datasetBoxTwo.setEnabled(b);
    }

    /**
     * Run when the warper is finished. Create the final image for display and
     * set the chart
     */
    public void finished() {
        try {

            ImageProcessor image = warper.get();
            
            CellularComponent consensusTemplate = datasetBoxTwo.getSelectedDataset().getCollection().getConsensus();
            IAnalysisDataset signalSource = datasetBoxOne.getSelectedDataset();
            UUID signalGroupId = signalBox.getSelectedID();
            
            ISignalGroup sg  = signalSource.getCollection().getSignalGroup(signalGroupId).get();
            IWarpedSignal ws = sg.getWarpedSignals().orElse(new DefaultWarpedSignal(signalGroupId));
            
            ws.addWarpedImage(consensusTemplate, image.convertToByteProcessor());
            sg.setWarpedSignals(ws);
            
            model.clearSelection();
            model.addImage(consensusTemplate, signalSource, signalGroupId, image);

            updateTree();
            updateChart();

            setEnabled(true);

            setStatusLoaded();

        } catch (Exception e) {
            error("Error getting warp results", e);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {

        if (evt.getNewValue() instanceof Integer) {
            int percent = (Integer) evt.getNewValue(); // should be percent
            if (percent >= 0 && percent <= 100) {
                if (progressBar.isIndeterminate()) {
                    progressBar.setIndeterminate(false);
                }
                progressBar.setValue(percent);
            }
        }

        if (evt.getPropertyName().equals("Finished")) {
            progressBar.setValue(0);
            progressBar.setVisible(false);
            finished();
        }

    }


    /**
     * Display the nucleus outline for dataset two
     * 
     */
    private void updateBlankChart() {

    	fine("Updating blank chart");
    	JFreeChart chart = null;


    	ChartOptions options = new ChartOptionsBuilder().setDatasets(datasetBoxTwo.getSelectedDataset()).build();

    	chart = new ConsensusNucleusChartFactory(options).makeNucleusOutlineChart();

    	chartPanel.setChart(chart);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        IAnalysisDataset sourceDataset = datasetBoxOne.getSelectedDataset();

        SignalManager m = sourceDataset.getCollection().getSignalManager();
        if (!m.hasSignals()) {
            signalBox.setEnabled(false);
            cellsWithSignalsBox.setEnabled(false);
            runButton.setEnabled(false);
            datasetBoxTwo.setEnabled(false);

        } else {
            signalBox.setEnabled(true);
            cellsWithSignalsBox.setEnabled(true);
            runButton.setEnabled(true);
            datasetBoxTwo.setEnabled(true);
        }

    }

}
