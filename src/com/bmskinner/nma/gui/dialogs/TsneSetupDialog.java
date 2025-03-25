package com.bmskinner.nma.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.classification.TsneMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.OptionsFactory;
import com.bmskinner.nma.components.profiles.ProfileType;

/**
 * Analysis setup dialog for tSNE method
 * @author Ben Skinner
 * @since 1.16.0
 *
 */
public class TsneSetupDialog extends SubAnalysisSetupDialog {
	
	private static final Logger LOGGER = Logger.getLogger(TsneSetupDialog.class.getName());

    private static final String DIALOG_TITLE = "t-SNE options";
    
    private static final double MIN_PERPLEXITY = 5;
    private static final double MAX_PERPLEXITY = 10000;
    private static final double STEP_PERPLEXITY = 1;
    
    protected final HashOptions options;

    public TsneSetupDialog(final @NonNull IAnalysisDataset dataset) {
        // modal dialog
        this(dataset, DIALOG_TITLE);
    }

    /**
     * Constructor that does not make panel visible
     * 
     * @param mw
     * @param title
     */
    protected TsneSetupDialog(final @NonNull IAnalysisDataset dataset, final String title) {
        super(dataset, title);
        options = OptionsFactory.makeDefaultTsneOptions().build();
        createUI();
        packAndDisplay();
    }

	@Override
	protected void setDefaults() {
		// nothing to set yet
	}

    @Override
    public IAnalysisMethod getMethod() {
    	return new TsneMethod(getFirstDataset(), options);
    }
    
    @Override
	public HashOptions getOptions() {
		return options;
	}

    @Override
    protected void createUI() {
    	getContentPane().add(createHeader(), BorderLayout.NORTH);
    	getContentPane().add(createFooter(), BorderLayout.SOUTH);
    	getContentPane().add(createOptionsPanel(), BorderLayout.CENTER);
    }
    
    protected JPanel createOptionsPanel() {

        JPanel panel = new JPanel();
        GridBagLayout layout = new GridBagLayout();
        panel.setLayout(layout);
        
        List<JLabel> labels = new ArrayList<>();
        List<Component> fields = new ArrayList<>();

        labels.add(new JLabel(TsneMethod.PROFILE_TYPE_KEY));
        fields.add(makeProfileComboBox());

        labels.add(new JLabel(TsneMethod.MAX_ITERATIONS_KEY));
        fields.add(makeMaxIterationsSpinner());

        labels.add(new JLabel(TsneMethod.PERPLEXITY_KEY));
        fields.add(makePerplexitySpinner());
        
        addLabelTextRows(labels, fields, layout, panel);
        return panel;
    }
    
    private JComboBox<ProfileType> makeProfileComboBox() {
    	 JComboBox<ProfileType> profileBox = new JComboBox<>(ProfileType.displayValues());
         profileBox.setSelectedItem(ProfileType.ANGLE);
         profileBox.setEnabled(true);
         profileBox.addActionListener(l->options.setString(TsneMethod.PROFILE_TYPE_KEY,profileBox.getSelectedItem().toString()));
         return profileBox;
    }
    
    /**
     * Create the iterations spinner with default perplexity based
     * on the number of nuclei in the dataset
     * @return
     */
    private JSpinner makeMaxIterationsSpinner() {
    	SpinnerModel iterationsModel = new SpinnerNumberModel(options.getInt(TsneMethod.MAX_ITERATIONS_KEY), // initial                                                                           // value
                500, // min
                5000, // max
                25); // step

        JSpinner iterationsSpinner = new JSpinner(iterationsModel);
        iterationsSpinner.setEnabled(true);
        iterationsSpinner.addChangeListener(l->{
        	try {
				iterationsSpinner.commitEdit();
				options.setInt(TsneMethod.MAX_ITERATIONS_KEY, (Integer) iterationsSpinner.getValue());
			} catch (ParseException e) {
				LOGGER.log(Level.SEVERE, "Parse error in spinner", e);
			}
        	
        });
        return iterationsSpinner;
    }
    
    /**
     * Create the perplexity spinner with default perplexity based
     * on the number of nuclei in the dataset
     * @return
     */
    private JSpinner makePerplexitySpinner() {
        int nNuclei = getFirstDataset().getCollection().getNucleusCount();
        double initialPerplexity = Math.max(MIN_PERPLEXITY, nNuclei/20d);
        options.setDouble(TsneMethod.PERPLEXITY_KEY, initialPerplexity);
        
        SpinnerModel perplexityModel = new SpinnerNumberModel(initialPerplexity, MIN_PERPLEXITY, MAX_PERPLEXITY, STEP_PERPLEXITY);

        JSpinner perplexitySpinner = new JSpinner(perplexityModel);
        perplexitySpinner.setEnabled(true);

        perplexitySpinner.addChangeListener(l->{
        	try {
        		perplexitySpinner.commitEdit();
				options.setDouble(TsneMethod.PERPLEXITY_KEY, (Double) perplexitySpinner.getValue());
			} catch (ParseException e) {
				LOGGER.log(Level.SEVERE, "Parse error in spinner", e);
			}
        	
        });  
        return perplexitySpinner;
    }

	
}
