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
package gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import analysis.AnalysisDataset;
import gui.MainWindow;

@SuppressWarnings("serial")
public class DatasetArithmeticSetupDialog extends SettingsDialog implements ActionListener{
	
	
	JComboBox<AnalysisDataset> boxOne;
	JComboBox<AnalysisDataset> boxTwo;
	JComboBox<DatasetArithmeticOperation> operatorBox;
	JLabel operatorDescription = new JLabel(DatasetArithmeticOperation.AND.getDescription());
	
	public enum DatasetArithmeticOperation {
		AND ("Cells are present in both datasets"),
		OR ("Cells are in either dataset (this merges the datasets"), 
		NOT ("Cells are in dataset one, but not dataset two"), 
		XOR ("Cells are in one or other dataset, but not both datasets");
		
		private String description;
		
		private DatasetArithmeticOperation(String description){
			this.description = description;
		}
		
		public String getDescription(){
			return this.description;
		}
	}

	public DatasetArithmeticSetupDialog(AnalysisDataset selected, List<AnalysisDataset> list, MainWindow mw) {
		super( mw, true);

		this.setTitle("Dataset arithmetic options");
		setSize(450, 300);
		this.setLocationRelativeTo(null);
		createGUI(selected, list);
		this.pack();
		this.setVisible(true);
	}
	
	public AnalysisDataset getDatasetOne(){
		return (AnalysisDataset) boxOne.getSelectedItem();
	}
	
	public AnalysisDataset getDatasetTwo(){
		return (AnalysisDataset) boxTwo.getSelectedItem();
	}
	
	public DatasetArithmeticOperation getOperation(){
		return (DatasetArithmeticOperation) operatorBox.getSelectedItem();
	}

	private void createGUI(AnalysisDataset selected, List<AnalysisDataset> list) {
		
		setLayout(new BorderLayout());

		JPanel panel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		panel.setLayout(layout);
		
		List<JLabel> labels = new ArrayList<JLabel>();
		List<Component> fields = new ArrayList<Component>();
		
		boxOne = new JComboBox<AnalysisDataset>();
		boxTwo = new JComboBox<AnalysisDataset>();
		for(AnalysisDataset d : list){
			boxOne.addItem(d);
			boxTwo.addItem(d);
		}
		boxOne.setSelectedItem(selected);
		
		operatorBox = new JComboBox<DatasetArithmeticOperation>(DatasetArithmeticOperation.values());
		operatorBox.setSelectedItem(DatasetArithmeticOperation.AND);
		operatorBox.addActionListener(this);
		
		labels.add(new JLabel("Dataset one"));
		fields.add(boxOne);
		labels.add(new JLabel("Operation"));
		fields.add(operatorBox);
		
		labels.add(new JLabel("Description"));
		fields.add(operatorDescription);
		
		labels.add(new JLabel("Dataset two"));
		fields.add(boxTwo);
		
		this.addLabelTextRows(labels, fields, layout, panel);
		
		this.add(panel, BorderLayout.CENTER);
		
		this.add(createFooter(), BorderLayout.SOUTH);
		
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		operatorDescription.setText(getOperation().getDescription());
		
	}
	
	

}
