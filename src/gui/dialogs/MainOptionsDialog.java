package gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import analysis.AnalysisDataset;
import gui.MainWindow;
import gui.components.ColourSelecter.ColourSwatch;

@SuppressWarnings("serial")
public class MainOptionsDialog extends SettingsDialog implements ActionListener {
	
	private MainWindow mw;
	
	private JComboBox<Level> levelBox;
	private JComboBox<ColourSwatch> colourBox;
	
	public MainOptionsDialog(MainWindow mw){
		super(mw.getProgramLogger());
		this.mw = mw;
		this.setLayout(new BorderLayout());
		this.setTitle("Options");
		
		
		this.add(createMainPanel(), BorderLayout.CENTER);
		this.add(createFooter(), BorderLayout.SOUTH);
		
		this.setLocationRelativeTo(null);
		this.setMinimumSize(new Dimension(100, 70));
		this.pack();
		this.setVisible(true);
				
	}
	
	private JPanel createMainPanel(){
		JPanel panel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		panel.setLayout(layout);
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		List<JLabel> labels = new ArrayList<JLabel>();
		List<Component> fields = new ArrayList<Component>();

		JLabel logLabel = new JLabel("Logging level");
		Level[] levelArray = { Level.INFO, Level.FINE, Level.FINEST };
		levelBox = new JComboBox<Level>(levelArray);
		levelBox.setSelectedItem(mw.getProgramLogger().getLevel());
		levelBox.addActionListener(this);
		
		labels.add(logLabel);
		fields.add(levelBox);
		
		JLabel swatchLabel = new JLabel("Colour swatch");
		colourBox = new JComboBox<ColourSwatch>(ColourSwatch.values());
		colourBox.setSelectedItem(mw.getColourSwatch());
		colourBox.addActionListener(this);
		
		labels.add(swatchLabel);
		fields.add(colourBox);
		
		this.addLabelTextRows(labels, fields, layout, panel);
		return panel;
	}


	@Override
	public void actionPerformed(ActionEvent arg0) {
		
		Level level = (Level) levelBox.getSelectedItem();
		if(!level.equals(mw.getProgramLogger().getLevel())){
			mw.getProgramLogger().setLevel(level);
			programLogger.log(Level.INFO, "Set the logging level to "+level.toString());
		}
		
		
		ColourSwatch swatch = (ColourSwatch) colourBox.getSelectedItem();
		
		if(! swatch.equals(mw.getColourSwatch())){

			for(AnalysisDataset d : mw.getPopulationsPanel().getAllDatasets()){
				d.setSwatch(swatch);
			}
			mw.setColourSwatch(swatch);
			programLogger.log(Level.INFO, "Set the colour swatch level to "+swatch.toString());
		}
		
		
	}

}
