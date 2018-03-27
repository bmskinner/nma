package com.bmskinner.nuclear_morphology.gui.dialogs;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions.IMutableClusteringOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.components.options.ClusteringOptions.ClusteringMethod;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.components.AnnotatedNucleusPanel;
import com.bmskinner.nuclear_morphology.io.ImageImportWorker;

/**
 * This dialog allows for manual clustering of nuclei based on appearance
 * @author ben
 * @since 1.13.8
 *
 */
public class ManualClusteringDialog extends LoadingIconDialog {
	
	protected static final String LOADING_LBL = "Loading";

    protected IAnalysisDataset dataset;
    protected ImageImportWorker worker;
    
    private AnnotatedNucleusPanel panel;
        
    /**
     * Nuclei assigned to groups
     */
    private List<List<ICell>> selectedCells  = new ArrayList<>(96);
    private List<JButton> buttons = new ArrayList<>();
    private int cellNumber = 0;
    
    private final List<ICell> cells;
    private final List<String> groupNames;

    public ManualClusteringDialog(@NonNull final IAnalysisDataset dataset, List<String> groupNames) {
        super();
        this.dataset = dataset;
        cells = new ArrayList<>(dataset.getCollection().getCells());
        this.groupNames = groupNames;
        for(int i=0; i<groupNames.size(); i++){
        	selectedCells.add(new ArrayList<ICell>());
        }

        this.panel = new AnnotatedNucleusPanel();
        openCell(0);
        this.setLayout(new BorderLayout());
        this.add(panel, BorderLayout.CENTER);
        this.add(createGroupPanel(groupNames), BorderLayout.SOUTH);

        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setModal(false);
        this.pack();
        this.centerOnScreen();
        
    } 
    
    public void run(){
    	this.setVisible(true);
    }
    
    public void addCollections(){
    	
    	// Save the clusters to the dataset
        List<IAnalysisDataset> list = new ArrayList<>();
        int clusterNumber = dataset.getMaxClusterGroupNumber() + 1;
        
        IMutableClusteringOptions op = OptionsFactory.makeClusteringOptions();
        op.setType(ClusteringMethod.MANUAL);
        
        IClusterGroup group = new ClusterGroup(IClusterGroup.CLUSTER_GROUP_PREFIX + "_" + clusterNumber, op);

        for(int i=0; i<selectedCells.size(); i++){

        	ICellCollection coll = new VirtualCellCollection(dataset, "Manual_cluster_" + i+"_"+groupNames.get(i));
    		
    		for(ICell c : selectedCells.get(i)){
    			coll.addCell(c);
    		}

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
//                log("Cluster " + cluster + ": " + c.size() + " nuclei");
                IAnalysisDataset clusterDataset = dataset.getChildDataset(coll.getID());
                clusterDataset.setRoot(false);

                // set shared counts
                coll.setSharedCount(dataset.getCollection(), coll.size());
                dataset.getCollection().setSharedCount(coll, coll.size());

                list.add(clusterDataset);
            }

        }
//        fine("Profiles copied to all clusters");
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
			// TODO Auto-generated catch block
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
        		selectedCells.get(index).add(cells.get(cellNumber));
        		openCell(++cellNumber);
        	});
        	p.add(b);
        }
    	return p;
    }

}
