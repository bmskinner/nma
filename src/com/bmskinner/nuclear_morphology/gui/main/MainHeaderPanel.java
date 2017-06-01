package com.bmskinner.nuclear_morphology.gui.main;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.gui.actions.NeutrophilAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.actions.NewAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.actions.PopulationImportAction;
import com.bmskinner.nuclear_morphology.gui.components.panels.MeasurementUnitSettingsPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.MainOptionsDialog;
import com.bmskinner.nuclear_morphology.logging.Loggable;

@SuppressWarnings("serial")
public class MainHeaderPanel extends JPanel implements Loggable {

    private static final String NEW_ANALYSIS_LBL   = "New analysis";
    private static final String NEW_STANDARD_LBL   = "Fluorescent nuclei";
    private static final String NEW_NEUTROPHIL_LBL = "Neutrophils";

    private static final String LOAD_DATASET_LBL   = "Load dataset";
    private static final String SAVE_ALL_LBL       = "Save all";
    private static final String SAVE_WORKSPACE_LBL = "Save workspace";
    private static final String OPTIONS_LBL        = "Options";

    private MainWindow mw;

    public MainHeaderPanel(MainWindow mw) {
        this.mw = mw;

        setLayout(new FlowLayout());

        createHeaderButtons();
    }

    /**
     * Create the panel of primary buttons
     */
    private void createHeaderButtons() {

        JButton btnNewAnalysis = new JButton(NEW_ANALYSIS_LBL);

        if (Version.currentVersion().isNewerThan(Version.v_1_13_4)) {

            final JPopupMenu popup = new JPopupMenu();

            popup.add(new JMenuItem(new AbstractAction(NEW_STANDARD_LBL) {
                public void actionPerformed(ActionEvent e) {
                    Runnable r = new NewAnalysisAction(mw);
                    r.run();
                }
            }));
            popup.add(new JMenuItem(new AbstractAction(NEW_NEUTROPHIL_LBL) {
                public void actionPerformed(ActionEvent e) {
                    Runnable r = new NeutrophilAnalysisAction(mw);
                    r.run();
                }
            }));

            btnNewAnalysis.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    popup.show(btnNewAnalysis, 0, btnNewAnalysis.getBounds().height);

                }
            });
        } else {
            btnNewAnalysis.addActionListener(e -> {
                Runnable r = new NewAnalysisAction(mw);
                r.run();
            });
        }

        add(btnNewAnalysis);

        // ---------------
        // load saved dataset button
        // ---------------

        JButton btnLoadSavedDataset = new JButton(LOAD_DATASET_LBL);

        btnLoadSavedDataset.addActionListener(e -> {
            finest("Creating import action");
            Runnable r = new PopulationImportAction(mw);
            r.run();
        });

        add(btnLoadSavedDataset);

        // ---------------
        // save button
        // ---------------

        JButton btnSavePopulation = new JButton(SAVE_ALL_LBL);
        btnSavePopulation.addActionListener(e -> {
            log("Saving root populations...");
            mw.getEventHandler().saveRootDatasets();
        });

        add(btnSavePopulation);

        // ---------------
        // save workspace button
        // ---------------

        JButton btnSaveWorkspace = new JButton(SAVE_WORKSPACE_LBL);
        btnSaveWorkspace.addActionListener(e -> {
            mw.getEventHandler().signalChangeReceived(
                    new SignalChangeEvent(this, SignalChangeEvent.EXPORT_WORKSPACE, this.getClass().getName()));
        });

        add(btnSaveWorkspace);

        JButton optionsButton = new JButton(OPTIONS_LBL);
        optionsButton.addActionListener(

                e -> {

                    MainOptionsDialog dialog = new MainOptionsDialog(mw);
                    dialog.addInterfaceEventListener(mw.getEventHandler());
                });
        add(optionsButton);

        MeasurementUnitSettingsPanel unitsPanel = new MeasurementUnitSettingsPanel();
        unitsPanel.addInterfaceEventListener(mw.getEventHandler());
        add(unitsPanel);

    }

}
