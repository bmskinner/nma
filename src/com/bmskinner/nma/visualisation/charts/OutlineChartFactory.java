/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nma.visualisation.charts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.Layer;

import com.bmskinner.nma.components.MissingLandmarkException;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.DefaultCell;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.mesh.DefaultMesh;
import com.bmskinner.nma.components.mesh.DefaultMeshImage;
import com.bmskinner.nma.components.mesh.Mesh;
import com.bmskinner.nma.components.mesh.MeshCreationException;
import com.bmskinner.nma.components.mesh.MeshEdge;
import com.bmskinner.nma.components.mesh.MeshFace;
import com.bmskinner.nma.components.mesh.MeshImage;
import com.bmskinner.nma.components.mesh.MeshImageCreationException;
import com.bmskinner.nma.components.mesh.MeshVertex;
import com.bmskinner.nma.components.mesh.UncomparableMeshImageException;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.components.signals.INuclearSignal;
import com.bmskinner.nma.components.signals.ISignalCollection;
import com.bmskinner.nma.components.signals.ISignalGroup;
import com.bmskinner.nma.gui.RotationMode;
import com.bmskinner.nma.gui.components.ColourSelecter;
import com.bmskinner.nma.io.ImageImporter;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.visualisation.ChartComponents;
import com.bmskinner.nma.visualisation.datasets.ChartDatasetCreationException;
import com.bmskinner.nma.visualisation.datasets.ComponentOutlineDataset;
import com.bmskinner.nma.visualisation.datasets.NuclearSignalXYDataset;
import com.bmskinner.nma.visualisation.datasets.NucleusDatasetCreator;
import com.bmskinner.nma.visualisation.datasets.NucleusMeshXYDataset;
import com.bmskinner.nma.visualisation.datasets.PointDataset;
import com.bmskinner.nma.visualisation.options.ChartOptions;

import ij.process.ImageProcessor;

/**
 * Factory for creating outlines of cellular components and signals
 * 
 * @author bms41
 *
 */
public class OutlineChartFactory extends AbstractChartFactory {

	private static final String ERROR_CREATING_MESH_MSG = "Error creating mesh";

	private static final Logger LOGGER = Logger.getLogger(OutlineChartFactory.class.getName());

	protected static final String NO_CONSENSUS_ERROR_LBL = "No consensus nucleus in dataset";

	private static final boolean SEGMENTED = true;
	private static final boolean NOT_SEGMENTED = false;

	public OutlineChartFactory(@NonNull ChartOptions o) {
		super(o);
	}

	/**
	 * Create a chart showing the nuclear signal locations in a consensus nucleus.
	 * 
	 * @return
	 */
	public JFreeChart makeSignalOutlineChart() {

		try {

			if (!options.hasDatasets()) {
				LOGGER.finer("No datasets for signal outline chart");
				return createEmptyChart();
			}

			if (options.isMultipleDatasets()) {
				LOGGER.finer("Multiple datasets for signal outline chart");
				return createMultipleDatasetEmptyChart();
			}

			if (!options.firstDataset().getCollection().hasConsensus()) {
				LOGGER.finer("No consensus for signal outline chart");
				return createTextAnnotatedEmptyChart(NO_CONSENSUS_ERROR_LBL);
			}

			LOGGER.finer("Signal CoM for signal outline chart");
			return makeSignalCoMNucleusOutlineChart();
		} catch (ChartCreationException e) {
			LOGGER.warning("Error making signal chart");
			LOGGER.log(Loggable.STACK, "Error making signal chart", e);
			return createErrorChart();
		}

	}

	/**
	 * Create a nucleus outline chart with nuclear signals drawn as transparent
	 * circles
	 * 
	 * @return
	 * @throws ChartDatasetCreationException
	 * @throws Exception
	 */
	private JFreeChart makeSignalCoMNucleusOutlineChart() throws ChartCreationException {

		NuclearSignalXYDataset signalCoMs = new NuclearSignalXYDataset(options);

		JFreeChart chart = new ConsensusNucleusChartFactory(options).makeNucleusOutlineChart();

		XYPlot plot = chart.getXYPlot();

		if (signalCoMs.getSeriesCount() > 0) {
			plot.setDataset(1, signalCoMs);

			XYLineAndShapeRenderer rend = new XYLineAndShapeRenderer();
			for (int series = 0; series < signalCoMs.getSeriesCount(); series++) {

				Shape circle = new Ellipse2D.Double(0, 0, 4, 4);
				rend.setSeriesShape(series, circle);

				String name = (String) signalCoMs.getSeriesKey(series);
				UUID seriesGroup = getSignalGroupFromLabel(name);

				Optional<ISignalGroup> g = options.firstDataset().getCollection()
						.getSignalGroup(seriesGroup);
				if (g.isPresent()) {
					Paint colour = g.get().getGroupColour().orElse(ColourSelecter.getColor(series));
					rend.setSeriesPaint(series, colour);
				}

				rend.setDefaultLinesVisible(false);
				rend.setDefaultShapesVisible(true);
				rend.setDefaultSeriesVisibleInLegend(false);
			}
			plot.setRenderer(1, rend);

			int j = 0;

			if (options.isShowAnnotations()) { // transparent ellipse surrounding CoM
				for (UUID signalGroup : options.firstDataset().getCollection().getSignalManager()
						.getSignalGroupIDs()) {
					List<Shape> shapes = signalCoMs.createSignalRadii(signalGroup);

					int signalCount = shapes.size();

					int alpha = (int) Math.floor(255 / ((double) signalCount)) + 20;
					alpha = alpha < 1 ? 1 : alpha > 156 ? 156 : alpha;

					Optional<ISignalGroup> g = options.firstDataset().getCollection()
							.getSignalGroup(signalGroup);
					if (g.isPresent()) {
						Paint colour = g.get().getGroupColour()
								.orElse(ColourSelecter.getColor(j++));
						for (Shape s : shapes) {
							XYShapeAnnotation an = new XYShapeAnnotation(s, null, null,
									ColourSelecter.getTransparentColour((Color) colour, true,
											alpha));
							plot.addAnnotation(an);
						}
					}
				}
			}
		}
		applyDefaultAxisOptions(chart);
		return chart;
	}

	/**
	 * Create a chart with the outline of the given cell
	 * 
	 * @return
	 */
	public JFreeChart makeCellOutlineChart() {

		if (options.getCell() == null || !options.hasDatasets())
			return createEmptyChart();

		try {

			if (!options.isShowAnnotations())
				return makeBareCellOutlineChart();

			if (options.isShowWarp()) {
				if (options.firstDataset().getCollection().hasConsensus()) {

					Mesh mesh1 = new DefaultMesh(options.getCell().getPrimaryNucleus());
					Mesh mesh2 = new DefaultMesh(
							options.firstDataset().getCollection().getConsensus(), mesh1);

					ImageProcessor nucleusIP = ImageImporter
							.importFullImageTo24bitGreyscale(options.getComponent().get(0));

					// Create a mesh image from the nucleus
					MeshImage im = new DefaultMeshImage(mesh1, nucleusIP);

					// Draw the image onto the shape described by the
					// consensus nucleus
					ImageProcessor ip = im.drawImage(mesh2);

					return drawImageAsAnnotation(ip);

				}
				return createEmptyChart();
			}
			return makeStandardCellOutlineChart();
		} catch (ChartCreationException | MissingLandmarkException | MeshCreationException
				| ComponentCreationException
				| MeshImageCreationException | UncomparableMeshImageException e) {
			LOGGER.warning("Error creating cell outline chart");
			LOGGER.log(Loggable.STACK, "Error creating cell outline chart", e);
			return createErrorChart();
		}

	}

	private JFreeChart makeBareCellOutlineChart() throws ChartCreationException {

		if (!options.hasCell())
			return ConsensusNucleusChartFactory.createEmptyChart();
		try {
			JFreeChart chart = createBaseXYChart();

			List<ComponentOutlineDataset> result = new ArrayList<>();

			ICell cell = options.getCell();

			if (cell.hasCytoplasm())
				result.add(new ComponentOutlineDataset(cell.getCytoplasm(), false,
						options.getScale()));

			if (cell.hasNucleus()) {
				for (Nucleus n : cell.getNuclei())
					result.add(new ComponentOutlineDataset(n, true, options.getScale()));
			}

			XYPlot plot = chart.getXYPlot();
			for (int i = 0; i < result.size(); i++) {
				plot.setDataset(result.get(i));
				plot.setRenderer(i,
						new XYLineAndShapeRenderer(options.isShowLines(), options.isShowPoints()));

				int seriesCount = plot.getDataset(i).getSeriesCount();
				for (int j = 0; j < seriesCount; j++) {
					plot.getRenderer(i).setSeriesVisibleInLegend(j, Boolean.FALSE);
					plot.getRenderer(i).setSeriesStroke(j, new BasicStroke(3));
					plot.getRenderer(i).setSeriesPaint(j, Color.BLACK);
				}
			}

			// Add a background image to the plot
			clearShapeAnnotations(plot);

			if (options.hasComponent()) {

				drawImageAsAnnotation(plot, options.getCell(), options.getComponent().get(0),
						options.getRotateMode());
			}
			applyDefaultAxisOptions(chart);
			return chart;
		} catch (ChartDatasetCreationException e) {
			LOGGER.log(Loggable.STACK, "Error making cytoplasm outline", e);
			return createErrorChart();
		}
	}

	/**
	 * Get a chart contaning the details of the given cell from the given dataset
	 * 
	 * @return
	 * @throws Exception
	 */
	private JFreeChart makeStandardCellOutlineChart() throws ChartCreationException {

		if (!options.hasCell())
			return ConsensusNucleusChartFactory.createEmptyChart();

		if (!options.hasDatasets())
			return ConsensusNucleusChartFactory.createEmptyChart();

		try {
			ICell cell = options.getCell();
			IAnalysisDataset dataset = options.firstDataset();

			if (options.getRotateMode().equals(RotationMode.VERTICAL)) {
				Nucleus n = cell.getPrimaryNucleus().getOrientedNucleus();
				cell = new DefaultCell(n);
			}

			List<ComponentOutlineDataset> cellDatasets = new ArrayList<>();

			/* Get the cytoplasm outline dataset */
			if (cell.hasCytoplasm())
				cellDatasets.add(new ComponentOutlineDataset(cell.getCytoplasm(), NOT_SEGMENTED,
						options.getScale()));

			/* Get the nucleus outline dataset */
			for (Nucleus n : cell.getNuclei()) {
				cellDatasets.add(new ComponentOutlineDataset(n, SEGMENTED, options.getScale()));

				if (options.isShowSignals()
						&& cell.getPrimaryNucleus().getSignalCollection().hasSignal()) {
					cellDatasets.addAll(
							new NucleusDatasetCreator(options).createSignalOutlines(cell, dataset));
				}
			}

			return renderCellDataset(cellDatasets);
		} catch (ChartDatasetCreationException | MissingLandmarkException
				| ComponentCreationException e) {
			throw new ChartCreationException("Unable to get nucleus outline", e);
		}
	}

	/**
	 * Render the given cell dataset according to the chart options
	 * 
	 * @param cellDataset
	 * @param options
	 * @return
	 */
	private JFreeChart renderCellDataset(List<ComponentOutlineDataset> cellDatasets) {

		JFreeChart chart = createBaseXYChart();
		XYPlot plot = chart.getXYPlot();

		plot.getRangeAxis().setInverted(true);

		if (options.getRotateMode().equals(RotationMode.VERTICAL)) {
			// Need to have top point at the top of the image
			plot.getRangeAxis().setInverted(false);
		}

		// set the rendering options for each dataset type
		for (int i = 0; i < cellDatasets.size(); i++) {

			ComponentOutlineDataset ds = cellDatasets.get(i);
			plot.setDataset(i, ds);

			boolean showLines = true;
			boolean showPoints = false;
			XYLineAndShapeRenderer rend = new XYLineAndShapeRenderer(showLines, showPoints);
			plot.setRenderer(i, rend);

			// go through each series in the dataset

			int seriesCount = ds.getSeriesCount();

			for (int series = 0; series < seriesCount; series++) {
				rend.setSeriesStroke(series, ChartComponents.MARKER_STROKE);
				rend.setSeriesVisibleInLegend(series, true);

				/* Segmented nucleus outline */
				if (ds.getComponent() instanceof Nucleus) {
					int colourIndex = getIndexFromLabel((String) ds.getSeriesKey(series));
					rend.setSeriesPaint(series, ColourSelecter.getColor(colourIndex));
				}

				/* Nuclear signals */
				if (ds.getComponent() instanceof INuclearSignal) {

					ISignalCollection sc = options.getCell().getPrimaryNucleus()
							.getSignalCollection();
					UUID signalId = (UUID) ds.getSeriesKey(series);
					Optional<UUID> signalGroup = sc.getSignalGroupIds().stream()
							.filter(sg -> sc.getSignals(sg).stream()
									.anyMatch(s -> s.getID().equals(signalId)))
							.findFirst();

					UUID seriesGroup = signalGroup.get();

					Optional<ISignalGroup> g = options.firstDataset().getCollection()
							.getSignalGroup(seriesGroup);
					if (g.isPresent()) {
						Paint colour = g.get().getGroupColour()
								.orElse(ColourSelecter.getColor(series));
						rend.setSeriesPaint(series, colour);
					}
				}

			}

		}

		try {
			// Add landmark locations
			PointDataset lmData = new PointDataset();

			Nucleus n = RotationMode.VERTICAL.equals(options.getRotateMode())
					? options.getCell().getPrimaryNucleus().getOrientedNucleus()
					: options.getCell().getPrimaryNucleus();

			for (OrientationMark om : n.getOrientationMarks())
				lmData.addPoint(n.getLandmark(om).toString(), n.getBorderPoint(om));

			int lmDataIndex = plot.getDatasetCount() + 1;
			plot.setDataset(lmDataIndex, lmData);

			// Add nucleus CoM
			PointDataset ncomData = new PointDataset();
			ncomData.addPoint(n.getNameAndNumber(), n.getCentreOfMass());
			int ncomDataIndex = plot.getDatasetCount() + 1;
			plot.setDataset(ncomDataIndex, ncomData);

			// Add signal CoMs
			PointDataset comData = new PointDataset();
			for (INuclearSignal s : n.getSignalCollection().getAllSignals())
				comData.addPoint(s.getID().toString(), s.getCentreOfMass());

			int comDataIndex = plot.getDatasetCount() + 1;
			plot.setDataset(plot.getDatasetCount() + 1, comData);

			// Set renderers
			plot.setRenderer(lmDataIndex, lmData.getRenderer(Color.GRAY));
			plot.setRenderer(ncomDataIndex, ncomData.getRenderer(Color.PINK));
			plot.setRenderer(comDataIndex, comData.getRenderer(Color.RED));

		} catch (Exception e) {
			return createErrorChart();
		}

		// Add a background image to the plot
		clearShapeAnnotations(plot);

		drawImageAsAnnotation(plot, options.getCell(), options.getComponent().get(0),
				options.getRotateMode());

		applyDefaultAxisOptions(chart);
		return chart;
	}

	/**
	 * Create the chart with the outlines of all the nuclei within a single dataset.
	 * 
	 * @param mesh      the mesh to draw
	 * @param log2ratio the ratio to set as full colour intensity
	 * @param options   the drawing options
	 * @return
	 * @throws Exception
	 */
	public JFreeChart createMeshChart(Mesh mesh, double log2Ratio) throws ChartCreationException {
		JFreeChart chart = createBaseXYChart();
		XYPlot plot = chart.getXYPlot();

		int datasetIndex = 0;

		if (options.isShowMeshVertices()) {
			try {
				NucleusMeshXYDataset dataset = new NucleusDatasetCreator(options)
						.createNucleusMeshVertexDataset(mesh);
				XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, true);
				renderer.setDefaultSeriesVisibleInLegend(false);

				for (int series = 0; series < dataset.getSeriesCount(); series++) {

					double ratio = dataset.getRatio(dataset.getSeriesKey(series));
					Color colour = getGradientColour(ratio, log2Ratio);
					Shape circle = new Ellipse2D.Double(0, 0, 4, 4);
					renderer.setSeriesShape(series, circle);
					renderer.setSeriesPaint(series, colour);
					renderer.setSeriesItemLabelsVisible(series, false);
					renderer.setSeriesVisible(series, true);

				}

				plot.setDataset(datasetIndex, dataset);
				plot.setRenderer(datasetIndex, renderer);
				datasetIndex++;
			} catch (Exception e) {
				throw new ChartCreationException("Cannot create mesh chart", e);
			}
		}

		if (options.isShowMeshEdges()) {
			try {
				NucleusMeshXYDataset dataset = new NucleusDatasetCreator(options)
						.createNucleusMeshEdgeDataset(mesh);
				XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
				renderer.setDefaultSeriesVisibleInLegend(false);
				renderer.setDefaultStroke(ChartComponents.MARKER_STROKE);

				for (int series = 0; series < dataset.getSeriesCount(); series++) {

					double ratio = dataset.getRatio(dataset.getSeriesKey(series));
					Color colour = getGradientColour(ratio, log2Ratio);

					renderer.setSeriesPaint(series, colour);
					renderer.setSeriesStroke(series, ChartComponents.MARKER_STROKE);
					renderer.setSeriesItemLabelsVisible(series, false);
					renderer.setSeriesVisible(series, true);

				}

				plot.setDataset(datasetIndex, dataset);
				plot.setRenderer(datasetIndex, renderer);
				datasetIndex++;
			} catch (Exception e) {
				throw new ChartCreationException("Cannot create mesh chart", e);
			}
		}

		// Show faces as polygon annotations under the chart. Faces are coloured by
		// underlying segment if possible
		if (options.isShowMeshFaces()) {

			try {
				List<IProfileSegment> segments = options.firstDataset().getCollection()
						.getConsensus()
						.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE).getSegments();

				for (int i = 0; i < segments.size(); i++) {
					IProfileSegment s = segments.get(i);
					for (MeshFace f : mesh.getFaces(s)) {
						Path2D path = f.toPath();
						Color colour = ColourSelecter.getColor(i, options.getSwatch());
						XYShapeAnnotation a = new XYShapeAnnotation(path, null, null, colour);
						plot.getRenderer().addAnnotation(a, Layer.BACKGROUND);
					}
				}

			} catch (MissingLandmarkException | MissingProfileException | ProfileException
					| ComponentCreationException e) {
				LOGGER.log(Loggable.STACK, "Error getting segments for mesh", e);
				LOGGER.log(Loggable.STACK, "Falling back to old mesh face annotation");

				for (MeshFace f : mesh.getFaces()) {

					Path2D path = f.toPath();
					Color colour = getGradientColour(f.getLog2Ratio(), log2Ratio);
					XYShapeAnnotation a = new XYShapeAnnotation(path, null, null, colour);
					plot.getRenderer().addAnnotation(a, Layer.BACKGROUND);
				}
			}

		}

		/*
		 * If the annotations are set, create a new set of labels for the vertices
		 */

		if (options.isShowAnnotations()) {

			for (MeshVertex v : mesh.getPeripheralVertices()) {
				XYTextAnnotation annotation = new XYTextAnnotation(v.getName(),
						v.getPosition().getX() - 1,
						v.getPosition().getY());
				annotation.setPaint(Color.BLACK);
				plot.addAnnotation(annotation);
			}

			for (MeshVertex v : mesh.getInternalVertices()) {
				XYTextAnnotation annotation = new XYTextAnnotation(v.getName(),
						v.getPosition().getX() - 1,
						v.getPosition().getY());
				annotation.setPaint(Color.BLACK);
				plot.addAnnotation(annotation);
			}

			if (options.isShowMeshEdges()) {

				for (MeshEdge v : mesh.getEdges()) {
					XYTextAnnotation annotation = new XYTextAnnotation(v.getName(),
							v.getMidpoint().getX(),
							v.getMidpoint().getY() + 1);
					annotation.setPaint(Color.BLUE);
					plot.addAnnotation(annotation);
				}
			}

			if (options.isShowMeshFaces()) {
				for (MeshFace f : mesh.getFaces()) {
					XYTextAnnotation annotation = new XYTextAnnotation(f.getName(),
							f.getMidpoint().getX(),
							f.getMidpoint().getY());
					annotation.setPaint(Color.GREEN);
					plot.addAnnotation(annotation);
				}
			}

		}

		applyDefaultAxisOptions(chart);

		return chart;
	}

	/**
	 * Log2 ratios are coming in, which must be converted to real ratios
	 * 
	 * @param ratio
	 * @param maxRatio
	 * @return
	 */
	private Color getGradientColour(double ratio, double maxRatio) {

		double log2Min = -maxRatio;
		double log2Max = maxRatio;

		int rValue = 0;
		int bValue = 0;

		if (ratio <= 0) {

			if (ratio < log2Min) {
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

			if (ratio > log2Max) {
				rValue = 255;
			} else {

				// differnce between 0 and minRatio
				double range = Math.abs(log2Max);
				double actual = range - Math.abs(ratio);

				double realRatio = 1 - (actual / range);
				rValue = (int) (255d * realRatio);
			}

		}
		int r = rValue;
		int g = 0;
		int b = bValue;
		return new Color(r, g, b);
	}
}
