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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisWorker;
import com.bmskinner.nuclear_morphology.charting.charts.ConsensusNucleusChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.dialogs.LoadingIconDialog;
import com.bmskinner.nuclear_morphology.gui.tabs.signals.warping.SignalWarpingModelRevamp.ImageCache.WarpedImageKey;

/**
 * Displays signals warped onto the consensus nucleus of a dataset
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class SignalWarpingDialogRevamp extends LoadingIconDialog implements PropertyChangeListener {
	
	private static final Logger LOGGER = Logger.getLogger(SignalWarpingDialogRevamp.class.getName());

    private static final String EXPORT_IMAGE_LBL    = "Export image";
	private static final String SOURCE_DATASET_LBL  = "Source dataset";
    private static final String TARGET_DATASET_LBL  = "Target dataset";
    private static final String SIGNAL_GROUP_LBL    = "Signal group";
    private static final String INCLUDE_CELLS_LBL   = "Only include cells with signals";
    private static final String RUN_LBL             = "Run";
    private static final String DIALOG_TITLE        = "Signal warping";
    private static final String MIN_THRESHOLD_LBL   = "Min threshold";
    private static final String BINARISE_LBL        = "Binarise";

    private List<IAnalysisDataset> datasets;
    private ExportableChartPanel   chartPanel;

    private JTable signalSelectionTable;

    private final SignalWarpingModelRevamp model;
    
    private final SignalWarpingDialogControllerRevamp controller;
    
    private final JProgressBar progressBar = new JProgressBar(0,100);

    private boolean ctrlPressed = false;

    private boolean isCtrlPressed() {
        synchronized (SignalWarpingDialogRevamp.class) {
            return ctrlPressed;
        }
    }
    
    private TreeSelectionListener selectionListener;

    /**
     * Construct with a list of datasets available to warp signals to and from
     * 
     * @param datasets
     */
    public SignalWarpingDialogRevamp(@NonNull final List<IAnalysisDataset> datasets,
    		@NonNull final SignalWarpingDialogControllerRevamp controller) {
        super();
        this.datasets = datasets;
        this.controller = controller;
        model = new SignalWarpingModelRevamp(datasets); // adds any saved warp images       
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
                synchronized (SignalWarpingDialogRevamp.class) {
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
        
        JPanel northPanel = createNorthPanel();
        add(northPanel, BorderLayout.NORTH);

        ChartOptions options = new ChartOptionsBuilder().setDatasets(datasets.get(0)).build();

        JFreeChart chart = new ConsensusNucleusChartFactory(options).makeNucleusOutlineChart();
        chartPanel = new ExportableChartPanel(chart);
        chartPanel.setFixedAspectRatio(true);
        
        JMenuItem exportImageItem = new JMenuItem(EXPORT_IMAGE_LBL);
        exportImageItem.addActionListener(e->controller.exportImage());
        chartPanel.getPopupMenu().add(exportImageItem);

        JPanel westPanel = createWestPanel();        
        
        JSplitPane centrePanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, westPanel, chartPanel);
        centrePanel.setDividerLocation(0.5);
        add(centrePanel, BorderLayout.CENTER);
        
//       this.controller = new SignalWarpingDialogController(model, chartPanel, signalSelectionTable, settingsPanel);
    }
    
    private JPanel createNorthPanel() {
    	JPanel panel = new JPanel(new FlowLayout());
    	
    	SignalWarpingRunSettingsPanel runPanel = new SignalWarpingRunSettingsPanel(controller, model);
    	runPanel.addSignalWarpingRunEventListener(controller);
    	panel.add(runPanel);
    	
    	SignalWarpingDisplaySettingPanel displayPanel = new SignalWarpingDisplaySettingPanel();
    	displayPanel.addSignalWarpingDisplayListener(controller);
    	panel.add(displayPanel);
    	
    	return panel;
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
        signalSelectionTable.setAutoCreateRowSorter(true);
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
        			controller.displayBlankChart();
        		}
        	}
        });
        
        JPopupMenu tableMenu = signalSelectionTable.getComponentPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addActionListener(e->{
        	List<WarpedImageKey> keys = new ArrayList<>();
        	for(int row : signalSelectionTable.getSelectedRows())
        		keys.add(model.getKey(row));
        	
        	for(WarpedImageKey k : keys)
        		controller.deleteWarpedSignal(k);
        	
        	controller.displayBlankChart();
        });
        tableMenu.add(deleteItem);
        
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
            progressBar.setEnabled(false);
            controller.warpingComplete();
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
