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
import java.net.URL;
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
	private AnalysisOptions options; // the options to detect with
	private File openImage;			// the image currently open
	private Logger logger;	
	private JLabel imageLabel;		// the JLabel to hold the image

	private ImageIcon imageIcon;	// the icon with the image, for display in JLabel
	private JLabel headerLabel;		// the header text and loading gif
	
	private ImageIcon loadingGif = null; // the icon for the loading gif
	
	private ImageIcon blankIcon ; 		// a black square
	private boolean ok = false;

	/**
	 * Create the dialog.
	 */
	public ImageProber(AnalysisOptions options, File logFile) {
		
		if(options==null){
			throw new IllegalArgumentException("Options is null");
		} 
		this.setModal(true);
		this.options = options;
		this.logger = new Logger(logFile, "ImageProber");
		this.setTitle("Image Prober");
		
		setBounds(100, 100, 450, 300);
		
		try{
			String pathToGif = "/ajax-loader.gif";
			URL urlToGif = this.getClass().getResource(pathToGif);
	
			
			loadingGif = new ImageIcon(urlToGif);
			
			if(loadingGif==null){
				IJ.log("Looking for: "+urlToGif.getFile());
				IJ.log("Unable to load gif");
			} else {
//				IJ.log("Loaded gif");
			}
			
			
		} catch (Exception e){
			IJ.log("Cannot load gif resource: "+e.getMessage());
			for(StackTraceElement e1 : e.getStackTrace()){
				IJ.log(e1.toString());
			}
		}

		getContentPane().setLayout(new BorderLayout());
		contentPanel.setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		
		JPanel header = this.createHeader();
		contentPanel.add(header, BorderLayout.NORTH);
		
		JPanel footer = this.createFooter();
		contentPanel.add(footer, BorderLayout.SOUTH);
		
		JPanel imagePane = createImagePanel();
		contentPanel.add(imagePane, BorderLayout.CENTER);
		
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		
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
			contentPanel.add(nextButton, BorderLayout.EAST);
		}

		importImages(options.getFolder());

		this.pack(); 
		this.setVisible(true);
	}
	
	private JPanel createHeader(){
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));

		headerLabel = new JLabel("Probing...");
		
		panel.add(headerLabel, BorderLayout.NORTH);
		
		return panel;
	}
	
	private JPanel createImagePanel(){
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		
        double w = screenSize.getWidth() * 0.75;
        double h = 650;

        blankIcon = new ImageIcon(new BufferedImage( (int) w, (int) h,BufferedImage.TYPE_INT_RGB));

		imageIcon = blankIcon;
		imageLabel = new JLabel("", imageIcon, JLabel.CENTER);
		panel.add(imageLabel, BorderLayout.CENTER);
				
		return panel;
	}
	
	
	private JPanel createFooter(){
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		
		JButton okButton = new JButton("Proceed with analysis");
		okButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				ImageProber.this.ok = true;
				ImageProber.this.setVisible(false);

			}
		});
		panel.add(okButton);
		
		getRootPane().setDefaultButton(okButton);

		JButton cancelButton = new JButton("Revise settings");
		cancelButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {

				ImageProber.this.ok = false;
				ImageProber.this.setVisible(false);

			}
		});
		panel.add(cancelButton);
		
		
		return panel;
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
			headerLabel.repaint();
			
//			imageIcon.getImage().flush();
//			imageIcon = blankIcon;
			imageLabel.setIcon(loadingGif);
			
			logger.log("Importing file: "+imageFile.getAbsolutePath(), Logger.DEBUG);
			ImageStack imageStack = ImageImporter.importImage(imageFile, logger.getLogfile());
			openImage = imageFile;
			
			ImageProcessor openProcessor = ImageExporter.convert(imageStack).getProcessor();

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

			headerLabel.setText("Showing "+imageFile.getAbsolutePath());
			headerLabel.setIcon(null);
			
			logger.log("New image loaded", Logger.DEBUG);


		} catch (Exception e) { // end try
			logger.error("Error in image processing", e);
		} // end catch

	}
	
	private ImageIcon createViewableImage(ImageProcessor ip){
		int originalWidth = ip.getWidth();
		int originalHeight = ip.getHeight();

		// set the image width to be less than half the screen width
		int smallWidth = (int) ((double) screenSize.getWidth() * 0.65);

		// keep the image aspect ratio
		double ratio = (double) originalWidth / (double) originalHeight;
		int smallHeight = (int) (smallWidth / ratio);
		
		ImageIcon smallImageIcon;

		if(ip.getWidth()>smallWidth){
			
			smallImageIcon = new ImageIcon(ip.resize(smallWidth, smallHeight ).getBufferedImage());
			
		} else {
			
			smallImageIcon = new ImageIcon( ip.getBufferedImage()  );
		}
		return smallImageIcon;
	}

}
