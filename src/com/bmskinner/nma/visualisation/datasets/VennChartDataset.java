package com.bmskinner.nma.visualisation.datasets;

import java.awt.Color;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.data.xy.DefaultXYDataset;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.visualisation.datasets.VennChartDataset.VennCircle.VennShape;
import com.bmskinner.nma.visualisation.datasets.VennCounter.VennDatasetPosition;
import com.bmskinner.nma.visualisation.datasets.VennCounter.VennIntersection;

@SuppressWarnings("serial")
public class VennChartDataset extends DefaultXYDataset {

	private static final Logger LOGGER = Logger.getLogger(VennChartDataset.class.getName());

	/**
	 * @param dataset the dataset being displayed
	 * @param xCentre the x centre position
	 * @param yCentre the y centre position
	 * @param rx      the x radius
	 * @param ry      the y radius
	 * @author ben
	 *
	 */
	public record VennCircle(IAnalysisDataset dataset, double xCentre, double yCentre, double rx, double ry,
			VennShape shape) {

		public enum VennShape {
			CIRCLE, HALF_CIRCLE_LEFT, HALF_CIRCLE_RIGHT;
		}

		public VennCircle(IAnalysisDataset dataset, double xCentre, double yCentre, double rx, double ry) {
			this(dataset, xCentre, yCentre, rx, ry, VennShape.CIRCLE);
		}

		public VennCircle {
			if (dataset == null)
				throw new IllegalArgumentException("Dataset is null creating circle");
		}

		public double yBottom() {
			return (yCentre - ry) * 1.1;
		}

		public double yTop() {
			return (yCentre + ry) * 1.1;
		}

		public double xDiameter() {
			return rx + rx;
		}

		public double yDiameter() {
			return ry + ry;
		}

		public double xMax() {
			return xCentre + rx;
		}

		public double xMin() {
			return xCentre - rx;
		}

		public double yMin() {
			return yCentre - ry;
		}

		public double yMax() {
			return yCentre + ry;
		}

		/**
		 * Get the x coordinate at given fraction of the diameter
		 * 
		 * @param f
		 * @return
		 */
		public double xFraction(double f) {
			return (xDiameter() * f) + xMin();
		}

		public double yFraction(double f) {
			return (yDiameter() * f) + yMin();
		}

		public XYShapeAnnotation toAnnotation(Color fill, Color outline,
				Stroke stroke) {
			
			Area s = new Area(new Ellipse2D.Double(xCentre - rx, yCentre - ry, rx + rx,
					ry + ry));

			if (VennShape.HALF_CIRCLE_LEFT.equals(shape)) {
				Area ra = new Area(new Rectangle2D.Double(xCentre, yCentre - ry, rx, ry + ry));
				s.subtract(ra);
			}

			if (VennShape.HALF_CIRCLE_RIGHT.equals(shape)) {
				Area ra = new Area(new Rectangle2D.Double(xCentre - rx, yCentre - ry, rx, ry + ry));
				s.subtract(ra);
			}

			return new XYShapeAnnotation(s, stroke, outline, fill);
		}

	}

	private static final int X_OFFSET = 4;

	private static final double Y_START = 0;

	private static final double DEFAULT_RADIUS = 0.7;
	private static final double HALF_RADIUS = 0.35;
	private static final double SUBSET_RADIUS = 0.17;

	/** Store the distinct clusters of datasets with shared cells */
	private Map<Comparable<?>, List<IAnalysisDataset>> clusters = new HashMap<>();

	/** Store the radii of Venn circles */
	private List<VennCircle> circles = new ArrayList<>();

	/** The locations of annotations with the shared counts */
	private List<Label> labels = new ArrayList<>();

	/**
	 * Store locations for count strings
	 * 
	 * @author bs19022
	 *
	 */
	public record Label(double x, double y, String label) {

		public Label(double x, double y, int i) {
			this(x, y, String.valueOf(i));
		}

		@Override
		public String toString() {
			return x + ", " + y + ": " + label;
		}
	}

	/**
	 * Create with datasets to be plotted
	 * 
	 * @param datasets
	 */
	public VennChartDataset(List<IAnalysisDataset> datasets) {
		super();
		for (IAnalysisDataset d : datasets) {
			addDataset(d);
		}
		createSeries();
	}

	/**
	 * We can only display a cluster of up to n circles. Checks the dataset can be
	 * drawn.
	 * 
	 * @return true if we can draw the dataset, false otherwise
	 */
	public boolean isValid() {
		return clusters.values().stream().allMatch(l -> l.size() <= 4);
	}

	public List<VennCircle> getCircles() {
		return circles;
	}

	/**
	 * Get the analysis datasets in the given cluster
	 * 
	 * @param clusterKey
	 * @return
	 */
	public List<IAnalysisDataset> getDatasets(Comparable<?> clusterKey) {
		return clusters.get(clusterKey);
	}

	public List<Label> getLabels() {
		return labels;
	}

	/**
	 * Create the Venn circles for a cluster
	 * 
	 * @param vc     the shared cell counts
	 * @param xStart the location of the datasets in the plot
	 */
	private void createLayout(VennCounter vc, double xStart) {

		LOGGER.fine(vc.getType());
		// Set the centre for each dataset circle in the cluster
		if (vc.size() == 1) {
			layoutType0001(vc, xStart);
		}

		if (vc.size() == 2) {
			switch (vc.getType()) {
			case "0011":
				layoutType0011(vc, xStart);
				break;
			default:
				layoutType0012(vc, xStart);
			}
		}

		if (vc.size() == 3) {
			switch (vc.getType()) {
			case "0020":
				layoutType0020(vc, xStart);
				break;
			case "0021":
				layoutType0021(vc, xStart);
				break;
			case "0022":
				layoutType0023(vc, xStart);
				break;
			case "0023":
				layoutType0023(vc, xStart);
				break;
			default:
				layoutType0133(vc, xStart);
			}

		}

		if (vc.size() == 4) {

			switch (vc.getType()) {
			case "0030":
				layoutType0030(vc, xStart);
				break;
			case "0033":
				layoutType0033(vc, xStart);
				break;
			default:
				layoutType1464(vc, xStart);
			}

		}
	}

	/**
	 * Single circle
	 * 
	 * @param vc
	 * @param xStart
	 */
	private void layoutType0001(VennCounter vc, double xStart) {

		VennCircle circ = new VennCircle(vc.getDataset(VennDatasetPosition.A),
				xStart, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS);
		circles.add(circ);

		// Count
		labels.add(new Label(circ.xCentre(), circ.yCentre(), String.valueOf(vc.getCount(VennIntersection.A))));

		// Name
		labels.add(new Label(circ.xCentre(), circ.yBottom(), vc.getDataset(VennDatasetPosition.A).getName()));
	}

	/**
	 * Two circles, one entirely within the other
	 * 
	 * @param vc
	 * @param xStart
	 */
	private void layoutType0011(VennCounter vc, double xStart) {
		VennCircle a = new VennCircle(vc.getDataset(VennDatasetPosition.A),
				xStart, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS);

		double fOverlapOfA = Math.max(vc.getCount(VennIntersection.AB) / (double) (vc.getCount(VennIntersection.A)
				+ vc.getCount(VennIntersection.B) + vc.getCount(VennIntersection.AB)) * 0.9, 0.1);

		VennCircle b = new VennCircle(vc.getDataset(VennDatasetPosition.B),
				xStart, Y_START, fOverlapOfA * DEFAULT_RADIUS, fOverlapOfA * DEFAULT_RADIUS);

		circles.add(a);
		circles.add(b);

		Label cA = new Label((b.xMin() + a.xMin()) / 2, a.yCentre(), vc.getCount(VennIntersection.A));
		Label cAB = new Label(b.xCentre(), b.yCentre(), vc.getCount(VennIntersection.AB));

		labels.add(cA);
		labels.add(cAB);

		labels.add(new Label(cA.x(), a.yBottom(), vc.getDataset(VennDatasetPosition.A).getName()));
		labels.add(new Label(cAB.x(), a.yTop(), vc.getDataset(VennDatasetPosition.B).getName()));

	}

	/**
	 * Two circles, partly overlapping
	 * 
	 * @param vc
	 * @param xStart
	 */
	private void layoutType0012(VennCounter vc, double xStart) {

		VennCircle a = new VennCircle(vc.getDataset(VennDatasetPosition.A),
				xStart, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS);

		double fOverlapOfA = vc.getCount(VennIntersection.AB) / (double) (vc.getCount(VennIntersection.A)
				+ vc.getCount(VennIntersection.B) + vc.getCount(VennIntersection.AB)) * 0.9;

		// Scale x overlap position by the fraction of overlapping cells
		double bxCentre = a.xCentre() + (a.xDiameter() * (1 - fOverlapOfA));
		VennCircle b = new VennCircle(vc.getDataset(VennDatasetPosition.B),
				bxCentre, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS);

		circles.add(a);
		circles.add(b);

		// Place count labels in the centre of the space between circles
		Label cA = new Label((b.xMin() + a.xMin()) / 2,
				a.yCentre(),
				String.valueOf(vc.getCount(VennIntersection.A)));

		Label cB = new Label((b.xMax() + a.xMax()) / 2,
				b.yCentre(),
				String.valueOf(vc.getCount(VennIntersection.B)));
		
		Label cAB = new Label((b.xMin() + a.xMax()) / 2, a.yCentre(),
				String.valueOf(vc.getCount(VennIntersection.AB)));

		labels.add(cA);
		labels.add(cB);
		labels.add(cAB);
		
		labels.add(new Label(cA.x(), a.yBottom(), vc.getDataset(VennDatasetPosition.A).getName()));
		labels.add(new Label(cB.x(), b.yBottom(), vc.getDataset(VennDatasetPosition.B).getName()));

	}

	/**
	 * Three circles, two entirely within the third, no unique cells in the outer
	 * 
	 * @param vc
	 * @param xStart
	 */
	private void layoutType0020(VennCounter vc, double xStart) {

		VennCircle b = new VennCircle(vc.getDataset(VennDatasetPosition.B),
				xStart, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS);

		VennCircle a = new VennCircle(vc.getDataset(VennDatasetPosition.A),
				xStart - 0.01, Y_START, DEFAULT_RADIUS * 0.96, DEFAULT_RADIUS * 0.96, VennShape.HALF_CIRCLE_LEFT);

		VennCircle c = new VennCircle(vc.getDataset(VennDatasetPosition.C),
				xStart + 0.01, Y_START, DEFAULT_RADIUS * 0.96, DEFAULT_RADIUS * 0.96, VennShape.HALF_CIRCLE_RIGHT);

		circles.add(a);
		circles.add(b);
		circles.add(c);

		Label cAB = new Label(a.xFraction(0.25), a.yCentre(), vc.getCount(VennIntersection.AB));
		Label cBC = new Label(c.xFraction(0.75), b.yCentre(), vc.getCount(VennIntersection.BC));

		labels.add(cAB);
		labels.add(cBC);

		labels.add(new Label(cAB.x(), b.yTop(), vc.getDataset(VennDatasetPosition.A).getName()));
		labels.add(new Label(b.xCentre(), b.yBottom(), vc.getDataset(VennDatasetPosition.B).getName()));
		labels.add(new Label(cBC.x(), b.yTop(), vc.getDataset(VennDatasetPosition.C).getName()));
	}

	/**
	 * Three circles, two entirely within the third
	 * 
	 * @param vc
	 * @param xStart
	 */
	private void layoutType0021(VennCounter vc, double xStart) {


		VennCircle b = new VennCircle(vc.getDataset(VennDatasetPosition.B),
				xStart, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS);

		VennCircle a = new VennCircle(vc.getDataset(VennDatasetPosition.A),
				xStart - 0.3, Y_START, SUBSET_RADIUS, SUBSET_RADIUS);

		VennCircle c = new VennCircle(vc.getDataset(VennDatasetPosition.C),
				xStart + 0.3, Y_START, SUBSET_RADIUS, SUBSET_RADIUS);

		circles.add(a);
		circles.add(b);
		circles.add(c);


		Label cB = new Label(b.xCentre(), b.yCentre(), vc.getCount(VennIntersection.B));
		Label cAB = new Label(a.xCentre(), a.yCentre(), vc.getCount(VennIntersection.AB));
		Label cBC = new Label(c.xCentre(), b.yCentre(), vc.getCount(VennIntersection.BC));

		labels.add(cB);
		labels.add(cAB);
		labels.add(cBC);

		labels.add(new Label(cAB.x(), b.yTop(), vc.getDataset(VennDatasetPosition.A).getName()));
		labels.add(new Label(cB.x(), b.yBottom(), vc.getDataset(VennDatasetPosition.B).getName()));
		labels.add(new Label(cBC.x(), b.yTop(), vc.getDataset(VennDatasetPosition.C).getName()));
	}

	/**
	 * Three circles, flat
	 * 
	 * @param vc
	 * @param xStart
	 */
	private void layoutType0023(VennCounter vc, double xStart) {
		VennCircle a = new VennCircle(vc.getDataset(VennDatasetPosition.A),
				xStart, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS);

		VennCircle b = new VennCircle(vc.getDataset(VennDatasetPosition.B),
				a.xCentre() + 0.8, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS);

		VennCircle c = new VennCircle(vc.getDataset(VennDatasetPosition.C),
				b.xCentre() + 0.8, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS);

		circles.add(a);
		circles.add(b);
		circles.add(c);
		
		Label cA = new Label((b.xMin() + a.xMin()) / 2,
				a.yCentre(),
				vc.getCount(VennIntersection.A));
		
		Label cB = new Label(b.xCentre(),
				b.yCentre(),
				vc.getCount(VennIntersection.B));

		Label cC = new Label((b.xMax() + c.xMax()) / 2,
				c.yCentre(),
				vc.getCount(VennIntersection.C));

		Label cAB = new Label((b.xMin() + a.xMax()) / 2,
				a.yCentre(),
				vc.getCount(VennIntersection.AB));

		Label cBC = new Label((c.xMin() + b.xMax()) / 2,
				b.yCentre(),
				vc.getCount(VennIntersection.BC));

		labels.add(cA);
		labels.add(cB);
		labels.add(cC);
		labels.add(cAB);
		labels.add(cBC);

		labels.add(new Label(cA.x(), a.yBottom(), vc.getDataset(VennDatasetPosition.A).getName()));
		labels.add(new Label(cB.x(), b.yBottom(), vc.getDataset(VennDatasetPosition.B).getName()));
		labels.add(new Label(cC.x(), a.yBottom(), vc.getDataset(VennDatasetPosition.C).getName()));
	}

	/**
	 * Three circles, triangle
	 * 
	 * @param vc
	 * @param xStart
	 */
	private void layoutType0133(VennCounter vc, double xStart) {

		VennCircle a = new VennCircle(vc.getDataset(VennDatasetPosition.A),
				xStart, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS);

		VennCircle b = new VennCircle(vc.getDataset(VennDatasetPosition.B),
				a.xCentre() + 0.8, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS);

		VennCircle c = new VennCircle(vc.getDataset(VennDatasetPosition.C),
				(a.xCentre + b.xCentre) / 2, Y_START + 0.6, DEFAULT_RADIUS, DEFAULT_RADIUS);

		circles.add(a);
		circles.add(b);
		circles.add(c);
		
		// Need to redraw all label positions
		labels.clear();

		Label cA = new Label((b.xMin() + a.xMin()) / 2,
				(a.yMin() + c.yMin()) / 2,
				String.valueOf(vc.getCount(VennIntersection.A)));

		Label cB = new Label((b.xMax() + a.xMax()) / 2,
				(b.yMin() + c.yMin()) / 2,
				String.valueOf(vc.getCount(VennIntersection.B)));

		Label cC = new Label(c.xCentre,
				(a.yMax() + c.yMax()) / 2,
				String.valueOf(vc.getCount(VennIntersection.C)));

		Label cAB = new Label((b.xMin() + a.xMax()) / 2, (a.yMin() + c.yMin()) / 2,
				String.valueOf(vc.getCount(VennIntersection.AB)));

		Label cAC = new Label((c.xMin() + b.xMin()) / 2,
						(c.yMin() + a.yMax()) / 2,
						String.valueOf(vc.getCount(VennIntersection.AC)));

		Label cBC = new Label((c.xMax() + a.xMax()) / 2,
				(c.yMin() + b.yMax()) / 2,
				String.valueOf(vc.getCount(VennIntersection.BC)));

		Label cABC = new Label((a.xMax() + b.xMin()) / 2,
				(c.yMin() + a.yMax()) / 2,
				String.valueOf(vc.getCount(VennIntersection.ABC)));

		labels.add(cA);
		labels.add(cB);
		labels.add(cC);
		labels.add(cAB);
		labels.add(cAC);
		labels.add(cBC);
		labels.add(cABC);


		labels.add(new Label(cA.x(), a.yBottom(), vc.getDataset(VennDatasetPosition.A).getName()));
		labels.add(new Label(cB.x(), b.yBottom(), vc.getDataset(VennDatasetPosition.B).getName()));
		labels.add(new Label(c.xCentre(), c.yTop(), vc.getDataset(VennDatasetPosition.C).getName()));


	}

	/**
	 * Four circles, all intersecting
	 * 
	 * @param vc
	 * @param xStart
	 */
	private void layoutType1464(VennCounter vc, double xStart) {

		VennCircle a = new VennCircle(vc.getDataset(VennDatasetPosition.A),
				xStart, Y_START, HALF_RADIUS, DEFAULT_RADIUS);

		VennCircle b = new VennCircle(vc.getDataset(VennDatasetPosition.B),
				xStart + 0.5, Y_START, HALF_RADIUS, DEFAULT_RADIUS);

		VennCircle c = new VennCircle(vc.getDataset(VennDatasetPosition.C),
				xStart + 0.5, Y_START - 0.5, DEFAULT_RADIUS, HALF_RADIUS);

		VennCircle d = new VennCircle(vc.getDataset(VennDatasetPosition.D),
				xStart + 0.5, Y_START, DEFAULT_RADIUS, HALF_RADIUS);

		circles.add(a);
		circles.add(b);
		circles.add(c);
		circles.add(d);

		Label cA = new Label(a.xCentre(), a.yFraction(0.9), vc.getCount(VennIntersection.A));
		Label cB = new Label(b.xCentre(), b.yFraction(0.9), vc.getCount(VennIntersection.B));
		Label cC = new Label(c.xFraction(0.9), c.yCentre(), vc.getCount(VennIntersection.C));
		Label cD = new Label(d.xFraction(0.9), d.yCentre(), vc.getCount(VennIntersection.D));

		Label cAB = new Label((b.xMin() + a.xMax()) / 2, a.yFraction(0.775), vc.getCount(VennIntersection.AB));
		Label cAC = new Label(a.xCentre(), c.yCentre(), vc.getCount(VennIntersection.AC));
		Label cAD = new Label(a.xCentre(), d.yCentre(), vc.getCount(VennIntersection.AD));
		Label cBC = new Label(b.xCentre(), c.yCentre(), vc.getCount(VennIntersection.BC));
		Label cBD = new Label(b.xCentre(), d.yCentre(), vc.getCount(VennIntersection.BD));
		Label cCD = new Label(c.xFraction(0.775), (d.yMin() + c.yMax()) / 2, vc.getCount(VennIntersection.CD));

		Label cABC = new Label((b.xMin() + a.xMax()) / 2, a.yFraction(0.225), vc.getCount(VennIntersection.ABC));
		Label cABD = new Label((b.xMin() + a.xMax()) / 2, d.yCentre(), vc.getCount(VennIntersection.ABD));
		Label cACD = new Label(a.xFraction(0.65), (d.yMin() + c.yMax()) / 2, vc.getCount(VennIntersection.ACD));
		Label cBCD = new Label(b.xCentre(), (d.yMin() + c.yMax()) / 2, vc.getCount(VennIntersection.BCD));

		Label cABCD = new Label((b.xMin() + a.xMax()) / 2, (d.yMin() + c.yMax()) / 2,
				vc.getCount(VennIntersection.ABCD));

		labels.add(cA);
		labels.add(cB);
		labels.add(cC);
		labels.add(cD);

		labels.add(cAB);
		labels.add(cAC);
		labels.add(cAD);
		labels.add(cBC);
		labels.add(cBD);
		labels.add(cCD);

		labels.add(cABC);
		labels.add(cABD);
		labels.add(cACD);
		labels.add(cBCD);

		labels.add(cABCD);
	}

	/**
	 * Four circles, three entirely within the fourth, but not overlapping
	 * 
	 * @param vc
	 * @param xStart
	 */
	private void layoutType0030(VennCounter vc, double xStart) {

		VennCircle b = new VennCircle(vc.getDataset(VennDatasetPosition.B),
				xStart, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS);

		VennCircle a = new VennCircle(vc.getDataset(VennDatasetPosition.A),
				xStart - 0.3, Y_START - 0.3, SUBSET_RADIUS, SUBSET_RADIUS);

		VennCircle c = new VennCircle(vc.getDataset(VennDatasetPosition.C),
				xStart + 0.3, Y_START - 0.3, SUBSET_RADIUS, SUBSET_RADIUS);

		VennCircle d = new VennCircle(vc.getDataset(VennDatasetPosition.D),
				xStart, Y_START + 0.3, SUBSET_RADIUS, SUBSET_RADIUS);

		circles.add(a);
		circles.add(b);
		circles.add(c);
		circles.add(d);

		Label cAB = new Label(a.xCentre(), a.yCentre(), vc.getCount(VennIntersection.AB));
		Label cBC = new Label(c.xCentre(), c.yCentre(), vc.getCount(VennIntersection.BC));
		Label cBD = new Label(d.xCentre(), d.yCentre(), vc.getCount(VennIntersection.BD));

		labels.add(cAB);
		labels.add(cBC);
		labels.add(cBD);
	}

	/**
	 * Four circles, one central intersecting the others once
	 * 
	 * @param vc
	 * @param xStart
	 */
	private void layoutType0033(VennCounter vc, double xStart) {

		VennCircle b = new VennCircle(vc.getDataset(VennDatasetPosition.B),
				xStart, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS);

		VennCircle a = new VennCircle(vc.getDataset(VennDatasetPosition.A),
				xStart - 1, Y_START - 0.4, DEFAULT_RADIUS, DEFAULT_RADIUS);

		VennCircle c = new VennCircle(vc.getDataset(VennDatasetPosition.C),
				xStart + 1, Y_START - 0.4, DEFAULT_RADIUS, DEFAULT_RADIUS);

		VennCircle d = new VennCircle(vc.getDataset(VennDatasetPosition.D),
				xStart, Y_START + 1, DEFAULT_RADIUS, DEFAULT_RADIUS);

		circles.add(a);
		circles.add(b);
		circles.add(c);
		circles.add(d);

		Label cA = new Label(a.xCentre(), a.yCentre(), vc.getCount(VennIntersection.A));
		Label cB = new Label(b.xCentre(), b.yCentre(), vc.getCount(VennIntersection.B));
		Label cC = new Label(c.xCentre(), c.yCentre(), vc.getCount(VennIntersection.C));
		Label cD = new Label(d.xCentre(), d.yCentre(), vc.getCount(VennIntersection.D));

		Label cAB = new Label((a.xMax() + b.xMin()) / 2, (a.yMax() + b.yMin()) / 2, vc.getCount(VennIntersection.AB));
		Label cBC = new Label((b.xMax() + c.xMin()) / 2, (c.yMax() + b.yMin()) / 2, vc.getCount(VennIntersection.BC));
		Label cBD = new Label(b.xCentre(), (b.yMin() + d.yMax()) / 2, vc.getCount(VennIntersection.BD));

		labels.add(cA);
		labels.add(cB);
		labels.add(cC);
		labels.add(cD);

		labels.add(cAB);
		labels.add(cBC);
		labels.add(cBD);
	}



	/**
	 * Add a new analysis dataset to this charting dataset
	 * 
	 * @param dataset
	 */
	private void addDataset(IAnalysisDataset dataset) {

		// Always declare a new cluster for first entry
		if (clusters.isEmpty()) {
			clusters.put("Cluster_" + clusters.size(), new ArrayList<>(Arrays.asList(dataset)));
			return;
		}

		// Check if we can add the dataset to an existing cluster
		boolean wasAdded = false;
		for (List<IAnalysisDataset> cluster : clusters.values()) {
			boolean addToCluster = cluster.stream().anyMatch(d -> d.getCollection().countShared(dataset) > 0);

			if (addToCluster) {
				cluster.add(dataset);
				wasAdded = true;
				break; // only add to the first cluster with matches
			}
		}

		// If not, make a new cluster
		if (!wasAdded) {
			clusters.put("Cluster_" + clusters.size(), new ArrayList<>(Arrays.asList(dataset)));
		}

		// Check if we can collapse any clusters with the latest addition
		collapseClusters();
	}

	private void createSeries() {
		int xStart = 0;

		for (Entry<Comparable<?>, List<IAnalysisDataset>> entry : clusters.entrySet()) {
			List<IAnalysisDataset> datasets = entry.getValue();

			VennCounter vc = new VennCounter(datasets);

//			LOGGER.fine(vc.getType());
			createLayout(vc, xStart);
			xStart += X_OFFSET;
		}

		// Create sentinal points to allow aspect ratio scaling of chart without
		// clipping annotated Venn circle outlines. These are points outside the range
		// of the circles.

		double xMax = circles.stream().mapToDouble(VennCircle::xMax).max().orElse(1) * 1.1;
		double xMin = circles.stream().mapToDouble(VennCircle::xMin).min().orElse(1) * 1.1;
		double yMax = circles.stream().mapToDouble(VennCircle::yMax).max().orElse(1) * 1.1;
		double yMin = circles.stream().mapToDouble(VennCircle::yMin).min().orElse(1) * 1.1;
		addSeries("Sentinals", new double[][] { { xMin, xMax }, { yMin, yMax } });
	}

	/**
	 * Identify clusters with shared nuclei and collapse them
	 * 
	 */
	private void collapseClusters() {

		if (clusters.size() == 1) {
			return;
		}

		Map<Comparable<?>, Boolean> includeInFinal = new HashMap<>();

		// Check if we can collapse any clusters with the latest addition
		Map<Comparable<?>, List<IAnalysisDataset>> replacementClusters = new HashMap<>();

		for (Entry<Comparable<?>, List<IAnalysisDataset>> entry1 : clusters.entrySet()) {
			for (Entry<Comparable<?>, List<IAnalysisDataset>> entry2 : clusters.entrySet()) {
				if (entry1.getKey().equals(entry2.getKey())) {
					continue;
				}

				boolean matchFound = entry1.getValue().stream().anyMatch(
						d -> entry2.getValue().stream().anyMatch(d2 -> d.getCollection().countShared(d2) > 0));

				if (matchFound) {
					entry1.getValue().addAll(entry2.getValue());
					entry2.getValue().clear();
					includeInFinal.put(entry2.getKey(), false);
				}

				includeInFinal.putIfAbsent(entry1.getKey(), true);

			}
		}

		// Remove entry2 from consideration if absorbed into entry1
		for (Entry<Comparable<?>, Boolean> entry : includeInFinal.entrySet()) {
			if (entry.getValue()) {
				replacementClusters.put("Cluster_" + replacementClusters.size(), clusters.get(entry.getKey()));
			}
		}

		clusters.clear();
		clusters.putAll(replacementClusters);
	}
}
