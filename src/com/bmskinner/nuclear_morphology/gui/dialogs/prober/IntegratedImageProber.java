package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.components.options.IMutableAnalysisOptions;
import com.bmskinner.nuclear_morphology.gui.dialogs.LoadingIconDialog;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings.SettingsPanel;

/**
 * Integrates the analysis setup dialog with the image prober.
 * 
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public abstract class IntegratedImageProber extends LoadingIconDialog {

    private static final String PROCEED_LBL = "Proceed with detection";

    protected IMutableAnalysisOptions options; // the active options

    protected SettingsPanel optionsSettingsPanel; // settings

    protected GenericImageProberPanel imageProberPanel; // result

    protected boolean ok = false;

    private JButton okButton = new JButton(PROCEED_LBL);

    /**
     * Make the footer panel, with ok and cancel buttons
     * 
     * @return
     */
    protected JPanel createFooter() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        okButton.addActionListener(e -> {
            okButtonClicked();
            ok = true;
            setVisible(false);
        });
        panel.add(okButton);

        getRootPane().setDefaultButton(okButton);

        return panel;
    }

    /**
     * Allow overriding of the "Proceed" button label
     * 
     * @param s
     */
    protected void setOkButtonText(String s) {
        okButton.setText(s);
    }

    /**
     * Check if the analysis is ready to run
     * 
     * @return
     */
    public boolean isOk() {
        return ok;
    }

    protected abstract void okButtonClicked();

}
