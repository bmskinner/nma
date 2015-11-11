/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package charting.datasets;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import components.Cell;
import components.nuclei.Nucleus;
import analysis.AnalysisDataset;

public class VennDatasetCreator {
	
	public VennDatasetCreator(List<AnalysisDataset> list){

	}
	
	public void createVennClusters(List<AnalysisDataset> list){
		
		List<VennSet> venns = new ArrayList<VennSet>();
		
		VennSet first = new VennSet(list.get(0));
		venns.add(first);
		
		for(AnalysisDataset dataset : list){
			
			for(VennSet v : venns){
				if(v.addDataset(dataset)){
					// it overlapped
				} else {
					// make a new one
					venns.add(new VennSet(dataset));
				}
			}
		}
		
		// There may be overlaps still between VennSets
		
		for(VennSet v1 : venns){
			
			for(VennSet v2 : venns){
				if(v1.overlaps(v2)){
					v1.merge(v2);
				}
			}
		}


			
//			int i = 0;
//			for(AnalysisDataset dataset2 : list){
//				
////				if(dataset2.getUUID().equals(dataset.getUUID())){
////
////				} else {
////					// compare the number of shared nucleus ids
////					int shared = 0;
////					for(Nucleus n : dataset.getCollection().getNuclei()){
////						UUID n1id = n.getID();
////						for(Nucleus n2 : dataset2.getCollection().getNuclei()){
////							if( n2.getID().equals(n1id)){
////								shared++;
////							}
////						}
//////						if( dataset2.getCollection().getNuclei().contains(n)){
//////							shared++;
//////						}
////					}
////					DecimalFormat df = new DecimalFormat("#0.00"); 
////					double pct = ((double) shared / (double) dataset2.getCollection().getNucleusCount())*100;
////	
////				}
//				i++;
//			}

//		}
		
	}
	
	
	public class VennSet {
		
		private List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
		private List<Cell> cells = new ArrayList<Cell>();
		
		public VennSet(AnalysisDataset dataset){
			this.list.add(dataset);
			cells.addAll(dataset.getCollection().getCells());
		}
		
		/**
		 * Add a dataset, or return false if no overlaps
		 * @param dataset
		 * @return
		 */
		public boolean addDataset(AnalysisDataset dataset){
			boolean overlap = false;
			for(Cell cell : dataset.getCollection().getCells()){
				
				if(this.cells.contains(cell)){
					overlap = true;
				}
			}
			
			if(overlap){
				this.list.add(dataset);
				cells.addAll(dataset.getCollection().getCells());
			}
			return overlap;
		}
		
		public List<Cell> getCells(){
			return this.cells;
		}
		
		public List<AnalysisDataset> getDatasets(){
			return this.list;
		}
		
		public boolean overlaps(VennSet v){
			boolean overlap = false;
			for(Cell cell : v.getCells()){
				if(this.cells.contains(cell)){
					overlap = true;
				}
			}
			return overlap;
		}
		
		public void merge(VennSet v){
			for(AnalysisDataset d : v.getDatasets()){
				this.addDataset(d);
			}
			
		}
	}
	

}
