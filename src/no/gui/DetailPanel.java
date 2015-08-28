package no.gui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPanel;

/**
 * Add the listener and signal change settings to save each panel
 * reimplementing them
 * @author bms41
 *
 */
public abstract class DetailPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	private List<Object> listeners = new ArrayList<Object>();
	
	public DetailPanel(){
		
	}
	
	public synchronized void addSignalChangeListener( SignalChangeListener l ) {
        listeners.add( l );
    }
    
    public synchronized void removeSignalChangeListener( SignalChangeListener l ) {
        listeners.remove( l );
    }
    
    /**
     * A message to log in the main window log panel
     * @param message
     */
    public void log(String message){
    	fireSignalChangeEvent("Log_"+message);
    }
    
    
    /**
     * A message to write in the main window status line
     * @param message
     */
    public void status(String message){
    	fireSignalChangeEvent("Status_"+message);
    }
    
    /**
     * Log an error to the main window
     * @param message
     * @param e
     */
    public void error(String message, Exception e){
    	log(message+": "+e.getMessage());
		for(StackTraceElement e1 : e.getStackTrace()){
			log(e1.toString());
		}
    }
	
    protected synchronized void fireSignalChangeEvent(String message) {
    	
        SignalChangeEvent event = new SignalChangeEvent( this, message, this.getClass().getSimpleName());
        Iterator<Object> iterator = listeners.iterator();
        while( iterator.hasNext() ) {
            ( (SignalChangeListener) iterator.next() ).signalChangeReceived( event );
        }
    }

}
