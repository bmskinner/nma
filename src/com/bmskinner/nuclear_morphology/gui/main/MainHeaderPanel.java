package com.bmskinner.nuclear_morphology.gui.main;

import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.actions.FishRemappingAction;
import com.bmskinner.nuclear_morphology.gui.actions.NewAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.actions.PopulationImportAction;
import com.bmskinner.nuclear_morphology.gui.components.panels.MeasurementUnitSettingsPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.MainOptionsDialog;
import com.bmskinner.nuclear_morphology.logging.Loggable;

@SuppressWarnings("serial")
public class MainHeaderPanel extends JPanel implements Loggable {
	
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


		JButton btnNewAnalysis = new JButton("New analysis");
		btnNewAnalysis.addActionListener(
				e ->{
					Runnable r = new NewAnalysisAction(mw);
					r.run();
				}
		);
		add(btnNewAnalysis);

		//---------------
		// load saved dataset button
		//---------------

		JButton btnLoadSavedDataset = new JButton("Load analysis dataset");
		
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

		JButton btnSavePopulation = new JButton("Save all");
		btnSavePopulation.addActionListener( e -> {
					log("Saving root populations...");
					mw.saveRootDatasets();
				}
		);

		add(btnSavePopulation);
		
		//---------------
		// save workspace button
		//---------------

		JButton btnSaveWorkspace = new JButton("Save workspace");
		btnSaveWorkspace.addActionListener( e -> {
					mw.saveWorkspace();
			}
		);

		add(btnSaveWorkspace);

		//---------------
		// FISH mapping button
		//---------------

		JButton btnPostanalysisMapping = new JButton("Post-FISH mapping");
		btnPostanalysisMapping.addActionListener(
				e ->{
					Runnable r = new FishRemappingAction(mw.getPopulationsPanel().getSelectedDatasets(), mw);
					r.run();
				}
		);
		add(btnPostanalysisMapping);
				
		JButton optionsButton = new JButton("Options");
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
