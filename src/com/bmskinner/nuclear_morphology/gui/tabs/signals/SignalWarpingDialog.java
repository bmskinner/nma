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
package com.bmskinner.nuclear_morphology.gui.tabs.signals;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalManager;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalWarper;
import com.bmskinner.nuclear_morphology.charting.charts.ConsensusNucleusChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.OutlineChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.datasets.SignalTableCell;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclear.DefaultWarpedSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.IWarpedSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.SignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.UnavailableSignalGroupException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.DefaultOptions;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions.IDetectionSubOptions;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.ConsistentRowTableCellRenderer;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.components.panels.DatasetSelectionPanel;
import com.bmskinner.nuclear_morphology.gui.components.panels.SignalGroupSelectionPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.LoadingIconDialog;
import com.bmskinner.nuclear_morphology.gui.tabs.signals.SignalWarpingModel.ImageCache.WarpedImageKey;

import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 * Displays signals warped onto the consensus nucleus of a dataset
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class SignalWarpingDialog extends LoadingIconDialog implements PropertyChangeListener, ActionListener {

    private static final String EXPORT_IMAGE_LBL    = "Export image";
	private static final String SOURCE_DATASET_LBL  = "Source dataset";
    private static final String TARGET_DATASET_LBL  = "Target dataset";
    private static final String SIGNAL_GROUP_LBL    = "Signal group";
    private static final String INCLUDE_CELLS_LBL   = "Only include cells with signals";
    private static final String PSEUDOCOLOUR_LBL    = "Pseudocolour signals";
    private static final String STRAIGHTEN_MESH_LBL = "Straighten meshes";
    private static final String RUN_LBL             = "Run";
    private static final String DIALOG_TITLE        = "Signal warping";
    private static final String MIN_THRESHOLD_LBL   = "Min threshold";

    
    private static final int KEY_COLUMN_INDEX = 4;

    private List<IAnalysisDataset> datasets;
    private ExportableChartPanel   chartPanel;

    private DatasetSelectionPanel datasetBoxOne;
    private DatasetSelectionPanel datasetBoxTwo;

    private SignalGroupSelectionPanel signalBox;

    private JButton   runButton;
    private JCheckBox cellsWithSignalsBox;
    private JSpinner minThresholdSpinner;
    private JSlider   thresholdSlider;
    private JCheckBox pseudocolourBox;

    private SignalWarper warper;

    private JProgressBar progressBar = new JProgressBar(0, 100);

    private JTable signalSelectionTable;

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
        model = new SignalWarpingModel(datasets); // adds any saved warp images       
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
        
        JMenuItem exportImageItem = new JMenuItem(EXPORT_IMAGE_LBL);
        exportImageItem.addActionListener(e->exportImage());
        chartPanel.getPopupMenu().add(exportImageItem);
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
        
        upperPanel.add(new JLabel(MIN_THRESHOLD_LBL));
        SpinnerModel minThresholdModel = new SpinnerNumberModel(SignalWarper.DEFAULT_MIN_SIGNAL_THRESHOLD, 0, 255, 1);
        minThresholdSpinner = new JSpinner(minThresholdModel);
        upperPanel.add(minThresholdSpinner);

        lowerPanel.add(new JLabel(TARGET_DATASET_LBL));
        lowerPanel.add(datasetBoxTwo);

        runButton = new JButton(RUN_LBL);

        runButton.addActionListener(e ->  ThreadManager.getInstance().submit( () -> runWarping() ));

        lowerPanel.add(runButton);

        if (!signalBox.hasSelection()) 
            runButton.setEnabled(false);
        

        
        thresholdSlider = new JSlider(0, SignalWarpingModel.THRESHOLD_ALL_VISIBLE);
        lowerPanel.add(thresholdSlider);
        thresholdSlider.setVisible(true);
        thresholdSlider.addChangeListener( makeThresholdChangeListener());
        
        pseudocolourBox = new JCheckBox(PSEUDOCOLOUR_LBL, true);
        pseudocolourBox.addActionListener(e->updateChart());
        lowerPanel.add(pseudocolourBox);

        lowerPanel.add(progressBar);
        progressBar.setVisible(false);

        headerPanel.add(upperPanel);
        headerPanel.add(lowerPanel);

        return headerPanel;
    }
    
    private ChangeListener makeThresholdChangeListener() {
    	return (e)-> {
        	JSlider s = (JSlider) e.getSource();
    		int value = SignalWarpingModel.THRESHOLD_ALL_VISIBLE - s.getValue();
        	model.setThresholdOfSelected(value);
        	chartPanel.setChart(model.getChart(pseudocolourBox.isSelected()));
        };
    }

    /**
     * Create the list of saved warp images
     * 
     * @return
     */
    private JPanel createWestPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        
        signalSelectionTable = new ExportableTable(model, false);
        signalSelectionTable.setCellSelectionEnabled(false);
        signalSelectionTable.setRowSelectionAllowed(true);
        signalSelectionTable.setColumnSelectionAllowed(false);
        TableColumn keyColumn = signalSelectionTable.getColumn(Labels.Signals.Warper.TABLE_HEADER_KEY_COLUMN);
        
        signalSelectionTable.removeColumn(keyColumn);
        
        TableColumn colourColumn = signalSelectionTable.getColumn(Labels.Signals.Warper.TABLE_HEADER_COLOUR_COLUMN);
        colourColumn.setCellRenderer(new SignalWarpingTableCellRenderer());
        ListSelectionModel cellSelectionModel = signalSelectionTable.getSelectionModel();
        cellSelectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        cellSelectionModel.addListSelectionListener(e->{
        	if(e.getValueIsAdjusting())
        		return;

        	model.clearSelection();
        	int[] selectedRow = signalSelectionTable.getSelectedRows();
        	thresholdSlider.setEnabled(selectedRow.length==1);
        	for(ChangeListener l : thresholdSlider.getChangeListeners())
        		thresholdSlider.removeChangeListener(l);
        	for (int i = 0; i < selectedRow.length; i++) {

        		WarpedImageKey selectedKey = (WarpedImageKey) signalSelectionTable.getModel().getValueAt(selectedRow[i], KEY_COLUMN_INDEX);
        		model.addSelection(selectedKey);
        		thresholdSlider.setValue(SignalWarpingModel.THRESHOLD_ALL_VISIBLE-model.getThreshold(selectedKey));
        	}
        	thresholdSlider.addChangeListener( makeThresholdChangeListener());
    		updateChart();
        });

        JScrollPane sp = new JScrollPane(signalSelectionTable);
        panel.add(sp, BorderLayout.CENTER);
        return panel;
    }
        
    /**
     * Update the chart to display the given image over the nucleus outline for
     * dataset two
     * 
     * @param image
     */
    private void updateChart() {

        Runnable task = () -> {
            chartPanel.setChart(model.getChart(pseudocolourBox.isSelected()));
            chartPanel.restoreAutoBounds();
        };
        ThreadManager.getInstance().submit(task);

    }

//    private void updateSignalSelectionTable() {
//    	
//    	DefaultTableModel tableModel = new DefaultTableModel();
//    	
//    	Vector<IAnalysisDataset> templateDatasets = new Vector<>();
//    	Vector<ISignalGroup>     templateSignals  = new Vector<>();
//    	Vector<Boolean>          isSignalsOnly    = new Vector<>();
//    	Vector<String > targetShape     = new Vector<>();
//    	Vector<WarpedImageKey> keys     = new Vector<>();
//    	
//        for (CellularComponent d : model.getTargets()) {
//            for (WarpedImageKey k : model.getKeys(d)) {
//            	
//            	templateDatasets.add(k.getTemplate());
//            	
//            	UUID signalGroupId = k.getSignalGroupId();
//            	ISignalGroup s = k.getTemplate().getCollection().getSignalGroup(signalGroupId).get();
//            	templateSignals.add(s);
//            	
//            	isSignalsOnly.add(k.isOnlyCellsWithSignals());
//            	targetShape.add(k.getTargetName());
//            	keys.add(k);
//            }
//        }
//        
//        tableModel.addColumn(Labels.Signals.Warper.TABLE_HEADER_SOURCE_DATASET, templateDatasets);
//        tableModel.addColumn(Labels.Signals.Warper.TABLE_HEADER_SOURCE_SIGNALS, templateSignals);
//        tableModel.addColumn(Labels.Signals.Warper.TABLE_HEADER_SIGNALS_ONLY, isSignalsOnly);
//        tableModel.addColumn(Labels.Signals.Warper.TABLE_HEADER_TARGET_SHAPE, targetShape);
//        tableModel.addColumn(Labels.Signals.Warper.TABLE_HEADER_KEY_COLUMN, keys);
//
//        signalSelectionTable.setModel(tableModel);
//        
//    }

    /**
     * Run the warper with the currently selected settings
     */
    private void runWarping() {

        try {
        	thresholdSlider.setVisible(false);
        	progressBar.setValue(0);

        	IAnalysisDataset sourceDataset = datasetBoxOne.getSelectedDataset();
        	IAnalysisDataset targetDataset = datasetBoxTwo.getSelectedDataset();

        	boolean cellsWithSignals = cellsWithSignalsBox.isSelected();

        	int minThreshold = (int) minThresholdSpinner.getValue();

        	Nucleus target = targetDataset.getCollection().getConsensus();
            setEnabled(false);

            progressBar.setStringPainted(true);

            progressBar.setVisible(true);
            
            HashOptions ho = new DefaultOptions();
            ho.setBoolean(SignalWarper.IS_STRAIGHTEN_MESH_KEY, SignalWarper.REGULAR_MESH);
            ho.setBoolean(SignalWarper.JUST_CELLS_WITH_SIGNAL_KEY,cellsWithSignals);
            ho.setInt(SignalWarper.MIN_SIGNAL_THRESHOLD_KEY, minThreshold);

            warper = new SignalWarper(sourceDataset, target, signalBox.getSelectedID(), ho);
            warper.addPropertyChangeListener(this);
            
            ThreadManager.getInstance().execute(warper);

        } catch (Exception e) {
        	warn("Error running warping");
            stack("Error running warping", e);
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
        minThresholdSpinner.setEnabled(b);
        pseudocolourBox.setEnabled(b);
    }

    /**
     * Run when the warper is finished. Create the final image for display and
     * set the chart
     */
    public void finished() {
        try {

            ImageProcessor image = warper.get();
            IAnalysisDataset targetDataset = datasetBoxTwo.getSelectedDataset();
            CellularComponent consensusTemplate = targetDataset.getCollection().getConsensus();
            IAnalysisDataset signalSource = datasetBoxOne.getSelectedDataset();
            UUID signalGroupId = signalBox.getSelectedID();
            
            boolean isCellsWithSignals = cellsWithSignalsBox.isSelected();
            
            ISignalGroup sg  = signalSource.getCollection().getSignalGroup(signalGroupId).get();
            IWarpedSignal ws = sg.getWarpedSignals().orElse(new DefaultWarpedSignal(signalGroupId));
            
            ws.addWarpedImage(consensusTemplate, targetDataset.getName(), isCellsWithSignals, image.convertToByteProcessor());
            sg.setWarpedSignals(ws);
            
            model.clearSelection();
            model.addImage(consensusTemplate, targetDataset.getName(), signalSource, signalGroupId, isCellsWithSignals, image);

//            updateSignalSelectionTable();
            updateChart();
            thresholdSlider.setVisible(true);
            setEnabled(true);

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

        if (IAnalysisWorker.FINISHED_MSG.equals(evt.getPropertyName())) {
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
    
    private void exportImage() {
    	ImageProcessor ip = model.getDisplayImage(pseudocolourBox.isSelected());
    	ip.flipVertical();
    	new ImagePlus("Image",ip).show();
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
    
    /**
     * Colour the background of the pseudocolour column in the signal warping table
     * @author bms41
     * @since 1.15.0
     *
     */
    @SuppressWarnings("serial")
    public class SignalWarpingTableCellRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
        	Component l = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            Color colour = (Color)value;
            l.setBackground(colour);
            l.setForeground(colour);
            return l;
        }
    }
}
