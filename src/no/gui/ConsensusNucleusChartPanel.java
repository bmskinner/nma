package no.gui;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

public class ConsensusNucleusChartPanel extends ChartPanel implements SignalChangeListener{

	private static final long serialVersionUID = 1L;
	public static final String SOURCE_COMPONENT = "ConsensusNucleusChartPanel"; 
	private List<Object> listeners = new ArrayList<Object>();

	public ConsensusNucleusChartPanel(JFreeChart chart) {
		super(chart);
		JPopupMenu popup = this.createPopupMenu(true, true, true, true);// new JPopupMenu();
		popup.addSeparator();
		
		JMenuItem alignItem = new JMenuItem("Align vertical");
		alignItem.addActionListener(this);
		alignItem.setActionCommand("AlignVertical");
		
		popup.add(alignItem);
		this.setPopupMenu(popup);

	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		super.actionPerformed(arg0);
		
		// handle the new menu items
		if(arg0.getActionCommand().equals("AlignVertical")){
			
			// select points, do rotation
			log("Select the top point");
			// wait for click, get point nearest, wait for click, get point nearest
			
			// run rotation
		}
		
	}

	@Override
	public void signalChangeReceived(SignalChangeEvent event) {
		// TODO Auto-generated method stub
		
	}
	
	public synchronized void addSignalChangeListener( SignalChangeListener l ) {
        listeners.add( l );
    }
    
    public synchronized void removeSignalChangeListener( SignalChangeListener l ) {
        listeners.remove( l );
    }
     
    private synchronized void fireSignalChangeEvent(String message) {
        SignalChangeEvent event = new SignalChangeEvent( this, message, SOURCE_COMPONENT );
        Iterator<Object> iterator = listeners.iterator();
        while( iterator.hasNext() ) {
            ( (SignalChangeListener) iterator.next() ).signalChangeReceived( event );
        }
    }
    
    private void log(String message){
    	fireSignalChangeEvent("Log_"+message);
    }

}
