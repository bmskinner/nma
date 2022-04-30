package com.bmskinner.nma.gui.components.panels;

import java.awt.BorderLayout;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.bmskinner.nma.components.generic.FloatPoint;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.gui.ImageClickListener;
import com.bmskinner.nma.utility.NumberTools;
import com.bmskinner.nma.visualisation.image.ImagePainter;

/**
 * A panel that can hold images. The images can be 
 * magnified on mouse-over
 * @author ben
 * @since 2.0.0
 *
 */
public class MagnifiableImagePanel extends JPanel {
	
	private static final Logger LOGGER = Logger.getLogger(MagnifiableImagePanel.class.getName());
	
	/** Stores the image displayed on screen */
	private JLabel imageLabel = new JLabel();
	
	/** The image with no decorations*/
	private BufferedImage raw;
	
	/** The display image with decorations*/
	private BufferedImage annotated;
	
	/** The raw image enlarged for bulge selection */
	private BufferedImage enlarged;
	
	/** The final image displayed in the panel */
	private BufferedImage output;

	/**	 The radius of the source box for bulge images */
	private int smallRadius = 25;
	
	/**	 The radius of the display box for bulge images */
	private int bigRadius   = 50;
	
	private ImagePainter painter;
	
	/** Listeners for cell clicks */
	private final List<ImageClickListener> cellClickListeners = new ArrayList<>();
	
	
	/**
	 * Create a new panel
	 */
	public MagnifiableImagePanel() {		
		setLayout(new BorderLayout());
		imageLabel = new JLabel();
		imageLabel.setHorizontalAlignment(JLabel.CENTER);
		imageLabel.setVerticalAlignment(JLabel.CENTER);
		imageLabel.setHorizontalTextPosition(JLabel.CENTER);
		imageLabel.setVerticalTextPosition(JLabel.CENTER);
		
		ImageMagnificationAdapter adapter = new ImageMagnificationAdapter();
		imageLabel.addMouseWheelListener(adapter);
		imageLabel.addMouseMotionListener(adapter);
		imageLabel.addMouseListener(new ImageClickAdapter());
		add(imageLabel, BorderLayout.CENTER);
	}
	
	/**
	 * Update the image displayed in this panel
	 * @param image
	 * @param painter
	 */
	public void set(ImagePainter painter) {
		this.painter = painter;
		raw 		= painter.paintRaw(this.getWidth(), this.getHeight());
		annotated   = painter.paintDecorated(this.getWidth(), this.getHeight());
		enlarged = createEnlargedImage();
		output      = annotated;
		imageLabel.setIcon(new ImageIcon(output));
		repaint();
	}
	
	
	/**
	 * Add a listener for cell click events
	 * @param l
	 */
	public void addImageClickListener(ImageClickListener l) {
		cellClickListeners.add(l);
	}
	
	/**
	 * Remove a listener for cell click events
	 * @param l
	 */
	public void removeImageClickListener(ImageClickListener l) {
		cellClickListeners.remove(l);
	}
	
	/**
	 * Create a version of the raw image enlarged for painting
	 * outline details. The size of the enlarged image is determined
	 * by the ratio of the large and small radii for the bulge window.	 * 
	 */
	private BufferedImage createEnlargedImage() {
		double bulgeRatio = (double)bigRadius/(double)smallRadius;
		int w = (int) (raw.getWidth()*bulgeRatio);
		int h = (int) (raw.getHeight()*bulgeRatio);
		// A large annotated image that can be sampled for the bulge box
		return painter.paintDecorated(w, h);
	}
	
		
	/**
	 * Create the final image for display, with any magnifications
	 * @param x the x coordinate of the mouse over the image
	 * @param y the y coordinate of the mouse over the image
	 * @return
	 */
	protected BufferedImage createOutputImage(int x, int y) {
		// Create the bulge box
		return painter.paintMagnified(annotated, enlarged, x, y, smallRadius, bigRadius);
	}
	
	
	
	
	/**
	 * Update the rendered image with the mouse at the given location
	 * @param x
	 * @param y
	 */
	public synchronized void updateImage(int x, int y) {
		output = createOutputImage(x, y);
		imageLabel.setIcon(new ImageIcon(output));
		repaint();
	}
	
	private void fireImageClicked(int x, int y) {
		for(ImageClickListener l : cellClickListeners) {
			l.imageClicked(x, y);
		}
	}
	
	/**
	 * Respond to mouse scroll wheel input
	 * @author bms41
	 * @since 2.0.0
	 *
	 */
	private class ImageClickAdapter extends MouseAdapter {
		@Override
		public void mouseReleased(MouseEvent e) {

			// Gives position within the imageLabel
			int x = e.getX();
			int y = e.getY();
			
//			LOGGER.fine("Position in imageLabel: "+x+" "+y);
			
			IPoint p = translatePanelLocationToSourceImage(x, y);
//			LOGGER.fine("Position in source image "+p.getX()+" "+p.getY());
			
			fireImageClicked(p.getXAsInt(), p.getYAsInt());
		}
	}
	
		
	/**
	 * Respond to mouse scroll wheel input
	 * @author bms41
	 * @since 2.0.0
	 *
	 */
	private class ImageMagnificationAdapter extends MouseAdapter {
		
		private static final int SMALL_MULTIPLIER = 1;
		private static final int LARGE_MULTIPLIER = 3;

		/** Minimum radius of the zoomed image */
		private static final int SMALL_MIN_RADIUS = 5;
		private static final int SMALL_MAX_RADIUS = 100;
		
		private static final int LARGE_MIN_RADIUS = 10;
		private static final int LARGE_MAX_RADIUS = 200;
		
		
		@Override
		public synchronized void mouseWheelMoved(MouseWheelEvent e) {
			if(imageLabel.getIcon()==null)
				return;
			if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) ==
					InputEvent.CTRL_DOWN_MASK){
				int temp = smallRadius +( SMALL_MULTIPLIER*e.getWheelRotation());
				smallRadius = NumberTools.constrain(temp, SMALL_MIN_RADIUS, SMALL_MAX_RADIUS);
			} else {
				int temp = bigRadius +( LARGE_MULTIPLIER * e.getWheelRotation());
				bigRadius = NumberTools.constrain(temp, LARGE_MIN_RADIUS, LARGE_MAX_RADIUS);
			}
			IPoint p = translatePanelLocationToRenderedImage(e); 
			updateImage(p.getXAsInt(), p.getYAsInt());
		}
		
		@Override
		public synchronized void mouseMoved(MouseEvent e){
			if(imageLabel.getIcon()==null)
				return;
			IPoint p = translatePanelLocationToRenderedImage(e); 
			updateImage(p.getXAsInt(), p.getYAsInt());
		}
	}
	
	
	
	/**
	 * Translate the given point from a location within this JPanel
	 * to a location within the original cell source image.
	 * @param x the location relative to this panel
	 * @param y the location relative to this panel
	 * @return
	 */
	private synchronized IPoint translatePanelLocationToSourceImage(int x, int y) {
		// The input image dimensions - scaled to fit the panel
		int w = raw.getWidth();
		int h = raw.getHeight();
		
		// The dimensions of the image in the icon; should be the same
		int iconWidth = imageLabel.getIcon().getIconWidth();
		int iconHeight = imageLabel.getIcon().getIconHeight();
		
		// The image panel dimensions
		int panelWidth = getWidth();
		int panelHeight = getHeight();
		
		// The position of the click relative to the icon
		int iconX = x-((panelWidth-iconWidth)/2);
		int iconY = y-((panelHeight-iconHeight)/2);
		
		// The position  of the click within the original image
		double xPositionInImage = (((double)iconX/(double) iconWidth)*w);
		double yPositionInImage = (((double)iconY/(double) iconHeight)*h);
		return new FloatPoint(xPositionInImage, yPositionInImage);
	}
		
	/**
	 * Translate the given point from a location within this JPanel
	 * to a location within the rendered image.
	 * @param e the event with coordinates
	 * @return
	 */
	private synchronized IPoint translatePanelLocationToRenderedImage(MouseEvent e) {
		// The rescaled dimensions
		if(imageLabel==null)
			return null;
		int iconWidth = imageLabel.getIcon().getIconWidth();
		int iconHeight = imageLabel.getIcon().getIconHeight();
		
		// The image panel dimensions
		int panelWidth = getWidth();
		int panelHeight = getHeight();
		
		// The position of the click relative to the icon
		int iconX = e.getX()-((panelWidth-iconWidth)/2);
		int iconY = e.getY()-((panelHeight-iconHeight)/2);
		return new FloatPoint(iconX, iconY);
	}	
}
