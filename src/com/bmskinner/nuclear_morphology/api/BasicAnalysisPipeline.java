package com.bmskinner.nuclear_morphology.api;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.nucleus.NucleusDetectionMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetProfilingMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.io.DatasetExportMethod;
import com.bmskinner.nuclear_morphology.io.DatasetStatsExporter;
import com.bmskinner.nuclear_morphology.io.Io;

/**
 * A test pipeline to be run from the command line. Analyse a folder with default
 * settings, save the nmd, and export a nuclear stats file
 * @author bms41
 * @since 1.14.0
 *
 */
public class BasicAnalysisPipeline {
	
	public BasicAnalysisPipeline(File folder) throws Exception {
		
    	IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(folder);

    	Date startTime = Calendar.getInstance().getTime();
        String outputFolderName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(startTime);
    	File outFolder = new File(folder+File.separator+outputFolderName);
    	outFolder.mkdirs();
    	File saveFile = new File(outFolder, folder.getName()+Io.SAVE_FILE_EXTENSION);
    	runNewAnalysis(folder.getAbsolutePath(), op, saveFile);
		
	}

	
	/**
     * Run a new analysis on the images using the given options.
     * @param folder the name of the output folder for the nmd file
     * @param op the detection options
     * @param saveFile the full path to the nmd file
     * @return the new dataset
     * @throws Exception
     */
    private void runNewAnalysis(String folder, IAnalysisOptions op, File saveFile) throws Exception {
        
        if(!op.getDetectionOptions(CellularComponent.NUCLEUS).get().getFolder().exists()){
            throw new IllegalArgumentException("Detection folder does not exist");
        }
        IAnalysisMethod m = new NucleusDetectionMethod(folder, op);
        IAnalysisResult r = m.call();
        
        IAnalysisDataset obs = r.getFirstDataset();
        
        IAnalysisMethod p = new DatasetProfilingMethod(obs);
        p.call();
        
        IAnalysisMethod seg = new DatasetSegmentationMethod(obs, MorphologyAnalysisMode.NEW);
        seg.call();
                
        IAnalysisMethod m2 = new DatasetExportMethod(obs, saveFile);
        m2.call();
        
        File statsFile = new File(saveFile.getParentFile(), saveFile.getName()+Io.TAB_FILE_EXTENSION);
        IAnalysisMethod m3 = new DatasetStatsExporter(statsFile, obs);
        m3.call();
    }

}
