package com.bmskinner.nuclear_morphology.charting;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.logging.Logger;

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.renderer.category.CategoryItemRendererState;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;

public class ViolinRenderer extends BoxAndWhiskerRenderer {
	
	private static final Logger LOGGER = Logger.getLogger(ViolinRenderer.class.getName());
	
	private boolean showBoxplot = true;
	private boolean showViolin = true;
	
	/**
     * Default constructor that draws the box and the violin
     */
    public ViolinRenderer() {
        this(true, true);
    }

    /**
     * Constructor specifying whether the boxplot and the violin should be drawn.
     * @param showBox true if the boxplot should be drawn
     * @param showViolin true if the violin should be drawn
     */
    public ViolinRenderer(boolean showBox, boolean showViolin) {
        super();
        showBoxplot = showBox;
        this.showViolin = showViolin;
        this.setMaximumBarWidth(0.5);
        this.setMeanVisible(false);
        this.setWhiskerWidth(0.05);
        this.setUseOutlinePaintForWhiskers(true);
    }

    @Override
    public void drawItem(Graphics2D g2, CategoryItemRendererState state, Rectangle2D dataArea, CategoryPlot plot,
            CategoryAxis domainAxis, ValueAxis rangeAxis, CategoryDataset dataset, int row, int column, int pass) {

        if (!(dataset instanceof ViolinCategoryDataset)) {
            throw new IllegalArgumentException(
                    "ViolinRenderer.drawItem() : the data should be " + "of type ViolinCategoryDataset only.");
        }

        PlotOrientation orientation = plot.getOrientation();

        if (orientation == PlotOrientation.HORIZONTAL) {
            // TODO
            // drawHorizontalItem(g2, state, dataArea, plot, domainAxis,
        	LOGGER.fine("Horizontal not supported");
        } else if (orientation == PlotOrientation.VERTICAL) {
            drawVerticalItem(g2, state, dataArea, plot, domainAxis, rangeAxis, dataset, row, column);
        }

    }

    private Shape makeViolinPath(ViolinCategoryDataset dataset, int row, int column, CategoryItemRendererState state,
            Rectangle2D dataArea, CategoryPlot plot, CategoryAxis domainAxis, ValueAxis rangeAxis) {

    	ViolinPath vp = new ViolinPath(dataset, row, column, state,
                dataArea, plot, domainAxis, rangeAxis);

        // Get the y-values that must be used for step calculations
        Path2D shape = new Path2D.Double();

        shape.moveTo(vp.xxmid, vp.yValue(0)); // start with the lowest value at the midpoint
        
        double[] values = dataset.getPdfValues(row, column);
        
        for (int i=0; i<values.length-1; i++) {
        	double v = values[i];            
        	double xx = vp.xValueR(v);
            double yy = vp.yValue(i);
            shape.lineTo(xx, yy);
        }
        
        // The final value in the array is at yMax (see ViolinDatasetCreator::addProbabilities)
        shape.lineTo(vp.xValueR(values[values.length-1]), vp.yMax());

        // Move back to the x-midpoint
        shape.lineTo(vp.xxmid, vp.yMax());
        // Now do the same on the other side of the midpoint
        shape.lineTo(vp.xValueL(values[values.length-1]), vp.yMax());
        for (int i=values.length-2; i>=0; i--) {
        	double v = values[i];            
        	double xx = vp.xValueL(v);
            double yy = vp.yValue(i);
            shape.lineTo(xx, yy);
        }
        
        // Move back to the x-midpoint
        shape.lineTo(vp.xxmid, vp.yValue(0));
        shape.closePath();

        return shape;
    }
    
    private class ViolinPath {
    	
    	Rectangle2D dataArea;
    	RectangleEdge location;
    	ViolinCategoryDataset dataset; 
    	CategoryItemRendererState state;
        CategoryPlot plot;
        CategoryAxis domainAxis;
        ValueAxis rangeAxis;
    	
    	int row;
    	int column;
    	double xxmid;
    	double stepSize;
    	double xxrange;
    	double yValMin;
    	double yValMax;
        double maxProbability;
    	
    	public ViolinPath(ViolinCategoryDataset dataset, int row, int column, CategoryItemRendererState state,
                Rectangle2D dataArea, CategoryPlot plot, CategoryAxis domainAxis, ValueAxis rangeAxis){
    		this.dataArea = dataArea;
    		this.dataset = dataset;
    		this.row = row;
    		this.column = column;
    		this.state = state;
    		this.plot = plot;
    		this.domainAxis = domainAxis;
    		this.rangeAxis = rangeAxis;
    		yValMin = dataset.getMin(row, column); // the lowest y-value
    		yValMax = dataset.getMax(row, column);
    		location = plot.getRangeAxisEdge();
    		stepSize = calculateStepSize();
    		xxmid = calculateXMid();
    		xxrange = state.getBarWidth();
    		maxProbability = dataset.getMaxPdfValue();
    	}
    	
    	public double yValue(int i){
    		double yValPos = yValMin + (stepSize*i); 
    		return rangeAxis.valueToJava2D(yValPos, dataArea, location);
    	}
    	
    	public double yMax(){
    		return rangeAxis.valueToJava2D(yValMax, dataArea, location);
    	}
    	
    	public double xValueR(double x){
            return xxmid + xOffset(x);
    	}
    	
    	public double xValueL(double x){
            return xxmid - xOffset(x);
    	}
    	
    	/**
    	 * Calculate the offset to apply to the x midpoint to draw the
    	 * given x value
    	 * @param x
    	 * @return
    	 */
    	private double xOffset(double x) {
    		 // Express the probability as a fraction of the total bar width
            double xFractionOfRange = x/maxProbability;
         // multiply the fraction by the bar width to get x position
            return ((xxrange / 2) * xFractionOfRange);
    	}
    	
    	private double calculateStepSize() {

    		if(!dataset.hasProbabilities())
    			return 0;
    		
            double[] values = dataset.getPdfValues(row, column);
            return (yValMax - yValMin) / values.length;
        }
    	
    	private double calculateXMid(){
    		double categoryEnd = domainAxis.getCategoryEnd(column, getColumnCount(), dataArea, plot.getDomainAxisEdge());
            double categoryStart = domainAxis.getCategoryStart(column, getColumnCount(), dataArea,
                    plot.getDomainAxisEdge());
            double categoryWidth = categoryEnd - categoryStart;

            double xx = categoryStart; //
            int seriesCount = getRowCount();
            int categoryCount = getColumnCount();

            if (seriesCount > 1) {

                double seriesGap = dataArea.getWidth() * getItemMargin() / (categoryCount * (seriesCount - 1));
                double usedWidth = (state.getBarWidth() * seriesCount) + (seriesGap * (seriesCount - 1));
                // offset the start of the boxes if the total width used is smaller
                // than the category width
                double offset = (categoryWidth - usedWidth) / 2;
                xx = xx + offset + (row * (state.getBarWidth() + seriesGap));

            } else {
                // offset the start of the box if the box width is smaller than the category width
                double offset = (categoryWidth - state.getBarWidth()) / 2;
                xx = xx + offset;
            }

            return xx + state.getBarWidth() / 2d; // the middle of the active bar
    	}
    	
    }
    
    /**
     * Draws the visual representation of a single data item when the plot has a
     * vertical orientation.
     *
     * @param g2
     *            the graphics device.
     * @param state
     *            the renderer state.
     * @param dataArea
     *            the area within which the plot is being drawn.
     * @param plot
     *            the plot (can be used to obtain standard color information
     *            etc).
     * @param domainAxis
     *            the domain axis.
     * @param rangeAxis
     *            the range axis.
     * @param dataset
     *            the dataset.
     * @param row
     *            the row index (zero-based).
     * @param column
     *            the column index (zero-based).
     */
    @Override
    public void drawVerticalItem(Graphics2D g2, CategoryItemRendererState state, Rectangle2D dataArea,
            CategoryPlot plot, CategoryAxis domainAxis, ValueAxis rangeAxis, CategoryDataset dataset, int row,
            int column) {

    	if(showViolin) {
    		ViolinCategoryDataset vioDataset = (ViolinCategoryDataset) dataset;

    		if (vioDataset.hasProbabilities(row, column)) {

    			// Draw the pdf behind of the boxplot

    			Shape leftShape = makeViolinPath(vioDataset, row, column, state, dataArea, plot, domainAxis, rangeAxis);

    			Paint p = getItemPaint(row, column);
    			Color c = (Color) p;

    			Color tr = new Color(c.getRed(), c.getGreen(), c.getBlue(), 128); // make
    			// the
    			// pdf
    			// transparent
    			// version
    			// of
    			// dataset
    			// colour

    			g2.setPaint(tr);

    			Stroke s = getItemStroke(row, column);
    			g2.setStroke(s);

    			g2.fill(leftShape);
    			g2.draw(leftShape);
    		}
    	}

    	if(showBoxplot)
    		drawVerticalBoxplotItem(g2, state, dataArea, plot, domainAxis, rangeAxis, dataset, row, column);
    }

    /**
     * Draws the visual representation of a single data item when the plot has a
     * vertical orientation.
     *
     * @param g2
     *            the graphics device.
     * @param state
     *            the renderer state.
     * @param dataArea
     *            the area within which the plot is being drawn.
     * @param plot
     *            the plot (can be used to obtain standard color information
     *            etc).
     * @param domainAxis
     *            the domain axis.
     * @param rangeAxis
     *            the range axis.
     * @param dataset
     *            the dataset (must be an instance of
     *            {@link BoxAndWhiskerCategoryDataset}).
     * @param row
     *            the row index (zero-based).
     * @param column
     *            the column index (zero-based).
     */
    public void drawVerticalBoxplotItem(Graphics2D g2, CategoryItemRendererState state, Rectangle2D dataArea,
            CategoryPlot plot, CategoryAxis domainAxis, ValueAxis rangeAxis, CategoryDataset dataset, int row,
            int column) {

        BoxAndWhiskerCategoryDataset bawDataset = (BoxAndWhiskerCategoryDataset) dataset;

        double categoryEnd = domainAxis.getCategoryEnd(column, getColumnCount(), dataArea, plot.getDomainAxisEdge());
        double categoryStart = domainAxis.getCategoryStart(column, getColumnCount(), dataArea,
                plot.getDomainAxisEdge());
        double categoryWidth = categoryEnd - categoryStart;

        double xx = categoryStart;
        int seriesCount = getRowCount();
        int categoryCount = getColumnCount();

        if (seriesCount > 1) {
            double seriesGap = dataArea.getWidth() * getItemMargin() / (categoryCount * (seriesCount - 1));
            double usedWidth = (state.getBarWidth() * seriesCount) + (seriesGap * (seriesCount - 1));
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

        double yyAverage;

        Paint itemPaint = getItemPaint(row, column);
        g2.setPaint(itemPaint);
        Stroke s = getItemStroke(row, column);
        g2.setStroke(s);

        double aRadius = 0; // average radius

        RectangleEdge location = plot.getRangeAxisEdge();

        Number yQ1 = bawDataset.getQ1Value(row, column);
        Number yQ3 = bawDataset.getQ3Value(row, column);
        Number yMax = bawDataset.getMaxRegularValue(row, column);
        Number yMin = bawDataset.getMinRegularValue(row, column);
        Shape box = null;

        // Get values for the box that are half the normal box width
        double barSixth = state.getBarWidth() / 6.0;
        double barThird = barSixth * 2d;
        double xxq1 = xx + barThird;
        double xxq3 = xxq1 + barThird;

        if (yQ1 != null && yQ3 != null && yMax != null && yMin != null) {

            double yyQ1 = rangeAxis.valueToJava2D(yQ1.doubleValue(), dataArea, location);
            double yyQ3 = rangeAxis.valueToJava2D(yQ3.doubleValue(), dataArea, location);
            double yyMax = rangeAxis.valueToJava2D(yMax.doubleValue(), dataArea, location);
            double yyMin = rangeAxis.valueToJava2D(yMin.doubleValue(), dataArea, location);
            double xxmid = xx + state.getBarWidth() / 2.0;

            double halfW = (state.getBarWidth() / 2.0) * this.getWhiskerWidth();

            // draw the body...
            // box = new Rectangle2D.Double(xx, Math.min(yyQ1, yyQ3),
            // state.getBarWidth(), Math.abs(yyQ1 - yyQ3));
            box = new Rectangle2D.Double(xxq1, Math.min(yyQ1, yyQ3), barThird, Math.abs(yyQ1 - yyQ3));
            if (this.getFillBox()) {
                g2.fill(box);
            }

            Paint outlinePaint = getItemOutlinePaint(row, column);
            if (this.getUseOutlinePaintForWhiskers()) {
                g2.setPaint(outlinePaint);
            }
            // draw the upper shadow...
            g2.draw(new Line2D.Double(xxmid, yyMax, xxmid, yyQ3));
            g2.draw(new Line2D.Double(xxmid - halfW, yyMax, xxmid + halfW, yyMax));

            // draw the lower shadow...
            g2.draw(new Line2D.Double(xxmid, yyMin, xxmid, yyQ1));
            g2.draw(new Line2D.Double(xxmid - halfW, yyMin, xxmid + halfW, yyMin));

            g2.setStroke(getItemOutlineStroke(row, column));
            g2.setPaint(outlinePaint);
            g2.draw(box);
        }

        g2.setPaint(this.getArtifactPaint());

        // draw mean - SPECIAL AIMS REQUIREMENT...
        if (this.isMeanVisible()) {
            Number yMean = bawDataset.getMeanValue(row, column);
            if (yMean != null) {
                yyAverage = rangeAxis.valueToJava2D(yMean.doubleValue(), dataArea, location);
                aRadius = state.getBarWidth() / 4;
                // here we check that the average marker will in fact be
                // visible before drawing it...
                if ((yyAverage > (dataArea.getMinY() - aRadius)) && (yyAverage < (dataArea.getMaxY() + aRadius))) {
                    Ellipse2D.Double avgEllipse = new Ellipse2D.Double(xx + aRadius, yyAverage - aRadius, aRadius * 2,
                            aRadius * 2);
                    g2.fill(avgEllipse);
                    g2.draw(avgEllipse);
                }
            }
        }

        // draw median...
        if (this.isMedianVisible()) {
            Number yMedian = bawDataset.getMedianValue(row, column);
            if (yMedian != null) {
                double yyMedian = rangeAxis.valueToJava2D(yMedian.doubleValue(), dataArea, location);
                g2.draw(new Line2D.Double(xxq1, yyMedian, xxq3, yyMedian));
            }
        }

        // collect entity and tool tip information...
        if (state.getInfo() != null && box != null) {
            EntityCollection entities = state.getEntityCollection();
            if (entities != null) {
                addItemEntity(entities, dataset, row, column, box);
            }
        }

    }

}