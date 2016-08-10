package gui.dialogs;

import java.awt.Dialog;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JDialog;

import analysis.AnalysisDataset;
import gui.DatasetEvent;
import gui.DatasetEventListener;
import gui.InterfaceEvent;
import gui.InterfaceEventListener;
import gui.DatasetEvent.DatasetMethod;
import gui.InterfaceEvent.InterfaceMethod;


@SuppressWarnings("serial")
public abstract class MessagingDialog extends JDialog {
	
	private final List<Object> datasetListeners 	= new ArrayList<Object>();
	private final List<Object> interfaceListeners 	= new ArrayList<Object>();
	
	public MessagingDialog(){
		super( );
	}
	
	public MessagingDialog(Dialog d){
		super( d );
	}
	

	public synchronized void addDatasetEventListener( DatasetEventListener l ) {
    	datasetListeners.add( l );
    }
    
    public synchronized void removeDatasetEventListener( DatasetEventListener l ) {
    	datasetListeners.remove( l );
    }
    
    public synchronized void addInterfaceEventListener( InterfaceEventListener l ) {
    	interfaceListeners.add( l );
    }
    
    public synchronized void removeInterfaceEventListener( InterfaceEventListener l ) {
    	interfaceListeners.remove( l );
    }
    
    
    protected synchronized void fireDatasetEvent(DatasetMethod method, List<AnalysisDataset> list) {
    	
        DatasetEvent event = new DatasetEvent( this, method, this.getClass().getSimpleName(), list);
        Iterator<Object> iterator = datasetListeners.iterator();
        while( iterator.hasNext() ) {
            ( (DatasetEventListener) iterator.next() ).datasetEventReceived( event );
        }
    }
    
    protected synchronized void fireDatasetEvent(DatasetMethod method, List<AnalysisDataset> list, AnalysisDataset template) {

    	DatasetEvent event = new DatasetEvent( this, method, this.getClass().getSimpleName(), list, template);
    	Iterator<Object> iterator = datasetListeners.iterator();
    	while( iterator.hasNext() ) {
    		( (DatasetEventListener) iterator.next() ).datasetEventReceived( event );
    	}
    }
    
    protected synchronized void fireDatasetEvent(DatasetEvent event) {
    	Iterator<Object> iterator = datasetListeners.iterator();
    	while( iterator.hasNext() ) {
    		( (DatasetEventListener) iterator.next() ).datasetEventReceived( event );
    	}
    }
    
    protected synchronized void fireInterfaceEvent(InterfaceMethod method) {
    	
    	InterfaceEvent event = new InterfaceEvent( this, method, this.getClass().getSimpleName());
        Iterator<Object> iterator = interfaceListeners.iterator();
        while( iterator.hasNext() ) {
            ( (InterfaceEventListener) iterator.next() ).interfaceEventReceived( event );
        }
    }
    
    protected synchronized void fireInterfaceEvent(InterfaceEvent event) {

        Iterator<Object> iterator = interfaceListeners.iterator();
        while( iterator.hasNext() ) {
            ( (InterfaceEventListener) iterator.next() ).interfaceEventReceived( event );
        }
    }
	

}
