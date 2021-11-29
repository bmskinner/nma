package com.bmskinner.nuclear_morphology.gui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.entity.XYItemEntity;

import com.bmskinner.nuclear_morphology.analysis.image.ImageAnnotator;
import com.bmskinner.nuclear_morphology.charting.datasets.ComponentXYDataset;
import com.bmskinner.nuclear_morphology.components.Imageable;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;

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
	
	private static final Logger LOGGER = Logger.getLogger(ImageThumbnailGenerator.class.getName());
	
	public static final boolean COLOUR_RGB = true;
	public static final boolean COLOUR_GREYSCALE = false;
	
	private final ChartPanel chartPanel;
	private XYItemEntity currentEntity = null; // allow chart to repaint whenever entity changes
	private boolean isRgb = COLOUR_GREYSCALE; // should we draw the RGB image or greyscale?
	
	/**
	 * Create a thumbnail generator for the given chart panel. Specify if the 
	 * thumbnails should be greyscale or RGB colour.
	 * @param chartPanel the chart panel to draw on
	 * @param isRgb true if the thumbnails should be RGB, false otherwise
	 */
	public ImageThumbnailGenerator(final @NonNull ChartPanel chartPanel, boolean isRgb) {
		this.chartPanel = chartPanel;
		this.isRgb = isRgb;
	}

	@Override
	public void chartMouseClicked(ChartMouseEvent event) {
		// do nothing
	}

	@Override
	public void chartMouseMoved(ChartMouseEvent event) { // display thumbnail of nucleus
		
		if( !(event.getEntity() instanceof XYItemEntity) ) {
			chartPanel.repaint(); // clear the chart
			currentEntity=null;
			return;
		}
		XYItemEntity entity = (XYItemEntity) event.getEntity();
		
		if(entity==currentEntity) // no unnecessary updates needed
			return;

		currentEntity = entity;
		
		if(!(entity.getDataset() instanceof ComponentXYDataset)) // only use datasets of the desired class 
			return;
		
		ComponentXYDataset<? extends CellularComponent> ds = (ComponentXYDataset<? extends CellularComponent>) entity.getDataset();

		String key = ds.getSeriesKey(entity.getSeriesIndex()).toString();
		CellularComponent n = ds.getComponent(key, entity.getItem());

		// Draw at mouse position
		int screenX = event.getTrigger().getX();
		int screenY = event.getTrigger().getY();

		if(n==null)
			return;

		Color annotationColour = isRgb ? Color.WHITE : Color.ORANGE;
		ImageProcessor ip;
		try {
			ip = isRgb ? n.getRGBImage() : n.getImage();
		} catch(UnloadableImageException e) {
			LOGGER.fine("Unable to load component image: "+e.getMessage());
			// No image, but we can still draw the outline
			ip = isRgb ? ImageAnnotator.createBlackColorProcessor(n.getWidth()+Imageable.COMPONENT_BUFFER*2, 
					n.getHeight()+Imageable.COMPONENT_BUFFER*2) :
						ImageAnnotator.createWhiteColorProcessor(n.getWidth()+Imageable.COMPONENT_BUFFER*2, 
								n.getHeight()+Imageable.COMPONENT_BUFFER*2);
		}


		ImageAnnotator an = new ImageAnnotator(ip)
				.drawBorder(n, annotationColour);
//				.annotateOutlineOnCroppedComponent(n, annotationColour, 3);
		an.crop(n);
		an.resizeKeepingAspect(150, 150);
		ip = an.toProcessor();


		Graphics2D g2  = (Graphics2D) chartPanel.getGraphics();

		// ensure the image is positioned within the bounds of the chart panel
		int topStart = screenY+ip.getHeight()>chartPanel.getHeight() ? screenY-ip.getHeight() : screenY;
		int leftStart = screenX+ip.getWidth()>chartPanel.getWidth() ? screenX-ip.getWidth() : screenX;

		g2.drawImage(ip.createImage(), leftStart, topStart, ip.getWidth(), ip.getHeight(), null);
		Color c = g2.getColor();

		Color textColour = isRgb ? Color.WHITE : Color.BLACK;
		g2.setColor(textColour);
		g2.drawString(n.getSourceFileName(), leftStart+4, topStart+ip.getHeight()-4);
		g2.setColor(Color.DARK_GRAY);
		g2.setStroke(new BasicStroke(3));
		g2.drawRect(leftStart, topStart, ip.getWidth(), ip.getHeight());
		g2.setColor(c);
	}

}
