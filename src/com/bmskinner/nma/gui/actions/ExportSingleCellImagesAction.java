package com.bmskinner.nma.gui.actions;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.MissingOptionException;
import com.bmskinner.nma.components.options.OptionsBuilder;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.dialogs.SubAnalysisSetupDialog;
import com.bmskinner.nma.io.CellImageExportMethod;

/**
 * Export each nucleus to a separate image
 * 
 * @author Ben Skinner
 *
 */
public class ExportSingleCellImagesAction extends MultiDatasetResultAction {

	private static final Logger LOGGER = Logger
			.getLogger(ExportSingleCellImagesAction.class.getName());

	private static final String PROGRESS_LBL = "Exporting single cells";

	public ExportSingleCellImagesAction(@NonNull List<IAnalysisDataset> datasets,
			@NonNull final ProgressBarAcceptor acceptor) {
		super(datasets, PROGRESS_LBL, acceptor);
	}

	@Override
	public void run() {
		setProgressBarIndeterminate();

		try {

			SubAnalysisSetupDialog dialog = new CellImageSetupDialog(datasets);

			if (dialog.isReadyToRun()) {
				IAnalysisMethod m = dialog.getMethod();

				int nNuclei = datasets.stream().mapToInt(d -> d.getCollection().getNucleusCount())
						.sum();

				worker = new DefaultAnalysisWorker(m, nNuclei);
				worker.addPropertyChangeListener(this);
				ThreadManager.getInstance().submit(worker);
			} else {
				// User cancelled
				this.cancel();
			}
		} catch (MissingOptionException e) {
			LOGGER.log(Level.SEVERE, "Cannot create UI:" + e.getMessage(), e);
			this.cancel();
		}

	}

	@SuppressWarnings("serial")
	private class CellImageSetupDialog extends SubAnalysisSetupDialog {

		private static final String DIALOG_TITLE = "Cell image export options";

		HashOptions o = new OptionsBuilder().build();

		public CellImageSetupDialog(final @NonNull List<IAnalysisDataset> datasets)
				throws MissingOptionException {
			this(datasets, DIALOG_TITLE);
		}

		/**
		 * Construct from datasets
		 * 
		 * @param datasets the datasets
		 * @param title    the dialog title
		 * @throws MissingOptionException
		 */
		protected CellImageSetupDialog(final @NonNull List<IAnalysisDataset> datasets,
				final String title) throws MissingOptionException {
			super(datasets, title);
			setDefaults();
			createUI();
			packAndDisplay();
		}

		@Override
		public IAnalysisMethod getMethod() {
			return new CellImageExportMethod(datasets, o);
		}

		@Override
		protected void createUI() throws MissingOptionException {

			getContentPane().add(createHeader(), BorderLayout.NORTH);
			getContentPane().add(createFooter(), BorderLayout.SOUTH);

			JPanel optionsPanel = new JPanel();
			GridBagLayout layout = new GridBagLayout();
			optionsPanel.setLayout(layout);

			List<JLabel> labels = new ArrayList<>();
			List<Component> fields = new ArrayList<>();

			JCheckBox maskBox = new JCheckBox();
			maskBox.setSelected(o.get(CellImageExportMethod.MASK_BACKGROUND_KEY));
			maskBox.addActionListener(
					e -> o.setBoolean(CellImageExportMethod.MASK_BACKGROUND_KEY,
							maskBox.isSelected()));

			labels.add(new JLabel("Mask pixels outside nucleus as black"));
			fields.add(maskBox);

			JCheckBox foreMaskBox = new JCheckBox();
			foreMaskBox.setSelected(o.get(CellImageExportMethod.MASK_FOREGROUND_KEY));
			foreMaskBox.addActionListener(
					e -> o.setBoolean(CellImageExportMethod.MASK_FOREGROUND_KEY,
							foreMaskBox.isSelected()));

			labels.add(new JLabel("Mask pixels inside nucleus as white"));
			fields.add(foreMaskBox);

			JCheckBox rgbBox = new JCheckBox();
			rgbBox.setSelected(
					o.get(CellImageExportMethod.SINGLE_CELL_IMAGE_IS_RGB_KEY));
			rgbBox.addActionListener(
					e -> o.setBoolean(CellImageExportMethod.SINGLE_CELL_IMAGE_IS_RGB_KEY,
							rgbBox.isSelected()));
			labels.add(new JLabel("Export all colour channels"));
			fields.add(rgbBox);

			// Set normalised image size
			JSpinner sizeSelector = new JSpinner(
					new SpinnerNumberModel(CellImageExportMethod.SINGLE_CELL_IMAGE_WIDTH_DEFAULT,
							50, 10000, 1));
			sizeSelector.setEnabled(
					o.getBoolean(CellImageExportMethod.SINGLE_CELL_IMAGE_IS_NORMALISE_WIDTH_KEY));
			sizeSelector.addChangeListener(e -> {
				try {
					sizeSelector.commitEdit();
					o.setInt(CellImageExportMethod.SINGLE_CELL_IMAGE_WIDTH_KEY,
							(int) sizeSelector.getValue());
				} catch (ParseException e1) {
					LOGGER.warning("Error setting size value: " + e1.getMessage());
				}
			});

			// Should image size be normalised
			JCheckBox sizeBox = new JCheckBox();
			sizeBox.setSelected(o.getBoolean(
					CellImageExportMethod.SINGLE_CELL_IMAGE_IS_NORMALISE_WIDTH_KEY));

			sizeBox.addActionListener(e -> {
				o.setBoolean(
						CellImageExportMethod.SINGLE_CELL_IMAGE_IS_NORMALISE_WIDTH_KEY,
						sizeBox.isSelected());
				sizeSelector.setEnabled(sizeBox.isSelected());
				if (sizeBox.isSelected()) {
					o.setInt(CellImageExportMethod.SINGLE_CELL_IMAGE_WIDTH_KEY,
							(int) sizeSelector.getValue());
				}
			});

			JCheckBox keypointBox = new JCheckBox();
			keypointBox.setSelected(o.getBoolean(
					CellImageExportMethod.SINGLE_CELL_IMAGE_IS_EXPORT_KEYPOINTS_KEY));
			keypointBox.addActionListener(
					e -> o.setBoolean(
							CellImageExportMethod.SINGLE_CELL_IMAGE_IS_EXPORT_KEYPOINTS_KEY,
							keypointBox.isSelected()));

			labels.add(new JLabel("Scale output image to a fixed (square) size"));
			fields.add(sizeBox);

			labels.add(new JLabel("Image width & height (pixels)"));
			fields.add(sizeSelector);

			labels.add(new JLabel("Export keypoints"));
			fields.add(keypointBox);

			this.addLabelTextRows(labels, fields, layout, optionsPanel);

			getContentPane().add(optionsPanel, BorderLayout.CENTER);
		}

		@Override
		protected void setDefaults() {
			o.setBoolean(CellImageExportMethod.MASK_BACKGROUND_KEY,
					CellImageExportMethod.MASK_BACKGROUND_DEFAULT);
			o.setBoolean(CellImageExportMethod.MASK_FOREGROUND_KEY,
					CellImageExportMethod.MASK_FOREGROUND_DEFAULT);
			o.setBoolean(CellImageExportMethod.SINGLE_CELL_IMAGE_IS_RGB_KEY,
					CellImageExportMethod.SINGLE_CELL_IMAGE_IS_RGB_DEFAULT);
			o.setBoolean(CellImageExportMethod.SINGLE_CELL_IMAGE_IS_NORMALISE_WIDTH_KEY,
					CellImageExportMethod.SINGLE_CELL_IMAGE_IS_NORMALISE_WIDTH_DEFAULT);
			o.setInt(CellImageExportMethod.SINGLE_CELL_IMAGE_WIDTH_KEY,
					CellImageExportMethod.SINGLE_CELL_IMAGE_WIDTH_DEFAULT);
			o.setBoolean(CellImageExportMethod.SINGLE_CELL_IMAGE_IS_EXPORT_KEYPOINTS_KEY,
					CellImageExportMethod.SINGLE_CELL_IMAGE_IS_EXPORT_KEYPOINTS_DEFAULT);
		}

		@Override
		public HashOptions getOptions() {
			return o;
		}
	}

}
