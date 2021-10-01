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
package com.bmskinner.nuclear_morphology.gui.tabs.cells_detail;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.UUID;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.ICytoplasm;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.signals.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.signals.ISignalCollection;
import com.bmskinner.nuclear_morphology.core.InputSupplier;

@SuppressWarnings("serial")
public class ComponentListPanel extends AbstractCellDetailPanel implements ListSelectionListener {
	
	private static final Logger LOGGER = Logger.getLogger(ComponentListPanel.class.getName());

    private static final String PANEL_TITLE_LBL = "Components";
    private JList<ComponentListCell> list;
    private JScrollPane              scrollPane;
    private String                   prevComponent = "";

    public ComponentListPanel(@NonNull InputSupplier context, CellViewModel model) {
        super(context, model, PANEL_TITLE_LBL);

        this.setLayout(new BorderLayout());

        scrollPane = new JScrollPane();

        list = new JList<ComponentListCell>();
        ListModel<ComponentListCell> objectModel = createListModel();

        list.setModel(objectModel);
        list.addListSelectionListener(this);
        list.setEnabled(false);

        scrollPane.setViewportView(list);
        Dimension size = new Dimension(120, 200);
        scrollPane.setMinimumSize(size);
        scrollPane.setPreferredSize(size);

        this.add(scrollPane, BorderLayout.CENTER);
    }
    
    /**
     * Create a list with all the components in the active cell
     * 
     * @return
     */
    private ListModel<ComponentListCell> createListModel() {
        DefaultListModel<ComponentListCell> model = new DefaultListModel<>();

        if (this.getCellModel().hasCell()) {

            Nucleus n = getCellModel().getCell().getPrimaryNucleus();
            // Every cell has a nucleus
            ComponentListCell nucleusCell = new ComponentListCell(CellularComponent.NUCLEUS, n);

            model.addElement(nucleusCell);

            // Cytoplasm
            if (getCellModel().getCell().hasCytoplasm()) {
                ICytoplasm cyto = getCellModel().getCell().getCytoplasm();
                ComponentListCell cytoCell = new ComponentListCell(CellularComponent.CYTOPLASM, cyto);
                model.addElement(cytoCell);
            }

            ISignalCollection signalCollection = n.getSignalCollection();

            // Add signals groups present
            for (UUID signalGroupId : signalCollection.getSignalGroupIds()) {

                if (signalCollection.hasSignal(signalGroupId)) {

                    // Since all we want is a single component within the
                    // collection, just take the first signal
                    INuclearSignal signal = signalCollection.getSignals(signalGroupId).get(0);
                    String signalGroupName;
                    signalGroupName = activeDataset().getCollection().getSignalGroup(signalGroupId).get().getGroupName();

                    ComponentListCell signalCell = new ComponentListCell(signalGroupName, signal);
                    model.addElement(signalCell);
                }
            }
        }
        return model;
    }

    public void update() {

        LOGGER.finest("Updating component list for cell");
        list.removeListSelectionListener(this);
        ListModel<ComponentListCell> model = createListModel();
        list.setModel(model);

        if (this.getCellModel().hasCell()) {
        	LOGGER.finest("Cell is not null");

            // Check if the new cell has the same component as the last
            int selectedIndex = 0;
            for (int i = 0; i < model.getSize(); i++) {
                ComponentListCell tableCell = (ComponentListCell) list.getModel().getElementAt(i);
                if (tableCell.toString().equals(prevComponent)) {
                    selectedIndex = i;
                }
            }
            list.setSelectedIndex(selectedIndex);
            prevComponent = ((ComponentListCell) list.getModel().getElementAt(selectedIndex)).toString(); // set
                                                                                                          // the
                                                                                                          // new
                                                                                                          // component
                                                                                                          // string
            this.getCellModel().setComponent(getSelectedComponent());
            list.setEnabled(true);
        } else {
            list.setEnabled(false);
        }
        list.addListSelectionListener(this);

    }

    private ComponentListCell getSelectedRow() {
        ComponentListCell tableCell = null;
        int row = list.getSelectedIndex();
        LOGGER.finer("Selected component row " + row);
        if (row >= 0) { // -1 if nothing selected
            tableCell = (ComponentListCell) list.getModel().getElementAt(row);
        }
        return tableCell;
    }

    private CellularComponent getSelectedComponent() {
        int row = list.getSelectedIndex();
        CellularComponent c = null;
        if (row >= 0) { // -1 if nothing selected
            ComponentListCell tableCell = getSelectedRow();
            c = tableCell.getComponent();
        }
        return c;
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {

    	LOGGER.finest("Component selection changed");
        ComponentListCell cell = getSelectedRow();
        if (cell != null) {
            prevComponent = cell.toString(); // set the new component string
            CellularComponent c = cell.getComponent();
            this.getCellModel().setComponent(c);
        }

    }

    @Override
    public void setChartsAndTablesLoading() {
    }

    @Override
    protected TableModel createPanelTableType(TableOptions options) {
        return null;
    }

    @Override
    protected JFreeChart createPanelChartType(ChartOptions options) {
        return null;
    }

}
