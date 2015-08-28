package no.gui;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.XYDataset;

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
		alignItem.setEnabled(false);
		
		JMenuItem rotateItem = new JMenuItem("Rotate by...");
		rotateItem.addActionListener(this);
		rotateItem.setActionCommand("RotateConsensus");
		
		JMenuItem resetItem = new JMenuItem("Reset rotation to tail");
		resetItem.addActionListener(this);
		resetItem.setActionCommand("RotateReset");
		
		JMenuItem offsetItem = new JMenuItem("Offset...");
		offsetItem.addActionListener(this);
		offsetItem.setActionCommand("OffsetAction");
		
		JMenuItem resetOffsetItem = new JMenuItem("Reset offset to zero");
		resetOffsetItem.addActionListener(this);
		resetOffsetItem.setActionCommand("OffsetReset");
		
		popup.add(alignItem);
		popup.add(rotateItem);
		popup.add(resetItem);
		popup.addSeparator();
		popup.add(offsetItem);
		popup.add(resetOffsetItem);
		
		this.setPopupMenu(popup);

	}
	
	@Override
	public void restoreAutoBounds() {
		XYPlot plot = (XYPlot) this.getChart().getPlot();
		
		int maxRange = 0;
		
		for(int i = 0; i<plot.getDatasetCount();i++){
			XYDataset dataset = plot.getDataset(i);
			
			Number maxX = DatasetUtilities.findMaximumDomainValue(dataset);
			Number minX = DatasetUtilities.findMinimumDomainValue(dataset);
			
			int absXmax = Math.abs(maxX.intValue());
			int absXmin = Math.abs(minX.intValue());
			int absX = absXmax > absXmin ? absXmax : absXmin;
			
			Number maxY = DatasetUtilities.findMaximumRangeValue(dataset);
			Number minY = DatasetUtilities.findMinimumRangeValue(dataset);
			
			int absYmax = Math.abs(maxY.intValue());
			int absYmin = Math.abs(minY.intValue());
			int absY = absYmax > absYmin ? absYmax : absYmin;
			
			int datasetAbs = absX > absY ? absX : absY;
			
			maxRange = datasetAbs > maxRange ? datasetAbs : maxRange;
		}
		maxRange *= 1.1; // add 10% as a border
		plot.getRangeAxis().setRange(-maxRange, maxRange);
		plot.getDomainAxis().setRange(-maxRange, maxRange);				
	} 
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		super.actionPerformed(arg0);
		
		// Align two points to the vertical
		if(arg0.getActionCommand().equals("AlignVertical")){
			
			fireSignalChangeEvent("AlignVertical");
			// select points, do rotation
			// wait for click, get point nearest, wait for click, get point nearest
			// run rotation
		}
		
		// Rotate the consensus in the chart by the given amount
		if(arg0.getActionCommand().equals("RotateConsensus")){
			fireSignalChangeEvent("RotateConsensus");
		}
		
		// reset the rotation to the orientation point (tail)
		if(arg0.getActionCommand().equals("RotateReset")){
			fireSignalChangeEvent("RotateReset");
		}
		
		if(arg0.getActionCommand().equals("OffsetAction")){
			fireSignalChangeEvent("OffsetAction");
		}
		
		if(arg0.getActionCommand().equals("OffsetReset")){
			fireSignalChangeEvent("OffsetReset");
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
