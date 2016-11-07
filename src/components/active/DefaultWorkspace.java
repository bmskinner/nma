package components.active;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import analysis.IAnalysisDataset;

/**
 * This is a grouping of open AnalysisDatasets,
 * which can act as a shortcut to opening a lot
 * of nmd files in one go. 
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultWorkspace implements IWorkspace {

	Set<File> datasets = new HashSet<File>();
	
	File saveFile = null;
	
	public DefaultWorkspace(File f){
		this.saveFile = f;
	}

	@Override
	public void add(IAnalysisDataset d) {
		if(d.isRoot()){
			datasets.add(d.getSavePath());
		}
		
		//TODO: warn or get root
	}
	
	@Override
	public void add(File f) {
		datasets.add(f);
	}

	@Override
	public void remove(IAnalysisDataset d) {
		datasets.remove(d.getSavePath());
	}
	
	@Override
	public void remove(File f) {
		datasets.remove(f);
	}

	@Override
	public Set<File> getFiles() {
		return datasets;
	}

	@Override
	public void setSaveFile(File f) {
		saveFile = f;
		
	}

	@Override
	public File getSaveFile() {
		return saveFile;
	}
	
	@Override
	public void save() {
		// TODO Auto-generated method stub
		
	}
	
	
	
}
