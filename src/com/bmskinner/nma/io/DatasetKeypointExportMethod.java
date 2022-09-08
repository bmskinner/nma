package com.bmskinner.nma.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.MultipleDatasetAnalysisMethod;
import com.bmskinner.nma.components.MissingLandmarkException;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.logging.Loggable;

import ij.IJ;

public class DatasetKeypointExportMethod extends MultipleDatasetAnalysisMethod implements Io {
	private static final Logger LOGGER = Logger
			.getLogger(DatasetKeypointExportMethod.class.getName());

	private static final String EXPORT_FOLDER = "Annotations_";

	private File outputFolder;

	/**
	 * Options are kept for future use even if not used at present
	 */
	private final HashOptions options;

	/**
	 * Create with a dataset of cells to export and options
	 * 
	 * @param dataset
	 * @param options
	 */
	public DatasetKeypointExportMethod(@NonNull IAnalysisDataset dataset,
			@NonNull HashOptions options) {
		super(dataset);
		this.options = options;
	}

	/**
	 * Create with datasets of cells to export and options
	 * 
	 * @param datasets
	 * @param options
	 */
	public DatasetKeypointExportMethod(@NonNull List<IAnalysisDataset> datasets,
			@NonNull HashOptions options) {
		super(datasets);
		this.options = options;
	}

	@Override
	public IAnalysisResult call() throws Exception {
		for (IAnalysisDataset d : datasets)
			exportKeypoints(d);
		return new DefaultAnalysisResult(datasets);
	}

	private void exportKeypoints(IAnalysisDataset d)
			throws MissingLandmarkException {
		outputFolder = new File(
				d.getSavePath().getParent() + File.separator + EXPORT_FOLDER + d.getName());
		if (!outputFolder.exists())
			outputFolder.mkdirs();

		for (File f : d.getCollection().getImageFiles()) {
			String fileName = String.format("%s.json", f.getName().replaceAll("\\.\\w+$", ""));

			File exportFile = new File(outputFolder, fileName);
			try {
				Files.deleteIfExists(exportFile.toPath());
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "Unable to delete file: " + fileName);
				LOGGER.log(Loggable.STACK, "Unable to delete existing file", e);
			}

			// Store coordinate pairs in JSON
			List<String> bb = new ArrayList<>();
			List<String> kk = new ArrayList<>();
			for (ICell c : d.getCollection().getCells(f)) {

				for (Nucleus n : c.getNuclei()) {

					// TODO - expand beyond rodent sperm keypoints
					IPoint rp = n.getBorderPoint(OrientationMark.REFERENCE);
					IPoint op = n.getBorderPoint(OrientationMark.Y);

					IPoint p1 = n.getBase();
					int x2 = (int) (p1.getXAsInt() + n.getWidth());
					int y2 = (int) (p1.getYAsInt() + n.getHeight());

					bb.add("[%s, %s, %s, %s]".formatted(p1.getXAsInt(), p1.getYAsInt(), x2, y2));
					kk.add("[[%s, %s, 1], [%s, %s, 1]]".formatted(rp.getXAsInt(), rp.getYAsInt(),
							op.getXAsInt(), op.getYAsInt()));
				}
			}

			// Combine into JSON
			String bboxes = "{\"bboxes\":[" + bb.stream().collect(Collectors.joining(", ")) + "], ";
			String keypoints = "\"keypoints\":[" + kk.stream().collect(Collectors.joining(", "))
					+ "]} ";

			IJ.append(bboxes + keypoints, exportFile.getAbsolutePath());
			fireProgressEvent();
		}

	}
}
