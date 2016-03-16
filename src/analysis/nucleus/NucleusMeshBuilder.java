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

public class NucleusMeshBuilder implements Loggable {
	
	private static final int DIVISION_LENGTH = 20;
	
	public NucleusMeshBuilder(){
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
	public NucleusMesh buildMesh(Nucleus nucleus) throws Exception{
		log(Level.FINEST, "Creating mesh for "+nucleus.getNameAndNumber());
		NucleusMesh mesh = new NucleusMesh(nucleus);
		
		mesh.addVertex(new NucleusMeshVertex(0, nucleus.getCentreOfMass()));
		
		List<NucleusBorderSegment> list = nucleus.getProfile(ProfileType.REGULAR).getOrderedSegments();
		
		int vertex = 1;
		int segNumber = 0;
		for(NucleusBorderSegment seg : list){
			
			int divisions = seg.length() / DIVISION_LENGTH; // find the number of divisions to make
			
			mesh.setDivision(segNumber++, divisions);
			log(Level.FINEST, "Dividing segment into "+divisions+" parts");
			
			double proportion = 1d / (double) divisions;
			
			for(double d=0; d<1; d+=proportion){
				int index = seg.getProportionalIndex(d);
				log(Level.FINEST, "Fetching point at index "+index);
				mesh.addVertex(new NucleusMeshVertex(vertex++, nucleus.getBorderPoint(index)));
			}
		}
		log(Level.FINEST, "Created mesh");
		return mesh;
		
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
		mesh.addVertex(new NucleusMeshVertex(0, nucleus.getCentreOfMass()));
		
		log(Level.FINEST, "Getting ordered segments");
		List<NucleusBorderSegment> list = nucleus.getProfile(ProfileType.REGULAR).getOrderedSegments();
		
		log(Level.FINEST, "Checking counts");
		if(template.getSegmentCount()!=list.size()){
			log(Level.FINEST, "Segment counts not equal:"+template.getSegmentCount()+" and "+list.size());
			throw new IllegalArgumentException("Segment counts are not equal");
		}
		log(Level.FINEST, "Segment counts equal:"+template.getSegmentCount()+" and "+list.size());
		
		int vertex = 1;
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
				mesh.addVertex(new NucleusMeshVertex(vertex++, nucleus.getBorderPoint(index)));
			}
		}
		log(Level.FINEST, "Created mesh");
		return mesh;
	}
	
	public class NucleusMesh {
		
		private Map<Integer, Integer> segmentDivisions = new HashMap<Integer, Integer>();
		private List<NucleusMeshVertex> vertices = new ArrayList<NucleusMeshVertex>();
		private String nucleusName;
		
		public NucleusMesh(Nucleus n){
			this.nucleusName = n.getNameAndNumber();
		}
		
		
		public String getNucleusName() {
			return nucleusName;
		}

		public void addVertex(NucleusMeshVertex v){
			vertices.add(v);
		}
		
		public int getSegmentCount(){
			return segmentDivisions.keySet().size();
		}
		
		public int getVertexCount(){
			return vertices.size();
		}
		
		public void setDivision(int segment, int divisions){
			segmentDivisions.put(segment, divisions);
		}
		
		public int getDivision(int segment){
			return segmentDivisions.get(segment);
		}
		
		public double getDistance(int vertex1, int vertex2){
			XYPoint point1 = vertices.get(vertex1).getPosition();
			XYPoint point2 = vertices.get(vertex2).getPosition();
			return point1.getLengthTo(point2);
		}
		
		public double compareDistance(int vertex1, int vertex2, NucleusMesh mesh){
			double thisDistance = getDistance(vertex1, vertex2);
			double thatDistance = mesh.getDistance(vertex1, vertex2);
			
			return thisDistance / thatDistance;
		}
		
		public List<NucleusMeshEdge> compare(NucleusMesh mesh){
			
			List<NucleusMeshEdge> edges = new ArrayList<NucleusMeshEdge>();
			log(Level.FINEST, "Comparing meshes");
			
			log(Level.FINEST, "Comparing each to CoM");
			for(int i=1; i<vertices.size(); i++){
				double ratio = compareDistance(0, i, mesh);
				edges.add( new NucleusMeshEdge(vertices.get(0), vertices.get(i), ratio) );
				
				log(Level.FINEST, "0 - "+i+": "+ratio);
			}
			
			log(Level.FINEST, "Comparing border pairs");
			for(int i=1, j=2; j<vertices.size(); i++, j++){
				double ratio = compareDistance(i, j, mesh);
				edges.add( new NucleusMeshEdge(vertices.get(i), vertices.get(j), ratio) );
				log(Level.FINEST, i+" - "+j+": "+ratio);
			}
			
			double ratio = compareDistance(vertices.size()-1, 1, mesh);
			edges.add(  new NucleusMeshEdge(vertices.get(vertices.size()-1), vertices.get(1), ratio) );
			log(Level.FINEST, "e - "+1+": "+ratio);
			
			log(Level.FINEST, "Comparion complete");
			
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
		
		
		
	}

}
