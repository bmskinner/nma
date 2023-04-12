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
package com.bmskinner.nma.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.gui.components.panels.WrappedLabel;

@SuppressWarnings("serial")
public class DatasetArithmeticSetupDialog extends SettingsDialog {
	private JComboBox<BooleanOperation> operatorBox;
	private WrappedLabel operatorDescription = new WrappedLabel(
			BooleanOperation.AND.getDescription());

	public enum BooleanOperation {
		AND("Cells are present in all datasets"),
		OR("Cells are in any dataset (this merges the datasets)"),
		NOT("Cells are in the first dataset,but not any other datasets"),
		XOR("Cells are in one dataset, but not shared with another dataset");

		private String description;

		private BooleanOperation(String description) {
			this.description = description;
		}

		public String getDescription() {
			return this.description;
		}
	}

	public DatasetArithmeticSetupDialog(List<IAnalysisDataset> list) {
		super(true);

		this.setTitle("Dataset boolean options");
		this.setLocationRelativeTo(null);
		createGUI();
		this.pack();
		this.setVisible(true);
	}

	public BooleanOperation getOperation() {
		return (BooleanOperation) operatorBox.getSelectedItem();
	}

	private void createGUI() {

		setLayout(new BorderLayout());

		JPanel panel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		panel.setLayout(layout);
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));

		List<JLabel> labels = new ArrayList<>();
		List<Component> fields = new ArrayList<>();

		operatorBox = new JComboBox<>(BooleanOperation.values());
		operatorBox.setSelectedItem(BooleanOperation.AND);
		operatorBox.addActionListener(
				e -> {
					operatorDescription.setText(getOperation().getDescription());
					operatorDescription.setPreferredSize(null);
					this.pack();

				});

		JPanel operatorPanel = new JPanel(new FlowLayout());
		operatorPanel.add(operatorBox);
		labels.add(new JLabel("Operation"));
		fields.add(operatorPanel);

		labels.add(new JLabel("Description"));
		fields.add(operatorDescription);

		this.addLabelTextRows(labels, fields, layout, panel);

		JPanel header = new JPanel(new FlowLayout());
		header.add(new JLabel("Create a new dataset using the following rule:"));

		this.add(header, BorderLayout.NORTH);
		this.add(panel, BorderLayout.CENTER);

		this.add(createFooter(), BorderLayout.SOUTH);

	}

}
