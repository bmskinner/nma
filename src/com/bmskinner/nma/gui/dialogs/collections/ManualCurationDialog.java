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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.MissingOptionException;
import com.bmskinner.nma.components.signals.ISignalGroup;
import com.bmskinner.nma.gui.components.SelectableCellIcon;
import com.bmskinner.nma.gui.events.UIController;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.gui.events.UserActionEvent;
import com.bmskinner.nma.io.ImageImportWorker;
import com.bmskinner.nma.io.ImageImporter;
import com.bmskinner.nma.io.ImageImporter.ImageImportException;
import com.bmskinner.nma.io.UnloadableImageException;
import com.bmskinner.nma.visualisation.image.ImageAnnotator;
import com.bmskinner.nma.visualisation.image.ImageFilterer;

import ij.process.ImageProcessor;

/**
 * This displays all the nuclei in the given dataset, annotated to show nuclei.
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class ManualCurationDialog extends AbstractCellCollectionDialog {

	private static final Logger LOGGER = Logger.getLogger(ManualCurationDialog.class.getName());

	private static final String MAKE_NEW_COLLECTION_LBL = "Make new collection from selected";
	private static final String SELECT_ALL_LBL = "Select all";
	private static final String ROTATE_VERTICAL_LBL = "Rotate vertical";
	private static final String NUCLEAR_SIGNAL_LBL = "Nuclear signal: ";
	public static final int ROW_IMAGE_HEIGHT = 150;

	private JButton curateBtn;
	private JCheckBox selectAllChkBox;

	/**
	 * Map ids for e.g. signal groups to a display name. Used in the component
	 * selection combobox
	 * 
	 * @author bms41
	 * @since 1.15.0
	 *
	 */
	private record ComponentSelectionItem(String displayName, UUID id) {
		@Override
		public String toString() {
			return displayName;
		}
	}

	public ManualCurationDialog(IAnalysisDataset dataset) {
		super(dataset);
	}

	@Override
	protected void createWorker() {
		worker = new CellImportWorker(dataset, table.getModel(), true, CellularComponent.NUCLEUS);
		worker.removePropertyChangeListener(this); // clear any existing listeners
		worker.addPropertyChangeListener(this);
		curateBtn.setEnabled(false);
		selectAllChkBox.setEnabled(false);
		worker.execute();

		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				worker.cancel(true);
			}
		});
	}

	@Override
	protected JPanel createHeader() {
		JPanel header = new JPanel(new FlowLayout());

		curateBtn = new JButton(MAKE_NEW_COLLECTION_LBL);

		JCheckBox rotateBtn = new JCheckBox(ROTATE_VERTICAL_LBL, true);

		List<Object> components = new ArrayList<>();

		boolean hasCytoplasm = dataset.getCollection().getCells().stream()
				.anyMatch(ICell::hasCytoplasm);
		if (hasCytoplasm) // Only add cytoplasm option if present in at least one cell
			components.add(CellularComponent.CYTOPLASM);

		components.add(CellularComponent.NUCLEUS);
		for (UUID signalGroupId : dataset.getCollection().getSignalGroupIDs()) {
			ISignalGroup sg = dataset.getCollection().getSignalGroup(signalGroupId).get();
			components.add(new ComponentSelectionItem(NUCLEAR_SIGNAL_LBL + sg.getGroupName(),
					signalGroupId));
		}

		JComboBox<Object> componntBox = new JComboBox<>(components.toArray(new Object[0]));
		componntBox.setSelectedItem(CellularComponent.NUCLEUS);
		componntBox.addActionListener(e -> {
			worker.cancel(true);
			worker.removePropertyChangeListener(this);
			progressBar.setVisible(true);
			progressBar.setValue(0);
			curateBtn.setEnabled(false);
			selectAllChkBox.setEnabled(false);
			worker = new CellImportWorker(dataset, table.getModel(), rotateBtn.isSelected(),
					componntBox.getSelectedItem());
			worker.addPropertyChangeListener(this);
			worker.execute();

		});

		rotateBtn.addActionListener(e -> {
			worker.cancel(true);
			worker.removePropertyChangeListener(this);
			progressBar.setVisible(true);
			progressBar.setValue(0);
			curateBtn.setEnabled(false);
			selectAllChkBox.setEnabled(false);
			worker = new CellImportWorker(dataset, table.getModel(), rotateBtn.isSelected(),
					componntBox.getSelectedItem());
			worker.addPropertyChangeListener(this);
			worker.execute();

		});

		header.add(componntBox);
		header.add(rotateBtn);

		selectAllChkBox = new JCheckBox(SELECT_ALL_LBL);
		selectAllChkBox.addActionListener(e -> {
			model.setAllSelected(selectAllChkBox.isSelected());
			table.repaint();
		});
		header.add(selectAllChkBox);

		curateBtn.addActionListener(e -> {
			Optional<IAnalysisDataset> newDataset = model.makeNewCollectionFromSelected();
			if (newDataset.isPresent()) {
				UserActionController.getInstance().userActionEventReceived(
						new UserActionEvent(this, UserActionEvent.REFOLD_CONSENSUS,
								newDataset.get()));
				UIController.getInstance().fireDatasetAdded(newDataset.get());
			} else
				LOGGER.info("No cells added to new dataset; not creating");
		});
		header.add(curateBtn);
		return header;

	}

	@Override
	protected void createUI() {

		this.setLayout(new BorderLayout());
		this.setTitle(String.format("Showing %s cells in %s", dataset.getCollection().size(),
				dataset.getName()));

		int cellCount = dataset.getCollection().size();

		int remainder = cellCount % COLUMN_COUNT == 0 ? 0 : 1;

		int rows = cellCount / COLUMN_COUNT + remainder;

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
			public synchronized void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 1) {
					Point pnt = e.getPoint();
					int row = table.rowAtPoint(pnt);
					int col = table.columnAtPoint(pnt);
					model.toggleSelected(row, col);
					setTitle(String.format("Showing %s cells in %s: %s selected",
							dataset.getCollection().size(),
							dataset.getName(), model.selectedCount()));
					table.repaint(table.getCellRect(row, col, true)); // need to trigger otherwise
																		// it will only update
																		// on the next click
				}
			}
		});

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(table);
		getContentPane().add(scrollPane, BorderLayout.CENTER);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		super.propertyChange(evt);
		if (evt.getPropertyName().equals("Finished")) {
			curateBtn.setEnabled(true);
			selectAllChkBox.setEnabled(true);
		}

	}

	/**
	 * Import the cell images for the curator
	 * 
	 * @author ben
	 * @since 1.15.0
	 *
	 */
	public class CellImportWorker extends ImageImportWorker {

		private Object component;

		/**
		 * Constructor
		 * 
		 * @param dataset   the dataset to fetch cells from
		 * @param model     the table to display cells in
		 * @param rotate    if true, cells are rotated to vertical
		 * @param component the component of the cell to display
		 */
		public CellImportWorker(IAnalysisDataset dataset, TableModel model, boolean rotate,
				Object component) {
			super(dataset, model, rotate);
			this.component = component;
		}

		/**
		 * Scale and rotate the image as needed
		 * 
		 * @param c
		 * @param ip
		 * @return
		 */
		private ImageProcessor flipAndScaleImage(ICell c, ImageProcessor ip) {
			if (rotate) {
				ip.flipVertical(); // Y axis needs inverting
				ip = ImageFilterer.orientImage(ip, c.getPrimaryNucleus());
			}
			// Rescale the resulting image
			ip = new ImageFilterer(ip)
					.resizeKeepingAspect(ROW_IMAGE_HEIGHT, ROW_IMAGE_HEIGHT)
					.toProcessor();
			return ip;
		}

		private ImageProcessor importCytoplasm(ICell c) throws UnloadableImageException {
			if (!c.hasCytoplasm())
				return ImageFilterer.createWhiteColorProcessor(ROW_IMAGE_HEIGHT,
						ROW_IMAGE_HEIGHT);
			ImageProcessor ip = ImageImporter.importCroppedImageTo24bitGreyscale(c.getCytoplasm());
			ImageAnnotator an = new ImageAnnotator(ip);
			an = an.annotateBorder(c.getCytoplasm(), c.getCytoplasm(), Color.CYAN);
			for (Nucleus n : c.getNuclei()) {
				an.annotateBorder(n, c.getCytoplasm(), Color.ORANGE);
			}

			ip = an.toProcessor();
			return flipAndScaleImage(c, ip);
		}

		private ImageProcessor importNucleus(ICell c) throws UnloadableImageException {
			ImageProcessor ip = ImageImporter
					.importFullImageTo24bitGreyscale(c.getPrimaryNucleus());
			ImageAnnotator an = new ImageAnnotator(ip);
			for (Nucleus n : c.getNuclei()) {
				an = an.drawSegments(n);
			}

			ip = an.crop(c).toProcessor();
			return flipAndScaleImage(c, ip);
		}

		private ImageProcessor importSignal(UUID signal, ICell c)
				throws ImageImportException, MissingOptionException {

			HashOptions signalOptions = dataset.getAnalysisOptions().get()
					.getNuclearSignalOptions(signal)
					.orElseThrow(MissingOptionException::new);

			if (signalOptions == null)
				return ImageFilterer.createWhiteColorProcessor(150, 150);

			// We need to import the image in which signals are found, whether a
			// signal was detected in this nucleus of not.
			File signalFile = chooseSignalFile(signal, c);

			if (!signalFile.exists()) {
				LOGGER.fine("Could not find image: " + signalFile.getAbsolutePath());
				return ImageFilterer.createWhiteColorProcessor(150, 150);
			}

			ImageProcessor ip = ImageImporter
					.importImage(signalFile, signalOptions.getInt(HashOptions.CHANNEL));
			ip.invert();
			ImageAnnotator an = new ImageAnnotator(ip);
			an.convertToColorProcessor();
			for (Nucleus n : c.getNuclei()) {
				an = an.annotateSegments(n, n);
			}
			ip = an.crop(c).toProcessor();
			return flipAndScaleImage(c, ip);
		}

		private File chooseSignalFile(UUID signal, ICell c) {
			// Ideally, find the folder containing the signals, and get the appropriate file
			// name
			// But if the dataset is merged, we can't use the value in the signal detection
			// options,
			// so hack it for now
			if (c.getPrimaryNucleus().getSignalCollection().hasSignal(signal))
				return c.getPrimaryNucleus().getSignalCollection().getSourceFile(signal);

			// Otherwise try the nucleus image and hope it's RGB
			return c.getPrimaryNucleus().getSourceFile();
		}

		@Override
		protected SelectableCellIcon importCellImage(ICell c) {
			ImageProcessor ip = null;

			try {

				if (CellularComponent.CYTOPLASM.equals(component.toString()))
					ip = importCytoplasm(c);

				if (CellularComponent.NUCLEUS.equals(component.toString()))
					ip = importNucleus(c);

				if (component instanceof ComponentSelectionItem comp) {
					ip = importSignal(comp.id, c);
				}

				return new SelectableCellIcon(ip, c);

			} catch (UnloadableImageException | ImageImportException | MissingOptionException e) {
				LOGGER.fine("Cannot load image for component: " + e.getMessage());
				return new SelectableCellIcon(
						ImageFilterer.createBlackColorProcessor(150, 150), c);
			}

		}
	}

}
