/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
package gui.tabs;

import java.awt.BorderLayout;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;

import charting.DefaultTableOptions;
import charting.DefaultTableOptions.TableType;
import charting.TableOptions;
import charting.datasets.NucleusTableDatasetCreator;
import gui.components.ExportableTable;

public class PairwiseVennDetailPanel extends DetailPanel {


	private static final long serialVersionUID = 1L;
	
	private JPanel mainPanel = new JPanel();

	private ExportableTable pairwiseVennTable;

	public PairwiseVennDetailPanel(Logger programLogger) {
		super(programLogger);
		this.setLayout(new BorderLayout());
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
				
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(mainPanel);
		
		this.add(scrollPane, BorderLayout.CENTER);
		
		
		JPanel pairwisePanel = new JPanel(new BorderLayout());
		
		pairwiseVennTable = new ExportableTable(NucleusTableDatasetCreator.createPairwiseVennTable(null));
		
		pairwisePanel.add(pairwiseVennTable, BorderLayout.CENTER);
		pairwisePanel.add(pairwiseVennTable.getTableHeader(), BorderLayout.NORTH);
		mainPanel.add(pairwisePanel);
		pairwiseVennTable.setEnabled(false);
		
	}
	
	/**
	 * Update the venn panel with data from the given datasets
	 * @param getDatasets() the datasets
	 */
	@Override
	public void updateDetail(){
		
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				updatePairwiseVennTable();
				setUpdating(false);
			}});
	}

	private void updatePairwiseVennTable(){
		programLogger.log(Level.FINE, "Updating pairwise venn table");


		// format the numbers and make into a tablemodel
		TableModel model = NucleusTableDatasetCreator.createPairwiseVennTable(null);

		if(!getDatasets().isEmpty() && getDatasets()!=null){

			TableOptions options = new DefaultTableOptions(getDatasets(), TableType.PAIRWISE_VENN);
			if(getTableCache().hasTable(options)){
				model = getTableCache().getTable(options);
			} else {
				model = NucleusTableDatasetCreator.createPairwiseVennTable(getDatasets());
				getTableCache().addTable(options, model);
			}

		}
		pairwiseVennTable.setModel(model);

		programLogger.log(Level.FINEST, "Updated pairwise venn panel");

	}
}

