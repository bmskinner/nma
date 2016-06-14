package gui.components.panels;

import java.util.List;

import javax.swing.JComboBox;

import analysis.AnalysisDataset;

/**
 * This creates a panel with a drop down list of datasets, specified in the constructor.
 * Add an actionlistener to the panel, and access the selected dataset via the getSelectedDataset()
 * method.
 *
 */
@SuppressWarnings("serial")
public class DatasetSelectionPanel extends EnumeratedOptionsPanel {
	
	JComboBox<AnalysisDataset> box;
	
	public DatasetSelectionPanel(List<AnalysisDataset> datasets){
		box = new JComboBox<AnalysisDataset>();
		for(AnalysisDataset d : datasets){
			box.addItem(d);
		}
		
		box.setSelectedItem(datasets.get(0));
		
		box.addActionListener(this);
		
		this.add(box);
	}
	
	public AnalysisDataset getSelectedDataset(){
		return (AnalysisDataset) box.getSelectedItem();
	}
	
	@Override
	public void setEnabled(boolean b){
		super.setEnabled(b);
		box.setEnabled(b);
	}
	
	public void setSelectionIndex(int i){
		
		if(i>box.getItemCount()-1){
			return;
		}
		if(i<0){
			return;
		}
		box.setSelectedItem(i);
	}
	
	public void setSelectedDataset(AnalysisDataset d){
		
		box.setSelectedItem(d);
	}
	
	

}
