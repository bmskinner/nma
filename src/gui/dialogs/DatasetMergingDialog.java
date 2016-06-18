package gui.dialogs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
	
	// Store the ids of signal groups that should be merged
	private Map<UUID, UUID> pairedSignalGroups = new HashMap<UUID, UUID>();
	
	
	
	public DatasetMergingDialog(List<AnalysisDataset> datasets){
		this.datasets = datasets;
		createUI();
		this.setModal(false);
		this.pack();
		this.setVisible(true);
		finest("Created dataset merging dialog");
	}
	
	private void createUI(){
		
		
	}



	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

}
