package com.bmskinner.nuclear_morphology.components.generic;

/**
 * Define a coordinate in 3D space
 * 
 * @author ben
 *
 */
public class Point3D extends FloatPoint {

    private static final long serialVersionUID = 1L;

    protected float z;

    public Point3D(float x, float y, float z) {
        super(x, y);
        this.z = z;
    }

    public Point3D(FloatPoint p, float z) {
        this(p.x, p.y, z);
    }

    public Point3D(Point3D p) {
        this(p.x, p.y, p.z);
    }

    public double getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
    }

    /**
     * Discard the z information and return the 2D point defined by the x and y
     * coordinates
     * 
     * @return
     */
    public IPoint to2D() {
        return IPoint.makeNew(x, y);
    }

    /**
     * Find the distance between this point and a given point
     *
     * @param a
     *            the point to measure to
     * @return the distance between the points
     */
    public double getLengthTo(final Point3D a) {

        if (a == null) {
            throw new IllegalArgumentException("Destination point is null");
        }

        // Get the h(xy) distance (hypotenuse)
        double xy = this.getLengthTo(a.to2D());

        // Now find the hypotenuse of the triangle with sides z and h(xy)

        double dz = Math.abs(z - a.z);

        double dz2 = dz * dz;
        double dh2 = xy * xy;
        double length = Math.sqrt(dz2 + dh2);
        return length;
    }

}
