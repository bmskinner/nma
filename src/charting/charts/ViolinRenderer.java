package charting.charts;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import logging.Loggable;

import org.jfree.chart.LegendItem;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.event.RendererChangeEvent;
import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.renderer.Outlier;
import org.jfree.chart.renderer.OutlierList;
import org.jfree.chart.renderer.OutlierListCollection;
import org.jfree.chart.renderer.category.AbstractCategoryItemRenderer;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.renderer.category.CategoryItemRendererState;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.MultiValueCategoryDataset;
import org.jfree.ui.RectangleEdge;

import stats.Stats;

/**
 * The ViolinRenderer draws a boxplot with a probability density function 
 * surrounding the box. It provides better visualisation of multimodal 
 * data than the boxplot. It takes a MultiValueCategoryDataset, which contains
 * the PDF at appropriate intervals across the range
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class ViolinRenderer extends BoxAndWhiskerRenderer implements Loggable {

//	/** The color used to paint the median line and average marker. */
//	private transient Paint artifactPaint;
//
//	/** A flag that controls whether or not the box is filled. */
//	private boolean fillBox;
//
//	/** The margin between items (boxes) within a category. */
//	private double itemMargin;

	/**
	 * Default constructor.
	  */
	public ViolinRenderer() {
		super();
		this.setMaximumBarWidth(0.1);
		this.setMeanVisible(false);
		this.setWhiskerWidth(0.05);
		this.setUseOutlinePaintForWhiskers(true);
	}

	
	@Override
	 public void drawItem(Graphics2D g2,
             CategoryItemRendererState state,
             Rectangle2D dataArea,
             CategoryPlot plot,
             CategoryAxis domainAxis,
             ValueAxis rangeAxis,
             CategoryDataset dataset,
             int row,
             int column,
             int pass) {
	
	
		
		 if (!(dataset instanceof ViolinCategoryDataset)) {
             throw new IllegalArgumentException(
                     "ViolinRenderer.drawItem() : the data should be " 
                     + "of type ViolinCategoryDataset only.");
         }
 
         PlotOrientation orientation = plot.getOrientation();
 
         log("Drawing item "+row+" - "+column);
         if (orientation == PlotOrientation.HORIZONTAL) {
        	 //TODO
//             drawHorizontalItem(g2, state, dataArea, plot, domainAxis, 
//                     rangeAxis, dataset, row, column);
        	 log("Horizontal not supported");
         } 
         else if (orientation == PlotOrientation.VERTICAL) {
             drawVerticalItem(g2, state, dataArea, plot, domainAxis, 
                     rangeAxis, dataset, row, column);
         }
		
	}
	
	private double calculateStepSize(ViolinCategoryDataset dataset, int row, int column){
		
		double stepSize = Double.NaN;
		log("Getting min and max y values");
		try {
			double ymax = dataset.getMax(row, column);
			log("Got ymax: "+ymax);
			double ymin = dataset.getMin(row, column);

			log("Got ymin: "+ymin);

			List<Number> values = dataset.getPdfValues(row, column);

			stepSize = (ymax - ymin) / values.size();
		} catch (Exception e){
			error("Error in step size", e);
		}
    	
    	return stepSize;
	}
	
	private Shape makeViolinPath(ViolinCategoryDataset dataset, 
			int row, 
			int column, 
			CategoryItemRendererState state,
            Rectangle2D dataArea,
            CategoryPlot plot, 
            CategoryAxis domainAxis, 
            ValueAxis rangeAxis){
		
		
		log("Calculating step size");
    	double stepSize = calculateStepSize(dataset, row, column);
    	log("Got step size: "+stepSize);
		
		double categoryEnd = domainAxis.getCategoryEnd(column, 
    			getColumnCount(), dataArea, plot.getDomainAxisEdge());
    	double categoryStart = domainAxis.getCategoryStart(column, 
    			getColumnCount(), dataArea, plot.getDomainAxisEdge());
    	double categoryWidth = categoryEnd - categoryStart;   	    	

        double xx = categoryStart; //
        int seriesCount   = getRowCount();
        int categoryCount = getColumnCount();

        if (seriesCount > 1) {
            double seriesGap = dataArea.getWidth() * getItemMargin() 
                               / (categoryCount * (seriesCount - 1));
            double usedWidth = (state.getBarWidth() * seriesCount) 
                               + (seriesGap * (seriesCount - 1));
            // offset the start of the boxes if the total width used is smaller
            // than the category width
            double offset = (categoryWidth - usedWidth) / 2;
            xx = xx + offset + (row * (state.getBarWidth() + seriesGap));
            
        } else {
            // offset the start of the box if the box width is smaller than the 
            // category width
            double offset = (categoryWidth - state.getBarWidth()) / 2;
            xx = xx + offset;
        } 
        
        log("Got bar positions");
        
        double xxmid = xx + state.getBarWidth() / 2d ; // the middle of the active bar
        double xxrange = state.getBarWidth()*2;
        
        // Get the y-values that must be used for step calculations
        double yValMin = dataset.getMin(row, column); // the lowest  y-value in the dataset
        double yValMax = dataset.getMax(row, column); // the highest y-value in the dataset
        
        
        RectangleEdge location = plot.getRangeAxisEdge();
        
        Path2D leftShape = new Path2D.Double();
        
        double yValPos = yValMin;
        
        leftShape.moveTo(xxmid, rangeAxis.valueToJava2D(yValPos, dataArea,
                  location)); // start with the lowest value
        
        log("Move to "+xxmid+" - "+rangeAxis.valueToJava2D(yValPos, dataArea,
                location));
        
        
        double maxProbability = Stats.max(dataset.getPdfValues(row, column)).doubleValue();
        
        List<Number> values = dataset.getPdfValues(row, column);
        for(Number v : values){
        	
        	// Get the y position on the chart
        	yValPos += stepSize;
        	
        	double yy = rangeAxis.valueToJava2D(yValPos, dataArea,
                  location); // convert y values to to java coordinates
        	
        	// Get the x position on the chart
        	
        	
        	double xValPos = (v.doubleValue() / maxProbability) ; // Express the proability as a fraction of the bar width
        	
        	double xxPosL = xxmid + ( (xxrange/2) * xValPos); // multiply the fraction by the bar width to get x position
        	
        	// Add to the line
        	leftShape.lineTo(xxPosL, yy);
        	
        	log("Drawing to "+xxPosL+" - "+yy);
        	
        }
        
        leftShape.lineTo(xxmid, rangeAxis.valueToJava2D(yValMax, dataArea,
                location)); // start with the lowest value
        log("Drawing to "+xxmid+" - "+rangeAxis.valueToJava2D(yValMax, dataArea,
                location));
        
        Collections.reverse(values);
        for(Number v : values){
        	
        	// Get the y position on the chart
        	yValPos -= stepSize;
        	
        	double yy = rangeAxis.valueToJava2D(yValPos, dataArea,
                  location); // convert y values to to java coordinates
        	
        	// Get the x position on the chart
        	
        	
        	double xValPos = (v.doubleValue() / maxProbability) ; // Express the proability as a fraction of the bar width
        	
        	double xxPosL = xxmid - ( (xxrange/2) * xValPos); // multiply the fraction by the bar width to get x position
        	
        	// Add to the line
        	leftShape.lineTo(xxPosL, yy);
        	
        	log("Drawing to "+xxPosL+" - "+yy);
        	
        }
        
        leftShape.closePath();
        return leftShape;
	}
	
    /**
     * Draws the visual representation of a single data item when the plot has 
     * a vertical orientation.
     *
     * @param g2  the graphics device.
     * @param state  the renderer state.
     * @param dataArea  the area within which the plot is being drawn.
     * @param plot  the plot (can be used to obtain standard color information 
     *              etc).
     * @param domainAxis  the domain axis.
     * @param rangeAxis  the range axis.
     * @param dataset  the dataset.
     * @param row  the row index (zero-based).
     * @param column  the column index (zero-based).
     */
    public void drawVerticalItem(Graphics2D g2, 
                                 CategoryItemRendererState state,
                                 Rectangle2D dataArea,
                                 CategoryPlot plot, 
                                 CategoryAxis domainAxis, 
                                 ValueAxis rangeAxis,
                                 CategoryDataset dataset, 
                                 int row, 
                                 int column) {
    	

    	ViolinCategoryDataset vioDataset 
                = (ViolinCategoryDataset) dataset;
    	
    	log("Drawing vertical item");
    	
    	
    	
    	// Draw the pdf on top of the boxplot
    	    	
    	Shape leftShape = makeViolinPath(vioDataset, column, column, state, dataArea, plot, domainAxis, rangeAxis);

    	Paint p = getItemPaint(row, column);
    	Color c = (Color) p;
    	
    	Color tr = new Color(c.getRed(), c.getGreen(), c.getBlue(), 128);
    	
    	if (p != null) {
    		g2.setPaint(tr);
    	}
    	Stroke s = getItemStroke(row, column);
    	g2.setStroke(s);

    	g2.fill(leftShape);
    	g2.draw(leftShape);
    	  	
    	
    	super.drawVerticalItem(g2, state, dataArea, plot, domainAxis, rangeAxis, dataset, row, column);
    	
    	
        // xxmid is the zero for the pdf
        // The max for the pdf is the bar
//        
//        double yyAverage = 0.0;
//        double yyOutlier;
//

//
//        double aRadius = 0;                 // average radius
//
//        RectangleEdge location = plot.getRangeAxisEdge();
//        
//        BoxplotData boxplotData = vioDataset.getBoxplot(row, column);
//        
//        log("Boxplot: "+boxplotData.toString());
//
//        Number yQ1  = boxplotData.getQ1();
//        Number yQ3  = boxplotData.getQ3();
//        Number yMax = boxplotData.getMax();
//        Number yMin = boxplotData.getMin();
//        Shape box = null;
//        if (yQ1 != null && yQ3 != null && yMax != null && yMin != null) {
//
//            double yyQ1 = rangeAxis.valueToJava2D(yQ1.doubleValue(), dataArea,
//                    location);
//            double yyQ3 = rangeAxis.valueToJava2D(yQ3.doubleValue(), dataArea, 
//                    location);
//            double yyMax = rangeAxis.valueToJava2D(yMax.doubleValue(), 
//                    dataArea, location);
//            double yyMin = rangeAxis.valueToJava2D(yMin.doubleValue(), 
//                    dataArea, location);
//            double xxmid = xx + state.getBarWidth() / 2.0;
//            
//            // draw the upper shadow...
//            g2.draw(new Line2D.Double(xxmid, yyMax, xxmid, yyQ3));
//            g2.draw(new Line2D.Double(xx, yyMax, xx + state.getBarWidth(), 
//                    yyMax));
//
//            // draw the lower shadow...
//            g2.draw(new Line2D.Double(xxmid, yyMin, xxmid, yyQ1));
//            g2.draw(new Line2D.Double(xx, yyMin, xx + state.getBarWidth(), 
//                    yyMin));
//
//            // draw the body...
//            box = new Rectangle2D.Double(xx, Math.min(yyQ1, yyQ3), 
//                    state.getBarWidth(), Math.abs(yyQ1 - yyQ3));
//            if (this.getFillBox()) {
//                g2.fill(box);
//            }
//            g2.draw(box);
//  
//        }
//        
//        g2.setPaint(this.getArtifactPaint());
//
//        // draw mean - SPECIAL AIMS REQUIREMENT...
//        Number yMean = boxplotData.getMean();
//        if (yMean != null) {
//            yyAverage = rangeAxis.valueToJava2D(yMean.doubleValue(), 
//                    dataArea, location);
//            aRadius = state.getBarWidth() / 8;
//            Ellipse2D.Double avgEllipse = new Ellipse2D.Double(xx + aRadius, 
//                    yyAverage - aRadius, aRadius * 2, aRadius * 2);
//            g2.fill(avgEllipse);
//            g2.draw(avgEllipse);
//        }
//
//        // draw median...
//        Number yMedian = boxplotData.getMedian();
//        if (yMedian != null) {
//            double yyMedian = rangeAxis.valueToJava2D(yMedian.doubleValue(), 
//                    dataArea, location);
//            g2.draw(new Line2D.Double(xx, yyMedian, xx + state.getBarWidth(), 
//                    yyMedian));
//        }
//        
//        // draw yOutliers...
//        double maxAxisValue = rangeAxis.valueToJava2D(
//                rangeAxis.getUpperBound(), dataArea, location) + aRadius;
//        double minAxisValue = rangeAxis.valueToJava2D(
//                rangeAxis.getLowerBound(), dataArea, location) - aRadius;
//
//        g2.setPaint(p);
//
//        // draw outliers
//        double oRadius = state.getBarWidth() / 3;    // outlier radius
//        List outliers = new ArrayList();
//        OutlierListCollection outlierListCollection 
//                = new OutlierListCollection();
//
//        // collect entity and tool tip information...
//        if (state.getInfo() != null && box != null) {
//            EntityCollection entities = state.getEntityCollection();
//            if (entities != null) {
//                String tip = null;
//                CategoryToolTipGenerator tipster 
//                        = getToolTipGenerator(row, column);
//                if (tipster != null) {
//                    tip = tipster.generateToolTip(dataset, row, column);
//                }
//                String url = null;
//                if (getItemURLGenerator(row, column) != null) {
//                    url = getItemURLGenerator(row, column).generateURL(dataset,
//                            row, column);
//                }
//                CategoryItemEntity entity = new CategoryItemEntity(box, tip, 
//                        url, dataset, row, dataset.getColumnKey(column), 
//                        column);
//                entities.add(entity);
//            }
//        }
//        
//        // Now draw the pdf using the values from the dataset getValues().
//        // These must be scaled on both axes:
//        // x - set the probability as a propoftion of the bar width
//        // y - space between boxplot min and max evenly for the number of values
        
    	

    }
	

}
