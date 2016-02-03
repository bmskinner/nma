package charting.charts;

import gui.RotationMode;
import gui.components.ColourSelecter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import components.Cell;
import components.generic.BorderTag;
import components.nuclear.BorderPoint;
import components.nuclei.Nucleus;
import components.nuclei.sperm.RodentSpermNucleus;

import analysis.AnalysisDataset;
import charting.datasets.NuclearSignalDatasetCreator;
import charting.datasets.NucleusDatasetCreator;
import charting.datasets.TailDatasetCreator;

public class OutlineChartFactory extends AbstractChartFactory {
	
	/**
	 * Create a nucleus outline chart with nuclear signals drawn as transparent
	 * circles
	 * @param dataset the AnalysisDataset to use to draw the consensus nucleus
	 * @param signalCoMs the dataset with the signal centre of masses
	 * @return
	 * @throws Exception 
	 */
	public static JFreeChart makeSignalCoMNucleusOutlineChart(AnalysisDataset dataset, XYDataset signalCoMs) throws Exception{
		JFreeChart chart = ConsensusNucleusChartFactory.makeNucleusOutlineChart(dataset);

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

			for(int signalGroup : dataset.getCollection().getSignalGroups()){
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
	
	/**
	 * Get a chart contaning the details of the given cell from the given dataset
	 * @param cell the cell to draw
	 * @param dataset the dataset the cell came from
	 * @param rotateMode the orientation of the image
	 * @return
	 * @throws Exception 
	 */
	public static JFreeChart makeCellOutlineChart(Cell cell, AnalysisDataset dataset, RotationMode rotateMode, boolean showhookHump) throws Exception{
		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
						null, null, null, PlotOrientation.VERTICAL, true, true,
						false);

		XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.WHITE);
		plot.getRangeAxis().setInverted(true);

		// make a hash to track the contents of each dataset produced
		Map<Integer, String> hash = new HashMap<Integer, String>(0); 
		Map<Integer, XYDataset> datasetHash = new HashMap<Integer, XYDataset>(0); 
		
		if(rotateMode.equals(RotationMode.VERTICAL)){
			// duplicate the cell
			Cell newCell = new Cell();
			Nucleus verticalNucleus = cell.getNucleus().duplicate();
			newCell.setNucleus(verticalNucleus);
			if(verticalNucleus.hasBorderTag(BorderTag.TOP_VERTICAL) && verticalNucleus.hasBorderTag(BorderTag.BOTTOM_VERTICAL)){

				// Rotate vertical
				BorderPoint[] points = verticalNucleus.getBorderPointsForVerticalAlignment();
				verticalNucleus.alignPointsOnVertical(points[0], points[1] );
			} else {
				// If the verticals are not present, use the orientation point
				verticalNucleus.rotatePointToBottom(verticalNucleus.getBorderTag(BorderTag.ORIENTATION_POINT));
			}
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
//			addCellImageToPlot(cell, plot);
			

		}
		return chart;
	}

}
