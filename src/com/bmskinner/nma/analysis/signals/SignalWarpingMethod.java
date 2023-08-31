package com.bmskinner.nma.analysis.signals;

import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nma.components.MissingComponentException;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.mesh.DefaultMesh;
import com.bmskinner.nma.components.mesh.DefaultMeshImage;
import com.bmskinner.nma.components.mesh.Mesh;
import com.bmskinner.nma.components.mesh.MeshCreationException;
import com.bmskinner.nma.components.mesh.MeshImage;
import com.bmskinner.nma.components.mesh.MeshImageCreationException;
import com.bmskinner.nma.components.mesh.UncomparableMeshImageException;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.options.MissingOptionException;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.signals.DefaultWarpedSignal;
import com.bmskinner.nma.components.signals.ISignalGroup;
import com.bmskinner.nma.components.signals.IWarpedSignal;
import com.bmskinner.nma.components.signals.SignalManager;
import com.bmskinner.nma.gui.components.ColourSelecter;
import com.bmskinner.nma.gui.tabs.signals.warping.SignalWarpingRunSettings;
import com.bmskinner.nma.io.ImageImporter;
import com.bmskinner.nma.io.ImageImporter.ImageImportException;
import com.bmskinner.nma.io.UnloadableImageException;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.visualisation.image.ImageFilterer;

import ij.process.ImageProcessor;

public class SignalWarpingMethod extends SingleDatasetAnalysisMethod {

	private static final Logger LOGGER = Logger.getLogger(SignalWarpingMethod.class.getName());

	/** The minimum value signals must have to be included */
	public static final int DEFAULT_MIN_SIGNAL_THRESHOLD = 0;

	/** The options for the analysis */
	protected final SignalWarpingRunSettings options;

	/** The number of cell images to be merged */
	private int totalCells;

	/** The mesh images are warped onto */
	private final Mesh meshConsensus;

	public SignalWarpingMethod(@NonNull final IAnalysisDataset d,
			@NonNull final SignalWarpingRunSettings options) {
		super(d);
		this.options = options;

		// Count the number of cells to include
		SignalManager m = dataset.getCollection().getSignalManager();
		List<ICell> cells = options
				.getBoolean(SignalWarpingRunSettings.IS_ONLY_CELLS_WITH_SIGNALS_KEY)
						? m.getCellsWithNuclearSignals(options.signalId(), true)
						: dataset.getCollection().getCells();
		totalCells = cells.size();
		try {

			Nucleus target = options.targetDataset().getCollection().getConsensus().duplicate();

			// Create the consensus mesh to warp each cell onto
			meshConsensus = new DefaultMesh(target);
		} catch (MeshCreationException | MissingLandmarkException | ComponentCreationException e2) {
			LOGGER.log(Loggable.STACK, "Error creating mesh", e2);
			throw new IllegalArgumentException("Could not create mesh", e2);
		}

	}

	@Override
	public IAnalysisResult call() throws Exception {
		fireUpdateProgressTotalLength(totalCells);
		run();
		return new DefaultAnalysisResult(dataset);
	}

	public void run() throws Exception {
		List<ImageProcessor> warpedImages = generateImages();

		// A 16-bit image
		ImageProcessor finalImage = ImageFilterer.addByteImages(warpedImages);

		ISignalGroup sg = dataset.getCollection().getSignalGroup(options.signalId())
				.orElseThrow(MissingComponentException::new);

		IWarpedSignal ws = new DefaultWarpedSignal(
				options.targetDataset().getCollection().getConsensus().duplicate(),
				options.targetDataset().getName(), dataset.getName(), sg.getGroupName(),
				options.templateDataset().getId(),
				options.getBoolean(SignalWarpingRunSettings.IS_ONLY_CELLS_WITH_SIGNALS_KEY),
				options.getInt(SignalWarpingRunSettings.MIN_THRESHOLD_KEY),
				options.getBoolean(SignalWarpingRunSettings.IS_BINARISE_SIGNALS_KEY),
				options.getBoolean(SignalWarpingRunSettings.IS_NORMALISE_TO_COUNTERSTAIN_KEY),
				IWarpedSignal.toArray(finalImage), finalImage.getWidth(),
				sg.getGroupColour().orElse(ColourSelecter.getSignalColour(0)), 255);

		sg.addWarpedSignal(ws);
	}

	/**
	 * Create the warped images for all selected nuclei in the dataset
	 * 
	 * @throws MissingOptionException
	 * 
	 */
	private List<ImageProcessor> generateImages() throws MissingOptionException {
		LOGGER.finer("Generating warped images for " + options.templateDataset().getName());
		final List<ImageProcessor> warpedImages = Collections.synchronizedList(new ArrayList<>());

		List<ICell> cells = getCells(
				options.getBoolean(SignalWarpingRunSettings.IS_ONLY_CELLS_WITH_SIGNALS_KEY));

		for (ICell c : cells) {
			for (Nucleus n : c.getNuclei()) {
				LOGGER.finer("Drawing signals for " + n.getNameAndNumber());
				ImageProcessor nImage = generateNucleusImage(n);
				warpedImages.add(nImage);
				fireProgressEvent();
			}
		}
		return warpedImages;
	}

	/**
	 * The empty processor to return if a warp fails. Finds the image dimensions for
	 * the warped images - used for blank images if the warping fails
	 * 
	 * @return
	 */
	private ImageProcessor createEmptyProcessor() {
		Rectangle r = meshConsensus.toPath().getBounds();
		return ImageFilterer.createBlackByteProcessor(r.width, r.height);
	}

	/**
	 * Create the warped image for a nucleus
	 * 
	 * @param n the nucleus to warp
	 * @return the warped image
	 * @throws MissingOptionException
	 */
	private ImageProcessor generateNucleusImage(@NonNull Nucleus n) throws MissingOptionException {

		try {
			Mesh cellMesh = new DefaultMesh(n, meshConsensus);

			ImageProcessor ip = getNucleusImageProcessor(n);

			if (options.getInt(SignalWarpingRunSettings.MIN_THRESHOLD_KEY) > 0)
				ip = new ImageFilterer(ip)
						.setBlackLevel(options.getInt(SignalWarpingRunSettings.MIN_THRESHOLD_KEY))
						.toProcessor();

			if (options.getBoolean(SignalWarpingRunSettings.IS_BINARISE_SIGNALS_KEY))
				ip.threshold(options.getInt(SignalWarpingRunSettings.MIN_THRESHOLD_KEY));

			if (options.getBoolean(SignalWarpingRunSettings.IS_NORMALISE_TO_COUNTERSTAIN_KEY)) {
				ip = new ImageFilterer(ip)
						.normaliseToCounterStain(ImageImporter.importFullImageTo8bit(n))
						.toProcessor();

				// The actual floating point values may not be visible to the human eye
				// Rescale the values to lie in the 0-255 range
				ip = ImageFilterer.rescaleImageIntensity(ip);
			}

			// Create a mesh coordinate image from the nucleus
			MeshImage meshImage = new DefaultMeshImage(cellMesh, ip);

			// Draw the mesh image onto the consensus mesh.
			LOGGER.finer("Warping image onto consensus mesh");
			return meshImage.drawImage(meshConsensus);

		} catch (IllegalArgumentException | MeshCreationException | UncomparableMeshImageException
				| MeshImageCreationException | UnloadableImageException e) {
			LOGGER.fine("Could not create warped image for " + n.getNameAndNumber() + ": "
					+ e.getMessage());
			return createEmptyProcessor();
		}

	}

	/**
	 * Fetch the appropriate image to warp for the given nucleus
	 * 
	 * @param n the nucleus to warp
	 * @return the nucleus image
	 * @throws MissingOptionException
	 */
	private ImageProcessor getNucleusImageProcessor(@NonNull Nucleus n)
			throws MissingOptionException {

		try {
			// Get the image with the signal
			ImageProcessor ip;
			if (n.getSignalCollection().hasSignal(options.signalId())) { // if there is no signal,
																			// getImage will throw
																			// exception
				ip = n.getSignalCollection().getImage(options.signalId());
				ip.invert(); // image is imported as white background. Need black background.
			} else {
				// We need to get the file in which no signals were detected
				// This is not stored in a nucleus, so combine the expected file name
				// with the source folder
				HashOptions signalOptions = getSignalOptions(n);

				if (signalOptions != null) {
					File imageFolder = options.templateDataset().getAnalysisOptions().get()
							.getNuclearSignalDetectionFolder(options.signalId())
							.orElseThrow(MissingOptionException::new);
					File imageFile = new File(imageFolder, n.getSourceFileName());
					ip = ImageImporter
							.importImage(imageFile, signalOptions.getInt(HashOptions.CHANNEL));

				} else {
					return createEmptyProcessor();
				}
			}
			return ip;
		} catch (UnloadableImageException | ImageImportException e) {
			LOGGER.log(Loggable.STACK, e.getMessage(), e);
			return createEmptyProcessor();
		}
	}

	/**
	 * Get the nuclear signal detection options, accounting for whether the dataset
	 * is merged or not merged.
	 * 
	 * @param n the nucleus to fetch options for
	 * @return the signal options if present, otherwise null
	 * @throws MissingOptionException
	 */
	private HashOptions getSignalOptions(@NonNull Nucleus n) throws MissingOptionException {

		// If merged datasets are being warped, the imageFolder will not
		// be correct, since the analysis options are mostly blank. We need
		// to find the correct source dataset, and take the analysis options
		// from that dataset.
		if (options.templateDataset().hasMergeSources()) {

			return options.templateDataset().getAllMergeSources().stream()
					.filter(d -> d.getCollection().contains(n))
					.findFirst().get().getAnalysisOptions()
					.orElseThrow(MissingOptionException::new)
					.getNuclearSignalOptions(options.signalId())
					.orElseThrow(MissingOptionException::new);
		}

		Optional<IAnalysisOptions> analysisOptions = options.templateDataset()
				.getAnalysisOptions();

		if (analysisOptions.isPresent()) {
			return analysisOptions.get().getNuclearSignalOptions(options.signalId())
					.orElseThrow(MissingOptionException::new);

		}

		return null;
	}

	/**
	 * Get the cells to be used for the warping
	 * 
	 * @param withSignalsOnly
	 * @return
	 */
	private List<ICell> getCells(boolean withSignalsOnly) {

		SignalManager m = options.templateDataset().getCollection().getSignalManager();
		List<ICell> cells;
		if (withSignalsOnly) {
			LOGGER.finer("Only fetching cells with signals");
			cells = m.getCellsWithNuclearSignals(options.signalId(), true);
		} else {
			LOGGER.finer("Fetching all cells");
			cells = options.templateDataset().getCollection().getCells();

		}
		return cells;
	}
}
