package gui.dialogs;

import java.awt.Dialog;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JDialog;

import logging.Loggable;
import analysis.AnalysisDataset;
import gui.DatasetEvent;
import gui.DatasetEventListener;
import gui.InterfaceEvent;
import gui.InterfaceEventListener;
import gui.InterfaceEvent.InterfaceMethod;


/**
 * This extension to a JDialog can fire DatasetEvents and InterfaceEvents
 * to registered listeners. Used to communicate dataset updates and chart
 * recache requests from dialogs which modify datasets.
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public abstract class MessagingDialog extends JDialog implements Loggable {
	
	protected final List<Object> listeners 	= new ArrayList<Object>();
	
	public MessagingDialog(){
		super( );
	}
	
	/**
	 * Create with the given Dialog as a parent. Use null to make this
	 * dialog have a taskbar icon
	 * @param d the parent. Can be null
	 */
	public MessagingDialog(Dialog d){
		super( d );
	}
	

	public synchronized void addDatasetEventListener( DatasetEventListener l ) {
		listeners.add( l );
    }
    
    public synchronized void removeDatasetEventListener( DatasetEventListener l ) {
    	listeners.remove( l );
    }
    
    public synchronized void addInterfaceEventListener( InterfaceEventListener l ) {
    	listeners.add( l );
    }
    
    public synchronized void removeInterfaceEventListener( InterfaceEventListener l ) {
    	listeners.remove( l );
    }
    
    
    protected synchronized void fireDatasetEvent(String method, List<AnalysisDataset> list) {
    	
        DatasetEvent event = new DatasetEvent( this, method, this.getClass().getSimpleName(), list);
        Iterator<Object> iterator = listeners.iterator();
        while( iterator.hasNext() ) {
            ( (DatasetEventListener) iterator.next() ).datasetEventReceived( event );
        }
    }
    
    protected synchronized void fireDatasetEvent(String method, AnalysisDataset dataset) {
    	
    	List<AnalysisDataset> list= new ArrayList<AnalysisDataset>();
    	list.add(dataset);
    	fireDatasetEvent(method, list);
    }
    
    protected synchronized void fireDatasetEvent(String method, List<AnalysisDataset> list, AnalysisDataset template) {

    	DatasetEvent event = new DatasetEvent( this, method, this.getClass().getSimpleName(), list, template);
    	Iterator<Object> iterator = listeners.iterator();
    	while( iterator.hasNext() ) {
    		( (DatasetEventListener) iterator.next() ).datasetEventReceived( event );
    	}
    }
    
    protected synchronized void fireDatasetEvent(DatasetEvent event) {
    	Iterator<Object> iterator = listeners.iterator();
    	while( iterator.hasNext() ) {
    		( (DatasetEventListener) iterator.next() ).datasetEventReceived( event );
    	}
    }
    
    protected synchronized void fireInterfaceEvent(InterfaceMethod method) {
    	
    	InterfaceEvent event = new InterfaceEvent( this, method, this.getClass().getSimpleName());
        Iterator<Object> iterator = listeners.iterator();
        while( iterator.hasNext() ) {
            ( (InterfaceEventListener) iterator.next() ).interfaceEventReceived( event );
        }
    }
    
    protected synchronized void fireInterfaceEvent(InterfaceEvent event) {

        Iterator<Object> iterator = listeners.iterator();
        while( iterator.hasNext() ) {
            ( (InterfaceEventListener) iterator.next() ).interfaceEventReceived( event );
        }
    }
	

}
