package com.bmskinner.nuclear_morphology.analysis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;

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

}
