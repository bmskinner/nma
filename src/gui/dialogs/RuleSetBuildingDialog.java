package gui.dialogs;

import gui.components.panels.ProfileTypeOptionsPanel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;

import logging.Loggable;
import components.generic.BorderTag;
import components.generic.ProfileType;
import analysis.profiles.Rule;
import analysis.profiles.Rule.RuleType;
import analysis.profiles.RuleSet;
import analysis.profiles.RuleSetCollection;

@SuppressWarnings("serial")
public class RuleSetBuildingDialog extends JDialog implements Loggable {
			
	private boolean isOK = false;
	
	private JPanel mainPanel;
		
	public RuleSetBuildingDialog(){
		super();
		this.setTitle("Ruleset Builder");
		this.setModal(true);
		
		createUI();
		
		this.pack();
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
				
				RuleSetPanel panel = (RuleSetPanel) c;
				collection.addRuleSet(BorderTag.REFERENCE_POINT, panel.getRuleSet());
				
			}
		}
		
		return collection;
	}
	
	public class RuleSetPanel extends JPanel {
		
		private JPanel footer;
		private JPanel main;
		
		private ProfileType type;
		List<RulePanel> rules = new ArrayList<RulePanel>();
		
		public RuleSetPanel(){
			
			this.setLayout(new BorderLayout());
			
			main = new JPanel();
			main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
			
			footer = new JPanel(new FlowLayout());

			
			ProfileTypeOptionsPanel profileOptions  = new ProfileTypeOptionsPanel();
			
			profileOptions.addActionListener( e -> {
				this.type = profileOptions.getSelected();
			});
			
			footer.add(profileOptions);
			
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
			
			footer.add(addRuleButton);
			footer.add(deleteRuleSetButton);
			
			this.add(main, BorderLayout.CENTER);
			this.add(footer, BorderLayout.SOUTH);
			
		}
		
		private void addRule(){
			
			RulePanel r = new RulePanel();
			rules.add(r);
			main.add(r);
			
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
			RuleSet r = new RuleSet(type);
			for(RulePanel panel : rules){
				r.addRule(panel.getRule());
			}
			return r;
		}
		
	};
	
	public class RulePanel extends JPanel implements ActionListener{
		
		private JComboBox<RuleType> typeBox = new JComboBox<RuleType>(RuleType.values());
		
		List<JSpinner> spinners = new ArrayList<JSpinner>();
		
		public RulePanel(){
			
			this.setLayout(new FlowLayout());
			
			this.add(typeBox);
			
			typeBox.setSelectedItem(RuleType.IS_MINIMUM);
			spinners.add(   new JSpinner()   );
			for(JSpinner spinner : spinners){
				this.add(spinner);
			}
			
			typeBox.addActionListener(this);
			
			
		}
		
		public Rule getRule(){
			Rule result = null;
			try {
				spinners.get(0).commitEdit();

				double first = (double) spinners.get(0).getValue();
				result = new Rule((RuleType) typeBox.getSelectedItem(), first);

				for(int i=1; i<spinners.size(); i++){
					spinners.get(i).commitEdit();
					result.addValue( (double) spinners.get(i).getValue()); 
				}

			} catch (ParseException e) {
				error("Error parsing spinner", e);
			}
			return  result;
		}


		@Override
		public void actionPerformed(ActionEvent e) {
						
			for(JSpinner spinner : spinners){
				this.remove(spinner);
			}
			
			spinners = new ArrayList<JSpinner>();
			
			switch(  (RuleType) typeBox.getSelectedItem()){
			
				case IS_MINIMUM:{
					spinners.add(   new JSpinner()   );
					break;
				}
				
				case IS_MAXIMUM:{
					spinners.add(   new JSpinner()   );
					break;
				}
				
				case IS_LOCAL_MINIMUM:{
					spinners.add(   new JSpinner()   );
					break;
				}
				
				case IS_LOCAL_MAXIMUM:{
					spinners.add(   new JSpinner()   );
					break;
				}
				
				case VALUE_IS_LESS_THAN:{
					spinners.add(   new JSpinner()   );
					break;
				}
				
				case VALUE_IS_MORE_THAN:{
					spinners.add(   new JSpinner()   );
					break;
				}
				
				case INDEX_IS_LESS_THAN:{
					spinners.add(   new JSpinner()   );
					break;
				}
				
				case INDEX_IS_MORE_THAN:{
					spinners.add(   new JSpinner()   );
					break;
				}
				
				
				
				case IS_CONSTANT_REGION:{
					spinners.add(   new JSpinner()   );
					spinners.add(   new JSpinner()   );
					spinners.add(   new JSpinner()   );
					break;
				}
				
				case FIRST_TRUE:{
					spinners.add(   new JSpinner()   );
					break;
				}
				
				case LAST_TRUE:{
					spinners.add(   new JSpinner()   );
					break;
				}
				
				default:
					break;
			
			}
			
			for(JSpinner spinner : spinners){
				this.add(spinner);
			}
			
			this.revalidate();
			this.repaint();
			
		}
		
	}

}
