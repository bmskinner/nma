

package no.components;

/**
 *  This class contains border points around the periphery of a nucleus.
 *	Mostly the same as an XYPoint now, after creation of Profiles. It does
 * allow linkage of points, but this is not yet used
 *
 */
public class NucleusBorderPoint
	extends no.components.XYPoint {

	private static final long serialVersionUID = 1L;
	
	
	private NucleusBorderPoint prevPoint = null;
	private NucleusBorderPoint nextPoint = null;
	
	public NucleusBorderPoint( double x, double y){
		super(x, y);
	}

	public NucleusBorderPoint( NucleusBorderPoint p){
		super(p.getX(), p.getY());
	}
	
	public void setNextPoint(NucleusBorderPoint next){
		this.nextPoint = next;
	}
	
	public void setPrevPoint(NucleusBorderPoint prev){
		this.prevPoint = prev;
	}
	
	public NucleusBorderPoint nextPoint(){
		return  this.nextPoint;
	}
	
	public NucleusBorderPoint prevPoint(){
		return  this.prevPoint;
	}
	
	public boolean hasNextPoint(){
		if(this.nextPoint()!=null){
			return  true;
		} else {
			return  false;
		}
	}
	
	public boolean hasPrevPoint(){
		if(this.prevPoint()!=null){
			return  true;
		} else {
			return  false;
		}
	}

}