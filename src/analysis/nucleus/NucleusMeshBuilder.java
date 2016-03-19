package analysis.nucleus;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import components.generic.Equation;
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
		
		mesh.addVertex(nucleus.getCentreOfMass(), false);
		
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
				mesh.addVertex(nucleus.getBorderPoint(index), true);
			}
		}
				
		createEdges(mesh);
		
//		mesh.subdivide();
//		mesh.pruneOverlaps();
		
		
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
		mesh.addVertex(nucleus.getCentreOfMass(), false);
		
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
				mesh.addVertex(nucleus.getBorderPoint(index), true);
			}
		}
		
		
		createEdges(mesh);
//		mesh.subdivide();
//		mesh.pruneOverlaps();
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
		
		
		/**
		 * Private constructor, to return a compared mesh
		 * @param mesh
		 * @param edges
		 */
		private NucleusMesh(NucleusMesh mesh, List<NucleusMeshEdge> edges){
			this.segmentDivisions = mesh.segmentDivisions;
			this.vertices         = mesh.vertices;
			this.edges            = edges;
		}
		
		
		public String getNucleusName() {
			return nucleusName;
		}

		
		/**
		 * Add the given point as a vertex to the mesh. Returns the number
		 * of the added vertex
		 * @param p
		 * @param peripheral
		 * @return
		 */
		public int addVertex(XYPoint p, boolean peripheral){
			int newIndex = vertices.size();
			vertices.add( new NucleusMeshVertex(newIndex, p, peripheral)  );
			return newIndex;
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
				
		public NucleusMesh compare(NucleusMesh mesh){
			
			if(mesh.getEdgeCount() != this.getEdgeCount()){
				throw new IllegalArgumentException("Meshes are not comparable: "+mesh.getEdgeCount()+" versus "+getEdgeCount());
			}
			
			log(Level.FINEST, "Comparing meshes");
			
			List<NucleusMeshEdge> edges = new ArrayList<NucleusMeshEdge>();
			
			for(int i=0; i<getEdgeCount(); i++){
				edges.add( getEdge(i).compare(mesh.getEdge(i)) );
			}
			return new NucleusMesh(this, edges);
		}
		
		/**
		 * Create new vertices at the midpoints of all edges that have
		 * one or fewer peripheral vertices. Add new edges between them.
		 */
		public void subdivide(){
			List<NucleusMeshEdge> toAdd = new ArrayList<NucleusMeshEdge>();
			
			for(final Iterator<NucleusMeshEdge> it = edges.iterator(); it.hasNext(); ){

				NucleusMeshEdge e = it.next();
				if( ! e.isPeripheral()){
					int index = addVertex (e.getMidpoint(), false);
					toAdd.add( new NucleusMeshEdge(e.getV1(), vertices.get(index), 1));
					toAdd.add( new NucleusMeshEdge(e.getV2(), vertices.get(index), 1));
					it.remove();

				}
			}
			
			for(NucleusMeshEdge e : toAdd){
				addEdge(e); 
			}
		}
		
		public void makePairwiseEdges(){
			List<NucleusMeshEdge> toAdd = new ArrayList<NucleusMeshEdge>();
			
			for(NucleusMeshVertex v1 : vertices){
				
				for(NucleusMeshVertex v2 : vertices){
					if(v1.equals(v2)){
						continue;
					}
					toAdd.add( new NucleusMeshEdge(v1, v2, 1));
				}
				
			}
			
			for(NucleusMeshEdge e : toAdd){
				addEdge(e); 
			}
			
		}
		
		/**
		 * Remove all edges that intersect another edge in the mesh.
		 * Discards the longer edge of the two
		 */
		public void pruneOverlaps(){
			Set<NucleusMeshEdge> toRemove = new HashSet<NucleusMeshEdge>();
			for(NucleusMeshEdge e1 : edges){
				
				for(NucleusMeshEdge e2 : edges){
					if(e1.equals(e2)){
						continue;
					}
					
					// TODO: account for the centre of mass
					if(e1.crosses(e2)){
						
						// don't count edges already pruned
						if(toRemove.contains(e1) || toRemove.contains(e2)){
							continue;
						}
						
						// Don't remove borders
						if( e1.isPeripheral()  && e2.isPeripheral() ){
							continue;
						}
						
						// Remove the longer edge if neither are on the border
						if( ! e1.isPeripheral()  && ! e2.isPeripheral()){

							if(e1.isLongerThan(e2)){
								toRemove.add(e1);
							} else {
								toRemove.add(e2);
							}
						} else {
							// If one is peripheral, remove the other
							if(  e1.isPeripheral()){
								toRemove.add(e2);
							} else if(e2.isPeripheral()) {
								toRemove.add(e1);
							}
						}
					}
				}
				
			}
			
			for(NucleusMeshEdge e : toRemove){
				edges.remove(e);
			}
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
		
		public double getLength(){
			return v1.getLengthTo(v2);
		}
		
		public XYPoint getMidpoint(){
			Equation eq = new Equation(v1.getPosition(), v2.getPosition());
			if(v1.getPosition().getX()<v2.getPosition().getX()){
				return eq.getPointOnLine(v1.getPosition(), getLength()/2);
			} else {
				return eq.getPointOnLine(v1.getPosition(), -(getLength()/2));
			}
			
		}
		
		/**
		 * Test if both vertices lie on the periphery of the mesh
		 * @return
		 */
		public boolean isPeripheral(){
			return v1.isPeripheral() && v2.isPeripheral();
		}
		
		public boolean isLongerThan(NucleusMeshEdge e){
			return getLength() > e.getLength();
		}
		
		public boolean crosses(NucleusMeshEdge e){
			
			Line2D line1 = new Line2D.Double(v1.getPosition().asPoint(), v2.getPosition().asPoint());
			Line2D line2 = new Line2D.Double(e.v1.getPosition().asPoint(), e.v2.getPosition().asPoint());

			if(line1.intersectsLine(line2)){
				
				// Ignore endpoints overlapping
				if(    line1.getP1().equals(line2.getP1())  || line1.getP2().equals(line2.getP1())
					|| line2.getP1().equals(line1.getP2())  || line2.getP2().equals(line1.getP2())){
				
					return false;
				} else {
					return true;
				}
			} 
			return false;
		}
		
		/**
		 * Compare the length of this edge to the given edge, and return
		 * a new edge with the ratio
		 * @param e
		 * @return
		 */
		public NucleusMeshEdge compare(NucleusMeshEdge e){
			
			double thisDistance = v1.getLengthTo(v2);
			double thatDistance = e.v1.getLengthTo(e.v2);
			
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
		private boolean peripheral; // is the vertex on the border of the object
		
		public NucleusMeshVertex(int i, XYPoint p, boolean peripheral){
			this.number = i;
			this.position = p;
			this.peripheral = peripheral;
		}
		
		public boolean isPeripheral() {
			return peripheral;
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
		
		public boolean overlaps(NucleusMeshVertex v){
			return position.overlapsPerfectly(v.position);
		}
		
		
	}

}
