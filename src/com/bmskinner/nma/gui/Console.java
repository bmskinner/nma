package com.bmskinner.nma.gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import com.bmskinner.nma.components.datasets.DatasetValidator;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.DatasetListManager;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.gui.events.UserActionEvent;

/**
 * Allow typed commands for functions that are not ready to expose through the
 * GUI
 * 
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class Console extends JPanel implements ActionListener {

	private static final Logger LOGGER = Logger.getLogger(Console.class.getName());

	private JTextField console = new JTextField();

	private final LogPanel logPanel;

	private int historyIndex = -1;
	private List<String> history = new LinkedList<>();

	private static final String NEXT_HISTORY_ACTION = "Next";
	private static final String PREV_HISTORY_ACTION = "Prev";

	private static final String HIST_CMD = "history";
	private static final String CHECK_CMD = "check";
	private static final String CHECK_DETAIL_CMD = "check detail";
	private static final String HELP_CMD = "help";
	private static final String CLEAR_CMD = "clear";
	private static final String GLCM_CMD = "glcm";
	private static final String HISTOGRAM_CMD = "histogram";
	private static final String KEYPOINT_EXPORT_CMD = "export keypoints";
	private static final String KEYPOINT_IMPORT_CMD = "import keypoints";
	private static final String LIST_CMD = "list";
	private static final String TASKS_CMD = "tasks";
	private static final String HASH_CMD = "hash";
	private static final String EDGE_CMD = "edge";
	private static final String REMAP_CMD = "remap";

	private final List<Command> runnableCommands = new ArrayList<>();

	/**
	 * Create with a log panel to display on
	 * 
	 * @param logPanel
	 */
	public Console(final LogPanel logPanel) {
		this.logPanel = logPanel;
		setLayout(new BorderLayout());
		makeCommandList();
		Font font = new Font("Monospaced", Font.PLAIN, 13);
		console.setFont(font);
		add(console, BorderLayout.CENTER);
		setVisible(false);
		console.addActionListener(this);

		console.getInputMap(JComponent.WHEN_FOCUSED).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
				PREV_HISTORY_ACTION);

		console.getActionMap().put(PREV_HISTORY_ACTION, new PrevHistoryAction());

		console.getInputMap(JComponent.WHEN_FOCUSED).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
				NEXT_HISTORY_ACTION);
		console.getActionMap().put(NEXT_HISTORY_ACTION, new NextHistoryAction());
	}

	/**
	 * Toggle the visibilty of the console
	 */
	public void toggle() {
		if (isVisible()) {
			setVisible(false);
		} else {
			setVisible(true);
			console.setText(null);
			console.grabFocus();
			console.requestFocus();
			console.requestFocusInWindow();
		}
		revalidate();
		repaint();
	}

	/**
	 * A command name, its help description and the runnable
	 * 
	 * @author bs19022
	 *
	 */
	private record Command(String name, String desc, Runnable func) {
	}

	/**
	 * Make the list of local commands to run
	 */
	private void makeCommandList() {

		runnableCommands.add(new Command(HELP_CMD,
				"show this help message",
				() -> {
					logPanel.println("Available commands:");
					for (Command s : runnableCommands)
						logPanel.println(s.name + " - " + s.desc);
				}));

		runnableCommands.add(new Command(EDGE_CMD,
				"filter poor edge detection",
				() -> UserActionController.getInstance()
						.userActionEventReceived(new UserActionEvent(this,
								UserActionEvent.FILTER_POOR_EDGE_DETECTION,
								DatasetListManager.getInstance().getSelectedDatasets()))));

		runnableCommands.add(new Command(HIST_CMD,
				"show the previous commands",
				() -> {
					LOGGER.info("History: ");
					for (String s : history)
						LOGGER.info("\t" + s);
				}));

		runnableCommands.add(new Command(HISTOGRAM_CMD,
				"calculate pixel histograms",
				() -> UserActionController.getInstance()
						.userActionEventReceived(new UserActionEvent(this,
								UserActionEvent.RUN_HISTOGRAM_CALC,
								DatasetListManager.getInstance().getSelectedDatasets()))));

		runnableCommands.add(new Command(HASH_CMD,
				"display the hashes of all open datasets",
				() -> UserActionController.getInstance()
						.userActionEventReceived(new UserActionEvent(this,
								UserActionEvent.PRINT_DATASET_HASH_CMD,
								DatasetListManager.getInstance().getSelectedDatasets()))));

		runnableCommands.add(new Command(CHECK_CMD,
				"check all datasets are valid, quietly",
				() -> validateDatasets(false)));

		runnableCommands.add(new Command(CHECK_DETAIL_CMD,
				"check all datasets are valid, verbosely",
				() -> validateDatasets(true)));

		runnableCommands.add(new Command(CLEAR_CMD,
				"clear the console",
				logPanel::clear));

		runnableCommands.add(new Command(LIST_CMD,
				"list all open datasets",
				this::listDatasets));

		runnableCommands.add(new Command(REMAP_CMD,
				"remap landmarks between nmd files",
				() -> UserActionController.getInstance()
						.userActionEventReceived(new UserActionEvent(this,
								UserActionEvent.REMAP_LANDMARKS))));

		runnableCommands.add(new Command(TASKS_CMD,
				"list all queued tasks",
				this::listTasks));

		runnableCommands.add(new Command(GLCM_CMD,
				"run GLCM on all selected datasets",
				() -> UserActionController.getInstance()
						.userActionEventReceived(new UserActionEvent(this,
								UserActionEvent.RUN_GLCM_ANALYSIS,
								DatasetListManager.getInstance().getSelectedDatasets()))));

		runnableCommands.add(new Command(KEYPOINT_EXPORT_CMD,
				"export keypoints from all selected datasets",
				() -> UserActionController.getInstance()
						.userActionEventReceived(new UserActionEvent(this,
								UserActionEvent.EXPORT_KEYPOINTS,
								DatasetListManager.getInstance().getSelectedDatasets()))));
		
		runnableCommands.add(new Command(KEYPOINT_IMPORT_CMD,
				"import keypoints to selected dataset",
				() -> UserActionController.getInstance()
						.userActionEventReceived(new UserActionEvent(this,
								UserActionEvent.IMPORT_KEYPOINTS,
								DatasetListManager.getInstance().getSelectedDatasets()))));

	}

	/**
	 * Run the given command from the console
	 * 
	 * @param command
	 */
	private void runCommand(String command) {
		Optional<Command> f = runnableCommands.stream().filter(c -> c.name.equals(command))
				.findFirst();

		if (f.isPresent())
			f.get().func.run();
		else
			LOGGER.info(() -> String.format("Command '%s' not recognised", command));
	}

	/*
	 * Listener for the console
	 */
	@Override
	public void actionPerformed(ActionEvent e) {

		if (e.getSource().equals(console)) {
			history.add(console.getText());
			historyIndex = history.size();
			runCommand(console.getText());
			console.setText("");
		}
	}

	private void listDatasets() {
		int i = 0;
		for (IAnalysisDataset d : DatasetListManager.getInstance().getAllDatasets()) {
			String type = d.getCollection().isReal() ? "Real" : "Virtual";
			LOGGER.info(i + "\t" + d.getName() + "\t" + type);
			i++;
		}
	}

	private void listTasks() {
		LOGGER.info(ThreadManager.getInstance().toString());
	}

	private void validateDatasets(boolean isDetail) {

		DatasetValidator v = new DatasetValidator();
		for (IAnalysisDataset d : DatasetListManager.getInstance().getRootDatasets()) {
			LOGGER.info("Validating " + d.getName() + "...");
			if (!v.validate(d)) {

				if (isDetail) {
					for (String s : v.getErrors()) {
						LOGGER.warning(s);
					}
				} else {
					for (String s : v.getSummary()) {
						LOGGER.warning(s);
					}
					LOGGER.warning("Use 'check detail' for full list of errors");
				}
			} else {
				LOGGER.info("Dataset OK");
			}

		}
	}

	@SuppressWarnings("serial")
	private class PrevHistoryAction extends AbstractAction {

		public PrevHistoryAction() {
			super("Prev");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (history.isEmpty())
				return;
			if (historyIndex > -1) {
				historyIndex--;
			}
			if (historyIndex == -1) {
				console.setText("");
				return;
			}
			console.setText(history.get(historyIndex));
		}
	}

	@SuppressWarnings("serial")
	private class NextHistoryAction extends AbstractAction {

		public NextHistoryAction() {
			super("Next");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (history.isEmpty()) {
				historyIndex = 0;
				return;
			}
			if (historyIndex >= history.size() - 1)
				return;
			historyIndex++;
			console.setText(history.get(historyIndex));

		}
	}
}
