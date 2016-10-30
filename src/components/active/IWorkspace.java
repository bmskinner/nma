package components.active;

import java.io.File;
import java.util.Set;

import analysis.IAnalysisDataset;

public interface IWorkspace {
	
	void add(IAnalysisDataset d);
	
	void add(File f);
	
	void remove(IAnalysisDataset d);
	
	void remove(File f);
	
	void save();
	
	Set<File> getFiles();
	
	void setSaveFile(File f);
	
	File getSaveFile();

}
