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
		super(mw.getProgramLogger(), mw, true);

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
