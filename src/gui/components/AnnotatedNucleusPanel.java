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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import charting.charts.MorphologyChartFactory;
import utility.Utils;
import components.Cell;
import components.nuclear.NucleusBorderPoint;
import components.nuclear.NucleusBorderSegment;
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

		
		this.programLogger = programLogger;
		this.setLayout(new BorderLayout());
		this.add(imageLabel, BorderLayout.CENTER);
		imageLabel.setHorizontalTextPosition(JLabel.CENTER);
		imageLabel.setVerticalTextPosition(JLabel.BOTTOM);
		imageLabel.setVerticalAlignment(JLabel.CENTER);
		imageLabel.setHorizontalAlignment(JLabel.CENTER);

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
		this.setSize(icon.getIconWidth(), icon.getIconHeight());
		imageLabel.setIcon(icon);
		imageLabel.revalidate();
		imageLabel.repaint();
		
		this.repaint();
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
		double[] positions = n.getPosition();

		
		// annotate the image processor with the nucleus outline
		List<NucleusBorderSegment> segmentList = n.getAngleProfile().getSegments();
		
		ip.setLineWidth(2);
		if(!segmentList.isEmpty()){ // only draw if there are segments
			
			for(NucleusBorderSegment seg  : segmentList){
				
				float[] x = new float[seg.length()+1];
				float[] y = new float[seg.length()+1];
				
				
				for(int j=0; j<=seg.length();j++){
					int k = Utils.wrapIndex(seg.getStartIndex()+j, n.getLength());
					NucleusBorderPoint p = n.getBorderPoint(k); // get the border points in the segment
					x[j] = (float) p.getX();
					y[j] = (float) p.getY();
				}
				
				int segIndex = MorphologyChartFactory.getIndexFromLabel (seg.getName());
				ip.setColor(ColourSelecter.getSegmentColor(segIndex));
				
				PolygonRoi segRoi = new PolygonRoi(x, y, PolygonRoi.POLYLINE);
				
				segRoi.setLocation(segRoi.getBounds().getMinX()+positions[Nucleus.X_BASE], segRoi.getBounds().getMinY()+positions[Nucleus.Y_BASE]);
				
				ip.draw(segRoi);

			}
		} else {

			ip.setColor(Color.ORANGE);
			FloatPolygon polygon = Utils.createPolygon(n.getBorderList());
			PolygonRoi roi = new PolygonRoi(polygon, PolygonRoi.POLYGON);
			roi.setLocation(positions[Nucleus.X_BASE], positions[Nucleus.Y_BASE]);
			ip.draw(roi);
		}

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
		
		// Choose a clip for the image (an enlargement of the original nucleus ROI
		double[] positions = cell.getNucleus().getPosition();
		int wideW = (int) (positions[Nucleus.WIDTH]+20);
		int wideH = (int) (positions[Nucleus.HEIGHT]+20);
		int wideX = (int) (positions[Nucleus.X_BASE]-10);
		int wideY = (int) (positions[Nucleus.Y_BASE]-10);

		wideX = wideX<0 ? 0 : wideX;
		wideY = wideY<0 ? 0 : wideY;

		ip.setRoi(wideX, wideY, wideW, wideH);
		ImageProcessor croppedProcessor = ip.crop();
		
		// The panel dimension
		Dimension screenSize = new Dimension(croppedProcessor.getWidth(), croppedProcessor.getHeight());
		
		
//		// set the image width to be less than half the screen width
//		int smallWidth = (int) ((double) screenSize.getWidth() );
//		
//		
//		// keep the image aspect ratio
//		double ratio = (double) originalWidth / (double) originalHeight;
//		int smallHeight = (int) (smallWidth / ratio);
//		
//
//		if(smallHeight > screenSize.getHeight()  ){ // image is too high, adjust to scale on height
//			smallHeight = (int) screenSize.getHeight();
//			smallWidth = (int) (smallHeight * ratio);
//		}
		
		// Create the image
		
		
		
		
		ImageIcon smallImageIcon;

//		if(croppedProcessor.getWidth()>smallWidth || croppedProcessor.getHeight() > smallHeight){
//			
//			smallImageIcon = new ImageIcon(croppedProcessor.resize(smallWidth, smallHeight ).getBufferedImage());
//			
//		} else {
			
			smallImageIcon = new ImageIcon( croppedProcessor.getBufferedImage()  );
//		}
		return smallImageIcon;
	}

}
