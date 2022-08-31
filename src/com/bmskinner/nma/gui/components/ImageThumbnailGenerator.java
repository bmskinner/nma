package com.bmskinner.nma.gui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
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
 * @author bms41
 * @since 1.16.0
 *
 */
public class ImageThumbnailGenerator implements ChartMouseListener {

	private static final Logger LOGGER = Logger.getLogger(ImageThumbnailGenerator.class.getName());

	private final ChartPanel chartPanel;
	private XYItemEntity currentEntity = null; // allow chart to repaint whenever entity changes

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
			chartPanel.repaint(); // clear the chart
			currentEntity = null;
			return;
		}
		XYItemEntity entity = (XYItemEntity) event.getEntity();

		if (entity == currentEntity) // no unnecessary updates needed
			return;

		currentEntity = entity;

		if (!(entity.getDataset() instanceof ComponentXYDataset)) // only use datasets of the
																	// desired class
			return;

		ComponentXYDataset<? extends CellularComponent> ds = (ComponentXYDataset<? extends CellularComponent>) entity
				.getDataset();

		String key = ds.getSeriesKey(entity.getSeriesIndex()).toString();
		CellularComponent n = ds.getComponent(key, entity.getItem());
		if (n == null)
			return;

		// Draw at mouse position
		int screenX = event.getTrigger().getX();
		int screenY = event.getTrigger().getY();

		if (n instanceof Nucleus nuc) {
			if (ds instanceof NuclearSignalXYDataset ns) {
				drawSignal(nuc, ns.getSignal(key, entity.getItem()), screenX, screenY);
			} else {
				drawNucleus(nuc, screenX, screenY);
			}

		}

	}

	private void drawSignal(Nucleus n, INuclearSignal ns, int x, int y) {
		String labelText = n.getNameAndNumber();

		Color nucleusColour = Color.WHITE;
		Color signalColour = Color.ORANGE;

		ImageProcessor ip = ImageImporter.importFullImageTo24bitGreyscale(n);

		ImageAnnotator an = new ImageAnnotator(ip).drawBorder(n, nucleusColour).drawBorder(ns,
				signalColour);
		an.crop(n);
		an.resizeKeepingAspect(150, 150);
		ip = an.toProcessor();

		Graphics2D g2 = (Graphics2D) chartPanel.getGraphics();

		// ensure the image is positioned within the bounds of the chart panel
		int topStart = y + ip.getHeight() > chartPanel.getHeight() ? y - ip.getHeight() : y;
		int leftStart = x + ip.getWidth() > chartPanel.getWidth() ? x - ip.getWidth() : x;

		g2.drawImage(ip.createImage(), leftStart, topStart, ip.getWidth(), ip.getHeight(), null);
		Color c = g2.getColor();

		Color textColour = Color.WHITE;
		g2.setColor(textColour);
		g2.drawString(labelText, leftStart + 4, topStart + ip.getHeight() - 4);
		g2.setColor(Color.DARK_GRAY);
		g2.setStroke(new BasicStroke(3));
		g2.drawRect(leftStart, topStart, ip.getWidth(), ip.getHeight());
		g2.setColor(c);
	}

	private void drawNucleus(Nucleus n, int x, int y) {
		String labelText = n.getNameAndNumber();

		Color annotationColour = Color.WHITE;
		ImageProcessor ip = ImageImporter.importFullImageTo24bitGreyscale(n);

		ImageAnnotator an = new ImageAnnotator(ip).drawBorder(n, annotationColour);
		an.crop(n);
		an.resizeKeepingAspect(150, 150);
		ip = an.toProcessor();

		Graphics2D g2 = (Graphics2D) chartPanel.getGraphics();

		// ensure the image is positioned within the bounds of the chart panel
		int topStart = y + ip.getHeight() > chartPanel.getHeight() ? y - ip.getHeight() : y;
		int leftStart = x + ip.getWidth() > chartPanel.getWidth() ? x - ip.getWidth() : x;

		g2.drawImage(ip.createImage(), leftStart, topStart, ip.getWidth(), ip.getHeight(), null);
		Color c = g2.getColor();

		Color textColour = Color.WHITE;
		g2.setColor(textColour);
		g2.drawString(labelText, leftStart + 4, topStart + ip.getHeight() - 4);
		g2.setColor(Color.DARK_GRAY);
		g2.setStroke(new BasicStroke(3));
		g2.drawRect(leftStart, topStart, ip.getWidth(), ip.getHeight());
		g2.setColor(c);
	}

}
