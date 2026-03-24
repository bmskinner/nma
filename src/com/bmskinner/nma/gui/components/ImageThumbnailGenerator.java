package com.bmskinner.nma.gui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.entity.XYItemEntity;

import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.signals.INuclearSignal;
import com.bmskinner.nma.io.ImageImporter;
import com.bmskinner.nma.visualisation.datasets.ComponentXYDataset;
import com.bmskinner.nma.visualisation.datasets.NuclearSignalXYDataset;
import com.bmskinner.nma.visualisation.image.ImageAnnotator;

import ij.process.ImageProcessor;

/**
 * Display an image thumbnail for a cellular component when the appropriate
 * point in an XY chart is hovered over. Usage on a JFreeChart ChartPanel
 * displaying a ComponentXYDataset:
 * {@code chartPanel.addChartMouseListener(new ImageThumbnailGenerator(chartPanel));}
 * 
 * @author Ben Skinner
 * @since 1.16.0
 *
 */
public class ImageThumbnailGenerator implements ChartMouseListener {

	private static final Logger LOGGER = Logger.getLogger(ImageThumbnailGenerator.class.getName());

	private final ChartPanel chartPanel;
	private XYItemEntity currentEntity = null; // allow chart to repaint whenever entity changes
	private Rectangle prevEntityDrawBounds = null;

	/**
	 * Create a thumbnail generator for the given chart panel. Specify if the
	 * thumbnails should be greyscale or RGB colour.
	 * 
	 * @param chartPanel the chart panel to draw on
	 */
	public ImageThumbnailGenerator(final @NonNull ChartPanel chartPanel) {
		this.chartPanel = chartPanel;
	}

	@Override
	public void chartMouseClicked(ChartMouseEvent event) {
		// do nothing
	}

	@Override
	public void chartMouseMoved(ChartMouseEvent event) { // display thumbnail of nucleus

		if (!(event.getEntity() instanceof XYItemEntity)) {
			currentEntity = null;
			chartPanel.repaint(); // clear the chart
			return;
		}
		final XYItemEntity entity = (XYItemEntity) event.getEntity();

		if (entity == currentEntity) // no unnecessary updates needed
			return;

		// We have a new entity. Repaint the previous draw bounds if different
		// Paint now, not using repaint so we do not get out of order painting with the
		// new annotation
		if (prevEntityDrawBounds != null & entity != null) {
			chartPanel.paintImmediately(prevEntityDrawBounds);
		}
		currentEntity = entity;

		// only use datasets of the desired class
		if (!(entity.getDataset() instanceof ComponentXYDataset))
			return;

		final ComponentXYDataset<? extends CellularComponent> ds = (ComponentXYDataset<? extends CellularComponent>) entity
				.getDataset();

		final String key = ds.getSeriesKey(entity.getSeriesIndex()).toString();
		final CellularComponent n = ds.getComponent(key, entity.getItem());

		// May not be an object at this location
		if (n == null)
			return;

		// Find the screen coordinates of the XYpoint. We want to draw the
		// annotation from the centre of the point, no matter where the mouse touched
		// it.
		final int screenX = entity.getArea().getBounds().x - (entity.getArea().getBounds().width / 2);
		final int screenY = entity.getArea().getBounds().y - (entity.getArea().getBounds().height / 2);

		if (n instanceof final Nucleus nuc) {
			if (ds instanceof final NuclearSignalXYDataset ns) {
				drawSignal(nuc, ns.getSignal(key, entity.getItem()), screenX, screenY);
			} else {
				drawNucleus(nuc, screenX, screenY);
			}

		}

	}

	private void drawSignal(Nucleus n, INuclearSignal ns, int x, int y) {
		final String labelText = n.getNameAndNumber();

		final Color nucleusColour = Color.WHITE;
		final Color signalColour = Color.ORANGE;

		ImageProcessor ip = ImageImporter.importFullImageTo24bitGreyscale(n);

		final ImageAnnotator an = new ImageAnnotator(ip).drawBorder(n, nucleusColour).drawBorder(ns,
				signalColour);
		an.crop(n);
		an.resizeKeepingAspect(150, 150);
		ip = an.toProcessor();

		final Graphics2D g2 = (Graphics2D) chartPanel.getGraphics();

		// ensure the image is positioned within the bounds of the chart panel
		final int topStart = y + ip.getHeight() > chartPanel.getHeight() ? y - ip.getHeight() : y;
		final int leftStart = x + ip.getWidth() > chartPanel.getWidth() ? x - ip.getWidth() : x;
		prevEntityDrawBounds = new Rectangle(leftStart - 10, topStart - 10, ip.getWidth() + 20, ip.getHeight() + 20);

		g2.drawImage(ip.createImage(), leftStart, topStart, ip.getWidth(), ip.getHeight(), null);
		final Color c = g2.getColor();

		final Color textColour = Color.WHITE;
		g2.setColor(textColour);
		g2.drawString(labelText, leftStart + 4, topStart + ip.getHeight() - 4);
		g2.setColor(Color.DARK_GRAY);
		g2.setStroke(new BasicStroke(3));
		g2.drawRect(leftStart, topStart, ip.getWidth(), ip.getHeight());
		g2.setColor(c);
	}

	private void drawNucleus(Nucleus n, int x, int y) {
		final String labelText = n.getNameAndNumber();

		final Color annotationColour = Color.ORANGE;
		ImageProcessor ip = ImageImporter.importFullImageTo24bitGreyscale(n);

		final ImageAnnotator an = new ImageAnnotator(ip).drawBorder(n, annotationColour);
		an.crop(n);
		an.resizeKeepingAspect(150, 150);
		ip = an.toProcessor();

		final Graphics2D g2 = (Graphics2D) chartPanel.getGraphics();

		// ensure the image is positioned within the bounds of the chart panel
		final int topStart = y + ip.getHeight() > chartPanel.getHeight() ? y - ip.getHeight() : y;
		final int leftStart = x + ip.getWidth() > chartPanel.getWidth() ? x - ip.getWidth() : x;

		prevEntityDrawBounds = new Rectangle(leftStart - 10, topStart - 10, ip.getWidth() + 20, ip.getHeight() + 20);

		g2.drawImage(ip.createImage(), leftStart, topStart, ip.getWidth(), ip.getHeight(), null);
		final Color c = g2.getColor();

		final Color textColour = Color.ORANGE;
		g2.setColor(textColour);
		g2.drawString(labelText, leftStart + 4, topStart + ip.getHeight() - 4);
		g2.setColor(Color.DARK_GRAY);
		g2.setStroke(new BasicStroke(3));
		g2.drawRect(leftStart, topStart, ip.getWidth(), ip.getHeight());
		g2.setColor(c);
	}

}
