package analysis;

import analysis.nucleus.NucleusClusterer;

public class ClusteringOptions {
	
	private int type;
	private int clusterNumber;
	private HierarchicalClusterMethod hierarchicalMethod;
	private int iterations;
	private boolean autoClusterNumber;
	
	private boolean includeModality;
	private int modalityRegions;
	
	public ClusteringOptions(int type){
		this.type = type;
	}


	public void setType(int type) {
		this.type = type;
	}
	
	public int getType() {
		return type;
	}
	
	public void setAutoClusterNumber(boolean autoClusterNumber) {
		this.autoClusterNumber = autoClusterNumber;
	}
	
	public boolean isIncludeModality() {
		return includeModality;
	}


	public void setIncludeModality(boolean includeModality) {
		this.includeModality = includeModality;
	}

	public int getModalityRegions() {
		return modalityRegions;
	}


	public void setModalityRegions(int modalityRegions) {
		this.modalityRegions = modalityRegions;
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
	public String[] getOptions(){
	
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
			options[3] = hierarchicalMethod.code();
		}
		
		return options;
	}
	
	public enum HierarchicalClusterMethod {
		WARD 			("Ward", "WARD"), 
		SINGLE			("Single", "SINGLE"), 
		COMPLETE		("Complete", "COMPLETE"), 
		AVERAGE			("Average", "AVERAGE"), 
		MEAN			("Mean", "MEAN"),
		CENTROID		("Centroid", "CENTROID"),
		ADJCOMPLETE		("Adjusted complete", "ADJCOMPLETE"),
		NEIGHBOR_JOINING("Neighbour joining", "NEIGHBOR_JOINING");
		
		private final String name;
		private final String code;
		
		HierarchicalClusterMethod(String name, String code){
			this.name = name;
			this.code = code;
		}
		
		public String toString(){
			return this.name;
		}
		
		public String code(){
			return this.code;
		}
	}
}
