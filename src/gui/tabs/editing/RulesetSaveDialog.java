package gui.tabs.editing;

import gui.dialogs.SettingsDialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import analysis.profiles.RuleSetCollection;
import components.generic.BorderTag;
import components.generic.BorderTagObject;

@SuppressWarnings("serial")
public class RulesetSaveDialog extends SettingsDialog {
	
	
	private Map<String, RuleSetCollection> customCollections;
	
	private JPanel main;
	
	
	public RulesetSaveDialog(Dialog owner, Map<String, RuleSetCollection> customCollections){
		super(owner, true);
		this.setTitle("Save rulesets");
		this.customCollections = customCollections;
		createUI();
		this.pack();
		this.setVisible(true);
	}
	
	private JPanel createHeader(){
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		
		panel.add(new JLabel("Select and name custom rulesets to retain"));
		
		return panel;
	}
	
	private void createUI(){
		this.setLayout(new BorderLayout());
		main = new JPanel();
		
		main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
		main.setBorder(new EmptyBorder(5, 5, 5, 5));
				
		for(String s : customCollections.keySet()){
			
			CollectionPanel p = new CollectionPanel(s, customCollections.get(s));
			main.add(p);
		}
		
		
		JPanel header = createHeader();
		JPanel footer = createFooter();
		
		this.add(header, BorderLayout.NORTH);
		this.add(main,   BorderLayout.CENTER);
		this.add(footer, BorderLayout.SOUTH);

	}
	
	public RuleSetCollection getSelected(){
		RuleSetCollection result = new RuleSetCollection();
		for(Component c : main.getComponents()){
			
			if(c instanceof CollectionPanel){
				RuleSetCollection r = ((CollectionPanel) c).getCollection();
					
				BorderTagObject o = new BorderTagObject(((CollectionPanel) c).getText(), BorderTag.CUSTOM);

				result.setRuleSets(o, r.getRuleSets(BorderTagObject.CUSTOM_POINT));
//				log("Added custom point: "+o.toString());
			}
			
		}
		return result;
	}
	
	public class CollectionPanel extends JPanel {
		private RuleSetCollection collection;
		final JCheckBox box;
		final JTextField text;
		
		public CollectionPanel(String s, RuleSetCollection collection){
			this.setLayout(new FlowLayout());
			this.collection = collection;
			box   = new JCheckBox("", true);
			text = new JTextField(s);
			
			box.addActionListener( e -> {
				text.setEnabled(box.isSelected());
			});
			
			this.add(box);
			this.add(text);
		}
		
		public String getText(){
			return text.getText();
		}
		
		public RuleSetCollection getCollection(){
			return collection;
		}
		
	}

}
