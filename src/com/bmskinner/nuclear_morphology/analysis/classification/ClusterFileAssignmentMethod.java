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
package com.bmskinner.nuclear_morphology.analysis.classification;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.ClusterAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.DefaultClusterGroup;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.datasets.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.datasets.VirtualCellCollection;
import com.bmskinner.nuclear_morphology.components.options.DefaultOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Allow clusters to be assigned to a dataset, based on their map in a file.
 * This is used to import clusters determined outside the program, so (e.g.) consensus refolding
 * can be performed on these clusters. The mapping format is {@code <UUID> <cluster_number>}. 
 * The nucleus name/number is not used because there can be multiple cells with the same name
 * if this is a merge of datasets.
 * @author bms41
 * @since 1.14.0
 *
 */
public class ClusterFileAssignmentMethod extends SingleDatasetAnalysisMethod {
	
	private static final Logger LOGGER = Logger.getLogger(ClusterFileAssignmentMethod.class.getName());
	
	private File clusterFile;
	private Map<UUID, Integer> cellMap;
	private boolean skipFirstLine = false;
	private static final String DELIMITER = "\t";
	
	public ClusterFileAssignmentMethod(@NonNull IAnalysisDataset d, @NonNull File f) {
		super(d);
		clusterFile = f;
		cellMap = new HashMap<>(dataset.getCollection().size());
	}

	@Override
	public IAnalysisResult call() throws Exception {
		
		if(!isFileFormatValid())
			return null;
		
	 LOGGER.info("Reading map file");
		readMapFile();
        LOGGER.fine("Read "+cellMap.size()+" cell ids");
		IClusterGroup group = assignClusters();
		return new ClusterAnalysisResult(dataset, group);
	}
	
	/**
	 * The valid file format for this mapping is a tab separated file,
	 * with two values per line: a cell UUID, and a cluster integer.
	 * e.g. 00000000-0000-0000-0000-000000000001	1
	 * @return true if the file meets the requirements for cluster assignment, false otherwise
	 * @throws ClusteringMethodException 
	 */
	private boolean isFileFormatValid() throws ClusteringMethodException {		
		ICellCollection collection = dataset.getCollection();
		List<UUID> found = new ArrayList<>(collection.size());
		Map<Integer, UUID> notInDataset = new HashMap<>();
		
		List<Integer> idErrors  = new ArrayList<>();
		List<Integer> numErrors = new ArrayList<>();
		int lineNo = 0;
		try(FileInputStream fstream = new FileInputStream(clusterFile);
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream, StandardCharsets.ISO_8859_1));) {
			

			String strLine;
			while (( strLine = br.readLine()) != null) {
				lineNo++;
				String[] arr = strLine.split(DELIMITER);
				UUID id = null;
				try {
					id = UUID.fromString(arr[0]);
				} catch(IllegalArgumentException e) {
					if(lineNo==1) { // check for a header line
					 LOGGER.info("Mapping file does not have a cell ID in line 1; assuming line 1 is a header");
						skipFirstLine = true;
						continue;
					}
					idErrors.add(lineNo);
				}

				try {
					Integer.parseInt(arr[1]);
				} catch(NumberFormatException e) {
					numErrors.add(lineNo);
				}
				found.add(id);
				if(!collection.contains(id)) {
					notInDataset.put(lineNo, id);
				}
			}
		}
		catch (Exception e) {
			LOGGER.warning("Parsing error reading mapping file");
			return false;
		}
		
		boolean ok = true;
		
		if(!idErrors.isEmpty()) {
			ok = false;
			LOGGER.warning("Mapping file has errors in the cell id column");
		}
		
		if(!numErrors.isEmpty()) {
			ok=false;
			LOGGER.warning("Mapping file has errors in the cluster number column");
		}
				
		if(!ok)
			LOGGER.warning("Unable to assign clusters; the mapping file is invalid. Please correct and try again.");
		return ok;
	}
	
	private void readMapFile() throws ClusteringMethodException {

        try(FileInputStream fstream = new FileInputStream(clusterFile);
            BufferedReader br = new BufferedReader(
                        new InputStreamReader(fstream, StandardCharsets.ISO_8859_1));) {

            String strLine;
            int lineNo = 0;
            while (( strLine = br.readLine()) != null) {
            	lineNo++;
            	if(lineNo==1 && skipFirstLine) {
            		continue;
            	}
            	String[] arr = strLine.split(DELIMITER);
            	UUID id = UUID.fromString(arr[0]);
            	int cluster = Integer.parseInt(arr[1]);
            	cellMap.put(id, cluster);
            	fireProgressEvent();
            }
        }
        catch (Exception e) {
        	LOGGER.log(Loggable.STACK, "Error parsing mapping file", e);
        	throw new ClusteringMethodException("Invalid mapping file format");
        }
    }

	
	private IClusterGroup assignClusters(){
        LOGGER.fine("Assigning clusters");

        Map<Integer, ICellCollection> clusterMap = new HashMap<>();
        
        int clusterNumber = dataset.getMaxClusterGroupNumber() + 1;

        IClusterGroup group = new DefaultClusterGroup(IClusterGroup.CLUSTER_GROUP_PREFIX + "_" + clusterNumber, 
        		new DefaultOptions());

        // Make collections for the new clusters
        long nClusters = cellMap.values().stream().distinct().count();
        LOGGER.fine("Creating "+nClusters+" child datasets");
        for (int i = 1; i <= nClusters; i++) {
            ICellCollection clusterCollection = new VirtualCellCollection(dataset, "Cluster_" + i);
            clusterCollection.setName("Cluster_" + i);
            clusterMap.put(i, clusterCollection);
        }
        
        // Get dataset cells not in the map file
        List<ICell> unmappedCells = dataset.getCollection().stream()
        		.filter(c->!cellMap.keySet().contains(c.getId()))
        		.collect(Collectors.toList());
        
        // Add all the cells to clusters
        for(Entry<UUID, Integer> entry : cellMap.entrySet()){
            int cluster = entry.getValue();
            LOGGER.fine("Assigning "+entry.getKey().toString()+" to cluster "+cluster);
            ICell cell = dataset.getCollection().getCell(entry.getKey());
            if(cell==null){
            	LOGGER.fine("Cell not found "+entry.getKey().toString());
                continue;
            }
            
            // Check that the cluster map has a cluster with the given number
            // Clusterers like DBSCAN can create a cluster 0 for unassigned 
            // nuclei. Cluster number starts from 1, so there is no cluster zero
            // to assign to. Put these in the unmapped collection
            if(clusterMap.containsKey(cluster)) {           
            	clusterMap.get(cluster).addCell(cell);
            } else {
            	LOGGER.fine("No cluster defined for "+entry.getKey().toString()+": expected "+cluster);
            	unmappedCells.add(cell);
            }
        }
        
        // Add unmapped cells to a final cluster
        ICellCollection clusterCollection = new VirtualCellCollection(dataset, "Unmapped");
        clusterCollection.setName("Unmapped");
        clusterCollection.addAll(unmappedCells);
        clusterMap.put( (int)nClusters+1, clusterCollection);
        

        // Assign profiles to the new cluster collections
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
        LOGGER.fine("Clusters contain "+cellsInClusters+" cells compared to "+dataset.getCollection().size()+" in parent");
        return group;
    }
}
