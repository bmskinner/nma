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
package com.bmskinner.nma.gui.tabs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import com.bmskinner.nma.gui.components.panels.WrappedLabel;
import com.bmskinner.nma.visualisation.tables.AbstractTableCreator;

@SuppressWarnings("serial")
public abstract class AbstractPairwiseDetailPanel extends TableDetailPanel {

	protected JPanel tablePanel;
	protected JScrollPane scrollPane = new JScrollPane();

	public AbstractPairwiseDetailPanel() {
		super();

		this.setLayout(new BorderLayout());

		tablePanel = createTablePanel();
		scrollPane.setViewportView(tablePanel);

		this.add(createInfoPanel(), BorderLayout.NORTH);
		this.add(scrollPane, BorderLayout.CENTER);
	}

	@Override
	public void setLoading() {
		super.setLoading();
		for (Component c : this.getComponents()) {
			if (c instanceof JTable) {

				((JTable) c).setModel(AbstractTableCreator.createLoadingTable());
			}
		}

	}

	/**
	 * Create the info panel
	 * 
	 * @return
	 */
	protected JPanel createInfoPanel() {
		JPanel infoPanel = new JPanel();
		infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

		String infoString = "Pairwise comparisons between populations using Mann-Whitney U test (aka Wilcoxon rank sum test)\n"
				+ "Above the diagonal: Mann-Whitney U statistics\n" + "Below the diagonal: p-values\n"
				+ "Significant values at 5% and 1% levels after Bonferroni correction are highlighted in yellow and green";

		infoPanel.add(new WrappedLabel(infoString));
		return infoPanel;
	}

	/**
	 * Create a new panel to hold tables
	 * 
	 * @return
	 */
	protected JPanel createTablePanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		Dimension minSize = new Dimension(10, 10);
		Dimension prefSize = new Dimension(10, 10);
		Dimension maxSize = new Dimension(Short.MAX_VALUE, 10);
		panel.add(new Box.Filler(minSize, prefSize, maxSize));
		return panel;
	}

	/**
	 * Prepare a wilcoxon table
	 * 
	 * @param panel the JPanel to add the table to
	 * @param table the table to add
	 * @param model the model to provide
	 * @param label the label for the table
	 */
	protected void addWilconxonTable(JPanel panel, JTable table, String label) {
		Dimension minSize = new Dimension(10, 10);
		Dimension prefSize = new Dimension(10, 10);
		Dimension maxSize = new Dimension(Short.MAX_VALUE, 10);
		panel.add(new Box.Filler(minSize, prefSize, maxSize));
		panel.add(new JLabel(label));
		panel.add(table);
		table.setEnabled(false);
	}
}
