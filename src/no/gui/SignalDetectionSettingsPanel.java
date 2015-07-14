package no.gui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import no.components.AnalysisOptions;
import no.components.AnalysisOptions.NuclearSignalOptions;

public class SignalDetectionSettingsPanel implements ChangeListener {

	private static final long serialVersionUID = 1L;
	private static final int    DEFAULT_SIGNAL_THRESHOLD = 70;
	private static final int    DEFAULT_MIN_SIGNAL_SIZE = 5;
	private static final double DEFAULT_MAX_SIGNAL_FRACTION = 0.5;
	private static final double DEFAULT_MIN_CIRC = 0.0;
	private static final double DEFAULT_MAX_CIRC = 1.0;
	
	private JPanel panel;
	
	private NuclearSignalOptions options;
	
	private JSpinner minSizeSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_MIN_SIGNAL_SIZE,	1, 10000, 1));
	private JSpinner maxFractSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_MAX_SIGNAL_FRACTION, 0, 1, 0.05));
	
	private JSpinner thresholdSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_SIGNAL_THRESHOLD,	0, 255, 1));
	
	private JSpinner minCircSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_MIN_CIRC,	0, 1, 0.05));
	private JSpinner maxCircSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_MAX_CIRC,	0, 1, 0.05));
	
	
	
	public SignalDetectionSettingsPanel(AnalysisOptions a, String type) {
		a.addCannyOptions(type);
		this.options = a.getNuclearSignalOptions(type);
		makeDefaultSettings();
		makePanel();
	}
	
	public JPanel getPanel(){
		return this.panel;
	}
	
	private void makeDefaultSettings(){
		options.setThreshold(DEFAULT_SIGNAL_THRESHOLD);
		options.setMinSize(DEFAULT_MIN_SIGNAL_SIZE);
		options.setMaxFraction(DEFAULT_MAX_SIGNAL_FRACTION);
		options.setMinCirc(DEFAULT_MIN_CIRC);
		options.setMaxCirc(DEFAULT_MAX_CIRC);
	}
	
	private void makePanel(){

		panel = new JPanel();
		panel.setBorder(BorderFactory.createTitledBorder("Signal detection"));
		panel.setLayout(new GridBagLayout());

		JLabel[] labels = new JLabel[5];
		JSpinner[] fields = new JSpinner[5];

		labels[0] = new JLabel("Signal threshold");
		labels[1] = new JLabel("Min signal size");
		labels[2] = new JLabel("Max signal fraction");
		labels[3] = new JLabel("Min signal circ");
		labels[4] = new JLabel("Max signal circ");

		fields[0] = thresholdSpinner;
		fields[1] = minSizeSpinner;
		fields[2] = maxFractSpinner;
		fields[3] = minCircSpinner;
		fields[4] = maxCircSpinner;

		thresholdSpinner.addChangeListener(this);
		minSizeSpinner.addChangeListener(this);
		maxFractSpinner.addChangeListener(this);
		minCircSpinner.addChangeListener(this);
		maxCircSpinner.addChangeListener(this);

		addLabelTextRows(labels, fields, new GridBagLayout(), panel );
	}
	
	@Override
	public void stateChanged(ChangeEvent e) {

		try{

			if(e.getSource()==thresholdSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();	
				this.options.setThreshold(  (Integer) j.getValue());
			}


			if(e.getSource()==minSizeSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				this.options.setMinSize(  (Integer) j.getValue());
			}
			
			if(e.getSource()==maxFractSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				this.options.setMaxFraction(  (Double) j.getValue());
			}
			
			if(e.getSource()==minCircSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				if((Double) j.getValue() > options.getMaxCirc()){
					j.setValue( maxCircSpinner.getValue() );
				}
				this.options.setMinCirc(  (Double) j.getValue());
			}
			
			if(e.getSource()==maxCircSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				
				if((Double) j.getValue()< options.getMinCirc()){
					j.setValue( minCircSpinner.getValue() );
				}
				this.options.setMaxCirc(  (Double) j.getValue());
			}

			
		} catch(Exception e1){
			//TODO
		}
	}

	private void addLabelTextRows(JLabel[] labels,
			JSpinner[] textFields,
			GridBagLayout gridbag,
			Container container) {
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.EAST;
		int numLabels = labels.length;

		for (int i = 0; i < numLabels; i++) {
			c.gridwidth = 1; //next-to-last
			c.fill = GridBagConstraints.NONE;      //reset to default
			c.weightx = 0.0;                       //reset to default
			container.add(labels[i], c);

			Dimension minSize = new Dimension(10, 5);
			Dimension prefSize = new Dimension(10, 5);
			Dimension maxSize = new Dimension(Short.MAX_VALUE, 5);
			c.gridwidth = GridBagConstraints.RELATIVE; //next-to-last
			c.fill = GridBagConstraints.NONE;      //reset to default
			c.weightx = 0.0;                       //reset to default
			container.add(new Box.Filler(minSize, prefSize, maxSize),c);

			c.gridwidth = GridBagConstraints.REMAINDER;     //end row
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
			container.add(textFields[i], c);
		}
	}

}
