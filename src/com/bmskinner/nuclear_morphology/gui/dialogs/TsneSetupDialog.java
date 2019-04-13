package com.bmskinner.nuclear_morphology.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.classification.ProfileTsneMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;

/**
 * Analysis setup dialog for tSNE method
 * @author bms41
 * @since 1.16.0
 *
 */
public class TsneSetupDialog extends SubAnalysisSetupDialog {

    private static final String DIALOG_TITLE = "t-SNE options";

    protected JPanel headingPanel;
    protected JPanel footerPanel;
    
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

        JPanel optionsPanel = createOptionsPanel();
        contentPanel.add(optionsPanel, BorderLayout.CENTER);
    }
    
    protected JPanel createOptionsPanel() {

        JPanel panel = new JPanel();
        GridBagLayout layout = new GridBagLayout();
        panel.setLayout(layout);
        
        List<JLabel> labels = new ArrayList<>();
        List<Component> fields = new ArrayList<>();
        
        JComboBox<ProfileType> profileBox = new JComboBox<>(ProfileType.displayValues());
        profileBox.setSelectedItem(ProfileType.ANGLE);
        profileBox.setEnabled(true);
        profileBox.addActionListener(l->options.setString(ProfileTsneMethod.PROFILE_TYPE_KEY,profileBox.getSelectedItem().toString()));
        
        labels.add(new JLabel(ProfileTsneMethod.PROFILE_TYPE_KEY));
        fields.add(profileBox);

        SpinnerModel iterationsModel = new SpinnerNumberModel(options.getInt(ProfileTsneMethod.MAX_ITERATIONS_KEY), // initial                                                                           // value
                500, // min
                5000, // max
                25); // step

        JSpinner iterationsSpinner = new JSpinner(iterationsModel);
        iterationsSpinner.setEnabled(true);
        iterationsSpinner.addChangeListener(l->{
        	try {
				iterationsSpinner.commitEdit();
				options.setInt(ProfileTsneMethod.MAX_ITERATIONS_KEY, (Integer) iterationsSpinner.getValue());
			} catch (ParseException e) {
				error("Parse error in spinner", e);
			}
        	
        });
        
        
        labels.add(new JLabel(ProfileTsneMethod.MAX_ITERATIONS_KEY));
        fields.add(iterationsSpinner);
        
        SpinnerModel perplexityModel = new SpinnerNumberModel(options.getDouble(ProfileTsneMethod.PERPLEXITY_KEY), // initial                                                                           // value
                5, // min
                10000, // max
                1); // step

        JSpinner perplexitySpinner = new JSpinner(perplexityModel);
        perplexitySpinner.setEnabled(true);

        labels.add(new JLabel(ProfileTsneMethod.PERPLEXITY_KEY));
        fields.add(perplexitySpinner);
        perplexitySpinner.addChangeListener(l->{
        	try {
				iterationsSpinner.commitEdit();
				options.setDouble(ProfileTsneMethod.PERPLEXITY_KEY, (Double) perplexitySpinner.getValue());
			} catch (ParseException e) {
				error("Parse error in spinner", e);
			}
        	
        });        
        addLabelTextRows(labels, fields, layout, panel);
        return panel;
    }
}
