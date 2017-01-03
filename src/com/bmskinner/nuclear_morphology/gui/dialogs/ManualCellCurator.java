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
package com.bmskinner.nuclear_morphology.gui.dialogs;

import com.bmskinner.nuclear_morphology.gui.LoadingIconDialog;

@SuppressWarnings("serial")
public class ManualCellCurator extends LoadingIconDialog {
//	
//	private JButton keepButton = new JButton("Keep");
//	private JButton rejectButton = new JButton("Reject");
//	
//	private AnnotatedNucleusPanel panel; 
//	
//	private AnalysisDataset dataset = null;
//	
//	private List<UUID> idsToKeep = new ArrayList<UUID>();
//	private int cellIndex = 0;
//
//	
//	public ManualCellCurator(AnalysisDataset dataset){
//		super();
//		
//		this.dataset = dataset;
//		this.setLocationRelativeTo(null);
//		
////		createUI();
//		
//		this.pack();
//		this.setModal(true);
//		this.setVisible(true);
//	}
//	
//	public List<UUID> getIDsToKeep(){
//		return this.idsToKeep;
//	}
//	
////	private void createUI(){
////		this.setLayout(new BorderLayout());
////		this.setSize(500,500);
////		this.panel = new AnnotatedNucleusPanel();
////		this.add(panel, BorderLayout.CENTER);
////		updateCell(dataset.getCollection().getCells().get(cellIndex));
////		
////		JPanel buttonPanel = makeButtonPanel();
////		this.add(buttonPanel, BorderLayout.SOUTH);
////		
////	}
//	
//	private void updateCell(Cell cell){
//		try {
//			int totalCells = dataset.getCollection().cellCount();
//			int cellNumber = cellIndex+1;
//			this.setTitle(cell.getNucleus().getNameAndNumber()+": Cell "+cellNumber+" of "+totalCells);
//			panel.updateCell(cell);
//
//		} catch (Exception e) {
//			log(Level.SEVERE, "Error updating cell", e);
//		}
//	}
//	
//	private JPanel makeButtonPanel(){
//		JPanel panel = new JPanel(new FlowLayout());
//		
//		panel.add(keepButton);
//		panel.add(rejectButton);
//
//		keepButton.addMouseListener(new MouseAdapter() {
//			@Override
//			public void mouseClicked(MouseEvent arg0) {
//				
//				if(cellIndex==dataset.getCollection().cellCount()-1){
//					// last cell
//					idsToKeep.add(dataset.getCollection().getCells().get(cellIndex).getId());
//					setVisible(false);
//				} else {
//
//					log(Level.FINEST, "Keeping cell");
//					idsToKeep.add(dataset.getCollection().getCells().get(cellIndex++).getId());
//					updateCell(dataset.getCollection().getCells().get(cellIndex));
//				}
//
//			}
//		});
//		
//		rejectButton.addMouseListener(new MouseAdapter() {
//			@Override
//			public void mouseClicked(MouseEvent arg0) {
//				if(cellIndex==dataset.getCollection().cellCount()-1){
//					// last cell
//					setVisible(false);
//				} else {
//					log(Level.FINEST, "Rejecting cell");
//					updateCell(dataset.getCollection().getCells().get(++cellIndex));
//				}
//
//			}
//		});
//		
//		
//		return panel;
//	}
//
}
