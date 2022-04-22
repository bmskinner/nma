package com.bmskinner.nuclear_morphology.visualisation.image;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.ComponentOrienter;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.UnavailableBorderPointException;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.cells.Nucleus;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.rules.PriorityAxis;
import com.bmskinner.nuclear_morphology.components.signals.INuclearSignal;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.io.ImageImporter;

/**
 * Paints cell outlines on JPanels, correcting for scales
 * 
 * @author ben
 * @since 2.0.0
 *
 */
public class CellImagePainter implements ImagePainter {

	private static final Logger LOGGER = Logger.getLogger(CellImagePainter.class.getName());

	private ICell cell;
	private CellularComponent component; // the source for images
	private int originalWidth;
	private int originalHeight;
	private boolean isOriented;

	/**
	 * Create with cell to paint
	 * 
	 * @param cell
	 */
	public CellImagePainter(@NonNull ICell cell, @NonNull CellularComponent component, boolean isOrient) {
		this.cell = cell;
		this.isOriented = isOrient;
		this.component = component;
	}

	@Override
	public BufferedImage paintDecorated(int w, int h) {

		BufferedImage input = paintRaw(w, h);

		try {

			// A transform to convert from original image coordinates to the final screen
			// coordinates

//			Values vals = calcRotatedImageDimensions(w, h);
//			double w2 = Math.cos(vals.rads) * originalWidth;
//			double h2 = Math.cos(vals.rads) * originalHeight;
//			AffineTransform at = createAspectPreservingScaleTransform((int) w2, (int) h2, w, h);

			AffineTransform at = createAspectPreservingScaleTransform(originalWidth, originalHeight, w, h);

			if (isOriented) {
				// If the image has been rotated, we will need to scale the points drawn down
				// by a ratio of the new image dimensions

				AffineTransform tat = createMainTransform(originalWidth, originalHeight);
				at.concatenate(tat);
//				pat.concatenate(tat);

//				pat.concatenate(at);
//				at = tat;
			}

			BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2 = output.createGraphics();
			g2.drawImage(input, 0, 0, null);
			paintNucleus(output, at);
			paintSignals(output, at);

			return output;
		} catch (MissingProfileException | ProfileException | UnavailableBorderPointException | MissingLandmarkException
				| ComponentCreationException e) {
			LOGGER.log(Level.FINE, "Unable to paint cell", e);
		}

		return input;
	}

	private void paintNucleus(BufferedImage output, AffineTransform at) throws MissingProfileException,
			MissingLandmarkException, ProfileException, UnavailableBorderPointException {
		Nucleus n = cell.getPrimaryNucleus();
		Graphics2D g2 = output.createGraphics();
		Object saved = g2.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL);
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

		g2.setStroke(new BasicStroke(3));

		ISegmentedProfile sp = cell.getPrimaryNucleus().getProfile(ProfileType.ANGLE);

		List<IProfileSegment> segs = sp.getSegments();
		for (int i = 0; i < segs.size(); i++) {
			g2.setColor(ColourSelecter.getColor(i));

			IProfileSegment seg = segs.get(i);

			for (int j = 0; j <= seg.length(); j++) {
				int k = n.wrapIndex(seg.getStartIndex() + j + n.getBorderIndex(Landmark.REFERENCE_POINT) - 1);
				IPoint p = n.getBorderPoint(k).minus(n.getBase()).plus(CellularComponent.COMPONENT_BUFFER);

				Point2D p2 = at.transform(p.toPoint2D(), null);

				double x = p2.getX();
				double y = p2.getY();
				g2.drawLine((int) x, (int) y, (int) x, (int) y);
			}

		}

		// Draw centre of mass
		IPoint com = n.getCentreOfMass().minus(n.getBase()).plus(CellularComponent.COMPONENT_BUFFER);
		Point2D com2 = at.transform(com.toPoint2D(), null);

		double x = com2.getX();
		double y = com2.getY();
		g2.setColor(Color.PINK);
		g2.drawLine((int) x, (int) y, (int) x, (int) y);

		// Draw the landmarks
		g2.setStroke(new BasicStroke(5));

		for (Landmark lm : n.getLandmarks().keySet()) {
			IPoint lp = n.getBorderPoint(lm).minus(n.getBase()).plus(CellularComponent.COMPONENT_BUFFER);
			Point2D lp2 = at.transform(lp.toPoint2D(), null);
			x = lp2.getX();
			y = lp2.getY();
			g2.setColor(ColourSelecter.getColour(lm));
			g2.drawLine((int) x, (int) y, (int) x, (int) y);
		}

		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, saved);
	}

	private void paintSignals(BufferedImage output, AffineTransform at) throws UnavailableBorderPointException {
		Nucleus n = cell.getPrimaryNucleus();
		Graphics2D g2 = output.createGraphics();
		Object saved = g2.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

		g2.setStroke(new BasicStroke(3));

		for (UUID id : n.getSignalCollection().getSignalGroupIds()) {

			for (INuclearSignal s : n.getSignalCollection().getSignals(id)) {
				g2.setColor(ColourSelecter.getColor(2));

				for (int j = 0; j < s.getBorderLength(); j++) {
					IPoint p = s.getBorderPoint(j).minus(n.getBase()).plus(CellularComponent.COMPONENT_BUFFER);

					Point2D p2 = at.transform(p.toPoint2D(), null);

					double x = p2.getX();
					double y = p2.getY();
					g2.drawLine((int) x, (int) y, (int) x, (int) y);
				}

				// Draw signal CoM
				IPoint com = s.getCentreOfMass().minus(n.getBase()).plus(CellularComponent.COMPONENT_BUFFER);
				Point2D com2 = at.transform(com.toPoint2D(), null);

				double x = com2.getX();
				double y = com2.getY();
				g2.drawLine((int) x, (int) y, (int) x, (int) y);
			}

		}

		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, saved);
	}

	@Override
	public BufferedImage paintRaw(int w, int h) {

		BufferedImage input = ImageImporter.importCroppedImageTo24bit(cell, component).getBufferedImage();
		originalWidth = input.getWidth();
		originalHeight = input.getHeight();

		if (!isOriented)
			return scalePreservingAspect(input, w, h);

		try {

			Values val = calcRotatedImageDimensions(input.getWidth(), input.getHeight());

			// Make a new canvas for the rotated image with white background
			BufferedImage output = new BufferedImage(val.w, val.h, BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics = output.createGraphics();
			graphics.setPaint(Color.WHITE);
			graphics.fillRect(0, 0, output.getWidth(), output.getHeight());

			AffineTransform at = createMainTransform(input.getWidth(), input.getHeight());
			AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);

			op.filter(input, output);

			return scalePreservingAspect(output, w, h);
		} catch (Exception e) {
			//
		}
		return input;
	}

	/**
	 * Create the transform needed to scale an image preserving aspect ratio.
	 * Converts from the original image dimensionms to the dimensions that will fill
	 * a given area.
	 * 
	 * @param w    the original width
	 * @param h    the original height
	 * @param maxW the maximum width
	 * @param maxH the maximum height
	 * @return
	 */
	private AffineTransform createAspectPreservingScaleTransform(int w, int h, double maxW, double maxH) {
		double r = calculateAspectPreservingScaleRatio(w, h, maxW, maxH);
		return AffineTransform.getScaleInstance(r, r);
	}

	/**
	 * Calculate the scale ratio needed to fill a space preserving aspect ratio
	 * 
	 * @param w
	 * @param h
	 * @param maxW
	 * @param maxH
	 * @return
	 */
	private double calculateAspectPreservingScaleRatio(int w, int h, double maxW, double maxH) {
		double r = maxW / w;
		if (r * h > maxH)
			r = maxH / h;
		return r;
	}

	/**
	 * Tuple for result of rotation calculation
	 * 
	 * @author ben
	 *
	 */
	public record Values(double rads, int w, int h, double ratio) {
	}

	/**
	 * Calculate the dimensions of an image of given dimensions after rotating the
	 * nucleus it contains to be oriented
	 * 
	 * @param w
	 * @param h
	 * @return
	 * @throws MissingLandmarkException
	 */
	private Values calcRotatedImageDimensions(int w, int h) throws MissingLandmarkException {
		double angle = 360 - ComponentOrienter.calcAngleToAlignVertically(cell.getPrimaryNucleus());
		double rads = Math.toRadians(angle);
		double sin = Math.abs(Math.sin(rads)), cos = Math.abs(Math.cos(rads));
		double newWidth = Math.floor(w * cos + h * sin);
		double newHeight = Math.floor(h * cos + w * sin);
		return new Values(rads, (int) newWidth, (int) newHeight, w / newWidth);
	}

	/**
	 * Create the transform needed to rotate, flip and translate the image to a
	 * rotated state.
	 * 
	 * @param w the original image height
	 * @param h the original image widht
	 * @return
	 * @throws MissingLandmarkException
	 * @throws ComponentCreationException
	 */
	private AffineTransform createMainTransform(int w, int h)
			throws MissingLandmarkException, ComponentCreationException {

		// Figure out how large the new image will be
		// so we can add a translation to paint the image
		// in the centre.

		Values p = calcRotatedImageDimensions(w, h);
		int newWidth = p.w;
		int newHeight = p.h;

		int x = (newWidth - w) / 2;
		int y = (newHeight - h) / 2;

		// Create the rotation transform about the centre of the image
		AffineTransform at = new AffineTransform();
		at.setToRotation(p.rads, x + (w / 2), y + (h / 2));
		at.translate(x, y);

		// Create the flipping transform
		AffineTransform at2 = new AffineTransform();
		at2.scale(1, -1); // flip vertical
		at2.translate(0, -newHeight);

		if (ComponentOrienter.isFlipNeeded(cell.getPrimaryNucleus())) {
			if (PriorityAxis.Y.equals(cell.getPrimaryNucleus().getPriorityAxis())) {
				at2.scale(-1, 1); // flip horizontal
				at2.translate(-newWidth, 0);
			} else {
				at2.scale(1, -1); // flip vertical
				at2.translate(0, -newHeight);
			}
		}

		// Concatenate them in reverse order
		at2.concatenate(at);
		return at2;

	}

	/**
	 * Scale an image to fit the given dimensions while preserving aspect ratio
	 * 
	 * @param input
	 * @param maxW
	 * @param maxH
	 * @return
	 */
	private BufferedImage scalePreservingAspect(BufferedImage input, double maxW, double maxH) {
		double r = calculateAspectPreservingScaleRatio(input.getWidth(), input.getHeight(), maxW, maxH);
		AffineTransform at = createAspectPreservingScaleTransform(input.getWidth(), input.getHeight(), maxW, maxH);
		BufferedImage output = new BufferedImage((int) (input.getWidth() * r), (int) (input.getHeight() * r),
				BufferedImage.TYPE_INT_ARGB);
		return new AffineTransformOp(at, AffineTransformOp.TYPE_BICUBIC).filter(input, output);
	}

	@Override
	public BufferedImage paintMagnified(BufferedImage smallInput, BufferedImage largeInput, int cx, int cy,
			int smallRadius, int bigRadius) {

		// the destination rectangle that will be painted to in the output
		int dx1 = cx - bigRadius;
		int dy1 = cy - bigRadius;
		int dx2 = cx + bigRadius;
		int dy2 = cy + bigRadius;

		// the source rectangle that will be sampled from the large input image
		double largeRatio = largeInput.getWidth() / smallInput.getWidth();
		int sx = (int) (cx * largeRatio);
		int sy = (int) (cy * largeRatio);

		int sx1 = sx - smallRadius;
		int sy1 = sy - smallRadius;
		int sx2 = sx + smallRadius;
		int sy2 = sy + smallRadius;

		// Find the clicked point in the original cell image
		double ratio = smallInput.getWidth() / originalWidth;
		double cellX = cx / ratio;
		double cellY = cy / ratio;

		// Find the cell border point under the cursor
		Optional<IPoint> point = cell.getPrimaryNucleus().getBorderList().stream()
				.map(p -> p.minus(cell.getPrimaryNucleus().getBase()).plus(CellularComponent.COMPONENT_BUFFER))
				.filter(p -> {
					return cellX >= p.getX() - 0.4 && cellX <= p.getX() + 0.4 && cellY >= p.getY() - 0.4
							&& cellY <= p.getY() + 0.4;

				}).findFirst();

		// Create the output image, copying the small input
		BufferedImage output = new BufferedImage(smallInput.getWidth(), smallInput.getHeight(),
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = output.createGraphics();
		g2.drawImage(smallInput, 0, 0, null);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

		// Choose the region of the large image to copy
		g2.drawImage(largeInput, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);

		// Save old colours
		Color c = g2.getColor();
		Stroke s = g2.getStroke();

		try {
			// Annotate the borders of the rectangle based on underlying content

			if (point.isPresent()) {
				g2.setColor(Color.CYAN);

				// Highlight the border depending on what border tags are present
				for (Landmark lm : cell.getPrimaryNucleus().getLandmarks().keySet()) {
					IPoint lp = cell.getPrimaryNucleus().getBorderPoint(lm).minus(cell.getPrimaryNucleus().getBase())
							.plus(CellularComponent.COMPONENT_BUFFER);
					if (cellX >= lp.getX() - 0.4 && cellX <= lp.getX() + 0.4 && cellY >= lp.getY() - 0.4
							&& cellY <= lp.getY() + 0.4) {
						g2.setColor(ColourSelecter.getColour(lm));
					}
				}

				g2.setStroke(new BasicStroke(3));
			} else {
				g2.setColor(Color.BLACK);
				g2.setStroke(new BasicStroke(2));
			}
		} catch (MissingLandmarkException e) {
			// no action needed, colour remains cyan
		}

		g2.drawRect(dx1, dy1, bigRadius * 2, bigRadius * 2);

		g2.setColor(c);
		g2.setStroke(s);
		return output;
	}

}
