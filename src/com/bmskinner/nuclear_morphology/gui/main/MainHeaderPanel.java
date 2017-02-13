package com.bmskinner.nuclear_morphology.gui.main;

import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.actions.NewAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.actions.NeutrophilAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.actions.PopulationImportAction;
import com.bmskinner.nuclear_morphology.gui.components.panels.MeasurementUnitSettingsPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.MainOptionsDialog;
import com.bmskinner.nuclear_morphology.logging.Loggable;

@SuppressWarnings("serial")
public class MainHeaderPanel extends JPanel implements Loggable {
	
	private static final String NEW_ANALYSIS_LBL   = "New analysis";
	private static final String NEW_NEUTRO_LBL     = "Neutrophil";
	private static final String LOAD_DATASET_LBL   = "Load analysis dataset";
	private static final String SAVE_ALL_LBL       = "Save all";
	private static final String SAVE_WORKSPACE_LBL = "Save workspace";
	private static final String OPTIONS_LBL        = "Options";
	
	private MainWindow mw;
	
	public MainHeaderPanel(MainWindow mw){
		this.mw = mw;
		
		setLayout(new FlowLayout());
		
		createHeaderButtons();
	}
	
	/**
	 * Create the panel of primary buttons
	 */
	private void createHeaderButtons(){


		JButton btnNewAnalysis = new JButton(NEW_ANALYSIS_LBL);
		btnNewAnalysis.addActionListener(
				e ->{
					Runnable r = new NewAnalysisAction(mw);
					r.run();
				}
		);
		add(btnNewAnalysis);
		
		
		JButton btnNeutrophil = new JButton(NEW_NEUTRO_LBL);
		btnNeutrophil.addActionListener(
				e ->{
					Runnable r = new NeutrophilAnalysisAction(mw);
					r.run();
				}
		);
		add(btnNeutrophil);
		

		//---------------
		// load saved dataset button
		//---------------

		JButton btnLoadSavedDataset = new JButton(LOAD_DATASET_LBL);
		
		btnLoadSavedDataset.addActionListener(	
			e -> {
				finest("Creating import action");
				Runnable r = new PopulationImportAction(mw);
				r.run();
			}
		);
			
		add(btnLoadSavedDataset);

		//---------------
		// save button
		//---------------

		JButton btnSavePopulation = new JButton(SAVE_ALL_LBL);
		btnSavePopulation.addActionListener( e -> {
					log("Saving root populations...");
					mw.saveRootDatasets();
				}
		);

		add(btnSavePopulation);
		
		//---------------
		// save workspace button
		//---------------

		JButton btnSaveWorkspace = new JButton(SAVE_WORKSPACE_LBL);
		btnSaveWorkspace.addActionListener( e -> {
					mw.saveWorkspace();
			}
		);

		add(btnSaveWorkspace);
				
		JButton optionsButton = new JButton(OPTIONS_LBL);
		optionsButton.addActionListener(
				
				e -> { 

					MainOptionsDialog dialog = new MainOptionsDialog(mw);
					dialog.addInterfaceEventListener(mw);
			}
		);		
		add(optionsButton);
		
		MeasurementUnitSettingsPanel unitsPanel = new MeasurementUnitSettingsPanel();
		unitsPanel.addInterfaceEventListener(mw);
		add(unitsPanel);

	}

}
