package gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
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

import utility.Constants;
import charting.charts.ExportableChartPanel;
import charting.charts.MorphologyChartFactory;
import components.generic.BooleanProfile;
import components.generic.BorderTag;
import components.generic.BorderTagObject;
import components.generic.Profile;
import components.generic.ProfileType;
import components.generic.BorderTag.BorderTagType;
import analysis.AnalysisDataset;
import analysis.profiles.ProfileIndexFinder;
import analysis.profiles.Rule;
import analysis.profiles.RuleSet;
import analysis.profiles.RuleSetCollection;
import gui.DatasetEvent;
import gui.DatasetEventListener;
import gui.InterfaceEvent;
import gui.InterfaceEventListener;
import gui.LoadingIconDialog;
import gui.DatasetEvent.DatasetMethod;
import gui.InterfaceEvent.InterfaceMethod;

@SuppressWarnings("serial")
public class RulesetDialog extends LoadingIconDialog implements  TreeSelectionListener  {
	
	private AnalysisDataset dataset;
	
	private ExportableChartPanel chartPanel;
	
	private JTree tree;
	
	private final List<Object> datasetListeners 	= new ArrayList<Object>();
	private final List<Object> interfaceListeners 	= new ArrayList<Object>();
	
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
		
		Object[] options = { "Save new RuleSets" , "Discard RuleSets", };
		int save = JOptionPane.showOptionDialog(null,
				"Do you want to save custom RuleSet(s)?", 
				"Save RuleSets?",
				JOptionPane.DEFAULT_OPTION, 
				JOptionPane.QUESTION_MESSAGE,
				null, options, options[0]);
		
		if(save==0){
			
			// make a dialog of custom sets to save
			
			// add the custom sets to the dataset
		}
		
	}
	
	private void changeData(RuleNodeData data){

		if(data.hasRuleSetCollection()){

			BorderTagObject tag = data.getTag();
					
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
	
	private void updateBorderTagAction(BorderTagObject tag){

		if(tag!=null){
			ProfileIndexFinder finder = new ProfileIndexFinder();
			
				int newTagIndex = finder.identifyIndex(dataset.getCollection(), tag);

				log("Updating "+tag+" to index "+newTagIndex);

				dataset
					.getCollection()
					.getProfileManager()
					.updateBorderTag(tag, newTagIndex);
								
				if(tag.type().equals(BorderTagType.CORE)){
					log("Resegmenting dataset");
					
					List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
					list.add(dataset);
					fireDatasetEvent(DatasetMethod.REFRESH_MORPHOLOGY, list);
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
	private void createNodes(DefaultMutableTreeNode root, AnalysisDataset dataset){

		RuleSetCollection c = dataset.getCollection().getRuleSetCollection();


		Set<BorderTagObject> tags = c.getTags();

		for(BorderTagObject t : tags){

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
						ruleData.setType(ruleSet.getType());
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
			
			r.setTag(BorderTagObject.CUSTOM_POINT); 
			r.setCollection(collection);

			DefaultMutableTreeNode node = new DefaultMutableTreeNode(r);
			root.add( node );

			for(RuleSet ruleSet : collection.getRuleSets(BorderTagObject.CUSTOM_POINT)){

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
			
			Profile p = dataset.getCollection().getProfileCollection(data.getType()).getProfile(BorderTagObject.REFERENCE_POINT, Constants.MEDIAN);
			BooleanProfile b = finder.getMatchingIndexes(p, r);
			chart = MorphologyChartFactory.getInstance().createBooleanProfileChart(p, b);
		}
		
		if(data.hasRuleSet()){
			
			RuleSet r = data.getRuleSet();
			Profile p = dataset.getCollection().getProfileCollection(r.getType()).getProfile(BorderTagObject.REFERENCE_POINT, Constants.MEDIAN);
			BooleanProfile b = finder.getMatchingIndexes(p, r);
			chart = MorphologyChartFactory.getInstance().createBooleanProfileChart(p, b);
		}
		
		if(data.hasRuleSetCollection()){
			
			RuleSetCollection c = data.getCollection();
			Profile p = dataset.getCollection().getProfileCollection(ProfileType.ANGLE).getProfile(BorderTagObject.REFERENCE_POINT, Constants.MEDIAN);
			
			BooleanProfile limits = finder.getMatchingProfile(dataset.getCollection(), c.getRuleSets(data.getTag()));
			
			chart = MorphologyChartFactory.getInstance().createBooleanProfileChart(p, limits);
			
		}

		// Draw the rule on the chart

		chartPanel.setChart(chart);
		
	}
	
	public synchronized void addDatasetEventListener( DatasetEventListener l ) {
    	datasetListeners.add( l );
    }
    
    public synchronized void removeDatasetEventListener( DatasetEventListener l ) {
    	datasetListeners.remove( l );
    }
    
    public synchronized void addInterfaceEventListener( InterfaceEventListener l ) {
    	interfaceListeners.add( l );
    }
    
    public synchronized void removeInterfaceEventListener( InterfaceEventListener l ) {
    	interfaceListeners.remove( l );
    }
    
    
    protected synchronized void fireDatasetEvent(DatasetMethod method, List<AnalysisDataset> list) {
    	
        DatasetEvent event = new DatasetEvent( this, method, this.getClass().getSimpleName(), list);
        Iterator<Object> iterator = datasetListeners.iterator();
        while( iterator.hasNext() ) {
            ( (DatasetEventListener) iterator.next() ).datasetEventReceived( event );
        }
    }
    
    protected synchronized void fireDatasetEvent(DatasetMethod method, List<AnalysisDataset> list, AnalysisDataset template) {

    	DatasetEvent event = new DatasetEvent( this, method, this.getClass().getSimpleName(), list, template);
    	Iterator<Object> iterator = datasetListeners.iterator();
    	while( iterator.hasNext() ) {
    		( (DatasetEventListener) iterator.next() ).datasetEventReceived( event );
    	}
    }
    
    protected synchronized void fireDatasetEvent(DatasetEvent event) {
    	Iterator<Object> iterator = datasetListeners.iterator();
    	while( iterator.hasNext() ) {
    		( (DatasetEventListener) iterator.next() ).datasetEventReceived( event );
    	}
    }
    
    protected synchronized void fireInterfaceEvent(InterfaceMethod method) {
    	
    	InterfaceEvent event = new InterfaceEvent( this, method, this.getClass().getSimpleName());
        Iterator<Object> iterator = interfaceListeners.iterator();
        while( iterator.hasNext() ) {
            ( (InterfaceEventListener) iterator.next() ).interfaceEventReceived( event );
        }
    }
    
    protected synchronized void fireInterfaceEvent(InterfaceEvent event) {

        Iterator<Object> iterator = interfaceListeners.iterator();
        while( iterator.hasNext() ) {
            ( (InterfaceEventListener) iterator.next() ).interfaceEventReceived( event );
        }
    }
		
	public class RuleNodeData {
		private String      name     = null;
		private RuleSet     ruleSet  = null;
		private Rule        rule     = null;
		private ProfileType type     = null;
		private BorderTagObject   tag      = null;
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
		
		public void setTag(BorderTagObject tag){
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

		public BorderTagObject getTag() {
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
