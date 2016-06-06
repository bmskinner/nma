package analysis.mesh;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import stats.Stats;
import components.generic.XYPoint;

public class NucleusMeshFace {
	
	private List<NucleusMeshEdge> edges = new ArrayList<NucleusMeshEdge>();
	private double value = 1;
	
	public NucleusMeshFace(final NucleusMeshEdge e1,  final NucleusMeshEdge e2,  final NucleusMeshEdge e3){
		
		// Check  that the edges make an enclosed space - there are only 3 unique vertices
		
		this.edges.add(e1);
		this.edges.add(e2);
		this.edges.add(e3);
		
		
		Set<NucleusMeshVertex> vertices = new HashSet<NucleusMeshVertex>();
		for(NucleusMeshEdge e : edges){
			vertices.add(e.getV1());
			vertices.add(e.getV2());
		}
		
		if(vertices.size()!=3){
			throw new IllegalArgumentException("Edges must enclose a triangle");
		}
		
		
	}
	
	/**
	 * Duplicate the face
	 * @param f
	 */
	public NucleusMeshFace(NucleusMeshFace f){
		for(NucleusMeshEdge e : f.edges){
			edges.add(new NucleusMeshEdge(e));
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
		if (edges == null) {
			if (other.edges != null)
				return false;
		} else {
			for(NucleusMeshEdge e : edges){
				if( ! other.edges.contains(e)){
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
