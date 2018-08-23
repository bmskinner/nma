package com.bmskinner.nuclear_morphology.gui.dialogs;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.ClusterGroup;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.VirtualCellCollection;
import com.bmskinner.nuclear_morphology.components.options.ClusteringOptions.ClusteringMethod;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions.IMutableClusteringOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.gui.components.AnnotatedNucleusPanel;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent.InterfaceMethod;

/**
 * This dialog allows for manual clustering of nuclei
 * @author ben
 * @since 1.13.8
 */
public class ManualClusteringDialog extends LoadingIconDialog {
	
	protected static final String LOADING_LBL = "Loading";

    protected IAnalysisDataset dataset;    
    private AnnotatedNucleusPanel panel;
        
    public class ManualGroup {
    	
    	private List<ICell> selectedCells  = new ArrayList<>(96);
    	public final String groupName;
    	private List<Long> timePerCell = new ArrayList<>(96);
    	
    	public ManualGroup(String name){
    		groupName = name;
    	}
    	
    	/**
    	 * Add a new cell to the group
    	 * @param c
    	 * @param time
    	 */
    	public void addCell(ICell c, long time){
    		selectedCells.add(c);
    		timePerCell.add(time);
    	}
    	
    	/**
    	 * Create a new virtual collection from the cells in the group
    	 * @param name
    	 * @return
    	 */
    	public ICellCollection toCollection(String name){
    		ICellCollection coll = new VirtualCellCollection(dataset, name);
    		
    		for(ICell c : selectedCells){
    			coll.addCell(c);
    		}
    		return coll;
    	}
    	
    	public String getTimes(){
    		StringBuilder b = new StringBuilder(groupName+System.getProperty("line.separator"));
    		for(int i=0; i<selectedCells.size(); i++){
    			b.append("Cell "+i+"\t"+selectedCells.get(i).getNucleus().getNameAndNumber()
    					+"\t"+timePerCell.get(i)
    					+"\t"+groupName
    					+System.getProperty("line.separator"));
    		}
    		return b.toString();
    	}
    	
    }
    
    /**
     * Nuclei assigned to groups
     */
    private List<ManualGroup> groups = new ArrayList<>();
    private List<JButton> buttons = new ArrayList<>();
    private int cellNumber = 0;
    
    private final List<ICell> cells;
    private long startTime = System.currentTimeMillis();
    
    public ManualClusteringDialog(@NonNull final IAnalysisDataset dataset, List<String> groupNames) {
        super();
        this.dataset = dataset;
        cells = new ArrayList<>(dataset.getCollection().getCells());
        Collections.shuffle(cells); // random ordering
        createGroups(groupNames);

        this.panel = new AnnotatedNucleusPanel();
        openCell(0);
        this.setLayout(new BorderLayout());
        this.add(panel, BorderLayout.CENTER);
        this.add(createGroupPanel(groupNames), BorderLayout.SOUTH);

        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setModal(false);
        this.setSize(400, 400);
        this.centerOnScreen();
        
    } 
    
    public void run(){
    	startTime = System.currentTimeMillis();
    	this.setVisible(true);
    }
    
    protected void createGroups(List<String> groupNames){
    	for(int i=0; i<groupNames.size(); i++){
    		groups.add(new ManualGroup(groupNames.get(i)));
        }
    }
    
    protected void addCollections(){
    	
    	// Save the clusters to the dataset
        int clusterNumber = dataset.getMaxClusterGroupNumber() + 1;
        
        IMutableClusteringOptions op = OptionsFactory.makeClusteringOptions();
        op.setType(ClusteringMethod.MANUAL);
        
        IClusterGroup group = new ClusterGroup(IClusterGroup.CLUSTER_GROUP_PREFIX + "_" + clusterNumber, op);

        for(int i=0; i<groups.size(); i++){

        	ICellCollection coll = groups.get(i).toCollection("Manual_cluster_" + i+"_"+groups.get(i).groupName);

            if (coll.hasCells()) {

                try {
                    dataset.getCollection().getProfileManager().copyCollectionOffsets(coll);
                } catch (ProfileException e) {
                    warn("Error copying collection offsets");
                    stack("Error in offsetting", e);
                }

                group.addDataset(coll);
                coll.setName(group.getName() + "_" + coll.getName());

                dataset.addChildCollection(coll);

                // attach the clusters to their parent collection
                IAnalysisDataset clusterDataset = dataset.getChildDataset(coll.getID());
                clusterDataset.setRoot(false);

                // set shared counts
                coll.setSharedCount(dataset.getCollection(), coll.size());
                dataset.getCollection().setSharedCount(coll, coll.size());
            }

        }
        dataset.addClusterGroup(group);
    	
    }
    
    private void openCell(int i){
    	
    	if(i==cells.size()){
    		System.out.println("Done");
    		addCollections();
    		
    		this.fireInterfaceEvent(InterfaceMethod.REFRESH_POPULATIONS);
    		this.dispose();
    		return;
    	}
    	
    	ICell c = cells.get(i);
    	try {
    		boolean annotateCellImage = false; 
			panel.showOnlyCell(c, annotateCellImage);
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	int count = cellNumber+1;
    	setTitle(count+" of "+cells.size());
    }
        
    private synchronized JPanel createGroupPanel(List<String> groupNames){
    	JPanel p = new JPanel();
    	for(int i=0; i<groupNames.size(); i++){
    		final int index = i;
    		JButton b = new JButton(groupNames.get(i));
        	buttons.add(b);
        	b.addActionListener( e -> {
        		long endTime = System.currentTimeMillis();
        		groups.get(index).addCell(cells.get(cellNumber), endTime-startTime);
        		startTime = endTime;
        		openCell(++cellNumber);
        	});
        	p.add(b);
        }
    	return p;
    }
}
