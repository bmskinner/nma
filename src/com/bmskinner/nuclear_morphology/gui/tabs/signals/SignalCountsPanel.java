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
import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.analysis.nucleus.CellCollectionFilterBuilder;
import com.bmskinner.nuclear_morphology.analysis.nucleus.CellCollectionFilterer;
import com.bmskinner.nuclear_morphology.analysis.nucleus.CellCollectionFilterer.CollectionFilteringException;
import com.bmskinner.nuclear_morphology.analysis.nucleus.FilteringOptions;
import com.bmskinner.nuclear_morphology.analysis.nucleus.FilteringOptions.FilterMatchType;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.charting.charts.AbstractChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.ViolinChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.datasets.VirtualDataset;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.components.panels.SignalGroupSelectionPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.SettingsDialog;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Plot the number of signals per nucleus/per cell
 * @author ben
 * @since 1.14.0
 *
 */
@SuppressWarnings("serial")
public class SignalCountsPanel extends DetailPanel {
	
	private static final Logger LOGGER = Logger.getLogger(SignalCountsPanel.class.getName());
	
	private static final String FILTER_LBL = "Filter nuclei";
	
	private static final String PANEL_TITLE_LBL = "Signal counts";
	
	private JButton filterBtn = new JButton(FILTER_LBL);
	
	private ExportableChartPanel chartPanel;
	
	public SignalCountsPanel(@NonNull InputSupplier context) {
        super(context, PANEL_TITLE_LBL);
        createUI();
    }

    private void createUI() {
        this.setLayout(new BorderLayout());
        JPanel header = createHeader();
        add(header, BorderLayout.NORTH);
        
        chartPanel = new ExportableChartPanel(AbstractChartFactory.createEmptyChart());
        add(chartPanel, BorderLayout.CENTER);
    }
    
    /**
     * Create the header panel
     * 
     * @return
     */
    private JPanel createHeader() {
        JPanel panel = new JPanel();
        filterBtn.addActionListener(e->{
        		SignalCountFilteringSetupDialog dialog = new SignalCountFilteringSetupDialog(activeDataset());
        		if(dialog.isReadyToRun())
        			dialog.filter();
        });
        panel.add(filterBtn);
        filterBtn.setEnabled(false);
        return panel;
    }

    @Override
    protected void updateSingle() {
        updateMultiple();
        if(activeDataset()!=null && activeDataset().getCollection().getSignalManager().hasSignals())
        	filterBtn.setEnabled(true);
    }

    @Override
    protected void updateMultiple() {
    	filterBtn.setEnabled(false);
    	ChartOptions options = new ChartOptionsBuilder()
    			.setDatasets(getDatasets())
        		.addStatistic(Measurement.NUCLEUS_SIGNAL_COUNT)
        		.setScale(GlobalOptions.getInstance().getScale())
        		.setSwatch(GlobalOptions.getInstance().getSwatch())
        		.setTarget(chartPanel)
        		.build();
        
        setChart(options);
    }

    @Override
    protected void updateNull() {
        updateMultiple();
        filterBtn.setEnabled(false);
    }
    
    @Override
    public void setChartsAndTablesLoading() {
        super.setChartsAndTablesLoading();
        chartPanel.setChart(AbstractChartFactory.createLoadingChart());

    }

    @Override
    protected JFreeChart createPanelChartType(@NonNull ChartOptions options) {
        return new ViolinChartFactory(options).createStatisticPlot(CellularComponent.NUCLEAR_SIGNAL) ;
    }
    

    private class SignalCountFilteringSetupDialog extends SettingsDialog {

        private static final String DIALOG_TITLE = "Signal filtering options";

        private final IAnalysisDataset dataset;
        
        private int minSignals;
        private int maxSignals;
        private SignalGroupSelectionPanel groupPanel;

        protected JPanel headingPanel;
        protected JPanel optionsPanel;
        protected JPanel footerPanel;

        public SignalCountFilteringSetupDialog(final @NonNull IAnalysisDataset dataset) {
            this(dataset, DIALOG_TITLE);
        }

        /**
         * Constructor that does not make panel visible
         * 
         * @param mw
         * @param title
         */
        protected SignalCountFilteringSetupDialog(final @NonNull IAnalysisDataset dataset, final String title) {
            super(true);
            this.dataset = dataset;
            setTitle(title);
            createUI();
            pack();
            setVisible(true);
        }

        protected JPanel createHeader() {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            return panel;
        }
        
        public void filter() {
        	
        	FilteringOptions options = new CellCollectionFilterBuilder()
        			.setMatchType(FilterMatchType.ALL_MATCH)
        			.add(Measurement.NUCLEUS_SIGNAL_COUNT, CellularComponent.NUCLEUS, groupPanel.getSelectedID(), minSignals, maxSignals)
        			.build();
        	
        	CellCollectionFilterer f = new CellCollectionFilterer(options);
            
            try {
            	ICellCollection filtered  = f.filter(dataset.getCollection());
            	ICellCollection virt = new VirtualDataset(dataset, filtered.getName());
            	filtered.getCells().forEach(c->virt.addCell(c));
            	virt.setName("Filtered_signal_count_"+groupPanel.getSelectedGroup().getGroupName());

            	dataset.getCollection().getProfileManager().copyCollectionOffsets(virt);
            	dataset.addChildCollection(virt);		
            } catch (CollectionFilteringException | ProfileException e1) {
            	LOGGER.log(Loggable.STACK, "Unable to filter collection for " + dataset.getName(), e1);
            }
            
            getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.REFRESH_POPULATIONS);
        }


        protected void createUI() {

            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(5, 5, 5, 5));

            headingPanel = createHeader();
            getContentPane().add(headingPanel, BorderLayout.NORTH);

            footerPanel = createFooter();
            getContentPane().add(footerPanel, BorderLayout.SOUTH);
            
            optionsPanel = new JPanel();
            GridBagLayout layout = new GridBagLayout();
            optionsPanel.setLayout(layout);
            
            List<JLabel> labels = new ArrayList<>();
            List<Component> fields = new ArrayList<>();
            
            
            groupPanel = new SignalGroupSelectionPanel(dataset);
            labels.add(new JLabel("Signal group to filter"));
            fields.add(groupPanel);
            
            int max = dataset.getCollection().getNuclei().stream()
            	.flatMap(n->n.getSignalCollection().getSignals().stream())
            	.mapToInt(l->l.size()).max().orElse(0);
            
            maxSignals = max;
            
            SpinnerNumberModel minSignalModel = new SpinnerNumberModel(0, 0, max, 1);
            JSpinner minSpinner = new JSpinner(minSignalModel);
            minSpinner.addChangeListener(e-> minSignals = (int) minSignalModel.getValue());
            
            labels.add(new JLabel("Min signals per nucleus"));
            fields.add(minSpinner);
            
            SpinnerNumberModel maxSignalModel = new SpinnerNumberModel(max, 0, max, 1);
            JSpinner maxSpinner = new JSpinner(maxSignalModel);
            maxSpinner.addChangeListener(e-> maxSignals = (int) maxSignalModel.getValue());
            
            labels.add(new JLabel("Max signals per nucleus"));
            fields.add(maxSpinner);
            
            this.addLabelTextRows(labels, fields, layout, optionsPanel);
            getContentPane().add(optionsPanel, BorderLayout.CENTER);
        }
    }

}
