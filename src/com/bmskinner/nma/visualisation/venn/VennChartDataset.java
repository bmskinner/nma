package com.bmskinner.nma.visualisation.venn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.jfree.data.xy.DefaultXYDataset;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.utility.NumberTools;
import com.bmskinner.nma.visualisation.venn.VennCounter.VennDatasetPosition;
import com.bmskinner.nma.visualisation.venn.VennCounter.VennIntersection;
import com.bmskinner.nma.visualisation.venn.VennShape.VennShapeType;

@SuppressWarnings("serial")
public class VennChartDataset extends DefaultXYDataset {

	private static final Logger LOGGER = Logger.getLogger(VennChartDataset.class.getName());

	private static final int X_OFFSET = 4;

	private static final double Y_START = 0;

	private static final double DEFAULT_RADIUS = 0.7;
	private static final double HALF_RADIUS = 0.35;
	private static final double SUBSET_RADIUS = 0.17;

	/** Store the distinct clusters of datasets with shared cells */
	private final Map<Comparable<?>, List<IAnalysisDataset>> clusters = new HashMap<>();

	/** Store the Venn shapes */
	private final List<VennShape> circles = new ArrayList<>();

	/** The locations of annotations with the shared counts */
	private final List<Label> labels = new ArrayList<>();

	/**
	 * Store locations for count strings
	 * 
	 * @author bs19022
	 *
	 */
	public record Label(double x, double y, String label, boolean isInt) {

		public Label(double x, double y, String label) {
			this(x, y, label, false);
		}

		public Label(double x, double y, int i) {
			this(x, y, String.valueOf(i), true);
		}

		public int intValue() {
			return Integer.valueOf(label);
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
		for (final IAnalysisDataset d : datasets) {
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
		return clusters.values().stream().allMatch(l -> l.size() <= 5);
	}

	public List<VennShape> getCircles() {
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

		// Set the centre for each dataset circle in the cluster
		if (vc.size() == 1) {
			layoutType00001(vc, xStart);
		}

		if (vc.size() == 2) {
			switch (vc.getType()) {
			case "00011":
				layoutType00011(vc, xStart);
				break;
			default:
				layoutType00012(vc, xStart);
			}
		}

		if (vc.size() == 3) {
			switch (vc.getType()) {
			case "00111":
				layoutType00111(vc, xStart);
				break;
			case "00020":
				layoutType00020(vc, xStart);
				break;
			case "00021":
				layoutType00021(vc, xStart);
				break;
			case "00022":
				layoutType00023(vc, xStart);
				break;
			case "00023":
				layoutType00023(vc, xStart);
				break;
			default:
				layoutType00133(vc, xStart);
			}

		}

		if (vc.size() == 4) {

			switch (vc.getType()) {
			case "00030":
				layoutType00030(vc, xStart);
				break;
			case "00033":
				layoutType00033(vc, xStart);
				break;

			case "00042":
				layoutType00042(vc, xStart);
				break;
			case "00201":
				layoutType00201(vc, xStart);
				break;
			case "00231":
				layoutType00231(vc, xStart);
				break;
			default:
				layoutType01464(vc, xStart);
			}

		}

		if (vc.size() == 5) {
			switch (vc.getType()) {

			case "00060":
				layoutType00060(vc, xStart);
				break;
			case "00420":
				layoutType00420(vc, xStart);
				break;

			default:
				layoutTypeFiveFull(vc, xStart);
			}
		}

	}

	/**
	 * Single circle
	 * 
	 * @param vc
	 * @param xStart
	 */
	private void layoutType00001(VennCounter vc, double xStart) {

		final VennShape circ = new VennShape(vc.getDataset(VennDatasetPosition.A),
				xStart, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS);
		circles.add(circ);

		// Count
		labels.add(new Label(circ.xCentre(), circ.yCentre(),
				vc.getCount(VennIntersection.A)));

		// Name
		labels.add(new Label(circ.xCentre(), circ.yBottom(),
				vc.getDataset(VennDatasetPosition.A).getName()));
	}

	/**
	 * Two circles, one entirely within the other
	 * 
	 * @param vc
	 * @param xStart
	 */
	private void layoutType00011(VennCounter vc, double xStart) {
		final VennShape a = new VennShape(vc.getDataset(VennDatasetPosition.A),
				xStart, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS);

		final double fBOverlapOfA = NumberTools.clamp(
				vc.getCount(VennIntersection.AB) / (double) (vc.getCount(VennIntersection.A)
						+ vc.getCount(VennIntersection.B) + vc.getCount(VennIntersection.AB)),
				0.1, 0.9);

		final VennShape b = new VennShape(vc.getDataset(VennDatasetPosition.B),
				xStart, Y_START, a.xRadius() * fBOverlapOfA, a.yRadius() * fBOverlapOfA);

		circles.add(a);
		circles.add(b);

		final Label cA = new Label((b.xMin() + a.xMin()) / 2, a.yCentre(),
				vc.getCount(VennIntersection.A));
		final Label cAB = new Label(b.xCentre(), b.yCentre(), vc.getCount(VennIntersection.AB));

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
	private void layoutType00012(VennCounter vc, double xStart) {

		final VennShape a = new VennShape(vc.getDataset(VennDatasetPosition.A),
				xStart, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS);

		final double fBOverlapOfA = NumberTools.clamp(
				vc.getCount(VennIntersection.AB) / (double) (vc.getCount(VennIntersection.A)
						+ vc.getCount(VennIntersection.B) + vc.getCount(VennIntersection.AB)),
				0.1, 0.9);

		// Scale x overlap position by the fraction of overlapping cells
		final double bxCentre = a.xCentre() + (a.xDiameter() * (1 - fBOverlapOfA));
		final VennShape b = new VennShape(vc.getDataset(VennDatasetPosition.B),
				bxCentre, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS);

		circles.add(a);
		circles.add(b);

		// Place count labels in the centre of the space between circles
		final Label cA = new Label((b.xMin() + a.xMin()) / 2,
				a.yCentre(),
				vc.getCount(VennIntersection.A));

		final Label cB = new Label((b.xMax() + a.xMax()) / 2,
				b.yCentre(),
				vc.getCount(VennIntersection.B));

		final Label cAB = new Label((b.xMin() + a.xMax()) / 2, a.yCentre(),
				vc.getCount(VennIntersection.AB));

		labels.add(cA);
		labels.add(cB);
		labels.add(cAB);

		labels.add(new Label(cA.x(), a.yBottom(), vc.getDataset(VennDatasetPosition.A).getName()));
		labels.add(new Label(cB.x(), b.yBottom(), vc.getDataset(VennDatasetPosition.B).getName()));

	}

	/**
	 * Three circles,turducken style
	 * 
	 * @param vc
	 * @param xStart
	 */
	private void layoutType00111(VennCounter vc, double xStart) {

		final VennShape a = new VennShape(vc.getDataset(VennDatasetPosition.A),
				xStart, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS);

		final double aAlone = vc.getCount(VennIntersection.A);
		final double ab = vc.getCount(VennIntersection.AB);
		final double abc = vc.getCount(VennIntersection.ABC);
		final double aAndB = ab + abc;

		final double fBOverlapOfA = NumberTools.clamp((aAndB / (aAlone + aAndB)), 0.1, 0.9);
		final double fCOverlapOfAB = NumberTools.clamp((abc / aAndB), 0.1, 0.9);

		final VennShape b = new VennShape(vc.getDataset(VennDatasetPosition.B),
				xStart, Y_START, fBOverlapOfA * a.xRadius(), fBOverlapOfA * a.yRadius());

		final VennShape c = new VennShape(vc.getDataset(VennDatasetPosition.C),
				xStart, Y_START, b.yRadius() * fCOverlapOfAB,
				b.yRadius() * fCOverlapOfAB);

		circles.add(a);
		circles.add(b);
		circles.add(c);

		final Label cA = new Label((b.xMin() + a.xMin()) / 2, a.yCentre(),
				vc.getCount(VennIntersection.A));
		final Label cAB = new Label((b.xMax() + c.xMax()) / 2, b.yCentre(),
				vc.getCount(VennIntersection.AB));

		final Label cABC = new Label(c.xCentre(), c.yCentre(), vc.getCount(VennIntersection.ABC));

		labels.add(cA);
		labels.add(cAB);
		labels.add(cABC);

		labels.add(new Label(cA.x(), a.yBottom(), vc.getDataset(VennDatasetPosition.A).getName()));
		labels.add(new Label(cAB.x(), a.yBottom(), vc.getDataset(VennDatasetPosition.B).getName()));
		labels.add(new Label(cABC.x(), a.yTop(), vc.getDataset(VennDatasetPosition.C).getName()));
	}

	/**
	 * Three circles, two entirely within the third, no unique cells in the outer.
	 * No overlap between inner
	 * 
	 * @param vc
	 * @param xStart
	 */
	private void layoutType00020(VennCounter vc, double xStart) {

		// outer
		final VennShape b = new VennShape(vc.getDataset(VennDatasetPosition.B),
				xStart, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS);

		// left
		final VennShape a = new VennShape(vc.getDataset(VennDatasetPosition.A),
				xStart - 0.01, Y_START, DEFAULT_RADIUS * 0.96, DEFAULT_RADIUS * 0.96,
				VennShapeType.HALF_CIRCLE_LEFT);

		// right
		final VennShape c = new VennShape(vc.getDataset(VennDatasetPosition.C),
				xStart + 0.01, Y_START, DEFAULT_RADIUS * 0.96, DEFAULT_RADIUS * 0.96,
				VennShapeType.HALF_CIRCLE_RIGHT);

		circles.add(a);
		circles.add(b);
		circles.add(c);

		final Label cAB = new Label(a.xFraction(0.25), a.yCentre(), vc.getCount(VennIntersection.AB));
		final Label cBC = new Label(c.xFraction(0.75), b.yCentre(), vc.getCount(VennIntersection.BC));

		labels.add(cAB);
		labels.add(cBC);

		labels.add(new Label(cAB.x(), b.yTop(), vc.getDataset(VennDatasetPosition.A).getName()));
		labels.add(new Label(b.xCentre(), b.yBottom(),
				vc.getDataset(VennDatasetPosition.B).getName()));
		labels.add(new Label(cBC.x(), b.yTop(), vc.getDataset(VennDatasetPosition.C).getName()));
	}

	/**
	 * Three circles, two entirely within the third
	 * 
	 * @param vc
	 * @param xStart
	 */
	private void layoutType00021(VennCounter vc, double xStart) {

		final VennShape b = new VennShape(vc.getDataset(VennDatasetPosition.B),
				xStart, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS);

		final VennShape a = new VennShape(vc.getDataset(VennDatasetPosition.A),
				xStart - 0.3, Y_START, SUBSET_RADIUS, SUBSET_RADIUS);

		final VennShape c = new VennShape(vc.getDataset(VennDatasetPosition.C),
				xStart + 0.3, Y_START, SUBSET_RADIUS, SUBSET_RADIUS);

		circles.add(a);
		circles.add(b);
		circles.add(c);

		final Label cB = new Label(b.xCentre(), b.yCentre(), vc.getCount(VennIntersection.B));
		final Label cAB = new Label(a.xCentre(), a.yCentre(), vc.getCount(VennIntersection.AB));
		final Label cBC = new Label(c.xCentre(), b.yCentre(), vc.getCount(VennIntersection.BC));

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
	private void layoutType00023(VennCounter vc, double xStart) {
		final VennShape a = new VennShape(vc.getDataset(VennDatasetPosition.A),
				xStart, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS);

		final VennShape b = new VennShape(vc.getDataset(VennDatasetPosition.B),
				a.xCentre() + 0.8, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS);

		final VennShape c = new VennShape(vc.getDataset(VennDatasetPosition.C),
				b.xCentre() + 0.8, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS);

		circles.add(a);
		circles.add(b);
		circles.add(c);

		final Label cA = new Label((b.xMin() + a.xMin()) / 2,
				a.yCentre(),
				vc.getCount(VennIntersection.A));

		final Label cB = new Label(b.xCentre(),
				b.yCentre(),
				vc.getCount(VennIntersection.B));

		final Label cC = new Label((b.xMax() + c.xMax()) / 2,
				c.yCentre(),
				vc.getCount(VennIntersection.C));

		final Label cAB = new Label((b.xMin() + a.xMax()) / 2,
				a.yCentre(),
				vc.getCount(VennIntersection.AB));

		final Label cBC = new Label((c.xMin() + b.xMax()) / 2,
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
	 * Four circles, three entirely within fourth. Type 00020 within an outer circle
	 * 
	 * @param vc
	 * @param xStart
	 */
	private void layoutType00201(VennCounter vc, double xStart) {

		// outer
		final VennShape d = new VennShape(vc.getDataset(VennDatasetPosition.D),
				xStart, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS);

		final double fInner = NumberTools.clamp(
				(vc.total() - vc.getCount(VennIntersection.D)) / vc.total(),
				0.1, 0.9);

		// inner outer
		final VennShape a = new VennShape(vc.getDataset(VennDatasetPosition.A),
				xStart, Y_START, d.xRadius() * fInner, d.yRadius() * fInner);

		// left
		final VennShape b = new VennShape(vc.getDataset(VennDatasetPosition.B),
				xStart - 0.01, Y_START, a.xRadius() * 0.96, a.xRadius() * 0.96,
				VennShapeType.HALF_CIRCLE_LEFT);

		// right
		final VennShape c = new VennShape(vc.getDataset(VennDatasetPosition.C),
				xStart + 0.01, Y_START, a.xRadius() * 0.96, a.xRadius() * 0.96,
				VennShapeType.HALF_CIRCLE_RIGHT);

		circles.add(a);
		circles.add(b);
		circles.add(c);
		circles.add(d);

		final Label cAB = new Label(a.xFraction(0.25), a.yCentre(), vc.getCount(VennIntersection.ABD));
		final Label cBC = new Label(c.xFraction(0.75), b.yCentre(), vc.getCount(VennIntersection.ACD));
		final Label cD = new Label(d.xCentre(), (d.yMax() + a.yMax()) / 2,
				vc.getCount(VennIntersection.D));

		labels.add(cAB);
		labels.add(cBC);
		labels.add(cD);

		labels.add(new Label(d.xFraction(0.1), d.yFraction(0.95),
				vc.getDataset(VennDatasetPosition.B).getName()));
		labels.add(new Label(b.xCentre(), d.yBottom(),
				vc.getDataset(VennDatasetPosition.A).getName()));
		labels.add(new Label(d.xFraction(0.9), d.yFraction(0.95),
				vc.getDataset(VennDatasetPosition.C).getName()));
		labels.add(
				new Label(d.xCentre(), d.yTop(),
						vc.getDataset(VennDatasetPosition.D).getName()));
	}

	/**
	 * Three circles, triangle
	 * 
	 * @param vc
	 * @param xStart
	 */
	private void layoutType00133(VennCounter vc, double xStart) {

		final VennShape a = new VennShape(vc.getDataset(VennDatasetPosition.A),
				xStart, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS);

		final VennShape b = new VennShape(vc.getDataset(VennDatasetPosition.B),
				a.xCentre() + 0.8, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS);

		final VennShape c = new VennShape(vc.getDataset(VennDatasetPosition.C),
				(a.xCentre() + b.xCentre()) / 2, Y_START + 0.6, DEFAULT_RADIUS, DEFAULT_RADIUS);

		circles.add(a);
		circles.add(b);
		circles.add(c);

		// Need to redraw all label positions
		labels.clear();

		final Label cA = new Label((b.xMin() + a.xMin()) / 2,
				(a.yMin() + c.yMin()) / 2,
				vc.getCount(VennIntersection.A));

		final Label cB = new Label((b.xMax() + a.xMax()) / 2,
				(b.yMin() + c.yMin()) / 2,
				vc.getCount(VennIntersection.B));

		final Label cC = new Label(c.xCentre(),
				(a.yMax() + c.yMax()) / 2,
				vc.getCount(VennIntersection.C));

		final Label cAB = new Label((b.xMin() + a.xMax()) / 2, (a.yMin() + c.yMin()) / 2,
				vc.getCount(VennIntersection.AB));

		final Label cAC = new Label((c.xMin() + b.xMin()) / 2,
				(c.yMin() + a.yMax()) / 2,
				vc.getCount(VennIntersection.AC));

		final Label cBC = new Label((c.xMax() + a.xMax()) / 2,
				(c.yMin() + b.yMax()) / 2,
				vc.getCount(VennIntersection.BC));

		final Label cABC = new Label((a.xMax() + b.xMin()) / 2,
				(c.yMin() + a.yMax()) / 2,
				vc.getCount(VennIntersection.ABC));

		labels.add(cA);
		labels.add(cB);
		labels.add(cC);
		labels.add(cAB);
		labels.add(cAC);
		labels.add(cBC);
		labels.add(cABC);

		labels.add(new Label(cA.x(), a.yBottom(), vc.getDataset(VennDatasetPosition.A).getName()));
		labels.add(new Label(cB.x(), b.yBottom(), vc.getDataset(VennDatasetPosition.B).getName()));
		labels.add(
				new Label(c.xCentre(), c.yTop(), vc.getDataset(VennDatasetPosition.C).getName()));

	}

	/**
	 * Four circles, all intersecting
	 * 
	 * @param vc
	 * @param xStart
	 */
	private void layoutType01464(VennCounter vc, double xStart) {

		final VennShape a = new VennShape(vc.getDataset(VennDatasetPosition.A),
				xStart, Y_START, HALF_RADIUS, DEFAULT_RADIUS);

		final VennShape b = new VennShape(vc.getDataset(VennDatasetPosition.B),
				xStart + 0.5, Y_START, HALF_RADIUS, DEFAULT_RADIUS);

		final VennShape c = new VennShape(vc.getDataset(VennDatasetPosition.C),
				xStart + 0.5, Y_START - 0.5, DEFAULT_RADIUS, HALF_RADIUS);

		final VennShape d = new VennShape(vc.getDataset(VennDatasetPosition.D),
				xStart + 0.5, Y_START, DEFAULT_RADIUS, HALF_RADIUS);

		circles.add(a);
		circles.add(b);
		circles.add(c);
		circles.add(d);

		final Label cA = new Label(a.xCentre(), a.yFraction(0.9), vc.getCount(VennIntersection.A));
		final Label cB = new Label(b.xCentre(), b.yFraction(0.9), vc.getCount(VennIntersection.B));
		final Label cC = new Label(c.xFraction(0.9), c.yCentre(), vc.getCount(VennIntersection.C));
		final Label cD = new Label(d.xFraction(0.9), d.yCentre(), vc.getCount(VennIntersection.D));

		final Label cAB = new Label((b.xMin() + a.xMax()) / 2, a.yFraction(0.775),
				vc.getCount(VennIntersection.AB));
		final Label cAC = new Label(a.xCentre(), c.yCentre(), vc.getCount(VennIntersection.AC));
		final Label cAD = new Label(a.xCentre(), d.yCentre(), vc.getCount(VennIntersection.AD));
		final Label cBC = new Label(b.xCentre(), c.yCentre(), vc.getCount(VennIntersection.BC));
		final Label cBD = new Label(b.xCentre(), d.yCentre(), vc.getCount(VennIntersection.BD));
		final Label cCD = new Label(c.xFraction(0.775), (d.yMin() + c.yMax()) / 2,
				vc.getCount(VennIntersection.CD));

		final Label cABC = new Label((b.xMin() + a.xMax()) / 2, a.yFraction(0.225),
				vc.getCount(VennIntersection.ABC));
		final Label cABD = new Label((b.xMin() + a.xMax()) / 2, d.yCentre(),
				vc.getCount(VennIntersection.ABD));
		final Label cACD = new Label(a.xFraction(0.65), (d.yMin() + c.yMax()) / 2,
				vc.getCount(VennIntersection.ACD));
		final Label cBCD = new Label(b.xCentre(), (d.yMin() + c.yMax()) / 2,
				vc.getCount(VennIntersection.BCD));

		final Label cABCD = new Label((b.xMin() + a.xMax()) / 2, (d.yMin() + c.yMax()) / 2,
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
	 * Four circles, three entirely within the fourth, no unique cells in the outer
	 * 
	 * @param vc
	 * @param xStart
	 */
	private void layoutType00030(VennCounter vc, double xStart) {

		final VennShape b = new VennShape(vc.getDataset(VennDatasetPosition.B),
				xStart, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS);

		final VennShape a = new VennShape(vc.getDataset(VennDatasetPosition.A),
				xStart, Y_START + 0.01, DEFAULT_RADIUS * 0.96, DEFAULT_RADIUS * 0.96,
				VennShapeType.THIRD_CIRCLE_UPPER);

		final VennShape c = new VennShape(vc.getDataset(VennDatasetPosition.C),
				xStart - 0.005, Y_START, DEFAULT_RADIUS * 0.96, DEFAULT_RADIUS * 0.96,
				VennShapeType.THIRD_CIRCLE_LEFT);

		final VennShape d = new VennShape(vc.getDataset(VennDatasetPosition.D),
				xStart + 0.005, Y_START, DEFAULT_RADIUS * 0.96, DEFAULT_RADIUS * 0.96,
				VennShapeType.THIRD_CIRCLE_RIGHT);

		circles.add(a);
		circles.add(b);
		circles.add(c);
		circles.add(d);

		final Label cAB = new Label(a.xCentre(), a.yFraction(0.75), vc.getCount(VennIntersection.AB));
		final Label cBC = new Label(c.xFraction(0.25), c.yFraction(0.33),
				vc.getCount(VennIntersection.BC));
		final Label cBD = new Label(d.xFraction(0.75), d.yFraction(0.33),
				vc.getCount(VennIntersection.BD));

		labels.add(cAB);
		labels.add(cBC);
		labels.add(cBD);

		labels.add(new Label(b.xCentre(), b.yBottom(),
				vc.getDataset(VennDatasetPosition.B).getName()));

		labels.add(new Label(cAB.x(), b.yTop(), vc.getDataset(VennDatasetPosition.A).getName()));
		labels.add(new Label(cBC.x() - 0.1, c.yBottom(),
				vc.getDataset(VennDatasetPosition.C).getName()));
		labels.add(new Label(cBD.x() + 0.1, d.yBottom(),
				vc.getDataset(VennDatasetPosition.D).getName()));
	}

	/**
	 * Four circles, one central intersecting the others once
	 * 
	 * @param vc
	 * @param xStart
	 */
	private void layoutType00033(VennCounter vc, double xStart) {

		final VennShape b = new VennShape(vc.getDataset(VennDatasetPosition.B),
				xStart, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS);

		final VennShape a = new VennShape(vc.getDataset(VennDatasetPosition.A),
				xStart - 1, Y_START - 0.4, DEFAULT_RADIUS, DEFAULT_RADIUS);

		final VennShape c = new VennShape(vc.getDataset(VennDatasetPosition.C),
				xStart + 1, Y_START - 0.4, DEFAULT_RADIUS, DEFAULT_RADIUS);

		final VennShape d = new VennShape(vc.getDataset(VennDatasetPosition.D),
				xStart, Y_START + 1, DEFAULT_RADIUS, DEFAULT_RADIUS);

		circles.add(a);
		circles.add(b);
		circles.add(c);
		circles.add(d);

		final Label cA = new Label(a.xCentre(), a.yCentre(), vc.getCount(VennIntersection.A));
		final Label cB = new Label(b.xCentre(), b.yCentre(), vc.getCount(VennIntersection.B));
		final Label cC = new Label(c.xCentre(), c.yCentre(), vc.getCount(VennIntersection.C));
		final Label cD = new Label(d.xCentre(), d.yCentre(), vc.getCount(VennIntersection.D));

		final Label cAB = new Label((a.xMax() + b.xMin()) / 2, (a.yMax() + b.yMin()) / 2,
				vc.getCount(VennIntersection.AB));
		final Label cBC = new Label((b.xMax() + c.xMin()) / 2, (c.yMax() + b.yMin()) / 2,
				vc.getCount(VennIntersection.BC));
		final Label cBD = new Label(b.xCentre(), (b.yMin() + d.yMax()) / 2,
				vc.getCount(VennIntersection.BD));

		labels.add(cA);
		labels.add(cB);
		labels.add(cC);
		labels.add(cD);

		labels.add(cAB);
		labels.add(cBC);
		labels.add(cBD);
	}

	/**
	 * Three circles, flat
	 * 
	 * @param vc
	 * @param xStart
	 */
	private void layoutType00231(VennCounter vc, double xStart) {
		final VennShape a = new VennShape(vc.getDataset(VennDatasetPosition.A),
				xStart, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS);

		final VennShape b = new VennShape(vc.getDataset(VennDatasetPosition.B),
				a.xCentre(), Y_START, SUBSET_RADIUS, SUBSET_RADIUS);

		final VennShape c = new VennShape(vc.getDataset(VennDatasetPosition.C),
				b.xCentre() - 0.25, Y_START, SUBSET_RADIUS, SUBSET_RADIUS);

		final VennShape d = new VennShape(vc.getDataset(VennDatasetPosition.D),
				b.xCentre() + 0.25, Y_START, SUBSET_RADIUS, SUBSET_RADIUS);

		circles.add(a);
		circles.add(b);
		circles.add(c);
		circles.add(d);

		final Label cA = new Label(a.xCentre(),
				a.yFraction(0.8),
				vc.getCount(VennIntersection.A));

		final Label cAB = new Label(b.xCentre(),
				b.yCentre(),
				vc.getCount(VennIntersection.AB));

		final Label cAC = new Label((c.xMin() + b.xMin()) / 2,
				c.yCentre(),
				vc.getCount(VennIntersection.AC));

		final Label cAD = new Label((d.xMax() + b.xMax()) / 2,
				d.yCentre(),
				vc.getCount(VennIntersection.AD));

		final Label cABC = new Label((c.xMax() + b.xMin()) / 2,
				b.yCentre(),
				vc.getCount(VennIntersection.ABC));

		final Label cABD = new Label((b.xMax() + d.xMin()) / 2,
				b.yCentre(),
				vc.getCount(VennIntersection.ABD));

		labels.add(cA);
		labels.add(cAB);
		labels.add(cAC);
		labels.add(cAD);
		labels.add(cABC);
		labels.add(cABD);

		labels.add(new Label(cA.x(), a.yBottom(), vc.getDataset(VennDatasetPosition.A).getName()));
		labels.add(new Label(cAB.x(), a.yTop(), vc.getDataset(VennDatasetPosition.B).getName()));
		labels.add(new Label(c.xMin(), a.yTop(), vc.getDataset(VennDatasetPosition.C).getName()));
		labels.add(new Label(d.xMax(), a.yTop(), vc.getDataset(VennDatasetPosition.D).getName()));
	}

	/**
	 * A and B do not overlap, C and D do not overlap
	 * 
	 * @param vc
	 * @param xStart
	 */
	private void layoutType00042(VennCounter vc, double xStart) {

		final VennShape a = new VennShape(vc.getDataset(VennDatasetPosition.A),
				xStart - 0.25, Y_START, DEFAULT_RADIUS / 3, DEFAULT_RADIUS / 3);

		final VennShape b = new VennShape(vc.getDataset(VennDatasetPosition.B),
				xStart + 0.25, Y_START, DEFAULT_RADIUS / 3, DEFAULT_RADIUS / 3);

		final VennShape c = new VennShape(vc.getDataset(VennDatasetPosition.C),
				xStart, Y_START + 0.25, DEFAULT_RADIUS / 3, DEFAULT_RADIUS / 3);

		final VennShape d = new VennShape(vc.getDataset(VennDatasetPosition.D),
				xStart, Y_START - 0.25, DEFAULT_RADIUS / 3, DEFAULT_RADIUS / 3);

		circles.add(a);
		circles.add(b);
		circles.add(c);
		circles.add(d);

		final Label cA = new Label(a.xCentre(),
				a.yCentre(),
				vc.getCount(VennIntersection.A));

		final Label cB = new Label(b.xCentre(),
				b.yCentre(),
				vc.getCount(VennIntersection.B));

		final Label cC = new Label(c.xCentre(),
				c.yCentre(),
				vc.getCount(VennIntersection.C));

		final Label cD = new Label(d.xCentre(),
				d.yCentre(),
				vc.getCount(VennIntersection.D));

		final Label cAD = new Label((a.xMax() + d.xMin()) / 2,
				(a.yMax() + d.yMin()) / 2,
				vc.getCount(VennIntersection.AD));

		final Label cAC = new Label((c.xMin() + a.xMax()) / 2,
				(c.yMax() + a.yMin()) / 2,
				vc.getCount(VennIntersection.AC));

		final Label cBD = new Label((b.xMax() + d.xMin()) / 2,
				(b.yMin() + d.yMax()) / 2,
				vc.getCount(VennIntersection.BD));

		final Label cBC = new Label((c.xMin() + b.xMax()) / 2,
				(c.yMin() + b.yMax()) / 2,
				vc.getCount(VennIntersection.BC));

		labels.add(cA);
		labels.add(cB);
		labels.add(cC);
		labels.add(cD);

		labels.add(cAD);
		labels.add(cAC);
		labels.add(cBD);
		labels.add(cBC);
	}

	/**
	 * Four contained within fifth, partial overlaps within
	 * 
	 * @param vc
	 * @param xStart
	 */
	private void layoutType00060(VennCounter vc, double xStart) {
		// A is large upper ellipse
		final VennShape a = new VennShape(vc.getDataset(VennDatasetPosition.A),
				xStart, Y_START + 0.3, DEFAULT_RADIUS, DEFAULT_RADIUS / 3);

		// A is large lower ellipse
		final VennShape b = new VennShape(vc.getDataset(VennDatasetPosition.B),
				xStart, Y_START - 0.3, DEFAULT_RADIUS, DEFAULT_RADIUS / 3);

		// C overlaps A & B, no other
		final VennShape c = new VennShape(vc.getDataset(VennDatasetPosition.C),
				xStart - 0.5, Y_START, DEFAULT_RADIUS / 3, DEFAULT_RADIUS / 3);

		// C overlaps A & B, no other
		final VennShape d = new VennShape(vc.getDataset(VennDatasetPosition.D),
				xStart, Y_START, DEFAULT_RADIUS / 3, DEFAULT_RADIUS / 3);

		// C overlaps A & B, no other
		final VennShape e = new VennShape(vc.getDataset(VennDatasetPosition.E),
				xStart + 0.5, Y_START, DEFAULT_RADIUS / 3, DEFAULT_RADIUS / 3);

		circles.add(a);
		circles.add(b);
		circles.add(c);
		circles.add(d);
		circles.add(e);

		final Label cA = new Label(a.xCentre(),
				a.yFraction(0.8),
				vc.getCount(VennIntersection.A));

		final Label cB = new Label(b.xCentre(),
				b.yFraction(0.2),
				vc.getCount(VennIntersection.B));

		final Label cC = new Label(c.xCentre(),
				c.yCentre(),
				vc.getCount(VennIntersection.C));

		final Label cD = new Label(d.xCentre(),
				d.yCentre(),
				vc.getCount(VennIntersection.D));
		final Label cE = new Label(e.xCentre(),
				e.yCentre(),
				vc.getCount(VennIntersection.E));

		labels.add(cA);
		labels.add(cB);
		labels.add(cC);
		labels.add(cD);
		labels.add(cE);

		final Label cAC = new Label((c.xMax() + a.xMin()) / 2,
				c.yFraction(0.9),
				vc.getCount(VennIntersection.AC));

		final Label cBC = new Label((c.xMax() + b.xMin()) / 2,
				c.yFraction(0.1),
				vc.getCount(VennIntersection.BC));

		final Label cAD = new Label(d.xCentre(),
				d.yFraction(0.8),
				vc.getCount(VennIntersection.AD));

		final Label cBD = new Label(d.xCentre(),
				d.yFraction(0.2),
				vc.getCount(VennIntersection.BD));

		final Label cAE = new Label((a.xMax() + e.xMin()) / 2,
				e.yFraction(0.9),
				vc.getCount(VennIntersection.AE));

		final Label cBE = new Label((b.xMax() + e.xMin()) / 2,
				e.yFraction(0.1),
				vc.getCount(VennIntersection.BE));

		labels.add(cAC);
		labels.add(cBC);
		labels.add(cAD);
		labels.add(cBD);

		labels.add(cAE);
		labels.add(cBE);

	}

	/**
	 * Four contained within fifth, partial overlaps within
	 * 
	 * @param vc
	 * @param xStart
	 */
	private void layoutType00420(VennCounter vc, double xStart) {
		// A is overall container
		final VennShape a = new VennShape(vc.getDataset(VennDatasetPosition.A),
				xStart, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS);

		// B overlaps D & E, not C
		final VennShape b = new VennShape(vc.getDataset(VennDatasetPosition.B),
				xStart - 0.25, Y_START, DEFAULT_RADIUS / 3, DEFAULT_RADIUS / 3);

		// C overlaps D & E, not B
		final VennShape c = new VennShape(vc.getDataset(VennDatasetPosition.C),
				xStart + 0.25, Y_START, DEFAULT_RADIUS / 3, DEFAULT_RADIUS / 3);
//
//		// D overlaps B & C, not E
		final VennShape d = new VennShape(vc.getDataset(VennDatasetPosition.D),
				xStart, Y_START + 0.25, DEFAULT_RADIUS / 3, DEFAULT_RADIUS / 3);
//
//		// E overlaps B & C, not D
		final VennShape e = new VennShape(vc.getDataset(VennDatasetPosition.E),
				xStart, Y_START - 0.25, DEFAULT_RADIUS / 3, DEFAULT_RADIUS / 3);

		circles.add(a);
		circles.add(b);
		circles.add(c);
		circles.add(d);
		circles.add(e);

		final Label cA = new Label(a.xCentre(),
				a.yFraction(0.9),
				vc.getCount(VennIntersection.A));

		final Label cB = new Label(b.xCentre(),
				b.yCentre(),
				vc.getCount(VennIntersection.AB));

		final Label cC = new Label(c.xCentre(),
				c.yCentre(),
				vc.getCount(VennIntersection.AC));

		final Label cD = new Label(d.xCentre(),
				d.yCentre(),
				vc.getCount(VennIntersection.AD));

		final Label cE = new Label(e.xCentre(),
				e.yCentre(),
				vc.getCount(VennIntersection.AE));

		final Label cABD = new Label((b.xMax() + d.xMin()) / 2,
				(b.yMax() + d.yMin()) / 2,
				vc.getCount(VennIntersection.ABD));

		final Label cACD = new Label((c.xMin() + d.xMax()) / 2,
				(c.yMax() + d.yMin()) / 2,
				vc.getCount(VennIntersection.ACD));

		final Label cABE = new Label((b.xMax() + e.xMin()) / 2,
				(b.yMin() + e.yMax()) / 2,
				vc.getCount(VennIntersection.ABE));

		final Label cACE = new Label((c.xMin() + e.xMax()) / 2,
				(c.yMin() + e.yMax()) / 2,
				vc.getCount(VennIntersection.ACE));

		labels.add(cA);
		labels.add(cB);
		labels.add(cC);
		labels.add(cD);
		labels.add(cE);

		labels.add(cABD);
		labels.add(cACD);
		labels.add(cABE);
		labels.add(cACE);

	}

	private void layoutTypeFiveFull(VennCounter vc, double xStart) {

		final VennShape a = new VennShape(vc.getDataset(VennDatasetPosition.A),
				xStart - 0.1, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS * 0.66, VennShapeType.CIRCLE,
				-Math.PI / 2);

		final VennShape b = new VennShape(vc.getDataset(VennDatasetPosition.B),
				xStart + 0.1, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS * 0.66, VennShapeType.CIRCLE,
				Math.PI / 7);
		final VennShape c = new VennShape(vc.getDataset(VennDatasetPosition.C),
				xStart + 0.1, Y_START - 0.05, DEFAULT_RADIUS, DEFAULT_RADIUS * 0.66,
				VennShapeType.CIRCLE, -Math.PI / 3);

		final VennShape d = new VennShape(vc.getDataset(VennDatasetPosition.D),
				xStart - 0.1, Y_START + 0.1, DEFAULT_RADIUS, DEFAULT_RADIUS * 0.66,
				VennShapeType.CIRCLE, Math.PI / 3);

		final VennShape e = new VennShape(vc.getDataset(VennDatasetPosition.E),
				xStart - 0.22, Y_START + 0.12, DEFAULT_RADIUS, DEFAULT_RADIUS * 0.66,
				VennShapeType.CIRCLE, -Math.PI / 8);

		circles.add(a);
		circles.add(b);
		circles.add(c);
		circles.add(d);
		circles.add(e);

		final Label cA = new Label((a.xMax() + a.xMin()) / 2,
				a.yMax() * 0.85,
				vc.getCount(VennIntersection.A));

		final Label cB = new Label(b.xFraction(0.9),
				b.yFraction(0.75),
				vc.getCount(VennIntersection.B));

		final Label cC = new Label(c.xFraction(0.85),
				c.yFraction(0.15),
				vc.getCount(VennIntersection.C));

		final Label cD = new Label(d.xFraction(0.2),
				d.yFraction(0.1),
				vc.getCount(VennIntersection.D));

		final Label cE = new Label(e.xFraction(0.15),
				e.yFraction(0.75),
				vc.getCount(VennIntersection.E));

		final Label cAB = new Label(a.xFraction(0.9),
				b.yFraction(0.9),
				vc.getCount(VennIntersection.AB));

		final Label cAC = new Label(a.xFraction(0.7),
				a.yFraction(0.075),
				vc.getCount(VennIntersection.AC));

		final Label cAD = new Label(a.xFraction(0.67),
				d.yFraction(0.97),
				vc.getCount(VennIntersection.AD));

		final Label cAE = new Label(a.xFraction(0.3),
				a.yFraction(0.87),
				vc.getCount(VennIntersection.AE));

		final Label cBC = new Label(c.xFraction(0.95),
				c.yFraction(0.42),
				vc.getCount(VennIntersection.BC));

		final Label cBD = new Label(b.xFraction(0.075),
				b.yFraction(0.2),
				vc.getCount(VennIntersection.BD));

		final Label cBE = new Label(e.xFraction(0.98),
				e.yFraction(0.33),
				vc.getCount(VennIntersection.BE));

		final Label cCD = new Label(d.xFraction(0.45),
				d.yFraction(0.1),
				vc.getCount(VennIntersection.CD));

		final Label cCE = new Label(e.xFraction(0.29),
				e.yFraction(0.68),
				vc.getCount(VennIntersection.CE));

		final Label cDE = new Label(e.xFraction(0.19),
				e.yFraction(0.33),
				vc.getCount(VennIntersection.DE));

		final Label cABC = new Label(c.xFraction(0.8),
				c.yFraction(0.35),
				vc.getCount(VennIntersection.ABC));

		final Label cABD = new Label(a.xFraction(0.77),
				d.yFraction(0.925),
				vc.getCount(VennIntersection.ABD));

		final Label cABE = new Label(a.xFraction(0.97),
				a.yFraction(0.53),
				vc.getCount(VennIntersection.ABE));

		final Label cACD = new Label(a.xFraction(0.40),
				d.yFraction(0.12),
				vc.getCount(VennIntersection.ACD));

		final Label cACE = new Label(a.xFraction(0.10),
				a.yFraction(0.75),
				vc.getCount(VennIntersection.ACE));

		final Label cADE = new Label(a.xFraction(0.50),
				d.yFraction(0.96),
				vc.getCount(VennIntersection.ADE));

		final Label cBCD = new Label(b.xFraction(0.12),
				b.yFraction(0.2),
				vc.getCount(VennIntersection.BCD));

		final Label cBCE = new Label(e.xFraction(0.97),
				e.yFraction(0.24),
				vc.getCount(VennIntersection.BCE));

		final Label cBDE = new Label(d.xFraction(0.12),
				d.yFraction(0.47),
				vc.getCount(VennIntersection.BDE));

		final Label cCDE = new Label(e.xFraction(0.26),
				e.yFraction(0.50),
				vc.getCount(VennIntersection.CDE));

		final Label cABCD = new Label(b.xFraction(0.4),
				b.yFraction(0.1),
				vc.getCount(VennIntersection.ABCD));

		final Label cABCE = new Label(e.xFraction(0.92),
				e.yFraction(0.24),
				vc.getCount(VennIntersection.ABCE));

		final Label cABDE = new Label(a.xFraction(0.77),
				d.yFraction(0.85),
				vc.getCount(VennIntersection.ABDE));

		final Label cACDE = new Label(e.xFraction(0.43),
				e.yFraction(0.72),
				vc.getCount(VennIntersection.ACDE));

		final Label cBCDE = new Label(e.xFraction(0.25),
				e.yFraction(0.33),
				vc.getCount(VennIntersection.BCDE));

		final Label cABCDE = new Label(a.xFraction(0.5),
				a.yFraction(0.45),
				vc.getCount(VennIntersection.ABCDE));

		labels.add(cA);
		labels.add(cB);
		labels.add(cC);
		labels.add(cD);
		labels.add(cE);
		labels.add(cAB);
		labels.add(cAC);
		labels.add(cAD);
		labels.add(cAE);
		labels.add(cBC);
		labels.add(cBD);
		labels.add(cBE);
		labels.add(cCD);
		labels.add(cCE);
		labels.add(cDE);
		labels.add(cABC);
		labels.add(cABD);
		labels.add(cABE);
		labels.add(cACD);
		labels.add(cACE);
		labels.add(cADE);
		labels.add(cBCD);
		labels.add(cBCE);
		labels.add(cBDE);
		labels.add(cCDE);
		labels.add(cABCD);
		labels.add(cABCE);
		labels.add(cABDE);
		labels.add(cACDE);
		labels.add(cBCDE);
		labels.add(cABCDE);

		labels.add(new Label(a.xCentre() * 1.1, a.yMax() * 1.1,
				vc.getDataset(VennDatasetPosition.A).getName()));

		labels.add(new Label(b.xMax(), b.yMax(),
				vc.getDataset(VennDatasetPosition.B).getName()));

		labels.add(new Label(c.xMax(), c.yMin(),
				vc.getDataset(VennDatasetPosition.C).getName()));

		labels.add(new Label(d.xMin(), d.yMin(),
				vc.getDataset(VennDatasetPosition.D).getName()));

		labels.add(new Label(e.xMin(), e.yMax(),
				vc.getDataset(VennDatasetPosition.E).getName()));
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
		for (final List<IAnalysisDataset> cluster : clusters.values()) {
			final boolean addToCluster = cluster.stream()
					.anyMatch(d -> d.getCollection().countShared(dataset) > 0);

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

		for (final Entry<Comparable<?>, List<IAnalysisDataset>> entry : clusters.entrySet()) {
			final List<IAnalysisDataset> datasets = entry.getValue();

			final VennCounter vc = new VennCounter(datasets);

			createLayout(vc, xStart);
			xStart += X_OFFSET;
		}

		// Create sentinal points to allow aspect ratio scaling of chart without
		// clipping annotated Venn circle outlines. These are points outside the range
		// of the circles.

		final double xMax = circles.stream().mapToDouble(VennShape::xMax).max().orElse(1) * 1.1;
		final double xMin = circles.stream().mapToDouble(VennShape::xMin).min().orElse(1) * 1.1; // will
																							// be
																							// negative
		final double yMax = circles.stream().mapToDouble(VennShape::yMax).max().orElse(1) * 1.1;
		final double yMin = circles.stream().mapToDouble(VennShape::yMin).min().orElse(1) * 1.1; // will
																							// be
																							// negative
		addSeries("Sentinals", new double[][] { { xMin, xMax }, { yMin, yMax } });
	}

	/**
	 * Identify clusters with shared nuclei and collapse them
	 * 
	 */
	private void collapseClusters() {

		if (clusters.size() == 1)
			return;

		final Map<Comparable<?>, Boolean> includeInFinal = new HashMap<>();

		// Check if we can collapse any clusters with the latest addition
		final Map<Comparable<?>, List<IAnalysisDataset>> replacementClusters = new HashMap<>();

		for (final Entry<Comparable<?>, List<IAnalysisDataset>> entry1 : clusters.entrySet()) {
			for (final Entry<Comparable<?>, List<IAnalysisDataset>> entry2 : clusters.entrySet()) {
				if (entry1.getKey().equals(entry2.getKey())) {
					continue;
				}

				final boolean matchFound = entry1.getValue().stream().anyMatch(
						d -> entry2.getValue().stream()
								.anyMatch(d2 -> d.getCollection().countShared(d2) > 0));

				if (matchFound) {
					entry1.getValue().addAll(entry2.getValue());
					entry2.getValue().clear();
					includeInFinal.put(entry2.getKey(), false);
				}

				includeInFinal.putIfAbsent(entry1.getKey(), true);

			}
		}

		// Remove entry2 from consideration if absorbed into entry1
		for (final Entry<Comparable<?>, Boolean> entry : includeInFinal.entrySet()) {
			if (entry.getValue()) {
				replacementClusters.put("Cluster_" + replacementClusters.size(),
						clusters.get(entry.getKey()));
			}
		}

		clusters.clear();
		clusters.putAll(replacementClusters);
	}
}
