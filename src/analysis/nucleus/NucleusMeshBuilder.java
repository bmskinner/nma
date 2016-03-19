package analysis.nucleus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import components.generic.ProfileType;
import components.generic.XYPoint;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;
import logging.Loggable;
import stats.Stats;

public class NucleusMeshBuilder implements Loggable {
	
	public static final int DIVISION_LENGTH = 10;
	
	public NucleusMeshBuilder(){
	}
	
	/**
	 * Create a mesh for the given Nucleus, using the default mesh size
	 * @param nucleus
	 * @return
	 * @throws Exception
	 */
	public NucleusMesh buildMesh(Nucleus nucleus) throws Exception{
		return buildMesh(nucleus, DIVISION_LENGTH);
	}
	
	/*
	 * Go through each segment in the nucleus.
	 * Since different nuclei must be compared,
	 * segment IDs are not useful here. Base on
	 * ordered segments from profiles.
	 * 
	 * Find the segment length, and subdivide appropriately.
	 * Make a vertex.
	 * 
	 * Vertices should start at the CoM, then go to the reference
	 * point, then around the perimeter.
	 */
	public NucleusMesh buildMesh(Nucleus nucleus, int meshSize) throws Exception{
		log(Level.FINEST, "Creating mesh for "+nucleus.getNameAndNumber());
		NucleusMesh mesh = new NucleusMesh(nucleus);
		
		mesh.addVertex(nucleus.getCentreOfMass());
		
		List<NucleusBorderSegment> list = nucleus.getProfile(ProfileType.REGULAR).getOrderedSegments();
		
		int segNumber = 0;
		for(NucleusBorderSegment seg : list){
			
			int divisions = seg.length() / meshSize; // find the number of divisions to make
			
			mesh.setDivision(segNumber++, divisions);
			log(Level.FINEST, "Dividing segment into "+divisions+" parts");
			
			double proportion = 1d / (double) divisions;
			
			for(double d=0; d<1; d+=proportion){
				int index = seg.getProportionalIndex(d);
				log(Level.FINEST, "Fetching point at index "+index);
				mesh.addVertex(nucleus.getBorderPoint(index));
			}
		}
		
		/*
		 * Add a ring of vertices between the CoM and each border point
		 */
		
		createEdges(mesh);
		
		log(Level.FINEST, "Created mesh");
		return mesh;
	}
	
	private void createEdges(NucleusMesh mesh){

		
		log(Level.FINEST, "Linking edges to CoM");
		for(int i=1; i<mesh.getVertexCount(); i++){
			NucleusMeshEdge e = new NucleusMeshEdge(mesh.getVertex(0), mesh.getVertex(i), 1);
			mesh.addEdge(e);
		}
		
		log(Level.FINEST, "Linking border pairs");
		for(int i=1, j=2; j<mesh.getVertexCount(); i++, j++){

			mesh.addEdge( new NucleusMeshEdge(mesh.getVertex(i), mesh.getVertex(j), 1) );
		}
		
		// Link the final perimeter point to the tip
		mesh.addEdge(  new NucleusMeshEdge(mesh.getVertex(mesh.getVertexCount()-1), mesh.getVertex(1), 1) );
		
		log(Level.FINEST, "Created edges");
	}
	
	/**
	 * Build a mesh for the input nucleus, based on a template mesh
	 * containing the divisions required for each segment
	 * @param mesh
	 * @return
	 * @throws Exception
	 */
	public NucleusMesh buildMesh(Nucleus nucleus, NucleusMesh template) throws Exception{
		log(Level.FINEST, "Creating mesh for "+nucleus.getNameAndNumber()+" using template "+template.getNucleusName());
		NucleusMesh mesh = new NucleusMesh(nucleus);
		
		log(Level.FINEST, "Adding centre of mass");
		mesh.addVertex(nucleus.getCentreOfMass());
		
		log(Level.FINEST, "Getting ordered segments");
		List<NucleusBorderSegment> list = nucleus.getProfile(ProfileType.REGULAR).getOrderedSegments();
		
		log(Level.FINEST, "Checking counts");
		if(template.getSegmentCount()!=list.size()){
			log(Level.FINEST, "Segment counts not equal:"+template.getSegmentCount()+" and "+list.size());
			throw new IllegalArgumentException("Segment counts are not equal");
		}
		log(Level.FINEST, "Segment counts equal:"+template.getSegmentCount()+" and "+list.size());
		
		int segNumber = 0;
		log(Level.FINEST, "Iterating over segments");
		for(NucleusBorderSegment seg : list){
			
			int divisions = template.getDivision(segNumber);
			log(Level.FINEST, "Seg "+segNumber+": "+divisions);
			segNumber++;
			
			log(Level.FINEST, "Dividing segment into "+divisions+" parts");
			
			double proportion = 1d / (double) divisions;
			
			for(double d=0; d<1; d+=proportion){
				int index = seg.getProportionalIndex(d);
				log(Level.FINEST, "Fetching point at index "+index);
				mesh.addVertex(nucleus.getBorderPoint(index));
			}
		}
		
		createEdges(mesh);
		log(Level.FINEST, "Created mesh");
		return mesh;
	}
	
	public class NucleusMesh {
		
		// Track the number of divisions for each segment to allow mapping between meshes
		private Map<Integer, Integer> segmentDivisions = new HashMap<Integer, Integer>();
		
		// Store the vertices in the mesh
		private List<NucleusMeshVertex> vertices = new ArrayList<NucleusMeshVertex>();
		
		// Not all vertices need to be linked - store edges for comparisons
		private List<NucleusMeshEdge> edges     = new ArrayList<NucleusMeshEdge>();
		
		private String nucleusName;
		
		public NucleusMesh(Nucleus n){
			this.nucleusName = n.getNameAndNumber();
		}
		
		
		public String getNucleusName() {
			return nucleusName;
		}

		
		public void addVertex(XYPoint p){
			vertices.add( new NucleusMeshVertex(vertices.size(), p)  );
		}
		
		public void addEdge(NucleusMeshEdge e){
			edges.add(e);
		}
		
		public int getSegmentCount(){
			return segmentDivisions.keySet().size();
		}
		
		public int getVertexCount(){
			return vertices.size();
		}
		
		public int getEdgeCount(){
			return edges.size();
		}
		
		public NucleusMeshVertex getVertex(int i){
			return  vertices.get(i);
		}
		
		public NucleusMeshEdge getEdge(int i){
			return edges.get(i);
		}
		
		public List<NucleusMeshEdge> getEdges(){
			return edges;
		}
		
		public void setDivision(int segment, int divisions){
			segmentDivisions.put(segment, divisions);
		}
		
		public int getDivision(int segment){
			return segmentDivisions.get(segment);
		}
				
		public List<NucleusMeshEdge> compare(NucleusMesh mesh){
			
			if(mesh.getEdgeCount() != this.getEdgeCount()){
				throw new IllegalArgumentException("Meshes are not comparable");
			}
			
			log(Level.FINEST, "Comparing meshes");
			
			List<NucleusMeshEdge> edges = new ArrayList<NucleusMeshEdge>();
			
			for(int i=0; i<getEdgeCount(); i++){
				edges.add( getEdge(i).compare(mesh.getEdge(i)) );
			}
			return edges;
		}
		
	}
	
	public class NucleusMeshEdge {
		private NucleusMeshVertex v1;
		private NucleusMeshVertex v2;
		private double ratio;
		
		public NucleusMeshEdge(NucleusMeshVertex v1, NucleusMeshVertex v2, double ratio){
			this.v1 = v1;
			this.v2 = v2;
			this.ratio = ratio;
		}

		public NucleusMeshVertex getV1() {
			return v1;
		}

		public NucleusMeshVertex getV2() {
			return v2;
		}

		public double getRatio() {
			return ratio;
		}
		
		public double getLog2Ratio(){
			return Stats.calculateLog2Ratio(ratio);
		}
		
		/**
		 * Compare the length of this edge to the given edge, and return
		 * a new edge with the ratio
		 * @param e
		 * @return
		 */
		public NucleusMeshEdge compare(NucleusMeshEdge e){
			
			double thisDistance = v1.getLengthTo(v2);
			double thatDistance = e.getV1().getLengthTo(e.getV2());
			
			double ratio = thisDistance / thatDistance ;
			return new NucleusMeshEdge(v1, v2, ratio);
			
		}
						
		public String toString(){
			return v1.getNumber()+"-"+v2.getNumber();
		}
		
	}
	
	public class NucleusMeshVertex {
		
		private int number; // the number in the mesh - use to compare vertexes between nuclei
		private XYPoint position; // the posiiton of the vertex
		
		public NucleusMeshVertex(int i, XYPoint p){
			this.number = i;
			this.position = p;
		}

		public int getNumber() {
			return number;
		}

		public XYPoint getPosition() {
			return position;
		}
		
		public double getLengthTo(NucleusMeshVertex v){
			return position.getLengthTo(v.getPosition());
		}
		
		
	}

}
