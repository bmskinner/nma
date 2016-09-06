package analysis.mesh;

import java.awt.geom.Path2D;
import java.util.HashSet;
import java.util.Set;

import logging.Loggable;
import stats.Stats;
import components.generic.Equation;
import components.generic.XYPoint;

public class NucleusMeshFace implements Loggable {
	
	final private Set<NucleusMeshEdge> edges      = new  HashSet<NucleusMeshEdge>();
	
	final private Set<NucleusMeshVertex> vertices = new HashSet<NucleusMeshVertex>();
	
	private double value = 1;
	
	public NucleusMeshFace(final NucleusMeshEdge e1,  final NucleusMeshEdge e2,  final NucleusMeshEdge e3){
		
		// Check  that the edges make an enclosed space - there are only 3 unique vertices
		this.edges.add(e1);
		this.edges.add(e2);
		this.edges.add(e3);
		
		for(NucleusMeshEdge e : edges){
			vertices.add(e.getV1());
			vertices.add(e.getV2());
		}
		
		if(vertices.size()!=3){
			throw new IllegalArgumentException("Edges must enclose a triangle");
		}
		
		
	}
	
	public NucleusMeshFace(final NucleusMeshVertex v1,  final NucleusMeshVertex v2,  final NucleusMeshVertex v3){
				
		if( ! v1.hasEdgeTo(v2) ){
			throw new IllegalArgumentException("Vertices v1 and v2 are not linked in face constructor: "+v1.toString()+" and "+v2.toString());
		}
		
		if( ! v1.hasEdgeTo(v3) ){
			throw new IllegalArgumentException("Vertices v1 and v3 are not linked in face constructor: "+v1.toString()+" and "+v3.toString());
		}
		
		if( ! v2.hasEdgeTo(v3) ){
			throw new IllegalArgumentException("Vertices v2 and v3 are not linked in face constructor: "+v2.toString()+" and "+v3.toString());
		}
				
		vertices.add(v1);
		vertices.add(v2);
		vertices.add(v3);
		
		edges.add(v1.getEdgeTo(v2));
		edges.add(v2.getEdgeTo(v3));	
		edges.add(v3.getEdgeTo(v1));		
		
	}
	
	
	/**
	 * Duplicate the face
	 * @param f
	 */
	public NucleusMeshFace(NucleusMeshFace f){
		for(NucleusMeshEdge e : f.edges){
			edges.add(new NucleusMeshEdge(e));
			
			vertices.add( e.getV1() );
			vertices.add( e.getV2() );
		}
		this.value = f.value;
	}
			
	/**
	 * Get the value stored with this face
	 * @return
	 */
	public double getValue() {
		return value;
	}
	
	public double getLog2Ratio(){
		return Stats.calculateLog2Ratio(value);
	}


	/**
	 * Store a value in this face
	 * @param value
	 */
	public void setValue(double value) {
		this.value = value;
	}



	public Set<NucleusMeshEdge> getEdges(){
		return edges;
	}
	
	public Set<NucleusMeshVertex> getVertices(){
		return vertices;
	}
	
	public boolean contains(NucleusMeshEdge e){
		return edges.contains(e);
	}
	
	public boolean contains(NucleusMeshVertex v){
		return vertices.contains(v);
	}
	
	/**
	 * Get the area of the face
	 * @return
	 */
	public double getArea(){
		// Use Heron's formula:
//		s = (a+b+c) /2
//		a = sqrt( s(s-a)(s-b)(s-c)  )
		
		double s = 0;
		for(NucleusMeshEdge e : edges){
			s += e.getLength();
		}
		s /= 2;
		
		double a2 = s;
		
		for(NucleusMeshEdge e : edges){
			double t = s - e.getLength();
			a2 *= t;
		}
		double a = Math.sqrt(a2);
		return a;
		
	}
	
	/**
	 * Get the vertex opposite the given edge. This is the vertex
	 * that does not contain the edge.
	 * @param e
	 * @return
	 */
	private NucleusMeshVertex getOppositeVertex(NucleusMeshEdge e){

		for(NucleusMeshVertex v : vertices){
			if( ! v.getEdges().contains(e)){
				return v;
			}
		}
		
		return null;
	}
	
	/**
	 * Get the edge opposite the given vertex. This is the edge
	 * that does not contain the vertex.
	 * @param e
	 * @return
	 */
	private NucleusMeshEdge getOppositeEdge(NucleusMeshVertex v){
		
		for(NucleusMeshEdge e : edges){
			if( ! e.containsVertex(v)){
				return e;
			}
		}
		return null;
		
	}
	

	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		
		for(NucleusMeshEdge e : edges){
			result = prime * result +  e.hashCode();
		}
		
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		
		NucleusMeshFace other = (NucleusMeshFace) obj;

		// vertex tests for name and peripheral only
		for(NucleusMeshVertex v : vertices){
			if( ! other.vertices.contains(v)){
				return false;
			}
		}
		
		
		return true;
	}

	public String getName(){
		StringBuilder b = new StringBuilder();

		for(NucleusMeshVertex v : getVertices()){
			b.append(v.getName()+" ");
		}
		return b.toString();
	}
	
	public XYPoint getMidpoint(){
		
		double avgX = 0;
		double avgY = 0;
		
		for(NucleusMeshVertex v : getVertices()){
			avgX += v.getPosition().getX();
			avgY += v.getPosition().getY();
		}
		avgX /= 3;
		avgY /= 3;
		
		return new XYPoint(avgX, avgY);
	}
	
	
	public String toString(){
		StringBuilder b = new StringBuilder();
		b.append("Face: "+this.countVertices(true)+" peripheral vertices | Area: "+this.getArea()+" | Value: "+this.getValue()+"\n");

		for(NucleusMeshVertex v : vertices){
			b.append(v.toString()+"\n");
		}
		return b.toString();
	}
	
	/**
	 * Test if the given point is within the face
	 * @param p
	 * @return
	 */
	public boolean contains(XYPoint p){
		
		return this.toPath().contains(p.asPoint());

	}
	
	/**
	 * Generate a closed path for the face
	 * @return
	 */
	public Path2D toPath(){
		Path2D path = new Path2D.Double();

		int i=0;
		for(NucleusMeshVertex v : vertices){
			
			
			if( i++ == 0){
				path.moveTo(v.getPosition().getX(), v.getPosition().getY());
			} else {
				path.lineTo(v.getPosition().getX(), v.getPosition().getY());
			}
			
			
		}
		path.closePath();
		return path;
	}
	
	
	/**
	 * Count the number of vertices in the face that are peripheral
	 * or internal
	 * @param peripheral
	 * @return
	 */
	private int countVertices(boolean peripheral){
		int count = 0;
		for(NucleusMeshVertex v : vertices){
			 if(  v.isPeripheral() ){
				 count++;
			 }
		}
		
		if(peripheral){
			return count;
		} else {
			return vertices.size()-count;
		}
	}
	
	/**
	 * Get the internal vertex of the face if it has two peripheral
	 * vertices, or the peripheral vertex if there are two internal vertices
	 * @return
	 */
	private NucleusMeshVertex getLowerInternalVertex(){
		
		int index = Integer.MAX_VALUE;
		NucleusMeshVertex result = null;
		
		for(NucleusMeshVertex v : vertices){
			 if( ! v.isPeripheral() ){
				 
				 int number = v.getNumber();
				 if( number<index ){
					 index  = number;
					 result = v;
				 }
			 }
		}
		return result;	
	}
	
	private NucleusMeshVertex getHigherInternalVertex(){
		
		int index = -1;
		NucleusMeshVertex result = null;
		
		for(NucleusMeshVertex v : vertices){
			 if( ! v.isPeripheral() ){
				 int number = v.getNumber();
				 if( number>index ){
					 index  = number;
					 result = v;
				 }
			 }
		}
		return result;	
	}
	
	/**
	 * Get the lower peripheral vertex of the face
	 * @return
	 */
	private NucleusMeshVertex getLowerPeripheralVertex(){
		
		int index = Integer.MAX_VALUE;
		NucleusMeshVertex result = null;
		
		for(NucleusMeshVertex v : vertices){
			 if( v.isPeripheral() ){
				 
				 int number = v.getNumber();
				 if( number<index ){
					 index  = number;
					 result = v;
				 }
			 }
		}
		return result;	
	}
	
	/**
	 * Get the upper peripheral vertex of the face
	 * @return
	 */
	private NucleusMeshVertex getHigherPeripheralVertex(){
		
		int index = -1;
		NucleusMeshVertex result = null;
		
		for(NucleusMeshVertex v : vertices){
			 if( v.isPeripheral() ){
				 int number = v.getNumber();
				 if( number>index ){
					 index  = number;
					 result = v;
				 }
			 }
		}
		return result;	
	}
	
	
	/**
	 * Get the proportional distance of the given point along the edge
	 * opposite the given vertex
	 * @param v the vertex opposite the edge
	 * @param p the point within the face
	 * @return
	 */
	private double getEdgeProportion(NucleusMeshVertex v, XYPoint p){
		
		// Line from vertex to point
		Equation eq1 = new Equation(v.getPosition(), p);
		
		// Edge opposite the vertex
		NucleusMeshEdge oppEdge = this.getOppositeEdge(v);
		oppEdge = correctEdgeOrientation(oppEdge);
		
		NucleusMeshVertex o1 = oppEdge.getV1(); 
		NucleusMeshVertex o2 = oppEdge.getV2(); 
		
		// Line marking opposite edge
		Equation eq2 = new Equation(o1.getPosition(), o2.getPosition());
		
		// Position on edge intercepting line from vertex through point p
		XYPoint intercept = eq2.getIntercept(eq1);
		
		// Proportion through edge
		double proportion = oppEdge.getPositionProportion(intercept);
		return proportion;
	}
	
	/**
	 * Check that v1 is the internal vertex, or the lower perpheral vertex.
	 * If not, return new new edge with reversed orientation
	 * @param e
	 * @return
	 */
	private NucleusMeshEdge correctEdgeOrientation(NucleusMeshEdge e){
		// Identify and correct the orientation of the edges
		boolean usePeripheral = countVertices(true)==2;
		
		NucleusMeshVertex p1 = usePeripheral ? getLowerPeripheralVertex() : getLowerInternalVertex();
		NucleusMeshVertex p2 = usePeripheral ? getHigherPeripheralVertex() : getHigherInternalVertex();
		NucleusMeshVertex i1 = usePeripheral ? getLowerInternalVertex() : getLowerPeripheralVertex();
		
		NucleusMeshVertex o1 = e.getV1(); 
		NucleusMeshVertex o2 = e.getV2(); 
					
		if(o1.equals(p1) && o2.equals(i1)){
			return e.reverse();
		}
		
		if(o1.equals(p2) && o2.equals(i1)){
			return e.reverse();
		}
		
		if(o1.equals(p2) && o2.equals(p1)){
			return e.reverse();
		}

		return e;
	}
	
	/**
	 * Given a point within the face, get the face coordinate
	 * @param p
	 * @return
	 */
	public NucleusMeshFaceCoordinate getFaceCoordinate(XYPoint p){
		
		if( ! contains(p)  ){
			throw new IllegalArgumentException("Point is not within face: "+p.toString());
		}
		
		boolean usePeripheral = countVertices(true)==2;
		
		NucleusMeshVertex p1 = usePeripheral ? getLowerPeripheralVertex() : getLowerInternalVertex();
		NucleusMeshVertex p2 = usePeripheral ? getHigherPeripheralVertex() : getHigherInternalVertex();
		NucleusMeshVertex i1 = usePeripheral ? getLowerInternalVertex() : getLowerPeripheralVertex();
		
//		NucleusMeshVertex p1 = getLowerPeripheralVertex();
//		NucleusMeshVertex p2 = getHigherPeripheralVertex();
//		NucleusMeshVertex i1 = getInternalVertex();
		
		double p1p = getEdgeProportion(p1, p);
		double p2p = getEdgeProportion(p2, p);
		double i1p = getEdgeProportion(i1, p);
		
		return new NucleusMeshFaceCoordinate(p1p, p2p, i1p);

	}
	
	/**
	 * This tracks coordinates within the face based on intersecting lines
	 * between vertices and proportional distances along opposite edges
	 * along edges. Immutable.
	 * @author bms41
	 *
	 */
	public class NucleusMeshFaceCoordinate{
				
		// edge opposite peripheral vertex with lower number
		// Value runs from 0 at internal vertex to 1 at peripheral vertex
		final private double p1; 
		
		// edge opposite peripheral vertex with higher number
		// Value runs from 0 at internal vertex to 1 at peripheral vertex
		final private double p2; 
		
		
		// edge opposite internal vertex
		// Value runs from 0 at peripheral vertex with lower number to 1 at peripheral vertex with higher number
		final private double i1; 
		
		public NucleusMeshFaceCoordinate(double p1, double p2, double i1){
			
			if(p1>1 || p2>1 || i1>1){
				throw new IllegalArgumentException("Coordinates must be less than 1");
			}
			
			if(p1<0 || p2<0 || i1<0){
				throw new IllegalArgumentException("Coordinates must be greater than 0");
			}
			
			this.p1 = p1;
			this.p2 = p2;
			this.i1 = i1;
		}
		
		/**
		 * Convert the face coordinate into the cartesian coordinates in the given face 
		 * @param face
		 * @return
		 */
		public XYPoint getPixelCoordinate(NucleusMeshFace face){
			
			// Identify the vertices
			boolean usePeripheral = face.countVertices(true)==2;
						
			NucleusMeshVertex p1 = usePeripheral ? face.getLowerPeripheralVertex() : face.getLowerInternalVertex();
			NucleusMeshVertex p2 = usePeripheral ? face.getHigherPeripheralVertex() : face.getHigherInternalVertex();
			NucleusMeshVertex i1 = usePeripheral ? face.getLowerInternalVertex() : face.getLowerPeripheralVertex();
			
//			finest("P1: "+p1.toString());
//			finest("P2: "+p2.toString());
//			finest("I1: "+i1.toString());;
									
			// Identify the edges
			NucleusMeshEdge i1_p1 = i1.getEdgeTo(p1);
			NucleusMeshEdge i1_p2 = i1.getEdgeTo(p2);
			NucleusMeshEdge p1_p2 = p1.getEdgeTo(p2);
			
			// Identify and correct the orientation of the edges
			i1_p1 = correctEdgeOrientation(i1_p1);
			i1_p2 = correctEdgeOrientation(i1_p2);
			p1_p2 = correctEdgeOrientation(p1_p2);
						
//			finer("Corrected edges");
//			finest(i1_p1.toString());
//			finest(i1_p2.toString());
//			finest(p1_p2.toString());
			
			// Draw lines
			XYPoint i1_p1_prop = i1_p1.getProportionalPosition(this.p2);
//			finest("Point along I1-P1: "+i1_p1_prop.toString());
			
			Equation eq1 = new Equation(p2.getPosition(), i1_p1_prop);
			
			XYPoint i1_p2_prop = i1_p2.getProportionalPosition(this.p1);
//			finest("Point along I1-P2: "+i1_p2_prop.toString());
			Equation eq2 = new Equation(p1.getPosition(), i1_p2_prop);
						
			// Find intersection
			XYPoint position = eq1.getIntercept(eq2);
			
			// Return at point
//			finest("\tFound intercept: "+position.toString());
			return position;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			long temp;
			temp = Double.doubleToLongBits(i1);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(p1);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(p2);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			NucleusMeshFaceCoordinate other = (NucleusMeshFaceCoordinate) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (Double.doubleToLongBits(i1) != Double
					.doubleToLongBits(other.i1))
				return false;
			if (Double.doubleToLongBits(p1) != Double
					.doubleToLongBits(other.p1))
				return false;
			if (Double.doubleToLongBits(p2) != Double
					.doubleToLongBits(other.p2))
				return false;
			return true;
		}

		private NucleusMeshFace getOuterType() {
			return NucleusMeshFace.this;
		}
		
		public String toString(){
			return i1 + " : " + p1 + " : " + p2;
		}
		
	}
}
