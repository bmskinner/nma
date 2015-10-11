package analysis;

import java.io.Serializable;

public class ClusteringOptions implements Serializable {

	private static final long serialVersionUID = 1L;
	private ClusteringMethod type;
	private int clusterNumber;
	private HierarchicalClusterMethod hierarchicalMethod;
	private int iterations;
	private boolean autoClusterNumber;
	
	private boolean includeModality;
	private int modalityRegions;
	
	public ClusteringOptions(ClusteringMethod type){
		this.type = type;
	}


	public void setType(ClusteringMethod type) {
		this.type = type;
	}
	
	public ClusteringMethod getType() {
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
	
	public int getClusterNumber() {
		return clusterNumber;
	}


	public HierarchicalClusterMethod getHierarchicalMethod() {
		return hierarchicalMethod;
	}


	public int getIterations() {
		return iterations;
	}


	/**
	 * Get a string array of the options set here suitable
	 * for the Weka HierarchicalClusterer
	 * @return
	 */
	public String[] getOptions(){
	
		String[] options = null;
		
		if(this.type.equals(ClusteringMethod.EM)){
			options = new String[2];
			options[0] = "-I";                 // max. iterations
			options[1] = String.valueOf((Integer)iterations);
		}
		
		if(this.type.equals(ClusteringMethod.HIERARCHICAL)){
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
	
	public enum ClusteringMethod {
		EM ("Expectation maximisation", 0),
		HIERARCHICAL( "Hierarchical",1);


		private final String name;
		private final int code;

		ClusteringMethod(String name, int code){
			this.name = name;
			this.code = code;
		}

		public String toString(){
			return this.name;
		}

		public int code(){
			return this.code;
		}
	}
}
