package com.bmskinner.nma.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.jdom2.Document;

import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.components.Version;
import com.bmskinner.nma.components.XMLNames;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.actions.VoidResultAction;
import com.bmskinner.nma.gui.events.FileImportEventListener.FileImportEvent;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.io.Io.Importer;
import com.bmskinner.nma.logging.Loggable;

/**
 * Import files. This class attempts to determine the file type and dispatch the
 * file to the correct importer.
 * 
 * @author ben
 *
 */
public class GenericFileImporter extends VoidResultAction implements Importer {

	private static final Logger LOGGER = Logger.getLogger(GenericFileImporter.class.getName());

	private static final byte[] NMD_V1_SIGNATURE = new byte[] { -84, -19, 0, 5 };

	private static final @NonNull String PROGRESS_BAR_LABEL = "Reading file...";

	private static final String WORKSPACE_FILE_TYPE = "Nuclear morphology workspace";
	private static final String DATASET_FILE_TYPE = "Nuclear morphology dataset";
	private static final String OPTIONS_FILE_TYPE = "Nuclear morphology options";

	private final File file;

	private XMLImportMethod method;

	/**
	 * Construct with a file to be read
	 * 
	 * @param f the saved dataset file
	 */
	public GenericFileImporter(@Nullable File file, @NonNull final ProgressBarAcceptor acceptor,
			@Nullable CountDownLatch latch, @Nullable String fileType) {
		super(PROGRESS_BAR_LABEL, acceptor);
		setLatch(latch);
		this.file = file == null ? selectFile(fileType) : file;
	}

	@Override
	public void run() {

		if (file == null || !file.exists()) {
			super.finished();
			return;
		}

		if (isOldNmdFormat()) {
			LOGGER.info("File is from NMA version 1.x.x, and cannot be opened in this version.");
			super.finished();
			return;
		}

		// We can't import image files direct to NMA, so give a sensible message
		if (ImageImporter.isImageFile(file)) {
			LOGGER.info(
					"Image files cannot be opened directly, provide a folder of images instead.");
			super.finished();
			return;
		}

		setProgressMessage(PROGRESS_BAR_LABEL);
		setProgressBarIndeterminate();

		try {
			method = new XMLImportMethod(file);
			worker = new DefaultAnalysisWorker(method, file.length());
			worker.addPropertyChangeListener(this);
			ThreadManager.getInstance().submit(worker);
		} catch (Exception e) {
			super.finished();
		}
	}

	/**
	 * Get the file to be loaded
	 * 
	 * @return
	 */
	private File selectFile(@Nullable String fileType) {

		File defaultDir = GlobalOptions.getInstance().getDefaultDir();
		JFileChooser fc = new JFileChooser("Select a file...");
		if (defaultDir.exists()) {
			fc = new JFileChooser(defaultDir);
		}

		if (fileType != null) {

			FileNameExtensionFilter filter = switch (fileType) {
			case XMLNames.XML_WORKSPACE -> new FileNameExtensionFilter(WORKSPACE_FILE_TYPE,
					Io.WRK_FILE_EXTENSION_NODOT);

			case XMLNames.XML_ANALYSIS_DATASET -> new FileNameExtensionFilter(
					DATASET_FILE_TYPE,
					Io.NMD_FILE_EXTENSION_NODOT);

			case XMLNames.XML_ANALYSIS_OPTIONS -> new FileNameExtensionFilter(
					OPTIONS_FILE_TYPE,
					Io.XML_FILE_EXTENSION_NODOT);

			default -> new FileNameExtensionFilter(
					DATASET_FILE_TYPE,
					Io.NMD_FILE_EXTENSION_NODOT);
			};
			fc.setFileFilter(filter);
		}

		int returnVal = fc.showOpenDialog(fc);
		if (returnVal != 0)
			return null;
		File f = fc.getSelectedFile();

		if (f.isDirectory())
			return null;
		return f;
	}

	/**
	 * Check the first four bytes for the old NMD signature. This is the magic
	 * number for serialised Java classes
	 * 
	 * @return
	 */
	private boolean isOldNmdFormat() {
		try (InputStream is = new FileInputStream(file);) {
			byte[] b = is.readNBytes(4);
			return Arrays.equals(b, NMD_V1_SIGNATURE);
		} catch (IOException e) {
			LOGGER.log(Loggable.STACK, "Error reading first bytes of file", e);
			return false;
		}
	}

	/**
	 * The v1 XML is readable by the document parser, but can't be unmarshalled.
	 * Check and report the version string.
	 * 
	 * @param doc
	 * @return
	 */
	private boolean isOldXmlFormat(Document doc) {
		if (doc.getRootElement().getChild("VersionCreated") != null) {
			String vString = doc.getRootElement().getChildText("VersionCreated");
			Version v = Version.fromString(vString);
			if (!Version.versionIsSupported(v)) {
				LOGGER.info(
						() -> "File was created in NMA version %s, and cannot be opened in this version."
								.formatted(v));
				return true;
			}
		}
		return false;
	}

	@Override
	public void finished() {

		// Read the document with a counted listener
		// Dispatch the document to the appropriate loader
		Document doc = method.getXMLDocument();

		if (doc == null) {
			LOGGER.info("Could not open " + file.getName());
			super.finished();
			return;
		}

		if (isOldXmlFormat(doc)) {
			super.finished();
			return;
		}

		// Check the name of the first element
		String name = doc.getRootElement().getName();

		// Choose what to do based on the root element name
		if (XMLNames.XML_ANALYSIS_DATASET.equals(name))
			UserActionController.getInstance().fileImported(
					new FileImportEvent(this, file, XMLNames.XML_ANALYSIS_DATASET, doc));

		if (XMLNames.XML_WORKSPACE.equals(name))
			UserActionController.getInstance().fileImported(
					new FileImportEvent(this, file, XMLNames.XML_WORKSPACE, doc));

		if (XMLNames.XML_ANALYSIS_OPTIONS.equals(name))
			UserActionController.getInstance().fileImported(
					new FileImportEvent(this, file, XMLNames.XML_ANALYSIS_OPTIONS, doc));

		super.finished();
	}

}
