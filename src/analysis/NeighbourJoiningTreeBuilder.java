package analysis;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import components.CellCollection;
import components.generic.BorderTag;
import components.generic.ProfileType;
import components.nuclei.Nucleus;
import jebl.evolution.distances.BasicDistanceMatrix;
import jebl.evolution.distances.DistanceMatrix;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.NeighborJoiningTreeBuilder;
import jebl.evolution.trees.Tree;

public class NeighbourJoiningTreeBuilder extends AnalysisWorker {
	
	private Tree tree;

	public NeighbourJoiningTreeBuilder(AnalysisDataset dataset, Logger programLogger) {
		super(dataset, programLogger);
		this.setProgressTotal(dataset.getCollection().cellCount());
		
	}

	@Override
	protected Boolean doInBackground() throws Exception {

		try{
//			this.tree = makeTree();
			return true;
		} catch(Exception e){
			logError("Error in neighbour joining", e);
			return false;
		}
	}
	
	public Tree getTree(){
		return this.tree;
	}
	
	/**
	 * Create the taxa for tree joining using the nuclei in the active dataset
	 * @return
	 */
	private Collection<Taxon> createTaxa(){
		
		Collection<Taxon> result = new HashSet<Taxon>(); 
		for(Nucleus n: getDataset().getCollection().getNuclei()){
			result.add(Taxon.getTaxon(n.getSourceFolder().getAbsolutePath()+"-"+n.getNameAndNumber()));
		}
		return result;
	}
	
	/**
	 * Get the similarty matrix if present in the collection, or 
	 * create it
	 * @param collection
	 * @return
	 * @throws Exception
	 */
//	private double[][] makeSimilarityMatrix(CellCollection collection) throws Exception{
//		
//		if(collection.hasNucleusSimilarityMatrix()){
//			log(Level.FINER, "Found existing matrix");
//			publish(collection.size());
//			return collection.getNucleusSimilarityMatrix();
//		} else {
//			log(Level.FINER, "Creating similarity matrix");
//			double[][] matrix = new double[collection.size()][collection.size()];
//
//			List<Nucleus> nuclei = collection.getNuclei();
//			int i = 0;
//			for(Nucleus n1 : nuclei){
//
//				int j = 0;
//				for(Nucleus n2 : nuclei){
//					
//					/*
//					 * TODO: We can cut this in half by flipping the matrix
//					 *    0  1  2  3  4 j
//					 *  0          b  a  
//					 *  1          d  c
//					 *  2 
//					 *  3 b d
//					 *  4 a c
//					 *  i
//					 *  
//					 *  length = 5
//					 *  for(i=0; i<length; i++{
//					 *  
//					 *  	for(j=length-1; j>=length/2 j--{
//					 *  		matrix[i][j]= n
//					 *  		matrix[j][i]= n
//					 *  	}
//					 *  	
//					 *  }
//					 */
////					for (i = 0;i < size; ++i) {
////						for (j = 0; j < i; ++j) {
////							result = do_operation(i,j);
////							matrix[i][j] = result;
////							matrix[j][i] = result ;
////						}
////					}
//					
//
//					double score = n1.getProfile(ProfileType.REGULAR, BorderTag.REFERENCE_POINT)
//							.absoluteSquareDifference(n2.getProfile(ProfileType.REGULAR, BorderTag.REFERENCE_POINT));
//										
//					matrix[i][j] = score;
//
//					j++;
//				}
//				publish(i++);
//			}
//			collection.setNucleusSimilarityMatrix(matrix);
//			return matrix;
//		}
//	}
	
//	private Tree makeTree() throws Exception {
//		log(Level.FINER, "Making taxa");
//		Collection<Taxon> taxa = createTaxa();
//		
//		log(Level.FINER, "Making distances");
////		double[][] distances = makeSimilarityMatrix(getDataset().getCollection());
//		
//		log(Level.FINER, "Making distance matrix");
////		DistanceMatrix matrix = new BasicDistanceMatrix(taxa, distances);
//
//		this.fireCooldown();
//		log(Level.FINER, "Making builder");
////		NeighborJoiningTreeBuilder builder = new NeighborJoiningTreeBuilder(matrix);
//		
//		log(Level.FINER, "Building tree");
//		return builder.build();
//		
//	}

}
