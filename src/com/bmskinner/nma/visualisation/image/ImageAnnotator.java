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
package com.bmskinner.nma.visualisation.image;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.signals.shells.ShellDetector;
import com.bmskinner.nma.components.Imageable;
import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.Taggable;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.generic.FloatPoint;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.components.signals.INuclearSignal;
import com.bmskinner.nma.components.signals.ISignalCollection;
import com.bmskinner.nma.components.signals.IWarpedSignal;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.gui.components.ColourSelecter;
import com.bmskinner.nma.logging.Loggable;

import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;

/**
 * Draw components and features on image processors.
 * 
 * @author ben
 *
 */
public class ImageAnnotator extends ImageFilterer {

	private static final Logger LOGGER = Logger.getLogger(ImageAnnotator.class.getName());

	private static final int BORDER_TAG_POINT_SIZE = 7;
	private static final int RP_POINT_SIZE = 9;
	private static final int DEFAULT_LINE_WIDTH = 3;
	/** Converts resized images to original image dimensions */
	private double scale = 1;

	private static final Map<OrientationMark, Color> DEFAULT_TAG_COLOURS = new HashMap<>();

	static {
		DEFAULT_TAG_COLOURS.put(OrientationMark.REFERENCE, Color.ORANGE);
		DEFAULT_TAG_COLOURS.put(OrientationMark.Y, Color.BLUE);
		DEFAULT_TAG_COLOURS.put(OrientationMark.TOP, Color.GREEN);
		DEFAULT_TAG_COLOURS.put(OrientationMark.BOTTOM, Color.GREEN);
	}

	public ImageAnnotator(final ImageProcessor ip) {
		super(ip);
	}

	/**
	 * Get the defined colour for a given tag, or a default colour if the tag is not
	 * defined
	 * 
	 * @param t the tag to get a colour for
	 * @return the defined colour, or a default colour
	 */
	private Color getDefaultColour(OrientationMark t) {
		if (DEFAULT_TAG_COLOURS.containsKey(t))
			return DEFAULT_TAG_COLOURS.get(t);
		return Color.PINK;
	}

	/**
	 * Get the conversion scale between the annotated image width and the input
	 * image width
	 * 
	 * @return the scale factor
	 */
	protected double getScale() {
		return scale;
	}

	/**
	 * Create using the given image, and rescale all annotations such that the image
	 * fits the given dimensions preserving aspect ratio
	 * 
	 * @param ip        the input image processor
	 * @param maxWidth  the maximum width the output image can be
	 * @param maxHeight the maximum height the output image can be
	 */
	public ImageAnnotator(final ImageProcessor ip, final int maxWidth, final int maxHeight) {
		super(ip);
		int originalWidth = ip.getWidth();
		int originalHeight = ip.getHeight();

		// keep the image aspect ratio
		double ratio = (double) originalWidth / (double) originalHeight;

		double finalWidth = maxHeight * ratio; // fix height
		finalWidth = finalWidth > maxWidth ? maxWidth : finalWidth; // but
																	// constrain
																	// width too

		scale = finalWidth / originalWidth;
		ImageProcessor result = ip.duplicate().resize((int) finalWidth);
		this.ip = result;
	}

	/**
	 * Draw the borders of all cell components. Assumes that the image contains the
	 * original coordinates of the cell
	 * 
	 * @param cell
	 * @return
	 */
	public final ImageAnnotator annotateCellBorders(@NonNull final ICell cell) {

		if (cell.hasCytoplasm()) {
			drawBorder(cell.getCytoplasm(), Color.CYAN);
		}

		for (Nucleus n : cell.getNuclei()) {
			drawBorder(n, Color.ORANGE);
		}
		return this;
	}

	public ImageAnnotator drawSegments(@NonNull Nucleus n) {
		try {
			// // Colour the border points for segments
			ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);
			if (profile.hasSegments()) {

				for (IProfileSegment seg : profile.getOrderedSegments()) {
					Paint color = ColourSelecter.getColor(seg.getPosition(),
							GlobalOptions.getInstance().getSwatch());
					Iterator<Integer> it = seg.iterator();
					int lastIndex = n.getIndexRelativeTo(OrientationMark.REFERENCE,
							seg.getEndIndex());
					while (it.hasNext()) {
						int index = n.getIndexRelativeTo(OrientationMark.REFERENCE, it.next());
						IPoint p = n.getBorderPoint(index);
						// since segments overlap, draw the last index larger so the next segment
						// can
						// overlay
						annotatePoint(p, (Color) color, lastIndex == index ? 3 : 2);
					}
				}
			}
		} catch (MissingDataException | SegmentUpdateException e) {
			LOGGER.log(Loggable.STACK, "Error annotating nucleus", e);
		}
		return this;
	}

	public ImageAnnotator drawSignals(@NonNull Nucleus n) {
		try {
			// // Colour the border points for segments
			ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);
			if (profile.hasSegments()) {

				for (IProfileSegment seg : profile.getOrderedSegments()) {
					Paint color = ColourSelecter.getColor(seg.getPosition(),
							GlobalOptions.getInstance().getSwatch());
					Iterator<Integer> it = seg.iterator();
					int lastIndex = n.getIndexRelativeTo(OrientationMark.REFERENCE,
							seg.getEndIndex());
					while (it.hasNext()) {
						int index = n.getIndexRelativeTo(OrientationMark.REFERENCE, it.next());
						IPoint p = n.getBorderPoint(index);
						// since segments overlap, draw the last index larger so the next segment
						// can
						// overlay
						annotatePoint(p, (Color) color, lastIndex == index ? 3 : 2);
					}
				}
			}
		} catch (MissingDataException | SegmentUpdateException e) {
			LOGGER.log(Loggable.STACK, "Error annotating nucleus", e);
		}
		return this;
	}

	/**
	 * Draw the outline of the given nucleus, with landmarks, segments and any
	 * signals marked
	 * 
	 * @param n the nucleus to draw
	 * @return this annotator
	 */
	public ImageAnnotator annotateNucleus(@NonNull Nucleus n) {
		annotateCoM(n);
		drawSegments(n);
		annotateSignals(n);
		for (OrientationMark lm : n.getOrientationMarks())
			drawLandmark(lm, n);

		return this;
	}

	/**
	 * Draw the given shell. Shells are assumed to be at the shell location in the
	 * source image ofthe component
	 * 
	 * @param shell
	 * @return
	 */
	public ImageAnnotator annotate(ShellDetector.Shell shell, @NonNull Color colour) {

		// This is in source image coordinates
		Roi roi = shell.toRoi();

		IPoint diff = shell.getSource().getOriginalBase().minus(shell.getOriginalBase());

		// add an adjustment for the buffer, and for the location within the nucleus

		IPoint base = shell.getSource().getBase().minus(diff);

		roi.setLocation(base.getX(), base.getY());
		annotateRoi(roi, colour, 1);
		return this;
	}

	/**
	 * Draw a point on the image processor with the default size
	 * 
	 * @param p the point to draw
	 * @param c the colour to draw
	 * @return the annotator
	 */
	public ImageAnnotator annotatePoint(@NonNull IPoint p, @NonNull Color c) {
		return annotatePoint(p, c, 3);
	}

	/**
	 * Draw a point on the image processor
	 * 
	 * @param p    the point to draw
	 * @param c    the colour to draw
	 * @param size the point size
	 * @return the annotator
	 */
	public ImageAnnotator annotatePoint(@NonNull IPoint p, @NonNull Color c, int size) {

		if (p.getXAsInt() < 0 || p.getXAsInt() > ip.getWidth())
			throw new IllegalArgumentException(
					"Point x " + p.getXAsInt() + " is out of image bounds (max width "
							+ ip.getWidth() + ")");

		if (p.getYAsInt() < 0 || p.getYAsInt() > ip.getHeight())
			throw new IllegalArgumentException(
					"Point y " + p.getYAsInt() + " is out of image bounds (max height "
							+ ip.getHeight() + ")");

		ip.setColor(c);
		ip.setLineWidth(size);
		ip.drawDot((int) (p.getX() * scale), (int) (p.getY() * scale));
		return this;
	}

	/**
	 * Draw the border of the given component on the component image of the
	 * template. These can be the same object.
	 * 
	 * @param p        the points to draw
	 * @param template the component to get the component image from
	 * @param c        the colour to draw the border
	 * @return this annotator
	 */
	public ImageAnnotator annotatePoint(@NonNull IPoint p, @NonNull Imageable template,
			@NonNull Color c) {
		IPoint offset = Imageable.translateCoordinateToComponentImage(p, template);
		return annotatePoint(offset, c);
	}

	/**
	 * Draw a line on the image processor with the default width
	 * 
	 * @param p1 the first endpoint
	 * @param p1 the second endpoint
	 * @param c  the colour to draw
	 * @return the annotator
	 */
	private ImageAnnotator annotateLine(@NonNull IPoint p1, @NonNull IPoint p2, @NonNull Color c) {
		return annotateLine(p1, p2, c, 1);
	}

	/**
	 * Draw a line on the image processor
	 * 
	 * @param p1    the first endpoint
	 * @param p1    the second endpoint
	 * @param c     the colour to draw
	 * @param width the line width
	 * @return the annotator
	 */
	private ImageAnnotator annotateLine(@NonNull IPoint p1, @NonNull IPoint p2, @NonNull Color c,
			int width) {
		ip.setColor(c);
		ip.setLineWidth(width);
		ip.drawLine((int) (p1.getXAsInt() * scale), (int) (p1.getYAsInt() * scale),
				(int) (p2.getXAsInt() * scale),
				(int) (p2.getYAsInt() * scale));
		return this;
	}

	private ImageAnnotator annotatePolygon(@NonNull PolygonRoi p, @NonNull Color c) {
		ip = annotatePolygon(ip, p, c);
		return this;
	}

	private static ImageProcessor annotatePolygon(ImageProcessor ip, @NonNull PolygonRoi p,
			@NonNull Color c) {
		ip.setColor(c);
		ip.setLineWidth(2);
		ip.draw(p);
		return ip;
	}

	private ImageAnnotator annotateRoi(@NonNull Roi p, @NonNull Color c, int width) {
		ip.setColor(c);
		ip.setLineWidth(width);
		ip.draw(p);
		return this;
	}

	/**
	 * Draw a landmark using the appropriate colour
	 * 
	 * @param lm
	 * @param n
	 * @return
	 */
	private ImageAnnotator drawLandmark(OrientationMark lm, @NonNull Nucleus n) {
		try {
			return annotatePoint(n.getBorderPoint(lm), getDefaultColour(lm));
		} catch (MissingLandmarkException e) {
			LOGGER.log(Loggable.STACK, "Cannot find border tag " + lm, e);
		}
		return this;
	}

	/**
	 * Draw the centre of mass of a component in magenta. For other colours use
	 * {@link ImageAnnotator#annotatePoint} directly
	 * 
	 * @param n the component
	 * @return the annotator
	 */
	public ImageAnnotator annotateCoM(@NonNull CellularComponent n) {
		return annotatePoint(n.getCentreOfMass(), Color.MAGENTA);
	}

	/**
	 * Draw the outline of the component in the given colour at the original
	 * position
	 * 
	 * @param n the component
	 * @param c the colour
	 * @return the annotator
	 */
	public ImageAnnotator drawBorder(@NonNull final CellularComponent n, final Color c) {
		FloatPolygon p = n.toOriginalPolygon();
		PolygonRoi roi = new PolygonRoi(p, PolygonRoi.POLYGON);

		return annotatePolygon(roi, c);
	}

	public static ImageProcessor drawBorder(ImageProcessor ip, @NonNull final CellularComponent n,
			final Color c) {
		FloatPolygon p = n.toOriginalPolygon();
		PolygonRoi roi = new PolygonRoi(p, PolygonRoi.POLYGON);
		return annotatePolygon(ip, roi, c);
	}

	/**
	 * Draw the border of the given component on the component image of the
	 * template. These can be the same object.
	 * 
	 * @param n        the component to draw
	 * @param template the component to get the component image from
	 * @param c        the colour to draw the border
	 * @return this annotator
	 */
	public ImageAnnotator annotateBorder(@NonNull final CellularComponent n,
			@NonNull final Imageable template,
			@NonNull final Color c) {
		FloatPolygon p = n.toOriginalPolygon();
		PolygonRoi roi = new PolygonRoi(p, PolygonRoi.POLYGON);

		IPoint base = n.getOriginalBase();
		IPoint offset = Imageable.translateCoordinateToComponentImage(base, template);
		roi.setLocation(offset.getX(), offset.getY());

		return annotatePolygon(roi, c);
	}

	/**
	 * Annotate the image with the given text. The text background is white and the
	 * text colour is black
	 * 
	 * @param x the string x
	 * @param y the string y
	 * @param s the text
	 * @return
	 */
	public ImageAnnotator annotateString(int x, int y, String s) {
		return this.annotateString(x, y, s, Color.BLACK);
	}

	/**
	 * Annotate the image with the given text. The text background is white.
	 * 
	 * @param x    the string x
	 * @param y    the string y
	 * @param s    the text
	 * @param text the text colour
	 * @return
	 */
	public ImageAnnotator annotateString(int x, int y, String s, Color text) {
		return this.annotateString(x, y, s, text, Color.WHITE);
	}

	/**
	 * Annotate the image with the given text.
	 * 
	 * @param x    the string x
	 * @param y    the string y
	 * @param s    the text
	 * @param text the text colour
	 * @param back the background colour
	 * @return
	 */
	public ImageAnnotator annotateString(int x, int y, String s, Color text, Color back) {

		ip.setFont(new Font("SansSerif", Font.PLAIN, 20)); // TODO - choose text
															// size based on
															// image size
		ip.setColor(text);
		ip.drawString(s, x, y, back);
		return this;
	}

	/**
	 * Draw the size and shape values over the CoM of the component.
	 * 
	 * @param n    the component to draw
	 * @param text the colour of the text
	 * @param back the colour of the background
	 * @return the annotator
	 */
	public ImageAnnotator annotateStats(CellularComponent n, Color text, Color back) {

		DecimalFormat df = new DecimalFormat("#.##");

		String areaLbl;
		String perimLbl;

		double circ;
		double area;

		String label;

		try {

			if (n instanceof INuclearSignal) {

				area = n.getMeasurement(Measurement.AREA);
				double perim2 = Math.pow(n.getMeasurement(Measurement.PERIMETER), 2);
				circ = (4 * Math.PI) * (area / perim2);

			} else {
				area = n.getMeasurement(Measurement.AREA);
				circ = n.getMeasurement(Measurement.CIRCULARITY);
			}

			areaLbl = "Area: " + df.format(area);
			perimLbl = "Circ: " + df.format(circ);
			label = areaLbl + "\n" + perimLbl;

		} catch (MissingDataException | ComponentCreationException | SegmentUpdateException e) {
			LOGGER.log(Level.FINE, "Missing measurement in annotator", e);
			label = "";
		}

		return this.annotateString(n.getOriginalCentreOfMass().getXAsInt(),
				n.getOriginalCentreOfMass().getYAsInt(),
				label, text, back);
	}

	/**
	 * Draw the size and shape values over the CoM of the component
	 * 
	 * @param n the component to draw
	 * @param c the color of the text
	 * @return
	 */
	public ImageAnnotator annotateSignalStats(CellularComponent parent, CellularComponent signal,
			Color text,
			Color back) {

		DecimalFormat df = new DecimalFormat("#.##");

		String areaLbl;
		String perimLbl;

		double circ = 0;
		double area = 0;
		double fraction;
		String label;

		try {

			if (signal instanceof INuclearSignal) {

				area = signal.getMeasurement(Measurement.AREA);
				double perim2 = Math.pow(signal.getMeasurement(Measurement.PERIMETER), 2);
				circ = (4 * Math.PI) * (area / perim2);

			}

			fraction = area / parent.getMeasurement(Measurement.AREA);

			areaLbl = "Area: " + df.format(area);
			perimLbl = "Circ: " + df.format(circ);
			String fractLabel = "Fract: " + df.format(fraction);

			label = areaLbl + "\n" + perimLbl + "\n" + fractLabel;

		} catch (MissingDataException | ComponentCreationException | SegmentUpdateException e) {
			LOGGER.log(Level.FINE, "Missing measurement in annotator: %s".formatted(e.getMessage()),
					e);
			label = "";
		}

		return this.annotateString(signal.getOriginalCentreOfMass().getXAsInt(),
				signal.getOriginalCentreOfMass().getYAsInt(), label, text, back);

	}

	/**
	 * Draw the segments of a profilable component in the global colour swatch
	 * colours. Draw the segments at their positions in the template component image
	 * 
	 * @param n the component
	 * @return the annotator
	 */
	public ImageAnnotator annotateSegments(Taggable n, Imageable template) {

		try {

			// only draw if segments are present
			if (n.getProfile(ProfileType.ANGLE).hasSegments()) {
				List<IProfileSegment> segs = n.getProfile(ProfileType.ANGLE).getSegments();

				for (int i = 0; i < segs.size(); i++) {

					IProfileSegment seg = segs.get(i);

					float[] xpoints = new float[seg.length() + 1];
					float[] ypoints = new float[seg.length() + 1];
					for (int j = 0; j <= seg.length(); j++) {
						int k = n.wrapIndex(seg.getStartIndex() + j);

						IPoint p = n.getOriginalBorderPoint(k);
						xpoints[j] = (float) (p.getX() * scale);
						ypoints[j] = (float) (p.getY() * scale);
					}

					PolygonRoi segRoi = new PolygonRoi(xpoints, ypoints, Roi.POLYLINE);

					// Offset the segment relative to the nucleus component
					// image
					IPoint base = new FloatPoint(segRoi.getXBase(), segRoi.getYBase());
					IPoint offset = Imageable.translateCoordinateToComponentImage(base, template);

					segRoi.setLocation(offset.getX(), offset.getY());

					annotatePolygon(segRoi, ColourSelecter.getColor(i));
				}
			}
		} catch (Exception e) {
			LOGGER.log(Loggable.STACK, "Error annotating segments", e);
		}
		return this;
	}

	/**
	 * Draw the signals within a nucleus on the current image. This assumes that the
	 * current image is a nucleus component image.
	 * 
	 * @param n the nucleus
	 * @return the annotator
	 */
	public ImageAnnotator annotateSignals(@NonNull Nucleus n) {

		ISignalCollection signalCollection = n.getSignalCollection();

		for (UUID id : signalCollection.getSignalGroupIds()) {

			if (signalCollection.hasSignal(id)) {

				Color colour = ColourSelecter
						.getSignalColour(signalCollection.getSourceChannel(id));

				List<INuclearSignal> signals = signalCollection.getSignals(id);

				for (INuclearSignal s : signals) {

					annotatePoint(s.getCentreOfMass().plus(Imageable.COMPONENT_BUFFER), colour);
					IPoint base = s.getBase().plus(Imageable.COMPONENT_BUFFER);

					FloatPolygon p = s.toPolygon();

					float[] x = p.xpoints;
					float[] y = p.ypoints;

					if (Math.abs(scale - 1) > 0.0000001) {
						for (int j = 0; j < p.npoints; j++) {
							x[j] *= scale;
							y[j] *= scale;
						}
					}

					PolygonRoi roi = new PolygonRoi(x, y, PolygonRoi.POLYGON);
					roi.setLocation(base.getX() * scale, base.getY() * scale);
					annotatePolygon(roi, colour);
				}
			}
		}
		return this;
	}

	/**
	 * Annotate the given signal group on the nucleus
	 * 
	 * @param n
	 * @param signalId
	 * @param colour   the signal colour
	 * @return this annotator
	 */
	public ImageAnnotator annotateSignal(@NonNull final Nucleus n, @NonNull UUID signalId,
			@NonNull Color colour) {

		ISignalCollection signalCollection = n.getSignalCollection();

		if (signalCollection.hasSignal(signalId)) {

			List<INuclearSignal> signals = signalCollection.getSignals(signalId);

			for (INuclearSignal s : signals) {
				IPoint base = s.getBase();

				FloatPolygon p = s.toPolygon();
				PolygonRoi roi = new PolygonRoi(p, PolygonRoi.POLYGON);

				roi.setLocation(base.getX(), base.getY());
				annotatePolygon(roi, colour);
			}
		}

		return this;
	}

	public static ImageProcessor createMergedWarpedSignals(List<IWarpedSignal> signals) {
		if (signals.isEmpty())
			return ImageFilterer.createWhiteByteProcessor(100, 100);

		// Recolour each of the grey images according to the stored colours
		List<ImageProcessor> recoloured = isCommonTargetSelected(signals)
				? recolourImagesWithSameTarget(signals)
				: recolourImagesWithDifferentTargets(signals);

		if (recoloured.size() == 1)
			return recoloured.get(0);

		// If multiple images are in the list, make an blend of their RGB
		// values so territories can be compared
		try {
			ImageProcessor ip1 = recoloured.get(0);
			for (int i = 1; i < recoloured.size(); i++) {
				// Weighting by fractions reduces intensity across the image
				// Weighting by integer multiples washes the image out.
				// Since there is little benefit to 3 or more blended,
				// just keep equal weighting
//        		float weight1 = i/(i+1);
//        		float weight2 = 1-weight1; 

				ip1 = ImageFilterer.blendImages(ip1, 1, recoloured.get(i), 1);
			}
			return ip1;

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error averaging images", e);
			return ImageFilterer.createWhiteByteProcessor(100, 100);
		}
	}

	/**
	 * Test if all the selected visible keys have the same target
	 * 
	 * @return
	 */
	private static synchronized boolean isCommonTargetSelected(List<IWarpedSignal> signals) {
		Nucleus t = signals.stream().findFirst().get().target();
		return signals.stream().allMatch(s -> s.target().getId().equals(t.getId()));
	}

	private static synchronized List<ImageProcessor> recolourImagesWithSameTarget(
			List<IWarpedSignal> signals) {
		List<ImageProcessor> recoloured = new ArrayList<>();
		for (IWarpedSignal k : signals) {
			// The image from the warper is greyscale. Change to use the signal colour
			ImageProcessor bp = k.toImage().convertToByteProcessor();
			bp.invert();

			ImageProcessor recol = bp;
			if (k.isPseudoColour())
				recol = ImageFilterer.recolorImage(bp, k.colour());
			else
				recol = bp.convertToColorProcessor();

			recol.setMinAndMax(0, k.displayThreshold());
			recoloured.add(recol);
		}
		return recoloured;
	}

	private static synchronized List<ImageProcessor> recolourImagesWithDifferentTargets(
			List<IWarpedSignal> signals) {

		List<ImageProcessor> images = ImageFilterer
				.fitToCommonCanvas(signals.stream().map(IWarpedSignal::toImage).toList());

		Map<IWarpedSignal, ImageProcessor> map = new HashMap<>();
		for (int i = 0; i < images.size(); i++)
			map.put(signals.get(i), images.get(i));

		List<ImageProcessor> recoloured = new ArrayList<>();

		for (Entry<IWarpedSignal, ImageProcessor> e : map.entrySet()) {

			// The image from the warper is greyscale. Change to use the signal colour
			ImageProcessor bp = e.getValue().convertToByteProcessor();
			bp.invert();

			ImageProcessor recol = bp;
			if (e.getKey().isPseudoColour())
				recol = ImageFilterer.recolorImage(bp, e.getKey().colour());
			else
				recol = bp.convertToColorProcessor();

			recol.setMinAndMax(0, e.getKey().displayThreshold());
			recoloured.add(recol);

		}
		return recoloured;
	}
}
