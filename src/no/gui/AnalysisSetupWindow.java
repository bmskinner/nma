package no.gui;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.FlowLayout;
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
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.border.EtchedBorder;

public class AnalysisSetupWindow extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField txtNucleusThreshold;
	private JTextField txtSignalThreshold;
	private JTextField txtMinNuclearSize;
	private JTextField txtMaxNuclearSize;
	private JTextField txtXOffset;
	private JTextField txtYOffset;
	private JTextField txtMinSignalSize;

	/**
	 * Launch the application.
	 */
//	public static void main(String[] args) {
//		EventQueue.invokeLater(new Runnable() {
//			public void run() {
//				try {
//					AnalysisSetupWindow frame = new AnalysisSetupWindow();
//					frame.setVisible(true);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		});
//	}

	/**
	 * Create the frame.
	 */
	public AnalysisSetupWindow() {
		setTitle("Create new analysis");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 450, 626);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		GridBagLayout gbl_contentPane = new GridBagLayout();
		gbl_contentPane.columnWidths = new int[]{424, 0};
		gbl_contentPane.rowHeights = new int[]{39, 0, 204, 0, 79, 0, 80, 30, 0};
		gbl_contentPane.columnWeights = new double[]{0.0, Double.MIN_VALUE};
		gbl_contentPane.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		contentPane.setLayout(gbl_contentPane);
		
		JPanel pnlNucleus = new JPanel();
		GridBagConstraints gbc_pnlNucleus = new GridBagConstraints();
		gbc_pnlNucleus.fill = GridBagConstraints.BOTH;
		gbc_pnlNucleus.insets = new Insets(0, 0, 5, 0);
		gbc_pnlNucleus.gridx = 0;
		gbc_pnlNucleus.gridy = 0;
		contentPane.add(pnlNucleus, gbc_pnlNucleus);
		pnlNucleus.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		JLabel lblNucleusType = new JLabel("Nucleus type");
		pnlNucleus.add(lblNucleusType);
		
		JComboBox comboBox = new JComboBox();
		pnlNucleus.add(comboBox);
		
		JLabel lblDetectionSettings = new JLabel("Detection settings");
		GridBagConstraints gbc_lblDetectionSettings = new GridBagConstraints();
		gbc_lblDetectionSettings.insets = new Insets(0, 0, 5, 0);
		gbc_lblDetectionSettings.gridx = 0;
		gbc_lblDetectionSettings.gridy = 1;
		contentPane.add(lblDetectionSettings, gbc_lblDetectionSettings);
		
		JPanel pnlDetection = new JPanel();
		pnlDetection.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		GridBagConstraints gbc_pnlDetection = new GridBagConstraints();
		gbc_pnlDetection.fill = GridBagConstraints.BOTH;
		gbc_pnlDetection.insets = new Insets(0, 0, 5, 0);
		gbc_pnlDetection.gridx = 0;
		gbc_pnlDetection.gridy = 2;
		contentPane.add(pnlDetection, gbc_pnlDetection);
		pnlDetection.setLayout(new GridLayout(0, 1, 0, 0));
		
		JPanel pnlThreshold = new JPanel();
		pnlThreshold.setBorder(null);
		pnlDetection.add(pnlThreshold);
		GridBagLayout gbl_pnlThreshold = new GridBagLayout();
		gbl_pnlThreshold.columnWidths = new int[]{0, 0, 0};
		gbl_pnlThreshold.rowHeights = new int[]{0, 0};
		gbl_pnlThreshold.columnWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
		gbl_pnlThreshold.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		pnlThreshold.setLayout(gbl_pnlThreshold);
		
		JLabel lblNucleusThreshold = new JLabel("Nucleus threshold");
		GridBagConstraints gbc_lblNucleusThreshold = new GridBagConstraints();
		gbc_lblNucleusThreshold.insets = new Insets(0, 0, 0, 5);
		gbc_lblNucleusThreshold.gridx = 0;
		gbc_lblNucleusThreshold.gridy = 0;
		pnlThreshold.add(lblNucleusThreshold, gbc_lblNucleusThreshold);
		
		txtNucleusThreshold = new JTextField();
		GridBagConstraints gbc_txtNucleusThreshold = new GridBagConstraints();
		gbc_txtNucleusThreshold.gridx = 1;
		gbc_txtNucleusThreshold.gridy = 0;
		pnlThreshold.add(txtNucleusThreshold, gbc_txtNucleusThreshold);
		txtNucleusThreshold.setText("Nucleus threshold");
		txtNucleusThreshold.setColumns(10);
		
		JPanel pnlMinSize = new JPanel();
		pnlMinSize.setBorder(null);
		pnlDetection.add(pnlMinSize);
		GridBagLayout gbl_pnlMinSize = new GridBagLayout();
		gbl_pnlMinSize.columnWidths = new int[]{0, 0, 0};
		gbl_pnlMinSize.rowHeights = new int[]{0, 0};
		gbl_pnlMinSize.columnWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
		gbl_pnlMinSize.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		pnlMinSize.setLayout(gbl_pnlMinSize);
		
		JLabel lblMinNuclearSize = new JLabel("Min nuclear size");
		GridBagConstraints gbc_lblMinNuclearSize = new GridBagConstraints();
		gbc_lblMinNuclearSize.insets = new Insets(0, 0, 0, 5);
		gbc_lblMinNuclearSize.gridx = 0;
		gbc_lblMinNuclearSize.gridy = 0;
		pnlMinSize.add(lblMinNuclearSize, gbc_lblMinNuclearSize);
		
		txtMinNuclearSize = new JTextField();
		GridBagConstraints gbc_txtMinNuclearSize = new GridBagConstraints();
		gbc_txtMinNuclearSize.gridx = 1;
		gbc_txtMinNuclearSize.gridy = 0;
		pnlMinSize.add(txtMinNuclearSize, gbc_txtMinNuclearSize);
		txtMinNuclearSize.setText("Min nuclear size");
		txtMinNuclearSize.setColumns(10);
		
		JPanel panel = new JPanel();
		panel.setBorder(null);
		pnlDetection.add(panel);
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[]{0, 0, 0};
		gbl_panel.rowHeights = new int[]{0, 0};
		gbl_panel.columnWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
		gbl_panel.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		panel.setLayout(gbl_panel);
		
		JLabel lblMaxNuclearSize = new JLabel("Max nuclear size");
		GridBagConstraints gbc_lblMaxNuclearSize = new GridBagConstraints();
		gbc_lblMaxNuclearSize.insets = new Insets(0, 0, 0, 5);
		gbc_lblMaxNuclearSize.gridx = 0;
		gbc_lblMaxNuclearSize.gridy = 0;
		panel.add(lblMaxNuclearSize, gbc_lblMaxNuclearSize);
		
		txtMaxNuclearSize = new JTextField();
		GridBagConstraints gbc_txtMaxNuclearSize = new GridBagConstraints();
		gbc_txtMaxNuclearSize.gridx = 1;
		gbc_txtMaxNuclearSize.gridy = 0;
		panel.add(txtMaxNuclearSize, gbc_txtMaxNuclearSize);
		txtMaxNuclearSize.setText("Max nuclear size");
		txtMaxNuclearSize.setColumns(10);
		
		JPanel panel_1 = new JPanel();
		panel_1.setBorder(null);
		pnlDetection.add(panel_1);
		GridBagLayout gbl_panel_1 = new GridBagLayout();
		gbl_panel_1.columnWidths = new int[]{0, 0, 0};
		gbl_panel_1.rowHeights = new int[]{0, 0};
		gbl_panel_1.columnWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
		gbl_panel_1.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		panel_1.setLayout(gbl_panel_1);
		
		JLabel lblMinNuclearCirc = new JLabel("Min nuclear circ");
		GridBagConstraints gbc_lblMinNuclearCirc = new GridBagConstraints();
		gbc_lblMinNuclearCirc.insets = new Insets(0, 0, 0, 5);
		gbc_lblMinNuclearCirc.gridx = 0;
		gbc_lblMinNuclearCirc.gridy = 0;
		panel_1.add(lblMinNuclearCirc, gbc_lblMinNuclearCirc);
		
		JSlider slider = new JSlider();
		GridBagConstraints gbc_slider = new GridBagConstraints();
		gbc_slider.gridx = 1;
		gbc_slider.gridy = 0;
		panel_1.add(slider, gbc_slider);
		slider.setValue(0);
		slider.setMinorTickSpacing(20);
		
		JPanel panel_2 = new JPanel();
		panel_2.setBorder(null);
		pnlDetection.add(panel_2);
		GridBagLayout gbl_panel_2 = new GridBagLayout();
		gbl_panel_2.columnWidths = new int[]{0, 0, 0};
		gbl_panel_2.rowHeights = new int[]{0, 0};
		gbl_panel_2.columnWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
		gbl_panel_2.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		panel_2.setLayout(gbl_panel_2);
		
		JLabel lblMaxNuclearCirc = new JLabel("Max nuclear circ");
		GridBagConstraints gbc_lblMaxNuclearCirc = new GridBagConstraints();
		gbc_lblMaxNuclearCirc.insets = new Insets(0, 0, 0, 5);
		gbc_lblMaxNuclearCirc.gridx = 0;
		gbc_lblMaxNuclearCirc.gridy = 0;
		panel_2.add(lblMaxNuclearCirc, gbc_lblMaxNuclearCirc);
		
		JSlider slider_1 = new JSlider();
		GridBagConstraints gbc_slider_1 = new GridBagConstraints();
		gbc_slider_1.gridx = 1;
		gbc_slider_1.gridy = 0;
		panel_2.add(slider_1, gbc_slider_1);
		slider_1.setValue(100);
		
		JPanel panel_3 = new JPanel();
		panel_3.setBorder(null);
		pnlDetection.add(panel_3);
		GridBagLayout gbl_panel_3 = new GridBagLayout();
		gbl_panel_3.columnWidths = new int[]{0, 0, 0};
		gbl_panel_3.rowHeights = new int[]{0, 0, 0};
		gbl_panel_3.columnWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
		gbl_panel_3.rowWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		panel_3.setLayout(gbl_panel_3);
		
		JLabel lblSignalThreshold = new JLabel("Signal threshold");
		GridBagConstraints gbc_lblSignalThreshold = new GridBagConstraints();
		gbc_lblSignalThreshold.insets = new Insets(0, 0, 5, 5);
		gbc_lblSignalThreshold.gridx = 0;
		gbc_lblSignalThreshold.gridy = 0;
		panel_3.add(lblSignalThreshold, gbc_lblSignalThreshold);
		
		txtSignalThreshold = new JTextField();
		GridBagConstraints gbc_txtSignalThreshold = new GridBagConstraints();
		gbc_txtSignalThreshold.insets = new Insets(0, 0, 5, 0);
		gbc_txtSignalThreshold.gridx = 1;
		gbc_txtSignalThreshold.gridy = 0;
		panel_3.add(txtSignalThreshold, gbc_txtSignalThreshold);
		txtSignalThreshold.setText("Signal threshold");
		txtSignalThreshold.setColumns(10);
		
		JPanel panel_4 = new JPanel();
		panel_4.setBorder(null);
		pnlDetection.add(panel_4);
		GridBagLayout gbl_panel_4 = new GridBagLayout();
		gbl_panel_4.columnWidths = new int[]{0, 0, 0};
		gbl_panel_4.rowHeights = new int[]{0, 0};
		gbl_panel_4.columnWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
		gbl_panel_4.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		panel_4.setLayout(gbl_panel_4);
		
		JLabel lblMinSignalSize = new JLabel("Min signal size");
		GridBagConstraints gbc_lblMinSignalSize = new GridBagConstraints();
		gbc_lblMinSignalSize.insets = new Insets(0, 0, 0, 5);
		gbc_lblMinSignalSize.anchor = GridBagConstraints.EAST;
		gbc_lblMinSignalSize.gridx = 0;
		gbc_lblMinSignalSize.gridy = 0;
		panel_4.add(lblMinSignalSize, gbc_lblMinSignalSize);
		
		txtMinSignalSize = new JTextField();
		txtMinSignalSize.setText("Min signal size");
		GridBagConstraints gbc_txtMinSignalSize = new GridBagConstraints();
		gbc_txtMinSignalSize.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtMinSignalSize.gridx = 1;
		gbc_txtMinSignalSize.gridy = 0;
		panel_4.add(txtMinSignalSize, gbc_txtMinSignalSize);
		txtMinSignalSize.setColumns(10);
		
		JPanel panel_5 = new JPanel();
		panel_5.setBorder(null);
		pnlDetection.add(panel_5);
		
		JLabel lblMaxSignalFraction = new JLabel("Max signal fraction");
		panel_5.add(lblMaxSignalFraction);
		
		JSlider slider_2 = new JSlider();
		panel_5.add(slider_2);
		
		JLabel lblRefoldingSettings = new JLabel("Refolding settings");
		GridBagConstraints gbc_lblRefoldingSettings = new GridBagConstraints();
		gbc_lblRefoldingSettings.insets = new Insets(0, 0, 5, 0);
		gbc_lblRefoldingSettings.gridx = 0;
		gbc_lblRefoldingSettings.gridy = 3;
		contentPane.add(lblRefoldingSettings, gbc_lblRefoldingSettings);
		
		JPanel pnlRefolding = new JPanel();
		pnlRefolding.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		GridBagConstraints gbc_pnlRefolding = new GridBagConstraints();
		gbc_pnlRefolding.fill = GridBagConstraints.BOTH;
		gbc_pnlRefolding.insets = new Insets(0, 0, 5, 0);
		gbc_pnlRefolding.gridx = 0;
		gbc_pnlRefolding.gridy = 4;
		contentPane.add(pnlRefolding, gbc_pnlRefolding);
		pnlRefolding.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		JCheckBox chckbxRefoldNucleus = new JCheckBox("Refold nucleus?");
		pnlRefolding.add(chckbxRefoldNucleus);
		
		JLabel lblRefoldingMode = new JLabel("Refolding mode");
		pnlRefolding.add(lblRefoldingMode);
		
		JRadioButton rdbtnFast = new JRadioButton("Fast");
		pnlRefolding.add(rdbtnFast);
		
		JRadioButton rdbtnIntensive = new JRadioButton("Intensive");
		pnlRefolding.add(rdbtnIntensive);
		
		JRadioButton rdbtnBrutal = new JRadioButton("Brutal");
		pnlRefolding.add(rdbtnBrutal);
		
		JLabel lblRemappingSettings = new JLabel("Remapping settings");
		GridBagConstraints gbc_lblRemappingSettings = new GridBagConstraints();
		gbc_lblRemappingSettings.insets = new Insets(0, 0, 5, 0);
		gbc_lblRemappingSettings.gridx = 0;
		gbc_lblRemappingSettings.gridy = 5;
		contentPane.add(lblRemappingSettings, gbc_lblRemappingSettings);
		
		JPanel pnlRemapping = new JPanel();
		GridBagConstraints gbc_pnlRemapping = new GridBagConstraints();
		gbc_pnlRemapping.fill = GridBagConstraints.BOTH;
		gbc_pnlRemapping.insets = new Insets(0, 0, 5, 0);
		gbc_pnlRemapping.gridx = 0;
		gbc_pnlRemapping.gridy = 6;
		contentPane.add(pnlRemapping, gbc_pnlRemapping);
		pnlRemapping.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		JCheckBox chckbxRemappingAnalysis = new JCheckBox("Remapping analysis?");
		pnlRemapping.add(chckbxRemappingAnalysis);
		
		JLabel lblOffsetX = new JLabel("Offset X");
		pnlRemapping.add(lblOffsetX);
		
		txtXOffset = new JTextField();
		pnlRemapping.add(txtXOffset);
		txtXOffset.setText("X offset");
		txtXOffset.setColumns(10);
		
		JLabel lblOffsetY = new JLabel("Offset Y");
		pnlRemapping.add(lblOffsetY);
		
		txtYOffset = new JTextField();
		pnlRemapping.add(txtYOffset);
		txtYOffset.setText("Y offset");
		txtYOffset.setColumns(10);
		
		JCheckBox chckbxDynamicallyRealignEach = new JCheckBox("Dynamically realign each image");
		pnlRemapping.add(chckbxDynamicallyRealignEach);
		
		JPanel pnlButtons = new JPanel();
		GridBagConstraints gbc_pnlButtons = new GridBagConstraints();
		gbc_pnlButtons.fill = GridBagConstraints.BOTH;
		gbc_pnlButtons.gridx = 0;
		gbc_pnlButtons.gridy = 7;
		contentPane.add(pnlButtons, gbc_pnlButtons);
		
		JButton btnOk = new JButton("OK");
		btnOk.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				// fetch settings, run analysis
			}
		});
		pnlButtons.setLayout(new GridLayout(0, 2, 0, 0));
		pnlButtons.add(btnOk);
		
		JButton btnCancel = new JButton("Cancel");
		pnlButtons.add(btnCancel);
		
		setVisible(true);
	}

}
