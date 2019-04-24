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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
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
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.dialogs.ClusterTreeDialog;
import com.bmskinner.nuclear_morphology.gui.dialogs.ManualClusteringDialog;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent.InterfaceMethod;

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
    private static final String TSNE_LBL        = "Profile t-SNE";

    private JButton clusterButton        = new JButton(NEW_CLUSTER_LBL);
    private JButton buildTreeButton      = new JButton(NEW_TREE_LBL);
    private JButton saveClassifierButton = new JButton(NEW_CLASS_LBL);
    private JButton manualClusterBtn     = new JButton(MAN_CLUSTER_LBL);
    private JButton tSneBtn              = new JButton(TSNE_LBL);

    private JLabel statusLabel = new JLabel(NO_CLUSTERS_LBL, SwingConstants.CENTER);
    private JPanel statusPanel = new JPanel(new BorderLayout());

    private JPanel showTreeButtonPanel;

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
        TableCellRenderer textRenderer = new JTextAreaColumnRenderer();
        
        clusterDetailsTable = new ExportableTable(optionsModel) {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
            
            public TableCellRenderer getCellRenderer(int row, int column) {
            	if(this.getValueAt(row, 0).equals(Labels.Clusters.TREE) && column>0 && this.getValueAt(row, column) instanceof IClusterGroup) {
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
				Object value = clusterDetailsTable.getValueAt(row, col);
	        	if(value instanceof IClusterGroup) {
	        		Runnable r = () ->{
	        			// find the dataset with this cluster group
	        			IAnalysisDataset d = getDatasets().stream().filter(t->t.getClusterGroups().stream().anyMatch(c->c.getId().equals(((IClusterGroup) value).getId()))).findFirst().orElse(null);
	            		ClusterTreeDialog clusterPanel = new ClusterTreeDialog(d, (IClusterGroup) value);
	                    clusterPanel.addDatasetEventListener(ClusterDetailPanel.this);
	                    clusterPanel.addInterfaceEventListener(ClusterDetailPanel.this);
	            	};
	                new Thread(r).start();;
	        	}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseExited(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mousePressed(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				// TODO Auto-generated method stub
				
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
        clusterButton.addActionListener(e -> {
            this.getDatasetEventHandler().fireDatasetEvent(DatasetEvent.CLUSTER, getDatasets());
        });

        buildTreeButton.addActionListener(e -> {
            this.getDatasetEventHandler().fireDatasetEvent(DatasetEvent.BUILD_TREE, getDatasets());
        });

        saveClassifierButton.addActionListener(e -> {
            this.getDatasetEventHandler().fireDatasetEvent(DatasetEvent.TRAIN_CLASSIFIER, getDatasets());
        });
        
        manualClusterBtn.addActionListener(e -> {
        	try {
        		int maxGroups = activeDataset().getCollection().getCells().size()-1; // more would be silly, fewer restrictive
        		int groups = getInputSupplier().requestInt("Number of groups", 2,2,maxGroups,1);

        		List<String> groupNames = new ArrayList<>();

        		for(int i=1; i<=groups; i++){
        			String name = getInputSupplier().requestString("Name for group "+i);
        			groupNames.add(name); 
        		}

        		ManualClusteringDialog mc = new ManualClusteringDialog(activeDataset(), groupNames);
        		mc.addInterfaceEventListener(this);
        		mc.run();
        		getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);

        	} catch (RequestCancelledException e1) {
        		return;
        	}
        });
                
        tSneBtn.addActionListener(e -> {
            this.getDatasetEventHandler().fireDatasetEvent(DatasetEvent.RUN_TSNE, getDatasets());
        });


        saveClassifierButton.setEnabled(false);
        buildTreeButton.setEnabled(true);
        manualClusterBtn.setEnabled(true);
        tSneBtn.setEnabled(true);
        buttonPanel.add(manualClusterBtn);
        buttonPanel.add(buildTreeButton);
        buttonPanel.add(clusterButton);
        buttonPanel.add(tSneBtn);

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
        tSneBtn.setEnabled(b);
        // saveClassifierButton.setEnabled(b); // not yet enabled
    }

    @Override
    protected void updateSingle() {
        updateMultiple();

    }

    @Override
    protected void updateMultiple() {
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
    protected void updateNull() {
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
        if (event.getSource() instanceof ClusterTreeDialog || event.getSource() instanceof ManualClusteringDialog) {
            getInterfaceEventHandler().fire(event);
        }
    }
        
    private static class JTextAreaColumnRenderer extends JTextArea  implements TableCellRenderer {

        private static final Font DEFAULT_FONT = UIManager.getFont("Label.font");

        private void setColor(boolean isSelected, JTable table) {
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            setText(value == null ? "" : value.toString());
            setColor(isSelected,table);
            setFont(DEFAULT_FONT);
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            setLineWrap(true);
            setWrapStyleWord(true);
            Color colour = Color.BLACK;
            if (value != null && !value.toString().equals("")) {
                if(value.toString().equals("false") || value.toString().equals(Labels.NA)) {
                    colour = Color.GRAY;
                }
            }

            setForeground(colour);
            return this;
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
            setText(value == null ? "" : "Show tree");
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            return this;
        }
    }
    
    
}
