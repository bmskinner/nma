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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.UUID;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.analysis.signals.SignalManager;
import com.bmskinner.nuclear_morphology.charting.charts.AbstractChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.OutlineChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ConsensusNucleusChartPanel;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.datasets.SignalTableCell;
import com.bmskinner.nuclear_morphology.charting.datasets.tables.AbstractTableCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.tables.NuclearSignalTableCreator;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.charting.options.DefaultTableOptions.TableType;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.events.ChartSetEventListener;
import com.bmskinner.nuclear_morphology.gui.events.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.tabs.CosmeticHandler;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;

@SuppressWarnings("serial")
public class SignalsOverviewPanel extends DetailPanel implements ActionListener, ChartSetEventListener {

    private static final String PANEL_TITLE_LBL = "Overview";
    
    
    /** Consensus nucleus with signals overlaid  */
    private ConsensusNucleusChartPanel chartPanel;
    
    /** signal stats */
    private ExportableTable statsTable;
    
    /** consensus chart and signal visibility checkboxes */
    private JPanel consensusAndCheckboxPanel;
    
    /** Signal visibility checkbox panel */
    private JPanel checkboxPanel;

    /** Launch signal warping */
    private JButton warpButton;

    /** Messages to clarify when UI is disabled */
    private JLabel headerText;

    private static final String SET_SIGNAL_GROUP_VISIBLE_ACTION = "GroupVisble_";

    private final CosmeticHandler cosmeticHandler = new CosmeticHandler(this);

    /**
     * Create with an input supplier
     * @param context the input supplier
     */
    public SignalsOverviewPanel(@NonNull InputSupplier inputSupplier) {
        super(inputSupplier);

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        JScrollPane scrollPane = createStatsPane();
        add(scrollPane);

        consensusAndCheckboxPanel = createConsensusPanel();
        add(consensusAndCheckboxPanel);

    }
    
    @Override
    public String getPanelTitle(){
        return PANEL_TITLE_LBL;
    }

    private JPanel createConsensusPanel() {

        final JPanel panel = new JPanel(new BorderLayout());
        JFreeChart chart = OutlineChartFactory.makeEmptyChart();

        // the chart is inside a chartPanel; the chartPanel is inside a JPanel
        // this allows a checkbox panel to be added to the JPanel later
        chartPanel = new ConsensusNucleusChartPanel(chart);// {
        panel.add(chartPanel, BorderLayout.CENTER);
        chartPanel.setFillConsensus(false);

        checkboxPanel = createSignalCheckboxPanel();

        panel.add(checkboxPanel, BorderLayout.NORTH);

        return panel;
    }

    private JScrollPane createStatsPane() {

        TableModel tableModel = AbstractTableCreator.createBlankTable();
        statsTable = new ExportableTable(tableModel); // table for basic stats
        statsTable.setEnabled(false);

        statsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

                JTable table = (JTable) e.getSource();

                int row = table.rowAtPoint(e.getPoint());
                int column = table.columnAtPoint(e.getPoint());

                if (e.getClickCount() == DOUBLE_CLICK && column>0) {

                    IAnalysisDataset d = getDatasets().get(column - 1);
                    int nextRow = row+1;
                    String nextRowName = table.getModel().getValueAt(nextRow, 0).toString();
                    if (nextRowName.equals(Labels.Signals.SIGNAL_GROUP_LABEL)) {
                        SignalTableCell signalGroup = getSignalGroupFromTable(table, nextRow, column);
                        cosmeticHandler.changeSignalColour(d, signalGroup.getID());
                        getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
                    }

                    
                    if( table.getModel().getValueAt(row, 0).toString().equals(Labels.Signals.SIGNAL_ID_LABEL)) {
                    	int signalGroupNameRow = row-3;
                    	String signalGroupRowName = table.getModel().getValueAt(signalGroupNameRow, 0).toString();
                    	String signalGroupName = table.getModel().getValueAt(row, column).toString();
                    	if(signalGroupRowName.equals(Labels.Signals.SIGNAL_GROUP_LABEL))                    		
                    		signalGroupName = table.getModel().getValueAt(signalGroupNameRow, column).toString();
                           
                        UUID signalGroup = UUID.fromString(table.getModel().getValueAt(row, column).toString());

                        String[] options = { "Don't delete signals", "Delete signals" };
                        

						try {
							int result = getInputSupplier().requestOption(options, 0, String.format("Delete signal group %s in %s?", signalGroupName, d.getName()), "Delete signal group?");
							if (result!=0) { 
	                             d.getCollection().getSignalManager().removeSignalGroup(signalGroup);
	                             getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
	                         }
							
						} catch (RequestCancelledException e1) {
							// no action
						}
                         
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(statsTable);
        return scrollPane;
    }

    private SignalTableCell getSignalGroupFromTable(JTable table, int row, int column) {
        return (SignalTableCell) table.getModel().getValueAt(row, column);
    }

    /**
     * Create the checkboxes that set each signal channel visible or not
     */
    private JPanel createSignalCheckboxPanel() {
        JPanel panel = new JPanel();

        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        warpButton = new JButton(Labels.Signals.WARP_BTN_LBL);
        warpButton.setToolTipText(Labels.Signals.WARP_BTN_TOOLTIP);
        warpButton.addActionListener(e ->  new SignalWarpingDialog(getDatasets()));
        warpButton.setEnabled(false);

        panel.add(warpButton);

        if (isSingleDataset()) {

            for (UUID signalGroup : activeDataset().getCollection().getSignalGroupIDs()) {

                if (signalGroup.equals(IShellResult.RANDOM_SIGNAL_ID))
                    continue;

                // get the status within each dataset
				boolean visible = activeDataset().getCollection().getSignalGroup(signalGroup).get().isVisible();

				String name = activeDataset().getCollection().getSignalManager().getSignalGroupName(signalGroup);

				// make a checkbox for each signal group in the dataset
				JCheckBox box = new JCheckBox(name, visible);

				// Don't enable when the consensus is missing
				box.setEnabled(activeDataset().getCollection().hasConsensus());

				box.addActionListener(e -> {
				    activeDataset().getCollection().getSignalGroup(signalGroup).get().setVisible(box.isSelected());
				    getSignalChangeEventHandler().fireSignalChangeEvent(SignalChangeEvent.GROUP_VISIBLE_PREFIX);
				    this.refreshChartCache(getDatasets());
				});
				panel.add(box);

            }

        }

        headerText = new JLabel("");
        headerText.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        panel.add(headerText);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

        return panel;
    }

    /**
     * Update the signal stats with the given datasets
     * 
     * @param list
     *            the datasets
     * @throws Exception
     */
    private void updateSignalStatsPanel() {

        TableOptions options = new TableOptionsBuilder().setDatasets(getDatasets())
                .setType(TableType.SIGNAL_STATS_TABLE)
                .setTarget(statsTable)
                .setRenderer(TableOptions.ALL_EXCEPT_FIRST_COLUMN, new SignalTableCellRenderer())
                .build();

        setTable(options);

    }

    private void updateCheckboxPanel() {
        if (isSingleDataset()) {

            // make a new panel for the active dataset
            consensusAndCheckboxPanel.remove(checkboxPanel);
            checkboxPanel = createSignalCheckboxPanel();

            // add this new panel
            consensusAndCheckboxPanel.add(checkboxPanel, BorderLayout.NORTH);
            consensusAndCheckboxPanel.revalidate();
            consensusAndCheckboxPanel.repaint();
            consensusAndCheckboxPanel.setVisible(true);

            if (activeDataset()!=null && activeDataset().getCollection().hasConsensus()
                    && activeDataset().getCollection().getSignalManager().hasSignals()) {
                warpButton.setEnabled(true);
            }

        }

        if (isMultipleDatasets()) {
            if (IAnalysisDataset.haveConsensusNuclei(getDatasets())) {

                // Check at least one of the selected datasets has signals
                String text = "";

                boolean hasSignals = false;
                for (IAnalysisDataset d : getDatasets()) {

                    SignalManager m = d.getCollection().getSignalManager();
                    if (m.hasSignals()) {
                        hasSignals = true;
                        break;
                    }
                }

                // Segments need to match for mesh creation
                boolean segmentsMatch = IBorderSegment.segmentCountsMatch(getDatasets());

                if (!segmentsMatch) {
                    text = "Segments do not match between datasets";
                }

                if (hasSignals && segmentsMatch) {
                    warpButton.setEnabled(true);
                } else {
                    warpButton.setEnabled(false);
                    headerText.setText(text);
                }

            } else {
                warpButton.setEnabled(false);
                headerText.setText("Datasets do not all have consensus");
            }
        }
    }

    private void updateSignalConsensusChart() {
        try {

            // The options do not hold which signal groups are visible
            // so we must invalidate the cache whenever they change
            this.clearChartCache(getDatasets());

            ChartOptions options = new ChartOptionsBuilder().setDatasets(getDatasets()).setShowWarp(false)
                    .setTarget(chartPanel).build();

            setChart(options);

        } catch (Exception e) {
        	chartPanel.setChart(AbstractChartFactory.makeErrorChart());
            stack("Error updating signal overview panel", e);
        }
    }

    private UUID getSignalGroupFromLabel(String label) {
        String[] names = label.split("_");
        return UUID.fromString(names[1]);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().startsWith(SET_SIGNAL_GROUP_VISIBLE_ACTION)) {

            UUID signalGroup = getSignalGroupFromLabel(e.getActionCommand());
			JCheckBox box = (JCheckBox) e.getSource();
			activeDataset().getCollection().getSignalGroup(signalGroup).get().setVisible(box.isSelected());
			getSignalChangeEventHandler().fireSignalChangeEvent(SET_SIGNAL_GROUP_VISIBLE_ACTION);
			this.refreshChartCache(getDatasets());
        }
        updateSignalConsensusChart();

    }

    @Override
    protected void updateSingle() {
        updateMultiple();

    }

    @Override
    protected void updateMultiple() {

        updateCheckboxPanel();
        updateSignalConsensusChart();
        updateSignalStatsPanel();
    }

    @Override
    protected void updateNull() {
        updateMultiple();

    }

    @Override
    public void setChartsAndTablesLoading() {
        super.setChartsAndTablesLoading();
        chartPanel.setChart(AbstractChartFactory.createLoadingChart());
        statsTable.setModel(AbstractTableCreator.createLoadingTable());

    }

    @Override
    protected JFreeChart createPanelChartType(@NonNull ChartOptions options) {
        return new OutlineChartFactory(options).makeSignalOutlineChart();
    }

    @Override
    protected TableModel createPanelTableType(@NonNull TableOptions options) {
        return new NuclearSignalTableCreator(options).createSignalStatsTable();
    }

    @Override
    public void chartSetEventReceived(ChartSetEvent e) {
        ((ExportableChartPanel) e.getSource()).restoreAutoBounds();

    }
}
