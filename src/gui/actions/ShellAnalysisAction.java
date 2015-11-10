package gui.actions;

import gui.MainWindow;

import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import analysis.AnalysisDataset;
import analysis.nucleus.ShellAnalysis;

public class ShellAnalysisAction extends ProgressableAction {

	public ShellAnalysisAction(AnalysisDataset dataset, MainWindow mw) {
		super(dataset, "Shell analysis", "Error in shell analysis", mw);

		SpinnerNumberModel sModel = new SpinnerNumberModel(5, 2, 10, 1);
		JSpinner spinner = new JSpinner(sModel);

		int option = JOptionPane.showOptionDialog(null, 
				spinner, 
				"Select number of shells", 
				JOptionPane.OK_CANCEL_OPTION, 
				JOptionPane.QUESTION_MESSAGE, null, null, null);
		if (option == JOptionPane.CANCEL_OPTION) {
			// user hit cancel
			this.cancel();
			return;

		} else if (option == JOptionPane.OK_OPTION)	{

			int shellCount = (Integer) spinner.getModel().getValue();
			worker = new ShellAnalysis(dataset,shellCount);

			worker.addPropertyChangeListener(this);
			worker.execute();	
		}
	}
}