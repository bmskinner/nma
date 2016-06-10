package charting.charts;

import gui.RotationMode;
import gui.components.ColourSelecter;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import io.ImageExporter;
import io.ImageImporter;
import utility.Constants;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Level;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.Layer;
import org.jfree.util.ShapeUtilities;

import components.Cell;
import components.CellularComponent;
import components.generic.BorderTag;
import components.generic.XYPoint;
import components.nuclear.BorderPoint;
import components.nuclei.Nucleus;
import components.nuclei.sperm.RodentSpermNucleus;
import analysis.AnalysisDataset;
import analysis.detection.BooleanAligner;
import analysis.mesh.NucleusMesh;
import analysis.mesh.NucleusMeshEdge;
import analysis.mesh.NucleusMeshFace;
import analysis.mesh.NucleusMeshImage;
import analysis.mesh.NucleusMeshVertex;
import analysis.signals.SignalManager;
import charting.ChartComponents;
import charting.datasets.NuclearSignalDatasetCreator;
import charting.datasets.NucleusDatasetCreator;
import charting.datasets.NucleusMeshXYDataset;
import charting.datasets.TailDatasetCreator;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;

public class OutlineChartFactory extends AbstractChartFactory {
	
	protected static final ForkJoinPool mainPool = new ForkJoinPool();
	
	private static OutlineChartFactory instance = null;
	
	private OutlineChartFactory(){}
	
	public static OutlineChartFactory getInstance(){
		if(instance==null){
			instance = new OutlineChartFactory();
		}
		return instance;
	}
	
	/**
	 * Create a chart showing the nuclear signal locations in a dataset
	 * @param options
	 * @return
	 */
	public JFreeChart makeSignalOutlineChart(ChartOptions options){
		
		try{
			
			if( ! options.hasDatasets()){
				return ConsensusNucleusChartFactory.getInstance().makeEmptyNucleusOutlineChart();
			}
			
			if(options.isMultipleDatasets()){
				return ConsensusNucleusChartFactory.getInstance().makeEmptyNucleusOutlineChart();
			}
			
			if( ! options.firstDataset().getCollection().hasConsensusNucleus()){
				return ConsensusNucleusChartFactory.getInstance().makeEmptyNucleusOutlineChart();
			}
			
			if(options.isShowWarp()){
				
				return makeSignalWarpChart(options);
			} else {
				
				return makeSignalCoMNucleusOutlineChart(options.firstDataset());
			}
		} catch(Exception e){
			warn("Error making signal chart");
			log(Level.FINE, "Error making signal chart", e);
			return ConsensusNucleusChartFactory.getInstance().makeErrorNucleusOutlineChart();
		}

	}
	
	/**
	 * Draw the given images onto a consensus outline nucleus.
	 * @param options
	 * @param images
	 * @return
	 */
	public JFreeChart makeSignalWarpChart(ChartOptions options, ImageProcessor[] images){
		
		AnalysisDataset dataset = options.firstDataset();
		JFreeChart chart = ConsensusNucleusChartFactory.getInstance().makeNucleusOutlineChart(dataset);

		XYPlot plot = chart.getXYPlot();
		
		// Get the bounding box size for the consensus, to find the offsets for the images created
		Rectangle r = dataset.getCollection().getConsensusNucleus().getBounds(); //.createPolygon().getBounds();
		r = r==null ? dataset.getCollection().getConsensusNucleus().createPolygon().getBounds() : r; // in case the bounds were not set (fixed 1.12.2)
		int w = (int) ( (double) r.width*1.2);
		int h = (int) ( (double) r.height*1.2);

		int xOffset = w >>1;
		int yOffset = h >>1;
				
//		int alpha = 20; // TODO : scale by number of images
//		int alpha = (int) Math.floor( 255 / ((double) images.length) )+20;
//		alpha = alpha < 10 ? 10 : alpha > 156 ? 156 : alpha;
		
		
		drawImagesAsAnnotation(images, plot, 255, -xOffset, -yOffset);
		
		XYDataset ds = NucleusDatasetCreator.createBareNucleusOutline(dataset);
		plot.setDataset(0, ds);
		plot.getRenderer(0).setBasePaint(Color.BLACK);
		plot.getRenderer(0).setBaseSeriesVisible(true);
				
		return chart;	
	}
	
	private JFreeChart makeSignalWarpChart(ChartOptions options){

		
		AnalysisDataset dataset = options.firstDataset();
		
		// Create the outline of the consensus
		
		JFreeChart chart = ConsensusNucleusChartFactory.getInstance().makeNucleusOutlineChart(dataset);

		XYPlot plot = chart.getXYPlot();
		
		// Get consensus mesh.
		NucleusMesh meshConsensus = new NucleusMesh(dataset.getCollection().getConsensusNucleus());
		
		// Get the bounding box size for the consensus, to find the offsets for the images created
		Rectangle r = dataset.getCollection().getConsensusNucleus().getBounds(); //.createPolygon().getBounds();
		r = r==null ? dataset.getCollection().getConsensusNucleus().createPolygon().getBounds() : r; // in case the bounds were not set (fixed 1.12.2)
		int w = (int) ( (double) r.width*1.2);
		int h = (int) ( (double) r.height*1.2);
		
		int xOffset = w >>1;
		int yOffset = h >>1;
		
		SignalManager m = dataset.getCollection().getSignalManager();
		List<Cell> cells = m.getCellsWithNuclearSignals(options.getSignalGroup(), true);
		
		for(Cell cell : cells){
			fine("Drawing signals for cell "+cell.getNucleus().getNameAndNumber());
			// Get each nucleus. Make a mesh.
			NucleusMesh cellMesh = new NucleusMesh(cell.getNucleus(), meshConsensus);
			
			// Get the image with the signal
			ImageProcessor ip = cell.getNucleus().getSignalCollection().getImage(options.getSignalGroup());
			
			// Create NucleusMeshImage from nucleus.
			NucleusMeshImage im = new NucleusMeshImage(cellMesh,ip);
			
			// Draw NucleusMeshImage onto consensus mesh.
			ImageProcessor warped = im.meshToImage(meshConsensus);
//			ImagePlus image = new ImagePlus(cell.getNucleus().getNameAndNumber(), warped);
//			image.show();
			
			
			drawImageAsAnnotation(warped, plot, 20, -xOffset, -yOffset);
		}
		XYDataset ds = NucleusDatasetCreator.createBareNucleusOutline(dataset);
		plot.setDataset(0, ds);
		plot.getRenderer(0).setBasePaint(Color.BLACK);
		plot.getRenderer(0).setBaseSeriesVisible(true);
				
		return chart;	
	}
	
	/**
	 * Create a nucleus outline chart with nuclear signals drawn as transparent
	 * circles
	 * @param dataset the AnalysisDataset to use to draw the consensus nucleus
	 * @param signalCoMs the dataset with the signal centre of masses
	 * @return
	 * @throws Exception 
	 */
	private JFreeChart makeSignalCoMNucleusOutlineChart(AnalysisDataset dataset) throws Exception{
		
		XYDataset signalCoMs = NuclearSignalDatasetCreator.createSignalCoMDataset(dataset);
		
		JFreeChart chart = ConsensusNucleusChartFactory.getInstance().makeNucleusOutlineChart(dataset);

		XYPlot plot = chart.getXYPlot();
		
		if(signalCoMs.getSeriesCount()>0){
			plot.setDataset(1, signalCoMs);

			XYLineAndShapeRenderer  rend = new XYLineAndShapeRenderer();
			for(int series=0;series<signalCoMs.getSeriesCount();series++){

				Shape circle = new Ellipse2D.Double(0, 0, 4, 4);
				rend.setSeriesShape(series, circle);

				String name = (String) signalCoMs.getSeriesKey(series);
				int seriesGroup = getIndexFromLabel(name);
				Color colour = dataset.getSignalGroupColour(seriesGroup);
				rend.setSeriesPaint(series, colour);
				rend.setBaseLinesVisible(false);
				rend.setBaseShapesVisible(true);
				rend.setBaseSeriesVisibleInLegend(false);
			}
			plot.setRenderer(1, rend);

			for(int signalGroup : dataset.getCollection().getSignalManager().getSignalGroups()){
				List<Shape> shapes = NuclearSignalDatasetCreator.createSignalRadiusDataset(dataset, signalGroup);

				int signalCount = shapes.size();

				int alpha = (int) Math.floor( 255 / ((double) signalCount) )+20;
				alpha = alpha < 10 ? 10 : alpha > 156 ? 156 : alpha;

				Color colour = dataset.getSignalGroupColour(signalGroup);

				for(Shape s : shapes){
					XYShapeAnnotation an = new XYShapeAnnotation( s, null,
							null, ColourSelecter.getTransparentColour(colour, true, alpha)); // layer transparent signals
					plot.addAnnotation(an);
				}
			}
		}
		return chart;
	}
	
	public JFreeChart makeCellOutlineChart(ChartOptions options){
		
		if(options.getCell()==null || !options.hasDatasets()){
			fine("No datasets or active cell");
			return ConsensusNucleusChartFactory.getInstance().makeEmptyNucleusOutlineChart();
		}
		
		try {
			if(options.isShowMesh()){
				
				if(options.firstDataset().getCollection().hasConsensusNucleus()){
	
					NucleusMesh mesh1 = options.getRotateMode().equals(RotationMode.ACTUAL) 
						  ? new NucleusMesh(options.getCell().getNucleus())
					      : new NucleusMesh(options.getCell().getNucleus().getVerticallyRotatedNucleus());
						
					
					NucleusMesh mesh2 = new NucleusMesh(options.firstDataset()
							.getCollection()
							.getConsensusNucleus(), mesh1);
					
					NucleusMesh result = mesh1.compareTo(mesh2);				
					return createMeshChart(result, 0.5, options);
	
				} else {
					return ConsensusNucleusChartFactory.getInstance().makeEmptyNucleusOutlineChart();
	
				} 
				
			}
			
			if(options.isShowWarp()){
				
				if(options.firstDataset().getCollection().hasConsensusNucleus()){
					
					NucleusMesh mesh1 = new NucleusMesh(options.getCell().getNucleus());
					NucleusMesh mesh2 = new NucleusMesh(options.firstDataset()
							.getCollection()
							.getConsensusNucleus(), mesh1);
					
					NucleusMeshImage im = new NucleusMeshImage(mesh1, options.getCell().getNucleus().getImage());
	
					ImageProcessor ip = im.meshToImage(mesh2);
					
					return OutlineChartFactory.drawImageAsAnnotation(ip);
	
				} else {
					return ConsensusNucleusChartFactory.getInstance().makeEmptyNucleusOutlineChart();
				}
			}
			return OutlineChartFactory.makeCellOutlineChart(options.getCell(), 
					options.firstDataset(), 
					options.getRotateMode(), 
					false, 
					options.getComponent());
		} catch(Exception e){
			warn("Error creating cell outline chart");
			log(Level.FINE, "Error creating cell outline chart", e);
			return ConsensusNucleusChartFactory.getInstance().makeErrorNucleusOutlineChart();
		}
		
	}
	
	/**
	 * Get a chart contaning the details of the given cell from the given dataset
	 * @param cell the cell to draw
	 * @param dataset the dataset the cell came from
	 * @param rotateMode the orientation of the image
	 * @return
	 * @throws Exception 
	 */
	private static JFreeChart makeCellOutlineChart(Cell cell, AnalysisDataset dataset, RotationMode rotateMode, boolean showhookHump, CellularComponent componentToHighlight) throws Exception{
		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
						null, null, null, PlotOrientation.VERTICAL, true, true,
						false);

		XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.WHITE);
		plot.getRangeAxis().setInverted(true);
		
		if(cell==null){
			return chart;
		}

		// make a hash to track the contents of each dataset produced
		Map<Integer, String> hash = new HashMap<Integer, String>(0); 
		Map<Integer, XYDataset> datasetHash = new HashMap<Integer, XYDataset>(0); 
		
		if(rotateMode.equals(RotationMode.VERTICAL)){
			// duplicate the cell
			Cell newCell = new Cell();
			Nucleus verticalNucleus = cell.getNucleus().getVerticallyRotatedNucleus();

			newCell.setNucleus(verticalNucleus);

			cell = newCell;
			
			// Need to have top point at the top of the image
			plot.getRangeAxis().setInverted(false);
		}

		/*
		 * Get the nucleus dataset
		 */
		
		XYDataset nucleus = NucleusDatasetCreator.createNucleusOutline(cell, true);
		hash.put(hash.size(), "Nucleus"); // add to the first free entry
		datasetHash.put(datasetHash.size(), nucleus);


		/*
		 * If the cell has a rodent sperm nucleus, get the hook and hump rois
		 */
		if(cell.getNucleus().getClass()==RodentSpermNucleus.class){
			XYDataset hookHump = NucleusDatasetCreator.createNucleusHookHumpOutline(cell);
			hash.put(hash.size(), "HookHump"); // add to the first free entry
			datasetHash.put(datasetHash.size(), hookHump);
		}
		
		
		// get the index tags
		XYDataset tags = NucleusDatasetCreator.createNucleusIndexTags(cell);
		hash.put(hash.size(), "Tags"); // add to the first free entry
		datasetHash.put(datasetHash.size(), tags);
		
		// get the signals datasets and add each group to the hash
		// Only display the signal outlines if the rotation is ACTUAL;
		// TODO: the RoundNucleus.rotate() is not working with signals 
		if(rotateMode.equals(RotationMode.ACTUAL)){
			if(cell.getNucleus().hasSignal()){
				List<DefaultXYDataset> signalsDatasets = NucleusDatasetCreator.createSignalOutlines(cell, dataset);

				for(XYDataset d : signalsDatasets){

					String name = "default_0";
					for (int i = 0; i < d.getSeriesCount(); i++) {
						name = (String) d.getSeriesKey(i);	
					}
					int signalGroup = getIndexFromLabel(name);
					hash.put(hash.size(), "SignalGroup_"+signalGroup); // add to the first free entry	
					datasetHash.put(datasetHash.size(), d);
				}
			}
		}

		// get tail datasets if present
		if(cell.hasTail()){

			XYDataset tailBorder = TailDatasetCreator.createTailOutline(cell);
			hash.put(hash.size(), "TailBorder");
			datasetHash.put(datasetHash.size(), tailBorder);
			XYDataset skeleton = TailDatasetCreator.createTailSkeleton(cell);
			hash.put(hash.size(), "TailSkeleton");
			datasetHash.put(datasetHash.size(), skeleton);
		}

		// set the rendering options for each dataset type

		for(int key : hash.keySet()){

			plot.setDataset(key, datasetHash.get(key));
			plot.setRenderer(key, new XYLineAndShapeRenderer(true, false));

			int seriesCount = plot.getDataset(key).getSeriesCount();
			// go through each series in the dataset
			for(int i=0; i<seriesCount;i++){

				// all datasets use the same stroke
				plot.getRenderer(key).setSeriesStroke(i, new BasicStroke(2));
				plot.getRenderer(key).setSeriesVisibleInLegend(i, false);

				/*
				 * Segmented nucleus outline
				 */
				if(hash.get(key).equals("Nucleus")){
					String name = (String) plot.getDataset(key).getSeriesKey(i);
					int colourIndex = getIndexFromLabel(name);
					
					plot.getRenderer().setSeriesPaint(i, dataset.getSwatch().color(colourIndex));
					
					/*
					 * Add a line between the top and bottom vertical points
					 */
					if(cell.getNucleus().hasBorderTag(BorderTag.TOP_VERTICAL) && cell.getNucleus().hasBorderTag(BorderTag.BOTTOM_VERTICAL)){
						BorderPoint[] verticals = cell.getNucleus().getBorderPointsForVerticalAlignment();
						plot.addAnnotation(new XYLineAnnotation(verticals[0].getX(),
								verticals[0].getY(),
								verticals[1].getX(),
								verticals[1].getY()
								));
					}

				}
				
				/*
				 * Hook and hump for rodent sperm
				 */
				if(hash.get(key).equals("HookHump")){
					
					if(showhookHump){
						String name = (String) plot.getDataset(key).getSeriesKey(i);
						// Colour the hook blue, the hump green
						Color color = name.equals("Hump") ? Color.GREEN : Color.BLUE;
						plot.getRenderer(key).setSeriesStroke(i, new BasicStroke(5));
						plot.getRenderer(key).setSeriesPaint(i, color);
						plot.getRenderer(key).setSeriesVisibleInLegend(i, true);
					}
					
				}
				
				/*
				 * Border tags
				 */
				
				if(hash.get(key).equals("Tags")){
					plot.getRenderer(key).setSeriesPaint(i, Color.BLACK);
					String name = plot.getDataset(key).getSeriesKey(i).toString().replace("Tag_", "");
					
					if(name.equals(BorderTag.ORIENTATION_POINT.toString())){
						plot.getRenderer(key).setSeriesPaint(i, Color.BLUE);
					}
					if(name.equals(BorderTag.REFERENCE_POINT.toString())){
						plot.getRenderer(key).setSeriesPaint(i, Color.ORANGE);
					}
						
				}

				/*
				 * Nuclear signals
				 */
				if(hash.get(key).startsWith("SignalGroup_")){
					int colourIndex = getIndexFromLabel(hash.get(key));
					Color colour = dataset.getSignalGroupColour(colourIndex);
					plot.getRenderer(key).setSeriesPaint(i, colour);
				}

				/*
				 * Sperm tail  / flagellum border
				 */
				if(hash.get(key).equals("TailBorder")){

					plot.getRenderer(key).setSeriesPaint(i, Color.GREEN);
				}


				/*
				 * Sperm tail  / flagellum skeleton
				 */
				if(hash.get(key).equals("TailSkeleton")){

					plot.getRenderer(key).setSeriesPaint(i, Color.BLACK);
				}
			}
			
			// Add a background image to the plot
			clearShapeAnnotations(plot);
			
			if(rotateMode.equals(RotationMode.ACTUAL)){
				drawImageAsAnnotation(plot, cell, componentToHighlight);
			}
			

		}
		return chart;
	}
	
	/**
	 * Remove the XYShapeAnnotations from this image
	 * This will leave all other annotation types.
	 */
	private static void clearShapeAnnotations(XYPlot plot){
		for(  Object a : plot.getAnnotations()){
			if(a.getClass()==XYShapeAnnotation.class){
				plot.removeAnnotation( (XYAnnotation) a);
			}
		}
	}
	
	/**
	 * Create a chart with overlapping images drawn as an annotation in the background layer.
	 * Note, all images must be the same size
	 * @param ip
	 * @param plot
	 * @param alpha the transparency
	 * @param xOffset a position to move the image 0,0 to
	 * @param yOffset a position to move the image 0,0 to
	 * @return
	 */		
	private static void drawImagesAsAnnotation( ImageProcessor[] images, XYPlot plot, int alpha, int xOffset, int yOffset){	
		plot.setBackgroundPaint(Color.WHITE);
		plot.getRangeAxis().setInverted(false);
		
		int w = images[0].getWidth();
		int h = images[0].getHeight();
		
		int nonNull = 0;
		
		// check sizes match
		for(ImageProcessor ip : images){
			if(ip==null){
				continue;
			}
			nonNull++;
			if(ip.getHeight()!=h && ip.getWidth()!=w){
				return;
			}
		}
		
		// Make a dataset to allow the autoscale to work
		XYDataset bounds = NucleusDatasetCreator.createAnnotationRectangleDataset(w, h);
		plot.setDataset(0, bounds);
		
		
//		plot.setRenderer(0, new DefaultXYItemRenderer());
		XYItemRenderer rend = plot.getRenderer(0); // index zero should be the nucleus outline dataset
		rend.setBaseSeriesVisible(false);
		
		plot.getDomainAxis().setRange(0, w);
		plot.getRangeAxis().setRange(0, h);
		
		
		for(int x=0; x<w; x++){
			for(int y=0; y<h; y++){

				int pixelTotal = 0;
				for(ImageProcessor ip : images){
					if(ip==null){
						continue;
					}
					pixelTotal += ip.get(x, y);
				}
				
				pixelTotal /= nonNull; // scale back down to 0-255;
				
				
				if(pixelTotal<255){// Ignore anything that is not signal - the background is already white
					
//					pixelTotal /= 2; // increase the intensities
				
					Color col = new Color(pixelTotal, pixelTotal, pixelTotal, alpha);

					// Ensure the 'pixels' overlap to avoid lines of background colour seeping through
					Rectangle2D r = new Rectangle2D.Double(x+xOffset-0.1, y+yOffset-0.1, 1.2, 1.2);
					XYShapeAnnotation a = new XYShapeAnnotation(r, null, null, col);

					rend.addAnnotation(a, Layer.BACKGROUND);
				}
			}
		}
		
	}
	

	/**
	 * Create a chart with an image drawn as an annotation in the background layer.
	 * @param ip
	 * @param plot
	 * @param alpha
	 * @param xOffset a position to move the image 0,0 to
	 * @param yOffset a position to move the image 0,0 to
	 * @return
	 */		
	private static void drawImageAsAnnotation( ImageProcessor ip, XYPlot plot, int alpha, int xOffset, int yOffset){	
		plot.setBackgroundPaint(Color.WHITE);
		plot.getRangeAxis().setInverted(false);
		
		// Make a dataset to allow the autoscale to work
		XYDataset bounds = NucleusDatasetCreator.createAnnotationRectangleDataset(ip.getWidth(), ip.getHeight());
		plot.setDataset(0, bounds);
		
		
//		plot.setRenderer(0, new DefaultXYItemRenderer());
		XYItemRenderer rend = plot.getRenderer(0); // index zero should be the nucleus outline dataset
		rend.setBaseSeriesVisible(false);
		
		plot.getDomainAxis().setRange(0, ip.getWidth());
		plot.getRangeAxis().setRange(0, ip.getHeight());
		
		for(int x=0; x<ip.getWidth(); x++){
			for(int y=0; y<ip.getHeight(); y++){

				int pixel = ip.get(x, y);
				
				if(pixel<255){// Ignore anything that is not signal - the background is already white
				
					Color col = new Color(pixel, pixel, pixel, alpha);

					// Ensure the 'pixels' overlap to avoid lines of background colour seeping through
					Rectangle2D r = new Rectangle2D.Double(x+xOffset-0.1, y+yOffset-0.1, 1.2, 1.2);
					XYShapeAnnotation a = new XYShapeAnnotation(r, null, null, col);

					rend.addAnnotation(a, Layer.BACKGROUND);
				}
			}
		}
		
	}
			
	/**
	 * Create a chart with an image drawn as an annotation in the background layer.
	 * The image pixels are fully opaque
	 * @param ip
	 * @param alpha
	 * @return
	 */
	private static void drawImageAsAnnotation( ImageProcessor ip, XYPlot plot, int alpha){
		drawImageAsAnnotation(ip, plot, alpha, 0, 0);
	}
	
	/**
	 * Create a chart with an image drawn as an annotation in the background layer.
	 * The image pixels are fully opaque
	 * @param ip
	 * @return
	 */
	private static JFreeChart drawImageAsAnnotation( ImageProcessor ip){
		return drawImageAsAnnotation(ip, 255);
	}
	
	/**
	 * Create a chart with an image drawn as an annotation in the background layer.
	 * The image pixels have the given alpha transparency value
	 * @param ip
	 * @param alpha
	 * @return
	 */
	private static JFreeChart drawImageAsAnnotation( ImageProcessor ip, int alpha){
		
		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
						null, null, null, PlotOrientation.VERTICAL, true, true,
						false);

		XYPlot plot = chart.getXYPlot();
		drawImageAsAnnotation(ip, plot, alpha);
		return chart;
	}
	
	
	/**
	 * Draw the greyscale image from teh given channel on the plot
	 * @param imageFile
	 * @param channel
	 */
	private static void drawImageAsAnnotation(XYPlot plot, Cell cell, CellularComponent component){
		
		if(component==null){
			return;
		}

		ImageProcessor openProcessor = component.getImage();

		if(openProcessor==null){	
			return;	
		}
		
		double[] positions = cell.getNucleus().getPosition();

		XYItemRenderer rend = plot.getRenderer(0); // index zero should be the nucleus outline dataset
		
		int padding = 10; // a border of pixels beyond the cell boundary
		int wideW = (int) (positions[CellularComponent.WIDTH]+(padding*2));
		int wideH = (int) (positions[CellularComponent.HEIGHT]+(padding*2));
		int wideX = (int) (positions[CellularComponent.X_BASE]-padding);
		int wideY = (int) (positions[CellularComponent.Y_BASE]-padding);

		wideX = wideX<0 ? 0 : wideX;
		wideY = wideY<0 ? 0 : wideY;

		openProcessor.setRoi(wideX, wideY, wideW, wideH);
		openProcessor = openProcessor.crop();

		for(int x=0; x<openProcessor.getWidth(); x++){
			for(int y=0; y<openProcessor.getHeight(); y++){

				//				int pixel = im.getRGB(x, y);
				int pixel = openProcessor.get(x, y);
				Color col = new Color(pixel, pixel, pixel, 255);

				// Ensure the 'pixels' overlap to avoid lines of background colour seeping through
				Rectangle2D r = new Rectangle2D.Double(x-padding-0.1, y-padding-0.1, 1.2, 1.2);
				XYShapeAnnotation a = new XYShapeAnnotation(r, null, null, col);

				rend.addAnnotation(a, Layer.BACKGROUND);
			}
		}


	}
	
	
	/**
	 * Create a chart with the outlines of all the nuclei within a dataset.
	 * The options should only contain a single dataset
	 * @param options
	 * @return
	 * @throws Exception 
	 */
	public static JFreeChart createVerticalNucleiChart(ChartOptions options) throws Exception{
		
		if( ! options.hasDatasets()){
			options.log(Level.FINEST, "No datasets - returning empty chart");
			return ConsensusNucleusChartFactory.getInstance().makeEmptyNucleusOutlineChart();
		}
		
		if(options.isMultipleDatasets()){
			options.log(Level.FINEST, "Multiple datasets - creating vertical nuclei chart");
			return createMultipleDatasetVerticalNucleiChart(options);
		}
		
		options.log(Level.FINEST, "Single dataset - creating vertical nuclei chart");
		return createSingleDatasetVerticalNucleiChart(options);
		
	}
	
	/**
	 * Create the chart with the outlines of all the nuclei within a single dataset.
	 * @param options
	 * @return
	 * @throws Exception 
	 */
	private static JFreeChart createSingleDatasetVerticalNucleiChart(ChartOptions options) throws Exception{
		
		JFreeChart chart = ChartFactory.createXYLineChart(null,
						null, null, null, PlotOrientation.VERTICAL, true, true,
						false);
		

		XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.WHITE);
		
		plot.addRangeMarker(new ValueMarker(0, Color.LIGHT_GRAY, ChartComponents.PROFILE_STROKE));
		plot.addDomainMarker(new ValueMarker(0, Color.LIGHT_GRAY, ChartComponents.PROFILE_STROKE));
		
		XYLineAndShapeRenderer r = new XYLineAndShapeRenderer(true, false);
		r.setBaseSeriesVisibleInLegend(false);
		r.setBaseStroke(ChartComponents.PROFILE_STROKE);
		r.setSeriesPaint(0, Color.LIGHT_GRAY);
		r.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
		
		/*
		 * Get a boolean mask for the consensus nucleus
		 */
		
//		if(options.isNormalised()){
//			if(options.firstDataset().getCollection().hasConsensusNucleus()){
//				log(Level.FINE, "Performing boolean alignment of nuclei");
//				boolean[][] reference = options.firstDataset().getCollection().getConsensusNucleus().getBooleanMask(200, 200);
////				BooleanAligner aligner = new BooleanAligner(reference);
//				
//				BooleanAlignmentTask task = new BooleanAlignmentTask(reference, 
//						options.firstDataset().getCollection().getNuclei().toArray(new Nucleus[0]));
////				task.addProgressListener(this);
////				task.invoke();
//				mainPool.invoke(task);
//			}
//		}
		
		boolean hasConsensus = options.firstDataset().getCollection().hasConsensusNucleus();
		boolean[][] reference = null;
		BooleanAligner aligner = null;
		
		if(options.isNormalised()){
			if(hasConsensus){

				reference = options.firstDataset().getCollection().getConsensusNucleus().getBooleanMask(200, 200);
				aligner = new BooleanAligner(reference);
			}
		}

		int i=0;
		
		if(options.isNormalised()){
			
			if(hasConsensus){
				
				options.log(Level.FINEST, "Creating consensus nucleus dataset");
				
				Nucleus consensus = options.firstDataset().getCollection().getConsensusNucleus();
				XYDataset consensusDataset = NucleusDatasetCreator.createNucleusOutline(consensus, false);
				
				XYLineAndShapeRenderer c = new XYLineAndShapeRenderer(true, false);
				c.setBaseSeriesVisibleInLegend(false);
				c.setBaseStroke(ChartComponents.PROFILE_STROKE);
				c.setSeriesPaint(0, Color.BLACK);
				
				plot.setDataset(i, consensusDataset);
				plot.setRenderer(i, c);
			}
			i++;
		}
		
		options.log(Level.FINEST, "Creating charting datasets for vertically rotated nuclei");
		
		for(Nucleus n : options.firstDataset().getCollection().getNuclei()){
			
//			options.log(Level.FINEST, "Fetching vertically rotated nucleus: "+i);
			Nucleus verticalNucleus = n.getVerticallyRotatedNucleus();
			
			/*
			 * Find the best offset for the CoM to fit the consensus nucleus if present
			 */
//			options.log(Level.FINEST, "Setting CoM: Nucleus "+i);
			if(options.isNormalised()){
				if(hasConsensus){
					boolean[][] test = verticalNucleus.getBooleanMask(200, 200);
					int[] offsets = aligner.align(test);
					verticalNucleus.moveCentreOfMass( new XYPoint(offsets[1], offsets[0]));
				}
			} else {
				verticalNucleus.moveCentreOfMass( new XYPoint(0, 0));
			}
			
//			options.log(Level.FINEST, "Creating outline: Nucleus "+i);
			XYDataset nucleusDataset = NucleusDatasetCreator.createNucleusOutline(verticalNucleus, false);
			
//			options.log(Level.FINEST, "Setting dataset and renderer: Nucleus "+i);
			plot.setDataset(i, nucleusDataset);
			plot.setRenderer(i, r);

			i++;
			
		}
		options.log(Level.FINEST, "Created vertical nuclei chart");
		return chart;
	}
	
	/**
	 * Create the chart with the outlines of all the nuclei within a single dataset.
	 * @param options
	 * @return
	 * @throws Exception 
	 */
	private static JFreeChart createMultipleDatasetVerticalNucleiChart(ChartOptions options) throws Exception{
		
		JFreeChart chart = ChartFactory.createXYLineChart(null,
						null, null, null, PlotOrientation.VERTICAL, true, true,
						false);

		XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.WHITE);
		
		plot.addRangeMarker(new ValueMarker(0, Color.LIGHT_GRAY, ChartComponents.PROFILE_STROKE));
		plot.addDomainMarker(new ValueMarker(0, Color.LIGHT_GRAY, ChartComponents.PROFILE_STROKE));

		StandardXYToolTipGenerator tooltip = new StandardXYToolTipGenerator();
		
		int i=0;
		int datasetNumber = 0;
		for(AnalysisDataset dataset : options.getDatasets()){
			
			Color colour = dataset.hasDatasetColour()
					? dataset.getDatasetColour()
					: ColourSelecter.getSegmentColor(datasetNumber++);
			
			XYLineAndShapeRenderer r = new XYLineAndShapeRenderer(true, false);
			r.setBaseSeriesVisibleInLegend(false);
			r.setBaseStroke(ChartComponents.PROFILE_STROKE);
			r.setSeriesPaint(0, colour);			
			r.setBaseToolTipGenerator(tooltip);

			for(Nucleus n : dataset.getCollection().getNuclei()){

				Nucleus verticalNucleus = n.getVerticallyRotatedNucleus();

				XYDataset nucleusDataset = NucleusDatasetCreator.createNucleusOutline(verticalNucleus, false);

				plot.setDataset(i, nucleusDataset);
				plot.setRenderer(i, r);

				i++;

			}
		}
		return chart;
	}
	
	/**
	 * Create the chart with the outlines of all the nuclei within a single dataset.
	 * @param options
	 * @return
	 * @throws Exception 
	 */
	public static JFreeChart createMeshChart(NucleusMesh mesh, double log2Ratio, ChartOptions options) throws Exception{
		
		NucleusMeshXYDataset dataset = NucleusDatasetCreator.createNucleusMeshEdgeDataset(mesh);

		JFreeChart chart = ChartFactory.createXYLineChart(null,
				null, null, null, PlotOrientation.VERTICAL, true, true,
				false);

		XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.WHITE);
		

		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
		renderer.setBaseSeriesVisibleInLegend(false);
		renderer.setBaseStroke(ChartComponents.MARKER_STROKE);
		
		for(int series=0; series<dataset.getSeriesCount(); series++){
			
			double ratio = dataset.getRatio(dataset.getSeriesKey(series));
			Color colour = getGradientColour(ratio, log2Ratio);
			
			renderer.setSeriesPaint(series, colour);
			renderer.setSeriesStroke(series, ChartComponents.MARKER_STROKE);
			renderer.setSeriesItemLabelsVisible(series, false);
			renderer.setSeriesVisible(series, options.isShowMeshEdges());

		}
		
		plot.setDataset(0, dataset);
		plot.setRenderer(0, renderer);	
				
		// Show faces as polygon annotations under the chart
		if(options.isShowMeshFaces()){ 
			
			for(NucleusMeshFace f : mesh.getFaces()){
				
				Path2D path = f.toPath();
				
				Color colour = getGradientColour(f.getLog2Ratio(), log2Ratio); // not quite black
				
				XYShapeAnnotation a = new XYShapeAnnotation(path, null, null, colour);

				renderer.addAnnotation(a, Layer.BACKGROUND);
			}
			
		}
		
		/*
		 * If the annotations are set, create a new set of labels for the vertices
		 */
		
		if(options.isShowAnnotations()){
			
			for(NucleusMeshVertex v : mesh.getPeripheralVertices()){
				XYTextAnnotation annotation = new XYTextAnnotation(v.getName(), v.getPosition().getX()-1, v.getPosition().getY());
				annotation.setPaint(Color.BLACK);
				plot.addAnnotation(annotation);
			}
			
			for(NucleusMeshVertex v : mesh.getInternalVertices()){
				XYTextAnnotation annotation = new XYTextAnnotation(v.getName(), v.getPosition().getX()-1, v.getPosition().getY());
				annotation.setPaint(Color.BLACK);
				plot.addAnnotation(annotation);
			}
			
			if(options.isShowMeshEdges()){ 

				for(NucleusMeshEdge v : mesh.getEdges()){
					XYTextAnnotation annotation = new XYTextAnnotation(v.getName(), v.getMidpoint().getX(), v.getMidpoint().getY()+1);
					annotation.setPaint(Color.BLUE);
					plot.addAnnotation(annotation);
				}
			}
			
			if(options.isShowMeshFaces()){ 
				for(NucleusMeshFace f : mesh.getFaces()){
					XYTextAnnotation annotation = new XYTextAnnotation(f.getName(),
							f.getMidpoint().getX(),
							f.getMidpoint().getY());
					annotation.setPaint(Color.GREEN);
					plot.addAnnotation(annotation);
				}
			}
			
		}
		
		chart.getXYPlot().getDomainAxis().setVisible(options.isShowXAxis());
		chart.getXYPlot().getRangeAxis().setVisible(options.isShowYAxis());
		
		chart.getXYPlot().getDomainAxis().setInverted(options.isInvertXAxis());
		chart.getXYPlot().getRangeAxis().setInverted(options.isInvertYAxis());
		
		return chart;
	}
	
	
	/**
	 * Log2 ratios are coming in, which must be converted to real ratios
	 * @param ratio
	 * @param minRatio
	 * @param maxRatio
	 * @return
	 */
	private static Color getGradientColour(double ratio, double maxRatio){
			
		double log2Min = -maxRatio;
		double log2Max = maxRatio;
		
		int rValue = 0;
		int bValue = 0;

		if(ratio <= 0){

			if(ratio<log2Min){
				bValue = 255;
			} else {
				// ratio of ratio
				
				// differnce between 0 and minRatio
				double range = Math.abs(log2Min);
				double actual = range - Math.abs(ratio);
				
				double realRatio = 1 - (actual / range);
				bValue = (int) (255d * realRatio);
			}

		} else {
			
			if(ratio>log2Max){
				rValue = 255;
			} else {
				
				// differnce between 0 and minRatio
				double range = Math.abs(log2Max);
				double actual = range - Math.abs(ratio);
				
				double realRatio =  1- (actual / range);
				rValue = (int) (255d * realRatio);				
			}

		}
		int r = rValue;
		int g = 0;
		int b = bValue;
		return new Color(r, g, b);
	}
	
	
	/**
	 * Create a histogram of log 2 ratios for a NucleusMesh
	 * @param mesh the comparison mesh with length ratios
	 * @return
	 * @throws Exception 
	 */
	public static JFreeChart createMeshHistogram(NucleusMesh mesh) throws Exception{

		HistogramDataset ds = NucleusDatasetCreator.createNucleusMeshHistogramDataset(mesh);
		JFreeChart chart = HistogramChartFactory.createHistogram(ds, "Log2 ratio", "Number of edges");
		XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.WHITE);
		plot.addDomainMarker(new ValueMarker(0, Color.BLACK, ChartComponents.PROFILE_STROKE));
		return chart;
	}

}
