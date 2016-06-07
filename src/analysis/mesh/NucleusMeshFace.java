package analysis.mesh;

import java.util.HashSet;
import java.util.Set;

import stats.Stats;
import components.generic.XYPoint;

public class NucleusMeshFace {
	
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
				
		if( ! v1.hasEdgeTo(v2) || ! v1.hasEdgeTo(v3) || ! v2.hasEdgeTo(v3) ){
			throw new IllegalArgumentException("Vertices must have linked edges");
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
		if (vertices == null) {
			if (other.vertices != null)
				return false;
		} else {
			for(NucleusMeshVertex v : vertices){
				if( ! other.vertices.contains(v)){
					return false;
				}
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
		b.append("Face: Area: "+this.getArea()+" | Value: "+this.getValue()+"\n");
		for(NucleusMeshEdge e : edges){
			b.append(e.toString()+"\n");
		}
		return b.toString();
	}
}
