package com.bmskinner.nuclear_morphology.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.io.ImageImporter;

/**
 * Stores the basic methods for an IAnalysisMethod
 * @author ben
 *
 */
public abstract class AbstractAnalysisMethod implements IAnalysisMethod, ProgressListener {
		
		protected IAnalysisDataset dataset;
		private List<Object> listeners = new ArrayList<Object>();
		protected IAnalysisResult result = null;
		
		public AbstractAnalysisMethod(IAnalysisDataset dataset){
			this.dataset = dataset;
		}

		@Override
		public void addProgressListener(ProgressListener l) {
			listeners.add(l);
		}

		@Override
		public void removeProgressListener(ProgressListener l) {
			listeners.remove(l);
		}
		
		protected void fireProgressEvent(){
			ProgressEvent e = new ProgressEvent(this);
			fireProgressEvent(e);
		}
		
		protected void fireProgressEvent(ProgressEvent e){
			Iterator<Object> iterator = listeners.iterator();
			while( iterator.hasNext() ) {
				( (ProgressListener) iterator.next() ).progressEventReceived( e );
			}
		}
		
		@Override
		public void progressEventReceived(ProgressEvent event) {
//			log("Heard progress event in method");
			fireProgressEvent(); // pass upwards
			
		}
		
		/**
		 *  Checks that the given file is suitable for analysis.
		 *  Is the file an image. Also check if it is in the 'banned list'.
		 *  These are prefixes that are attached to exported images
		 *  at later stages of analysis. This prevents exported images
		 *  from previous runs being analysed.
		 *
		 *  @param file the File to check
		 *  @return a true or false of whether the file passed checks
		 */
		public static boolean checkFile(File file){

			if(file==null){
				return false;
			}
			
			if( ! file.isFile()){
				return false;
			}

			String fileName = file.getName();

			for( String prefix : ImageImporter.PREFIXES_TO_IGNORE){
				if(fileName.startsWith(prefix)){
					return false;
				}
			}

			for( String fileType : ImageImporter.IMPORTABLE_FILE_TYPES){
				if( fileName.endsWith(fileType) ){
					return true;
				}
			}
			return false;
		}		

}
