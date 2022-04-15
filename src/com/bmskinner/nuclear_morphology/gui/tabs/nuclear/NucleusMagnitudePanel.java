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
package com.bmskinner.nuclear_morphology.gui.tabs.nuclear;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.components.panels.WrappedLabel;
import com.bmskinner.nuclear_morphology.gui.components.renderers.PairwiseTableCellRenderer;
import com.bmskinner.nuclear_morphology.gui.dialogs.RandomSamplingDialog;
import com.bmskinner.nuclear_morphology.gui.tabs.AbstractPairwiseDetailPanel;
import com.bmskinner.nuclear_morphology.visualisation.datasets.AnalysisDatasetTableCreator;
import com.bmskinner.nuclear_morphology.visualisation.options.TableOptions;
import com.bmskinner.nuclear_morphology.visualisation.options.TableOptionsBuilder;

@SuppressWarnings("serial")
public class NucleusMagnitudePanel extends AbstractPairwiseDetailPanel {

    private static final String PANEL_TITLE_LBL = "Magnitude";
    
    private JButton randomSamplingButton;

    public NucleusMagnitudePanel() {
        super();
    }
    
    @Override
    public String getPanelTitle(){
        return PANEL_TITLE_LBL;
    }

    /**
     * Create the info panel
     * 
     * @return
     */
    @Override
    protected JPanel createInfoPanel() {

        /*
         * Header labels
         */
        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.Y_AXIS));
        
        String infoString = "Pairwise magnitude comparisons between populations\n"
        		+"Row median value as a proportion of column median value";
        
        labelPanel.add(new WrappedLabel(infoString));

        /*
         * Control buttons
         */
        JPanel buttonPanel = new JPanel(new FlowLayout());
        randomSamplingButton = new JButton("Random sampling");
        randomSamplingButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                new RandomSamplingDialog(activeDataset());

            }
        });
        randomSamplingButton.setEnabled(false);

        buttonPanel.add(randomSamplingButton);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.X_AXIS));
        infoPanel.add(labelPanel);
        infoPanel.add(buttonPanel);
        return infoPanel;
    }

    /**
     * This method must be overridden by the extending class to perform the
     * actual update when a single dataset is selected
     */
    protected void updateSingle() {
        scrollPane.setColumnHeaderView(null);
        tablePanel = createTablePanel();
        randomSamplingButton.setEnabled(true);

        JPanel panel = new JPanel(new FlowLayout());
        panel.add(new JLabel(Labels.SINGLE_DATASET, JLabel.CENTER));
        tablePanel.add(panel);

        scrollPane.setViewportView(tablePanel);
        ;
        tablePanel.repaint();
    }

    /**
     * This method must be overridden by the extending class to perform the
     * actual update when a multiple datasets are selected
     */
    protected void updateMultiple() {
        scrollPane.setColumnHeaderView(null);
        tablePanel = createTablePanel();
        randomSamplingButton.setEnabled(false);

        // NucleusType type =
        // IAnalysisDataset.getBroadestNucleusType(getDatasets());
        for (Measurement stat : Measurement.getNucleusStats()) {

            TableOptions options = new TableOptionsBuilder().setDatasets(getDatasets()).addStatistic(stat).build();

            TableModel model = getTable(options);

            ExportableTable table = new ExportableTable(model);
            setRenderer(table, new PairwiseTableCellRenderer());
            addWilconxonTable(tablePanel, table, stat.toString());
            scrollPane.setColumnHeaderView(table.getTableHeader());

        }
        tablePanel.revalidate();
        scrollPane.setViewportView(tablePanel);
        ;
        tablePanel.repaint();
    }

    @Override
    protected TableModel createPanelTableType(@NonNull TableOptions options) {
        return new AnalysisDatasetTableCreator(options).createMagnitudeStatisticTable(CellularComponent.NUCLEUS);
    }

    /**
     * This method must be overridden by the extending class to perform the
     * actual update when a no datasets are selected
     */
    protected void updateNull() {
        randomSamplingButton.setEnabled(false);
        tablePanel.add(new JLabel("No datasets selected", JLabel.CENTER));
        scrollPane.setViewportView(tablePanel);
        ;
        tablePanel.repaint();
    }
}
