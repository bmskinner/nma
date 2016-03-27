package analysis.nucleus;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import analysis.BooleanAligner;
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
	 * Create a mesh comparing the given nuclei
	 * @param n1 the first nucleus. Used as the mesh template
	 * @param n2 the comparison nucleus.
	 * @param meshSize the distance between vertices
	 * @return a mesh with the ratios between edge lengths
	 * @throws Exception
	 */
	public NucleusMesh createComparisonMesh(Nucleus n1, Nucleus n2, int meshSize) throws Exception{
		
		NucleusMesh n1Mesh = buildMesh(n1, meshSize);
		
		/*
		 * Ensure input nuclei have a best fit alignment
		 * TODO: determine size of mask
		 */
		BooleanAligner aligner = new BooleanAligner(n1.getBooleanMask(200, 200));
		int[] alignment = aligner.align(n2.getBooleanMask(200, 200));
		
		n2 = n2.duplicate();
		n2.moveCentreOfMass( new XYPoint(alignment[BooleanAligner.Y], alignment[BooleanAligner.X]));
		
		/*
		 * Create the mesh
		 */
		
		NucleusMesh n2Mesh = buildMesh(n2, n1Mesh);
		
		n1Mesh.makePairwiseEdges();
		n2Mesh.makePairwiseEdges();
		
		
		NucleusMesh result = n1Mesh.compare(n2Mesh);
		return result;
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
		
//		mesh.addVertex(nucleus.getCentreOfMass(), false);
		
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
		
//		log(Level.FINEST, "Adding centre of mass");
//		mesh.addVertex(nucleus.getCentreOfMass(), false);
		
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
		mesh.makeCentreVertices();

		log(Level.FINEST, "Created mesh");
		return mesh;
	}
	
	private void createEdges(NucleusMesh mesh){

		
//		log(Level.FINEST, "Linking edges to CoM");
//		for(int i=1; i<mesh.getVertexCount(); i++){
//			NucleusMeshEdge e = new NucleusMeshEdge(mesh.getVertex(0), mesh.getVertex(i), 1);
//			mesh.addInternalEdge(e);
//			mesh.getVertex(0).addEdge(e);
//			mesh.getVertex(i).addEdge(e);
//		}
		
		log(Level.FINEST, "Linking border pairs");
		for(int i=0, j=1; j<mesh.getVertexCount(); i++, j++){
			NucleusMeshEdge e = new NucleusMeshEdge(mesh.getVertex(i), mesh.getVertex(j), 1);
			mesh.addPeripheralEdge( e );
			mesh.getVertex(i).addEdge(e);
			mesh.getVertex(j).addEdge(e);
		}
		
		// Link the final perimeter point to the tip
		NucleusMeshEdge e = new NucleusMeshEdge(mesh.getVertex(mesh.getVertexCount()-1), mesh.getVertex(0), 1);
		mesh.addPeripheralEdge(  e );
		mesh.getVertex(mesh.getVertexCount()-1).addEdge(e);
		mesh.getVertex(1).addEdge(e);
		
		log(Level.FINEST, "Created edges");
	}
	
	
	/**
	 * @author bms41
	 *
	 */
	/**
	 * @author bms41
	 *
	 */
	public class NucleusMesh {
		
		// Track the number of divisions for each segment to allow mapping between meshes
		private Map<Integer, Integer> segmentDivisions = new HashMap<Integer, Integer>();
		
		// Store the vertices in the mesh
		private List<NucleusMeshVertex> vertices = new ArrayList<NucleusMeshVertex>();
		
		// Not all vertices need to be linked - store edges for comparisons
		private List<NucleusMeshEdge> internalEdges     = new ArrayList<NucleusMeshEdge>();
		
		// Track the edges linking border vertices separately from internal edges
		private List<NucleusMeshEdge> peripheralEdges  = new ArrayList<NucleusMeshEdge>();
		
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
		private NucleusMesh(NucleusMesh mesh, List<NucleusMeshEdge> internal, List<NucleusMeshEdge> peripheral){
			this.segmentDivisions = mesh.segmentDivisions;
			this.vertices         = mesh.vertices;
			this.internalEdges    = internal;
			this.peripheralEdges  = peripheral;

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
		
		public void addInternalEdge(NucleusMeshEdge e){
			internalEdges.add(e);
		}
		
		public void addPeripheralEdge(NucleusMeshEdge e){
			peripheralEdges.add(e);
		}
		
		public int getSegmentCount(){
			return segmentDivisions.keySet().size();
		}
		
		public int getVertexCount(){
			return vertices.size();
		}
		
		public int getEdgeCount(){
			return internalEdges.size() + peripheralEdges.size();
		}
		
		public int getInternalEdgeCount(){
			return internalEdges.size();
		}
		
		public int getPeripheralEdgeCount(){
			return peripheralEdges.size();
		}
		
		public NucleusMeshVertex getVertex(int i){
			return  vertices.get(i);
		}
		
		public NucleusMeshEdge getInternalEdge(int i){
			return internalEdges.get(i);
		}
		
		public NucleusMeshEdge getPeripheralEdge(int i){
			return peripheralEdges.get(i);
		}
		
		public List<NucleusMeshEdge> getEdges(){
			List<NucleusMeshEdge> result = new ArrayList<NucleusMeshEdge>();
			result.addAll(internalEdges);
			result.addAll(peripheralEdges);
			return result;
		}

		
		public List<NucleusMeshEdge> getInternalEdges(){
			return internalEdges;
		}
		
		public List<NucleusMeshEdge> getPeripheralEdges(){
			return peripheralEdges;
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
			
			List<NucleusMeshEdge> internalEdges   = new ArrayList<NucleusMeshEdge>();
			List<NucleusMeshEdge> peripheralEdges = new ArrayList<NucleusMeshEdge>();
			
			for(int i=0; i<getInternalEdgeCount(); i++){
				internalEdges.add( getInternalEdge(i).compare(mesh.getInternalEdge(i)) );
			}
			
			for(int i=0; i<getPeripheralEdgeCount(); i++){
				peripheralEdges.add( getPeripheralEdge(i).compare(mesh.getPeripheralEdge(i)) );
			}
			
			return new NucleusMesh(this, internalEdges, peripheralEdges);
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
				this.addVertex (test.getMidpoint(), false);
			}
			
		}
		
		/**
		 * Create new vertices at the midpoints of all edges that have
		 * one or fewer peripheral vertices. Add new edges between them.
		 */
		public void subdivide(){
			List<NucleusMeshEdge> toAdd = new ArrayList<NucleusMeshEdge>();
			
			for(final Iterator<NucleusMeshEdge> it = internalEdges.iterator(); it.hasNext(); ){

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
				addInternalEdge(e); 
			}
		}
		
		public void makePairwiseEdges(){
			List<NucleusMeshEdge> toAdd = new ArrayList<NucleusMeshEdge>();
			
			
			
			for(NucleusMeshVertex v1 : vertices){
				
				for(NucleusMeshVertex v2 : vertices){
					if(v1.overlaps(v2)){
						continue;
					}
					NucleusMeshEdge e = new NucleusMeshEdge(v1, v2, 1);
					toAdd.add( e );
					
				}
				
			}
			
			for(NucleusMeshEdge e : toAdd){
				addInternalEdge(e); 
			}
			
			
			
		}
		
		/**
		 * Remove edges that are direct duplicates (completely overlapping endpoints)
		 */
		private void mergeDuplicateEdges(){
			Set<NucleusMeshEdge> toRemove = new HashSet<NucleusMeshEdge>();
			log(Level.FINEST, "Starting edges: "+internalEdges.size());
			
			for(NucleusMeshEdge e1 : internalEdges){
				
				if(toRemove.contains(e1)){
					continue; 
				}
				
				for(NucleusMeshEdge e2 : internalEdges){
					
					if(e1==e2){  // ignore self self
						continue;
					}
					
					if(toRemove.contains(e2)){
						continue; // ignore existing matches
					}
					
					if(e1.overlaps(e2)){
						// not the same object, but equivalent
						// ensure vertices track the edge being kept
												
						
						NucleusMeshVertex v1 = e1.getV1();
						NucleusMeshVertex v2 = e1.getV2();
						v1.addEdge(e2);
						v2.addEdge(e2);
						toRemove.add(e1);
						
					}

					
				}
				
			}
			log(Level.FINEST, "Merging edges: "+toRemove.size());
			removeEdges(toRemove);
			log(Level.FINEST, "Remaining edges: "+internalEdges.size());
			
			// Check that vertices don't have an edge twice following assignment
			mergeDuplicateEdgesByVertex();
		}
		
		
		/**
		 * Check if any vertices have a duplicated edge in inverted format.
		 * If so, remove the duplicates. 
		 */
		private void mergeDuplicateEdgesByVertex(){
			
			Set<NucleusMeshEdge> toRemove = new HashSet<NucleusMeshEdge>();
			
			for(NucleusMeshVertex v : vertices){
				
				Set<NucleusMeshEdge> list = v.getEdges();
				
				for(NucleusMeshEdge e1 : list){
					
					if(toRemove.contains(e1)){
						continue; 
					}
					
					for(NucleusMeshEdge e2 : list){
						
						if(e1==e2){
							continue;
						}
						
						if(toRemove.contains(e2)){
							continue; 
						}
						
						if(e1.overlaps(e2)){
							toRemove.add(e2);
						}
						
					}
					
				}
				
			}
			log(Level.FINEST, "Merging edges at vertices: "+toRemove.size());
			removeEdges(toRemove);
			log(Level.FINEST, "Remaining edges: "+internalEdges.size());
		}
		
				
		/**
		 * Remove edges from the list that span more than one third the bounding height of
		 * the mesh
		 * @return
		 */
		private List<NucleusMeshEdge> getLongestEdges(List<NucleusMeshEdge> input, double factor){
			
			double maxLength = input.parallelStream()
				.max( (e1, e2) -> Double.compare(e1.getLength(), e2.getLength()))
				.get()
				.getLength();
			
			
			final double max = maxLength / factor;
			
			List<NucleusMeshEdge> result = input.parallelStream()
				.filter(e -> e.getLength() > max)
				.collect(Collectors.toList());
			
			
			return result;
			
		}
		
		private NucleusMeshEdge getLongestEdge(List<NucleusMeshEdge> input){
			return input.parallelStream()
			.max( (e1, e2) -> Double.compare(e1.getLength(), e2.getLength()))
			.get();
		}
				
		private void removeEdges(Collection<NucleusMeshEdge> toRemove){
			for(NucleusMeshEdge e : toRemove){
				internalEdges.remove(e);
				peripheralEdges.remove(e);
				e.getV1().removeEdge(e);
				e.getV2().removeEdge(e);
			}
		}
		
		/**
		 * Remove internal edges whose midpoint lies outside the nucleus
		 */
		private void removeExternalEdges(){
			List<NucleusMeshEdge> toRemove = internalEdges.parallelStream()
					.filter( e -> ! nucleus.containsPoint(e.getMidpoint()))
					.collect(Collectors.toList());
			
			log(Level.FINEST, "Removing "+toRemove.size()+" external edges");
			removeEdges(toRemove);
		}
				
		
		private List<NucleusMeshVertex> getPeripheralVertices(){
			List<NucleusMeshVertex> result = vertices.parallelStream()
					.filter( e -> e.isPeripheral())
					.collect(Collectors.toList());
			return result;
		}
		
		private List<NucleusMeshVertex> getInternalVertices(){
			List<NucleusMeshVertex> result = vertices.parallelStream()
					.filter( e -> ! e.isPeripheral())
					.collect(Collectors.toList());
			return result;
		}
				
		private boolean testEdgeCrossesPeriphery(NucleusMeshEdge e){
			
//			List<NucleusMeshEdge> preipheralEdges = getPeripheralEdges();
			
			return getPeripheralEdges().parallelStream()
				.anyMatch( p -> e.crosses(p));
			
//			for(NucleusMeshEdge p : preipheralEdges){
//				if(e.crosses(p)){
//					return true;
//				}
//			}
//			return  false;
		}
		
		
		/**
		 * Get any edges that link central non-adjacent vertices
		 * @return
		 */
		private Set<NucleusMeshEdge> getNonAdjacentCentralVertexEdges(){
			Set<NucleusMeshEdge> result = new HashSet<NucleusMeshEdge>();
			
			List<NucleusMeshVertex> list = getInternalVertices();
			
			for(int i=0; i<list.size(); i++){
				
				int prev = i==0 ? list.size()-1 : i-1;
				int next = i==list.size()-1 ? 0 : i+1;
				
				NucleusMeshVertex p1 = list.get(prev);
				NucleusMeshVertex v1 = list.get(i);
				NucleusMeshVertex p2 = list.get(next);
				
				for(NucleusMeshEdge e : v1.getEdges()){
					
					// The edge is entirely within the central vertices
					if( list.contains(e.getV1()) && list.contains(e.getV2())){

						// the edge does not list to an adjacent vertex 
						if( !e.containsVertex(p1) && !e.containsVertex(p2) ){
							result.add(e);
						}
					}
				}
				
			}
			return result;
		}
		
		private List<NucleusMeshEdge> getOverlappingInternalEdges(NucleusMeshEdge e1){
				
			List<NucleusMeshEdge> result = internalEdges.parallelStream()
				.filter( e -> e1.crosses(e))
				.collect(Collectors.toList());
			
			return result;
		}
		/**
		 * Remove all edges that intersect another edge in the mesh.
		 * Discards the longer edge of the two
		 */
		public void pruneOverlaps(){

			this.removeExternalEdges();
			this.mergeDuplicateEdges();
			
			Set<NucleusMeshEdge> toRemove = new HashSet<NucleusMeshEdge>();

			
			log(Level.FINEST, "Pruning overlaps");
			log(Level.FINEST, "Starting with "+vertices.size()+" vertices");
			log(Level.FINEST, "Starting with "+internalEdges.size()+" edges");
			
			List<NucleusMeshEdge> longestEdges  = getLongestEdges(internalEdges, 3);
			log(Level.FINEST, "Found "+longestEdges.size()+" long edges using factor "+3);

			
			/*
			 * Add the longest edges
			 */
			toRemove.addAll(  
					
					internalEdges.parallelStream()
					.filter( e -> longestEdges.contains(e))
					.collect(Collectors.toList())
					
				);
			
			
			log(Level.FINEST, "Removing "+toRemove.size()+" edges combining length");
			
			/*
			 * Add the edges that cross the peripheral edges
			 */
			toRemove.addAll(  
					
					internalEdges.parallelStream()
						.filter( e -> testEdgeCrossesPeriphery(e))
						.collect(Collectors.toList())
					
				);
			log(Level.FINEST, "Removing "+toRemove.size()+" edges combining periphery crossing");
			
			
			/*
			 * Add connections between peripheral vertices that are not peripheral edges
			 */
			
			toRemove.addAll(  
					
					internalEdges.parallelStream()
						.filter( e -> e.getV1().isPeripheral() && e.getV2().isPeripheral())
						.collect(Collectors.toList())
					
				);
			log(Level.FINEST, "Removing "+toRemove.size()+" edges combining peripheral vertices");
			
			/*
			 * Add connections between internal vertices (the centre line) that are not adjacent
			 */
			
			log(Level.FINEST, "Removing "+toRemove.size()+" edges combining peripheral vertices");
			
			toRemove.addAll( getNonAdjacentCentralVertexEdges());
			
		
			/*
			 * Clear out the first set of removable edges
			 */
			removeEdges(toRemove);
			log(Level.FINEST, "Remaining internal edges: "+internalEdges.size());
			/*
			 * Remove the longer of internal edges that cross another internal edge
			 */
			
			log(Level.FINEST, "Removing crossing internal edges");
			boolean result = true;
			int totalRemoved = 0;
			int counter = 0;
			while(result && counter<50){
				
				/*
				 * Go over each vertex, trying to remove an edge
				 */
//				logIJ(" ");
//				logIJ("Loop iteration over "+vertices.size()+" vertices");
				int removed = removeLoop();
//				logIJ("Removed "+removed+" edges");
				if(removed==0){
					result=false;
				}
				totalRemoved+=removed;
				counter++;
			}
			log(Level.FINEST, "Removed "+totalRemoved+" internal edges");
			

			log(Level.FINEST, "Remaining internal edges: "+internalEdges.size());
		}
		
		private int removeLoop(){
			
			int result = 0;
			
			for(NucleusMeshVertex v : vertices){
//				logIJ(" ");
//				logIJ("Testing vertex "+v.toString()+" with "+v.getEdges().size()+" connected edges");
				if(removeEdgeLoop(v)){
					result++;
				} 
			}
			return result;
		}
		
		private boolean removeEdgeLoop(NucleusMeshVertex v){
			Set<NucleusMeshEdge> edges = v.getEdges();

			for(NucleusMeshEdge e1 : edges){
//				logIJ("   Edge "+e1.toString());
				if(removeLongestOverlappingEdge(e1)){
//					logIJ("Able to remove edge overlapping "+e1.toString());
					return true;
				}
			}
			return false;
		}
		
		private boolean removeLongestOverlappingEdge(NucleusMeshEdge e1){
			Set<NucleusMeshEdge> toRemove = getRemovableEdges(e1);
			if(toRemove.isEmpty()){
				return false;
			} else {
				removeEdges(toRemove);
				return true;
			}
		}
		
		private Set<NucleusMeshEdge> getRemovableEdges(NucleusMeshEdge e1){

				
//			logIJ("Testing edge "+e1.toString()+": "+e1.getLength());
				Set<NucleusMeshEdge> toRemove = new HashSet<NucleusMeshEdge>();
				
				List<NucleusMeshEdge> overlaps = getOverlappingInternalEdges(e1);
				if( ! overlaps.isEmpty()){
					overlaps.add(e1); // include self for possible deletion
					
//					for(NucleusMeshEdge e : overlaps){
//						logIJ(e.toString()+": "+e.getLength());
//					}

					NucleusMeshEdge longest = getLongestEdge(overlaps);
					toRemove.add(longest);
//					logIJ("Removing edge "+longest.toString()+": "+longest.getLength());
				}
				return toRemove;
			
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
		 * Test if both vertices lie on the periphery of the mesh, and are adjacent
		 * in the vertex list. TODO: end of list wrapping
		 * @return
		 */
		public boolean isPeripheral(){
			
			if(v1.isPeripheral() && v2.isPeripheral()){
				if(  Math.abs(v1.getNumber() - v2.getNumber())==1 ){
					return true;
				}
			}
			return false;
		}
		
		public boolean isLongerThan(NucleusMeshEdge e){
			return getLength() > e.getLength();
		}
		
		/**
		 * Test if the edges share both endpoints
		 * @param e
		 * @return
		 */
		public boolean overlaps(NucleusMeshEdge e){
			return this.containsVertex(e.v1) && this.containsVertex(e.v2);			
		}
		
		public boolean crosses(NucleusMeshEdge e){
			
			Line2D line1 = new Line2D.Double(v1.getPosition().asPoint(), v2.getPosition().asPoint());
			Line2D line2 = new Line2D.Double(e.v1.getPosition().asPoint(), e.v2.getPosition().asPoint());

			if(line1.intersectsLine(line2)){
				

				if(sharesEndpoint(e)){
					return false;
				} else {
					return true;
				}
			} 
			return false;
		}
		
		/**
		 * Check if any of the endpoints of the edges are shared
		 * @param e
		 * @return
		 */
		public boolean sharesEndpoint(NucleusMeshEdge e){
			return this.containsVertex(e.v1) || this.containsVertex(e.v2);			
		}
		
		public boolean containsVertex(NucleusMeshVertex v){
			return v1.overlaps(v)  || v2.overlaps(v);
		}
		
		/**
		 * Compare the length of this edge to the given edge, and return
		 * a new edge with the ratio
		 * @param e
		 * @return
		 */
		public NucleusMeshEdge compare(NucleusMeshEdge e){
			
			double thisDistance = getLength();
			double thatDistance = e.getLength();
			
			double ratio = thisDistance / thatDistance ;
			return new NucleusMeshEdge(v1, v2, ratio);
			
		}
						
		public String toString(){
			return v1.toString()+" : "+v2.toString()+" : "+getLength();
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
		
		public String toString(){
			return position.toString();
		}
		
	}

}
