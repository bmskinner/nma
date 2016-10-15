package components.generic;

/**
 * Define a coordinate in 3D space
 * @author ben
 *
 */
public class Point3D extends XYPoint {

	private static final long serialVersionUID = 1L;
	
	protected double z;
	
	public Point3D(double x, double y, double z) {
		super(x, y);
		this.z = z;
	}
	
	public Point3D(XYPoint p, double z) {
		this(p.x, p.y, z);
	}
	
	public Point3D(Point3D p) {
		this(p.x, p.y, p.z);
	}

	public double getZ() {
		return z;
	}

	public void setZ(double z) {
		this.z = z;
	}
	
	/**
	 * Discard the z information and return the 2D point
	 * defined by the x and y coordinates
	 * @return
	 */
	public XYPoint to2D(){
		return new XYPoint(x, y);
	}

	/**
	 * Find the distance between this point and
	 * a given point
	 *
	 * @param a the point to measure to
	 * @return the distance between the points
	 */
	public double getLengthTo(final Point3D a){

		if(a==null){
			throw new IllegalArgumentException("Destination point is null");
		}

		// Get the h(xy) distance (hypotenuse)
		double xy = this.getLengthTo(a.to2D());

		// Now find the hypotenuse of the triangle with sides z and h(xy)

		double dz = Math.abs(z - a.z);

		double dz2 = dz * dz;
		double dh2 = xy * xy;
		double length = Math.sqrt(dz2+dh2);
		return length;
	}
	
	

}
