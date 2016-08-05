package gui;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import analysis.AnalysisDataset;

/**
 * Track the open datasets in the program
 * @author bms41
 *
 */
public final class DatasetListManager implements WindowListener {
	
	private static DatasetListManager instance = null;
	
	private final Set<AnalysisDataset> list = new HashSet<AnalysisDataset>();
	
	protected DatasetListManager(){}
	
	/**
	 * Fetch an instance of the factory
	 * @return
	 */
	public static DatasetListManager getInstance(){
		if(instance==null){
			instance = new DatasetListManager();
		}
		return instance;
	}
	
	public Set<AnalysisDataset> getRootDatasets(){
		return list;
	}
	
	public boolean hasDatasets(){
		return list.size()>0;
	}
	
	
	public Set<AnalysisDataset> getAllDatasets(){
		
		Set<AnalysisDataset> result = new HashSet<AnalysisDataset>();
		for(AnalysisDataset d : list){
			result.addAll(d.getAllChildDatasets());
		}
		return result;
		
	}
	
	/**
	 * Get the dataset with the given id, or null
	 * @param id
	 * @return
	 */
	public boolean hasDataset(UUID id){
		for(AnalysisDataset d : list){
			if(d.getUUID().equals(id)){
				return true;
			}
			
			for(AnalysisDataset child : d.getAllChildDatasets()){
				if(child.getUUID().equals(id)){
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Get the dataset with the given id, or null
	 * @param id
	 * @return
	 */
	public AnalysisDataset getDataset(UUID id){
		for(AnalysisDataset d : list){
			if(d.getUUID().equals(id)){
				return d;
			}
			
			for(AnalysisDataset child : d.getAllChildDatasets()){
				if(child.getUUID().equals(id)){
					return child;
				}
			}
		}
		return null;
	}
	
	public void addDataset(AnalysisDataset d){
		list.add(d);
	}
	
	public void removeDataset(AnalysisDataset d){
		list.remove(d);
	}
	
	public int count(){
		return list.size();
	}
	
	public void clear(){
		list.clear();
	}

	@Override
	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosed(WindowEvent arg0) {
		clear();
		
	}

	@Override
	public void windowClosing(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowIconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

}
