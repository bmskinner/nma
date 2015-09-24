package no.gui;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import utility.Logger;
import utility.Utils;
import cell.Cell;
import no.analysis.NucleusDetector;
import no.analysis.NucleusFinder;
import no.components.AnalysisOptions;
import no.export.ImageExporter;
import no.imports.ImageImporter;
import no.nuclei.Nucleus;

@SuppressWarnings("serial")
public class ImageProber extends JDialog {
	
	Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();

	private final JPanel contentPanel = new JPanel();
	private AnalysisOptions options;
	private File openImage;
	private Logger logger;
	private JLabel imageLabel;
	private JPanel imagePane;
	private ImageIcon imageIcon;
	private JLabel headerLabel;
	
	private ImageIcon loadingGif = null;
	
	private ImageIcon blankIcon ;
	private boolean ok = false;

	/**
	 * Create the dialog.
	 */
	public ImageProber(AnalysisOptions options, File logFile) {
		this.setModal(true);
		this.options = options;
		this.logger = new Logger(logFile, "ImageProber");
		this.setTitle("Image Prober");
		
		try{
			loadingGif = new ImageIcon(this.getClass().getResource("/ajax-loader.gif"));
		} catch (Exception e){
			IJ.log("Cannot load gif resource: "+e.getMessage());
		}
		if(loadingGif==null){
			IJ.log("Unable to load gif");
		}

		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setLayout(new FlowLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("Proceed with analysis");
				okButton.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent arg0) {
						ImageProber.this.ok = true;
						ImageProber.this.setVisible(false);

					}
				});
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Revise settings");
				cancelButton.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent arg0) {
						
						ImageProber.this.ok = false;
						ImageProber.this.setVisible(false);

					}
				});
				buttonPane.add(cancelButton);
			}
			{
				JButton nextButton = new JButton("Next");
				nextButton.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent arg0) {
						
						final File f = getNextImage();
						
						Thread thr = new Thread(){
							 public void run() {
								 importAndDisplayImage(f);
							 }
						 };	
						 thr.start();

					}
				});
				getContentPane().add(nextButton, BorderLayout.EAST);
			}
		}
		
		imagePane = new JPanel();
		imagePane.setLayout(new BorderLayout());
		
		double w = screenSize.getWidth() * 0.75;
		double h = 600;
		
		blankIcon = new ImageIcon(new BufferedImage( (int) w, (int) h,BufferedImage.TYPE_INT_RGB));
		imageIcon = blankIcon;
		imageLabel = new JLabel("", imageIcon, JLabel.CENTER);
		imagePane.add(imageLabel, BorderLayout.CENTER);
		
		headerLabel = new JLabel("Probing...");
		
		imagePane.add(headerLabel, BorderLayout.NORTH);
		contentPanel.add(imagePane, BorderLayout.CENTER);
		
		
		if(options!=null){
			importImages(options.getFolder());
		} else {
			logger.log("Cannot probe image: options is null", Logger.ERROR);
		}
		
		this.pack(); 
		this.setVisible(true);
	}
		
	public boolean getOK(){
		return this.ok;
	}
	
	private File getNextImage(){
		
		boolean use = false;
		File[] listOfFiles = options.getFolder().listFiles();
		for (File file : listOfFiles) {
			if(use){
				return file;
			}
			if(file.equals(openImage)){
				use = true;
			}
		}
		return openImage;
	}
		
	private void importImages(final File folder){

		Thread thr = new Thread(){
			public void run() {
				File[] listOfFiles = folder.listFiles();

				File firstFile = null;
				for (File file : listOfFiles) {

					boolean ok = NucleusDetector.checkFile(file); // check file extension

					if(ok){
						firstFile = file;
						break;

					}
				}
				importAndDisplayImage(firstFile);
			}
		};	
		thr.start();
	}
	
	private void importAndDisplayImage(File imageFile){

		try {
		    
			headerLabel.setText("Probing...");
			headerLabel.setIcon(loadingGif);
			
			imageIcon.getImage().flush();
			imageIcon = blankIcon;
			imageLabel.setIcon(imageIcon);
			
			logger.log("Importing file: "+imageFile.getAbsolutePath(), Logger.DEBUG);
			ImageStack imageStack = ImageImporter.importImage(imageFile, logger.getLogfile());
			openImage = imageFile;
			
			ImagePlus image = ImageExporter.convert(imageStack);

			ImageProcessor openProcessor = image.getProcessor();
			openProcessor.setColor(Color.YELLOW);


			List<Cell> cells = NucleusFinder.getCells(imageStack, 
					options, 
					logger.getLogfile(), 
					imageFile, 
					null);

			for(Cell cell : cells){

				Nucleus n = cell.getNucleus();
				// annotate the image processor with the nucleus outline

				double[] positions = n.getPosition();
				FloatPolygon polygon = Utils.createPolygon(n.getBorderList());
				PolygonRoi roi = new PolygonRoi(polygon, PolygonRoi.POLYGON);
				roi.setLocation(positions[Nucleus.X_BASE], positions[Nucleus.Y_BASE]);
				openProcessor.setLineWidth(2);
				openProcessor.draw(roi);
			}

			imageIcon.getImage().flush();
			imageIcon = createViewableImage(openProcessor);

			imageLabel.setIcon(imageIcon);
			imageLabel.revalidate();
			imageLabel.repaint();

			contentPanel.revalidate();;
			contentPanel.repaint();
			headerLabel.setText("Viewing "+imageFile.getAbsolutePath());
			headerLabel.setIcon(null);
			logger.log("New image loaded", Logger.DEBUG);
//			}

		} catch (Exception e) { // end try
			logger.error("Error in image processing", e);
		} // end catch

	}
	
	private ImageIcon createViewableImage(ImageProcessor ip){
		int originalWidth = ip.getWidth();
		int originalHeight = ip.getHeight();

		// set the image width to be less than half the screen width
		int smallWidth = (int) ((double) screenSize.getWidth() * 0.75);

		// keep the image aspect ratio
		double ratio = (double) originalWidth / (double) originalHeight;
		int smallHeight = (int) (smallWidth / ratio);

		final ImagePlus smallImage;

		if(ip.getWidth()>smallWidth){
			smallImage = new ImagePlus("small", ip.resize(smallWidth, smallHeight ));
		} else {
			smallImage = new ImagePlus("small", ip);
		}
		
		ImageIcon smallImageIcon = new ImageIcon(smallImage.getBufferedImage());
		return smallImageIcon;
	}

}
