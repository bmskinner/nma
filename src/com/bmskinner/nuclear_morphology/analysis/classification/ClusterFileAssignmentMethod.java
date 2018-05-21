package com.bmskinner.nuclear_morphology.analysis.classification;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.ClusterAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.ClusterGroup;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.VirtualCellCollection;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;

/**
 * Allow clusters to be assigned to a dataset, based on their map in a file.
 * This is used to import clusters determined outside the program, so (e.g.) consensus refolding
 * can be performed on these clusters.
 * @author bms41
 * @since 1.14.0
 *
 */
public class ClusterFileAssignmentMethod extends SingleDatasetAnalysisMethod {
	
	private File clusterFile;
	private Map<UUID, Integer> cellMap;
	
	public ClusterFileAssignmentMethod(@NonNull IAnalysisDataset d, @NonNull File f) {
		super(d);
		clusterFile = f;
	}

	@Override
	public IAnalysisResult call() throws Exception {
		
		if(!isFileFormatValid(clusterFile))
			throw new ClusteringMethodException("Invalid mapping file format");
		
		log("Reading map file");
		readMapFile();
        fine("Read "+cellMap.size()+" cell ids");
		IClusterGroup group = assignClusters();
		return new ClusterAnalysisResult(dataset, group);
	}
	
	/**
	 * The valid file format for this mapping is a tab separated file,
	 * with two values per line: a cell UUID, and a cluster integer.
	 * e.g. 00000000-0000-0000-0000-000000000001	1
	 * All the cells in the dataset should be represented in the file.
	 * @param file
	 * @return true if the file meets the requirements for cluster assignment, false otherwise
	 */
	private boolean isFileFormatValid(@NonNull File file) {
		return true;
	}
	
	private void readMapFile() throws ClusteringMethodException{
        
        cellMap = new HashMap<>(12000);

        try {
            FileInputStream fstream = new FileInputStream(clusterFile);
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(fstream, Charset.forName("ISO-8859-1")));

            int i = 0;
            String strLine;
            while (( strLine = br.readLine()) != null) {
                i++;
                if (i > 1) {
                    String[] arr = strLine.split("\\t");
                    UUID id = UUID.fromString(arr[0]);
                    int cluster = Integer.parseInt(arr[1]);
                    cellMap.put(id, cluster);
                    fireProgressEvent();
                }

            }
            fstream.close();
        }
        catch (Exception e) {
        	stack("Error parsing mapping file", e);
        	throw new ClusteringMethodException("Invalid mapping file format");
        }
    }

	
	private IClusterGroup assignClusters(){
        fine("Assigning clusters");

        Map<Integer, ICellCollection> clusterMap = new HashMap<>();
        
        int clusterNumber = dataset.getMaxClusterGroupNumber() + 1;

        IClusterGroup group = new ClusterGroup(IClusterGroup.CLUSTER_GROUP_PREFIX + "_" + clusterNumber, OptionsFactory.makeClusteringOptions());

        
        long nClusters = cellMap.values().stream().distinct().count();
        fine("Creating "+nClusters+" child datasets");
        for (int i = 1; i <= nClusters; i++) {
            ICellCollection clusterCollection = new VirtualCellCollection(dataset, "Cluster_" + i);
            clusterCollection.setName("Cluster_" + i);
            clusterMap.put(i, clusterCollection);
        }
        
        // Add all the cells to clusters
        for(UUID id : cellMap.keySet()){
            int cluster = cellMap.get(id);
            fine("Assigning "+id.toString()+" to cluster "+cluster);
            ICell cell = dataset.getCollection().getCell(id);
            if(cell==null){
            	fine("Cell not found "+id.toString());
                continue;
            }
            clusterMap.get(cluster).addCell(cell);
        }
        
        
        int cellsInClusters = 0;
        for (int i = 1; i <= clusterMap.size(); i++) {

            ICellCollection c = clusterMap.get(i);
            
            cellsInClusters += c.size();

            if (c.hasCells()) {

                try {
                	dataset.getCollection().getProfileManager().copyCollectionOffsets(c);
                } catch (ProfileException e) {
                    e.printStackTrace();
                }

                group.addDataset(c);
                c.setName(group.getName() + "_" + c.getName());
                dataset.addChildCollection(c);

                // attach the clusters to their parent collection
                IAnalysisDataset clusterDataset = dataset.getChildDataset(c.getID());
                clusterDataset.setRoot(false);

                // set shared counts
                c.setSharedCount(dataset.getCollection(), c.size());
                dataset.getCollection().setSharedCount(c, c.size());

            }

        }
        dataset.addClusterGroup(group);
        fine("Clusters contain "+cellsInClusters+" cells compared to "+dataset.getCollection().size()+" in parent");
        return group;
    }
}
