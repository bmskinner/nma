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

package gui.tabs.cells;

import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleEdge;

import analysis.AnalysisDataset;
import components.Cell;
import components.generic.BorderTagObject;
import components.generic.ProfileType;
import components.generic.SegmentedProfile;
import components.generic.XYPoint;
import components.nuclear.BorderPoint;
import gui.ChartSetEvent;
import gui.ChartSetEventListener;
import gui.RotationMode;
import gui.components.panels.DualChartPanel;
import charting.charts.ConsensusNucleusChartFactory;
import charting.charts.EllipticalOverlay;
import charting.charts.EllipticalOverlayObject;
import charting.charts.ExportableChartPanel;
import charting.charts.OutlineChartFactory;
import charting.charts.CoupledProfileOutlineChartPanel.BorderPointEvent;
import charting.charts.CoupledProfileOutlineChartPanel.BorderPointEventListener;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;


/**
 * This dialog allows the border points in a CellularComponent to be removed or moved.
 * Upon completion, the border FloatPolygon is re-interpolated to provide a 
 * new BorderPoint list
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class CellBorderAdjustmentDialog 
    extends    AbstractCellEditingDialog 
	implements BorderPointEventListener, 
	           ChartSetEventListener,
	           MouseListener,
	           MouseMotionListener, 
	           MouseWheelListener{
	
	
	private DualChartPanel dualPanel;
	
	private JButton deletePointsBtn;
	
	Map<BorderPoint, XYShapeAnnotation> selectedPoints = new HashMap<BorderPoint, XYShapeAnnotation>();
	
	private boolean canMove = false; // set if a point can be moved or not
	
	private double initialMovePointY = 0;
	private double  finalMovePointY = 0;
	
	private double initialMovePointX = 0;
	private double  finalMovePointX = 0;
	
	private XYItemEntity xyItemEntity = null;
	
	EllipticalOverlayObject ellipse;
	
	/*
	 * Notes:
	 * 
	 * Click a point to highlight / select it. 
	 * 
	 * Drag a window to select points in the border. These must become highlighted.
	 * 
	 * Option to delete highlighted points
	 * 
	 * Option to move highlighted points.
	 * 
	 * The border list must be updated to reflect changes and deletions
	 * 
	 * Show a minimap
	 * 
	 * Right mouse down - drag zoomed image
	 * Left mouse down - select ponts
	 * 
	 */
	
	public CellBorderAdjustmentDialog(CellViewModel model){
		super( model );
		
//		dualPanel.restoreAutoBounds(); // fixed aspect must be set after components are packed		
	}
	
	@Override
	public void load(final Cell cell, final AnalysisDataset dataset){
		super.load(cell, dataset);
		
		this.setTitle("Adjusting border in "+cell.getNucleus().getNameAndNumber());
		updateCharts(cell);
		setVisible(true);
	}
	
	@Override
	protected void createUI(){

		try{
			finer("Creating border adjustment dialog");
			this.setLayout(new BorderLayout());

			JPanel header = createHeader();
			this.add(header, BorderLayout.NORTH);
			
	
			JFreeChart empty1 = ConsensusNucleusChartFactory.getInstance().makeEmptyChart();
			JFreeChart empty2 = ConsensusNucleusChartFactory.getInstance().makeEmptyChart();
			
			dualPanel = new CellBorderDualPanel();
			
			dualPanel.setCharts(empty1, empty2);
			
			ExportableChartPanel mainPanel = dualPanel.getMainPanel();
			mainPanel.setFixedAspectRatio(true);
			mainPanel.setPopupMenu(null);

			mainPanel.addBorderPointEventListener(this);
			mainPanel.addMouseMotionListener(this);
			mainPanel.addMouseListener(this);
			mainPanel.addMouseWheelListener(this);

			JPanel chartPanel = new JPanel();
			chartPanel.setLayout(new GridBagLayout());
			
			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.EAST;
			c.gridx = 0;
			c.gridy = 0;
			c.gridwidth  = 1; 
			c.gridheight = 1;
			c.fill = GridBagConstraints.BOTH;      //reset to default
			c.weightx = 0.6; 
			c.weighty = 1.0;
			
			chartPanel.add(mainPanel, c);
			c.weightx = 0.4;
			c.gridx = 1;
			c.gridy = 0;
			
			ExportableChartPanel rangePanel = dualPanel.getRangePanel();
			rangePanel.setFixedAspectRatio(true);
			chartPanel.add(rangePanel, c);
			
			this.add(chartPanel, BorderLayout.CENTER);

			Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
			this.setPreferredSize(new Dimension(  (int)(screenSize.width*0.9), (int)(screenSize.height*0.5)));
			this.pack();
			
			// Create an ellipse overlay for the main panel with circular appearance
			double aspect = mainPanel.getAspectRatio();
			
			double h = 3;
			double w = h / aspect; 
			
			ellipse = new EllipticalOverlayObject(0, w, 0, h);
			mainPanel.addOverlay( new EllipticalOverlay(ellipse));
			
		} catch (Exception e){
			fine("Error making UI", e);
		}
	}
	
	private JPanel createHeader(){
		JPanel panel = new JPanel(new FlowLayout());
		
		deletePointsBtn = new JButton("Delete selected point(s)");
		deletePointsBtn.addActionListener( e -> {
			
			log("Clicked delete button");
			deleteSelectedPoints();
		});
		panel.add(deletePointsBtn);
			
		
		JButton undoBtn = new JButton("Undo");
		undoBtn.addActionListener( e ->{
			workingCell = new Cell(cell);
			updateCharts(workingCell);
			
		});
		panel.add(undoBtn);
		
		return panel;
	}
	

	
	
	@Override
	protected void updateCharts(Cell cell){
			
			finer("Making outline chart options");
			ChartOptions outlineOptions = new ChartOptionsBuilder()
				.setDatasets(dataset)
				.setCell(cell)
				.setRotationMode(RotationMode.ACTUAL)
				.setShowAnnotations(false)
				.setInvertYAxis( true ) // only invert for actual
				.setShowXAxis(false)
				.setShowYAxis(false)
				.setShowPoints(true)
				.setCellularComponent(cell.getNucleus())
				.build();
	
//			finer("Making chart");
			JFreeChart outlineChart = OutlineChartFactory.getInstance().makeCellOutlineChart(outlineOptions);
			JFreeChart outlineChart2 = OutlineChartFactory.getInstance().makeCellOutlineChart(outlineOptions);
//			finer("Updating chart");
			dualPanel.setCharts(outlineChart, outlineChart2);
//			dualPanel.restoreAutoBounds();

	}

	@Override
	public void borderPointEventReceived(BorderPointEvent event) {
		log("Border point event received");
		
	}
	
	private void selectClickedPoint(XYPoint clickedPoint){
		for(BorderPoint point : workingCell.getNucleus().getBorderList()){
			
			if(point.overlapsPerfectly( clickedPoint )){
				
				if(! selectedPoints.containsKey(point)){

					double aspect = dualPanel.getMainPanel().getAspectRatio();
					double radius = 0.6;
					double h = radius*2;
					double w = h / aspect; 
					
					Ellipse2D r = new Ellipse2D.Double(point.getX()-(w/2),
							point.getY()-radius,
							w, h);
					XYShapeAnnotation a = new XYShapeAnnotation(r, null, null, Color.BLUE);
					selectedPoints.put(point, a);		
					dualPanel.getMainPanel().getChart().getXYPlot().addAnnotation(a);
				}
				break;
			}
		}
	}
	
	private void clearSelectedPoints(){

		for (XYShapeAnnotation a : selectedPoints.values()){
			dualPanel.getMainPanel().getChart().getXYPlot().removeAnnotation(a);
	
		}
		selectedPoints = new HashMap<BorderPoint, XYShapeAnnotation>();
	}
	
	private void moveSelectedPoint(){
		// Move the selected point in the border list copy
		List<BorderPoint> borderList = workingCell.getNucleus().getBorderList();
		for(int i=0; i<borderList.size(); i++){
		
			BorderPoint point = borderList.get(i);
			if(selectedPoints.containsKey(point)){
				workingCell.getNucleus().updateBorderPoint(i, finalMovePointX, finalMovePointY);
				break;
			}
		}
		setCellChanged(true);
		updateWorkingCell(workingCell.getNucleus().getBorderList());
	}
	
	private void deleteSelectedPoints(){
		// Remove the selected points from the border list copy
		List<BorderPoint> borderList = workingCell.getNucleus().getBorderList();
		Iterator<BorderPoint> it = borderList.iterator();
		while(it.hasNext()){
			BorderPoint point = it.next();
			if(selectedPoints.containsKey(point)){
				it.remove();
			}
		}
		setCellChanged(true);
		updateWorkingCell(borderList);

		
	}
	
	private void updateWorkingCell(List<BorderPoint> borderList){
		// Make a interpolated FloatPolygon from the new array
		float[] xPoints = new float[borderList.size()];
		float[] yPoints = new float[borderList.size()];
		int nPoints     = borderList.size();
		int type = Roi.POLYGON;

		for(int i=0; i<nPoints; i++){
			xPoints[i] = (float) borderList.get(i).getX();
			yPoints[i] = (float) borderList.get(i).getY();
		}

		PolygonRoi roi = new PolygonRoi(xPoints, yPoints, nPoints, type);
		FloatPolygon fp = roi.getInterpolatedPolygon();

		// Make new border list and assign to the working cell

		List<BorderPoint> newList = new ArrayList<BorderPoint>();
		for(int i=0; i<fp.npoints; i++){
			BorderPoint point = new BorderPoint( fp.xpoints[i], fp.ypoints[i]);

			if(i>0){
				point.setPrevPoint(newList.get(i-1));
				point.prevPoint().setNextPoint(point);
			}
			newList.add(point);
		}
		// link endpoints
		newList.get(newList.size()-1).setNextPoint(newList.get(0));
		newList.get(0).setPrevPoint(newList.get(newList.size()-1));

		Rectangle boundingRectangle = new Rectangle(fp.getBounds());

		workingCell.getNucleus().setBorderList(newList);
		workingCell.getNucleus().setBoundingRectangle(boundingRectangle);

		Range domainRange = dualPanel.getMainPanel().getChart().getXYPlot().getDomainAxis().getRange();
		Range  rangeRange = dualPanel.getMainPanel().getChart().getXYPlot().getRangeAxis().getRange();
		updateCharts(workingCell);
		clearSelectedPoints();
		

		// Recalculate profiles for the cell, and update segments to best fit positions
		updateWorkingCellProfiles();
		
		
		dualPanel.getMainPanel().getChart().getXYPlot().getDomainAxis().setRange(domainRange);
		dualPanel.getMainPanel().getChart().getXYPlot().getRangeAxis().setRange(rangeRange);

	}
	
	private void updateWorkingCellProfiles(){
		
		// Get the positions of segment boundaries
		SegmentedProfile templateProfile = workingCell.getNucleus().getProfile(ProfileType.ANGLE);
		Map<BorderTagObject, Integer> tagMap = workingCell.getNucleus().getBorderTags();
		
		try {
			workingCell.getNucleus().calculateProfiles();
			
			// Use the previous boundary positions as a template on the new profile
//			SegmentedProfile updatedProfile = workingCell.getNucleus().getProfile(ProfileType.ANGLE);
//			updatedProfile = updatedProfile.frankenNormaliseToProfile(templateProfile);
//			workingCell.getNucleus().setProfile(ProfileType.ANGLE, updatedProfile);
			
			// Get the border tag positions, and set equivalent positions in the new profile
			
			
		} catch (Exception e) {
			error("Cannot calculate profiles for cell", e);
		}

	}
		
	
	public void movePoint(MouseEvent me) {
	    if (canMove) {
	        int itemIndex = xyItemEntity.getItem();
	        Point pt = me.getPoint();
	        XYPlot xy = dualPanel.getMainPanel().getChart().getXYPlot();
	        Rectangle2D dataArea = dualPanel.getMainPanel().getChartRenderingInfo()
	            .getPlotInfo().getDataArea();
	        Point2D p = dualPanel.getMainPanel().translateScreenToJava2D(pt);
	        finalMovePointY = xy.getRangeAxis().java2DToValue(p.getY(),
	            dataArea, xy.getRangeAxisEdge());
	        
	        finalMovePointX = xy.getDomainAxis().java2DToValue(p.getX(),
		            dataArea, xy.getDomainAxisEdge());
	        	        
//	        log("Moving to "+finalMovePointX+" , "+finalMovePointY);
//	        
//	        double differenceY = finalMovePointY - initialMovePointY;
//	        double differenceX = finalMovePointX - initialMovePointX;
//	        
////	        XYSeries series = 
//	        		
//	        		xy.getDataset(). 		
//	        
//	        if (series.getY(itemIndex).doubleValue()
//	            + difference > xy.getRangeAxis().getRange().getLength()
//	            || series.getY(itemIndex).doubleValue()
//	            + difference < 0.0D) {
//	            initialMovePointY = finalMovePointY;
//	        }
//	        // retrict movement for upper and lower limit (upper limit
//	        // should be as per application needs)
//	        double targetPoint = xy.getDataset().getX(xyItemEntity.getSeriesIndex(), itemIndex).series.getY(itemIndex).doubleValue() + difference;
//	        if (targetPoint > 15 || targetPoint < 0) {
//	            return;
//	        } else {
//	            series.update(Integer.valueOf(itemIndex), Double.valueOf(targetPoint));
//	        }
//	        panel.getChart().fireChartChanged();
//	        panel.updateUI();
//	        initialMovePointY = finalMovePointY;
	    }
	}

	@Override
	public void chartSetEventReceived(ChartSetEvent e) {
		// This is required in the setup for ExportableChartPanel
		
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		movePoint(e);
		
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		
		int x = e.getX();
		int y = e.getY();
		
		// Update the overlay position on the main chart
		
		Rectangle2D dataArea = dualPanel.getMainPanel().getScreenDataArea();
		JFreeChart  chart    = dualPanel.getMainPanel().getChart();
		XYPlot      plot     = (XYPlot) chart.getPlot();
		ValueAxis   xAxis    = plot.getDomainAxis();
		ValueAxis   yAxis    = plot.getRangeAxis();
								
		double xValue = xAxis.java2DToValue(x, dataArea, 
				RectangleEdge.BOTTOM);
		
		double yValue = yAxis.java2DToValue(y, dataArea, 
				RectangleEdge.LEFT);
		
		
		ellipse.setXValue(xValue);
		ellipse.setYValue(yValue);
		
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		canMove = false;
	    initialMovePointY = 0;
	    initialMovePointX = 0;
	}

	@Override
	public void mousePressed(MouseEvent e) {

	    EntityCollection entities = dualPanel.getMainPanel().getChartRenderingInfo().getEntityCollection();
	    

	    if( (e.getModifiers() & InputEvent.BUTTON1_MASK) ==InputEvent.BUTTON1_MASK){
	    	// Find the entities under the ellipse overlay

	    	XYDataset ds = dualPanel.getMainPanel().getChart().getXYPlot().getDataset();
	    	for(Object entity : entities.getEntities()){

	    		if(entity instanceof XYItemEntity){

	    			xyItemEntity = (XYItemEntity) entity;

	    			int series = xyItemEntity.getSeriesIndex();
	    			int item   = xyItemEntity.getItem();

	    			double xVal = ds.getXValue(series, item);
	    			double yVal = ds.getYValue(series, item);

	    			if(ellipse.contains(xVal,  yVal)){
	    				XYPoint clickedPoint = new XYPoint(xVal, yVal);
	    				selectClickedPoint(clickedPoint);
	    			}
	    		}

	    	}
	    }
	    
	    if( (e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK){
	    	clearSelectedPoints();
	    }
	    
	    
//	    ChartMouseEvent cme = new ChartMouseEvent(dualPanel.getMainPanel().getChart(), e, entities
//	        .getEntity(x, y));
//	    
//	    ChartEntity entity = cme.getEntity();
//	    if ((entity != null) && (entity instanceof XYItemEntity)) {
//	        xyItemEntity = (XYItemEntity) entity;
//	    } else if (!(entity instanceof XYItemEntity)) {
//	        xyItemEntity = null;
//	        return;
//	    }
//	    if (xyItemEntity == null) {
//	        return; // return if not pressed on any series point
//	    }
//	    Point pt = e.getPoint();
//	    log("Mouse pressed on entity");
//
//		Number xN = xyItemEntity.getDataset().getX(xyItemEntity.getSeriesIndex(), xyItemEntity.getItem());
//		Number yN = xyItemEntity.getDataset().getY(xyItemEntity.getSeriesIndex(), xyItemEntity.getItem());
//					
//		XYPoint clickedPoint = new XYPoint(xN.doubleValue(), yN.doubleValue());
//		
//		if( (e.getModifiers() & InputEvent.BUTTON1_MASK) ==InputEvent.BUTTON1_MASK){
//
//				clearSelectedPoints();
//				selectClickedPoint(clickedPoint);
//			    
//			    
//			    XYPlot xy = dualPanel.getMainPanel().getChart().getXYPlot();
//			    Rectangle2D dataArea = dualPanel.getMainPanel().getChartRenderingInfo()
//			        .getPlotInfo().getDataArea();
//			    Point2D p = dualPanel.getMainPanel().translateScreenToJava2D(pt);
//			    
//			    initialMovePointY = xy.getRangeAxis().java2DToValue(p.getY(), dataArea,
//			        xy.getRangeAxisEdge());
//			    
//			    initialMovePointX = xy.getDomainAxis().java2DToValue(p.getX(), dataArea,
//				        xy.getDomainAxisEdge());
//			    
//			    finalMovePointY = initialMovePointY;
//			    finalMovePointX = initialMovePointX;
//			    canMove = true;
//			    
//			    dualPanel.getMainPanel().setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
//		    
//		} 
//
//		if( (e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK){
//
//			selectClickedPoint(clickedPoint);
//
//		}
//		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// stop dragging on mouse released
		
		if( canMove && (e.getModifiers() & InputEvent.BUTTON1_MASK) ==InputEvent.BUTTON1_MASK){
			canMove = false;


			if(finalMovePointX != initialMovePointX && finalMovePointY != initialMovePointY){

				// Point was moved, get the item to change in the dataset
				log("Released at "+finalMovePointX+" , "+finalMovePointY);
				moveSelectedPoint();
			}

			initialMovePointY = 0;
			initialMovePointX = 0;
			dualPanel.getMainPanel().setCursor(Cursor.getDefaultCursor());
		}
		
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		int rotation  = e.getWheelRotation();
		
		// + = zoom in
		
		if(rotation>0){
			ellipse.setXRadius(ellipse.getXRadius()/2);
			ellipse.setYRadius(ellipse.getYRadius()/2);
		}
		if(rotation<0){
			ellipse.setXRadius(ellipse.getXRadius()*2);
			ellipse.setYRadius(ellipse.getYRadius()*2);
		}
		
		
	}

}
