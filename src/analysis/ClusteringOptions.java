package analysis;

import analysis.nucleus.NucleusClusterer;

public class ClusteringOptions {
	
	private int type;
	private int clusterNumber;
	private HierarchicalClusterMethod hierarchicalMethod;
	private int iterations;
	
	public ClusteringOptions(int type){
		this.type = type;
	}


	public void setType(int type) {
		this.type = type;
	}


	public void setClusterNumber(int clusterNumber) {
		this.clusterNumber = clusterNumber;
	}


	public void setHierarchicalMethod(HierarchicalClusterMethod hierarchicalMethod) {
		this.hierarchicalMethod = hierarchicalMethod;
	}

	public void setIterations(int iterations) {
		this.iterations = iterations;
	}



	/**
	 * Get a string array of the options set here suitable
	 * for the Weka HierarchicalClusterer
	 * @return
	 */
	private String[] getOptions(){
	
		String[] options = null;
		
		if(this.type==NucleusClusterer.EM){
			options = new String[2];
			options[0] = "-I";                 // max. iterations
			options[1] = String.valueOf((Integer)iterations);
		}
		
		if(this.type==NucleusClusterer.HIERARCHICAL){
			options = new String[4];
			options[0] = "-N";                 // number of clusters
			options[1] = String.valueOf((Integer)clusterNumber);
			options[2] = "-L";                 // algorithm
			options[3] = hierarchicalMethod.toString();
		}
		
		return options;
	}
	
	public enum HierarchicalClusterMethod {
		WARD ("WARD");
		
		private final String name;
		
		HierarchicalClusterMethod(String name){
			this.name = name;
		}
		
		public String toString(){
			return this.name;
		}
	}
}
