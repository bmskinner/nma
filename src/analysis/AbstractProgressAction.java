package analysis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.RecursiveAction;

@SuppressWarnings("serial")
public abstract class AbstractProgressAction extends RecursiveAction implements ProgressListener {

	private final List<Object> listeners = new ArrayList<Object>();
	
	 public synchronized void addProgressListener( ProgressListener l ) {
	        listeners.add( l );
	    }
	    
	    public synchronized void removeProgressListener( ProgressListener l ) {
	        listeners.remove( l );
	    }
	    
	    protected synchronized void fireProgressEvent() {
	    	
	        ProgressEvent event = new ProgressEvent( this);
	        Iterator<Object> iterator = listeners.iterator();
	        while( iterator.hasNext() ) {
	            ( (ProgressListener) iterator.next() ).progressEventReceived( event );
	        }
	    }
	    
	    @Override
	    public void progressEventReceived(ProgressEvent event) {
	    	// pass up the chain
	    	fireProgressEvent();
	    	
	    }
	    
}
