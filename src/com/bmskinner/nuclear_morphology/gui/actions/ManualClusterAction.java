package com.bmskinner.nuclear_morphology.gui.actions;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.ClusterGroup;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.VirtualCellCollection;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions.ClusteringMethod;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.components.AnnotatedNucleusPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.SubAnalysisSetupDialog;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.logging.Loggable;

public class ManualClusterAction extends SingleDatasetResultAction {
	
	private static final Logger LOGGER = Logger.getLogger(Loggable.ROOT_LOGGER);
	
	private static final String PROGRESS_BAR_LABEL = "Clustering cells";

    public ManualClusterAction(IAnalysisDataset dataset, @NonNull ProgressBarAcceptor acceptor, @NonNull EventHandler eh) {
        super(dataset, PROGRESS_BAR_LABEL, acceptor, eh);
    }

    @Override
    public void run() {

    	try {
    		int maxGroups = dataset.getCollection().getCells().size()-1; // more would be silly, fewer restrictive
    		int groups = eh.getInputSupplier().requestInt("Number of groups", 2,2,maxGroups,1);

    		List<String> groupNames = new ArrayList<>();

    		for(int i=1; i<=groups; i++){
    			String name = eh.getInputSupplier().requestString("Name for group "+i);
    			groupNames.add(name); 
    		}

    		ManualClusteringDialog mc = new ManualClusteringDialog(dataset, groupNames);   
    		
    		// blocks until closed
    		if(mc.isReadyToRun()) {
    			getDatasetEventHandler().fireDatasetEvent(DatasetEvent.SAVE, dataset);
    	        getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.REFRESH_POPULATIONS);
    		}
    		cancel();
    	} catch (RequestCancelledException e1) {
    		cancel();
    		return;
    	}
    }
    
    private class ManualClusteringDialog extends SubAnalysisSetupDialog {
    	
        private AnnotatedNucleusPanel panel;
            
        public class ManualGroup {
        	
        	private List<ICell> selectedCells  = new ArrayList<>(96);
        	public final String groupName;
        	
        	public ManualGroup(String name){
        		groupName = name;
        	}
        	
        	/**
        	 * Add a new cell to the group
        	 * @param c
        	 * @param time
        	 */
        	public void addCell(ICell c){
        		selectedCells.add(c);
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
        }
        
        /** Nuclei assigned to groups */
        private List<ManualGroup> groups = new ArrayList<>();
        List<String> groupNames = new ArrayList<>();
        private List<JButton> buttons = new ArrayList<>();
        private int cellNumber = 0;
        
        private final List<ICell> cells;
         
        public ManualClusteringDialog(@NonNull final IAnalysisDataset dataset, List<String> groupNames) {
            super(dataset, "Manual clustering");
            this.groupNames = groupNames;
            cells = new ArrayList<>(dataset.getCollection().getCells());
            Collections.shuffle(cells); // random ordering
            createGroups();   
            createUI();
            packAndDisplay();
        } 
               

		@Override
		public IAnalysisMethod getMethod() {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public HashOptions getOptions() {
			return null;
		}

		@Override
		protected void createUI() {
			this.panel = new AnnotatedNucleusPanel();
            openCell(0);
            this.setLayout(new BorderLayout());
            this.add(panel, BorderLayout.CENTER);
            this.add(createGroupPanel(groupNames), BorderLayout.SOUTH);
		}

		@Override
		protected void setDefaults() {
			// TODO Auto-generated method stub
		}
        
        protected void createGroups(){
        	for(int i=0; i<groupNames.size(); i++){
        		groups.add(new ManualGroup(groupNames.get(i)));
            }
        }
        
        private void addCollections(){
        	
        	// Save the clusters to the dataset
            int clusterNumber = dataset.getMaxClusterGroupNumber() + 1;
            
            IClusteringOptions op = OptionsFactory.makeClusteringOptions();
            op.setType(ClusteringMethod.MANUAL);
            op.setIncludeProfile(false);
            
            IClusterGroup group = new ClusterGroup(IClusterGroup.CLUSTER_GROUP_PREFIX + "_" + clusterNumber, op);

            for(int i=0; i<groups.size(); i++){

            	ICellCollection coll = groups.get(i).toCollection("Manual_cluster_" + i+"_"+groups.get(i).groupName);

                if (coll.hasCells()) {

                    try {
                        dataset.getCollection().getProfileManager().copyCollectionOffsets(coll);
                    } catch (ProfileException e) {
                        LOGGER.warning("Error copying collection offsets");
                        LOGGER.log(Loggable.STACK, "Error in offsetting", e);
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
            readyToRun = true;
        }
        
        private void openCell(int i){
        	
        	if(i==cells.size()){
        		LOGGER.fine("Finished manual clustering");
        		addCollections();
        		
        		fireInterfaceEvent(InterfaceMethod.REFRESH_POPULATIONS);
        		dispose();
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
            		groups.get(index).addCell(cells.get(cellNumber));
            		if(cellNumber == dataset.getCollection().size())
            			addCollections();
            		else
            			openCell(++cellNumber);
            	});
            	p.add(b);
            }
        	return p;
        }
    }

}
