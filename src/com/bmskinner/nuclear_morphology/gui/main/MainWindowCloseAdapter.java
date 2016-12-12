package com.bmskinner.nuclear_morphology.gui.main;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CountDownLatch;

import javax.swing.JOptionPane;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.DatasetListManager;
import com.bmskinner.nuclear_morphology.gui.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.actions.SaveDatasetAction;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * This window adapter is used to check if datasets have changed when the main 
 * window is closed, and offers to save changed datasets before completing the 
 * close.
 * @author ben
 * @since 1.13.3
 *
 */
public class MainWindowCloseAdapter extends WindowAdapter implements Loggable {
	
	private MainWindow mw;
	
	public MainWindowCloseAdapter(MainWindow mw){
		super();
		this.mw = mw;
	}
	
	@Override
	public void windowClosing(WindowEvent e) {
		fine("Checking dataset state");

		if(DatasetListManager.getInstance().hashCodeChanged()){
			fine("Found changed hashcode");
			Object[] options = { "Save datasets" , "Exit without saving", "Cancel exit" };
			int save = JOptionPane.showOptionDialog(mw,
					"Datasets have changed since last save!", 
					"Save datasets?",
					JOptionPane.DEFAULT_OPTION, 
					JOptionPane.QUESTION_MESSAGE,
					null, options, options[0]);

			if(save==0){
				saveAndClose();

			} 
			
			if(save==1){
				fine("Exiting without save");
				close();					
			} 
			
			if(save==2){
				fine("Ignoring close");
			}
		} else {
			fine("No change found");
			close();
		}
	}
	
				
	  public void windowClosed(WindowEvent e) {
		  close();
	  }

	  public void close(){
		  DatasetListManager.getInstance().clear();
		  GlobalOptions.getInstance().setDefaults();
		  mw.dispose();
		  if(mw.isStandalone()){
			  System.exit(0);
		  }
	  }

	  /**
	   * Save the root datasets, then dispose the frame
	   */
	  private void saveAndClose(){
		  Runnable r = () -> {
			  for(IAnalysisDataset root : DatasetListManager.getInstance().getRootDatasets()){
				  final CountDownLatch latch = new CountDownLatch(1);

				  Runnable task = new SaveDatasetAction(root, mw, latch, false);
				  task.run();
				  try {
					  latch.await();
				  } catch (InterruptedException e) {
					  error("Interruption to thread", e);
				  }
			  }
			  log("All root datasets saved");
			  close();
		  };

		  ThreadManager.getInstance().execute(r);
	  }

}
