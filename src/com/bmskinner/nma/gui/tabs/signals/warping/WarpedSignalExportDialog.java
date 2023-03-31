package com.bmskinner.nma.gui.tabs.signals.warping;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.generic.FloatPoint;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.signals.IWarpedSignal;
import com.bmskinner.nma.core.DatasetListManager;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.gui.DefaultInputSupplier;
import com.bmskinner.nma.gui.dialogs.SettingsDialog;
import com.bmskinner.nma.io.Io;
import com.bmskinner.nma.utility.FileUtils;
import com.bmskinner.nma.visualisation.image.ImageFilterer;
import com.bmskinner.nma.visualisation.image.ImageAnnotator;
import com.bmskinner.nma.visualisation.image.ImageConverter;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.RGBStackMerge;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

public class WarpedSignalExportDialog extends SettingsDialog {

	private static final Logger LOGGER = Logger.getLogger(WarpedSignalExportDialog.class.getName());

	private static final String TITLE = "Export warped image";

	private static final String[] COLOUR_PAIRS = { "Red-Green", "Blue-Yellow", "Green-Magenta",
			"Existing" };

	private static final boolean IS_MODAL = true;

	private final List<IWarpedSignal> signals = new ArrayList<>();

	/** Show pseudocolours of warped images */
	private JCheckBox isIncludeConsensus;

	private JComboBox<String> colourPairsBox;

	private ImagePlus warpedImage;

	private JLabel imgLbl;

	private JButton exportBtn;

	public WarpedSignalExportDialog(List<IWarpedSignal> signals) {
		super(IS_MODAL);
		this.setTitle(TITLE);

		this.signals.addAll(signals);

		JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		infoPanel.add(new JLabel(
				"<html>Select a colour pair for signals; the preset options will<br>"
						+ " remain distinct even if the current signal pseudocolours are similar."
						+ "<br>Use your own colours with the 'Existing' option.</html>",
				SwingConstants.CENTER));

		JPanel btnpanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

		JPanel headerPanel = new JPanel();
		headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
		headerPanel.add(infoPanel);
		headerPanel.add(btnpanel);

		isIncludeConsensus = new JCheckBox("Include consensus");
		isIncludeConsensus.setSelected(true);
		isIncludeConsensus.addActionListener(e -> update());

		colourPairsBox = new JComboBox<>(COLOUR_PAIRS);
		colourPairsBox.addActionListener(e -> update());

		imgLbl = new JLabel("");

		exportBtn = new JButton("Export");
		exportBtn.addActionListener(e -> export());

		btnpanel.add(colourPairsBox);
		btnpanel.add(isIncludeConsensus);
		btnpanel.add(exportBtn);

		btnpanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		makeImage(true);

		JPanel imgPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		imgPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		imgLbl.setIcon(new ImageIcon(warpedImage.getBufferedImage()));
		imgPanel.add(imgLbl);

		add(headerPanel, BorderLayout.NORTH);
		add(imgPanel, BorderLayout.CENTER);

		this.setLocationRelativeTo(null);

		this.pack();
		this.setVisible(true);

	}

	private void update() {
		makeImage(isIncludeConsensus.isSelected());
		imgLbl.setIcon(new ImageIcon(warpedImage.getBufferedImage()));
	}

	private void export() {

		try {
			boolean doSave = true;

			Set<IAnalysisDataset> datasets = new HashSet<>();
			for (IWarpedSignal s : signals)
				datasets.add(DatasetListManager.getInstance().getDataset(s.sourceDatasetId()));

			File defaultFolder = FileUtils.commonPathOfDatasets(datasets);

			File saveFile = new DefaultInputSupplier().requestFileSave(defaultFolder,
					warpedImage.getTitle(),
					Io.TIFF_FILE_EXTENSION_NODOT);
			if (saveFile.exists()) {
				doSave = new DefaultInputSupplier().requestApproval("Overwrite existing file?",
						"Overwrite?");
			}
			if (doSave)
				IJ.saveAsTiff(warpedImage, saveFile.getAbsolutePath());

		} catch (RequestCancelledException e) {
		}
	}

	/**
	 * Export the selected warped image with no chart decorations. If one image is
	 * selected, the image is pseudocoloured and thresholded according to the
	 * current display settings. If two images are selected, the images are combined
	 * into an RGB image using green and magenta channels for easily distinguishable
	 * overlaps.
	 * 
	 * @throws RequestCancelledException
	 */
	private void makeImage(boolean includeConsensus) {
		int colourOption = colourPairsBox.getSelectedIndex();
		// If they want to use their own colours...
		if (colourOption == 3) {
			if (signals.size() == 1) {
				warpedImage = createSingleCustomImage(includeConsensus);
			} else {
				warpedImage = createDualCustomImage(includeConsensus);
			}

		}

		// Use the given preset
		if (colourOption < 3) {
			if (signals.size() == 1) {

				int c1 = colourOption == 0 ? 0 : colourOption == 1 ? 2 : 1;
				warpedImage = createSinglePresetImage(c1, includeConsensus);
			} else {

				int c1 = colourOption == 0 ? 0 : colourOption == 1 ? 2 : 1;
				int c2 = colourOption == 0 ? 1 : colourOption == 1 ? 6 : 5;
				warpedImage = createDualPresetImage(c1, c2, includeConsensus);
			}

		}
	}

	/**
	 * Get a colour name from an index of { R, G, B, W, C, M, Y }
	 * 
	 * @param i
	 * @return
	 */
	private String colToName(int i) {
		// { R, G, B, W, C, M, Y }
		return switch (i) {
		case 0 -> "red";
		case 1 -> "green";
		case 2 -> "blue";
		case 3 -> "white";
		case 4 -> "cyan";
		case 5 -> "magenta";
		case 6 -> "yellow";
		default -> "null";
		};
	}

	/**
	 * Create an image from two warped images, using the ImageJ colour merge tool
	 * 
	 * @param colour1 the index of the colour for image 1 in { R, G, B, W, C, M, Y }
	 * @param colour2 the index of the colour for image 2 in { R, G, B, W, C, M, Y }
	 * @return
	 */
	private ImagePlus createDualPresetImage(int colour1, int colour2, boolean includeConsensus) {

		IWarpedSignal s0 = signals.get(0);
		IWarpedSignal s1 = signals.get(1);

		List<ImageProcessor> imageList = ImageFilterer
				.fitToCommonCanvas(signals.stream().map(IWarpedSignal::toImage).toList());

		ImageProcessor ip1 = imageList.get(0);
		ImageProcessor ip2 = imageList.get(1);

		// Order of images - RGBWCMY
		ImagePlus[] images = { null, null, null, null, null, null, null };
		images[colour1] = new ImagePlus("1", ip1);
		images[colour2] = new ImagePlus("2", ip2);

		// Merge and flatten to an RGB image
		ImagePlus result = RGBStackMerge.mergeChannels(images, false).flatten();
		ImageProcessor ip3 = result.getProcessor();

		// Add a border so we don't drop outline pixels at the edge
		int buffer = 10;
		ip3 = ImageConverter.expandCanvas(ip3, buffer, Color.black);

		List<Nucleus> targets = signals.stream().map(IWarpedSignal::target).distinct().toList();

		String imageName = s0.sourceDatasetName() + "-" + s0.sourceSignalGroupName() + "_("
				+ colToName(colour1)
				+ ")_vs_" + s1.sourceDatasetName() + "-" + s1.sourceSignalGroupName() + "_("
				+ colToName(colour2)
				+ ")_on_" + s0.targetName();

		if (includeConsensus)
			return drawConsensusOnImage(ip3, targets, Color.white, imageName);
		ip3.flipVertical();
		return new ImagePlus(imageName, ip3);
	}

	/**
	 * Add a consensus nucleus to the current display image
	 * 
	 * @return
	 */
	private ImagePlus createSinglePresetImage(int colour1, boolean includeConsensus) {
		IWarpedSignal s = signals.get(0);
		ByteProcessor ip = s.toImage().convertToByteProcessor();

		ColorProcessor ip2 = new ColorProcessor(ip.getWidth(), ip.getHeight());
		ip2.setChannel(colour1 + 1, ip);

		String imageName = s.sourceDatasetName() + "-" + s.sourceSignalGroupName() + "_on_"
				+ s.targetName();

		// Add a border so we don't drop outline pixels at the edge
		int buffer = 10;
		ImageProcessor ip3 = ImageConverter.expandCanvas(ip2, buffer, Color.black);

		List<Nucleus> targets = List.of(s.target());

		if (includeConsensus)
			return drawConsensusOnImage(ip3, targets, Color.white, imageName);
		ip3.flipVertical();
		return new ImagePlus(imageName, ip3);
	}

	/**
	 * Add a consensus nucleus to the current display image
	 * 
	 * @return
	 */
	private ImagePlus createSingleCustomImage(boolean includeConsensus) {
		IWarpedSignal s = signals.get(0);
		ImageProcessor ip = ImageAnnotator.createMergedWarpedSignals(signals);
		int buffer = 10;
		ip = ImageConverter.expandCanvas(ip, buffer, Color.white);
		List<Nucleus> targets = signals.stream().map(IWarpedSignal::target).distinct().toList();

		String imageName = s.sourceDatasetName() + "-" + s.sourceSignalGroupName() + "_on_"
				+ s.targetName();

		if (includeConsensus)
			return drawConsensusOnImage(ip, targets, Color.black, imageName);
		ip.flipVertical();
		return new ImagePlus(imageName, ip);
	}

	/**
	 * Create a dual channel image for export with the consensus drawn atop, using
	 * the display image rather than recolouring to sensible values
	 * 
	 * @return
	 */
	private ImagePlus createDualCustomImage(boolean includeConsensus) {
		IWarpedSignal s0 = signals.get(0);
		IWarpedSignal s1 = signals.get(1);

		ImageProcessor ip = ImageAnnotator.createMergedWarpedSignals(signals);
		int buffer = 10;
		ip = ImageConverter.expandCanvas(ip, buffer, Color.white);
		List<Nucleus> targets = signals.stream().map(IWarpedSignal::target).distinct().toList();

		String imageName = s0.sourceDatasetName() + "-" + s0.sourceSignalGroupName() + "_vs_"
				+ s1.sourceDatasetName()
				+ "-" + s1.sourceSignalGroupName() + "_on_" + s0.targetName();

		if (includeConsensus)
			return drawConsensusOnImage(ip, targets, Color.black, imageName);
		ip.flipVertical();
		return new ImagePlus(imageName, ip);
	}

	private ImagePlus drawConsensusOnImage(ImageProcessor ip, List<Nucleus> targets, Color colour,
			String imageName) {

		try {
			for (Nucleus target : targets) {
				// Don't move the existing template
				target = target.duplicate();
				target.orient(); // ensure rotation is valid

				// Centre the outline on the canvas
				int wBuffer = (int) Math.round(ip.getWidth() - target.getWidth()) / 2;
				int hBuffer = (int) Math.round(ip.getHeight() - target.getHeight()) / 2;

				// CoM starts at 0, 0; offset to image coordinates
				target.moveCentreOfMass(
						new FloatPoint(Math.abs(target.getMinX()) + wBuffer,
								Math.abs(target.getMinY()) + hBuffer));
				ip.setColor(colour);

				// Draw the border
				for (IPoint p : target.getBorderList()) {
					ip.drawDot(p.getXAsInt(), p.getYAsInt());
				}
			}
		} catch (MissingLandmarkException e) {
			LOGGER.warning("Error getting consensus nucleus");
		}

		// Y-coordinates in images increase top to bottom
		ip.flipVertical();

		return new ImagePlus(imageName, ip);
	}

}
