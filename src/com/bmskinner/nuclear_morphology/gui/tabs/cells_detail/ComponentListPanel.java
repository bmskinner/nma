package com.bmskinner.nuclear_morphology.gui.tabs.cells_detail;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.UUID;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAcrosome;
import com.bmskinner.nuclear_morphology.components.ICytoplasm;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalCollection;
import com.bmskinner.nuclear_morphology.components.nuclear.UnavailableSignalGroupException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;

@SuppressWarnings("serial")
public class ComponentListPanel extends AbstractCellDetailPanel implements ListSelectionListener {

    private JList<ComponentListCell> list;
    private JScrollPane              scrollPane;
    private String                   prevComponent = "";

    public ComponentListPanel(CellViewModel model) {
        super(model);

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
        DefaultListModel<ComponentListCell> model = new DefaultListModel<ComponentListCell>();

        if (this.getCellModel().hasCell()) {

            Nucleus n = getCellModel().getCell().getNucleus();
            // Every cell has a nucleus
            ComponentListCell nucleusCell = new ComponentListCell(CellularComponent.NUCLEUS, n);

            model.addElement(nucleusCell);

            // Cytoplasm
            if (getCellModel().getCell().hasCytoplasm()) {
                ICytoplasm cyto = getCellModel().getCell().getCytoplasm();
                ComponentListCell cytoCell = new ComponentListCell(CellularComponent.CYTOPLASM, cyto);
                model.addElement(cytoCell);
            }

            // Acrosomes
            if (getCellModel().getCell().hasAcrosome()) {
                IAcrosome a = getCellModel().getCell().getAcrosomes().get(0);
                ComponentListCell cell = new ComponentListCell(CellularComponent.ACROSOME, a);
                model.addElement(cell);
            }

            ISignalCollection signalCollection = n.getSignalCollection();

            // Add signals groups present
            for (UUID signalGroupId : signalCollection.getSignalGroupIDs()) {

                if (signalCollection.hasSignal(signalGroupId)) {

                    // Since all we want is a single component within the
                    // collection, just take the first signal
                    INuclearSignal signal = signalCollection.getSignals(signalGroupId).get(0);
                    String signalGroupName;
                    try {

                        signalGroupName = activeDataset().getCollection().getSignalGroup(signalGroupId).getGroupName();

                    } catch (UnavailableSignalGroupException e) {
                        fine("Signal group is not present in collection", e);
                        continue;
                    }

                    ComponentListCell signalCell = new ComponentListCell(signalGroupName, signal);
                    model.addElement(signalCell);
                }
            }
        }
        return model;
    }

    public void update() {

        finest("Updating component list for cell");
        list.removeListSelectionListener(this);
        ListModel<ComponentListCell> model = createListModel();
        list.setModel(model);

        if (this.getCellModel().hasCell()) {
            finest("Cell is not null");

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
        finer("Selected component row " + row);
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

        finest("Component selection changed");
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
