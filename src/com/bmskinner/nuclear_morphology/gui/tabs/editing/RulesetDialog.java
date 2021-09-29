/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.gui.tabs.editing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileIndexFinder;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileIndexFinder.NoDetectedIndexException;
import com.bmskinner.nuclear_morphology.charting.charts.MorphologyChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.ProfileChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.DefaultChartOptions;
import com.bmskinner.nuclear_morphology.components.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.profiles.BooleanProfile;
import com.bmskinner.nuclear_morphology.components.profiles.LandmarkType;
import com.bmskinner.nuclear_morphology.components.profiles.IProfile;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.rules.Rule;
import com.bmskinner.nuclear_morphology.components.rules.RuleApplicationType;
import com.bmskinner.nuclear_morphology.components.rules.RuleSet;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.dialogs.LoadingIconDialog;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.io.xml.RuleSetCollectionXMLImporter;
import com.bmskinner.nuclear_morphology.io.xml.XMLReader.XMLReadingException;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;

@SuppressWarnings("serial")
public class RulesetDialog extends LoadingIconDialog 
	implements TreeSelectionListener, ListSelectionListener {
	
	private static final Logger LOGGER = Logger.getLogger(RulesetDialog.class.getName());

    private IAnalysisDataset dataset;

    private ExportableChartPanel chartPanel;

    private JTree rulesetTree;
    private JTable borderTagTable;

    /** Imported ruleset collections may give multiple instances of a single tag
     * Allow multiple ruleset collections to be stored */ 
    private Map<String, RuleSetCollection> customCollections = new HashMap<>();

    public RulesetDialog(IAnalysisDataset dataset) {
        super();
        this.setLayout(new GridBagLayout());
        this.setTitle(Labels.EditingBorderTags.RULESET_DIALOG_TITLE + dataset.getName());
        this.dataset = dataset;
        
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.1;
        this.add(createHeader(), c);
        
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0.3;
        c.weighty = 0.9;
        this.add(createWestPanel(), c);
        
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 0.7;
        c.weighty = 0.9;
        this.add(createChartPanel(), c);

        this.setModal(false);
        this.pack();
        this.centerOnScreen();

        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (!customCollections.isEmpty()) {
                    askToSaveCustomPoints();
                }
            }
        });

    }

    /**
     * Create the panel to display the points
     * in the dataset
     * @return
     */
    private JPanel createWestPanel() {

        JPanel panel = new JPanel(new BorderLayout());
        
        //Separate upper and lower panels for tags and detail
        JPanel tagPanel = new JPanel(new BorderLayout());
        borderTagTable = createBorderPointTable();
        JScrollPane upperScrollPane = new JScrollPane(borderTagTable);
        tagPanel.setMinimumSize(new Dimension(10, 50));
        tagPanel.add(upperScrollPane, BorderLayout.CENTER);
        
        createRuleSetTree();
        JPanel rulePanel = new JPanel(new BorderLayout());
        rulePanel.setMinimumSize(new Dimension(10, 50));
        JScrollPane lowerScrollPane = new JScrollPane(rulesetTree);
        rulePanel.add(lowerScrollPane, BorderLayout.CENTER);
        
        // Ensure the two panels take equal Y space
        JPanel resizingPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
       
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.fill = GridBagConstraints.BOTH; // reset to default
        c.weightx = 1.0;
        c.weighty = 0.5;
        
        resizingPanel.add(tagPanel, c);
        c.gridy = 1;
        c.weightx = 1.0;
        c.weighty = 0.5;
        resizingPanel.add(rulePanel, c);
                
        panel.add(createButtonPanel(), BorderLayout.NORTH);
        panel.add(resizingPanel, BorderLayout.CENTER);

        return panel;

    }
    
    /**
     * Create the  tree of 
     * rulesets for a given point
     */
    private void createRuleSetTree() {
        rulesetTree = new JTree();
        updateTreeNodes();
        rulesetTree.addTreeSelectionListener(this);
        rulesetTree.setToggleClickCount(2);
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        panel.add(new JLabel(Labels.EditingBorderTags.RULESET_DIALOG_HEADER_0));
        panel.add(new JLabel(Labels.EditingBorderTags.RULESET_DIALOG_HEADER_1));

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout());
        
        // Allow switching of dataset rule type
        RuleApplicationType currentRule = dataset.getAnalysisOptions().get().getRuleApplicationType(); 
        ButtonGroup bg = new ButtonGroup();
        for(RuleApplicationType ruleType: RuleApplicationType.values()) {
        	JRadioButton btn = new JRadioButton(ruleType.toString());
        	
        	btn.addActionListener(e->dataset.getAnalysisOptions().get()
            		.setRuleApplicationType(ruleType));
        	btn.setSelected(currentRule.equals(ruleType));
        	bg.add(btn);
        	panel.add(btn);
        }
        

        JButton addButton = new JButton(Labels.EditingBorderTags.RULESET_DIALOG_CUSTOM_BTN);

        addButton.addActionListener(e -> {

            RuleSetBuildingDialog builder = new RuleSetBuildingDialog();
            if (builder.isOK()) {
                RuleSetCollection custom = builder.getCollection();
                int size = customCollections.size();
                customCollections.put("Custom_" + size, custom);
                borderTagTable.setModel(createBorderTagTableModel(dataset));
            }

        });

        panel.add(addButton);
        
        
        JButton importButton = new JButton(Labels.EditingBorderTags.RULESET_DIALOG_IMPORT_BTN);
        importButton.addActionListener(e->{
        	try {
				File f = InputSupplier.getDefault()
						.requestFile(Labels.EditingBorderTags.RULESET_DIALOG_SELECT_FILE, 
								dataset.getSavePath().getParentFile(), 
								"xml", 
								"XML file");
				
				RuleSetCollectionXMLImporter ri = new RuleSetCollectionXMLImporter(f);
				RuleSetCollection rsc = ri.importRuleset();
				
				customCollections.put(f.getName(), rsc);
				borderTagTable.setModel(createBorderTagTableModel(dataset));
				
			} catch (RequestCancelledException e1) {
				// User cancelled, no action
			} catch (XMLReadingException e1) {
				LOGGER.warning("Unable to import file: "+e1.getMessage());
				LOGGER.log(Loggable.STACK, "Error importing XML file", e);
			}
        });
        panel.add(importButton);
        return panel;

    }

    /**
     * Create the table to display border tags
     * @return
     */
    private JTable createBorderPointTable() {
    	JTable table = new JTable(createBorderTagTableModel(dataset));
    	table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    	table.getSelectionModel().addListSelectionListener(this);
    	return table;
    }
    
    /**
     * Create a table showing all the tags in the given dataset
     * @param d the dataset to display tags for
     * @return
     */
    private TableModel createBorderTagTableModel(IAnalysisDataset d) {
    	DefaultTableModel model = new DefaultTableModel() {
    		@Override
    	    public boolean isCellEditable(int row, int column) {
    	       return false;
    	    }
    	};
    	List<Landmark> tagList = new ArrayList<>();
    	List<String> tagCollection = new ArrayList<>();
    	
    	// Dataset tags
    	tagList.addAll(d.getCollection().getProfileCollection().getBorderTags());
    	tagCollection.addAll(tagList.stream().map(t->dataset.getName()).collect(Collectors.toList()));
    	
    	// Custom tags
    	for(Entry<String, RuleSetCollection> entry : customCollections.entrySet()) {
    		tagList.addAll(entry.getValue().getTags());
    		tagCollection.addAll(entry.getValue().getTags().stream().map(t->entry.getKey()).collect(Collectors.toList()));
    	}
    	
    	
    	Landmark[] tags = tagList.toArray(new Landmark[0]);
    	String[] collections = tagCollection.toArray(new String[0]);
    	
    	model.addColumn("Tag", tags);    	
    	model.addColumn("Dataset", collections);    
    	
    	return model;
    }

    /**
     * Recreate the tree model from the dataset border tags and any custom
     * collections in this dialog
     */
    private void updateTreeNodes() {
    	DefaultMutableTreeNode root;
    	if(borderTagTable.getSelectedRow()<0) {
    		root = new DefaultMutableTreeNode("");
    	} else {
    		Landmark selectedTag = (Landmark)borderTagTable.getValueAt(borderTagTable.getSelectedRow(), 0);  
    		String collection = (String) borderTagTable.getValueAt(borderTagTable.getSelectedRow(), 1);  
    		root = createTagNodes(selectedTag, collection);
    	}
        
        TreeModel model = new DefaultTreeModel(root);
        rulesetTree.setModel(model);

        for (int i = 0; i < rulesetTree.getRowCount(); i++) {
        	rulesetTree.expandRow(i);
        }
    }
    
    /**
     * Create tree nodes for the given tag
     * @param tag the tag
     * @param collection the collection the tag is within. Use {@link IAnalysisDataset::getName} to use the current dataset.
     * @return
     */
    private DefaultMutableTreeNode createTagNodes(@NonNull Landmark tag, @NonNull String collection) {
    	LOGGER.finest("Adding nodes for "+tag);
    	
    	// Get the appropriate RulesetCollection
    	List<RuleSet> rulesets = getRulesetsForTag(tag, collection);
    	
    	RuleNodeData r = new RuleNodeData(tag, null, null);
    	DefaultMutableTreeNode tagNode = new DefaultMutableTreeNode(r);

    	for (RuleSet ruleSet : rulesets) {
    		addNodesForRuleSet(tagNode, ruleSet, tag);
    	}
    	return tagNode;
    }
    
    /**
     * Fetch the rulesets for the given tag, whether in the dataset
     * or unsaved custom rulesets
     * @param t the tag to fetch
     * @param collection the collection to use. Use {@link IAnalysisDataset::getName} to use the current dataset.
     * @return the rulesets for the tag
     */
    private List<RuleSet> getRulesetsForTag(@NonNull Landmark t, @NonNull String collection){
    	if(collection==null || collection.equals(dataset.getName())) {
    		RuleSetCollection datasetCollection = dataset.getCollection().getRuleSetCollection();
    		if(datasetCollection.hasRulesets(t))
    			return datasetCollection.getRuleSets(t);
    	}
    	return customCollections.get(collection).getRuleSets(t);
    }

    private void askToSaveCustomPoints() {

        Object[] options = { Labels.EditingBorderTags.RULESET_DIALOG_SAVE_OPTION, 
        		Labels.EditingBorderTags.RULESET_DIALOG_DISCARD_OPTION };
        int save = JOptionPane.showOptionDialog(null, Labels.EditingBorderTags.RULESET_DIALOG_SAVE_CHOICE,
        		Labels.EditingBorderTags.RULESET_DIALOG_SAVE_TITLE, JOptionPane.DEFAULT_OPTION, 
                JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (save == 0) {
            saveCustomPoints();
        }
    }

    private void saveCustomPoints() {
        // make a dialog of custom sets to save
        // checkbox to save; name field to edit; existing name
        RulesetSaveDialog d = new RulesetSaveDialog(this, customCollections);

        if (d.isReadyToRun()) {
            // get selected sets

            RuleSetCollection rsc = d.getSelected();
            
            // If the reference point is in the collection, handle it first
            // TODO: this will eventually cause threading issues with large datasets
            if(rsc.getTags().contains(Landmark.REFERENCE_POINT)) {
            	if(rsc.hasRulesets(Landmark.REFERENCE_POINT)) {
                    dataset.getCollection().getRuleSetCollection().setRuleSets(Landmark.REFERENCE_POINT, rsc.getRuleSets(Landmark.REFERENCE_POINT));
                    updateBorderTagAction(Landmark.REFERENCE_POINT);
                }
            }
            
            for (Landmark tag : rsc.getTags()) {
            	if(tag.equals(Landmark.REFERENCE_POINT))
            		continue;
            	LOGGER.fine("Testing existence of tag "+tag);
                if (rsc.hasRulesets(tag)) {
                    dataset.getCollection().getRuleSetCollection().setRuleSets(tag, rsc.getRuleSets(tag));
                    updateBorderTagAction(tag);
                }
            }

            // add the custom sets to the dataset
            // trigger a point finding for cells
        } else {
            LOGGER.fine("Save was cancelled");
        }
    }

    private void updateBorderTagAction(Landmark tag) {

        if (tag != null) {
            ProfileIndexFinder finder = new ProfileIndexFinder();
            try {

                int newTagIndex = finder.identifyIndex(dataset.getCollection(), tag);

                LOGGER.info("Updating " + tag + " to index " + newTagIndex);

                dataset.getCollection().getProfileManager().updateBorderTag(tag, newTagIndex);
            } catch (IndexOutOfBoundsException | ProfileException | UnavailableBorderTagException
                    | UnavailableProfileTypeException e) {
                LOGGER.warning("Unable to update border tag index");
                LOGGER.log(Loggable.STACK, "Profile error", e);
                return;
            } catch (NoDetectedIndexException e) {
                LOGGER.warning("Unable to update border tag index - cannot find index with given ruleset");
                LOGGER.log(Loggable.STACK, "Unable to find matching index", e);
                return;
            }

            if (tag.type().equals(LandmarkType.CORE)) {
                LOGGER.info("Resegmenting dataset");

                fireDatasetEvent(DatasetEvent.REFRESH_MORPHOLOGY, dataset);
            } else {
                LOGGER.fine("Firing refresh cache request for loaded datasets");
                fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
            }

        } else {
            LOGGER.fine("Tag is null");
        }
    }
        
    /**
     * Create tree nodes for each rule in a ruleset
     * @param tagNode
     * @param ruleSet
     */
    private void addNodesForRuleSet(DefaultMutableTreeNode tagNode, RuleSet ruleSet, Landmark tag) {
    	RuleNodeData profileData = new RuleNodeData(tag, ruleSet, null);
		DefaultMutableTreeNode profileNode = new DefaultMutableTreeNode(profileData);
		tagNode.add(profileNode);

		for (Rule rule : ruleSet.getRules()) {

			RuleNodeData ruleData = new RuleNodeData(tag, ruleSet, rule);
			DefaultMutableTreeNode ruleNode = new DefaultMutableTreeNode(ruleData);
			profileNode.add(ruleNode);
		}
    }

    private JPanel createChartPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        chartPanel = new ExportableChartPanel(ProfileChartFactory.createEmptyChart(ProfileType.ANGLE));
        panel.add(chartPanel, BorderLayout.CENTER);
        return panel;
    }
    
    /**
     * Give a tag, create the boolean chart showing the points identified
     * by the tag's rulesets, either in the dataset or unsaved custom
     * @param t the tag to display
     * 
     * @return a chart showing the index found by the tag
     * @throws UnavailableBorderTagException
     * @throws UnavailableProfileTypeException
     * @throws ProfileException
     */
    private JFreeChart createChart(@NonNull Landmark t, @NonNull String collection) throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException {
    	LOGGER.finest("Creating chart for "+t);
    	JFreeChart chart = ProfileChartFactory.createEmptyChart(ProfileType.ANGLE);
    	ChartOptions options = new DefaultChartOptions((IAnalysisDataset) null);
        MorphologyChartFactory chf = new MorphologyChartFactory(options);
        ProfileIndexFinder finder = new ProfileIndexFinder();
        IProfile p = dataset.getCollection().getProfileCollection().getProfile(ProfileType.ANGLE,
                Landmark.REFERENCE_POINT, Stats.MEDIAN);
        
        BooleanProfile limits = finder.getMatchingProfile(dataset.getCollection(),
        		getRulesetsForTag(t, collection));

    	chart = chf.createBooleanProfileChart(p, limits);
        return chart;
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
        RuleNodeData data = (RuleNodeData) node.getUserObject();
        chartPanel.setChart(data.getChart());
    }

    /**
     * Nodes for the ruleset tree.
     * @author ben
     *
     */
    public class RuleNodeData {
        private RuleSet ruleSet = null;
        private Rule    rule    = null;
        private Landmark     tag     = null;
        
        public RuleNodeData(@NonNull Landmark tag, @Nullable RuleSet ruleSet, @Nullable Rule rule) {;
            this.tag = tag;
            this.ruleSet = ruleSet;
            this.rule = rule;
        }
        
        /**
         * Create the appropriate chart for the node 
         * @return
         */
        public JFreeChart getChart() {

        	try {
        		ProfileIndexFinder finder = new ProfileIndexFinder();
        		ChartOptions options = new DefaultChartOptions((IAnalysisDataset) null);
        		MorphologyChartFactory chf = new MorphologyChartFactory(options);

        		if(rule!=null) {
        			IProfile p = dataset.getCollection().getProfileCollection().getProfile(ruleSet.getType(),
        					Landmark.REFERENCE_POINT, Stats.MEDIAN);
        			BooleanProfile b = finder.getMatchingIndexes(p, rule);
        			return chf.createBooleanProfileChart(p, b);
        		}

        		if(ruleSet!=null) {
        			IProfile p = dataset.getCollection().getProfileCollection().getProfile(ruleSet.getType(),
        					Landmark.REFERENCE_POINT, Stats.MEDIAN);
        			BooleanProfile b = finder.getMatchingIndexes(p, ruleSet);
        			return chf.createBooleanProfileChart(p, b);
        		}

        		return createChartForSelectedTableRow();

        	} catch(Exception e) {
        		LOGGER.log(Loggable.STACK, "Error creating profile chart: "+e.getMessage(), e);
        		return MorphologyChartFactory.createErrorChart();
        	}
        }

        public String toString() {
        	if(rule!=null)
            	return rule.toString();
            if(ruleSet!=null)
            	return ruleSet.getType().toString();
            return tag.toString();
        }
    }
    
    /**
     * Create a chart for the currently selected row in the tag table
     * @return
     */
    private JFreeChart createChartForSelectedTableRow() {
    	int row = borderTagTable.getSelectedRow();
		if(row<0)
			return MorphologyChartFactory.createEmptyChart();
		Landmark t = (Landmark)borderTagTable.getValueAt(row, 0);
		String collection = (String) borderTagTable.getValueAt(row, 1);
		try {
			return createChart(t, collection);
		} catch (UnavailableBorderTagException | UnavailableProfileTypeException | ProfileException e) {
			LOGGER.log(Loggable.STACK, "Unable to make chart: "+e.getMessage(), e);
			return MorphologyChartFactory.createErrorChart();
		}
    }

	@Override
	public synchronized void valueChanged(ListSelectionEvent e) {
		if(!e.getValueIsAdjusting()) {
			chartPanel.setChart(createChartForSelectedTableRow());
			updateTreeNodes();
		}
	}
}
