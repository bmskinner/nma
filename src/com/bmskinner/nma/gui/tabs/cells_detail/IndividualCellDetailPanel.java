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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.EmptyBorder;

import com.bmskinner.nma.gui.tabs.DetailPanel;
import com.bmskinner.nma.logging.Loggable;

@SuppressWarnings("serial")
public class IndividualCellDetailPanel extends DetailPanel {

	private static final Logger LOGGER = Logger
			.getLogger(IndividualCellDetailPanel.class.getName());

	private static final String PANEL_TITLE_LBL = "Cells";
	private static final String PANEL_DESC_LBL = "Display individual cells and their landmarks";

	/** Cells in the active dataset */
	private CellsListPanel cellsListPanel;

	/** Choose image channels to display */
	private ComponentListPanel signalListPanel;

	/** Track the cell on display */
	private CellViewModel model = new CellViewModel(null, null);

	public IndividualCellDetailPanel() {
		super(PANEL_TITLE_LBL, PANEL_DESC_LBL);

		try {

			createSubPanels();

			this.setLayout(new BorderLayout());
			JPanel westPanel = createCellandSignalListPanels();

			CellOutlinePanel outlinePanel = new CellOutlinePanel(model); // the outline of the cell
			model.addView(outlinePanel);

			JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
			sp.setLeftComponent(westPanel);
			sp.setRightComponent(outlinePanel);
			sp.setResizeWeight(0.25);
			add(sp, BorderLayout.CENTER);

		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error creating cell detail panel");
			LOGGER.log(Loggable.STACK, "Error creating cell detail panel", e);
		}
	}

	private void createSubPanels() {
		// no actions

	}

	private JPanel createCellandSignalListPanels() {
		JPanel panel = new JPanel(new GridBagLayout());

		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.BOTH;
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.gridheight = 2;
		constraints.gridwidth = 1;
		constraints.weightx = 0.5;
		constraints.weighty = 0.6;
		constraints.anchor = GridBagConstraints.CENTER;

		cellsListPanel = new CellsListPanel(model);
		model.addView(cellsListPanel);
		cellsListPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		panel.add(cellsListPanel, constraints);

		constraints.gridx = 0;
		constraints.gridy = 2;
		constraints.gridheight = 2;
		constraints.gridwidth = 1;
		constraints.weightx = 0.5;
		constraints.weighty = 0.4;
		signalListPanel = new ComponentListPanel(model);
		model.addView(signalListPanel);
		signalListPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		panel.add(signalListPanel, constraints);

		return panel;
	}

	@Override
	protected void updateSingle() {

		if (model.hasCell() && !activeDataset().getCollection().containsExact(model.getCell())) {
			model.setCell(null);
		}
	}

	@Override
	protected void updateMultiple() {
		updateNull();
	}

	@Override
	protected void updateNull() {
		model.setCell(null);
		model.setComponent(null);
	}

}
