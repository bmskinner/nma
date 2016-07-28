package gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import org.jfree.chart.JFreeChart;

import utility.Constants;
import charting.charts.MorphologyChartFactory;
import components.generic.BooleanProfile;
import components.generic.BorderTag;
import components.generic.Profile;
import components.generic.ProfileType;
import analysis.AnalysisDataset;
import analysis.profiles.ProfileIndexFinder;
import analysis.profiles.Rule;
import analysis.profiles.RuleSet;
import analysis.profiles.RuleSetCollection;
import gui.LoadingIconDialog;
import gui.components.ExportableChartPanel;

@SuppressWarnings("serial")
public class RulesetDialog extends LoadingIconDialog implements  TreeSelectionListener {
	
	private AnalysisDataset dataset;
	
	private ExportableChartPanel chartPanel;
	
	private JTree tree;
	
	private Map<String, RuleSetCollection> customCollections = new HashMap<String, RuleSetCollection>();
	
	public RulesetDialog (AnalysisDataset dataset){
		super();
		this.setLayout(new BorderLayout());
		this.setTitle("RuleSets for "+dataset.getName());
		this.dataset = dataset;
		
		
		JPanel westPanel = createWestPanel();
		JPanel sidePanel = createChartPanel();
		
		this.add(westPanel, BorderLayout.WEST);
		this.add(sidePanel, BorderLayout.CENTER);
		
		this.setModal(false);
		this.pack();
		this.centerOnScreen();
		this.setVisible(true);
		
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
	
	private JPanel createFooter(){
		JPanel panel = new JPanel(new FlowLayout());
		
		JButton addButton = new JButton("Custom ruleset");
		
		addButton.addActionListener( e -> { 
			
			RuleSetBuildingDialog builder = new RuleSetBuildingDialog();
			if(builder.isOK()){

				RuleSetCollection custom = builder.getCollection();
				int size = customCollections.size();
				customCollections.put("Custom_"+size, custom);

//				log(custom.toString());
				
				DefaultMutableTreeNode root = new DefaultMutableTreeNode(new RuleNodeData(dataset.getName()));
				
				createNodes(root, dataset);
								
				TreeModel model = new DefaultTreeModel(root);
				tree.setModel(model);
				
				for (int i = 0; i < tree.getRowCount(); i++) {
				    tree.expandRow(i);
				}
			}
			
		} );
		
		panel.add(addButton);
		return panel;
		
	}
	
	private JPanel createRuleSetUI(){
		JPanel panel = new JPanel();
		
		panel.setLayout(new BorderLayout());
		
		tree = new JTree();
		
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(new RuleNodeData(dataset.getName()));
		
		createNodes(root, dataset);
						
		TreeModel model = new DefaultTreeModel(root);
		tree.setModel(model);
		
		for (int i = 0; i < tree.getRowCount(); i++) {
		    tree.expandRow(i);
		}
		
		panel.add(tree);
		tree.addTreeSelectionListener(this);
		
		return panel;
		
	}
	
	/**
	 * Create the nodes in the tree
	 * @param root the root node
	 * @param dataset the dataset to use
	 */
	private void createNodes(DefaultMutableTreeNode root, AnalysisDataset dataset){

		RuleSetCollection c = dataset.getCollection().getRuleSetCollection();


		Set<BorderTag> tags = c.getTags();

		for(BorderTag t : tags){

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
						RuleNodeData ruleData = new RuleNodeData(rule.toString());
						ruleData.setRule(rule);
						DefaultMutableTreeNode ruleNode = new DefaultMutableTreeNode(ruleData);
						profileNode.add(ruleNode);

					}

				}

			}
		}
		
		// Add any custom collections created
		for(String s : customCollections.keySet()){
			
			RuleSetCollection collection = customCollections.get(s);
			
			RuleNodeData r = new RuleNodeData(s);
			r.setTag(BorderTag.REFERENCE_POINT); // Always use reference point as tag for custom
			r.setCollection(collection);

			DefaultMutableTreeNode node = new DefaultMutableTreeNode(r);
			root.add( node );

			for(RuleSet ruleSet : collection.getRuleSets(BorderTag.REFERENCE_POINT)){

				RuleNodeData profileData = new RuleNodeData(ruleSet.getType().toString());
				profileData.setRuleSet(ruleSet);
				DefaultMutableTreeNode profileNode = new DefaultMutableTreeNode(profileData);
				node.add(profileNode);

				for(Rule rule : ruleSet.getRules()){
					RuleNodeData ruleData = new RuleNodeData(rule.toString());
					ruleData.setRule(rule);
					DefaultMutableTreeNode ruleNode = new DefaultMutableTreeNode(ruleData);
					profileNode.add(ruleNode);

				}

			}
			
		}

	}

	private JPanel createChartPanel(){
		JPanel panel = new JPanel(new BorderLayout());
		
		chartPanel = new ExportableChartPanel(MorphologyChartFactory.getInstance().makeEmptyChart(ProfileType.ANGLE));
		
		panel.add(chartPanel, BorderLayout.CENTER);
		
		return panel;
	}
	
		
	@Override
	public void valueChanged(TreeSelectionEvent e) {

		DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
		RuleNodeData data = (RuleNodeData) node.getUserObject();
		
		
		ProfileIndexFinder finder = new ProfileIndexFinder();
		
		JFreeChart chart = MorphologyChartFactory.getInstance().makeEmptyChart(ProfileType.ANGLE);
		
		if(data.hasRule()){
			
			Rule r = data.getRule();
			
			Profile p = dataset.getCollection().getProfileCollection(ProfileType.ANGLE).getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN);
			BooleanProfile b = finder.getMatchingIndexes(p, r);
			chart = MorphologyChartFactory.getInstance().createBooleanProfileChart(p, b);
		}
		
		if(data.hasRuleSet()){
			
			RuleSet r = data.getRuleSet();
			Profile p = dataset.getCollection().getProfileCollection(r.getType()).getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN);
			BooleanProfile b = finder.getMatchingIndexes(p, r);
			chart = MorphologyChartFactory.getInstance().createBooleanProfileChart(p, b);
		}
		
		if(data.hasRuleSetCollection()){
			
			RuleSetCollection c = data.getCollection();
			Profile p = dataset.getCollection().getProfileCollection(ProfileType.ANGLE).getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN);
			
			BooleanProfile limits = new BooleanProfile(p, true);
			for(RuleSet r : c.getRuleSets(data.getTag())){
				Profile profile = dataset.getCollection().getProfileCollection(r.getType()).getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN);
				BooleanProfile b = finder.getMatchingIndexes(profile, r);
				limits = limits.and(b);
			}
			
			chart = MorphologyChartFactory.getInstance().createBooleanProfileChart(p, limits);
			
		}

		// Draw the rule on the chart

		chartPanel.setChart(chart);
		
	}
		
	public class RuleNodeData {
		private String      name     = null;
		private RuleSet     ruleSet  = null;
		private Rule        rule     = null;
		private ProfileType type     = null;
		private BorderTag   tag      = null;
		private RuleSetCollection collection  = null;
		
		
		public RuleNodeData(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		
		public void setRuleSet(RuleSet r){
			this.ruleSet = r;
		}
		
		public void setRule(Rule r){
			this.rule = r;
		}
		
		public void setType(ProfileType type){
			this.type = type;
		}
		
		public void setTag(BorderTag tag){
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

		public BorderTag getTag() {
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
