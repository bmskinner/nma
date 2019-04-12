package com.bmskinner.nuclear_morphology.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.classification.ProfileTsneMethod;
import com.bmskinner.nuclear_morphology.analysis.classification.TreeBuildingMethod;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions.HierarchicalClusterMethod;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.stats.DipTester;

/**
 * Analysis setup dialog for tSNE method
 * @author bms41
 * @since 1.15.3
 *
 */
public class TsneSetupDialog extends SubAnalysisSetupDialog implements ChangeListener {

    private static final String DIALOG_TITLE = "t-SNE options";

    protected JPanel headingPanel;
    protected JPanel optionsPanel;
    protected JPanel footerPanel;
    
    private JSpinner perplexitySpinner;
    private JSpinner iterationsSpinner;

    protected final HashOptions options;

    public TsneSetupDialog(final IAnalysisDataset dataset) {
        // modal dialog
        this(dataset, DIALOG_TITLE);
    }

    /**
     * Constructor that does not make panel visible
     * 
     * @param mw
     * @param title
     */
    protected TsneSetupDialog(final IAnalysisDataset dataset, final String title) {
        super(dataset, title);
        options = OptionsFactory.makeDefaultTsneOptions();
        createUI();
        packAndDisplay();
    }

	@Override
	protected void setDefaults() {
		// nothing to set yet
	}

    protected JPanel createHeader() {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    @Override
    public IAnalysisMethod getMethod() {
    	return new ProfileTsneMethod(dataset, options);
    }

    @Override
    protected void createUI() {

        contentPanel.setLayout(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPanel);


        headingPanel = createHeader();
        contentPanel.add(headingPanel, BorderLayout.NORTH);

        footerPanel = createFooter();
        contentPanel.add(footerPanel, BorderLayout.SOUTH);

        optionsPanel = new JPanel();
        GridBagLayout layout = new GridBagLayout();
        optionsPanel.setLayout(layout);
        
        List<JLabel> labels = new ArrayList<>();
        List<Component> fields = new ArrayList<>();

        SpinnerModel iterationsModel = new SpinnerNumberModel(options.getInt(ProfileTsneMethod.MAX_ITERATIONS_KEY), // initial                                                                           // value
                500, // min
                5000, // max
                25); // step

        iterationsSpinner = new JSpinner(iterationsModel);
        iterationsSpinner.setEnabled(true);
        iterationsSpinner.addChangeListener(this);
        
        
        labels.add(new JLabel(ProfileTsneMethod.MAX_ITERATIONS_KEY));
        fields.add(iterationsSpinner);
        
        SpinnerModel perplexityModel = new SpinnerNumberModel(options.getDouble(ProfileTsneMethod.PERPLEXITY_KEY), // initial                                                                           // value
                5, // min
                10000, // max
                1); // step

        perplexitySpinner = new JSpinner(perplexityModel);
        perplexitySpinner.setEnabled(true);

        labels.add(new JLabel(ProfileTsneMethod.PERPLEXITY_KEY));
        fields.add(perplexitySpinner);
        perplexitySpinner.addChangeListener(this);
        
        addLabelTextRows(labels, fields, layout, optionsPanel);
        

        contentPanel.add(optionsPanel, BorderLayout.CENTER);
    }

    @Override
    public void stateChanged(ChangeEvent e) {

        if (e.getSource() == iterationsSpinner) {
            options.setInt(ProfileTsneMethod.MAX_ITERATIONS_KEY, (Integer) iterationsSpinner.getValue());
        }
        
        if (e.getSource() == perplexitySpinner) {
            options.setDouble(ProfileTsneMethod.PERPLEXITY_KEY, (Double) perplexitySpinner.getValue());
        }
    }

}
