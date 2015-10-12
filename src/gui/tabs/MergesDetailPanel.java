package gui.tabs;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import analysis.AnalysisDataset;

@SuppressWarnings("serial")
public class MergesDetailPanel extends DetailPanel {
	
	private JTable		mergeSources;
	private JButton		getSourceButton = new JButton("Recover source");
	
	public MergesDetailPanel(){
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
				
				// get the dataset name selected
				String name = (String) mergeSources.getModel().getValueAt(mergeSources.getSelectedRow(), 0);

				fireSignalChangeEvent("ExtractSource_"+name);

			}
		});
		getSourceButton.setVisible(false);
		this.add(getSourceButton, BorderLayout.SOUTH);
	}
	
	public void update(List<AnalysisDataset> list){
		getSourceButton.setVisible(false);
		if(list.size()==1){
			AnalysisDataset dataset = list.get(0);

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
