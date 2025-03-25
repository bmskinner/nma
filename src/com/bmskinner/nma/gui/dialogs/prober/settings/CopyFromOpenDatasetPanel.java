package com.bmskinner.nma.gui.dialogs.prober.settings;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.core.DatasetListManager;

/**
 * Base class for settings panels allowing detection options to be copied from an existing dataset
 * @author Ben Skinner
 * @since 1.15.0
 *
 */
public abstract class CopyFromOpenDatasetPanel extends DetectionSettingsPanel {
	
	protected static final String COPY_FROM_OPEN_LBL     = "From dataset";
	private static final String COPY_FROM_OPEN_TOOLTIP = "Copy from existing open dataset";
    
    protected static final String OPEN_SETTINGS_LBL     = "From file";
    private static final String OPEN_SETTINGS_TOOLTIP = "Choose a saved options file";

    protected static final String CHOOSE_DATASET_MSG_LBL = "Choose source dataset";
    protected static final String CHOOSE_DATASET_TTL_LBL = "Source dataset";

    protected JButton copyBtn = new JButton(COPY_FROM_OPEN_LBL);
    
    protected JButton openBtn = new JButton(OPEN_SETTINGS_LBL);
    
    protected IAnalysisOptions parent;
    
    public CopyFromOpenDatasetPanel(IAnalysisOptions parent, HashOptions op) {
    	super(op);
        this.parent = parent;
        this.add(createPanel(), BorderLayout.CENTER);
    }
    
    /**
     * Create the listener for clicking the copy button
     * @return
     */
    protected abstract ActionListener createCopyActionListener();
    
    /**
     * Create the listener for clicking the open button
     * @return
     */
    protected abstract ActionListener createOpenActionListener();
    
    private JPanel createPanel() {

        JPanel panel = new JPanel();

        panel.setLayout(new FlowLayout(FlowLayout.CENTER));

        panel.add(copyBtn);
        panel.add(openBtn);
        
        copyBtn.setEnabled(DatasetListManager.getInstance().hasDatasets());
        copyBtn.setToolTipText(COPY_FROM_OPEN_TOOLTIP);
        openBtn.setToolTipText(OPEN_SETTINGS_TOOLTIP);
        
        copyBtn.addActionListener(createCopyActionListener());
        openBtn.addActionListener(createOpenActionListener());
        return panel;
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        openBtn.setEnabled(b);
        copyBtn.setEnabled(b ? DatasetListManager.getInstance().hasDatasets() : false);
    }

}
