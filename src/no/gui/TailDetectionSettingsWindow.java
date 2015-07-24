package no.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import utility.Constants;
import no.components.AnalysisOptions;

public class TailDetectionSettingsWindow extends SettingsDialog implements ActionListener {

	private static final long serialVersionUID = 1L;
	
	private JPanel contentPanel;
	
	private JComboBox<String> channelSelection;
	
	private CannyPanel cannyPanel;
	
	private AnalysisOptions options;
	
	private int channel;

	/**
	 * Create the dialog.
	 */
	public TailDetectionSettingsWindow(AnalysisOptions a) {
		
		setModal(true);
		this.options = a;
		createGUI();
		
		pack();
		setVisible(true);
	}
	private void createGUI(){
		setTitle("Tail detection");
		setBounds(100, 100, 450, 300);
		
		getContentPane().setLayout(new BorderLayout());
		
		contentPanel = new JPanel();

		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		channelSelection = new JComboBox<String>(channelOptionStrings);
		channelSelection.addActionListener(this);
		contentPanel.add(channelSelection);
		
		cannyPanel = new CannyPanel(options.getCannyOptions("tail"));
		contentPanel.add(cannyPanel);
		
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		
		getContentPane().add(makeLowerButtonPanel(), BorderLayout.SOUTH);
	}
	
	/**
	 * Create the panel with ok and cancel buttons
	 * @return the panel
	 */
	private JPanel makeLowerButtonPanel(){
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout());

		JButton btnOk = new JButton("OK");
		btnOk.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				TailDetectionSettingsWindow.this.setVisible(false);
			}
		});

		panel.add(btnOk);

		JButton btnCancel = new JButton("Cancel");
		btnCancel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				TailDetectionSettingsWindow.this.dispose();
			}
		});
		panel.add(btnCancel);
		return panel;
	}
	@Override
	public void actionPerformed(ActionEvent e) {

		JComboBox<String> cb = (JComboBox<String>)e.getSource();
        String channelName = (String)cb.getSelectedItem();
        
        channel = channelName.equals("Red") 
				? Constants.RGB_RED
						: channelName.equals("Green") 
						? Constants.RGB_GREEN
								: Constants.RGB_BLUE;
		
	}
		
	public int getChannel(){
		return this.channel;
	}

}
