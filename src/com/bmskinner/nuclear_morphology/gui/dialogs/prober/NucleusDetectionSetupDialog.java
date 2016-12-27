package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.analysis.nucleus.DefaultNucleusDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.DefaultAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableDetectionOptions;
import com.bmskinner.nuclear_morphology.gui.dialogs.SettingsDialog;

/**
 * A test class to confirm that options panels are working before using in
 * the image prober
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class NucleusDetectionSetupDialog extends SettingsDialog {
	
	private IMutableAnalysisOptions options;
	
	private static final String DIALOG_TITLE_BAR_LBL = "Nucleus detection settings";

	/**
	 * Create using a folder to analyse.
	 * @param folder the folder of images to be analysed
	 */
	public NucleusDetectionSetupDialog(File folder) {		
		this(folder, null);
	}
	
	/**
	 * Create the dialog with an existing set of options
	 * Allows settings to be reloaded.
	 * @param folder the folder of images to be analysed
	 * @param op the existing options
	 */
	public NucleusDetectionSetupDialog(File folder, IMutableAnalysisOptions op ) {
		super();
		
		if(folder==null){
			throw new IllegalArgumentException("Input folder cannot be null");
		}
		
		if(op==null){
			options = new DefaultAnalysisOptions();
		} else {
			options = op;
		}

		IMutableDetectionOptions nucleusOptions = new DefaultNucleusDetectionOptions(folder);
		options.setDetectionOptions(IAnalysisOptions.NUCLEUS, nucleusOptions);


		setModal(true); // ensure nothing happens until this window is closed
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		this.addWindowListener(new WindowAdapter() {

			public void windowClosing(WindowEvent e) {
				NucleusDetectionSetupDialog.this.options = null;
				NucleusDetectionSetupDialog.this.setVisible(false);
			}


		});

		createAndShowGUI();
		this.setTitle(DIALOG_TITLE_BAR_LBL);
		pack();
		setVisible(true);

			
	}

	/**
	 * Get the current options 
	 * @return an AnalysisOptions
	 */
	public IMutableAnalysisOptions getOptions(){
		return options;
	}

	public void createAndShowGUI(){
		finer("Creating UI");
		setTitle("Create new analysis");
		setBounds(200, 100, 450, 626);
		this.setLocationRelativeTo(null);

		JPanel contentPane = new JPanel(new BorderLayout());
		setContentPane(contentPane);
		
		contentPane.add(new NucleusDetectionSettingsPanel(options), BorderLayout.CENTER);
		contentPane.add(makeLowerButtonPanel(), BorderLayout.SOUTH);

		finer("Created UI");

	}
	
	private JPanel makeLowerButtonPanel(){
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout());
		
		// Button to copy existing dataset options
//		JButton btnCopy = new JButton("Copy");
//		btnCopy.addMouseListener(new MouseAdapter() {
//			
//			@Override
//			public void mouseClicked(MouseEvent arg0) {
//				
//				// display panel of open datasets
//				
//				IAnalysisDataset[] nameArray = openDatasets.toArray(new AnalysisDataset[0]);
//
//				IAnalysisDataset sourceDataset = (IAnalysisDataset) JOptionPane.showInputDialog(null, 
//						"Choose source dataset",
//						"Source dataset",
//						JOptionPane.QUESTION_MESSAGE, 
//						null, 
//						nameArray, 
//						nameArray[0]);
//
//				
//				if(sourceDataset!=null){
//
//					fine("Copying options from dataset: "+sourceDataset.getName());
//					setOptions(sourceDataset.getAnalysisOptions());
//					
//				}	else {
//					fine("No dataset selected");
//				}
//				
//			}
//			
//		});
		
		// Only enable if there are open datasets
//		if( ! hasOpenDatasetTemplates()){
//			btnCopy.setEnabled(false);
//		}
//		btnCopy.setToolTipText("Copy from open dataset");
//		panel.add(btnCopy);

		JButton btnOk = new JButton("OK");
		btnOk.addActionListener( e -> {
				
			// probe the first image
			// show the results of the current settings
			ImageProber p = new NucleusDetectionImageProber(options,  
					options.getDetectionOptions(IAnalysisOptions.NUCLEUS).getFolder());
			if(p.getOK()==false){

				// Do nothing, revise options

			} else {

				// ok, close the window
				NucleusDetectionSetupDialog.this.setVisible(false);
			}

		});

		panel.add(btnOk);

		JButton btnCancel = new JButton("Cancel");
		btnCancel.addActionListener( e -> {
			options = null;
			NucleusDetectionSetupDialog.this.setVisible(false);
		});
		panel.add(btnCancel);
		return panel;
	}

}
