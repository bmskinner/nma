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
package charting.charts.panels;

import gui.GlobalOptions;
import gui.SignalChangeEvent;
import gui.SignalChangeListener;
import gui.components.ColourSelecter;

import java.awt.Color;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.util.Iterator;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;

import components.CellularComponent;
import components.nuclei.Nucleus;
import charting.charts.overlays.ComponentOverlay;
import charting.charts.overlays.ShapeOverlayObject;
import charting.datasets.ComponentOutlineDataset;

@SuppressWarnings("serial")
public class ConsensusNucleusChartPanel extends ExportableChartPanel {

	public static final String SOURCE_COMPONENT = "ConsensusNucleusChartPanel"; 
	
	private boolean fillConsensus = true;

	private ComponentOverlay consensusOverlay = null;

	public ConsensusNucleusChartPanel(JFreeChart chart) {
		super(chart);
		
		JPopupMenu popup = createPopupMenu();
		this.setPopupMenu(popup);
		this.validate();
		this.setFixedAspectRatio(true);
		consensusOverlay = new ComponentOverlay();
		this.addOverlay( consensusOverlay);

	}
	
	/**
	 * Provide an override to the GlobalOptions for this panel.
	 * If this is false, the consensus will never be filled. If this
	 * is true, the consensus will be filled then the GlobalOptions
	 * is also true
	 * @return
	 */
	public void setFillConsensus(boolean b){
		// 
		fillConsensus = b;
	}
	
	/**
	 * Check if this panel is overriding the global options
	 * @return
	 */
	public boolean isFillConsensus(){
		return fillConsensus;
	}
	
	@Override
	public void setChart(JFreeChart chart){
		
		super.setChart(chart);
		
		// Clear the overlay
		if(consensusOverlay !=null){
			consensusOverlay.clearShapes();

			fine("Cleared consensus overlays");
			
			if( ! GlobalOptions.getInstance().isFillConsensus()){
				fine("Consenus not set in global options");
				return;
				
			}
			
			if( ! fillConsensus){
				fine("Consenus not set in panel options");
				return;
			}
				
			fine("Attempting to make overlay");
			
			if( ! (chart.getPlot() instanceof XYPlot)){
				
				fine("Not an XYPlot");
				return;
			}


			if( ! (chart.getXYPlot().getDataset() instanceof ComponentOutlineDataset)){
				fine("Not a component outline dataset");
				return;
			}
				

			fine("Found outline dataset");

			ComponentOutlineDataset ds = (ComponentOutlineDataset) chart.getXYPlot().getDataset();

			for(int series=0; series<ds.getSeriesCount(); series++){

				Comparable seriesKey = ds.getSeriesKey(series);

				CellularComponent n = ds.getComponent(seriesKey);

				Paint c = chart.getXYPlot().getRenderer().getSeriesPaint(series);

				fine("Adding component overlay for "+seriesKey);

				if(n!=null){

					fine("Component is not null, making shape");
					//							Color c = ColourSelecter.getColor(series);
					c = ColourSelecter.getTransparentColour((Color) c, true, 128);
					ShapeOverlayObject o = new ShapeOverlayObject(n.toShape(), null, null, c);
					consensusOverlay.addShape(o, n);
				} else {
					fine("Component is null for "+seriesKey);
				}
			}

		}
		
		
		
		
		
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
