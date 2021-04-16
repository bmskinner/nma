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
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.charts.ConsensusNucleusChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.dialogs.LoadingIconDialog;
import com.bmskinner.nuclear_morphology.gui.tabs.CosmeticHandler;
import com.bmskinner.nuclear_morphology.gui.tabs.TabPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.signals.warping.SignalWarpingModel.ImageCache.WarpedImageKey;

/**
 * Displays signals warped onto the consensus nucleus of a dataset
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class SignalWarpingDialog 
	extends LoadingIconDialog {
	
	private static final Logger LOGGER = Logger.getLogger(SignalWarpingDialog.class.getName());

    private static final String EXPORT_IMAGE_LBL    = "Export image";
    private static final String DIALOG_TITLE        = "Signal warping";

    private List<IAnalysisDataset> datasets;
    private ExportableChartPanel   chartPanel;

    private JTable signalSelectionTable;

    private final SignalWarpingModel model;
    
    private final SignalWarpingDialogController controller;
    
//    private final JProgressBar progressBar = new JProgressBar(0,100);
    
    private JSplitPane centrePanel;

    private boolean ctrlPressed = false;    
    
    /** Allow access to cosmetic handlers for signal colour changes */
    private final TabPanel parent;

    /**
     * Construct with a list of datasets available to warp signals to and from
     * 
     * @param datasets
     */
    public SignalWarpingDialog(@NonNull final List<IAnalysisDataset> datasets,     
    	    TabPanel parent) {
        super();
        this.parent = parent;
        this.datasets = datasets;
        model = new SignalWarpingModel(datasets); // adds any saved warp images 
        
        createUIElements();

        controller = new SignalWarpingDialogController(model, 
        		chartPanel, 
        		signalSelectionTable, 
        		new SignalWarpingDisplaySettings());
        
        layoutUI();

        addCtrlPressListener();
        this.setModal(false);
        
        this.pack();
        centrePanel.setDividerLocation(0.65);
        chartPanel.restoreAutoBounds();
        this.setVisible(true);
    }
    

    private boolean isCtrlPressed() {
        synchronized (SignalWarpingDialog.class) {
            return ctrlPressed;
        }
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
    
    /**
     * Create the outline chart panel
     */
    private void createChartPanel() {
    	ChartOptions options = new ChartOptionsBuilder()
    			.setDatasets(datasets.get(0))
    			.build();
        JFreeChart chart = new ConsensusNucleusChartFactory(options)
        		.makeNucleusOutlineChart();
        
        chartPanel = new ExportableChartPanel(chart);
        chartPanel.setFixedAspectRatio(true);
    }
    
    /**
     * Create the table to show selected signals
     */
    private void createTablePanel() {
    	signalSelectionTable = new ExportableTable(model, false) {
    		@Override
    		public Class<?> getColumnClass(int column) {
    			// Render true/false column as checkbox
    			if(column==model.getColumnIndex(Labels.Signals.Warper.TABLE_HEADER_SIGNALS_ONLY))
    				return Boolean.class;
    			if(column==model.getColumnIndex(Labels.Signals.Warper.TABLE_HEADER_BINARISED))
    				return Boolean.class;
    			if(column==model.getColumnIndex(Labels.Signals.Warper.TABLE_HEADER_DELETE_COLUMN))
    				return JButton.class;
    			else
    				return super.getColumnClass(column);
    		}
    	};
    
    	signalSelectionTable.setCellSelectionEnabled(false);
        signalSelectionTable.setRowSelectionAllowed(true);
        signalSelectionTable.setColumnSelectionAllowed(false);
        signalSelectionTable.setAutoCreateRowSorter(true);
                
        // Set colour renderer
        TableColumn colourColumn = signalSelectionTable.getColumn(Labels.Signals.Warper.TABLE_HEADER_COLOUR_COLUMN);
        colourColumn.setCellRenderer(new SignalWarpingTableCellRenderer());
        
        // Set JButton renderer
        TableColumn buttonColumn = signalSelectionTable.getColumn(Labels.Signals.Warper.TABLE_HEADER_DELETE_COLUMN);
        buttonColumn.setCellRenderer(new SignalWarpingButtonRenderer());
        
        // Handle mouse events
        signalSelectionTable.addMouseListener(new MouseAdapter() { 	
        	private static final int DOUBLE_CLICK = 2;
        	@Override
        	public void mouseClicked(MouseEvent e) {
        		JTable table = (JTable) e.getSource();
        		int row = table.rowAtPoint(e.getPoint());
        		int col = table.columnAtPoint(e.getPoint());
        		
        		
        		// Change signal group colour
        		if(e.getClickCount()==DOUBLE_CLICK &&
        				col==model.getColumnIndex(Labels.Signals.Warper.TABLE_HEADER_COLOUR_COLUMN)) {

        			WarpedImageKey key = model.getKey(row);         			
    				new CosmeticHandler(parent).changeSignalColour(key.getTemplate(), 
    						key.getSignalGroupId());
    				model.recachePseudoColour(key);
    				controller.updateChart();
        		}
        		
        		// Click the delete button
        		if(col==model.getColumnIndex(Labels.Signals.Warper.TABLE_HEADER_DELETE_COLUMN)) {
        			controller.deleteWarpedSignal(row);
        			controller.displayBlankChart();
        		}
        	}
        });
        
        TableColumn keyColumn = signalSelectionTable.getColumn(Labels.Signals.Warper.TABLE_HEADER_KEY_COLUMN);
        signalSelectionTable.removeColumn(keyColumn);
        
        signalSelectionTable.setRowHeight(25); //TODO make dynamic?
        
        createTablePopupMenu();
    }
    
    /**
     * Create the right-click popup menu for the table
     */
    private void createTablePopupMenu() {
    	// Allow deletion of multiple selected rows via right-click menu
        JPopupMenu tableMenu = signalSelectionTable.getComponentPopupMenu();
        JMenuItem deleteItem = new JMenuItem(Labels.Signals.Warper.TABLE_HEADER_DELETE_COLUMN);
        deleteItem.addActionListener(e->{
        	List<WarpedImageKey> keys = new ArrayList<>();
        	for(int row : signalSelectionTable.getSelectedRows())
        		keys.add(model.getKey(row));
        	
        	for(WarpedImageKey k : keys)
        		controller.deleteWarpedSignal(k);
        	
        	controller.displayBlankChart();
        });
        tableMenu.add(deleteItem);
    }
    
    /**
     * Populate the final fields of the class
     */
    private void createUIElements() {
    	createChartPanel();
    	createTablePanel();
    }
    
    /**
     * Decorate and layout the UI elements
     */
    private void layoutUI() {
    	
    	
        this.setLayout(new BorderLayout());
        this.setTitle(DIALOG_TITLE);
        
        JPanel runPanel = createRunSettingsPanel();
        JPanel displayPanel = createDisplaySettingsPanel();
        JPanel tablePanel = layoutTablePanel();  
        SignalWarpingMSSSIMPanel msssimPanel = new SignalWarpingMSSSIMPanel(model);
        controller.addSignalWarpingMSSSIMUpdateListener(msssimPanel);
        
        JPanel eastPanel = new JPanel(new BorderLayout());
        eastPanel.add(displayPanel, BorderLayout.NORTH);
        eastPanel.add(chartPanel, BorderLayout.CENTER);
        
        JPanel westPanel = new JPanel(new BorderLayout());
        westPanel.add(runPanel, BorderLayout.NORTH);
        westPanel.add(tablePanel, BorderLayout.CENTER);
        westPanel.add(msssimPanel, BorderLayout.SOUTH);

        centrePanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
        		westPanel, 
        		eastPanel);
        add(centrePanel, BorderLayout.CENTER);
    }
    
    private JPanel createRunSettingsPanel() {
    	SignalWarpingRunSettingsPanel runPanel = new SignalWarpingRunSettingsPanel(controller, model);
    	runPanel.addSignalWarpingRunEventListener(controller);
    	runPanel.setBorder(BorderFactory.createTitledBorder("Run settings"));
    	controller.addSignalWarpingProgressEventListener(runPanel);
    	return runPanel;

    }
    
    private JPanel createDisplaySettingsPanel() {
    	SignalWarpingDisplaySettingPanel displayPanel = new SignalWarpingDisplaySettingPanel(controller);
    	displayPanel.setBorder(BorderFactory.createTitledBorder("Display settings"));
    	displayPanel.addSignalWarpingDisplayListener(controller);
    	
    	// Changes to model selection need to update the display
    	controller.addSignalWarpingDisplayListener(displayPanel);
    	return displayPanel;
    }   
    
    /**
     * Create the list of saved warp images
     * 
     * @return
     */
    private JPanel layoutTablePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        JScrollPane sp = new JScrollPane(signalSelectionTable);
        panel.add(sp, BorderLayout.CENTER);
        return panel;
    }

    
    /**
     * Colour the background of the pseudocolour column in the signal warping table
     * @author bms41
     * @since 1.15.0
     *
     */
    public class SignalWarpingTableCellRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
        	Component l = super.getTableCellRendererComponent(table, value, 
        			isSelected, hasFocus, row, column);
            Color colour = (Color)value;
            l.setBackground(colour);
            l.setForeground(colour);
            return l;
        }
    }

    public class SignalWarpingButtonRenderer extends DefaultTableCellRenderer {
    	   public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    	         return (Component)value;
    	       
    	   }
    }
    

}
