package com.bmskinner.nuclear_morphology.gui.components.panels;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.analysis.signals.ShellRandomDistributionCreator;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalManager;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.UnavailableSignalGroupException;



@SuppressWarnings("serial")
public class SignalGroupSelectionPanel extends EnumeratedOptionsPanel {
	
	
	JComboBox<SignalIDToGroup> box;
	
	
	public SignalGroupSelectionPanel(IAnalysisDataset d) {
		
		
		List<SignalIDToGroup> list = getGroups(d);
		
		

		box = new JComboBox<SignalIDToGroup>(list.toArray( new SignalIDToGroup[0] ));
		
		SignalManager m =  d.getCollection().getSignalManager();
		if( m.hasSignals()){
			box.setSelectedIndex(0);
		}
		
		box.addActionListener(this);
		this.add(box);
	}
	
	public void setDataset(IAnalysisDataset d){
		
		SignalManager m =  d.getCollection().getSignalManager();
		if( ! m.hasSignals()){
			this.setEnabled(false);
			return;
		}
		
		List<SignalIDToGroup> list = getGroups(d);
		
		ComboBoxModel<SignalIDToGroup> model = new DefaultComboBoxModel<SignalIDToGroup>(list.toArray( new SignalIDToGroup[0] ));

		box.setModel(model);
		box.setSelectedIndex(0);

	}
	
	public boolean hasSelection(){
		return box.getSelectedItem() != null;
	}
	
	public ISignalGroup getSelectedGroup(){
		SignalIDToGroup temp = (SignalIDToGroup) box.getSelectedItem();
		return temp.getGroup();
	}
	
	public UUID getSelectedID(){
		SignalIDToGroup temp = (SignalIDToGroup) box.getSelectedItem();
		return temp.getId();
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
	
	
	private List<SignalIDToGroup> getGroups(IAnalysisDataset d){
		SignalManager m =  d.getCollection().getSignalManager();
		Set<UUID> signalGroups = m.getSignalGroupIDs();
		List<SignalIDToGroup> list = new ArrayList<SignalIDToGroup>();
		for(UUID id : signalGroups){
			
			if(id.equals(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID)){
        		continue;
        	}
			
			try {
				list.add(new SignalIDToGroup(id, d.getCollection().getSignalGroup(id)));
			} catch (UnavailableSignalGroupException e) {
				fine("Cannot add signal group to panel", e);
			}
		}
		return list;
	}

	private class SignalIDToGroup {
		
		final private UUID id;
		final private ISignalGroup group;
		
		public SignalIDToGroup(final UUID id, final ISignalGroup group){
			this.id = id;
			this.group = group;
		}
				
		public UUID getId() {
			return id;
		}

		public ISignalGroup getGroup() {
			return group;
		}



		public String toString(){
			return group.getGroupName();
		}
	
	}

}
