package com.bmskinner.nuclear_morphology.analysis;

import java.util.Iterator;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.stats.Area;

/**
 * Calculate basic stats for cellular components. Used to fill in missing values
 * when objects were not created using a detector
 * 
 * @author ben
 * @since 1.13.5
 *
 */
public class ComponentMeasurer {

    /**
     * Calculate the perimeter of the shape based on the border list
     * 
     * @param c
     *            the component to measure
     * @return
     */
    public static double calculatePerimeter(CellularComponent c) {
        double perimeter = 0;
        Iterator<IBorderPoint> it = c.getBorderList().iterator();
        while (it.hasNext()) {
            IBorderPoint point = it.next();
            perimeter += point.getLengthTo(point.prevPoint());
        }
        return perimeter;
    }

    /**
     * Calculate the area of the object
     * 
     * @param c
     *            the component to measure
     * @return
     */
    public static double calculateArea(CellularComponent c) {
        return new Area(c.toShape()).doubleValue();
    }

}
