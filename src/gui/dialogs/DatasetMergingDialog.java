package gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import analysis.AnalysisDataset;
import gui.LoadingIconDialog;
import gui.components.panels.DatasetSelectionPanel;
import gui.components.panels.SignalGroupSelectionPanel;

/**
 * Allows options to be set when merging datasets. E.g. which
 * signal groups are equivalent.
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class DatasetMergingDialog extends LoadingIconDialog implements ActionListener{
		
	private List<AnalysisDataset> datasets;
	
	private DatasetSelectionPanel datasetBoxOne;
	private DatasetSelectionPanel datasetBoxTwo;
	
	private SignalGroupSelectionPanel signalBoxOne;
	private SignalGroupSelectionPanel signalBoxTwo;
	
	private JTable matchTable;
	
	private JButton mergeButton;
	private JButton setEqualButton;
	
	// Store the ids of signal groups that should be merged
	private Map<UUID, Set<UUID>> pairedSignalGroups = new HashMap<UUID, Set<UUID>>();
	
	
	
	public DatasetMergingDialog(List<AnalysisDataset> datasets){
		this.datasets = datasets;
		createUI();

		this.setModal(true);
		this.pack();
		centerOnScreen();
		this.setVisible(true);
		finest("Created dataset merging dialog");
	}
	
	public Map<UUID, Set<UUID>> getPairedSignalGroups(){
		return this.pairedSignalGroups;
	}
	
	private void createUI(){
		
		this.setLayout(new BorderLayout());
		this.setTitle("Merge datasets");
		
		
		JPanel header = createHeader();
		this.add(header, BorderLayout.NORTH);
		
		JScrollPane scrollPanel = new JScrollPane();
		matchTable = new JTable(createTableModel());
		scrollPanel.setViewportView(matchTable);
		this.add(scrollPanel, BorderLayout.CENTER);
		
		
		JPanel footer = createFooter();
		this.add(footer, BorderLayout.SOUTH);
	}
	
	private JPanel createFooter(){
		JPanel panel = new JPanel(new FlowLayout());
		
		mergeButton = new JButton("Merge");
		mergeButton.addActionListener(this);
		
		panel.add(mergeButton);
		return panel;
	}
	
	private JPanel createHeader(){
		JPanel headerPanel = new JPanel();
		headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
		
		JPanel upperPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JPanel lowerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		
		upperPanel.add(new JLabel("Datasets have signals. Choose which signal groups to merge"));
		
		datasetBoxOne = new DatasetSelectionPanel(datasets);
		datasetBoxTwo = new DatasetSelectionPanel(datasets);
		
		datasetBoxOne.setSelectedDataset(datasets.get(0));
		datasetBoxTwo.setSelectedDataset(datasets.get(1));
		
		datasetBoxOne.addActionListener(this);
		datasetBoxTwo.addActionListener(this);
		
		signalBoxOne = new SignalGroupSelectionPanel(datasets.get(0));
		signalBoxTwo = new SignalGroupSelectionPanel(datasets.get(1));
		
		setEqualButton = new JButton("Set equal");
		setEqualButton.addActionListener(this);
		
//		upperPanel.add(new JLabel("Source dataset"));
		lowerPanel.add(datasetBoxOne);
		lowerPanel.add(signalBoxOne);
		lowerPanel.add(setEqualButton);
		lowerPanel.add(signalBoxTwo);
		lowerPanel.add(datasetBoxTwo);

		
		headerPanel.add(upperPanel);
		headerPanel.add(lowerPanel);
		return headerPanel;
	}
	
	private TableModel createTableModel(){
		
		DefaultTableModel model = new DefaultTableModel();
		
		Object[] columns = { "Signal group", "Signal group" };
		
		model.setColumnIdentifiers(columns);
		
		return model;
		
	}
	
	private void updateTable(){
		
		DefaultTableModel model = new DefaultTableModel();
		
		Object[] columns = { "Signal group", "Signal group" };
		
		model.setColumnIdentifiers(columns);
		
		for(UUID id1 : pairedSignalGroups.keySet()){
			String col1 = "";
			for(AnalysisDataset d : datasets){
				if(d.getCollection().getSignalManager().hasSignals(id1)){
					col1 = d.getName()+" : "+d.getCollection().getSignalGroup(id1).getGroupName();
				}
			}
			Set<UUID> idList = pairedSignalGroups.get(id1);
			for( UUID id2 :idList){
				String col2 =  "";
				for(AnalysisDataset d : datasets){
					if(d.getCollection().getSignalManager().hasSignals(id2)){
						col2 = d.getName()+" : "+d.getCollection().getSignalGroup(id2).getGroupName();
					}
				}
				
				Object[] row = { col1, col2};
				model.addRow(row);
			}
			
			
		}
		
		
		matchTable.setModel(model);
	}



	@Override
	public void actionPerformed(ActionEvent e) {
		
		AnalysisDataset d1 = datasetBoxOne.getSelectedDataset();
		AnalysisDataset d2 = datasetBoxTwo.getSelectedDataset();
		
		UUID id1 = signalBoxOne.getSelectedID();
		UUID id2 = signalBoxTwo.getSelectedID();
		
		if(e.getSource()==datasetBoxOne){
			signalBoxOne.setDataset(d1);
		}
		if(e.getSource()==datasetBoxTwo){
			signalBoxTwo.setDataset(d2);
		}
		
		if(e.getSource()==setEqualButton){
			Set<UUID> idSet = pairedSignalGroups.get(id1);
			if(idSet==null){
				idSet = new HashSet<UUID>();
				pairedSignalGroups.put(id1, idSet);
			}
			idSet.add(id2);
			updateTable();
		}
		
		if(e.getSource()==mergeButton){
			log("Merging datasets");
			this.setVisible(false);
		}
		
	}

}
