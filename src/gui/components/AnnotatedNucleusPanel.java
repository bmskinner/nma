package gui.components;

import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import io.ImageExporter;
import io.ImageImporter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import utility.Utils;
import components.Cell;
import components.nuclei.Nucleus;

/**
 * Display the original image for a cell, with
 * the nucleus outlines drawn on it. 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class AnnotatedNucleusPanel extends JPanel {
	
	private Cell cell;
	private JLabel imageLabel = new JLabel();
	private Logger programLogger;
	
	public AnnotatedNucleusPanel(Logger programLogger){
//		this.cell = cell;
		this.programLogger = programLogger;
		this.setLayout(new BorderLayout());
		this.add(imageLabel, BorderLayout.CENTER);
		
	}
	
	public void updateCell(Cell c) throws Exception {
		this.cell = c;
		importNucleusImage();
	}
	
	private void importNucleusImage() throws Exception {
		File imageFile = cell.getNucleus().getSourceFile();
		ImageStack imageStack = ImageImporter.importImage(imageFile, programLogger);
		
		// Get the counterstain stack, make greyscale and invert
		ImageProcessor openProcessor = ImageExporter.makeGreyRGBImage(imageStack).getProcessor();
		openProcessor.invert();
		
		drawNucleus(openProcessor);
		
		
		
		ImageIcon icon = null;
		if(imageLabel.getIcon()!=null){
			icon = (ImageIcon) imageLabel.getIcon();
			icon.getImage().flush();
		}
		icon = createViewableImage(openProcessor);
		imageLabel.setIcon(icon);
		imageLabel.revalidate();
		imageLabel.repaint();
	}
	
	private void annotateImage(){
		
	}
	
	/**
	 * Draw the outline of a nucleus on the given processor
	 * @param cell
	 * @param ip
	 */
	private void drawNucleus(ImageProcessor ip) throws Exception {
		if(cell==null){
			throw new IllegalArgumentException("Input cell is null");
		}
		
		Nucleus n = cell.getNucleus();
		// annotate the image processor with the nucleus outline
		

		ip.setColor(Color.ORANGE);

		
		
		double[] positions = n.getPosition();
		FloatPolygon polygon = Utils.createPolygon(n.getBorderList());
		PolygonRoi roi = new PolygonRoi(polygon, PolygonRoi.POLYGON);
		roi.setLocation(positions[Nucleus.X_BASE], positions[Nucleus.Y_BASE]);
		ip.setLineWidth(2);
		ip.draw(roi);
	}
	
	/**
	 * Rezize the given image processor to fit in the screen,
	 * and make an icon
	 * @param ip an image processor
	 * @return an image icon with the resized image
	 */
	private ImageIcon createViewableImage(ImageProcessor ip){
		int originalWidth = ip.getWidth();
		int originalHeight = ip.getHeight();
		
		// The panel dimension
		Dimension screenSize = this.getSize();
		
		
		// set the image width to be less than half the screen width
		int smallWidth = (int) ((double) screenSize.getWidth() );
		
		
		// keep the image aspect ratio
		double ratio = (double) originalWidth / (double) originalHeight;
		int smallHeight = (int) (smallWidth / ratio);
		

		if(smallHeight > screenSize.getHeight()  ){ // image is too high, adjust to scale on height
			smallHeight = (int) screenSize.getHeight();
			smallWidth = (int) (smallHeight * ratio);
		}
		
		
		
		
		// Create the image
		
		ImageIcon smallImageIcon;

		if(ip.getWidth()>smallWidth || ip.getHeight() > smallHeight){
			
			smallImageIcon = new ImageIcon(ip.resize(smallWidth, smallHeight ).getBufferedImage());
			
		} else {
			
			smallImageIcon = new ImageIcon( ip.getBufferedImage()  );
		}
		return smallImageIcon;
	}

}
