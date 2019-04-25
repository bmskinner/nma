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
package com.bmskinner.nuclear_morphology.gui.tabs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.datasets.AnalysisDatasetTableCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.tables.AbstractTableCreator;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.IClusterGroup;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.components.renderers.JTextAreaCellRenderer;
import com.bmskinner.nuclear_morphology.gui.dialogs.ClusterTreeDialog;
import com.bmskinner.nuclear_morphology.gui.dialogs.TsneDialog;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent;

/**
 * This panel shows any cluster groups that have been created, and the
 * clustering options that were used to create them.
 * 
 * @author bms41
 * @since 1.9.0
 *
 */
@SuppressWarnings("serial")
public class ClusterDetailPanel extends DetailPanel {

    private static final String PANEL_TITLE_LBL = "Clusters";
    private static final String NEW_CLUSTER_LBL = "Cluster cells";
    private static final String NEW_TREE_LBL    = "Create tree";
    private static final String NEW_CLASS_LBL   = "Create classifier";
    private static final String SHOW_TREE_LBL   = "Show tree";
    private static final String NO_CLUSTERS_LBL = "No clusters present";
    private static final String MAN_CLUSTER_LBL = "Manual cluster";

    private JButton clusterButton        = new JButton(NEW_CLUSTER_LBL);
    private JButton buildTreeButton      = new JButton(NEW_TREE_LBL);
    private JButton saveClassifierButton = new JButton(NEW_CLASS_LBL);
    private JButton manualClusterBtn     = new JButton(MAN_CLUSTER_LBL);

    private JLabel statusLabel = new JLabel(NO_CLUSTERS_LBL, SwingConstants.CENTER);
    private JPanel statusPanel = new JPanel(new BorderLayout());

    private JPanel          mainPanel;
    private ExportableTable clusterDetailsTable;

    public ClusterDetailPanel(@NonNull InputSupplier context) {
        super(context);

        this.setLayout(new BorderLayout());

        mainPanel = createMainPanel();
        statusPanel = createHeader();

        this.add(mainPanel, BorderLayout.CENTER);
        this.add(statusPanel, BorderLayout.NORTH);

        setEnabled(false);

    }
    
    @Override
    public String getPanelTitle(){
        return PANEL_TITLE_LBL;
    }

    /**
     * Create the main panel with cluster table
     * 
     * @return
     */
    private JPanel createMainPanel() {
        JPanel panel = new JPanel();

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        TableModel optionsModel = AbstractTableCreator.createBlankTable();
        
        TableCellRenderer buttonRenderer = new JButtonRenderer();
        TableCellRenderer textRenderer = new JTextAreaCellRenderer();
        
        clusterDetailsTable = new ExportableTable(optionsModel) {
           
        	@Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
            
            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
            	if( (this.getValueAt(row, 0).equals(Labels.Clusters.TREE) || this.getValueAt(row, 0).equals(Labels.Clusters.CLUSTER_DIM_PLOT)) 
            			&& column>0
            			&& !(getValueAt(row, column).equals(Labels.NA))) {
            		return buttonRenderer;
            	}
            	return textRenderer;
            }
        };
        
        MouseListener mouseListener = new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {				
				int row = clusterDetailsTable.rowAtPoint(e.getPoint());
				int col = clusterDetailsTable.columnAtPoint(e.getPoint());
				if(col==0)
					return;
				
				IClusterGroup group = (IClusterGroup) clusterDetailsTable.getValueAt(0, col);
				// find the dataset with this cluster group
				IAnalysisDataset d = getDatasets().stream().filter(t->t.hasClusterGroup(group)).findFirst().orElse(null);
				
	        	if(clusterDetailsTable.getValueAt(row, 0).equals(Labels.Clusters.TREE) && 
	        			!clusterDetailsTable.getValueAt(row, col).equals(Labels.NA)) {
	        		Runnable r = () ->{
	        			ClusterTreeDialog clusterPanel = new ClusterTreeDialog(d, group);
	                    clusterPanel.addDatasetEventListener(ClusterDetailPanel.this);
	                    clusterPanel.addInterfaceEventListener(ClusterDetailPanel.this);
	            	};
	                new Thread(r).start();
	        	}
	        	
	        	if(clusterDetailsTable.getValueAt(row, 0).equals(Labels.Clusters.CLUSTER_DIM_PLOT) && 
	        			!clusterDetailsTable.getValueAt(row, col).equals(Labels.NA)) {
	        		Runnable r = () ->{
	        			TsneDialog tsneDialog = new TsneDialog(d, group);
	        			tsneDialog.addDatasetEventListener(ClusterDetailPanel.this);
	        			tsneDialog.addInterfaceEventListener(ClusterDetailPanel.this);
	        		};
	        		new Thread(r).start();
	        	}
	        	
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				// Not needed
			}

			@Override
			public void mouseExited(MouseEvent e) {
				// Not needed
			}

			@Override
			public void mousePressed(MouseEvent e) {
				// Not needed
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				// Not needed
			}
        	
        };
        
        clusterDetailsTable.addMouseListener(mouseListener);

        clusterDetailsTable.setRowSelectionAllowed(false);

        JScrollPane scrollPane = new JScrollPane(clusterDetailsTable);

        JPanel tablePanel = new JPanel(new BorderLayout());

        tablePanel.add(scrollPane, BorderLayout.CENTER);
        tablePanel.add(clusterDetailsTable.getTableHeader(), BorderLayout.NORTH);

        panel.add(tablePanel);
        return panel;

    }

    /**
     * This panel shows the status of the dataset, and holds the clustering
     * button
     * 
     * @return
     */
    private JPanel createHeader() {

        JPanel panel = new JPanel(new BorderLayout());

        JPanel buttonPanel = new JPanel(new FlowLayout());
        clusterButton.addActionListener(e ->  getDatasetEventHandler().fireDatasetEvent(DatasetEvent.CLUSTER, getDatasets()));
        buildTreeButton.addActionListener(e ->  getDatasetEventHandler().fireDatasetEvent(DatasetEvent.BUILD_TREE, getDatasets()));
        saveClassifierButton.addActionListener(e -> getDatasetEventHandler().fireDatasetEvent(DatasetEvent.TRAIN_CLASSIFIER, getDatasets()));
        manualClusterBtn.addActionListener(e ->  getDatasetEventHandler().fireDatasetEvent(DatasetEvent.MANUAL_CLUSTER, getDatasets()));
                
        saveClassifierButton.setEnabled(false);
        buildTreeButton.setEnabled(true);
        manualClusterBtn.setEnabled(true);
        buttonPanel.add(manualClusterBtn);
        buttonPanel.add(buildTreeButton);
        buttonPanel.add(clusterButton);

        buttonPanel.add(manualClusterBtn);
        buttonPanel.add(buildTreeButton);
        buttonPanel.add(clusterButton);
        
        // buttonPanel.add(saveClassifierButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);
        panel.add(statusLabel, BorderLayout.CENTER);
        return panel;
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        clusterButton.setEnabled(b);
        buildTreeButton.setEnabled(b);
        manualClusterBtn.setEnabled(b);
        // saveClassifierButton.setEnabled(b); // not yet enabled
    }

    @Override
    protected synchronized void updateSingle() {
        updateMultiple();

    }

    @Override
    protected synchronized void updateMultiple() {
        setEnabled(true);

        TableOptions options = new TableOptionsBuilder()
        		.setDatasets(getDatasets())
        		.setTarget(clusterDetailsTable)
                .build();

        setTable(options);

        if (!hasDatasets()) {
            statusLabel.setText(Labels.NULL_DATASETS);
            setEnabled(false);
        } else {

            if (isSingleDataset()) {

                setEnabled(true);

                if (!activeDataset().hasClusters()) {

                    statusLabel.setText(NO_CLUSTERS_LBL);

                } else {
                	int nGroups = activeDataset().getClusterGroups().size();
                	String plural = nGroups==1 ? "" : "s"; 
                    statusLabel.setText("Dataset has " + activeDataset().getClusterGroups().size() + " cluster group"+plural);
                }
            } else { // more than one dataset selected
                statusLabel.setText(Labels.MULTIPLE_DATASETS);
                setEnabled(false);
            }
        }
    }

    @Override
    protected synchronized void updateNull() {
        updateMultiple();

    }

    @Override
    protected JFreeChart createPanelChartType(ChartOptions options) {
        return null;
    }

    @Override
    protected TableModel createPanelTableType(TableOptions options) {
        return new AnalysisDatasetTableCreator(options).createClusterOptionsTable();
    }

    @Override
    public void eventReceived(DatasetEvent event) {
        super.eventReceived(event);

        if (event.getSource() instanceof ClusterTreeDialog) {
            this.getDatasetEventHandler().fireDatasetEvent(event);
        }
    }

    public void interfaceEventReceived(InterfaceEvent event) {
    	 super.eventReceived(event);
        if (event.getSource() instanceof ClusterTreeDialog) {
            getInterfaceEventHandler().fire(event);
        }
    }
            
    /**
     * Render a button in a cell. Note, this is non-functional - it just paints 
     * a button shape. Use a mouse listener on the table for functionality
     * @author bms41
     * @since 1.16.0
     *
     */
    private class JButtonRenderer extends JButton  implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
			String text = value==null ? "" : value instanceof IClusterGroup ? SHOW_TREE_LBL : value.toString();
            setText(text);
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            return this;
        }
    }
    
    
}
