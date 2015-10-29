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

import gui.DatasetEvent.DatasetMethod;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import analysis.AnalysisDataset;

@SuppressWarnings("serial")
public class MergesDetailPanel extends DetailPanel {
	
	private JTable		mergeSources;
	private JButton		getSourceButton = new JButton("Recover source");
	private AnalysisDataset activeDataset;
	private Logger programLogger;
	
	public MergesDetailPanel(Logger programLogger){
		super(programLogger);
		this.setLayout(new BorderLayout());
		mergeSources = new JTable(makeBlankTable()){
			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {
			    return false;
			}
		};
		mergeSources.setEnabled(true);
		mergeSources.setCellSelectionEnabled(false);
		mergeSources.setColumnSelectionAllowed(false);
		mergeSources.setRowSelectionAllowed(true);
		
		this.add(mergeSources, BorderLayout.CENTER);
		this.add(mergeSources.getTableHeader(), BorderLayout.NORTH);
		
		getSourceButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				
				// get the dataset selected
				List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
				String name = (String) mergeSources.getModel().getValueAt(mergeSources.getSelectedRow(), 0);
				
				// get the dataset with the selected name
				for( UUID id : activeDataset.getMergeSources()){
					AnalysisDataset mergeSource = activeDataset.getMergeSource(id);
					if(mergeSource.getName().equals(name)){
						list.add(mergeSource);
					}
				}
				fireDatasetEvent(DatasetMethod.EXTRACT_SOURCE, list);
//				fireSignalChangeEvent("ExtractSource_"+name);

			}
		});
		getSourceButton.setVisible(false);
		this.add(getSourceButton, BorderLayout.SOUTH);
	}
	
	public void update(List<AnalysisDataset> list){
		programLogger.log(Level.FINEST, "Updating merges panel");
		getSourceButton.setVisible(false);
		if(list.size()==1){
			AnalysisDataset dataset = list.get(0);
			activeDataset = dataset;

			if(dataset.hasMergeSources()){
				
				DefaultTableModel model = new DefaultTableModel();

				Vector<Object> names 	= new Vector<Object>();
				Vector<Object> nuclei 	= new Vector<Object>();

				for( UUID id : dataset.getMergeSources()){
					AnalysisDataset mergeSource = dataset.getMergeSource(id);
					names.add(mergeSource.getName());
					nuclei.add(mergeSource.getCollection().getNucleusCount());
				}
				model.addColumn("Merge source", names);
				model.addColumn("Nuclei", nuclei);

				mergeSources.setModel(model);
				getSourceButton.setVisible(true);
				
			} else {
				try{
				mergeSources.setModel(makeBlankTable());
				} catch (Exception e){
//					TODO: fix error
				}
			}
		} else { // more than one dataset selected
			mergeSources.setModel(makeBlankTable());
		}
		programLogger.log(Level.FINEST, "Updated merges panel");
		
	}
	
	private DefaultTableModel makeBlankTable(){
		DefaultTableModel model = new DefaultTableModel();

		Vector<Object> names 	= new Vector<Object>();
		Vector<Object> nuclei 	= new Vector<Object>();

		names.add("No merge sources");
		nuclei.add("");


		model.addColumn("Merge source", names);
		model.addColumn("Nuclei", nuclei);
		return model;
	}
}
