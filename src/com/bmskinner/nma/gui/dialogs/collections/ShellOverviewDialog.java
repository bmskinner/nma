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
package com.bmskinner.nma.gui.dialogs.collections;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import com.bmskinner.nma.analysis.signals.shells.ShellAnalysisMethod.ShellAnalysisException;
import com.bmskinner.nma.analysis.signals.shells.ShellDetector;
import com.bmskinner.nma.analysis.signals.shells.ShellDetector.Shell;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.signals.IShellResult.ShrinkType;
import com.bmskinner.nma.components.signals.ISignalCollection;
import com.bmskinner.nma.gui.components.SelectableCellIcon;
import com.bmskinner.nma.io.ImageImportWorker;
import com.bmskinner.nma.io.ImageImporter;
import com.bmskinner.nma.io.Io;

import com.bmskinner.nma.visualisation.image.ImageFilterer;
import com.bmskinner.nma.visualisation.image.ImageAnnotator;

import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.ImageProcessor;

/**
 * Display all nuclei in the collection with signals and shell overlays
 * 
 * @author Ben Skinner
 * @since 1.13.7
 *
 */
public class ShellOverviewDialog extends AbstractCellCollectionDialog {

	private static final Logger LOGGER = Logger.getLogger(ShellOverviewDialog.class.getName());

	private static final String HEADER_LBL = "Double click a nucleus to export image to ";

	public ShellOverviewDialog(IAnalysisDataset dataset) {
		super(dataset);
	}

	@Override
	protected void createWorker() {
		worker = new ShellAnnotationWorker(dataset, table.getModel(), false);
		worker.addPropertyChangeListener(this);
		worker.execute();
	}

	@Override
	protected JPanel createHeader() {
		JPanel header = new JPanel(new FlowLayout());

		String folderPath = "";
		Optional<IAnalysisOptions> analOpt = dataset.getAnalysisOptions();
		if (analOpt.isPresent()) {
			folderPath = analOpt.get().getNucleusDetectionFolder().get().getAbsolutePath();
		}

		header.add(new JLabel(HEADER_LBL + folderPath));
		return header;

	}

	@Override
	protected void createUI() {

		this.setLayout(new BorderLayout());
		this.setTitle(
				"Showing " + dataset.getCollection().size() + " cells in " + dataset.getName());

		progressBar = new JProgressBar();
		progressBar.setString(LOADING_LBL);
		progressBar.setStringPainted(true);

		JPanel header = createHeader();
		getContentPane().add(header, BorderLayout.NORTH);
		getContentPane().add(progressBar, BorderLayout.SOUTH);

		model = new CellCollectionModel(dataset);

		createTable();

		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					Point pnt = e.getPoint();
					int row = table.rowAtPoint(pnt);
					int col = table.columnAtPoint(pnt);
					export(model.getCell(row, col));
				}
			}
		});

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(table);

		getContentPane().add(scrollPane, BorderLayout.CENTER);

	}

	private void export(ICell cell) {
		ImageProcessor full = renderFullImage(cell);

		File folder = dataset.getSavePath().getParentFile();
		File outputfile = new File(folder,
				cell.getPrimaryNucleus().getNameAndNumber() + Io.TIFF_FILE_EXTENSION);

		FileSaver saver = new FileSaver(new ImagePlus("", full));
		saver.saveAsTiff(outputfile.getAbsolutePath());
	}

	private ImageProcessor renderFullImage(ICell c) {
		ImageProcessor ip;

		if (c.hasCytoplasm()) {
			ip = ImageImporter.importFullImageTo24bitGreyscale(c.getCytoplasm());
		} else {
			ip = ImageImporter.importFullImageTo24bitGreyscale(c.getPrimaryNucleus());
		}

		if (!dataset.getCollection().getSignalManager().hasShellResult())
			return ip;

		int shellCount = dataset.getCollection().getSignalManager().getShellCount();
		if (shellCount == 0)
			LOGGER.fine("No shells present, cannot draw");

		ShrinkType t = dataset.getCollection().getSignalManager().getShrinkType().get();

		ImageAnnotator an = new ImageAnnotator(ip);

		for (Nucleus n : c.getNuclei()) {

			try {

				List<Shell> shells = new ShellDetector(n, shellCount, t).getShells();

				for (Shell shell : shells) {
					LOGGER.finest("Drawing shell at " + shell.getBase().toString());
					an = an.annotate(shell, Color.ORANGE);
				}
			} catch (ShellAnalysisException e1) {
				LOGGER.warning("Error making shells");
				LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
			}

			ISignalCollection signalCollection = n.getSignalCollection();
			for (UUID id : signalCollection.getSignalGroupIds()) {

				Optional<Color> col = dataset.getCollection().getSignalGroup(id).get()
						.getGroupColour();
				an = an.annotateSignal(n, id, col.orElse(Color.YELLOW));
			}
		}

		ip = an.crop(c).toProcessor();

		return ip;
	}

	public class ShellAnnotationWorker extends ImageImportWorker {

		public ShellAnnotationWorker(IAnalysisDataset dataset, TableModel model, boolean rotate) {
			super(dataset, model, rotate);
		}

		@Override
		protected SelectableCellIcon importCellImage(ICell c) {
			ImageProcessor ip = renderFullImage(c);

			if (rotate) {
				try {
					ip = rotateToVertical(c, ip);
				} catch (MissingLandmarkException e) {
					LOGGER.log(Level.SEVERE, "Unable to rotate", e);
				}
				ip.flipVertical(); // Y axis needs inverting
			}
			// Rescale the resulting image
			ip = new ImageFilterer(ip).resizeKeepingAspect(150, 150).toProcessor();

			return new SelectableCellIcon(ip, c);
		}
	}

}
