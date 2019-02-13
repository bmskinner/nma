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
package com.bmskinner.nuclear_morphology.charting.charts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
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
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.Layer;

import com.bmskinner.nuclear_morphology.analysis.image.ImageConverter;
import com.bmskinner.nuclear_morphology.analysis.mesh.DefaultMesh;
import com.bmskinner.nuclear_morphology.analysis.mesh.DefaultMeshImage;
import com.bmskinner.nuclear_morphology.analysis.mesh.Mesh;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshCreationException;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshEdge;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshFace;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshImage;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshImageCreationException;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshVertex;
import com.bmskinner.nuclear_morphology.analysis.mesh.UncomparableMeshImageException;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalManager;
import com.bmskinner.nuclear_morphology.charting.ChartComponents;
import com.bmskinner.nuclear_morphology.charting.datasets.CellDataset;
import com.bmskinner.nuclear_morphology.charting.datasets.ChartDatasetCreationException;
import com.bmskinner.nuclear_morphology.charting.datasets.ComponentOutlineDataset;
import com.bmskinner.nuclear_morphology.charting.datasets.NuclearSignalDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.NucleusDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.NucleusMeshXYDataset;
import com.bmskinner.nuclear_morphology.charting.datasets.OutlineDataset;
import com.bmskinner.nuclear_morphology.charting.datasets.OutlineDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.DefaultCell;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.generic.BorderTag;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclei.LobedNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.gui.RotationMode;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;

import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

/**
 * Factory for creating outlines of cellular components and signals
 * 
 * @author bms41
 *
 */
public class OutlineChartFactory extends AbstractChartFactory {
	
	protected static final String NO_CONSENSUS_ERROR_LBL = "No consensus nucleus in dataset";

    public OutlineChartFactory(ChartOptions o) {
        super(o);
    }

    /**
     * Create a chart showing the nuclear signal locations in a consensus nucleus.
     * @return
     */
    public JFreeChart makeSignalOutlineChart() {

        try {

            if (!options.hasDatasets()) {
                finer("No datasets for signal outline chart");
                return createEmptyChart();
            }

            if (options.isMultipleDatasets()) {
                finer("Multiple datasets for signal outline chart");
                return createMultipleDatasetEmptyChart();
            }

            if (!options.firstDataset().getCollection().hasConsensus()) {
                finer("No consensus for signal outline chart");
                return createTextAnnotatedEmptyChart(NO_CONSENSUS_ERROR_LBL);
            }

            if (options.isShowWarp()) {
                finer("Warp chart for signal outline chart");
                return makeSignalWarpChart();
            }
			finer("Signal CoM for signal outline chart");
			return makeSignalCoMNucleusOutlineChart();
        } catch (ChartCreationException | ChartDatasetCreationException e) {
            warn("Error making signal chart");
            stack("Error making signal chart", e);
            return createErrorChart();
        }

    }
    
    /**
	 * Create a nucleus outline chart with nuclear signals drawn as transparent
	 * circles
	 * 
	 * @param dataset the AnalysisDataset to use to draw the consensus nucleus
	 * @param signalCoMs the dataset with the signal centre of masses
	 * @return
     * @throws ChartDatasetCreationException 
	 * @throws Exception
	 */
	private JFreeChart makeSignalCoMNucleusOutlineChart() throws ChartCreationException, ChartDatasetCreationException {

		XYDataset signalCoMs = new NuclearSignalDatasetCreator(options).createSignalCoMDataset();

		JFreeChart chart = new ConsensusNucleusChartFactory(options).makeNucleusOutlineChart();

		XYPlot plot = chart.getXYPlot();

		if (signalCoMs.getSeriesCount() > 0) {
			plot.setDataset(1, signalCoMs);

			XYLineAndShapeRenderer rend = new XYLineAndShapeRenderer();
			for (int series = 0; series < signalCoMs.getSeriesCount(); series++) {

				Shape circle = new Ellipse2D.Double(0, 0, 4, 4);
				rend.setSeriesShape(series, circle);

				String name = (String) signalCoMs.getSeriesKey(series);
				// int seriesGroup = getIndexFromLabel(name);
				UUID seriesGroup = getSignalGroupFromLabel(name);

				Optional<ISignalGroup> g = options.firstDataset().getCollection().getSignalGroup(seriesGroup);
				if(g.isPresent()){
					Paint colour = g.get().getGroupColour().orElse(ColourSelecter.getColor(series));
					rend.setSeriesPaint(series, colour);
				}

				rend.setBaseLinesVisible(false);
				rend.setBaseShapesVisible(true);
				rend.setBaseSeriesVisibleInLegend(false);
			}
			plot.setRenderer(1, rend);

			int j = 0;

			if(options.isShowAnnotations()) { // transparent ellipse surrounding CoM
				for (UUID signalGroup : options.firstDataset().getCollection().getSignalManager().getSignalGroupIDs()) {
					List<Shape> shapes = new NuclearSignalDatasetCreator(options)
							.createSignalRadiusDataset(options.firstDataset(), signalGroup);

					int signalCount = shapes.size();

					int alpha = (int) Math.floor(255 / ((double) signalCount)) + 20;
					alpha = alpha < 1 ? 1 : alpha > 156 ? 156 : alpha;

					Optional<ISignalGroup> g = options.firstDataset().getCollection().getSignalGroup(signalGroup);
					if(g.isPresent()){
						Paint colour = g.get().getGroupColour().orElse(ColourSelecter.getColor(j++));
						for (Shape s : shapes) {
							XYShapeAnnotation an = new XYShapeAnnotation(s, null, null,
									ColourSelecter.getTransparentColour((Color) colour, true, alpha)); // layer
							// transparent
							// signals
							plot.addAnnotation(an);
						}
					}
				}
			}
		}
		return chart;
	}

    /**
     * Draw the given images onto a consensus outline nucleus.
     * 
     * @param image the image processor to be drawn
     * @return
     */
    public JFreeChart makeSignalWarpChart(ImageProcessor image) {

    	// Create the outline of the nucleus
        JFreeChart chart = new ConsensusNucleusChartFactory(options).makeNucleusBareOutlineChart();

        XYPlot plot = chart.getXYPlot();

        // Make an outline of the component to draw
        XYDataset ds;
        try {
            ds = new NucleusDatasetCreator(options).createBareNucleusOutline(options.getComponent());
        } catch (ChartDatasetCreationException e) {
            stack("Error creating outline", e);
            return createErrorChart();
        }

        // Calculate the offset at which to draw the image
        // The plot area is larger than the image to be drawn 
        double xChartMin = DatasetUtilities.findMinimumDomainValue(ds).doubleValue();
        double yChartMin = DatasetUtilities.findMinimumRangeValue(ds).doubleValue();
        
     // Get the bounding box size for the consensus, to find the offsets for the images created
        Rectangle2D consensusBounds = options.getComponent().getBounds();
        fine(String.format("Consensus bounds: %s x %s : x%s y%s", consensusBounds.getWidth(), consensusBounds.getHeight(), consensusBounds.getX(), consensusBounds.getY() ));
        fine(String.format("Image bounds: %s x %s",image.getWidth(), image.getHeight()));
        
        int xOffset = (int) Math.round(-xChartMin);
        int yOffset = (int) Math.round(-yChartMin);

        drawImageAsAnnotation(image, plot, 255, -xOffset, -yOffset, options.isShowBounds());

        // Set the colour of the nucleus outline
        plot.setDataset(0, ds);
        plot.getRenderer(0).setBasePaint(Color.BLACK);
        plot.getRenderer(0).setBaseSeriesVisible(true);

        applyDefaultAxisOptions(chart);

        return chart;
    }

    private JFreeChart makeSignalWarpChart() {

        IAnalysisDataset dataset = options.firstDataset();

        // Create the outline of the consensus

        JFreeChart chart = new ConsensusNucleusChartFactory(options).makeNucleusOutlineChart();

        XYPlot plot = chart.getXYPlot();

        // Get consensus mesh.
        DefaultMesh meshConsensus;
        try {
            meshConsensus = new DefaultMesh(dataset.getCollection().getConsensus());
        } catch (MeshCreationException e) {
            stack("Error creating consensus mesh", e);
            return createErrorChart();
        }

        // Get the bounding box size for the consensus, to find the offsets for
        // the images created
        Rectangle2D r = dataset.getCollection().getConsensus().getBounds(); // .createPolygon().getBounds();
        r = r == null ? dataset.getCollection().getConsensus().toPolygon().getBounds() : r; // in
                                                                                            // case
                                                                                            // the
                                                                                            // bounds
                                                                                            // were
                                                                                            // not
                                                                                            // set
                                                                                            // (fixed
                                                                                            // 1.12.2)
        int w = (int) (r.getWidth() * 1.2);
        int h = (int) (r.getHeight() * 1.2);

        int xOffset = w >> 1;
        int yOffset = h >> 1;

        SignalManager m = dataset.getCollection().getSignalManager();
        Set<ICell> cells = m.getCellsWithNuclearSignals(options.getSignalGroup(), true);

        for (ICell cell : cells) {
            fine("Drawing signals for cell " + cell.getNucleus().getNameAndNumber());
            // Get each nucleus. Make a mesh.
            DefaultMesh cellMesh;
            try {
                cellMesh = new DefaultMesh(cell.getNucleus(), meshConsensus);
            } catch (MeshCreationException e1) {
                fine("Cannot make mesh for " + cell.getNucleus().getNameAndNumber());
                stack("Error creating mesh", e1);
                return createErrorChart();
            }

            // Get the image with the signal
            ImageProcessor ip;
            try {
                ImageProcessor warped;
                try {
                    ip = cell.getNucleus().getSignalCollection().getImage(options.getSignalGroup());

                    // Create NucleusMeshImage from nucleus.
                    MeshImage<Nucleus> im = new DefaultMeshImage(cellMesh, ip);

                    // Draw NucleusMeshImage onto consensus mesh.

                    warped = im.drawImage(meshConsensus);
                } catch (UncomparableMeshImageException | MeshImageCreationException e) {
                    fine("Cannot make mesh for " + cell.getNucleus().getNameAndNumber());
                    stack("Error creating mesh", e);
                    return createErrorChart();
                    // warped = null;
                }

                // ImagePlus image = new
                // ImagePlus(cell.getNucleus().getNameAndNumber(), warped);
                // image.show();
                drawImageAsAnnotation(warped, plot, 20, -xOffset, -yOffset, options.isShowBounds());

            } catch (UnloadableImageException e) {
                warn("Unable to load signal image for signal group " + options.getSignalGroup() + " in cell "
                        + cell.getNucleus().getNameAndNumber());
                stack("Unable to load signal image for signal group " + options.getSignalGroup() + " in cell "
                        + cell.getNucleus().getNameAndNumber(), e);
            }

        }
        XYDataset ds;
        try {
            ds = new NucleusDatasetCreator(options).createBareNucleusOutline(dataset);
        } catch (ChartDatasetCreationException e) {
            stack("Error creating outline", e);
            return createErrorChart();
        }
        plot.setDataset(0, ds);
        plot.getRenderer(0).setBasePaint(Color.BLACK);
        plot.getRenderer(0).setBaseSeriesVisible(true);
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

            if (options.isShowMesh()) {
                if (options.firstDataset().getCollection().hasConsensus()) {

                    try {

                        Mesh<Nucleus> mesh1 = options.getRotateMode().equals(RotationMode.ACTUAL)
                                ? new DefaultMesh(options.getCell().getNucleus())
                                : new DefaultMesh(options.getCell().getNucleus().getVerticallyRotatedNucleus());

                        Mesh<Nucleus> mesh2 = new DefaultMesh(options.firstDataset().getCollection().getConsensus(),
                                mesh1);

                        Mesh<Nucleus> result = mesh1.comparison(mesh2);
                        return createMeshChart(result, 0.5);

                    } catch (MeshCreationException e) {
                        stack("Error creating mesh", e);
                        return createErrorChart();
                    }

                }
				return createEmptyChart();

            }

            if (options.isShowWarp()) {
                if (options.firstDataset().getCollection().hasConsensus()) {

                    try {

                        Mesh<Nucleus> mesh1 = new DefaultMesh(options.getCell().getNucleus());
                        Mesh<Nucleus> mesh2 = new DefaultMesh(options.firstDataset().getCollection().getConsensus(),
                                mesh1);

                        //
                        ImageProcessor nucleusIP = options.getCell().getNucleus().getImage();

                        // Create a mesh image from the nucleus
                        MeshImage<Nucleus> im = new DefaultMeshImage(mesh1, nucleusIP);

                        // Draw the image onto the shape described by the
                        // consensus nucleus
                        ImageProcessor ip = im.drawImage(mesh2);

                        return drawImageAsAnnotation(ip);

                    } catch (UnloadableImageException e) {
                        warn("Cannot load nucleus image: "
                                + options.getCell().getNucleus().getSourceFile().getAbsolutePath());
                        stack("Error loading nucleus image", e);
                        return createErrorChart();
                    } catch (MeshImageCreationException e) {
                        fine("Cannot create mesh for " + options.getCell().getNucleus().getNameAndNumber());
                        stack("Error creating mesh", e);
                        return createErrorChart();
                    } catch (UncomparableMeshImageException e) {
                        fine("Cannot compare mesh for " + options.getCell().getNucleus().getNameAndNumber());
                        stack("Error comparing mesh", e);
                        return createErrorChart();
                    } catch (MeshCreationException e) {
                        stack("Error creating mesh", e);
                        return createErrorChart();
                    }

                }
				return createEmptyChart();
            }
            return makeStandardCellOutlineChart();
        } catch (ChartCreationException e) {
            warn("Error creating cell outline chart");
            fine("Error creating cell outline chart", e);
            return createErrorChart();
        }

    }

    private JFreeChart makeBareCellOutlineChart() throws ChartCreationException {

        if (!options.hasCell()) {
            finest("No cell to draw");
            return ConsensusNucleusChartFactory.createEmptyChart();
        }

        JFreeChart chart = createBaseXYChart();
        ComponentOutlineDataset<CellularComponent> ds = new ComponentOutlineDataset<CellularComponent>();

        ICell cell = options.getCell();

        if (cell.hasCytoplasm()) {

            try {
                new OutlineDatasetCreator(options, cell.getCytoplasm()).addOutline(ds, false);
            } catch (ChartDatasetCreationException e) {
                fine("Error making cytoplasm outline", e);
                return createErrorChart();
            }

        }

        if (cell.hasNucleus()) {

            for (Nucleus n : cell.getNuclei()) {

                try {
                    new OutlineDatasetCreator(options, n).addOutline(ds, false);
                } catch (ChartDatasetCreationException e) {
                    fine("Error making nucleus outline", e);
                    return createErrorChart();
                }

            }

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

        if (options.hasComponent()) {
            drawImageAsAnnotation(plot, options.getCell(), options.getComponent(), true);
        }
        applyDefaultAxisOptions(chart);
        return chart;

    }

    /**
     * Get a chart contaning the details of the given cell from the given
     * dataset
     * 
     * @param cell the cell to draw
     * @param dataset the dataset the cell came from
     * @param rotateMode the orientation of the image
     * @return
     * @throws Exception
     */
    private JFreeChart makeStandardCellOutlineChart() throws ChartCreationException {

        if (!options.hasCell())
            return ConsensusNucleusChartFactory.createEmptyChart();

        if (!options.hasDatasets())
            return ConsensusNucleusChartFactory.createEmptyChart();

        ICell cell = options.getCell();
        IAnalysisDataset dataset = options.firstDataset();

        if (options.getRotateMode().equals(RotationMode.VERTICAL)) {
            Nucleus n = cell.getNucleus().getVerticallyRotatedNucleus();
            cell = new DefaultCell(n);
        }

        CellDataset cellDataset = new CellDataset(cell);

        /*
         * Get the cytoplasm outline dataset
         */
        if (cell.hasCytoplasm()) {
            try {

                OutlineDataset<CellularComponent> cytoDataset = new OutlineDatasetCreator(options, cell.getCytoplasm())
                        .createOutline(false);
                cellDataset.addOutline(CellularComponent.CYTOPLASM, cytoDataset);

            } catch (ChartDatasetCreationException e) {

                throw new ChartCreationException("Unable to get cytoplasm outline", e);

            }
        }

        /*
         * Get the nucleus outline dataset
         */
        try {

            for (Nucleus n : cell.getNuclei()) {

                OutlineDataset<CellularComponent> nDataset = new OutlineDatasetCreator(options, n).createOutline(true);
                cellDataset.addOutline(CellularComponent.NUCLEUS + "_" + n.getID(), nDataset);

                if (options.isShowBorderTags()) {
                    XYDataset tags = new NucleusDatasetCreator(options).createNucleusIndexTags(n);
                    cellDataset.addTags("Tags_" + n.getID(), tags);
                }

                if (n instanceof LobedNucleus) {

                    OutlineDataset lobes = new NucleusDatasetCreator(options)
                            .createNucleusLobeDataset((LobedNucleus) n);
                    cellDataset.addLobes(CellularComponent.NUCLEAR_LOBE + "_" + n.getID(), lobes);
                }

                if (options.isShowSignals()) {
                    finest("Displaying signals on chart");
                    if (cell.getNucleus().getSignalCollection().hasSignal()) {

                        List<ComponentOutlineDataset> signalsDatasets = new NucleusDatasetCreator(options)
                                .createSignalOutlines(cell, dataset);

                        finest("Fetched signal outline datasets for " + cell.getNucleus().getNameAndNumber());

                        for (OutlineDataset d : signalsDatasets) {

                            for (int series = 0; series < d.getSeriesCount(); series++) {
                                String seriesKey = d.getSeriesKey(series).toString();

                                finest("Adding outline for " + seriesKey + " to dataset hash");

                                cellDataset.addOutline(seriesKey, d);
                            }
                        }
                    }
                }

                finest("Created nucleus outline");
            }

        } catch (ChartDatasetCreationException e) {

            throw new ChartCreationException("Unable to get nucleus outline", e);

        }

        return renderCellDataset(cellDataset, options);

    }

    /**
     * Render the given cell dataset according to the chart options
     * 
     * @param cellDataset
     * @param options
     * @return
     */
    private JFreeChart renderCellDataset(CellDataset cellDataset, ChartOptions options) {

        JFreeChart chart = createBaseXYChart();
        XYPlot plot = chart.getXYPlot();

        plot.getRangeAxis().setInverted(true);

        if (options.getRotateMode().equals(RotationMode.VERTICAL)) {
            // Need to have top point at the top of the image
            plot.getRangeAxis().setInverted(false);
        }

        // set the rendering options for each dataset type
        int i = 0;
        for (String key : cellDataset.getKeys()) {

            // log("Dataset "+key);

            XYDataset ds = cellDataset.getDataset(key);
            plot.setDataset(i, ds);

            boolean showLines = key.startsWith(CellularComponent.NUCLEAR_LOBE) ? true : options.isShowLines();
            boolean showPoints = key.startsWith(CellularComponent.NUCLEAR_LOBE) ? false : options.isShowPoints();
            XYLineAndShapeRenderer rend = new XYLineAndShapeRenderer(showLines, showPoints);
            plot.setRenderer(i, rend);

            // go through each series in the dataset

            int seriesCount = ds.getSeriesCount();

            for (int series = 0; series < seriesCount; series++) {

                // all datasets use the same stroke
                rend.setSeriesStroke(series, ChartComponents.MARKER_STROKE);
                rend.setSeriesVisibleInLegend(series, true);

                String name = ds.getSeriesKey(series).toString();
                // log("\t"+name);

                /*
                 * Segmented nucleus outline
                 */
                if (key.startsWith(CellularComponent.NUCLEUS)) {
                    int colourIndex = getIndexFromLabel(name);
                    Paint colour = ColourSelecter.getColor(colourIndex);
                    rend.setSeriesPaint(series, colour);
                }

                if (key.startsWith(CellularComponent.NUCLEAR_LOBE)) {
                    // rend.setSeriesShape(series,
                    // ChartComponents.DEFAULT_POINT_SHAPE);
                    rend.setSeriesPaint(series, ColourSelecter.DEFAULT_LOBE_OUTLINE);
                }

                /*
                 * Cytoplasm outline
                 */
                if (key.startsWith(CellularComponent.CYTOPLASM)) {
                    rend.setSeriesPaint(series, ColourSelecter.DEFAULT_CELL_OUTLINE);
                }

                /*
                 * Border tags
                 */

                if (key.startsWith("Tags_")) {
                    rend.setSeriesPaint(i, Color.BLACK);

                    String tagName = name.replace("Tag_", "");

                    if (tagName.equals(BorderTag.ORIENTATION_POINT.toString())) {
                        rend.setSeriesPaint(series, Color.BLUE);
                    }
                    if (tagName.equals(BorderTag.REFERENCE_POINT.toString())) {
                        rend.setSeriesPaint(series, Color.ORANGE);
                    }
                    if (tagName.equals(BorderTag.INTERSECTION_POINT.toString())) {
                        rend.setSeriesPaint(series, Color.CYAN);
                    }
                    if (tagName.equals(BorderTag.TOP_VERTICAL.toString())) {
                        rend.setSeriesPaint(series, Color.GRAY);
                    }
                    if (tagName.equals(BorderTag.BOTTOM_VERTICAL.toString())) {
                        rend.setSeriesPaint(series, Color.GRAY);
                    }

                }

                /*
                 * Nuclear signals
                 */
                if (key.startsWith(CellularComponent.NUCLEAR_SIGNAL)) {

                    UUID seriesGroup = getSignalGroupFromLabel(key);
                    
                    Optional<ISignalGroup> g = options.firstDataset().getCollection().getSignalGroup(seriesGroup);
                    if(g.isPresent()){
                    	Paint colour = g.get().getGroupColour().orElse(ColourSelecter.getColor(series));
                    	rend.setSeriesPaint(series, colour);
                    }
                }

            }

            // Add a background image to the plot
            clearShapeAnnotations(plot);

            ICell cell = cellDataset.getCell();
            if (options.getRotateMode().equals(RotationMode.ACTUAL)) {

                if (cell.hasCytoplasm()) { // if there is a cytoplasm, probably
                                           // H&E for now. Otherwise
                                           // fluorescence
                    drawImageAsAnnotation(plot, cell, cell.getCytoplasm(), true);
                } else {
                    drawImageAsAnnotation(plot, cell, options.getComponent(), false);
                }

            }

            i++;

        }
        applyDefaultAxisOptions(chart);
        return chart;
    }

    /**
     * Remove the XYShapeAnnotations from this image This will leave all other
     * annotation types.
     */
    private static void clearShapeAnnotations(XYPlot plot) {
        for (Object a : plot.getAnnotations()) {
            if (a.getClass() == XYShapeAnnotation.class) {
                plot.removeAnnotation((XYAnnotation) a);
            }
        }
    }

    /**
     * Create a chart with an image drawn as an annotation in the background
     * layer.
     * 
     * @param ip the image
     * @param plot the plot to draw on
     * @param alpha the opacity (0-255)
     * @param xOffset a position to move the image 0,0 to
     * @param yOffset a position to move the image 0,0 to
     * @return
     */
    private void drawImageAsAnnotation(ImageProcessor ip, XYPlot plot, int alpha, int xOffset, int yOffset,
            boolean showBounds) {
        plot.setBackgroundPaint(Color.WHITE);
        plot.getRangeAxis().setInverted(false);

        // Make a dataset to allow the autoscale to work even if no other datasets are present
        // Hide the dataset from visibility
        XYDataset bounds = new NucleusDatasetCreator(options).createAnnotationRectangleDataset(ip.getWidth(),
                ip.getHeight());
        plot.setDataset(0, bounds);
        XYItemRenderer rend = plot.getRenderer(0);
        rend.setBaseSeriesVisible(false);

        plot.getDomainAxis().setRange(0, ip.getWidth());
        plot.getRangeAxis().setRange(0, ip.getHeight());

        for (int x = 0; x < ip.getWidth(); x++) {
            for (int y = 0; y < ip.getHeight(); y++) {

                int pixel = ip.get(x, y);
                Color col = null;

                if (ip instanceof ColorProcessor) {
                    if (pixel < 16777215) {
                        col = new Color(pixel);
                        col = ColourSelecter.getTransparentColour(col, true, alpha);
                    }

                } else {
                    if (pixel < 255) // Ignore anything that is not signal - the background is already white
                        col = new Color(pixel, pixel, pixel, alpha);
                }

                if (col == null && showBounds)
                    col = new Color(255, 0, 0, alpha);

                if (col != null) {
                    // Ensure the 'pixels' overlap to avoid lines of background
                    // colour seeping through
                    Rectangle2D r = new Rectangle2D.Double(x + xOffset - 0.1, y + yOffset - 0.1, 1.2, 1.2);
                    XYShapeAnnotation a = new XYShapeAnnotation(r, null, null, col);

                    rend.addAnnotation(a, Layer.BACKGROUND);
                }
                
                // Draw the image border - debug only
//                if( x==0 || x==ip.getWidth()-1 ||y==0 || y==ip.getHeight()-1) {
//                	Rectangle2D r = new Rectangle2D.Double(x + xOffset - 0.1, y + yOffset - 0.1, 1.2, 1.2);
//                    rend.addAnnotation( new XYShapeAnnotation(r, null, null, Color.BLACK), Layer.BACKGROUND);
//                }
            }
        }

    }

    /**
     * Create a chart with an image drawn as an annotation in the background
     * layer. The image pixels are fully opaque
     * 
     * @param ip
     * @param alpha
     * @return
     */
    private void drawImageAsAnnotation(ImageProcessor ip, XYPlot plot, int alpha) {
        drawImageAsAnnotation(ip, plot, alpha, 0, 0, false);
    }

    /**
     * Create a chart with an image drawn as an annotation in the background
     * layer. The image pixels are fully opaque
     * 
     * @param ip
     * @return
     */
    private JFreeChart drawImageAsAnnotation(ImageProcessor ip) {
        return drawImageAsAnnotation(ip, 255);
    }

    /**
     * Create a chart with an image drawn as an annotation in the background
     * layer. The image pixels have the given alpha transparency value
     * 
     * @param ip
     * @param alpha
     * @return
     */
    private JFreeChart drawImageAsAnnotation(ImageProcessor ip, int alpha) {

        JFreeChart chart = ChartFactory.createXYLineChart(null, null, null, null, PlotOrientation.VERTICAL, true, true,
                false);

        XYPlot plot = chart.getXYPlot();
        drawImageAsAnnotation(ip, plot, alpha);
        return chart;
    }

    /**
     * Draw the greyscale image from teh given channel on the plot
     * 
     * @param imageFile
     * @param channel
     */
    private static void drawImageAsAnnotation(@NonNull XYPlot plot, @NonNull ICell cell, @NonNull CellularComponent component, boolean isRGB) {

        if (component == null || cell == null || plot == null)
            return;

        ImageProcessor openProcessor;
        try {
//        	ImageProcessor ip = isRGB ? component.getRGBImage() : component.getImage();
        	ImageConverter ic = new ImageConverter(component.getGreyscaleImage()).invert();
        	openProcessor = ic.convertToRGBGreyscale().toProcessor();
//            openProcessor = isRGB ? component.getRGBImage() : component.getGreyscaleImage();
        } catch (UnloadableImageException e) {
            return;
        }

        int[] positions = component.getPosition();

        XYItemRenderer rend = plot.getRenderer(0); // index zero should be the
                                                   // nucleus outline dataset

        int xBase = positions[CellularComponent.X_BASE];
        int yBase = positions[CellularComponent.Y_BASE];

        int padding = 10; // a border of pixels beyond the cell boundary
        int wideW = positions[CellularComponent.WIDTH] + (padding * 2);
        int wideH = positions[CellularComponent.HEIGHT] + (padding * 2);
        int wideX = xBase - padding;
        int wideY = yBase - padding;

        wideX = wideX < 0 ? 0 : wideX;
        wideY = wideY < 0 ? 0 : wideY;

        openProcessor.setRoi(wideX, wideY, wideW, wideH);
        openProcessor = openProcessor.crop();

        for (int x = 0; x < openProcessor.getWidth(); x++) {
            for (int y = 0; y < openProcessor.getHeight(); y++) {

                // int pixel = im.getRGB(x, y);
                int pixel = openProcessor.get(x, y);
                Color col = new Color(pixel);
                // Color col = new Color(pixel, pixel, pixel, 255);

                // Ensure the 'pixels' overlap to avoid lines of background
                // colour seeping through
                Rectangle2D r = new Rectangle2D.Double(xBase + x - padding - 0.6, yBase + y - padding - 0.6, 1.2, 1.2);
                XYShapeAnnotation a = new XYShapeAnnotation(r, null, null, col);

                rend.addAnnotation(a, Layer.BACKGROUND);
            }
        }
    }

    /**
     * Create a chart with the outlines of all the nuclei within a dataset. The
     * options should only contain a single dataset
     * 
     * @return
     */
    public JFreeChart createVerticalNucleiChart() {

        if (!options.hasDatasets())
            return createEmptyChart();
        if (options.isMultipleDatasets())
            return createMultipleDatasetVerticalNucleiChart();
        return createEmptyChart();
    }

    /**
     * Create the chart with the outlines of all the nuclei within a single
     * dataset.
     * 
     * @param options
     * @return
     * @throws Exception
     */
    private JFreeChart createMultipleDatasetVerticalNucleiChart() {

        JFreeChart chart = createBaseXYChart();
        XYPlot plot = chart.getXYPlot();

        plot.addRangeMarker(new ValueMarker(0, Color.LIGHT_GRAY, ChartComponents.PROFILE_STROKE));
        plot.addDomainMarker(new ValueMarker(0, Color.LIGHT_GRAY, ChartComponents.PROFILE_STROKE));

        StandardXYToolTipGenerator tooltip = new StandardXYToolTipGenerator();

        int i = 0;
        int datasetNumber = 0;
        for (IAnalysisDataset dataset : options.getDatasets()) {
        	Paint colour = dataset.getDatasetColour().orElse(ColourSelecter.getColor(datasetNumber++));

            XYLineAndShapeRenderer r = new XYLineAndShapeRenderer(true, false);
            r.setBaseSeriesVisibleInLegend(false);
            r.setBaseStroke(ChartComponents.PROFILE_STROKE);
            r.setSeriesPaint(0, colour);
            r.setBaseToolTipGenerator(tooltip);

            for (Nucleus n : dataset.getCollection().getNuclei()) {

                Nucleus verticalNucleus = n.getVerticallyRotatedNucleus();

                OutlineDatasetCreator dc = new OutlineDatasetCreator(options, verticalNucleus);

                try {

                    XYDataset nucleusDataset = dc.createOutline(false);
                    plot.setDataset(i, nucleusDataset);
                    plot.setRenderer(i, r);

                } catch (ChartDatasetCreationException e) {
                    warn("Cannot create data for dataset " + dataset.getName());
                    fine("Error getting chart data", e);
                } finally {
                    i++;
                }

            }
        }
        applyDefaultAxisOptions(chart);
        return chart;
    }

    /**
     * Create the chart with the outlines of all the nuclei within a single
     * dataset.
     * 
     * @param mesh the mesh to draw
     * @param log2ratio the ratio to set as full colour intensity
     * @param options the drawing options
     * @return
     * @throws Exception
     */
    public JFreeChart createMeshChart(Mesh<Nucleus> mesh, double log2Ratio) throws ChartCreationException {
    	JFreeChart chart = createBaseXYChart();
		XYPlot plot = chart.getXYPlot();
		
		int datasetIndex = 0;
		
		if(options.isShowMeshVertices()) {
			try {
				NucleusMeshXYDataset dataset = new NucleusDatasetCreator(options).createNucleusMeshVertexDataset(mesh);
				XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, true);
				renderer.setBaseSeriesVisibleInLegend(false);

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
		
		if(options.isShowMeshEdges()) {
			try {
				NucleusMeshXYDataset dataset = new NucleusDatasetCreator(options).createNucleusMeshEdgeDataset(mesh);
				XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
				renderer.setBaseSeriesVisibleInLegend(false);
				renderer.setBaseStroke(ChartComponents.MARKER_STROKE);

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

        // Show faces as polygon annotations under the chart. Faces are coloured by underlying segment if possible
        if (options.isShowMeshFaces()) {
        	
        	try {
				List<IBorderSegment> segments = options.firstDataset().getCollection().getConsensus().getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT).getSegments();
				
				for(int i=0; i<segments.size(); i++) {
					IBorderSegment s = segments.get(i);
					for (MeshFace f : mesh.getFaces(s)) {
						Path2D path = f.toPath();
	        			Color colour = ColourSelecter.getColor(i, options.getSwatch());
	        			XYShapeAnnotation a = new XYShapeAnnotation(path, null, null, colour);
	        			plot.getRenderer().addAnnotation(a, Layer.BACKGROUND);
					}
				}
				
        	} catch (UnavailableBorderTagException | UnavailableProfileTypeException | ProfileException e) {
        		stack("Error getting segments for mesh");
        		stack(e);
        		stack("Falling back to old mesh face annotation");

        		for (MeshFace f : mesh.getFaces()) {

        			Path2D path = f.toPath();
        			Color colour = getGradientColour(f.getLog2Ratio(), log2Ratio);
        			XYShapeAnnotation a = new XYShapeAnnotation(path, null, null, colour);
        			plot.getRenderer().addAnnotation(a, Layer.BACKGROUND);
        		}
        	}

           

        }

        /*
         * If the annotations are set, create a new set of labels for the
         * vertices
         */

        if (options.isShowAnnotations()) {

            for (MeshVertex v : mesh.getPeripheralVertices()) {
                XYTextAnnotation annotation = new XYTextAnnotation(v.getName(), v.getPosition().getX() - 1,
                        v.getPosition().getY());
                annotation.setPaint(Color.BLACK);
                plot.addAnnotation(annotation);
            }

            for (MeshVertex v : mesh.getInternalVertices()) {
                XYTextAnnotation annotation = new XYTextAnnotation(v.getName(), v.getPosition().getX() - 1,
                        v.getPosition().getY());
                annotation.setPaint(Color.BLACK);
                plot.addAnnotation(annotation);
            }

            if (options.isShowMeshEdges()) {

                for (MeshEdge v : mesh.getEdges()) {
                    XYTextAnnotation annotation = new XYTextAnnotation(v.getName(), v.getMidpoint().getX(),
                            v.getMidpoint().getY() + 1);
                    annotation.setPaint(Color.BLUE);
                    plot.addAnnotation(annotation);
                }
            }

            if (options.isShowMeshFaces()) {
                for (MeshFace f : mesh.getFaces()) {
                    XYTextAnnotation annotation = new XYTextAnnotation(f.getName(), f.getMidpoint().getX(),
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
     * @param minRatio
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

//    /**
//     * Create a histogram of log 2 ratios for a NucleusMesh
//     * 
//     * @param mesh
//     *            the comparison mesh with length ratios
//     * @return
//     * @throws Exception
//     */
//    public JFreeChart createMeshHistogram(Mesh<Nucleus> mesh) throws ChartCreationException {
//
//        HistogramDataset ds;
//        try {
//            ds = new NucleusDatasetCreator(options).createNucleusMeshHistogramDataset(mesh);
//        } catch (Exception e) {
//            throw new ChartCreationException("Cannot make mesh histogram", e);
//        }
//        JFreeChart chart = HistogramChartFactory.createHistogram(ds, "Log2 ratio", "Number of edges");
//        XYPlot plot = chart.getXYPlot();
//        plot.setBackgroundPaint(Color.WHITE);
//        plot.addDomainMarker(new ValueMarker(0, Color.BLACK, ChartComponents.PROFILE_STROKE));
//
//        return chart;
//    }

}
