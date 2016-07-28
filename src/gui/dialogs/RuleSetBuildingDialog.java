package gui.dialogs;

import gui.LoadingIconDialog;
import gui.components.panels.ProfileTypeOptionsPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import logging.Loggable;
import sun.awt.SunHints.Value;
import components.generic.BorderTag;
import components.generic.ProfileType;
import analysis.profiles.Rule;
import analysis.profiles.Rule.RuleType;
import analysis.profiles.RuleSet;
import analysis.profiles.RuleSetCollection;

@SuppressWarnings("serial")
public class RuleSetBuildingDialog extends LoadingIconDialog implements Loggable {
			
	private boolean isOK = false;
	
	private JPanel mainPanel;
		
	public RuleSetBuildingDialog(){
		super();
		this.setTitle("Ruleset Builder");
		this.setModal(true);
		
		createUI();

		Dimension dim = new Dimension(500, 400);
		this.setMinimumSize(dim);
		this.pack();
		this.centerOnScreen();
		this.setVisible(true);
	}
	
	public void createUI(){
		
		this.setLayout(new BorderLayout());
		
		JPanel header = createHeader();
		this.add(header, BorderLayout.NORTH);
		
		mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(mainPanel);
		
		this.add(scrollPane, BorderLayout.CENTER);
		
		JPanel footer = createFooter();
		this.add(footer, BorderLayout.SOUTH);
		
	}
	
	private JPanel createHeader(){
		JPanel panel = new JPanel(new FlowLayout());
		
		JButton addButton = new JButton("Add RuleSet");
		
		addButton.addActionListener( e -> { 
			addRuleSet();
		} );
		
		panel.add(addButton);
		
		return panel;
	}
	
	
	private void addRuleSet(){
		
		// Add a panel to the main panel with a profile type, and button to add a rule
		RuleSetPanel ruleSetPanel = new RuleSetPanel();	
		ruleSetPanel.setBorder(BorderFactory.createTitledBorder("Ruleset"));
		mainPanel.add(ruleSetPanel);
		mainPanel.add(Box.createVerticalGlue());
		mainPanel.revalidate();
		mainPanel.repaint();
	}
	
	
 	private JPanel createFooter(){
		JPanel panel = new JPanel(new FlowLayout());
		
		
		JButton okButton = new JButton("OK");
		
		okButton.addActionListener( e -> { 
			isOK = true;
			setVisible(false);
		} );
		
		
		JButton cancelButton = new JButton("Cancel");
		
		cancelButton.addActionListener( e -> { 
			isOK = false;
			setVisible(false);
		} );
		
		
		panel.add(okButton);
		panel.add(cancelButton);
		return panel;
		
	}
	
	
	public boolean isOK(){
		return isOK;
	}
	
	public RuleSetCollection getCollection(){
		RuleSetCollection collection = new RuleSetCollection();
		for(Component c : mainPanel.getComponents()){
			if(c.getClass().isAssignableFrom(RuleSetPanel.class)){
//				log("Found ruleset panel, fetching ruleset");
				RuleSetPanel panel = (RuleSetPanel) c;
				collection.addRuleSet(BorderTag.REFERENCE_POINT, panel.getRuleSet());
				
			}
			
		}
//		log("Added all rulesets");
		
		return collection;
	}
	
	public class RuleSetPanel extends JPanel {
		
		private JPanel header;
		private JPanel main;
		
		private ProfileTypeOptionsPanel profileOptions = new ProfileTypeOptionsPanel();
		List<RulePanel> rules = new ArrayList<RulePanel>();
		
		public RuleSetPanel(){
			
			this.setLayout(new BorderLayout());
			
			main = new JPanel();
			main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
			
			header = new JPanel(new FlowLayout());			
			header.add(profileOptions);
			
			JButton addRuleButton = new JButton("Add rule");
			
			addRuleButton.addActionListener( e -> { 
				addRule();
			} );
			
			JButton deleteRuleSetButton = new JButton("Remove ruleset");
			
			deleteRuleSetButton.addActionListener( e -> { 
				mainPanel.remove(this);
				mainPanel.revalidate();
				mainPanel.repaint();
			} );
			
			header.add(addRuleButton);
			header.add(deleteRuleSetButton);
			
			this.add(main, BorderLayout.CENTER);
			this.add(header, BorderLayout.NORTH);
			
		}
		
		private void addRule(){
			
			RulePanel r = new RulePanel(this);
			rules.add(r);
			main.add(r);
			main.add(Box.createVerticalStrut(5)) ;
			
			this.revalidate();
			this.repaint();
		}
		
		private void removeRule(RulePanel r){
			rules.remove(r);
			main.remove(r);
			this.revalidate();
			this.repaint();
		}
		
									
		public RuleSet getRuleSet(){
//			log("Getting ruleset");
			RuleSet r = new RuleSet(profileOptions.getSelected());
			for(RulePanel panel : rules){
				Rule rule = panel.getRule();
//				log("  Adding rule "+rule.toString());
				r.addRule(rule);
			}
//			log("Ruleset final:");
//			log(r.toString());
			return r;
		}
		
	};
	
	public class RulePanel extends JPanel implements ActionListener{
		
		private JComboBox<RuleType> typeBox = new JComboBox<RuleType>(RuleType.values());
		
		private final Dimension SPINNER_DIMENSION = new Dimension(80, 20);
		
		private final Dimension PANEL_MAX_DIMENSION = new Dimension(500, 50);
		private final Dimension PANEL_MIN_DIMENSION = new Dimension(300, 50);
				
		private RuleSetPanel parent;
		
		List<JSpinner> spinners = new ArrayList<JSpinner>();
		
		public RulePanel(RuleSetPanel parent){
			this.parent = parent;
			
			this.setLayout(new FlowLayout(FlowLayout.LEFT));
			
			JButton removeButton = new JButton("-");
			removeButton.addActionListener( e -> {
				this.parent.removeRule(this);
			});
			this.add(removeButton);
			
			this.add(typeBox);
			
			typeBox.setSelectedItem(RuleType.IS_MINIMUM);
			
			JSpinner s = new JSpinner(new SpinnerNumberModel(1,	0, 1, 1));
			s.setToolTipText("1=True; 0=False");
			spinners.add(   s   );
			
			for(JSpinner spinner : spinners){
				spinner.setPreferredSize(SPINNER_DIMENSION);
				this.add(spinner);
			}
			
			typeBox.addActionListener(this);
			
			this.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
			this.setMaximumSize(PANEL_MAX_DIMENSION);
			this.setMinimumSize(PANEL_MIN_DIMENSION);
			this.setPreferredSize(PANEL_MIN_DIMENSION);
		}
		
		public Rule getRule(){
			Rule result = null;
			try {
								
				List<Double> values = new ArrayList<Double>();
				
				for(JSpinner spinner : spinners){
					spinner.commitEdit();
					double value = ((SpinnerNumberModel )spinner.getModel()).getNumber().doubleValue();
					values.add(value);
				}


				double first = values.get(0);
				RuleType type = (RuleType) typeBox.getSelectedItem();
				result = new Rule(type, first);

				for(int i=1; i<values.size(); i++){
					result.addValue( values.get(i)  ); 
				}
				
			} catch (ParseException e) {
				error("Error parsing spinner", e);
			} catch (Exception e){
				error("Other error in spinners", e);
			}
			return  result;
		}


		@Override
		public void actionPerformed(ActionEvent e) {
						
			for(JSpinner spinner : spinners){
				this.remove(spinner);
			}
			
			spinners = new ArrayList<JSpinner>();
			RuleType type = (RuleType) typeBox.getSelectedItem();
			
			switch(  type ){
			
				case IS_MINIMUM:{
					
					JSpinner spinner = new JSpinner(new SpinnerNumberModel(1,	0, 1, 1));
					spinner.setToolTipText("1=True; 0=False");
					spinners.add(   spinner   );
					break;
				}
				
				case IS_MAXIMUM:{
					JSpinner spinner = new JSpinner(new SpinnerNumberModel(1,	0, 1, 1));
					spinner.setToolTipText("1=True; 0=False");
					spinners.add(   spinner   );
					break;
				}
				
				case IS_LOCAL_MINIMUM:{
					
					JSpinner spinner1 =  new JSpinner(new SpinnerNumberModel(1,	0, 1, 1));
					JSpinner spinner2 =  new JSpinner(new SpinnerNumberModel(5,	1, 100, 1));
					spinner1.setToolTipText("1=True; 0=False");
					spinner2.setToolTipText("Smoothing window size");
					spinners.add(  spinner1  );
					spinners.add(  spinner2    );
					break;
				}
				
				case IS_LOCAL_MAXIMUM:{
					JSpinner spinner1 =  new JSpinner(new SpinnerNumberModel(1,	0, 1, 1));
					JSpinner spinner2 =  new JSpinner(new SpinnerNumberModel(5,	1, 100, 1));
					spinner1.setToolTipText("1=True; 0=False");
					spinner2.setToolTipText("Window size");
					spinners.add(  spinner1  );
					spinners.add(  spinner2    );
					break;
				}
				
				case VALUE_IS_LESS_THAN:{
					JSpinner spinner = new JSpinner();
					spinner.setToolTipText("Value");
					spinners.add(   spinner   );
					break;
				}
				
				case VALUE_IS_MORE_THAN:{
					JSpinner spinner = new JSpinner();
					spinner.setToolTipText("Value");
					spinners.add(   spinner   );
					break;
				}
				
				case INDEX_IS_LESS_THAN:{
					JSpinner spinner = new JSpinner(new SpinnerNumberModel(0.5,	0, 1, 0.01));
					spinner.setToolTipText("Fraction of profile 0-1");
					spinners.add(    spinner   );
					break;
				}
				
				case INDEX_IS_MORE_THAN:{
					JSpinner spinner = new JSpinner(new SpinnerNumberModel(0.5,	0, 1, 0.01));
					spinner.setToolTipText("Fraction of profile 0-1");
					spinners.add(    spinner   );
					break;
				}
				
				
				
				case IS_CONSTANT_REGION:{
					
					JSpinner spinner1 =  new JSpinner();
					JSpinner spinner2 =  new JSpinner();
					JSpinner spinner3 =  new JSpinner();
					spinner1.setToolTipText("Value");
					spinner2.setToolTipText("Minimum length of region");
					spinner3.setToolTipText("Maximum permitted difference from value");
					spinners.add(  spinner1  );
					spinners.add(  spinner2  );
					spinners.add(  spinner3  );
					break;
				}
				
				case FIRST_TRUE:{
					JSpinner spinner = new JSpinner(new SpinnerNumberModel(1,	0, 1, 1));
					spinner.setToolTipText("1=True; 0=False");
					spinners.add(   spinner   );
					break;
				}
				
				case LAST_TRUE:{
					JSpinner spinner = new JSpinner(new SpinnerNumberModel(1,	0, 1, 1));
					spinner.setToolTipText("1=True; 0=False");
					spinners.add(   spinner   );
					break;
				}
				
				default:
					break;
			
			}

			for(JSpinner spinner : spinners){
				spinner.setPreferredSize(SPINNER_DIMENSION);
				this.add(spinner);
			}
			
			this.revalidate();
			this.repaint();
			
		}
		
	}

}
