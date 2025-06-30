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
import org.jfree.data.Range;
import org.jfree.svg.SVGGraphics2D;
import org.jfree.svg.SVGUnits;

import com.bmskinner.nma.gui.components.panels.ExportableChartPanel;

/**
 * Given a chart, produce an image suitable for a report or export
 * 
 * @author Ben Skinner
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
	 * When rescaling chart axes to fit the data, the amount of buffer to include on
	 * each end of axis. Expressed as a fraction of the axis length.
	 */
	private static final double RANGE_EXPANSION_FRACTION = 0.05;

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
	public static BufferedImage createPNG(JFreeChart chart, int wmm, int hmm, int dpi,
			boolean isFixedAspect)
			throws TranscoderException, IOException {

		final String svg = ChartImageConverter.createSVG(chart, wmm, hmm, dpi, isFixedAspect);

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
		final double mmPerPixel = PIXEL_SCALE_1_DPI / dpi;
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
		final double mmPerPixel = PIXEL_SCALE_1_DPI / dpi;
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
	public static String createSVG(JFreeChart chart, int wmm, int hmm, int dpi,
			boolean isFixedAspect) {

		if (isFixedAspect) {
			chart = fixAspect(chart, wmm, hmm);
		}

		// Get the DPI of the display
		int screenDpi = DEFAULT_SCREEN_DPI;
		try {
			screenDpi = Toolkit.getDefaultToolkit().getScreenResolution();
		} catch (final HeadlessException e) {
			// no monitors present to report, just use the default
			screenDpi = DEFAULT_SCREEN_DPI;
		}

		final double dpiScale = (double) screenDpi / dpi;

		final int wpx = mmToPixels(wmm, dpi);
		final int hpx = mmToPixels(hmm, dpi);

		// Adjust for scaling of chart elements
		final int w = (int) (wpx * dpiScale);
		final int h = (int) (hpx * dpiScale);

		final SVGGraphics2D g2 = new SVGGraphics2D(w, h, SVGUnits.PX);
		final Rectangle r = new Rectangle(0, 0, w, h);
		chart.draw(g2, r);
		return g2.getSVGDocument();
	}

	/**
	 * Given a chart with the desired output dimensions, change the axis ranges such
	 * that the exported image has a fixed aspect ratio. This will only ever
	 * increase the range of one axis.
	 * 
	 * @param chart the chart
	 * @param wmm   the output width
	 * @param hmm   the output height
	 * @return
	 */
	private static JFreeChart fixAspect(JFreeChart c, int wmm, int hmm) {
		try {

			// Clone the original chart. Note that the chart may have axes wider than the
			// data range in order that aspect is preserved. For an output figure with new
			// dimensions, the chart ranges may need to be changed. For example, single cell
			// outline images.
			final JFreeChart chart = (JFreeChart) c.clone();

			final double desiredAspect = (double) wmm / hmm;

			final Range xPlotRange = chart.getXYPlot().getDomainAxis().getRange();
			final Range yPlotRange = chart.getXYPlot().getRangeAxis().getRange();

			// Check if the actual ranges can be trimmed down to fit the data better.
			Range xDataRange = ExportableChartPanel.getDataDomainRange(chart.getXYPlot());
			Range yDataRange = ExportableChartPanel.getDataRangeRange(chart.getXYPlot());

			xDataRange = Range.expand(xDataRange, RANGE_EXPANSION_FRACTION, RANGE_EXPANSION_FRACTION);
			yDataRange = Range.expand(yDataRange, RANGE_EXPANSION_FRACTION, RANGE_EXPANSION_FRACTION);

			LOGGER.finest("X plot range %s".formatted(xPlotRange.toString()));
			LOGGER.finest("Y plot range %s".formatted(yPlotRange.toString()));
			LOGGER.finest("X data range %s".formatted(xDataRange.toString()));
			LOGGER.finest("Y data range %s".formatted(yDataRange.toString()));

			final double plotAspect = xPlotRange.getLength() / yPlotRange.getLength();
			final double dataAspect = xDataRange.getLength() / yDataRange.getLength();

			LOGGER.finest("Plot aspect %s".formatted(plotAspect));
			LOGGER.finest("Data aspect %s".formatted(dataAspect));
			LOGGER.finest("Desired aspect %s".formatted(desiredAspect));

			if (dataAspect < desiredAspect) { // y is longer than x, increase x axis range

				final double newXLength = yDataRange.getLength() * desiredAspect;
				final double toAdd = newXLength - xDataRange.getLength();

				// Fraction of range to expand each end of the axis by
				final double margin = (toAdd / xDataRange.getLength()) / 2;

				xDataRange = Range.expand(xDataRange, margin, margin);
				chart.getXYPlot().getDomainAxis().setRange(xDataRange);
				chart.getXYPlot().getRangeAxis().setRange(yDataRange);

			} else { // x is longer than y, increase y axis range
				final double newYLength = xDataRange.getLength() / desiredAspect;
				final double toAdd = newYLength - yDataRange.getLength();

				// Fraction of range to expand each end of the axis by
				final double margin = (toAdd / yDataRange.getLength()) / 2;

				yDataRange = Range.expand(yDataRange, margin, margin);
				chart.getXYPlot().getRangeAxis().setRange(yDataRange);
				chart.getXYPlot().getDomainAxis().setRange(xDataRange);
			}

			LOGGER.finest("Final plot x range %s".formatted(chart.getXYPlot().getDomainAxis().getRange()));
			LOGGER.finest("Final plot y range %s".formatted(chart.getXYPlot().getRangeAxis().getRange()));

			return chart;

		} catch (final CloneNotSupportedException e) {
			return c;
		}
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
		final float mmPerPixel = PIXEL_SCALE_1_DPI / dpi;
		final int w_px = mmToPixels(wmm, dpi);

		final TranscoderInput transcoderInput = new TranscoderInput(new StringReader(svg));

		final ByteArrayOutputStream resultByteStream = new ByteArrayOutputStream();

		final TranscoderOutput transcoderOutput = new TranscoderOutput(resultByteStream);

		final PNGTranscoder pngTranscoder = new PNGTranscoder();
		pngTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, (float) w_px);
		pngTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER,
				mmPerPixel);
		pngTranscoder.transcode(transcoderInput, transcoderOutput);

		resultByteStream.flush();

		return ImageIO.read(new ByteArrayInputStream(resultByteStream.toByteArray()));
	}
}
