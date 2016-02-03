/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package gui.tabs.signals;

import gui.components.ExportableTable;
import gui.tabs.DetailPanel;

import java.awt.BorderLayout;
import java.util.logging.Logger;

import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import charting.datasets.NuclearSignalDatasetCreator;


@SuppressWarnings("serial")
public class SignalsAnalysisPanel extends DetailPanel {

	private ExportableTable table;			// table for analysis parameters
	private JScrollPane     scrollPane;


	public SignalsAnalysisPanel(Logger logger){
		super(logger);
		this.setLayout(new BorderLayout());

		table  = new ExportableTable(new DefaultTableModel());
		table.setAutoCreateColumnsFromModel(false);
		table.setEnabled(false);
		scrollPane = new JScrollPane(table);
		this.add(scrollPane, BorderLayout.CENTER);
	}

	@Override
	protected void updateSingle() throws Exception {
		TableModel model = NuclearSignalDatasetCreator.createSignalDetectionParametersTable(getDatasets());
		table.setModel(model);
		table.createDefaultColumnsFromModel();
		
	}

	@Override
	protected void updateMultiple() throws Exception {
		updateSingle();
	}

	@Override
	protected void updateNull() throws Exception {
		TableModel model = NuclearSignalDatasetCreator.createSignalDetectionParametersTable(null);
		table.setModel(model);
		table.createDefaultColumnsFromModel();
		
	}

}
