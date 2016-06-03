package analysis.mesh;

import java.util.HashSet;
import java.util.Set;

import components.generic.XYPoint;

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
			if( e.getV1().equals(v)  || e.getV2().equals(v)){
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
