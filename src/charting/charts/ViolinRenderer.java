package charting.charts;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.List;

import logging.Loggable;

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.renderer.category.CategoryItemRendererState;
import org.jfree.data.category.CategoryDataset;
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
 
//         log("Drawing item "+row+" - "+column);
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
//		log("Getting min and max y values");
		try {
			double ymax = dataset.getMax(row, column);
//			log("Got ymax: "+ymax);
			double ymin = dataset.getMin(row, column);

//			log("Got ymin: "+ymin);

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
		
		
    	double stepSize = calculateStepSize(dataset, row, column);
		
		double categoryEnd = domainAxis.getCategoryEnd(column, 
    			getColumnCount(), dataArea, plot.getDomainAxisEdge());
    	double categoryStart = domainAxis.getCategoryStart(column, 
    			getColumnCount(), dataArea, plot.getDomainAxisEdge());
    	double categoryWidth = categoryEnd - categoryStart;   	    	

        double xx = categoryStart; //
        int seriesCount   = getRowCount();
        int categoryCount = getColumnCount();
        
//        log("Plot has "+seriesCount+" series and "+categoryCount+" categories");
//        
//        log("Series is"+dataset.getRowKey(row)+" - "+dataset.getColumnKey(column));

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
        	
        }
        
        // Move back to the x-midpoint at the highest value in the y-axis
        leftShape.lineTo(xxmid, rangeAxis.valueToJava2D(yValMax, dataArea,
                location)); // start with the lowest value
        
        // Now do the same on the other side of the midpoint        
        Collections.reverse(values);
        leftShape.lineTo(xxmid, rangeAxis.valueToJava2D(yValMax, dataArea,
                location)); // start with the lowest value
        
        for(Number v : values){

        	double yy = rangeAxis.valueToJava2D(yValPos, dataArea,
                  location); // convert y values to to java coordinates
        	
        	// Get the x position on the chart
        	
        	
        	double xValPos = (v.doubleValue() / maxProbability) ; // Express the proability as a fraction of the bar width
        	
        	double xxPosL = xxmid - ( (xxrange/2) * xValPos); // multiply the fraction by the bar width to get x position
        	
        	// Add to the line
        	leftShape.lineTo(xxPosL, yy);
        	
        	// Get the y position on the chart
        	yValPos -= stepSize;
        	
//        	log("Drawing to "+xxPosL+" - "+yy);
        	
        }
        
        leftShape.closePath();
        Collections.reverse(values); // restore original order
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
    	
    	
    	if(vioDataset.hasProbabilities()){
    	
    		// Draw the pdf behind of the boxplot

    		Shape leftShape = makeViolinPath(vioDataset, row, column, state, dataArea, plot, domainAxis, rangeAxis);

    		Paint p = getItemPaint(row, column);
    		Color c = (Color) p;

    		Color tr = new Color(c.getRed(), c.getGreen(), c.getBlue(), 128); // make the pdf transparent version of dataset colour

    		if (p != null) {
    			g2.setPaint(tr);
    		}
    		Stroke s = getItemStroke(row, column);
    		g2.setStroke(s);

    		g2.fill(leftShape);
    		g2.draw(leftShape);
    	}
    	
    	super.drawVerticalItem(g2, state, dataArea, plot, domainAxis, rangeAxis, dataset, row, column);
  
    }
	

}
