/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package gui;

import ij.IJ;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import analysis.AnalysisOptions.CannyOptions;
import analysis.AnalysisOptions.NuclearSignalOptions;

/**
 * Contains methods for laying out panels in settings dialog options
 */
public abstract class SettingsDialog extends JDialog {
	
	private static final long serialVersionUID = 1L;
	
	String[] channelOptionStrings = {"Greyscale", "Red", "Green", "Blue"};

	protected void addLabelTextRows(JLabel[] labels,
			Component[] fields,
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
			container.add(fields[i], c);
		}
	}
	
	/**
	 * Create a panel with the standard Canny detection options. Must have an
	 * attached CannyOptions to store the settings
	 */
	public class CannyPanel extends JPanel implements ChangeListener, ActionListener{

		private static final long serialVersionUID = 1L;
				
		private JSpinner cannyLowThreshold;
		private JSpinner cannyHighThreshold;
		private JSpinner cannyKernelRadius;
		private JSpinner cannyKernelWidth;
		private JSpinner closingObjectRadiusSpinner;
		private JCheckBox cannyAutoThresholdCheckBox;
		
		private JCheckBox 	useKuwaharaCheckBox;
		private JSpinner 	kuwaharaRadiusSpinner;
		
		private JCheckBox 	flattenImageCheckBox;
		private JSpinner 	flattenImageThresholdSpinner;
		
		
		private CannyOptions options;
		
		public CannyPanel(CannyOptions options){
			this.options = options;
			getDefaults();
			createPanel();
		}
		
		/**
		 * Create the spinners with the default options in the CannyOptions
		 * CannyOptions must therefore have been assigned defaults
		 */
		private void getDefaults(){
			
			cannyLowThreshold = new JSpinner(new SpinnerNumberModel(options.getLowThreshold(),	0, 10, 0.05));
			cannyHighThreshold = new JSpinner(new SpinnerNumberModel(options.getHighThreshold(),	0, 20, 0.05));
			cannyKernelRadius = new JSpinner(new SpinnerNumberModel(options.getKernelRadius(),	0, 20, 0.05));
			cannyKernelWidth = new JSpinner(new SpinnerNumberModel(options.getKernelWidth(),	1, 50, 1));
			closingObjectRadiusSpinner = new JSpinner(new SpinnerNumberModel(options.getClosingObjectRadius(), 1,100 , 1));
			
			kuwaharaRadiusSpinner = new JSpinner(new SpinnerNumberModel(options.getKuwaharaKernel(), 1,11 , 2));
			flattenImageThresholdSpinner = new JSpinner(new SpinnerNumberModel(options.getFlattenThreshold(), 0,255 , 1));
			
		}
		
		
		private void createPanel(){
			
			this.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			Dimension minSize = new Dimension(10, 5);
			Dimension prefSize = new Dimension(10, 5);
			Dimension maxSize = new Dimension(Short.MAX_VALUE, 5);
			c.gridwidth = GridBagConstraints.REMAINDER; //next-to-last
			c.fill = GridBagConstraints.NONE;      //reset to default
			c.weightx = 0.1;                       //reset to default
			this.add(new Box.Filler(minSize, prefSize, maxSize),c);
			
			cannyAutoThresholdCheckBox = new JCheckBox("Canny auto threshold");
			cannyAutoThresholdCheckBox.setSelected(false);
			cannyAutoThresholdCheckBox.setActionCommand("CannyAutoThreshold");
			cannyAutoThresholdCheckBox.addActionListener(this);
			this.add(cannyAutoThresholdCheckBox);
			
			this.add(new Box.Filler(minSize, prefSize, maxSize),c);
			
			
			// add the canny settings
			List<JLabel> labelList	 = new ArrayList<JLabel>();
			List<JSpinner> fieldList = new ArrayList<JSpinner>();
			
			labelList.add(new JLabel("Canny low threshold"));
			labelList.add(new JLabel("Canny high threshold"));
			labelList.add(new JLabel("Canny kernel radius"));
			labelList.add(new JLabel("Canny kernel width"));
			labelList.add(new JLabel("Closing radius"));
			
			JLabel[] labels = labelList.toArray(new JLabel[0]);
			
			fieldList.add(cannyLowThreshold);
			fieldList.add(cannyHighThreshold);
			fieldList.add(cannyKernelRadius);
			fieldList.add(cannyKernelWidth);
			fieldList.add(closingObjectRadiusSpinner);
			
			JSpinner[] fields = fieldList.toArray(new JSpinner[0]);
			
			// add the change listeners
			cannyLowThreshold.addChangeListener(this);
			cannyHighThreshold.addChangeListener(this);
			cannyKernelRadius.addChangeListener(this);
			cannyKernelWidth.addChangeListener(this);
			closingObjectRadiusSpinner.addChangeListener(this);
						
			addLabelTextRows(labels, fields, new GridBagLayout(), this );
			
			// add a space
			this.add(new Box.Filler(minSize, prefSize, maxSize),c);
			
			// add the Kuwahara checkbox
			useKuwaharaCheckBox = new JCheckBox("Use Kuwahara filter");
			useKuwaharaCheckBox.setSelected(options.isUseKuwahara());
			useKuwaharaCheckBox.setActionCommand("UseKuwahara");
			useKuwaharaCheckBox.addActionListener(this);
			this.add(useKuwaharaCheckBox);
			this.add(new Box.Filler(minSize, prefSize, maxSize),c);
			
			// add the Kuwahara radius spinner
			labels = new JLabel[1];
			fields = new JSpinner[1];
			
			labels[0] = new JLabel("Kuwahara kernel");
			fields[0] = kuwaharaRadiusSpinner;
			
			addLabelTextRows(labels, fields, new GridBagLayout(), this );
			
			// add a space
			this.add(new Box.Filler(minSize, prefSize, maxSize),c);
			
			
			// add the chromocentre flattening checkbox
			flattenImageCheckBox = new JCheckBox("Flatten chromocentres");
			flattenImageCheckBox.setSelected(options.isUseKuwahara());
			flattenImageCheckBox.setActionCommand("FlattenChromocentres");
			flattenImageCheckBox.addActionListener(this);
			this.add(flattenImageCheckBox);
			this.add(new Box.Filler(minSize, prefSize, maxSize),c);
			
			// add the flattening threshold spinner
			labels = new JLabel[1];
			fields = new JSpinner[1];

			labels[0] = new JLabel("Flattening threshold");
			fields[0] = flattenImageThresholdSpinner;

			addLabelTextRows(labels, fields, new GridBagLayout(), this );
			
			
			
		}


		@Override
		public void stateChanged(ChangeEvent e) {
			try{
				if(e.getSource()==cannyLowThreshold){
					JSpinner j = (JSpinner) e.getSource();
					j.commitEdit();

					if( (Double) j.getValue() > (Double) cannyHighThreshold.getValue() ){
						j.setValue( cannyHighThreshold.getValue() );
					}
					Double doubleValue = (Double) j.getValue();
					options.setLowThreshold(    doubleValue.floatValue() );
				}

				if(e.getSource()==cannyHighThreshold){
					JSpinner j = (JSpinner) e.getSource();
					j.commitEdit();

					if( (Double) j.getValue() < (Double) cannyLowThreshold.getValue() ){
						j.setValue( cannyLowThreshold.getValue() );
					}
					Double doubleValue = (Double) j.getValue();
					options.setHighThreshold( doubleValue.floatValue() );
				}

				if(e.getSource()==cannyKernelRadius){
					JSpinner j = (JSpinner) e.getSource();
					j.commitEdit();
					Double doubleValue = (Double) j.getValue();
					options.setKernelRadius( doubleValue.floatValue());
				}

				if(e.getSource()==cannyKernelWidth){
					JSpinner j = (JSpinner) e.getSource();
					j.commitEdit();
					options.setKernelWidth( (Integer) j.getValue());
				}

				if(e.getSource()==closingObjectRadiusSpinner){
					JSpinner j = (JSpinner) e.getSource();
					j.commitEdit();
					options.setClosingObjectRadius( (Integer) j.getValue());
				}
				
				if(e.getSource()==kuwaharaRadiusSpinner){
					JSpinner j = (JSpinner) e.getSource();
					j.commitEdit();
					Integer value = (Integer) j.getValue();
					if(value % 2 == 0){ // even
						value--; // only odd values are allowed
						j.setValue(value);
						j.commitEdit();
						j.repaint();
					}
					options.setKuwaharaKernel(value);
				}
				
				if(e.getSource()==flattenImageThresholdSpinner){
					JSpinner j = (JSpinner) e.getSource();
					j.commitEdit();
					options.setFlattenThreshold( (Integer) j.getValue());
				}
				
			
			} catch (ParseException e1) {
				IJ.log("Parsing error in JSpinner");
			}
			
		}


		@Override
		public void actionPerformed(ActionEvent e) {
			if(e.getActionCommand().equals("CannyAutoThreshold")){

				if(cannyAutoThresholdCheckBox.isSelected()){
					options.setCannyAutoThreshold(true);
					cannyLowThreshold.setEnabled(false);
					cannyHighThreshold.setEnabled(false);
				} else {
					options.setCannyAutoThreshold(false);
					cannyLowThreshold.setEnabled(true);
					cannyHighThreshold.setEnabled(true);
				}
			}
			
			if(e.getActionCommand().equals("UseKuwahara")){

				if(useKuwaharaCheckBox.isSelected()){
					options.setUseKuwahara(true);
					kuwaharaRadiusSpinner.setEnabled(true);
				} else {
					options.setUseKuwahara(false);
					kuwaharaRadiusSpinner.setEnabled(false);
				}
			}
			
			if(e.getActionCommand().equals("FlattenChromocentres")){

				if(flattenImageCheckBox.isSelected()){
					options.setFlattenImage(true);
					flattenImageThresholdSpinner.setEnabled(true);
				} else {
					options.setFlattenImage(false);
					flattenImageThresholdSpinner.setEnabled(false);
				}
			}
			
		}
		
	}
	
	/**
	 * Create a panel with the standard signal detection options. Must have an
	 * attached CannyOptions to store the settings
	 */
	public class NuclearSignalPanel extends JPanel implements ChangeListener, ActionListener{
		
		private static final long serialVersionUID = 1L;
		
		private NuclearSignalOptions options;
				
		private JComboBox<String> channelSelection;
		private JTextField groupName;
		
		private JSpinner minSizeSpinner = new JSpinner(new SpinnerNumberModel(NuclearSignalOptions.DEFAULT_MIN_SIGNAL_SIZE,	1, 10000, 1));
		private JSpinner maxFractSpinner = new JSpinner(new SpinnerNumberModel(NuclearSignalOptions.DEFAULT_MAX_SIGNAL_FRACTION, 0, 1, 0.05));
		
		private JSpinner thresholdSpinner = new JSpinner(new SpinnerNumberModel(NuclearSignalOptions.DEFAULT_SIGNAL_THRESHOLD,	0, 255, 1));
		
		private JSpinner minCircSpinner = new JSpinner(new SpinnerNumberModel(NuclearSignalOptions.DEFAULT_MIN_CIRC,	0, 1, 0.05));
		private JSpinner maxCircSpinner = new JSpinner(new SpinnerNumberModel(NuclearSignalOptions.DEFAULT_MAX_CIRC,	0, 1, 0.05));
		
		public NuclearSignalPanel(NuclearSignalOptions options){
			this.options = options;
			createPanel();
		}
		
		private void createPanel(){
			
			this.setLayout(new GridBagLayout());
//			GridBagConstraints c = new GridBagConstraints();
//			Dimension minSize = new Dimension(10, 5);
//			Dimension prefSize = new Dimension(10, 5);
//			Dimension maxSize = new Dimension(Short.MAX_VALUE, 5);
//			c.gridwidth = GridBagConstraints.REMAINDER; //next-to-last
//			c.fill = GridBagConstraints.NONE;      //reset to default
//			c.weightx = 0.1;                       //reset to default
//			this.add(new Box.Filler(minSize, prefSize, maxSize),c);
//			
//			
//			
//			
//			this.add(new Box.Filler(minSize, prefSize, maxSize),c);
			
			
			// add the canny settings
			List<JLabel> labelList	 	= new ArrayList<JLabel>();
			List<JComponent> fieldList  = new ArrayList<JComponent>();
						
			labelList.add(new JLabel("Channel"));
			labelList.add(new JLabel("Signal threshold"));
			labelList.add(new JLabel("Min signal size"));
			labelList.add(new JLabel("Max signal fraction"));
			labelList.add(new JLabel("Min signal circ"));
			labelList.add(new JLabel("Max signal circ"));
			labelList.add(new JLabel("Group name"));
			
			JLabel[] labels = labelList.toArray(new JLabel[0]);
			
			fieldList.add(channelSelection);
			fieldList.add(thresholdSpinner);
			fieldList.add(minSizeSpinner);
			fieldList.add(maxFractSpinner);
			fieldList.add(minCircSpinner);
			fieldList.add(maxCircSpinner);
			fieldList.add(groupName);
			
			JComponent[] fields = fieldList.toArray(new JComponent[0]);
			
			// add the change listeners
			thresholdSpinner.addChangeListener(this);
			minSizeSpinner.addChangeListener(this);
			maxFractSpinner.addChangeListener(this);
			minCircSpinner.addChangeListener(this);
			maxCircSpinner.addChangeListener(this);
						
			addLabelTextRows(labels, fields, new GridBagLayout(), this );
			
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			try{

				if(e.getSource()==thresholdSpinner){
					JSpinner j = (JSpinner) e.getSource();
					j.commitEdit();	
				}


				if(e.getSource()==minSizeSpinner){
					JSpinner j = (JSpinner) e.getSource();
					j.commitEdit();
				}
				
				if(e.getSource()==maxFractSpinner){
					JSpinner j = (JSpinner) e.getSource();
					j.commitEdit();
				}
				
				if(e.getSource()==minCircSpinner){
					JSpinner j = (JSpinner) e.getSource();
					j.commitEdit();
					if((Double) j.getValue() > (Double) maxCircSpinner.getValue()   ){
						j.setValue( maxCircSpinner.getValue() );
					}
				}
				
				if(e.getSource()==maxCircSpinner){
					JSpinner j = (JSpinner) e.getSource();
					j.commitEdit();
					
					if((Double) j.getValue()< (Double) minCircSpinner.getValue()  ){
						j.setValue( minCircSpinner.getValue() );
					}
				}

			} catch(Exception e1){
				IJ.log("Error getting signal values: "+e1.getMessage());
				for(StackTraceElement e2 : e1.getStackTrace()){
					IJ.log(e2.toString());
				}
			}
			
		}
		
	}

}
