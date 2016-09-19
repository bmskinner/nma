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
package charting.charts;

import gui.SignalChangeEvent;
import gui.SignalChangeListener;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.jfree.chart.JFreeChart;

@SuppressWarnings("serial")
public class ConsensusNucleusChartPanel extends ExportableChartPanel {

	public static final String SOURCE_COMPONENT = "ConsensusNucleusChartPanel"; 
	private List<Object> listeners = new ArrayList<Object>();

	public ConsensusNucleusChartPanel(JFreeChart chart) {
		super(chart);
		
		JPopupMenu popup = createPopupMenu();
		this.setPopupMenu(popup);
		this.validate();
		this.setFixedAspectRatio(true);

	}
		
	private JPopupMenu createPopupMenu(){
		JPopupMenu popup = this.getPopupMenu();
		popup.addSeparator();
		
		JMenuItem alignItem = new JMenuItem("Align vertical");
		alignItem.addActionListener(this);
		alignItem.setActionCommand("AlignVertical");
		alignItem.setEnabled(true);
		
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
		return popup;
	}
	
		
	@Override
	public void actionPerformed(ActionEvent arg0) {
		super.actionPerformed(arg0);
		
		// Align two points to the vertical
		if(arg0.getActionCommand().equals("AlignVertical")){
			
			fireSignalChangeEvent("AlignVertical");
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


}
