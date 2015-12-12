package gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
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

public class DatasetArithmeticSetupDialog extends SettingsDialog{
	
	
	JComboBox<AnalysisDataset> boxOne;
	JComboBox<AnalysisDataset> boxTwo;
	JComboBox<DatasetArithmeticOperation> operatorBox;
	
	public enum DatasetArithmeticOperation {
		AND, OR, NOT, XOR
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
		
		labels.add(new JLabel("Dataset one"));
		fields.add(boxOne);
		labels.add(new JLabel("Operation"));
		fields.add(operatorBox);
		labels.add(new JLabel("Dataset two"));
		fields.add(boxTwo);
		
		this.addLabelTextRows(labels, fields, layout, panel);
		
		this.add(panel, BorderLayout.CENTER);
		
		this.add(createFooter(), BorderLayout.SOUTH);
		
		
	}
	
	

}
