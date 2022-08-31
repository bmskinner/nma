package com.bmskinner.nma.gui.actions;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
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
import com.bmskinner.nma.components.options.OptionsBuilder;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.dialogs.SubAnalysisSetupDialog;
import com.bmskinner.nma.io.CellImageExportMethod;

/**
 * Export each nucleus to a separate image
 * 
 * @author ben
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

		IAnalysisMethod m = new CellImageSetupDialog(datasets).getMethod();

		int nNuclei = datasets.stream().mapToInt(d -> d.getCollection().getNucleusCount()).sum();

		worker = new DefaultAnalysisWorker(m, nNuclei);
		worker.addPropertyChangeListener(this);
		ThreadManager.getInstance().submit(worker);
	}

	@SuppressWarnings("serial")
	private class CellImageSetupDialog extends SubAnalysisSetupDialog {

		private static final String DIALOG_TITLE = "Cell image export options";

		HashOptions o = new OptionsBuilder().build();

		public CellImageSetupDialog(final @NonNull List<IAnalysisDataset> datasets) {
			this(datasets, DIALOG_TITLE);
		}

		/**
		 * Constructor that does not make panel visible
		 * 
		 * @param dataset the dataset
		 * @param title
		 */
		protected CellImageSetupDialog(final @NonNull List<IAnalysisDataset> datasets,
				final String title) {
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
		protected void createUI() {

			getContentPane().add(createHeader(), BorderLayout.NORTH);
			getContentPane().add(createFooter(), BorderLayout.SOUTH);

			JPanel optionsPanel = new JPanel();
			GridBagLayout layout = new GridBagLayout();
			optionsPanel.setLayout(layout);

			List<JLabel> labels = new ArrayList<>();
			List<Component> fields = new ArrayList<>();

			JCheckBox maskBox = new JCheckBox();
			maskBox.setSelected(o.getBoolean(CellImageExportMethod.MASK_BACKGROUND_KEY));
			maskBox.addActionListener(
					e -> o.setBoolean(CellImageExportMethod.MASK_BACKGROUND_KEY,
							maskBox.isSelected()));

			labels.add(new JLabel("Mask background"));
			fields.add(maskBox);

			JCheckBox rgbBox = new JCheckBox();
			rgbBox.setSelected(
					o.getBoolean(CellImageExportMethod.SINGLE_CELL_IMAGE_IS_RGB_KEY));
			rgbBox.addActionListener(
					e -> o.setBoolean(CellImageExportMethod.SINGLE_CELL_IMAGE_IS_RGB_KEY,
							rgbBox.isSelected()));
			labels.add(new JLabel("Export all channels"));
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

			labels.add(new JLabel("Fixed size image"));
			fields.add(sizeBox);

			labels.add(new JLabel("Image size (pixels)"));
			fields.add(sizeSelector);

			this.addLabelTextRows(labels, fields, layout, optionsPanel);

			getContentPane().add(optionsPanel, BorderLayout.CENTER);
		}

		@Override
		protected void setDefaults() {
			o.setBoolean(CellImageExportMethod.MASK_BACKGROUND_KEY,
					CellImageExportMethod.MASK_BACKGROUND_DEFAULT);
			o.setBoolean(CellImageExportMethod.SINGLE_CELL_IMAGE_IS_RGB_KEY,
					CellImageExportMethod.SINGLE_CELL_IMAGE_IS_RGB_DEFAULT);
			o.setBoolean(CellImageExportMethod.SINGLE_CELL_IMAGE_IS_NORMALISE_WIDTH_KEY,
					CellImageExportMethod.SINGLE_CELL_IMAGE_IS_NORMALISE_WIDTH_DEFAULT);
			o.setInt(CellImageExportMethod.SINGLE_CELL_IMAGE_WIDTH_KEY,
					CellImageExportMethod.SINGLE_CELL_IMAGE_WIDTH_DEFAULT);
		}

		@Override
		public HashOptions getOptions() {
			return o;
		}
	}

}
