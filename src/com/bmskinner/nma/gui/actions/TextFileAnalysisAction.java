package com.bmskinner.nma.gui.actions;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.analysis.AnalysisMethodException;
import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.detection.TextFileDetectionMethod;
import com.bmskinner.nma.analysis.detection.TextFileNucleusFinder;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.DefaultAnalysisOptions;
import com.bmskinner.nma.components.options.DefaultOptions;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.options.OptionsFactory;
import com.bmskinner.nma.components.profiles.DefaultLandmark;
import com.bmskinner.nma.components.profiles.Landmark;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.DefaultInputSupplier;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.dialogs.SettingsDialog;
import com.bmskinner.nma.gui.dialogs.prober.settings.ImageChannelSettingsPanel;
import com.bmskinner.nma.gui.dialogs.prober.settings.NucleusProfileSettingsPanel;
import com.bmskinner.nma.gui.dialogs.prober.settings.SettingsPanel;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.gui.events.UserActionEvent;

/**
 * Analysis of a new dataset from text files (e.g. YOLO segmentation model). See
 * the {@link TextFileDetectionMethod} for details on the expected file format
 * 
 * @author Ben Skinner
 *
 */
public class TextFileAnalysisAction extends VoidResultAction {
	private static final Logger LOGGER = Logger.getLogger(TextFileAnalysisAction.class.getName());

	private File folder = null;

	public static final int NEW_ANALYSIS = 0;

	private static final @NonNull String PROGRESS_BAR_LABEL = "Nucleus detection";

	/**
	 * Create a new analysis. The folder of text files to analyse will be requested
	 * by a dialog.
	 * 
	 * @param mw the main window to which a progress bar will be attached
	 */
	public TextFileAnalysisAction(@NonNull ProgressBarAcceptor acceptor) {
		this(acceptor, null);
	}

	/**
	 * Create a new analysis, specifying the initial directory of images
	 * 
	 * @param acceptor     the main window to which a progress bar will be attached
	 * @param folder the folder of text files to analyse
	 */
	public TextFileAnalysisAction(@NonNull ProgressBarAcceptor acceptor, @Nullable final File folder) {
		super(PROGRESS_BAR_LABEL, acceptor);
		this.folder = folder;
	}

	@Override
	public void run() {

		TextFileAnalysisOptionsDialog dialog = new TextFileAnalysisOptionsDialog();

		if(dialog.isReadyToRun()) {

			IAnalysisOptions options = dialog.getOptions();

			this.setProgressBarIndeterminate();

			LOGGER.fine("Creating for " + folder.getAbsolutePath());

			// Check the folder is present
			Optional<File> detectionFolder = options.getDetectionFolder(CellularComponent.NUCLEUS);
			if (!detectionFolder.isPresent()) {
				cancel();
				return;
			}

			LOGGER.info("Detecting objects in '%s'".formatted(detectionFolder.get().getName()));

			Instant inst = Instant.ofEpochMilli(options.getAnalysisTime());
			LocalDateTime anTime = LocalDateTime.ofInstant(inst, ZoneId.systemDefault());

			File outputFolder = new File(detectionFolder.get(), anTime
					.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")));

			try {
				IAnalysisMethod m = new TextFileDetectionMethod(outputFolder, options);
				worker = new DefaultAnalysisWorker(m);
				worker.addPropertyChangeListener(this);
				ThreadManager.getInstance().submit(worker);
			} catch (AnalysisMethodException e) {
				LOGGER.info("Unable to run detection: " + e.getMessage());
			}
		} else {
			this.cancel();
		}
	}

	@Override
	public void finished() {

		try {
			IAnalysisResult r = worker.get();
			List<IAnalysisDataset> datasets = r.getDatasets();
			IAnalysisDataset d = r.getFirstDataset();

			if (datasets == null || datasets.isEmpty()) {
				LOGGER.info("No datasets returned");
			} else {

				// A landmark file may have been specified. If so, use it. 
				// Otherwise do conventional profiling via rulesets
				if(d.getAnalysisOptions().get()
						.getNucleusDetectionOptions().get()
						.has(HashOptions.LANDMARK_LOCATION_FILE_KEY)) {
					UserActionController.getInstance().userActionEventReceived(
							new UserActionEvent(this, UserActionEvent.TEXT_PROFILING_AND_LANDMARKING,
									datasets));
					
				} else {
					UserActionController.getInstance().userActionEventReceived(
							new UserActionEvent(this, UserActionEvent.MORPHOLOGY_ANALYSIS_ACTION,
									datasets));

				}
			}

		} catch (InterruptedException e) {
			LOGGER.log(Level.SEVERE, "Interruption to analysis worker: %s".formatted(e.getMessage()), e);
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) {
			LOGGER.log(Level.SEVERE, "Error in analysis worker: %s".formatted(e.getMessage()), e);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Unable to run landmark detection: %s".formatted(e.getMessage()), e);
		} finally {
			super.finished();
		}
	}

	/**
	 * Configure the  analysis
	 * 
	 * @author Ben Skinner
	 * @since 2.3.0
	 *
	 */
	private class TextFileAnalysisOptionsDialog extends SettingsDialog {

		private IAnalysisOptions options = new DefaultAnalysisOptions();

		private static final String TITLE = "Analysis setup";
		private static final String PROFILE_LBL = "Profiles";
		private static final String IMAGE_LBL = "Image";
		private static final String RP_LANDMARK_LBL = "Reference landmark";
		
		private static final String IMAGE_COLUMN_INDEX_LBL = "Image column index";
		private static final String Y_COLUMN_INDEX_LBL = "Y column  index";
		private static final String X_COLUMN_INDEX_LBL = "X column index";
		private static final String NUCLEUS_COLUMN_INDEX_LBL = "Nucleus column index";

		public TextFileAnalysisOptionsDialog() {
			this.setTitle(TITLE);
			this.setModal(true);

			JPanel contentPanel = new JPanel();
			contentPanel.setLayout(new BorderLayout());
			contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
			setContentPane(contentPanel);
			setDefaults();
			createUI();
			this.pack();
			this.setLocationRelativeTo(null);
			this.setVisible(true);
		}

		public IAnalysisOptions getOptions() {
			
			// Make sure we have consistency in landmark options
			// Since the RP selection is added by default, we should check if we are really using a landmark file.
			
			if(!options.getNucleusDetectionOptions().get().has(HashOptions.LANDMARK_LOCATION_FILE_KEY)) {
				options.getNucleusDetectionOptions().get().remove(HashOptions.LANDMARK_RP_NAME);
			}
			
			return options;
		}

		private void createUI() {
			JPanel panel = new JPanel();
			BoxLayout layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
			panel.setLayout(layout);
			
			// Folder with outline files
			String outlineFileName = folder==null ? "Click to choose folder" : folder.getAbsolutePath();
			JTextField  outlineFileBox = new JTextField(outlineFileName);
			outlineFileBox.setEditable(false);
			outlineFileBox.addMouseListener(new MouseAdapter() {

				@Override
				public void mouseClicked(MouseEvent e) {
					// Image directory may have been specified; if not, request from user

					try {
						folder = new DefaultInputSupplier().requestFolder("Select outline directory", 
								GlobalOptions.getInstance().getDefaultDir());
						options.setDetectionFolder(CellularComponent.NUCLEUS, folder);
						outlineFileBox.setText(folder.getAbsolutePath());
					} catch (RequestCancelledException ex) {
						// user cancelled
					}
				}
			});

			
			// Folder with landmark files
			JTextField  landmarkFileBox = new JTextField("Click to choose file");
			landmarkFileBox.setEditable(false);
			landmarkFileBox.addMouseListener(new MouseAdapter() {

				@Override
				public void mouseClicked(MouseEvent e) {
					// Image directory may have been specified; if not, request from user

					try {
						File landmarkFile = new DefaultInputSupplier().requestFile("Select landmark directory", 
								GlobalOptions.getInstance().getDefaultDir(), "tsv", "Tab separated values");
						options.getNucleusDetectionOptions().get().setFile(HashOptions.LANDMARK_LOCATION_FILE_KEY, landmarkFile);
						landmarkFileBox.setText(landmarkFile.getAbsolutePath());
					} catch (RequestCancelledException ex) {
						// user cancelled
					}
				}
			});
			
			JSpinner imageColSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
			imageColSpinner.addChangeListener(e -> {
				try {
					JSpinner j = (JSpinner) e.getSource();
					j.commitEdit();
					options.getNucleusDetectionOptions().get().setInt(TextFileNucleusFinder.IMAGE_FILE_COL, (Integer) j.getValue());

				} catch (ParseException e1) {
					LOGGER.log(Level.SEVERE, "Parsing error in image column index selector: %s".formatted(e1.getMessage()), e1);
				}
			});
			
			
			JSpinner nucleusColSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 100, 1));
			nucleusColSpinner.addChangeListener(e -> {
				try {
					JSpinner j = (JSpinner) e.getSource();
					j.commitEdit();
					options.getNucleusDetectionOptions().get().setInt(TextFileNucleusFinder.NUCLEUS_ID_COL, (Integer) j.getValue());

				} catch (ParseException e1) {
					LOGGER.log(Level.SEVERE, "Parsing error in nucleus id column index selector: %s".formatted(e1.getMessage()), e1);
				}
			});
			
			JSpinner xColSpinner = new JSpinner(new SpinnerNumberModel(2, 0, 100, 1));
			xColSpinner.addChangeListener(e -> {
				try {
					JSpinner j = (JSpinner) e.getSource();
					j.commitEdit();
					options.getNucleusDetectionOptions().get().setInt(TextFileNucleusFinder.X_COORDINATE_COL, (Integer) j.getValue());

				} catch (ParseException e1) {
					LOGGER.log(Level.SEVERE, "Parsing error in x column index selector: %s".formatted(e1.getMessage()), e1);
				}
			});
			
			JSpinner yColSpinner = new JSpinner(new SpinnerNumberModel(3, 0, 100, 1));
			yColSpinner.addChangeListener(e -> {
				try {
					JSpinner j = (JSpinner) e.getSource();
					j.commitEdit();
					options.getNucleusDetectionOptions().get().setInt(TextFileNucleusFinder.Y_COORDINATE_COL, (Integer) j.getValue());

				} catch (ParseException e1) {
					LOGGER.log(Level.SEVERE, "Parsing error in y column index selector: %s".formatted(e1.getMessage()), e1);
				}
			});

			// Image channel
	        SettingsPanel imagePanel = new ImageChannelSettingsPanel(options.getNucleusDetectionOptions().get());
	        imagePanel.setBorder(BorderFactory.createTitledBorder(IMAGE_LBL));
			
	        // Create the profile panel with rulesets. This also sets the default ruleset in the options
			SettingsPanel profilePanel = new NucleusProfileSettingsPanel(options);
			profilePanel.setBorder(BorderFactory.createTitledBorder(PROFILE_LBL));

			// Which landmark is the RP? Set defaults
			JComboBox<Landmark> rpLandmarkBox = new JComboBox<Landmark>(options.getRuleSetCollection().getLandmarks().toArray(new DefaultLandmark[0]));
			options.getNucleusDetectionOptions().get().setString(HashOptions.LANDMARK_RP_NAME, rpLandmarkBox.getSelectedItem().toString());
			
			rpLandmarkBox.addActionListener(e->{
				options.getNucleusDetectionOptions().get().setString(HashOptions.LANDMARK_RP_NAME, rpLandmarkBox.getSelectedItem().toString());
			});
			
			// Profile settings
			profilePanel.addOptionsChangeListener(l->{
				rpLandmarkBox.setModel(new DefaultComboBoxModel<Landmark>(options.getRuleSetCollection().getLandmarks().toArray(new DefaultLandmark[0])));
				options.getNucleusDetectionOptions().get().setString(HashOptions.LANDMARK_RP_NAME, rpLandmarkBox.getSelectedItem().toString());
			});


			JPanel spinnerPanel = new JPanel();
			spinnerPanel.setLayout(new BoxLayout(spinnerPanel, BoxLayout.Y_AXIS));
			spinnerPanel.setBorder(BorderFactory.createTitledBorder("Nucleus outlines"));
			spinnerPanel.add(outlineFileBox);
			spinnerPanel.add(new JLabel(IMAGE_COLUMN_INDEX_LBL));
			spinnerPanel.add(imageColSpinner);
			spinnerPanel.add(new JLabel(NUCLEUS_COLUMN_INDEX_LBL));
			spinnerPanel.add(nucleusColSpinner);
			spinnerPanel.add(new JLabel(X_COLUMN_INDEX_LBL));
			spinnerPanel.add(xColSpinner);
			spinnerPanel.add(new JLabel(Y_COLUMN_INDEX_LBL));
			spinnerPanel.add(yColSpinner);
			
			panel.add(spinnerPanel);
			
			panel.add(imagePanel);
			panel.add(profilePanel);
			
			JPanel landmarkPanel = new JPanel();
			landmarkPanel.setLayout(new BoxLayout(landmarkPanel, BoxLayout.Y_AXIS));
			landmarkPanel.setBorder(BorderFactory.createTitledBorder("Landmarks"));

			landmarkPanel.add(landmarkFileBox);
			landmarkPanel.add(new JLabel(RP_LANDMARK_LBL));
			landmarkPanel.add(rpLandmarkBox);
			panel.add(landmarkPanel);
						
			add(panel, BorderLayout.CENTER);
			add(createFooter(), BorderLayout.SOUTH);
		}

		private void setDefaults() {

			options = OptionsFactory.makeAnalysisOptions(RuleSetCollection.roundRuleSetCollection());

			options.setAngleWindowProportion(HashOptions.DEFAULT_PROFILE_WINDOW);
			options.getProfilingOptions().setBoolean(HashOptions.IS_SEGMENT_PROFILES, false);
			options.setDetectionFolder(CellularComponent.NUCLEUS, folder);


			HashOptions nucleusOptions = new DefaultOptions();
			
			nucleusOptions.setInt(HashOptions.CHANNEL, 2);
			nucleusOptions.setDouble(HashOptions.SCALE, 1);
			
			options.setDetectionOptions(CellularComponent.NUCLEUS, nucleusOptions);
		}

	}

}

