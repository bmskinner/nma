package com.bmskinner.nma.visualisation;

import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.jfree.chart.JFreeChart;
import org.jfree.svg.SVGGraphics2D;
import org.jfree.svg.SVGUnits;

/**
 * Given a chart, produce an image suitable for a report or export
 * 
 * @author bs19022
 *
 */
public class ChartImageConverter {

	private static final Logger LOGGER = Logger.getLogger(ChartImageConverter.class.getName());

	/** The resolution that a chart expects to draw an image to */
	private static final int DEFAULT_SCREEN_DPI = 96;

	/** The number of mm in a pixel at 300 DPI */
	private static final float PIXEL_SCALE_300_DPI = 0.084672f;

	/** The number of mm in a pixel at 1 DPI */
	private static final float PIXEL_SCALE_1_DPI = 0.084672f * 300f;

	/**
	 * Create a PNG representation of the given chart at 300 DPI
	 * 
	 * @param chart the chart to draw
	 * @param w     the desired width in mm
	 * @param h     the desired height in mm
	 * @return
	 * @throws TranscoderException
	 * @throws IOException
	 */
	public static BufferedImage createPNG(JFreeChart chart, int wmm, int hmm, int dpi)
			throws TranscoderException, IOException {

		String svg = ChartImageConverter.createSVG(chart, wmm, hmm, dpi);

		return ChartImageConverter.convertSVGToPNG(svg, wmm, dpi);
	}

	/**
	 * Calculate the number of pixels required in an image of the given dimension
	 * and resolution.
	 * 
	 * @param mm
	 * @param dpi
	 * @return
	 */
	public static int mmToPixels(int mm, int dpi) {
		// The number of mm in a single pixel at this resolution
		double mmPerPixel = PIXEL_SCALE_1_DPI / dpi;
		return (int) Math.round(mm / mmPerPixel);
	}

	/**
	 * Calculate the number of mm covered by pixels at the given resolution.
	 * 
	 * @param mm
	 * @param dpi
	 * @return
	 */
	public static int pixelsToMM(int pixels, int dpi) {
		// The number of mm in a single pixel at this resolution
		double mmPerPixel = PIXEL_SCALE_1_DPI / dpi;
		return (int) Math.round(mmPerPixel * pixels);

	}

	/**
	 * Create an SVG representation of the given chart scaled to the given output
	 * dimensions. Charts assume that they will be drawn at a 72 or 96 DPI for a
	 * monitor. If we ask for the standard 1004 or 2008 pixel width output for
	 * rendering at 300 DPI the text and line elements will be too small to read. We
	 * therefore need to generate the SVG for an assumed lower resolution, so all
	 * vector elements have an appropriate weight.
	 * 
	 * @param chart the chart to draw
	 * @param wmm   the output width in mm
	 * @param hmm   the output height in mm
	 * @param dpi   the resolution of the output image
	 * @return
	 */
	public static String createSVG(JFreeChart chart, int wmm, int hmm, int dpi) {

		// Get the DPI of the display
		int screenDpi = DEFAULT_SCREEN_DPI;
		try {
			screenDpi = Toolkit.getDefaultToolkit().getScreenResolution();
		} catch (HeadlessException e) {
			// no monitors present to report, just use the default
			screenDpi = DEFAULT_SCREEN_DPI;
		}

		double dpiScale = (double) screenDpi / dpi;

		int w_px = mmToPixels(wmm, dpi);
		int h_px = mmToPixels(hmm, dpi);

		// Adjust for scaling of chart elements
		int w = (int) (w_px * dpiScale);
		int h = (int) (h_px * dpiScale);

		SVGGraphics2D g2 = new SVGGraphics2D(w, h, SVGUnits.PX);

		Rectangle r = new Rectangle(0, 0, w, h);
		chart.draw(g2, r);
		return g2.getSVGDocument();
	}

	/**
	 * Convert the given SVG string to a PNG of the given width. The height is
	 * calculated automatically from the SVG aspect ratio.
	 * 
	 * @param svg     the SVG image
	 * @param wPixels the output width in pixels
	 * @return
	 * @throws TranscoderException
	 * @throws IOException
	 */
	private static BufferedImage convertSVGToPNG(String svg, int wmm, int dpi)
			throws TranscoderException, IOException {
		float mmPerPixel = PIXEL_SCALE_1_DPI / dpi;
		int w_px = mmToPixels(wmm, dpi);

		TranscoderInput transcoderInput = new TranscoderInput(new StringReader(svg));

		ByteArrayOutputStream resultByteStream = new ByteArrayOutputStream();

		TranscoderOutput transcoderOutput = new TranscoderOutput(resultByteStream);

		PNGTranscoder pngTranscoder = new PNGTranscoder();
		pngTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, (float) w_px);
		pngTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER,
				mmPerPixel);
		pngTranscoder.transcode(transcoderInput, transcoderOutput);

		resultByteStream.flush();

		return ImageIO.read(new ByteArrayInputStream(resultByteStream.toByteArray()));
	}
}
