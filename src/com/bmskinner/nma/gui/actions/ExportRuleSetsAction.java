package com.bmskinner.nma.gui.actions;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.io.Io;
import com.bmskinner.nma.io.XMLWriter;
import com.bmskinner.nma.utility.FileUtils;

/**
 * Action to export ruleset collections as XML
 * 
 * @author Ben Skinner
 * @since 1.18.3
 *
 */
public class ExportRuleSetsAction extends MultiDatasetResultAction {

	private static final Logger LOGGER = Logger.getLogger(ExportRuleSetsAction.class.getName());

	private static final String PROGRESS_LBL = "Exporting options";

	public ExportRuleSetsAction(@NonNull List<IAnalysisDataset> datasets,
			@NonNull final ProgressBarAcceptor acceptor) {
		super(datasets, PROGRESS_LBL, acceptor);
	}

	@Override
	public void run() {
		setProgressBarIndeterminate();
		try {
			File folder = is.requestFolder(FileUtils.commonPathOfDatasets(datasets));

			if (datasets.size() == 1) {

				File file = new File(folder, datasets.get(0).getName() + Io.XML_FILE_EXTENSION);

				Runnable r = () -> {

					RuleSetCollection rsc = datasets.get(0).getCollection().getRuleSetCollection();
					try {
						XMLWriter.writeXML(rsc.toXmlElement(), file);
					} catch (IOException e) {
						LOGGER.warning("Unable to write rulesets to file");
					}
					super.finished();
				};
				ThreadManager.getInstance().submit(r);
			} else {

				// More than one dataset, choose folder only
				Runnable r = () -> {

					for (IAnalysisDataset d : datasets) {
						File f = new File(folder, d.getName() + Io.XML_FILE_EXTENSION);
						try {
							XMLWriter.writeXML(
									d.getCollection().getRuleSetCollection().toXmlElement(), f);
						} catch (IOException e) {
							LOGGER.warning("Unable to write rulesets to file");
						}
						LOGGER.info(() -> String.format("Exported %s rulesets to %s", d.getName(),
								f.getAbsolutePath()));
					}
					super.finished();
				};
				ThreadManager.getInstance().submit(r);
			}
		} catch (RequestCancelledException e) {
			super.finished();
		}
	}
}
