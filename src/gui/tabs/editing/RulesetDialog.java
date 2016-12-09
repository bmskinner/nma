package gui.tabs.editing;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import org.jfree.chart.JFreeChart;

import stats.Quartile;
import charting.charts.MorphologyChartFactory;
import charting.charts.panels.ExportableChartPanel;
import components.ProfileableCellularComponent.IndexOutOfBoundsException;
import components.generic.BooleanProfile;
import components.generic.BorderTagObject;
import components.generic.IProfile;
import components.generic.ProfileType;
import components.generic.UnavailableBorderTagException;
import components.generic.UnavailableProfileTypeException;
import components.generic.BorderTag.BorderTagType;
import components.generic.Tag;
import analysis.IAnalysisDataset;
import analysis.profiles.ProfileException;
import analysis.profiles.ProfileIndexFinder;
import analysis.profiles.Rule;
import analysis.profiles.RuleSet;
import analysis.profiles.RuleSetCollection;
import gui.DatasetEvent;
import gui.LoadingIconDialog;
import gui.InterfaceEvent.InterfaceMethod;

@SuppressWarnings("serial")
public class RulesetDialog extends LoadingIconDialog implements  TreeSelectionListener  {
	
	private IAnalysisDataset dataset;
	
	private ExportableChartPanel chartPanel;
	
	private JTree tree;
	
	private Map<String, RuleSetCollection> customCollections = new HashMap<String, RuleSetCollection>();
	
	public RulesetDialog (IAnalysisDataset dataset){
		super();
		this.setLayout(new BorderLayout());
		this.setTitle("RuleSets for "+dataset.getName());
		this.dataset = dataset;
		
		
		JPanel westPanel = createWestPanel();
		JPanel sidePanel = createChartPanel();
		JPanel headPanel = createHeader();
		
		this.add(headPanel, BorderLayout.NORTH);
		this.add(westPanel, BorderLayout.WEST);
		this.add(sidePanel, BorderLayout.CENTER);
		
		this.setModal(false);
		this.pack();
		this.centerOnScreen();
			
		this.addWindowListener(new WindowAdapter(){
			 public void windowClosing(WindowEvent e) {
				  if(!customCollections.isEmpty()){
					  askToSaveCustomPoints();
				  }
			  }
		});
		
	}
	
	private JPanel createWestPanel(){
		
		JPanel panel = new JPanel(new BorderLayout());
		
		
		JPanel mainPanel = createRuleSetUI();
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(mainPanel);
		
		panel.add(scrollPane, BorderLayout.CENTER);
		
		JPanel footer = createFooter();
		
		panel.add(footer, BorderLayout.SOUTH);
		
		return panel;
		
	}
	
	private JPanel createHeader(){
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder( BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		JLabel label0 = new JLabel("This window displays the rules that are used to find points in the outlines of nuclei");
		JLabel label1 = new JLabel("Double click a border point name to replace an existing ruleset");
		JLabel label2 = new JLabel("Experiment with rules via 'Custom ruleset' - these can be saved when you close this window");
		JLabel label3 = new JLabel("Rules are combined via the logical AND operator ('ANDed') to determine final valid positions");
		
		panel.add(label0);
		panel.add(label1);
		panel.add(label2);
		panel.add(label3);
		
		return panel;
	}
	
	private JPanel createFooter(){
		JPanel panel = new JPanel(new FlowLayout());
		
		JButton addButton = new JButton("Custom ruleset");
		
		addButton.addActionListener( e -> { 
			
			RuleSetBuildingDialog builder = new RuleSetBuildingDialog();
			if(builder.isOK()){

//				log("Getting custom collection");
				RuleSetCollection custom = builder.getCollection();
				int size = customCollections.size();
				customCollections.put("Custom_"+size, custom);
//				log("Added as "+"Custom_"+size);
				updateTreeNodes();		
			}
			
		} );
		
		panel.add(addButton);
		return panel;
		
	}
	
	private JPanel createRuleSetUI(){
		JPanel panel = new JPanel();
		
		panel.setLayout(new BorderLayout());
		
		tree = new JTree();
		updateTreeNodes();		
		
		panel.add(tree);
		tree.addTreeSelectionListener(this);
		tree.setToggleClickCount(3);
		
		tree.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				
				if(e.getClickCount()==2){

					DefaultMutableTreeNode o = (DefaultMutableTreeNode) tree.getSelectionModel().getSelectionPath().getLastPathComponent();
					RuleNodeData r = (RuleNodeData) o.getUserObject();
					changeData(r);
				}
			}
		});
		
		return panel;
		
	}
	
	
	/**
	 * Recreate the tree model from the dataset border tags and any custom collections
	 * in this dialog
	 */
	private void updateTreeNodes(){
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(new RuleNodeData(dataset.getName()));
		
		createNodes(root, dataset);
						
		TreeModel model = new DefaultTreeModel(root);
		tree.setModel(model);
		
		for (int i = 0; i < tree.getRowCount(); i++) {
		    tree.expandRow(i);
		}
	}
	
	
	private void askToSaveCustomPoints(){
		
		Object[] options = { "Save new rulesets" , "Discard rulesets", };
		int save = JOptionPane.showOptionDialog(null,
				"Do you want to save some or all custom ruleset(s)?", 
				"Save rulesets?",
				JOptionPane.DEFAULT_OPTION, 
				JOptionPane.QUESTION_MESSAGE,
				null, options, options[0]);
		
		if(save==0){
			saveCustomPoints();
			
		}
		
	}
	
	private void saveCustomPoints(){
		// make a dialog of custom sets to save
		// checkbox to save; name field to edit; existing name
		RulesetSaveDialog d = new RulesetSaveDialog(this, customCollections);
		
		if(d.isReadyToRun()){
			// get selected sets
			
			RuleSetCollection r = d.getSelected();
//			log(r.toString());
			
			for(Tag tag : r.getTags()){
				
				if(r.hasRulesets(tag)){
					
					dataset.getCollection().getRuleSetCollection().setRuleSets(tag, r.getRuleSets(tag));
					updateBorderTagAction(tag);
				}
				
			}
			
			// add the custom sets to the dataset
			
			// trigger a point finding for cells
			
			
		} else {
			fine("Save was cancelled");
		}
		
		
	}
	
	private void changeData(RuleNodeData data){

		if(data.hasRuleSetCollection()){

			Tag tag = data.getTag();
					
			RuleSetBuildingDialog builder = new RuleSetBuildingDialog(tag);
			if(builder.isOK()){

				RuleSetCollection custom = builder.getCollection();
				List<RuleSet> list = custom.getRuleSets(tag);

				dataset.getCollection().getRuleSetCollection().setRuleSets(tag, list);

				updateTreeNodes();		
				log("Replaced RuleSet for "+tag.toString());
				//TODO - update points in cell profiles
				updateBorderTagAction(tag);

			}
			
		}
		
	}
	
	private void updateBorderTagAction(Tag tag){

		if(tag!=null){
			ProfileIndexFinder finder = new ProfileIndexFinder();
			
				int newTagIndex = finder.identifyIndex(dataset.getCollection(), tag);

				log("Updating "+tag+" to index "+newTagIndex);

				try {
					dataset
						.getCollection()
						.getProfileManager()
						.updateBorderTag(tag, newTagIndex);
				} catch (IndexOutOfBoundsException | ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException e) {
					warn("Unable to update border tag index");
					stack("Profile error", e);
					return;
				}
								
				if(tag.type().equals(BorderTagType.CORE)){
					log("Resegmenting dataset");

					fireDatasetEvent(DatasetEvent.REFRESH_MORPHOLOGY, dataset);
				} else {					
					fine("Firing refresh cache request for loaded datasets");
					fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
				}

			
		} else {
			fine("Tag is null");
			return;
		}

	}
	
	/**
	 * Create the nodes in the tree
	 * @param root the root node
	 * @param dataset the dataset to use
	 */
	private void createNodes(DefaultMutableTreeNode root, IAnalysisDataset dataset){

		RuleSetCollection c = dataset.getCollection().getRuleSetCollection();


		Set<Tag> tags = c.getTags();
		
		List<Tag> sortedList = new ArrayList<Tag>(tags);
		Collections.sort(sortedList);

		for(Tag t : sortedList){

			if(c.hasRulesets(t)){

				RuleNodeData r = new RuleNodeData(t.toString());
				r.setTag(t);
				r.setCollection(c);

				DefaultMutableTreeNode node = new DefaultMutableTreeNode(r);
				root.add( node );

				for(RuleSet ruleSet : c.getRuleSets(t)){

					RuleNodeData profileData = new RuleNodeData(ruleSet.getType().toString());
					profileData.setRuleSet(ruleSet);
					DefaultMutableTreeNode profileNode = new DefaultMutableTreeNode(profileData);
					node.add(profileNode);

					
					for(Rule rule : ruleSet.getRules()){
						
						RuleSet summedRules = new RuleSet(ruleSet.getType());
						for(Rule prev : ruleSet.getRules()){
							summedRules.addRule(prev);
							if(prev==rule){
								break;
							}
						}
						RuleNodeData ruleData = new RuleNodeData(rule.toString());
						ruleData.setRuleSet(summedRules);
						
//						RuleNodeData ruleData = new RuleNodeData(rule.toString());
//						ruleData.setRule(rule);
//						ruleData.setType(ruleSet.getType());
						DefaultMutableTreeNode ruleNode = new DefaultMutableTreeNode(ruleData);
						profileNode.add(ruleNode);

					}

				}

			}
		}
		
		// Add any custom collections created in ascending order
//		log("Adding custom nodes");
		List<String> customList = new ArrayList<String>(customCollections.keySet());
		Collections.sort(customList);
		for(String s : customList){
//			log("Adding "+s);
			RuleSetCollection collection = customCollections.get(s);
			
			RuleNodeData r = new RuleNodeData(s);
//			BorderTagObject tagObject = new BorderTagObject(s, BorderTag.CUSTOM);
//			log("Adding node for "+tagObject);
			
			r.setTag(Tag.CUSTOM_POINT); 
			r.setCollection(collection);

			DefaultMutableTreeNode node = new DefaultMutableTreeNode(r);
			root.add( node );

			for(RuleSet ruleSet : collection.getRuleSets(Tag.CUSTOM_POINT)){

				RuleNodeData profileData = new RuleNodeData(ruleSet.getType().toString());
				profileData.setRuleSet(ruleSet);
				DefaultMutableTreeNode profileNode = new DefaultMutableTreeNode(profileData);
				node.add(profileNode);

				for(Rule rule : ruleSet.getRules()){
					
					RuleNodeData ruleData = new RuleNodeData(rule.toString());
					ruleData.setRule(rule);
					ruleData.setType(ruleSet.getType());
					DefaultMutableTreeNode ruleNode = new DefaultMutableTreeNode(ruleData);
					profileNode.add(ruleNode);

				}

			}
			
		}

	}

	private JPanel createChartPanel(){
		JPanel panel = new JPanel(new BorderLayout());
		
		chartPanel = new ExportableChartPanel(MorphologyChartFactory.makeEmptyChart(ProfileType.ANGLE));
		
		panel.add(chartPanel, BorderLayout.CENTER);
		
		return panel;
	}
	
		
	@Override
	public void valueChanged(TreeSelectionEvent e) {

		DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
		RuleNodeData data = (RuleNodeData) node.getUserObject();
				
		ProfileIndexFinder finder = new ProfileIndexFinder();
		
		JFreeChart chart = MorphologyChartFactory.makeEmptyChart(ProfileType.ANGLE);
		
		try {

			if(data.hasRule()){

				Rule r = data.getRule();

				IProfile p = dataset.getCollection().getProfileCollection()
						.getProfile(data.getType(), Tag.REFERENCE_POINT, Quartile.MEDIAN);
				BooleanProfile b = finder.getMatchingIndexes(p, r);
				chart = MorphologyChartFactory.createBooleanProfileChart(p, b);
			}

			if(data.hasRuleSet()){
				RuleSet r = data.getRuleSet();

				IProfile p = dataset.getCollection().getProfileCollection()
						.getProfile(data.getType(), Tag.REFERENCE_POINT, Quartile.MEDIAN);

				BooleanProfile b = finder.getMatchingIndexes(p, r);
				chart = MorphologyChartFactory.createBooleanProfileChart(p, b);

			}

			if(data.hasRuleSetCollection()){
				RuleSetCollection c = data.getCollection();
				IProfile p = dataset.getCollection().getProfileCollection()
						.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN);

				BooleanProfile limits = finder.getMatchingProfile(dataset.getCollection(), c.getRuleSets(data.getTag()));

				chart = MorphologyChartFactory.createBooleanProfileChart(p, limits);

			}

		} catch(ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException | IllegalArgumentException e1){
			stack("Error getting profile", e1);
			chart = MorphologyChartFactory.makeErrorChart();
		}

		// Draw the rule on the chart

		chartPanel.setChart(chart);
		
	}
		
	public class RuleNodeData {
		private String      name     = null;
		private RuleSet     ruleSet  = null;
		private Rule        rule     = null;
		private ProfileType type     = null;
		private Tag         tag      = null;
		private RuleSetCollection collection  = null;
		
		
		public RuleNodeData(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		
		public void setRuleSet(RuleSet r){
			this.ruleSet = r;
			this.type = r.getType();
		}
		
		public void setRule(Rule r){
			this.rule = r;
		}
		
		public void setType(ProfileType type){
			this.type = type;
		}
		
		public void setTag(Tag tag){
			this.tag = tag;
		}
		
				
		public RuleSet getRuleSet() {
			return ruleSet;
		}

		public Rule getRule() {
			return rule;
		}

		public ProfileType getType() {
			return type;
		}
		
		
		public RuleSetCollection getCollection() {
			return collection;
		}

		public void setCollection(RuleSetCollection collection) {
			this.collection = collection;
		}

		public Tag getTag() {
			return tag;
		}

		public boolean hasRuleSet(){
			return ruleSet!=null;
		}
		
		public boolean hasRule(){
			return rule!=null;
		}
		
		public boolean hasType(){
			return type!=null;
		}
		
		public boolean hasTag(){
			return tag!=null;
		}
		
		public boolean hasRuleSetCollection(){
			return collection!=null;
		}

		public String toString() {
			return name;
		}
	}

	
}
