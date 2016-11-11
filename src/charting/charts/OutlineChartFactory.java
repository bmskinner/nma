/*******************************************************************************
 *  	Copyright (C) 2015, 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details. Gluten-free. May contain 
 *     traces of LDL asbestos. Avoid children using heavy machinery while under the
 *     influence of alcohol.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package charting.charts;

import gui.RotationMode;
import gui.components.ColourSelecter;
import ij.process.ImageProcessor;
import io.UnloadableImageException;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.Layer;

import components.Cell;
import components.CellularComponent;
import components.ICell;
import components.active.DefaultCell;
import components.active.generic.FloatPoint;
import components.active.generic.UnavailableProfileTypeException;
import components.active.generic.UnavailableSignalGroupException;
import components.generic.BorderTag;
import components.generic.ProfileType;
import components.generic.XYPoint;
import components.nuclei.Nucleus;
import analysis.IAnalysisDataset;
import analysis.detection.BooleanAligner;
import analysis.mesh.NucleusMesh;
import analysis.mesh.NucleusMeshEdge;
import analysis.mesh.NucleusMeshFace;
import analysis.mesh.NucleusMeshImage;
import analysis.mesh.NucleusMeshVertex;
import analysis.signals.SignalManager;
import charting.ChartComponents;
import charting.datasets.ChartDatasetCreationException;
import charting.datasets.ComponentOutlineDataset;
import charting.datasets.NucleusDatasetCreator;
import charting.datasets.NucleusMeshXYDataset;
import charting.datasets.OutlineDatasetCreator;
import charting.datasets.TailDatasetCreator;
import charting.options.ChartOptions;

public class OutlineChartFactory extends AbstractChartFactory {
	
	public OutlineChartFactory(ChartOptions o){
		super(o);
	}
	
	
	public static JFreeChart makeEmptyChart(){
		return ConsensusNucleusChartFactory.makeEmptyChart();
	}
	
	/**
	 * Create a chart showing the nuclear signal locations in a dataset
	 * @param options
	 * @return
	 */
	public JFreeChart makeSignalOutlineChart(){
		
		try{
			
			if( ! options.hasDatasets()){
				finer("No datasets for signal outline chart");
				return makeEmptyChart();
			}
			
			if(options.isMultipleDatasets()){
				finer("Multiple datasets for signal outline chart");
				return makeEmptyChart();
			}
			
			if( ! options.firstDataset().getCollection().hasConsensusNucleus()){
				finer("No consensus for signal outline chart");
				return makeEmptyChart();
			}
			
			if(options.isShowWarp()){
				finer("Warp chart for signal outline chart");
				return makeSignalWarpChart();
			} else {
				finer("Signal CoM for signal outline chart");
				return new NuclearSignalChartFactory(options).makeSignalCoMNucleusOutlineChart();
			}
		} catch(Exception e){
			warn("Error making signal chart");
			log(Level.FINE, "Error making signal chart", e);
			return makeErrorChart();
		}

	}
	
	/**
	 * Draw the given images onto a consensus outline nucleus.
	 * @param options
	 * @param images
	 * @return
	 */
	public JFreeChart makeSignalWarpChart(ImageProcessor image){
		
		IAnalysisDataset dataset = options.firstDataset();
		JFreeChart chart = new ConsensusNucleusChartFactory(options).makeNucleusOutlineChart();

		XYPlot plot = chart.getXYPlot();
		
		NucleusMesh meshConsensus = new NucleusMesh( dataset.getCollection().getConsensusNucleus());
		
		if(options.isStraightenMesh()){
			meshConsensus = meshConsensus.straighten();
		}
		
		XYDataset ds;
		try {
			ds = new NucleusDatasetCreator().createBareNucleusOutline(dataset);
		} catch (ChartDatasetCreationException e) {
			fine("Error creating outline", e);
			return makeErrorChart();
		}
		
		double xMin = DatasetUtilities.findMinimumDomainValue(ds).doubleValue();
		double yMin = DatasetUtilities.findMinimumRangeValue(ds).doubleValue();
		
		// Get the bounding box size for the consensus, to find the offsets for the images created
		Rectangle r = meshConsensus.toPath().getBounds();

		int xOffset = (int) Math.round( -xMin);
		int yOffset = (int) Math.round( -yMin);
		
		int w = r.width;
		int h = r.height;
		
		finest("Consensus bounds: "+w+" x "+h+" : "+r.x+", "+r.y);
		finest("Image: "+image.getWidth()+" x "+image.getHeight());

		drawImageAsAnnotation(image, plot, 255, -xOffset, -yOffset, options.isShowBounds());
		
		
		plot.setDataset(0, ds);
		plot.getRenderer(0).setBasePaint(Color.BLACK);
		plot.getRenderer(0).setBaseSeriesVisible(true);
		
		applyAxisOptions(chart);
				
		return chart;	
	}
	
	private JFreeChart makeSignalWarpChart(){

		
		IAnalysisDataset dataset = options.firstDataset();
		
		// Create the outline of the consensus
		
		JFreeChart chart = new ConsensusNucleusChartFactory(options).makeNucleusOutlineChart();

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
		Set<ICell> cells = m.getCellsWithNuclearSignals(options.getSignalGroup(), true);
		
		for(ICell cell : cells){
			fine("Drawing signals for cell "+cell.getNucleus().getNameAndNumber());
			// Get each nucleus. Make a mesh.
			NucleusMesh cellMesh = new NucleusMesh(cell.getNucleus(), meshConsensus);
			
			// Get the image with the signal
			ImageProcessor ip;
			try {
				ip = cell.getNucleus().getSignalCollection().getImage(options.getSignalGroup());
				
				// Create NucleusMeshImage from nucleus.
				NucleusMeshImage im = new NucleusMeshImage(cellMesh,ip);
				
				// Draw NucleusMeshImage onto consensus mesh.
				ImageProcessor warped = im.meshToImage(meshConsensus);
//				ImagePlus image = new ImagePlus(cell.getNucleus().getNameAndNumber(), warped);
//				image.show();
				drawImageAsAnnotation(warped, plot, 20, -xOffset, -yOffset, options.isShowBounds());
				
			} catch (UnloadableImageException e) {
				warn("Unable to load signal image for signal group "+options.getSignalGroup()+" in cell "+cell.getNucleus().getNameAndNumber());
				fine("Unable to load signal image for signal group "+options.getSignalGroup()+" in cell "+cell.getNucleus().getNameAndNumber(), e);
			}
	
			
		}
		XYDataset ds;
		try {
			ds = new NucleusDatasetCreator().createBareNucleusOutline(dataset);
		} catch (ChartDatasetCreationException e) {
			fine("Error creating outline", e);
			return makeErrorChart();
		}
		plot.setDataset(0, ds);
		plot.getRenderer(0).setBasePaint(Color.BLACK);
		plot.getRenderer(0).setBaseSeriesVisible(true);
		applyAxisOptions(chart);
		return chart;	
	}
		
	/**
	 * Create a chart with the outline of the given cell
	 * @return
	 */
	public JFreeChart makeCellOutlineChart(){
		
		if(options.getCell()==null || !options.hasDatasets()){
			fine("No datasets or active cell");
			return makeEmptyChart();
		}
		
		
		
		try {
			
			if( ! options.isShowAnnotations()){
				finest("Annotations not shown, creating bare outline chart");
				return makeBareCellOutlineChart();
			}
			
			if(options.isShowMesh()){
				finest("Making mesh chart");
				if(options.firstDataset().getCollection().hasConsensusNucleus()){
	
					NucleusMesh mesh1 = options.getRotateMode().equals(RotationMode.ACTUAL) 
						  ? new NucleusMesh(options.getCell().getNucleus())
					      : new NucleusMesh(options.getCell().getNucleus().getVerticallyRotatedNucleus());
						
					
					NucleusMesh mesh2 = new NucleusMesh(options.firstDataset()
							.getCollection()
							.getConsensusNucleus(), mesh1);
					
					NucleusMesh result = mesh1.compareTo(mesh2);				
					return createMeshChart(result, 0.5);
	
				} else {
					return makeEmptyChart();
	
				} 
				
			}
			
			if(options.isShowWarp()){
				finest("Making warp chart");
				if(options.firstDataset().getCollection().hasConsensusNucleus()){
					
					NucleusMesh mesh1 = new NucleusMesh(options.getCell().getNucleus());
					NucleusMesh mesh2 = new NucleusMesh(options.firstDataset()
							.getCollection()
							.getConsensusNucleus(), mesh1);
					
					Rectangle2D bounds1 = mesh1.toPath().getBounds2D();
					finest("Mesh1 bounds are "+bounds1.getWidth()
							+" x "+bounds1.getHeight()
							+" at "+bounds1.getX()
							+", "+bounds1.getY());
					
					
					Rectangle2D bounds = mesh2.toPath().getBounds2D();
					finest("Mesh2 bounds are "+bounds.getWidth()
							+" x "+bounds.getHeight()
							+" at "+bounds.getX()
							+", "+bounds.getY());
					
					// Create a mesh image from the nucleus
					NucleusMeshImage im;
					try {
						im = new NucleusMeshImage(mesh1, options.getCell().getNucleus().getImage());
					} catch (UnloadableImageException e) {
						warn("Cannot load nucleus image: "+options.getCell().getNucleus().getSourceFile().getAbsolutePath());
						return makeErrorChart();
					}
	
					// Apply the mesh image to the shape of the consensus image
					ImageProcessor ip = im.meshToImage(mesh2);
										
					return OutlineChartFactory.drawImageAsAnnotation(ip);
	
				} else {
					return makeEmptyChart();
				}
			}
			finest("Making standart cell outline chart");
			return makeStandardCellOutlineChart();
		} catch(ChartCreationException e){
			warn("Error creating cell outline chart");
			fine("Error creating cell outline chart", e);
			return makeErrorChart();
		}
		
	}
	
	private JFreeChart makeBareCellOutlineChart() throws ChartCreationException {
		
		if(! options.hasCell()){
			finest("No cell to draw");
			return ConsensusNucleusChartFactory.makeEmptyChart();
		}
		
		
		JFreeChart chart = createBaseXYChart();
		XYDataset ds;
		try {
			ds = new NucleusDatasetCreator().createBareNucleusOutline(options.getCell().getNucleus());
		} catch (ChartDatasetCreationException e) {
			fine("Error making outline", e);
			return makeErrorChart();
		}


		XYPlot plot = chart.getXYPlot();
		plot.setDataset(ds);

		plot.setRenderer(new XYLineAndShapeRenderer(options.isShowLines(), options.isShowPoints()));

		int seriesCount = plot.getSeriesCount();

		for (int i = 0; i < seriesCount; i++) {
			plot.getRenderer().setSeriesVisibleInLegend(i, Boolean.FALSE);
			plot.getRenderer().setSeriesStroke(i, new BasicStroke(3));
			plot.getRenderer().setSeriesPaint(i, Color.BLACK);
		}	
		
		// Add a background image to the plot
		clearShapeAnnotations(plot);

		if(options.hasComponent()){
			drawImageAsAnnotation(plot, options.getCell(), options.getComponent());
		}
		applyAxisOptions(chart);
		return chart;
		
	}
	
	/**
	 * Get a chart contaning the details of the given cell from the given dataset
	 * @param cell the cell to draw
	 * @param dataset the dataset the cell came from
	 * @param rotateMode the orientation of the image
	 * @return
	 * @throws Exception 
	 */
	private JFreeChart makeStandardCellOutlineChart() throws ChartCreationException {
		
		if(! options.hasCell()){
			finest("No cell to draw");
			return ConsensusNucleusChartFactory.makeEmptyChart();
		}
		
		if(! options.hasDatasets()){
			finest("No dataset to draw");
			return ConsensusNucleusChartFactory.makeEmptyChart();
		}
		
		ICell cell = options.getCell();
		IAnalysisDataset dataset = options.firstDataset();
		
		JFreeChart chart = createBaseXYChart();
		XYPlot plot = chart.getXYPlot();
		
		plot.getRangeAxis().setInverted(true);
		
		
		// make a hash to track the contents of each dataset produced
		Map<Integer, String>    hash        = new HashMap<Integer, String>(0); 
		Map<Integer, XYDataset> datasetHash = new HashMap<Integer, XYDataset>(0); 

		if(options.getRotateMode().equals(RotationMode.VERTICAL)){

			try {
				finest("Rotation mode is vertical");
				// duplicate the cell
				ICell newCell = new DefaultCell();
				finest("Cell segments    :"+ cell.getNucleus().getProfile(ProfileType.ANGLE).toString());

				Nucleus verticalNucleus = cell.getNucleus().getVerticallyRotatedNucleus();
				finest("Vertical nucleus is "+verticalNucleus.getNameAndNumber());
				newCell.setNucleus(verticalNucleus);
				finest("Vertical segments:"+verticalNucleus.getProfile(ProfileType.ANGLE).toString());

				cell = newCell;
				finest("Fetched vertical nucleus");

				// Need to have top point at the top of the image
				plot.getRangeAxis().setInverted(false);
			} catch(UnavailableProfileTypeException e){
				warn("Cannot create vertical nucleus");
				fine("Cannot create vertical nucleus", e);
				return makeErrorChart();
			}
		}

		/*
		 * Get the nucleus outline dataset
		 */
		try {
			
			OutlineDatasetCreator dc = new OutlineDatasetCreator(cell.getNucleus());
			
			XYDataset nucleusDataset = dc.createOutline(true);
			hash.put(hash.size(), "Nucleus"); // add to the first free entry
			datasetHash.put(datasetHash.size(), nucleusDataset);
			finest("Created nucleus outline");
			
		} catch (ChartDatasetCreationException e) {
			
			throw new ChartCreationException("Unable to get nucleus outline", e);
			
		}
				
		// get the index tags

		if(options.isShowBorderTags()){
			XYDataset tags;
			try {
				tags = new NucleusDatasetCreator().createNucleusIndexTags(cell);
			} catch (ChartDatasetCreationException e) {
				throw new ChartCreationException("Cannot get tags for nucleus", e);
			}
			hash.put(hash.size(), "Tags"); // add to the first free entry
			datasetHash.put(datasetHash.size(), tags);
			finest("Created border index tags");
		}

		// get the signals datasets and add each group to the hash
		
		if(options.isShowSignals()){
			finest("Displaying signals on chart");
			if(cell.getNucleus().getSignalCollection().hasSignal()){
				List<ComponentOutlineDataset> signalsDatasets;
				try {
					signalsDatasets = new NucleusDatasetCreator().createSignalOutlines(cell, dataset);
				} catch (ChartDatasetCreationException e) {
					return makeErrorChart();
				}

				finest("Fetched signal outline datasets for "+cell.getNucleus().getNameAndNumber());
				
				if(signalsDatasets.size()==0){
					finest("No signal datasets returned");
				}
				
				for(ComponentOutlineDataset d : signalsDatasets){

					for(int series=0; series<d.getSeriesCount(); series++){
						String seriesKey = d.getSeriesKey(series).toString();
						
						finest("Adding outline for "+seriesKey+" to dataset hash");
						d.getComponent(seriesKey);
						hash.put(hash.size(), seriesKey); // add to the first free entry	
						datasetHash.put(datasetHash.size(), d);
					}
				}
			}
		}

//		// get tail datasets if present
//		if(cell.hasTail()){
//
//			XYDataset tailBorder = TailDatasetCreator.createTailOutline(cell);
//			hash.put(hash.size(), "TailBorder");
//			datasetHash.put(datasetHash.size(), tailBorder);
//			XYDataset skeleton = TailDatasetCreator.createTailSkeleton(cell);
//			hash.put(hash.size(), "TailSkeleton");
//			datasetHash.put(datasetHash.size(), skeleton);
//		}

		// set the rendering options for each dataset type
		finest("Rendering chart");
		for(int key : hash.keySet()){

			plot.setDataset(key, datasetHash.get(key));
			
			plot.setRenderer(key, new XYLineAndShapeRenderer(options.isShowLines(), options.isShowPoints()));

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
					Paint colour = ColourSelecter.getColor(colourIndex);
					plot.getRenderer(key).setSeriesPaint(i, colour);					
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
					if(name.equals(BorderTag.INTERSECTION_POINT.toString())){
						plot.getRenderer(key).setSeriesPaint(i, Color.CYAN);
					}
					if(name.equals(BorderTag.TOP_VERTICAL.toString())){
						plot.getRenderer(key).setSeriesPaint(i, Color.GRAY);
					}
					if(name.equals(BorderTag.BOTTOM_VERTICAL.toString())){
						plot.getRenderer(key).setSeriesPaint(i, Color.GRAY);
					}
						
				}

				/*
				 * Nuclear signals
				 */
				if(hash.get(key).startsWith("SignalGroup_")){

					UUID seriesGroup = getSignalGroupFromLabel(hash.get(key));
					
					Paint colour = ColourSelecter.getColor(i);
					try {

						colour = dataset.getCollection().getSignalGroup(seriesGroup).hasColour()
								? dataset.getCollection().getSignalGroup(seriesGroup).getGroupColour()
								: colour;

					
					} catch (UnavailableSignalGroupException e){
	        			fine("Signal group "+seriesGroup+" is not present in collection", e);
	        		} finally {
	        			plot.getRenderer(key).setSeriesPaint(i, colour);
	        		}
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
			
			if(options.getRotateMode().equals(RotationMode.ACTUAL)){
				drawImageAsAnnotation(plot, cell, options.getComponent());
			}
			

		}
		applyAxisOptions(chart);
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
	 * Create a chart with an image drawn as an annotation in the background layer.
	 * @param ip
	 * @param plot
	 * @param alpha
	 * @param xOffset a position to move the image 0,0 to
	 * @param yOffset a position to move the image 0,0 to
	 * @return
	 */		
	private static void drawImageAsAnnotation( ImageProcessor ip, XYPlot plot, int alpha, int xOffset, int yOffset, boolean showBounds){	
		plot.setBackgroundPaint(Color.WHITE);
		plot.getRangeAxis().setInverted(false);
		
		// Make a dataset to allow the autoscale to work
		XYDataset bounds = new NucleusDatasetCreator().createAnnotationRectangleDataset(ip.getWidth(), ip.getHeight());
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
				} else {
					if(showBounds){
						Color col = new Color(255, 0, 0, alpha);

						// Ensure the 'pixels' overlap to avoid lines of background colour seeping through
						Rectangle2D r = new Rectangle2D.Double(x+xOffset-0.1, y+yOffset-0.1, 1.2, 1.2);
						XYShapeAnnotation a = new XYShapeAnnotation(r, null, null, col);

						rend.addAnnotation(a, Layer.BACKGROUND);
					}
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
		drawImageAsAnnotation(ip, plot, alpha, 0, 0, false);
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
	private static void drawImageAsAnnotation(XYPlot plot, ICell cell, CellularComponent component){
		
		if(component==null || cell==null || plot==null){
			return;
		}
		
		ImageProcessor openProcessor;
		try {
			openProcessor = component.getImage();
		} catch (UnloadableImageException e) {
			return;
		}

		int[] positions = cell.getNucleus().getPosition();

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
				Color col = new Color(pixel);
				//				Color col = new Color(pixel, pixel, pixel, 255);

				// Ensure the 'pixels' overlap to avoid lines of background colour seeping through
				Rectangle2D r = new Rectangle2D.Double(x-padding-0.6, y-padding-0.6, 1.2, 1.2);
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
	public JFreeChart createVerticalNucleiChart(){
		
		if( ! options.hasDatasets()){
			finest("No datasets - returning empty chart");
			return makeEmptyChart();
		}
		
		if(options.isMultipleDatasets()){
			finest("Multiple datasets - creating vertical nuclei chart");
			return createMultipleDatasetVerticalNucleiChart();
		}
		
		finest("Single dataset - creating vertical nuclei chart");
		return createSingleDatasetVerticalNucleiChart();
		
	}
	
	/**
	 * Create the chart with the outlines of all the nuclei within a single dataset.
	 * @param options
	 * @return
	 * @throws Exception 
	 */
	private JFreeChart createSingleDatasetVerticalNucleiChart(){
		
		JFreeChart chart = createBaseXYChart();
		XYPlot plot = chart.getXYPlot();
		
		plot.addRangeMarker(new ValueMarker(0, Color.LIGHT_GRAY, ChartComponents.PROFILE_STROKE));
		plot.addDomainMarker(new ValueMarker(0, Color.LIGHT_GRAY, ChartComponents.PROFILE_STROKE));
		
		XYLineAndShapeRenderer r = new XYLineAndShapeRenderer(true, false);
		r.setBaseSeriesVisibleInLegend(false);
		r.setBaseStroke(ChartComponents.PROFILE_STROKE);
		r.setSeriesPaint(0, Color.LIGHT_GRAY);
		r.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
				
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
				
				finest("Creating consensus nucleus dataset");
				
				Nucleus consensus = options.firstDataset().getCollection().getConsensusNucleus();
				
				OutlineDatasetCreator dc = new OutlineDatasetCreator(consensus);

				
				try {
					
					XYDataset consensusDataset = dc.createOutline(false);
					XYLineAndShapeRenderer c = new XYLineAndShapeRenderer(true, false);
					c.setBaseSeriesVisibleInLegend(false);
					c.setBaseStroke(ChartComponents.PROFILE_STROKE);
					c.setSeriesPaint(0, Color.BLACK);
					
					plot.setDataset(i, consensusDataset);
					plot.setRenderer(i, c);
					
				} catch (ChartDatasetCreationException e) {
					warn("Cannot create data for dataset "+options.firstDataset().getName());
					fine("Error getting chart data", e);
				}

			}
			i++;
		}
		
		finest( "Creating charting datasets for vertically rotated nuclei");
		
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
					verticalNucleus.moveCentreOfMass( new FloatPoint(offsets[1], offsets[0]));
				}
			} else {
				verticalNucleus.moveCentreOfMass( new FloatPoint(0, 0));
			}
			

			OutlineDatasetCreator dc = new OutlineDatasetCreator(verticalNucleus);
			
			try {
				
				XYDataset nucleusDataset = dc.createOutline(false);
				plot.setDataset(i, nucleusDataset);
				plot.setRenderer(i, r);
				
			} catch (ChartDatasetCreationException e) {
				warn("Cannot create data for dataset "+options.firstDataset().getName());
				fine("Error getting chart data", e);
			} finally {
				i++;
			}
			
		}
		finest("Created vertical nuclei chart");
		applyAxisOptions(chart);
		return chart;
	}
	
	/**
	 * Create the chart with the outlines of all the nuclei within a single dataset.
	 * @param options
	 * @return
	 * @throws Exception 
	 */
	private JFreeChart createMultipleDatasetVerticalNucleiChart(){
		
		JFreeChart chart = createBaseXYChart();
		XYPlot plot = chart.getXYPlot();
		
		plot.addRangeMarker(new ValueMarker(0, Color.LIGHT_GRAY, ChartComponents.PROFILE_STROKE));
		plot.addDomainMarker(new ValueMarker(0, Color.LIGHT_GRAY, ChartComponents.PROFILE_STROKE));

		StandardXYToolTipGenerator tooltip = new StandardXYToolTipGenerator();
		
		int i=0;
		int datasetNumber = 0;
		for(IAnalysisDataset dataset : options.getDatasets()){
			
			Paint colour = dataset.hasDatasetColour()
					? dataset.getDatasetColour()
					: ColourSelecter.getColor(datasetNumber++);
			
			XYLineAndShapeRenderer r = new XYLineAndShapeRenderer(true, false);
			r.setBaseSeriesVisibleInLegend(false);
			r.setBaseStroke(ChartComponents.PROFILE_STROKE);
			r.setSeriesPaint(0, colour);			
			r.setBaseToolTipGenerator(tooltip);

			for(Nucleus n : dataset.getCollection().getNuclei()){

				Nucleus verticalNucleus = n.getVerticallyRotatedNucleus();
				
				OutlineDatasetCreator dc = new OutlineDatasetCreator(verticalNucleus);
				
				try {
					
					XYDataset nucleusDataset = dc.createOutline(false);
					plot.setDataset(i, nucleusDataset);
					plot.setRenderer(i, r);
					
				} catch (ChartDatasetCreationException e) {
					warn("Cannot create data for dataset "+dataset.getName());
					fine("Error getting chart data", e);
				} finally {
					i++;
				}

			}
		}
		applyAxisOptions(chart);
		return chart;
	}
	
	/**
	 * Create the chart with the outlines of all the nuclei within a single dataset.
	 * @param mesh the mesh to draw
	 * @param log2ratio the ratio to set as full colour intensity
	 * @param options the drawing options
	 * @return
	 * @throws Exception 
	 */
	public JFreeChart createMeshChart(NucleusMesh mesh, double log2Ratio) throws ChartCreationException {
		
		NucleusMeshXYDataset dataset;
		try {
			dataset = new NucleusDatasetCreator().createNucleusMeshEdgeDataset(mesh);
		} catch (Exception e) {
			throw new ChartCreationException("Cannot create mesh chart", e);
		}

		JFreeChart chart = createBaseXYChart();
		XYPlot plot = chart.getXYPlot();
		

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
		
		applyAxisOptions(chart);
		
		return chart;
	}
	
	
	/**
	 * Log2 ratios are coming in, which must be converted to real ratios
	 * @param ratio
	 * @param minRatio
	 * @param maxRatio
	 * @return
	 */
	private Color getGradientColour(double ratio, double maxRatio){
			
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
	public static JFreeChart createMeshHistogram(NucleusMesh mesh) throws ChartCreationException {

		HistogramDataset ds;
		try {
			ds = new NucleusDatasetCreator().createNucleusMeshHistogramDataset(mesh);
		} catch (Exception e) {
			throw new ChartCreationException("Cannot make mesh histogram", e);
		}
		JFreeChart chart = HistogramChartFactory.createHistogram(ds, "Log2 ratio", "Number of edges");
		XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.WHITE);
		plot.addDomainMarker(new ValueMarker(0, Color.BLACK, ChartComponents.PROFILE_STROKE));

		return chart;
	}

}
