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


package com.bmskinner.nuclear_morphology.gui.tabs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.datasets.AnalysisDatasetTableCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.tables.AbstractTableCreator;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.IClusterGroup;
import com.bmskinner.nuclear_morphology.gui.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.DatasetEventListener;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.dialogs.ClusterTreeDialog;
import com.bmskinner.nuclear_morphology.gui.dialogs.ManualClusteringDialog;

/**
 * This panel shows any cluster groups that have been created, and the
 * clustering options that were used to create them.
 * 
 * @author bms41
 * @since 1.9.0
 *
 */
@SuppressWarnings("serial")
public class ClusterDetailPanel extends DetailPanel implements DatasetEventListener {

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

    private JPanel showTreeButtonPanel;

    private JPanel          mainPanel;
    private ExportableTable clusterDetailsTable;

    // private ClustersPanel clusterPanel;

    public ClusterDetailPanel() {
        super();

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
        clusterDetailsTable = new ExportableTable(optionsModel) {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        };

        setRenderer(clusterDetailsTable, new ClusterTableCellRenderer());

        JScrollPane scrollPane = new JScrollPane(clusterDetailsTable);

        JPanel tablePanel = new JPanel(new BorderLayout());

        tablePanel.add(scrollPane, BorderLayout.CENTER);
        tablePanel.add(clusterDetailsTable.getTableHeader(), BorderLayout.NORTH);

        panel.add(tablePanel);

        showTreeButtonPanel = createTreeButtonPanel(null);
        panel.add(showTreeButtonPanel);
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
        		final String chooseLbl  = "Number of groups";

    			SpinnerNumberModel sModel = new SpinnerNumberModel(2,2,5,1);

    			JSpinner spinner = new JSpinner(sModel);

    			int option = JOptionPane.showOptionDialog(null, spinner, chooseLbl, 
    					JOptionPane.OK_CANCEL_OPTION,
    					JOptionPane.QUESTION_MESSAGE, null, null, null);

    			if (option == JOptionPane.OK_OPTION) {

    				int groups = (int) spinner.getModel().getValue();
    				
    				List<String> groupNames = new ArrayList<>();
    				
    				for(int i=0; i<groups; i++){
    					groupNames.add(JOptionPane.showInputDialog("Name for group "+i)); 
    				}

    				if (groups > 1) {
    					ManualClusteringDialog mc = new ManualClusteringDialog(getDatasets().get(0), groupNames);
    					mc.addInterfaceEventListener(this);
    					mc.run();
    					getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
    				} else {
    					warn("Must have at least two groups");
    				}
    			}
    		}catch(Exception ex){
    			warn("Error getting manual cluster count");
    			stack("Error getting manual cluster count", ex);
    		}
        	
            
        });

        saveClassifierButton.setEnabled(false);
        buildTreeButton.setEnabled(true);
        manualClusterBtn.setEnabled(true);
        buttonPanel.add(manualClusterBtn);
        buttonPanel.add(buildTreeButton);
        buttonPanel.add(clusterButton);
        
        // buttonPanel.add(saveClassifierButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);
        panel.add(statusLabel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Create the panel holding 'Show tree' buttons
     * 
     * @param buttons
     *            the buttons to be drawn
     * @return
     */
    private JPanel createTreeButtonPanel(List<JComponent> buttons) {
        JPanel panel = new JPanel();

        GridBagLayout gbl = new GridBagLayout();
        panel.setLayout(gbl);

        GridBagConstraints c = new GridBagConstraints();

        c.anchor = GridBagConstraints.CENTER; // place the buttons in the middle
                                              // of their grid
        c.gridwidth = buttons == null ? 1 : buttons.size() + 1; // one button
                                                                // per column,
                                                                // plus a blank
        c.gridheight = 1;
        c.fill = GridBagConstraints.NONE; // don't resize the buttons
        c.weightx = 1.0; // buttons have padding between them

        Dimension fillerSize = new Dimension(10, 5);
        panel.add(new Box.Filler(fillerSize, fillerSize, fillerSize), c);
        if (buttons != null) {
            for (JComponent button : buttons) {
                panel.add(button, c);
            }
        }

        return panel;
    }

    /**
     * Create the appropriate number of 'Show tree' buttons for the selected
     * datasets, and return them as a list
     * 
     * @return
     */
    private List<JComponent> createShowTreeButtons() {

        if (!hasDatasets())
            return null;

        List<JComponent> result = new ArrayList<JComponent>();
        Dimension fillerSize = new Dimension(10, 5);

        for (final IAnalysisDataset d : getDatasets()) {

            for (final IClusterGroup g : d.getClusterGroups()) {

                if (g.hasTree()) {
                    JButton button = new JButton(SHOW_TREE_LBL);
                    button.addActionListener(e -> {
                        Thread thr = new Thread() {
                            public void run() {
                                ClusterTreeDialog clusterPanel = new ClusterTreeDialog(d, g);
                                clusterPanel.addDatasetEventListener(ClusterDetailPanel.this);
                                clusterPanel.addInterfaceEventListener(ClusterDetailPanel.this);
                            }
                        };
                        thr.start();
                    });
                    result.add(button);
                } else {
                    result.add(new Box.Filler(fillerSize, fillerSize, fillerSize));
                }

            }
        }

        return result;
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        clusterButton.setEnabled(b);
        buildTreeButton.setEnabled(b);
        manualClusterBtn.setEnabled(b);
        // saveClassifierButton.setEnabled(b); // not yet enabled
    }

    private void updateTreeButtonsPanel() {

        mainPanel.remove(showTreeButtonPanel);

        List<JComponent> buttons = createShowTreeButtons();

        showTreeButtonPanel = createTreeButtonPanel(buttons);

        // add this new panel
        mainPanel.add(showTreeButtonPanel);
        mainPanel.revalidate();
        mainPanel.repaint();
        mainPanel.setVisible(true);
    }

    @Override
    protected void updateSingle() {
        updateMultiple();

    }

    @Override
    protected void updateMultiple() {
        setEnabled(true);

        TableOptions options = new TableOptionsBuilder().setDatasets(getDatasets()).setTarget(clusterDetailsTable)
                .setRenderer(TableOptions.ALL_EXCEPT_FIRST_COLUMN, new ClusterTableCellRenderer()).build();

        setTable(options);

        updateTreeButtonsPanel();

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
        finest("Updated cluster panel");

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
    public void datasetEventReceived(DatasetEvent event) {
        super.datasetEventReceived(event);

        if (event.getSource() instanceof ClusterTreeDialog) {
            this.getDatasetEventHandler().fireDatasetEvent(event);
        }
    }

    public void interfaceEventReceived(InterfaceEvent event) {
        super.interfaceEventReceived(event);
        if (event.getSource() instanceof ClusterTreeDialog || event.getSource() instanceof ManualClusteringDialog) {
            getInterfaceEventHandler().fireInterfaceEvent(event);
        }
    }

    /**
     * Colour analysis parameter table cell background. If parameters are false
     * or N/A, make the text colour grey
     */
    public class ClusterTableCellRenderer extends DefaultTableCellRenderer {

        public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            Color colour = Color.BLACK;

            if (value != null && !value.toString().equals("")) {

                if (value.toString().equals("false") || value.toString().equals("N/A")) {
                    colour = Color.GRAY;
                }
            }

            setForeground(colour);
            return this;
        }

    }

}
