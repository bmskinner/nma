/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.bmskinner.nuclear_morphology.gui.actions;

import java.io.File;
import java.util.List;

import javax.swing.JFileChooser;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.ThreadManager;
import com.bmskinner.nuclear_morphology.io.DatasetStatsExporter;
import com.bmskinner.nuclear_morphology.io.Exporter;

/**
 * The action for exporting stats from datasets
 * @author bms41
 * @since 1.13.4
 *
 */
public class ExportStatsAction extends MultiDatasetResultAction {
	
	private static final String PROGRESS_LBL = "Exporting stats";
		
	public ExportStatsAction(final List<IAnalysisDataset> datasets, final MainWindow mw) {
		super(datasets, PROGRESS_LBL, mw);
	}

	@Override
	public void run() {
		
		File file = chooseExportFile();
		
		if(file==null){
			cancel();
			return;
		}
		
		IAnalysisMethod m = new DatasetStatsExporter(file, datasets);
		worker = new DefaultAnalysisWorker(m, datasets.size());
		worker.addPropertyChangeListener(this);
		this.setProgressMessage("Exporting stats");
		ThreadManager.getInstance().submit(worker);
		
	}
	
	@Override
	public void finished(){
		
		this.cancel();
		fine("Refolding finished, cleaning up");
		super.finished();
		this.countdownLatch();
	}
	
	private File chooseExportFile(){
		
		String defaultFile = null;
		File dir = null;
		if(datasets.size()==1){
			dir = datasets.get(0).getSavePath().getParentFile();
			defaultFile = datasets.get(0).getName()+Exporter.TAB_FILE_EXTENSION;
			
		} else {
			defaultFile = "Multiple_stats_export"+Exporter.TAB_FILE_EXTENSION;
			dir =  IAnalysisDataset.commonPathOfFiles(datasets);
			if( ! dir.exists() || ! dir.isDirectory()){
				dir = GlobalOptions.getInstance().getDefaultDir();
			}
		}


		JFileChooser fc = new JFileChooser( dir ); 
		fc.setSelectedFile(new File(defaultFile));
		fc.setDialogTitle("Specify a file to save as");

		int returnVal = fc.showSaveDialog(fc);
		if (returnVal != 0)	{
			return null; // user cancelled
		}

		File file = fc.getSelectedFile();
		
		// Add extension if needed
		if( ! file.getAbsolutePath().endsWith(Exporter.TAB_FILE_EXTENSION)){
			file = new File(file.getAbsolutePath()+Exporter.TAB_FILE_EXTENSION);
		}

		return file;
	}


}
