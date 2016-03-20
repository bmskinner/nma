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
import java.util.stream.Collector;
import java.util.stream.Collectors;

import analysis.nucleus.NucleusMeshBuilder.NucleusMeshEdge;
import components.generic.Equation;
import components.generic.ProfileType;
import components.generic.XYPoint;
import components.nuclear.BorderPoint;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;
import ij.process.FloatPolygon;
import logging.Loggable;
import stats.Stats;

public class NucleusMeshBuilder implements Loggable {
	
	public static final int DIVISION_LENGTH = 10;
	
	private Nucleus nucleus;
	
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
		
		mesh.makeCentreVertices();
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
			mesh.getVertex(0).addEdge(e);
			mesh.getVertex(i).addEdge(e);
		}
		
		log(Level.FINEST, "Linking border pairs");
		for(int i=1, j=2; j<mesh.getVertexCount(); i++, j++){
			NucleusMeshEdge e = new NucleusMeshEdge(mesh.getVertex(i), mesh.getVertex(j), 1);
			mesh.addEdge( e );
			mesh.getVertex(i).addEdge(e);
			mesh.getVertex(j).addEdge(e);
		}
		
		// Link the final perimeter point to the tip
		NucleusMeshEdge e = new NucleusMeshEdge(mesh.getVertex(mesh.getVertexCount()-1), mesh.getVertex(1), 1);
		mesh.addEdge(  e );
		mesh.getVertex(mesh.getVertexCount()-1).addEdge(e);
		mesh.getVertex(1).addEdge(e);
		
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
		mesh.makeCentreVertices();
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
		
		private Nucleus nucleus;
		
		private String nucleusName;
		
		public NucleusMesh(Nucleus n){
			this.nucleus = n;
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
			this.nucleus          = mesh.nucleus;
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
		 * Starting at the reference point, create vertices
		 * between the peripheral vertex pairs down to the tail
		 */
		public void makeCentreVertices(){
			
			List<NucleusMeshVertex> list = getPeripheralVertices();
			/*
			 * The first index in the list is the reference point.
			 * Take the second and last, the third and next-to-last etc
			 */
			for(int i=1; i<list.size()/2; i++){
				
				NucleusMeshVertex v1 = list.get(i);
				NucleusMeshVertex v2 = list.get(list.size()-i);
				
				NucleusMeshEdge test = new NucleusMeshEdge(v1, v2, 1);
				addVertex (test.getMidpoint(), false);
			}
			
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
					
					NucleusMeshEdge e1 = new NucleusMeshEdge(e.getV1(), vertices.get(index), 1);
					NucleusMeshEdge e2 = new NucleusMeshEdge(e.getV2(), vertices.get(index), 1);
					
					getVertex(index).addEdge(e1);
					e.getV1().addEdge(e1);
					e.getV2().addEdge(e2);
					getVertex(index).addEdge(e2);
					toAdd.add( e1 );
					toAdd.add( e2 );
					e.getV1().removeEdge(e);
					e.getV2().removeEdge(e);
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
					NucleusMeshEdge e = new NucleusMeshEdge(v1, v2, 1);
					toAdd.add( e );
					
				}
				
			}
			
			for(NucleusMeshEdge e : toAdd){
				addEdge(e); 
			}
			
		}
		
		/**
		 * Remove edges that are direct duplicates (completely overlapping)
		 */
		public void removeDuplicateEdges(){
			Set<NucleusMeshEdge> toRemove = new HashSet<NucleusMeshEdge>();
			log(Level.INFO, "Starting edges: "+edges.size());
			for(NucleusMeshEdge e1 : edges){
				
				for(NucleusMeshEdge e2 : edges){
					if(e1.equals(e2)){
						continue;
					}
					
					if(toRemove.contains(e1) || toRemove.contains(e2)){
						continue;
					}
					
					// TODO: This is enough to remove all edges to the CoM.Fix
					if( (  e1.getV1().overlaps(e2.getV1()) ||
					       e1.getV2().overlaps(e2.getV1())   ) &&
					    (  e1.getV1().overlaps(e2.getV2()) ||
					       e1.getV2().overlaps(e2.getV2()    ))){
						toRemove.add(e1);
					}
				
					
				}
				
			}
			log(Level.INFO, "Removing: "+toRemove.size());
			for(NucleusMeshEdge e : toRemove){
				edges.remove(e);
			}
			log(Level.INFO, "Remaining edges: "+edges.size());
		}
		
		
		/**
		 * Get the edges that may be removed. Exclude edges on the periphery
		 * @return
		 */
		private List<NucleusMeshEdge> getPotentialPrunableEdges(){
			List<NucleusMeshEdge> result = new ArrayList<NucleusMeshEdge>();
			
			for(NucleusMeshEdge e1 : edges){
				
				if( ! e1.isPeripheral()){
					result.add(e1);
				}
				
			}
			return result;
			
		}
		
		/**
		 * Remove edges from the list that span more than one eighth the bounding height of
		 * the mesh
		 * @return
		 */
		private List<NucleusMeshEdge> getLongestEdges(List<NucleusMeshEdge> input){
			log(Level.INFO, "Finding longest edges");
			List<NucleusMeshEdge> result = new ArrayList<NucleusMeshEdge>();
			double maxLength = 0;
			for(NucleusMeshEdge e1 : input){	
				maxLength = Math.max(e1.getLength(), maxLength);	
			}
			for(NucleusMeshEdge e1 : input){	
				if(e1.getLength()>maxLength / 3 ){
					result.add(e1);
				}
			}
			log(Level.INFO, "Found "+result.size()+" long edges");
			return result;
			
		}
		
		/**
		 * REmove any vertices that have been created outside the mesh boundary 
		 */
		public void removeExternalVertices(){
			
			
//			FloatPolygon f = createPolygon();
			
			List<NucleusMeshVertex> toRemove = new ArrayList<NucleusMeshVertex>();
			for(NucleusMeshVertex v : vertices){
				
				if( ! nucleus.containsPoint(v.getPosition())){
//				if( ! f.contains(  (float) v.getPosition().getX(), (float) v.getPosition().getY())){
					toRemove.add(v);
				}
			}
			log(Level.INFO, "Removing "+toRemove.size()+" external vertices");
			for(NucleusMeshVertex v : toRemove){
				vertices.remove(v);
				for(NucleusMeshEdge e : v.getEdges()){
					edges.remove(e);
				}
			}
			
		}
		
		/**
		 * Remove edges whose midpoint lies outside the nucleus
		 */
		public void removeExternalEdges(){
			List<NucleusMeshEdge> toRemove = new ArrayList<NucleusMeshEdge>();
			for(NucleusMeshEdge e : edges){
				
				if( ! nucleus.containsPoint(e.getMidpoint())){
					toRemove.add(e);
				}
			}
			log(Level.INFO, "Removing "+toRemove.size()+" external edges");
			for(NucleusMeshEdge e : toRemove){
				edges.remove(e);
				e.getV1().removeEdge(e);
				e.getV2().removeEdge(e);
			}
		}
		
		private NucleusMeshEdge getLongestEdge(List<NucleusMeshEdge> edges){
			NucleusMeshEdge longestEdge = null;
			for(NucleusMeshEdge e : edges){
				if(longestEdge == null || e.getLength()>longestEdge.getLength()){
					longestEdge=e;
				}
			}
			return longestEdge;
		}
		
		/**
		 * Fetch the vertex in the mesh closest to the given vertex, that
		 * does not have an edge to it already
		 * @param v
		 * @return
		 */
		private NucleusMeshVertex getClosestVertex(NucleusMeshVertex v){
			NucleusMeshVertex result = null;
			
			double minLength = Double.MAX_VALUE;
			
			for(NucleusMeshVertex v1 : vertices){
				if(v.hasEdgeTo(v1)){ // skip existing edges
					continue;
				}
				
				double length = v.getLengthTo(v1);
				if(length < minLength){
					minLength = length;
					result = v1;
				}
			}
			return result;
		}
		
		private int buildEdgeForEachVertex(){
			
			/*
			 * For each vertex, list the vertices by distance
			 * Take the closest. If it has an edge, continue.
			 * Else make an edge, unless that would cross an existing edge
			 */
			
			int edgesAdded = 0;
			
			for(NucleusMeshVertex v : vertices){
				
				NucleusMeshVertex v1 = getClosestVertex(v);
				NucleusMeshEdge test = new NucleusMeshEdge(v, v1, 1);
				
				boolean crosses = false;
				for(NucleusMeshEdge e : edges){
					if(e.crosses(test)){
						crosses = true;
						break;
					}
				}
				
				if(! crosses){
					addEdge(test);
					v1.addEdge(test);
					v.addEdge(test);
					edgesAdded++;
				}
			}
			return edgesAdded;
			
		}
		
		public void buildEdges(){
			log(Level.INFO, "Building edges");
			int edgesAdded = 1;
			while(edgesAdded>0){
				
				edgesAdded = buildEdgeForEachVertex();
				log(Level.INFO, "Added "+ edgesAdded+" edges");
			}
			
		}
		
		private List<NucleusMeshVertex> getPeripheralVertices(){
			List<NucleusMeshVertex> result = vertices.stream()
					.filter( e -> e.isPeripheral())
					.collect(Collectors.toList());
			return result;
		}
		
		private List<NucleusMeshEdge> getPeripheralEdges(){
			List<NucleusMeshEdge> result = edges.stream()
					.filter( e -> e.isPeripheral())
					.collect(Collectors.toList());
			return result;
		}
		
		private boolean testEdgeCrossesPeriphery(NucleusMeshEdge e){
			
			List<NucleusMeshEdge> preipheralEdges = getPeripheralEdges();
			
			for(NucleusMeshEdge p : preipheralEdges){
				if(e.crosses(p)){
					return true;
				}
			}
			return  false;
		}
		
		/**
		 * Remove all edges that intersect another edge in the mesh.
		 * Discards the longer edge of the two
		 */
		public void pruneOverlaps(){
			log(Level.INFO, "Pruning overlaps");
			
			List<NucleusMeshEdge> prunableEdges = getPotentialPrunableEdges();
			List<NucleusMeshEdge> longestEdges  = getLongestEdges(edges);
			
			for(NucleusMeshEdge e : longestEdges){
				edges.remove(e);
				prunableEdges.remove(e);
				e.getV1().removeEdge(e);
				e.getV2().removeEdge(e);
			}
			
			log(Level.INFO, "Prunable edges: "+prunableEdges.size());
			Set<NucleusMeshEdge> toRemove = new HashSet<NucleusMeshEdge>();
						
//			for(NucleusMeshEdge p : prunableEdges){
//				if(testEdgeCrossesPeriphery(p)){
//					toRemove.add(p);
//				}
//			}
//			boolean removable = true;
//			
//			while(removable){
//				NucleusMeshEdge longestEdge = getLongestEdge(prunableEdges);
//				for(Iterator<NucleusMeshEdge> it = prunableEdges.iterator(); it.hasNext(); ){
//					NucleusMeshEdge e = it.next();
//					if(e.crosses(longestEdge)){
//						prunableEdges.remove(longestEdge);
//						toRemove.add(longestEdge);
//						break;
//					}
//					removable = false;
//				}
//			}
//			
			for(NucleusMeshEdge e : toRemove){
				edges.remove(e);
				e.getV1().removeEdge(e);
				e.getV2().removeEdge(e);
			}
			
			
			
//			for(NucleusMeshEdge e1 : prunableEdges){
//				
//				for(NucleusMeshEdge e2 : prunableEdges){
//					if(e1.equals(e2)){
//						continue;
//					}
//					
//									
//					if(e1.crosses(e2)){
//						
//						// don't count edges already pruned
//						if(toRemove.contains(e1) || toRemove.contains(e2)){
//							continue;
//						}
//						
//						
//						// Remove the longer edge if neither are on the border
//						if( ! e1.isPeripheral()  && ! e2.isPeripheral()){
//
//							if(e1.isLongerThan(e2)){
//								toRemove.add(e1);
//							} else {
//								toRemove.add(e2);
//							}
//						} else {
//							// If one is peripheral, remove the other
//							if(  e1.isPeripheral()){
//								toRemove.add(e2);
//							} else if(e2.isPeripheral()) {
//								toRemove.add(e1);
//							}
//						}
//					}
//				}
//				
//			}
			
//			log(Level.INFO, "Pruning: "+toRemove.size());
//			for(NucleusMeshEdge e : toRemove){
//				edges.remove(e);
//			}
			log(Level.INFO, "Remaining edges: "+edges.size());
		}
		
//		private boolean isInsideMesh(NucleusMeshVertex v){
//			FloatPolygon f = createPolygon();
//			return f.contains(  (float) v.getPosition().getX(), (float) v.getPosition().getY());
//		}
		
		/**
		 * Make an offset polygon from 
		 * @return
		 */
//		private FloatPolygon createPolygon(){
//			
//			List<NucleusMeshVertex> peripheralVertices = vertices.stream()
//					.filter( e -> e.isPeripheral() )
//					.collect(Collectors.toList());
//						
//			float[] xpoints = new float[peripheralVertices.size()+1];
//			float[] ypoints = new float[peripheralVertices.size()+1];
//			
//			for(int i=0;i<peripheralVertices.size();i++){
//				NucleusMeshVertex p = peripheralVertices.get(i);
//				xpoints[i] = (float) p.getPosition().getX();
//				ypoints[i] = (float) p.getPosition().getY();
//			}
//
//			// Ensure the polygon is closed
//			xpoints[peripheralVertices.size()] = (float) peripheralVertices.get(0).getPosition().getX();
//			ypoints[peripheralVertices.size()] = (float) peripheralVertices.get(0).getPosition().getY();
//
//			return new FloatPolygon(xpoints, ypoints);
//		}
		
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
		
		Set<NucleusMeshEdge> edges = new HashSet<NucleusMeshEdge>(); // store the edges attached to the vertex
		
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
		
		public void addEdge(NucleusMeshEdge e){
			edges.add(e);
		}
		
		public void removeEdge(NucleusMeshEdge e){
			edges.remove(e);
		}
		
		public Set<NucleusMeshEdge> getEdges(){
			return edges;
		}
		
		public boolean hasEdgeTo(NucleusMeshVertex v){
			
			for(NucleusMeshEdge e : edges){
				if( e.v1.equals(v)  || e.v2.equals(v)){
					return true;
				}
			}
			return false;
		}
		
		public double getLengthTo(NucleusMeshVertex v){
			return position.getLengthTo(v.getPosition());
		}
		
		public boolean overlaps(NucleusMeshVertex v){
			return position.overlapsPerfectly(v.position);
		}
		
		
	}

}
