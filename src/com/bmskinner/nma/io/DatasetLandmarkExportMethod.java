package com.bmskinner.nma.io;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.profiles.Landmark;

/**
 * Export the locations of landmarks of datasets to file. The file will be tab-separated with the following fields:
 * 
 * <pre>
 * Dataset name
 * Image folder name
 * Image file name
 * Cell ID
 * Nucleus ID (under the generic name component ID)
 * Nucleus bounding box x min
 * Nucleus bounding box x max
 * Nucleus bounding box y min
 * Nucleus bounding box y max
 * Landmark name
 * Landmark x float coordinate
 * Landmark y float coordinate
 * </pre>
 */
public class DatasetLandmarkExportMethod extends MeasurementsExportMethod implements Io {
	private static final Logger LOGGER = Logger
			.getLogger(DatasetLandmarkExportMethod.class.getName());


	/**
	 * Create with a dataset of cells to export and options
	 * 
	 * @param dataset
	 * @param options
	 */
	public DatasetLandmarkExportMethod(@NonNull File file, @NonNull List<IAnalysisDataset> list,
			@NonNull HashOptions options) {
		super(file, list, options);
	}

	/**
	 * Create with datasets of cells to export and options
	 * 
	 * @param datasets
	 * @param options
	 */
	public DatasetLandmarkExportMethod(@NonNull File file, @NonNull IAnalysisDataset dataset,
			@NonNull HashOptions options) {
		this(file,List.of(dataset), options);
	}

	@Override
	protected void appendHeader(@NonNull StringBuilder outLine) {
		outLine.append("Dataset").append(TAB)
		.append("ImageFile").append(TAB)
		.append("CellID").append(TAB)
		.append("NucleusID").append(TAB)
		.append("BoundingXMin").append(TAB)
		.append("BoundingXMax").append(TAB)
		.append("BoundingYMin").append(TAB)
		.append("BoundingYMax").append(TAB)
		.append("LandmarkName").append(TAB)
		.append("LandmarkX").append(TAB)
		.append("LandmarkY").append(NEWLINE);
		
	}

	@Override
	protected void append(@NonNull IAnalysisDataset d, @NonNull PrintWriter pw) throws Exception {
		for (ICell cell : d.getCollection().getCells()) {

			StringBuilder outLine = new StringBuilder();

			if (cell.hasNucleus()) {

				for (Nucleus n : cell.getNuclei()) {
					
					IPoint base = n.getBase();

					for(Landmark l : n.getLandmarks()) {
						IPoint p = n.getBorderPoint(l);

						outLine.append(d.getName() + TAB)
						.append(n.getSourceFolder() + TAB)
						.append(n.getSourceFileName() + TAB)
						.append(cell.getId() + TAB)
						.append(n.getId() + TAB)
						.append(base.getX() + TAB)
						.append(base.getX()+n.getWidth() + TAB)
						.append(base.getY() + TAB)
						.append(base.getY()+n.getHeight() + TAB)
						.append(l.getName() + TAB)
						.append(p.getX() + TAB)
						.append(p.getY()+TAB);

						outLine.append(NEWLINE);
					}
				}
				pw.write(outLine.toString());
			}
		}

	}	
}
