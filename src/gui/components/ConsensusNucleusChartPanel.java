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
package gui.components;

import gui.SignalChangeEvent;
import gui.SignalChangeListener;

import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
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

public class ConsensusNucleusChartPanel extends FixedAspectRatioChartPanel implements SignalChangeListener{

	private static final long serialVersionUID = 1L;
	public static final String SOURCE_COMPONENT = "ConsensusNucleusChartPanel"; 
	private List<Object> listeners = new ArrayList<Object>();

	public ConsensusNucleusChartPanel(JFreeChart chart) {
		super(chart);
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
		
		this.setPopupMenu(popup);

	}
	
//	@Override
//	public void restoreAutoBounds() {
//		XYPlot plot = (XYPlot) this.getChart().getPlot();
//		
//		int maxRange = 0;
//		
//		for(int i = 0; i<plot.getDatasetCount();i++){
//			XYDataset dataset = plot.getDataset(i);
//			
//			Number maxX = DatasetUtilities.findMaximumDomainValue(dataset);
//			Number minX = DatasetUtilities.findMinimumDomainValue(dataset);
//			
//			int absXmax = Math.abs(maxX.intValue());
//			int absXmin = Math.abs(minX.intValue());
//			int absX = absXmax > absXmin ? absXmax : absXmin;
//			
//			Number maxY = DatasetUtilities.findMaximumRangeValue(dataset);
//			Number minY = DatasetUtilities.findMinimumRangeValue(dataset);
//			
//			int absYmax = Math.abs(maxY.intValue());
//			int absYmin = Math.abs(minY.intValue());
//			int absY = absYmax > absYmin ? absYmax : absYmin;
//			
//			int datasetAbs = absX > absY ? absX : absY;
//			
//			maxRange = datasetAbs > maxRange ? datasetAbs : maxRange;
//		}
//		maxRange *= 1.1; // add 10% as a border
//		plot.getRangeAxis().setRange(-maxRange, maxRange);
//		plot.getDomainAxis().setRange(-maxRange, maxRange);				
//	} 
	
//	@Override
//	public void restoreAutoBounds() {
//		
//		XYPlot plot = (XYPlot) this.getChart().getPlot();
//		
//		double chartWidth = this.getWidth();
//		double chartHeight = this.getHeight();
//		double aspectRatio = chartWidth / chartHeight;
//		
//		// start with impossible values
//		double xMin = chartWidth;
//		double yMin = chartHeight;
////		
//		double xMax = 0;
//		double yMax = 0;
//		
//		
//		// get the max and min values of the chart
//		for(int i = 0; i<plot.getDatasetCount();i++){
//			XYDataset dataset = plot.getDataset(i);
//			
//			if(dataset==null){
//				return;
//			}
//			
//				xMax = DatasetUtilities.findMaximumDomainValue(dataset).doubleValue() > xMax
//						? DatasetUtilities.findMaximumDomainValue(dataset).doubleValue()
//						: xMax;
//				
//				xMin = DatasetUtilities.findMinimumDomainValue(dataset).doubleValue() < xMin
//						? DatasetUtilities.findMinimumDomainValue(dataset).doubleValue()
//						: xMin;
//						
//				yMax = DatasetUtilities.findMaximumRangeValue(dataset).doubleValue() > yMax
//						? DatasetUtilities.findMaximumRangeValue(dataset).doubleValue()
//						: yMax;
//				
//				yMin = DatasetUtilities.findMinimumRangeValue(dataset).doubleValue() < yMin
//						? DatasetUtilities.findMinimumRangeValue(dataset).doubleValue()
//						: yMin;
//			
//		}
//		
//
//		// find the ranges they cover
//		double xRange = xMax - xMin;
//		double yRange = yMax - yMin;
//		
////		double aspectRatio = xRange / yRange;
//
//		double newXRange = xRange;
//		double newYRange = yRange;
//
//		// test the aspect ratio
////		IJ.log("Old range: "+xMax+"-"+xMin+", "+yMax+"-"+yMin);
//		if( (xRange / yRange) > aspectRatio){
//			// width is not enough
////			IJ.log("Too narrow: "+xRange+", "+yRange+":  aspect ratio "+aspectRatio);
//			newXRange = xRange * 1.1;
//			newYRange = newXRange / aspectRatio;
//		} else {
//			// height is not enough
////			IJ.log("Too short: "+xRange+", "+yRange+":  aspect ratio "+aspectRatio);
//			newYRange = yRange * 1.1; // add some extra x space
//			newXRange = newYRange * aspectRatio; // get the new Y range
//		}
//		
//
//		// with the new ranges, find the best min and max values to use
//		double xDiff = (newXRange - xRange)/2;
//		double yDiff = (newYRange - yRange)/2;
//
//		xMin -= xDiff;
//		xMax += xDiff;
//		yMin -= yDiff;
//		yMax += yDiff;
////		IJ.log("New range: "+xMax+"-"+xMin+", "+yMax+"-"+yMin);
//
//		plot.getRangeAxis().setRange(yMin, yMax);
//		plot.getDomainAxis().setRange(xMin, xMax);				
//	} 
	
	@Override
	//override the default zoom to keep aspect ratio
	public void zoom(java.awt.geom.Rectangle2D selection){
		
		Rectangle2D.Double newSelection = null;
		if(selection.getWidth()>selection.getHeight()){
			newSelection = new Rectangle2D.Double(selection.getX(), 
					selection.getY(), 
					selection.getWidth(), 
					selection.getWidth());					
		} else {
			newSelection = new Rectangle2D.Double(selection.getX(), 
					selection.getY(), 
					selection.getHeight(), 
					selection.getHeight());		
		}
		super.zoom(newSelection);
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
    
//    private void log(String message){
//    	fireSignalChangeEvent("Log_"+message);
//    }

}
