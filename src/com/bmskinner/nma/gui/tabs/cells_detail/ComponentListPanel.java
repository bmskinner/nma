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
package com.bmskinner.nma.gui.tabs.cells_detail;

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

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ICytoplasm;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.signals.INuclearSignal;
import com.bmskinner.nma.components.signals.ISignalCollection;

@SuppressWarnings("serial")
public class ComponentListPanel extends AbstractCellDetailPanel implements ListSelectionListener {

	public record SelectableComponent(@NonNull String name, @NonNull CellularComponent component) {
		@Override
		public String toString() {
			return name;
		}
	}

	private static final Logger LOGGER = Logger.getLogger(ComponentListPanel.class.getName());

	private static final String PANEL_TITLE_LBL = "Components";
	private JList<SelectableComponent> list;
	private JScrollPane scrollPane;
	private String prevComponent = "";

	public ComponentListPanel(CellViewModel model) {
		super(model, PANEL_TITLE_LBL);

		this.setLayout(new BorderLayout());

		scrollPane = new JScrollPane();

		list = new JList<>();
		ListModel<SelectableComponent> objectModel = createListModel();

		list.setModel(objectModel);
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
	private ListModel<SelectableComponent> createListModel() {
		DefaultListModel<SelectableComponent> model = new DefaultListModel<>();

		if (this.getCellModel().hasCell()) {

			// Every cell has a nucleus
			Nucleus n = getCellModel().getCell().getPrimaryNucleus();
			model.addElement(new SelectableComponent(CellularComponent.NUCLEUS, n));

			if (getCellModel().getCell().hasCytoplasm()) {
				ICytoplasm cyto = getCellModel().getCell().getCytoplasm();
				model.addElement(new SelectableComponent(CellularComponent.CYTOPLASM, cyto));
			}

			ISignalCollection signalCollection = n.getSignalCollection();

			// Add signals groups present
			for (UUID signalGroupId : signalCollection.getSignalGroupIds()) {
				String signalGroupName = activeDataset().getCollection().getSignalGroup(signalGroupId).get()
						.getGroupName();
				if (signalCollection.hasSignal(signalGroupId)) {

					// Since all we want is a single component within the
					// collection, just take the first signal
					INuclearSignal signal = signalCollection.getSignals(signalGroupId).get(0);
					model.addElement(new SelectableComponent(signalGroupName, signal));
				}
			}
		}
		return model;
	}

	@Override
	public void update() {
//		LOGGER.fine("Removing selection listeners");
		for (var l : list.getListSelectionListeners())
			list.removeListSelectionListener(l);

		ListModel<SelectableComponent> model = createListModel();
		list.setModel(model);

		// When the dataset changes, we need to set the selected index to match the
		// previous selection
		if (this.getCellModel().hasCell()) {

			// Check if the new cell has the same component as the last
			int selectedIndex = 0;
			for (int i = 0; i < model.getSize(); i++) {
				SelectableComponent tableCell = list.getModel().getElementAt(i);
				if (tableCell.toString().equals(prevComponent)) {
					selectedIndex = i;
				}
			}
			list.setSelectedIndex(selectedIndex);
			prevComponent = list.getModel().getElementAt(selectedIndex).toString(); // set
																					// the
																					// new
																					// component
																					// string
			this.getCellModel().setComponent(getSelectedComponent());
			list.setEnabled(true);
			list.addListSelectionListener(this);
		} else {
			list.setEnabled(false);
		}

	}

	private SelectableComponent getSelectedRow() {
		int row = list.getSelectedIndex();
		if (row >= 0) { // -1 if nothing selected
			return list.getModel().getElementAt(row);
		}
		return null;
	}

	private CellularComponent getSelectedComponent() {
		int row = list.getSelectedIndex();

		if (row >= 0) { // -1 if nothing selected
			return getSelectedRow().component;
		}
		return null;
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting())
			return;

		SelectableComponent cell = getSelectedRow();
		if (cell != null) {
			prevComponent = cell.name; // set the new component string
			this.getCellModel().setComponent(cell.component());
		}

	}

	@Override
	public void setLoading() {
	}

}
