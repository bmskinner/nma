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
package com.bmskinner.nma.gui.dialogs.prober;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import com.bmskinner.nma.analysis.detection.Finder;
import com.bmskinner.nma.analysis.detection.Finder.DetectedObjectEvent;
import com.bmskinner.nma.analysis.detection.Finder.DetectedObjectListener;
import com.bmskinner.nma.analysis.detection.FinderDisplayType;
import com.bmskinner.nma.analysis.detection.FluorescentNucleusFinder;
import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.gui.dialogs.prober.settings.ConstructableSettingsPanel;


/**
 * An image prober for detecting nuclei
 * 
 * @author Ben Skinner
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class NucleusImageProber extends IntegratedImageProber
		implements DetectedObjectListener<ICell> {

	private static final Logger LOGGER = Logger.getLogger(NucleusImageProber.class.getName());

	private static final String DIALOG_TITLE_BAR_LBL = "Nucleus detection settings";

	private JTable detectedObjectsTable;

	/**
	 * Construct with a folder of images to probe, and the initial options
	 * 
	 * @param folder
	 * @param o
	 */
	public NucleusImageProber(final File folder, final IAnalysisOptions o) {

		try {
			this.options = o;

			optionsSettingsPanel = new ConstructableSettingsPanel(options)
					.addCopyFromOpenPanel(CellularComponent.NUCLEUS)
					.addImageChannelPanel(CellularComponent.NUCLEUS)
					.addImageProcessingPanel(CellularComponent.NUCLEUS)
					.addEdgeThresholdSwitchPanel(CellularComponent.NUCLEUS)
					.addSizePanel(CellularComponent.NUCLEUS)
					.addNucleusProfilePanel(CellularComponent.NUCLEUS)
					.build();
			optionsSettingsPanel.setEnabled(false);

			Finder<ICell> finder = new FluorescentNucleusFinder(options,
					FinderDisplayType.PREVIEW);

			imageProberPanel = new GenericImageProberPanel(folder, finder, this);

			JPanel footerPanel = createFooter();

			detectedObjectsTable = new JTable(createEmptyDetectedObjectTable());
			detectedObjectsTable.setDefaultRenderer(Object.class, new ObjectTableRenderer());

			JScrollPane scrollPane = new JScrollPane(detectedObjectsTable);
			scrollPane.setColumnHeaderView(detectedObjectsTable.getTableHeader());

			JPanel westPanel = new JPanel();
			westPanel.setLayout(new BoxLayout(westPanel, BoxLayout.Y_AXIS));

			JPanel tablePanel = new JPanel(new BorderLayout());
			tablePanel.setBorder(BorderFactory.createTitledBorder("Detected objects"));
			tablePanel.add(scrollPane, BorderLayout.CENTER);

			// Ensure the table panel does not stretch wider than the options panel
			tablePanel.setPreferredSize(
					new Dimension(optionsSettingsPanel.getPreferredSize().width, 200));

			westPanel.add(optionsSettingsPanel);
			westPanel.add(tablePanel);

			this.add(westPanel, BorderLayout.WEST);

			this.add(imageProberPanel, BorderLayout.CENTER);
			this.add(footerPanel, BorderLayout.SOUTH);

			this.setTitle(DIALOG_TITLE_BAR_LBL);

			// Options will listen for image updating, and blank out
			optionsSettingsPanel.addProberReloadEventListener(imageProberPanel);

			// Prober will run when an update is heard from the options
			imageProberPanel.addPanelUpdatingEventListener(optionsSettingsPanel);

			// Detected objects from previews are returned in event objects. Add size and
			// circularity to a table
			finder.addDetectedObjectEventListener(this);

		} catch (Exception e) {
			LOGGER.warning("Error launching analysis window");
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			this.dispose();
		}

		this.pack();
		this.setModal(true);
		this.setLocationRelativeTo(null);
		this.centerOnScreen();
		this.setVisible(true);
	}

	private TableModel createEmptyDetectedObjectTable() {
		DefaultTableModel model = new DefaultTableModel();
		model.addColumn("Area");
		model.addColumn("Circularity");
		return model;

	}

	public IAnalysisOptions getOptions() {
		return options;
	}

	@Override
	protected void okButtonClicked() {
		// no other action here
	}

	@Override
	public void detectedObjectEventReceived(DetectedObjectEvent<ICell> e) {
		TableModel model = new ObjectTableModel(e.getValidObjects(), e.getInvalidObjects());
		detectedObjectsTable.setModel(model);
	}

	/**
	 * Table model for valid and invalid cells. Shows only area and circularity
	 * measurements
	 * 
	 * @author Ben Skinner
	 *
	 */
	private class ObjectTableModel extends AbstractTableModel {

		private ICell[] cells;
		private boolean[] isValid;

		public ObjectTableModel(Collection<ICell> valid, Collection<ICell> invalid) {
			cells = new ICell[valid.size() + invalid.size()];
			isValid = new boolean[valid.size() + invalid.size()];
			int i = 0;
			Iterator<ICell> vi = valid.iterator();
			while (vi.hasNext()) {
				cells[i] = vi.next();
				isValid[i++] = true;

			}
			Iterator<ICell> ivi = invalid.iterator();
			while (ivi.hasNext()) {
				cells[i] = ivi.next();
				isValid[i++] = false;
			}
		}

		public boolean isValid(int rowIndex) {
			return isValid[rowIndex];
		}

		@Override
		public String getColumnName(int column) {
			if (column == 0)
				return "Area";
			return "Circularity";
		}

		@Override
		public int getRowCount() {
			return cells.length;
		}

		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			Nucleus n = cells[rowIndex].getPrimaryNucleus();

			try {

				if (columnIndex == 0)
					return n.getMeasurement(Measurement.AREA);
				return n.getMeasurement(Measurement.CIRCULARITY);

			} catch (MissingDataException | ComponentCreationException | SegmentUpdateException e) {
				return Double.NaN;
			}
		}

	}

	/**
	 * Display for valid and invalid cells table.
	 * 
	 * @author Ben Skinner
	 *
	 */
	private class ObjectTableRenderer extends DefaultTableCellRenderer {

		protected static final String DEFAULT_DECIMAL_FORMAT = "#0.00";
		protected static final DecimalFormat DF = new DecimalFormat(DEFAULT_DECIMAL_FORMAT);

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value,
				boolean isSelected, boolean hasFocus, int row, int column) {

			JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected,
					hasFocus, row, column);

			if (value instanceof Double d) {
				l.setText(DF.format(d));
			}

			boolean isValid = ((ObjectTableModel) table.getModel()).isValid(row);
			if (isValid) {
				l.setForeground(Color.BLACK);
			} else {
				l.setForeground(Color.RED);
			}
			return l;
		}
	}

}
