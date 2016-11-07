package components.active;

import java.io.File;
import java.util.Set;

import analysis.IAnalysisDataset;

/**
 * A workspace is a collection of nmd files that can be
 * reopened together. This interface mey be extended depending on
 * how useful workspaces turn out to be. 
 * @author ben
 * @since 1.13.3
 *
 */
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
