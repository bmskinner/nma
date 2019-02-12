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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
 * can be performed on these clusters. The mapping format is {@code <UUID> <cluster_number>}. 
 * The nucleus name/number is not used because there can be multiple cells with the same name
 * if this is a merge of datasets.
 * @author bms41
 * @since 1.14.0
 *
 */
public class ClusterFileAssignmentMethod extends SingleDatasetAnalysisMethod {
	
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
	 * @throws ClusteringMethodException 
	 */
	private boolean isFileFormatValid() throws ClusteringMethodException {		
		ICellCollection collection = dataset.getCollection();
		int cells = 0;
		List<UUID> found = new ArrayList<>(collection.size());
		Map<Integer, UUID> notInDataset = new HashMap<>(0);
		
		List<Integer> idErrors = new ArrayList<>();
		List<Integer> numErrors = new ArrayList<>();
		int lineNo = 0;
		try {
			FileInputStream fstream = new FileInputStream(clusterFile);
			BufferedReader br = new BufferedReader(
					new InputStreamReader(fstream, Charset.forName("ISO-8859-1")));

			String strLine;
			while (( strLine = br.readLine()) != null) {
				lineNo++;
				String[] arr = strLine.split(DELIMITER);
				UUID id = null;
				try {
					id = UUID.fromString(arr[0]);
				} catch(IllegalArgumentException e) {
					if(lineNo==1) { // check for a header line
						log("Mapping file does not have a cell ID in line 1; assuming line 1 is a header");
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
				if(collection.contains(id)) {
					cells++;			
				} else {
					notInDataset.put(lineNo, id);
				}



			}
			fstream.close();
		}
		catch (Exception e) {
			warn("Parsing error reading mapping file");
			return false;
		}
		
		boolean ok = true;
		
		if(idErrors.size()!=0) {
			ok=false;
			warn("Mapping file has errors in the cell id column");
			for(Integer line : idErrors) {
				warn(String.format("Line %d does not have a cell id", line));
			}
		}
		
		if(numErrors.size()!=0) {
			ok=false;
			warn("Mapping file has errors in the cluster number column");
			for(Integer line : numErrors) {
				warn(String.format("Line %d does not have a readable number", line));
			}
		}
		
		if(notInDataset.size()!=0) {
			warn(String.format("Mapping file (%d cells) has cells not in the dataset (%d cells)", cells, collection.size()));
			for(Integer line : notInDataset.keySet()) {
				if(notInDataset.get(line)!=null)
					warn(String.format("Line %d: Cell with id %s is not in dataset", line, notInDataset.get(line) ));
			}
			ok = false;
		}
		
		if(collection.size()>cells) {
			List<UUID> missing = collection.getCellIDs().stream().filter(id->!found.contains(id)).collect(Collectors.toList());
			warn(String.format("Mapping file (%d cells) does not contain all the dataset cells (%d cells)", cells, collection.size()));
			for(UUID id : missing) {
				warn(String.format("Cell with id %s is not in the mapping file", id ));
			}
			ok = false;
		}
		
		if(!ok)
			warn("Unable to assign clusters; the mapping file is invalid. Please correct and try again.");
		
		return ok;
	}
	
	private void readMapFile() throws ClusteringMethodException {

        try {
            FileInputStream fstream = new FileInputStream(clusterFile);
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(fstream, Charset.forName("ISO-8859-1")));

            String strLine;
            int lineNo = 0;
            while (( strLine = br.readLine()) != null) {
            	lineNo++;
            	if(skipFirstLine && lineNo==1) {
            		continue;
            	}
            	String[] arr = strLine.split(DELIMITER);
            	UUID id = UUID.fromString(arr[0]);
            	int cluster = Integer.parseInt(arr[1]);
            	cellMap.put(id, cluster);
            	fireProgressEvent();
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
