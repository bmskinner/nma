package no.gui;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import java.awt.FlowLayout;

import javax.swing.JDialog;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JRadioButton;
import javax.swing.JButton;
import javax.swing.JComboBox;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;

import java.awt.Container;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.border.EtchedBorder;

public class AnalysisSetupWindow extends JDialog {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField txtNucleusThreshold;
	private JTextField txtSignalThreshold;
	private JTextField txtMinNuclearSize;
	private JTextField txtMaxNuclearSize;
	private JTextField txtXOffset;
	private JTextField txtYOffset;
	private JTextField txtMinSignalSize;
	
	private JSpinner minSignalSizeSpinner;
	private JSpinner maxSignalFractSpinner;
	
	private String[] nucleusTypes = { "Round nucleus", "Rodent sperm", "Pig sperm"};
	private JComboBox nucleusSelectionBox;
	

	/**
	 * Create the frame.
	 */
	public AnalysisSetupWindow() {
		setTitle("Create new analysis");
		setBounds(100, 100, 450, 626);
		contentPane = new JPanel();
		contentPane.setLayout(new BorderLayout());
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);

		// add the combo box for nucleus type
		contentPane.add(makeNucleusTypePanel(), BorderLayout.NORTH);
		
		// add the buttons
		contentPane.add(makeLowerButtonPanel(), BorderLayout.SOUTH);
		

		// add the other settings
		JPanel settingsPanel = new JPanel();
		settingsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
		
		settingsPanel.add(makeDetectionSettingsPanel());
		
		
		contentPane.add(settingsPanel, BorderLayout.CENTER);
		

		
//		JPanel pnlThreshold = new JPanel();
//		JLabel lblNucleusThreshold = new JLabel("Nucleus threshold");
//
//		
//		txtNucleusThreshold = new JTextField();
//
//		txtNucleusThreshold.setText("Nucleus threshold");
//		txtNucleusThreshold.setColumns(10);
		
		JPanel pnlMinSize = new JPanel();
		pnlMinSize.setBorder(null);
////		pnlDetection.add(pnlMinSize);
//
//		
//		JLabel lblMinNuclearSize = new JLabel("Min nuclear size");
//
//		
//		txtMinNuclearSize = new JTextField();
//
//		txtMinNuclearSize.setText("Min nuclear size");
//		txtMinNuclearSize.setColumns(10);
//		
//		JPanel panel = new JPanel();
//		panel.setBorder(null);
//		pnlDetection.add(panel);
//
//		
//		JLabel lblMaxNuclearSize = new JLabel("Max nuclear size");
//
//		
//		txtMaxNuclearSize = new JTextField();
//
//		txtMaxNuclearSize.setText("Max nuclear size");
//		txtMaxNuclearSize.setColumns(10);
//		
//		JPanel panel_1 = new JPanel();
//		panel_1.setBorder(null);
//		pnlDetection.add(panel_1);
//
//		
//		JLabel lblMinNuclearCirc = new JLabel("Min nuclear circ");
//
//		panel_1.add(lblMinNuclearCirc);
//		
//		JSlider slider = new JSlider();
//		GridBagConstraints gbc_slider = new GridBagConstraints();
//		gbc_slider.gridx = 1;
//		gbc_slider.gridy = 0;
//		panel_1.add(slider, gbc_slider);
//		slider.setValue(0);
//		slider.setMinorTickSpacing(20);
//		
//		JPanel panel_2 = new JPanel();
//		panel_2.setBorder(null);
//		pnlDetection.add(panel_2);
//
//
//		
//		JLabel lblMaxNuclearCirc = new JLabel("Max nuclear circ");
//
//		panel_2.add(lblMaxNuclearCirc);
//		
//		JSlider slider_1 = new JSlider();
//
//		slider_1.setValue(100);
//		
//		JPanel panel_3 = new JPanel();
//		panel_3.setBorder(null);
//		pnlDetection.add(panel_3);
//
//
//		
//		JLabel lblSignalThreshold = new JLabel("Signal threshold");
//
//		panel_3.add(lblSignalThreshold );
//		
//		txtSignalThreshold = new JTextField();
//		
//		txtSignalThreshold.setText("Signal threshold");
//		txtSignalThreshold.setColumns(10);
//		
//		JPanel panel_4 = new JPanel();
//		panel_4.setBorder(null);
//		pnlDetection.add(panel_4);
//		
//
//		
//		JLabel lblMinSignalSize = new JLabel("Min signal size");
//		
//		panel_4.add(lblMinSignalSize);
//		
//		txtMinSignalSize = new JTextField();
//		txtMinSignalSize.setText("Min signal size");
//		
//
//		txtMinSignalSize.setColumns(10);
//		
//		JPanel panel_5 = new JPanel();
//		panel_5.setBorder(null);
//		pnlDetection.add(panel_5);
//		
//		JLabel lblMaxSignalFraction = new JLabel("Max signal fraction");
//		panel_5.add(lblMaxSignalFraction);
//		
//		JSlider slider_2 = new JSlider();
//		panel_5.add(slider_2);
//		
//		JLabel lblRefoldingSettings = new JLabel("Refolding settings");
//		
//		
//		JPanel pnlRefolding = new JPanel();
//		pnlRefolding.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
//		
//		pnlRefolding.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
//		
//		JCheckBox chckbxRefoldNucleus = new JCheckBox("Refold nucleus?");
//		pnlRefolding.add(chckbxRefoldNucleus);
//		
//		JLabel lblRefoldingMode = new JLabel("Refolding mode");
//		pnlRefolding.add(lblRefoldingMode);
//		
//		JRadioButton rdbtnFast = new JRadioButton("Fast");
//		pnlRefolding.add(rdbtnFast);
//		
//		JRadioButton rdbtnIntensive = new JRadioButton("Intensive");
//		pnlRefolding.add(rdbtnIntensive);
//		
//		JRadioButton rdbtnBrutal = new JRadioButton("Brutal");
//		pnlRefolding.add(rdbtnBrutal);
//		
//		JLabel lblRemappingSettings = new JLabel("Remapping settings");
//		GridBagConstraints gbc_lblRemappingSettings = new GridBagConstraints();
//		gbc_lblRemappingSettings.insets = new Insets(0, 0, 5, 0);
//		gbc_lblRemappingSettings.gridx = 0;
//		gbc_lblRemappingSettings.gridy = 5;
//		contentPane.add(lblRemappingSettings, gbc_lblRemappingSettings);
//		
//		JPanel pnlRemapping = new JPanel();
//		GridBagConstraints gbc_pnlRemapping = new GridBagConstraints();
//		gbc_pnlRemapping.fill = GridBagConstraints.BOTH;
//		gbc_pnlRemapping.insets = new Insets(0, 0, 5, 0);
//		gbc_pnlRemapping.gridx = 0;
//		gbc_pnlRemapping.gridy = 6;
//		contentPane.add(pnlRemapping, gbc_pnlRemapping);
//		pnlRemapping.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
//		
//		JCheckBox chckbxRemappingAnalysis = new JCheckBox("Remapping analysis?");
//		pnlRemapping.add(chckbxRemappingAnalysis);
//		
//		JLabel lblOffsetX = new JLabel("Offset X");
//		pnlRemapping.add(lblOffsetX);
//		
//		txtXOffset = new JTextField();
//		pnlRemapping.add(txtXOffset);
//		txtXOffset.setText("X offset");
//		txtXOffset.setColumns(10);
//		
//		JLabel lblOffsetY = new JLabel("Offset Y");
//		pnlRemapping.add(lblOffsetY);
//		
//		txtYOffset = new JTextField();
//		pnlRemapping.add(txtYOffset);
//		txtYOffset.setText("Y offset");
//		txtYOffset.setColumns(10);
//		
//		JCheckBox chckbxDynamicallyRealignEach = new JCheckBox("Dynamically realign each image");
//		pnlRemapping.add(chckbxDynamicallyRealignEach);

		pack();
		setVisible(true);
	}
	
	private JPanel makeNucleusTypePanel(){
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.CENTER));

//		JLabel lblNucleusType = new JLabel("Nucleus type");
		panel.add(new JLabel("Nucleus type"));

		nucleusSelectionBox = new JComboBox(nucleusTypes);
		nucleusSelectionBox.setSelectedIndex(0);
		panel.add(nucleusSelectionBox);
		return panel;
	}
	
	private JPanel makeLowerButtonPanel(){
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout());

		JButton btnOk = new JButton("OK");
		btnOk.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				// make invisible
			}
		});
		
		panel.add(btnOk);

		JButton btnCancel = new JButton("Cancel");
		btnCancel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				AnalysisSetupWindow.this.dispose();
			}
		});
		panel.add(btnCancel);
		return panel;
	}
	
	private JPanel makeDetectionSettingsPanel(){

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		JLabel lblDetectionSettings = new JLabel("Detection settings");
		panel.add(lblDetectionSettings);
		
		JLabel[] labels = new JLabel[5];
		JTextField[] fields = new JTextField[5];
		
		labels[0] = new JLabel("Nucleus threshold");
		labels[1] = new JLabel("Signal threshold");
		labels[2] = new JLabel("Min size");
		labels[3] = new JLabel("Max size");
		labels[4] = new JLabel("Min circ");
		
		for(int i=0;i<fields.length;i++){
			fields[i] = new JTextField();
		}
		
		JPanel pnlThreshold = new JPanel();
		addLabelTextRows(labels, fields, new GridBagLayout(), pnlThreshold );
		panel.add(pnlThreshold);
//		
//		JLabel lblNucleusThreshold = new JLabel("Nucleus threshold");
//
//		txtNucleusThreshold = new JTextField();
//		txtNucleusThreshold.setText("Nucleus threshold");
//		txtNucleusThreshold.setColumns(10);
	
		return panel;
	}
	
	private void addLabelTextRows(JLabel[] labels,
			JTextField[] textFields,
			GridBagLayout gridbag,
			Container container) {
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.EAST;
		int numLabels = labels.length;

		for (int i = 0; i < numLabels; i++) {
			c.gridwidth = GridBagConstraints.RELATIVE; //next-to-last
			c.fill = GridBagConstraints.NONE;      //reset to default
			c.weightx = 0.0;                       //reset to default
			container.add(labels[i], c);

			c.gridwidth = GridBagConstraints.REMAINDER;     //end row
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
			container.add(textFields[i], c);
		}
	}

}
