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
package com.bmskinner.nuclear_morphology.gui.tabs.signals.warping;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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

import javax.swing.BorderFactory;
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
import com.bmskinner.nuclear_morphology.gui.tabs.signals.warping.SignalWarpingModel.ImageCache.WarpedImageKey;

import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 * Displays signals warped onto the consensus nucleus of a dataset
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class SignalWarpingDialog extends LoadingIconDialog implements PropertyChangeListener {

    private static final String EXPORT_IMAGE_LBL    = "Export image";
	private static final String SOURCE_DATASET_LBL  = "Source dataset";
    private static final String TARGET_DATASET_LBL  = "Target dataset";
    private static final String SIGNAL_GROUP_LBL    = "Signal group";
    private static final String INCLUDE_CELLS_LBL   = "Only include cells with signals";
    private static final String PSEUDOCOLOUR_LBL    = "Pseudocolour signals";
    private static final String ENHANCE_LBL         = "Enhance signals";
    private static final String STRAIGHTEN_MESH_LBL = "Straighten meshes";
    private static final String RUN_LBL             = "Run";
    private static final String DIALOG_TITLE        = "Signal warping";
    private static final String MIN_THRESHOLD_LBL   = "Min threshold";
    private static final String BINARISE_LBL        = "Binarise";

    private List<IAnalysisDataset> datasets;
    private ExportableChartPanel   chartPanel;
    private SignalWarper warper;

    private JProgressBar progressBar = new JProgressBar(0, 100);

    private JTable signalSelectionTable;

    private final SignalWarpingModel model;
    
    private SignalWarpingDialogController controller;

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

        WarpingSettingsPanel settingsPanel = new WarpingSettingsPanel();
        this.add(settingsPanel, BorderLayout.NORTH);

        ChartOptions options = new ChartOptionsBuilder().setDatasets(datasets.get(0)).build();

        JFreeChart chart = new ConsensusNucleusChartFactory(options).makeNucleusOutlineChart();
        chartPanel = new ExportableChartPanel(chart);
        chartPanel.setFixedAspectRatio(true);
        
        JMenuItem exportImageItem = new JMenuItem(EXPORT_IMAGE_LBL);
        exportImageItem.addActionListener(e->controller.exportImage());
        chartPanel.getPopupMenu().add(exportImageItem);
        this.add(chartPanel, BorderLayout.CENTER);

        JPanel westPanel = createWestPanel();        
        this.add(westPanel, BorderLayout.WEST);
        this.controller = new SignalWarpingDialogController(model, chartPanel, signalSelectionTable, settingsPanel);
    }
    
    /**
     * Settings header panel
     */
    public class WarpingSettingsPanel extends JPanel implements ActionListener{
    	

    	    private DatasetSelectionPanel datasetBoxOne;
    	    private DatasetSelectionPanel datasetBoxTwo;

    	    private SignalGroupSelectionPanel signalBox;

    	    private JButton   runButton;
    	    private JCheckBox cellsWithSignalsBox;
    	    private JSpinner minThresholdSpinner;
    	    private JSlider   thresholdSlider;
    	    private JCheckBox pseudocolourBox;
    	    private JCheckBox enhanceBox;
    	    private JCheckBox binariseBox;
    	    
    	    public WarpingSettingsPanel() {
    	    	
    	    	setLayout(new BorderLayout());
    	    	
    	    	JPanel descriptionPanel = new JPanel(new FlowLayout());
    	    	descriptionPanel.add(new JLabel("Choose a signal group to be warped, and the conensus nucleus shape to warp onto"));
    	    	
    	    	JPanel settingsPanel = new JPanel();
    	    	
    	    	settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.X_AXIS));

    	    	JPanel setupPanel = createAnalysisSettingsPanel();
    	    	setupPanel.setBorder(BorderFactory.createTitledBorder("Setup"));
    	    	
    	    	JPanel displayPanel = createDisplaySettingsPanel();
    	    	displayPanel.setBorder(BorderFactory.createTitledBorder("Display"));
    	    	
    	    	settingsPanel.add(setupPanel);
    	    	settingsPanel.add(displayPanel);
    	    	
    	    	add(descriptionPanel, BorderLayout.NORTH);
    	    	add(settingsPanel, BorderLayout.CENTER);
    	    }
    
    	    
    	    /**
    	     * Set the signal-specific controls - anything
    	     * after a signal group has been chosen
    	     * @param b
    	     */
    	    private void setSignalSettingsEnabled(boolean b) {
    	    	signalBox.setEnabled(b);
	            cellsWithSignalsBox.setEnabled(b);
	            runButton.setEnabled(b);
	            datasetBoxTwo.setEnabled(b);
	            binariseBox.setEnabled(b);
	            minThresholdSpinner.setEnabled(b);
    	    }
    	    
    	    private JPanel createAnalysisSettingsPanel() {
    	    	JPanel panel = new JPanel();
    	    	panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    	    	
    	    	JPanel upperPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    	    	JPanel midPanel   = new JPanel(new FlowLayout(FlowLayout.LEFT));
    	    	JPanel lowerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

    	    	datasetBoxOne = new DatasetSelectionPanel(datasets);
    	    	datasetBoxTwo = new DatasetSelectionPanel(datasets);

    	    	datasetBoxOne.setSelectedDataset(datasets.get(0));
    	    	datasetBoxTwo.setSelectedDataset(datasets.get(0));
    	    	datasetBoxOne.addActionListener(e -> {
    	    		if (datasetBoxOne.getSelectedDataset().getCollection().getSignalManager().hasSignals()) {
    	    			signalBox.setDataset(datasetBoxOne.getSelectedDataset());
    	    			
    	    			int threshold = datasetBoxOne.getSelectedDataset().getAnalysisOptions().get()
    	    					.getNuclearSignalOptions(signalBox.getSelectedID()).getThreshold();
    	    			minThresholdSpinner.setValue(threshold);
    	    		}
    	    		
    	    	});
    	    	datasetBoxTwo.addActionListener(e -> controller.updateBlankChart() );

    	    	signalBox = new SignalGroupSelectionPanel(datasetBoxOne.getSelectedDataset());
    	    	if (!signalBox.hasSelection())
    	    		signalBox.setEnabled(false);
//    	    	signalBox.addActionListener(this);
    	    	signalBox.addActionListener(e->{
    	    		IAnalysisDataset sourceDataset = datasetBoxOne.getSelectedDataset();

        	        SignalManager m = sourceDataset.getCollection().getSignalManager();
        	        if (!m.hasSignals()) {
        	        	setSignalSettingsEnabled(false);
        	        } else {
        	        	setSignalSettingsEnabled(true);
        	            int threshold = datasetBoxOne.getSelectedDataset().getAnalysisOptions().get()
    	    					.getNuclearSignalOptions(signalBox.getSelectedID()).getThreshold();
    	    			minThresholdSpinner.setValue(threshold);
        	        }
    	    	});

    	    	cellsWithSignalsBox = new JCheckBox(INCLUDE_CELLS_LBL, true);
    	    	cellsWithSignalsBox.addActionListener(this);
    	    	
    	    	// Set the initial value to the signal detection threshold of the initial selected signal group
    	    	int threshold = datasetBoxOne.getSelectedDataset().getAnalysisOptions().get()
    					.getNuclearSignalOptions(signalBox.getSelectedID()).getThreshold();
    	    	SpinnerModel minThresholdModel = new SpinnerNumberModel(threshold, 0, 255, 1);
    	    	minThresholdSpinner = new JSpinner(minThresholdModel);

    	    	binariseBox = new JCheckBox(BINARISE_LBL, true);
    	    	binariseBox.addActionListener(this);
    	    	
    	    	
    	    	runButton = new JButton(RUN_LBL);
    	    	runButton.addActionListener(e ->  ThreadManager.getInstance().submit( () -> runWarping() ));
    	    	if(!signalBox.hasSelection()) 
    	    		runButton.setEnabled(false);
    	    	
    	    	progressBar.setVisible(false);
    	    	
    	    	upperPanel.add(new JLabel(SOURCE_DATASET_LBL));
    	    	upperPanel.add(datasetBoxOne);
    	    	upperPanel.add(new JLabel(SIGNAL_GROUP_LBL));
    	    	upperPanel.add(signalBox);

    	    	midPanel.add(new JLabel(MIN_THRESHOLD_LBL));
    	    	midPanel.add(minThresholdSpinner);
    	    	midPanel.add(binariseBox);
    	    	midPanel.add(cellsWithSignalsBox);

    	    	lowerPanel.add(new JLabel(TARGET_DATASET_LBL));
    	    	lowerPanel.add(datasetBoxTwo);
    	    	lowerPanel.add(runButton);
    	    	
    	    	lowerPanel.add(progressBar);

    	    	panel.add(upperPanel);
    	    	panel.add(midPanel);
    	    	panel.add(lowerPanel);
    	    	
    	    	return panel;
    	    }
    	    
    	    private JPanel createDisplaySettingsPanel() {
    	    	JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    	    	
    	    	thresholdSlider = new JSlider(0, SignalWarpingModel.THRESHOLD_ALL_VISIBLE);
    	    	thresholdSlider.setVisible(true);
    	    	thresholdSlider.addChangeListener( makeThresholdChangeListener());

    	    	pseudocolourBox = new JCheckBox(PSEUDOCOLOUR_LBL, true);
    	    	pseudocolourBox.addActionListener(e->controller.updateChart());

    	    	enhanceBox = new JCheckBox(ENHANCE_LBL, true);
    	    	enhanceBox.addActionListener(e->controller.updateChart());

    	    	panel.add(pseudocolourBox);
    	    	panel.add(enhanceBox);
    	    	panel.add(new JLabel("Threshold"));
    	    	panel.add(thresholdSlider);

    	    	return panel;
    	    }
    	    
    	    
    	    public boolean isPseudocolour() {
    	    	return pseudocolourBox.isSelected();
    	    }
    	    
    	    public boolean isEnhance() {
    	    	return enhanceBox.isSelected();
    	    }
    	    
    	    public boolean isCellsWithSignals() {
    	    	return cellsWithSignalsBox.isSelected();
    	    }
    	    
    	    public boolean isBinarise() {
    	    	return binariseBox.isSelected();
    	    }
    	    
    	    public IAnalysisDataset getSource() {
    	    	return datasetBoxOne.getSelectedDataset();
    	    }

    	    public IAnalysisDataset getTarget() {
    	    	return datasetBoxTwo.getSelectedDataset();
    	    }
    	    
    	    public int getMinThreshold() {
    	    	return (int) minThresholdSpinner.getValue();
    	    }
    	    
    	    public int getDisplayThreshold() {
    	    	return thresholdSlider.getValue();
    	    }
    	    
    	    public UUID getSignalId() {
    	    	return signalBox.getSelectedID();
    	    }
    	    
    	    public void setDisplayThreshold(int value) {
    	    	for(ChangeListener l : thresholdSlider.getChangeListeners())
            		thresholdSlider.removeChangeListener(l);
    	    	
            	thresholdSlider.setValue(value);
            	thresholdSlider.addChangeListener( makeThresholdChangeListener());
    	    }
    	    
    	    @Override
    	    public void setEnabled(boolean b) {
    	        signalBox.setEnabled(b);
    	        cellsWithSignalsBox.setEnabled(b);
    	        runButton.setEnabled(b);
    	        datasetBoxOne.setEnabled(b);
    	        datasetBoxTwo.setEnabled(b);
    	        minThresholdSpinner.setEnabled(b);
    	        thresholdSlider.setEnabled(b);
    	        pseudocolourBox.setEnabled(b);
    	        enhanceBox.setEnabled(b);
    	        binariseBox.setEnabled(b);
    	    }
    	    
    	    public void setSettingsEnabled(boolean b) {
    	        signalBox.setEnabled(b);
    	        cellsWithSignalsBox.setEnabled(b);
    	        runButton.setEnabled(b);
    	        datasetBoxOne.setEnabled(b);
    	        datasetBoxTwo.setEnabled(b);
    	        minThresholdSpinner.setEnabled(b);
    	        binariseBox.setEnabled(b);
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
    	            binariseBox.setEnabled(false);

    	        } else {
    	            signalBox.setEnabled(true);
    	            cellsWithSignalsBox.setEnabled(true);
    	            runButton.setEnabled(true);
    	            datasetBoxTwo.setEnabled(true);
    	            binariseBox.setEnabled(true);
    	        }

    	    }
    	    
    	    /**
    	     * Run the warper with the currently selected settings
    	     */
    	    private void runWarping() {

    	        try {
    	        	progressBar.setValue(0);

    	        	IAnalysisDataset sourceDataset = datasetBoxOne.getSelectedDataset();
    	        	IAnalysisDataset targetDataset = datasetBoxTwo.getSelectedDataset();

    	        	boolean cellsWithSignals = cellsWithSignalsBox.isSelected();
    	        	
    	        	boolean binarise = binariseBox.isSelected();

    	        	int minThreshold = (int) minThresholdSpinner.getValue();

    	        	Nucleus target = targetDataset.getCollection().getConsensus();
    	        	setSettingsEnabled(false);

    	            progressBar.setStringPainted(true);
    	            progressBar.setVisible(true);
    	            
    	            HashOptions ho = new DefaultOptions();
    	            ho.setBoolean(SignalWarper.IS_STRAIGHTEN_MESH_KEY, SignalWarper.REGULAR_MESH);
    	            ho.setBoolean(SignalWarper.JUST_CELLS_WITH_SIGNAL_KEY,cellsWithSignals);
    	            ho.setBoolean(SignalWarper.BINARISE_KEY, binarise);
    	            ho.setInt(SignalWarper.MIN_SIGNAL_THRESHOLD_KEY, minThreshold);

    	            warper = new SignalWarper(sourceDataset, target, getSignalId(), ho);
    	            warper.addPropertyChangeListener(SignalWarpingDialog.this);
    	            
    	            ThreadManager.getInstance().execute(warper);

    	        } catch (Exception e) {
    	        	warn("Error running warping");
    	            stack("Error running warping", e);
    	            JFreeChart chart = ConsensusNucleusChartFactory.createErrorChart();
    	            chartPanel.setChart(chart);
    	            setSettingsEnabled(true);
    	        }
    	    }
    	    
    	    private ChangeListener makeThresholdChangeListener() {
    	    	return (e)-> {
    	        	JSlider s = (JSlider) e.getSource();
    	    		int value = SignalWarpingModel.THRESHOLD_ALL_VISIBLE - s.getValue();
    	        	model.setThresholdOfSelected(value);
    	        	controller.updateChart();
    	        };
    	    }

    
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
        
        signalSelectionTable.addMouseListener(new MouseAdapter() { 	
        	private static final int DOUBLE_CLICK = 2;
        	@Override
        	public void mouseClicked(MouseEvent e) {
        		JTable table = (JTable) e.getSource();
        		int row = table.rowAtPoint(e.getPoint());
        		if (e.getClickCount() == DOUBLE_CLICK) {
        			controller.deleteWarpedSignal(row);
        			controller.updateBlankChart();
        		}
        	}
        });
        
        JScrollPane sp = new JScrollPane(signalSelectionTable);
        panel.add(sp, BorderLayout.CENTER);
        return panel;
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
            controller.warpingComplete(warper);
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
