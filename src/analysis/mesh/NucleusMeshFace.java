package analysis.mesh;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NucleusMeshFace {
	
	private List<NucleusMeshEdge> edges = new ArrayList<NucleusMeshEdge>();
	private double value = 0;
	
	public NucleusMeshFace(final List<NucleusMeshEdge> edges){
		if(edges.size()!=3){
			throw new IllegalArgumentException("Must have three edges in a face");
		}
		
		// Check  that the edges make an enclosed space - there are only 3 unique vertices
		Set<NucleusMeshVertex> vertices = new HashSet<NucleusMeshVertex>();
		for(NucleusMeshEdge e : edges){
			vertices.add(e.getV1());
			vertices.add(e.getV2());
		}
		
		if(vertices.size()!=3){
			throw new IllegalArgumentException("Edges must enclose a triangle");
		}
		
		this.edges = edges;
	}
		
	/**
	 * Get the value stored with this face
	 * @return
	 */
	public double getValue() {
		return value;
	}


	/**
	 * Store a value in this face
	 * @param value
	 */
	public void setValue(double value) {
		this.value = value;
	}



	public List<NucleusMeshEdge> getEdges(){
		return edges;
	}
	
	public Set<NucleusMeshVertex> getVertices(){
		Set<NucleusMeshVertex> vertices = new HashSet<NucleusMeshVertex>();
		for(NucleusMeshEdge e : edges){
			vertices.add(e.getV1());
			vertices.add(e.getV2());
		}
		return vertices;
	}
	
	public boolean hasEdge(NucleusMeshEdge e){
		return edges.contains(e);
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
}
