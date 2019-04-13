package com.bmskinner.nuclear_morphology.gui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.ui.RectangleEdge;

import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.charting.datasets.ComponentXYDataset;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.process.ImageProcessor;

/**
 * Display an image thumbnail for a cellular component when the appropriate point in an XY chart
 * is hovered over. Usage on a JFreeChart ChartPanel displaying a ComponentXYDataset: 
 * {@code chartPanel.addChartMouseListener(new ImageThumbnailGenerator(chartPanel));}
 * @author bms41
 * @since 1.16.0
 *
 */
public class ImageThumbnailGenerator implements ChartMouseListener {
	
	private static final Logger LOGGER = Logger.getLogger(Loggable.ROOT_LOGGER);
	
	private final ChartPanel chartPanel;
	private XYItemEntity currentEntity = null; // allow chart to repaint whenever entity changes
	
	public ImageThumbnailGenerator(final @NonNull ChartPanel chartPanel) {
		this.chartPanel = chartPanel;
	}

	@Override
	public void chartMouseClicked(ChartMouseEvent event) {
		//do something on mouse click
//		System.out.println("Entity clicked: " + event.getEntity());
	}

	@Override
	public void chartMouseMoved(ChartMouseEvent event) { // display thumbnail of nucleus
		
		if( !(event.getEntity() instanceof XYItemEntity) ) {
			chartPanel.repaint(); // clear the chart
			return;
		}
		XYItemEntity entity = (XYItemEntity) event.getEntity();
		
		if(entity==currentEntity)
			return;

		currentEntity = entity;
		
		if(!(entity.getDataset() instanceof ComponentXYDataset)) // only use datasets of the desired class 
			return;
		
		ComponentXYDataset<? extends CellularComponent> ds = (ComponentXYDataset<? extends CellularComponent>) entity.getDataset();

		String key = ds.getSeriesKey(entity.getSeriesIndex()).toString();
		CellularComponent n = ds.getComponent(key, entity.getItem());
		
		// Draw at the entity coordinates, not the mouse position
		double entityX = ds.getXValue(entity.getSeriesIndex(), entity.getItem());
		double entityY = ds.getXValue(entity.getSeriesIndex(), entity.getItem());
		Rectangle2D dataArea = chartPanel.getScreenDataArea();
		int screenX = (int) chartPanel.getChart().getXYPlot().getDomainAxis().valueToJava2D(entityX, dataArea, RectangleEdge.BOTTOM);
		int screenY = (int) chartPanel.getChart().getXYPlot().getRangeAxis().valueToJava2D(entityY, dataArea, RectangleEdge.LEFT);

		if(n==null)
			return;
		
		try {
			ImageProcessor ip = n.getComponentRGBImage();
			ip = new ImageFilterer(ip).resizeKeepingAspect(150, 150).toProcessor();

			Graphics2D g2  = (Graphics2D) chartPanel.getGraphics();
			
			// ensure the image is positioned within the bounds of the chart panel
			int topStart = screenY+ip.getHeight()>chartPanel.getHeight() ? screenY-ip.getHeight() : screenY;
			int leftStart = screenX+ip.getWidth()>chartPanel.getWidth() ? screenX-ip.getWidth() : screenX;
			
			g2.drawImage(ip.createImage(), leftStart, topStart, ip.getWidth(), ip.getHeight(), null);
			Color c = g2.getColor();
			g2.setColor(Color.WHITE);
			g2.drawString(n.getSourceFileName(), leftStart+4, topStart+ip.getHeight()-4);
			g2.setColor(Color.DARK_GRAY);
			g2.setStroke(new BasicStroke(3));
			g2.drawRect(leftStart, topStart, ip.getWidth(), ip.getHeight());
			g2.setColor(c);
		} catch(UnloadableImageException e) {
			LOGGER.log(Loggable.STACK, "Error making image thumbnail", e);
		}
	}

}
