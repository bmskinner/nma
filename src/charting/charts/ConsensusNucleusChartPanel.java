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
public class ConsensusNucleusChartPanel extends FixedAspectRatioChartPanel {

	public static final String SOURCE_COMPONENT = "ConsensusNucleusChartPanel"; 
	private List<Object> listeners = new ArrayList<Object>();

	public ConsensusNucleusChartPanel(JFreeChart chart) {
		super(chart);
		
		JPopupMenu popup = createPopupMenu();
		this.setPopupMenu(popup);
		this.validate();

	}
	
//	@Override
//	public void restoreAutoBounds() {
//		log("Auto bounds");
//		try {
//			XYPlot plot = (XYPlot) this.getChart().getPlot();
//			if(plot.getDatasetCount()==0){
//				return;
//			}
//
//			// Find the aspect ratio of the chart
//			double chartWidth  = this.getWidth();
//			double chartHeight = this.getHeight();
//			double aspectRatio = chartWidth / chartHeight;
//			
//			log("Plot w: "+chartWidth+"; h: "+chartHeight+"; asp: "+aspectRatio);
//
//			// start with impossible values, before finding the real chart values
//			double xMin = Double.MAX_VALUE;
//			double yMin = Double.MAX_VALUE;
//			//		
//			double xMax = Double.MIN_VALUE;
//			double yMax = Double.MIN_VALUE;
//
////			finest("Plot has "+plot.getDatasetCount()+" datasets");
//			
//			// get the max and min values of each dataset in the chart
//			for(int i = 0; i<plot.getDatasetCount();i++){
//				XYDataset dataset = plot.getDataset(i);
//
//				if(dataset==null){
//					log("Null dataset "+i);
//					continue;
//				}
//				
//				// No values in the dataset, skip
//				if(DatasetUtilities.findMaximumDomainValue(dataset)==null){
//					continue;
//				}
//
//				xMax = DatasetUtilities.findMaximumDomainValue(dataset).doubleValue() > xMax
//						? DatasetUtilities.findMaximumDomainValue(dataset).doubleValue()
//								: xMax;
//
//				xMin = DatasetUtilities.findMinimumDomainValue(dataset).doubleValue() < xMin
//					 ? DatasetUtilities.findMinimumDomainValue(dataset).doubleValue()
//					 : xMin;
//
//				yMax = DatasetUtilities.findMaximumRangeValue(dataset).doubleValue() > yMax
//					 ? DatasetUtilities.findMaximumRangeValue(dataset).doubleValue()
//					 : yMax;
//
//				yMin = DatasetUtilities.findMinimumRangeValue(dataset).doubleValue() < yMin
//					 ? DatasetUtilities.findMinimumRangeValue(dataset).doubleValue()
//					 : yMin;
//			}
//			
//			// If no datasets were found, set defaults
//			log("Setting defaults");
//			if(xMin == Double.MAX_VALUE || yMin == Double.MAX_VALUE){
//				xMin = -DEFAULT_AUTO_RANGE;
//				yMin = -DEFAULT_AUTO_RANGE;
//				xMax = DEFAULT_AUTO_RANGE;
//				yMax = DEFAULT_AUTO_RANGE;
//			}
//			
//
//			// find the ranges they cover
//			double xRange = xMax - xMin;
//			double yRange = yMax - yMin;
//
//			double newXRange = xRange;
//			double newYRange = yRange;
//
//			// test the aspect ratio
//			if( (xRange / yRange) > aspectRatio){
//				// width is not enough
//				newXRange = xRange * 1.1;
//				newYRange = newXRange / aspectRatio;
//			} else {
//				// height is not enough
//				newYRange = yRange * 1.1; // add some extra x space
//				newXRange = newYRange * aspectRatio; // get the new Y range
//			}
//
//
//			// with the new ranges, find the best min and max values to use
//			double xDiff = (newXRange - xRange)/2;
//			double yDiff = (newYRange - yRange)/2;
//
//			xMin -= xDiff;
//			xMax += xDiff;
//			yMin -= yDiff;
//			yMax += yDiff;
//
//			plot.getRangeAxis().setRange(yMin, yMax);
//			plot.getDomainAxis().setRange(xMin, xMax);
//
//		} catch (Exception e){
//			log(Level.INFO,"Error restoring auto bounds, falling back to default", e);
//			super.restoreAutoBounds();
//		}
//	
//	}
	
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
	
	
//	@Override
//	//override the default zoom to keep aspect ratio
//	public void zoom(java.awt.geom.Rectangle2D selection){
//		
//		Rectangle2D.Double newSelection = null;
//		if(selection.getWidth()>selection.getHeight()){
//			newSelection = new Rectangle2D.Double(selection.getX(), 
//					selection.getY(), 
//					selection.getWidth(), 
//					selection.getWidth());					
//		} else {
//			newSelection = new Rectangle2D.Double(selection.getX(), 
//					selection.getY(), 
//					selection.getHeight(), 
//					selection.getHeight());		
//		}
//		super.zoom(newSelection);
//	}
	
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
