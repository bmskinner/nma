/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.analysis.image;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Quartile;

import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

/**
 * Calculate colour values within components. Used to identify e.g. eosinophils
 * versus basophils.
 * 
 * @author ben
 * @since 1.13.5
 *
 */
public class ColourMeasurometer implements Loggable {

    // public ColourMeasurometer(){}

    /**
     * Calculate the median greyscale intensity in the given component of a
     * cell.
     * 
     * @param component
     * @return
     * @throws UnloadableImageException
     */
    public int calculateAverageIntensity(CellularComponent component) throws UnloadableImageException {
        Area a = new Area(component.toOriginalShape());
        return calculateAverageIntensity(a, component.getImage(), Quartile.MEDIAN);
    }

    /**
     * Calculate the median greyscale intensity of the given processor within
     * the area of the given component
     * 
     * @param component
     * @param ip
     *            the image processor to search
     * @return
     */
    public static int calculateAverageIntensity(CellularComponent component, ImageProcessor ip) {
        Area a = new Area(component.toOriginalShape());
        return calculateAverageIntensity(a, ip, Quartile.MEDIAN);
    }
    
    /**
     * Get the average RGB values within the area.
     * 
     * @param a
     * @param ip
     *            a color immage processor
     * @return
     */
    private static int calculateAverageIntensity(Area a, ImageProcessor ip, double quartile) {

        if (!(ip instanceof ByteProcessor)) {
            throw new IllegalArgumentException("Must be an 8-bit greyscale image processor");
        }

        Rectangle r = a.getBounds();

        List<Integer> grey = new ArrayList<Integer>(100);

        for (int x = 0; x < ip.getWidth(); x++) {

            if (x < r.getMinX() || x > r.getMaxX()) {
                continue;
            }

            for (int y = 0; y < ip.getHeight(); y++) {
                if (y < r.getMinY() || y > r.getMaxY()) {
                    continue;
                }

                if (a.contains(x, y)) {

                    grey.add(ip.get(x, y));

                }

            }
        }

        DescriptiveStatistics ds = new DescriptiveStatistics();
        for(int d : grey){
        	ds.addValue(d);
        }
        return (int) ds.getPercentile(quartile);

//        Quartile q = Quartile.quartile(grey, quartile);
//        return q.intValue();
    }

    /**
     * Calculate the median greyscale intensity of the given processor within
     * the area of the given component
     * 
     * @param component
     * @param ip
     *            the image processor to search
     * @param quartile
     *            the quartile to return, from 0-100
     * @return
     */
    public static int calculateIntensity(CellularComponent component, ImageProcessor ip, double quartile) {

        if (component == null || ip == null) {
            throw new IllegalArgumentException("Component or image is null");
        }

        if (quartile < 0 || quartile > 100) {
            throw new IllegalArgumentException("Quartile must be between 0-100");
        }

        Area a = new Area(component.toOriginalShape());
        return calculateAverageIntensity(a, ip, quartile);
    }

    /**
     * Calculate the median RGB value in the given component of a cell. If the
     * component contains other components (e.g cytoplasm contains nucleus) the
     * area of the sub-component will be excluded from the calculation
     * 
     * @param c
     * @param component
     * @return
     * @throws UnloadableImageException
     *             if the image file can't be read
     */
    public static Color calculateAverageRGB(ICell c, String component) throws UnloadableImageException {

        // Get the component of interest, and remove any subcomponents
        // from its shape before calculating

        if (CellularComponent.CYTOPLASM.equals(component)) {
            return calculateAverageCytoplasmRGB(c);
        }

        if (CellularComponent.NUCLEUS.equals(component)) {
            // TODO
        }

        return Color.BLACK; // default if nothing calculated

    }

    /**
     * Calculate the median RGB value in the given component.
     * 
     * @param component
     * @return
     * @throws UnloadableImageException
     *             if the image file can't be read
     */
    public static Color calculateAverageRGB(CellularComponent component) throws UnloadableImageException {

        if (component == null) {
            throw new IllegalArgumentException("Component or image is null");
        }

        Area a = new Area(component.toOriginalShape());
        return calculateAverageRGB(a, component.getRGBImage());

    }




    /**
     * Calculate the pixel values for cytoplasm, excluding nuclei
     * 
     * @param c
     * @return
     * @throws UnloadableImageException
     *             if the image file can't be read
     */
    private static Color calculateAverageCytoplasmRGB(ICell c) throws UnloadableImageException {
        CellularComponent comp = c.getCytoplasm();
        Shape s = comp.toOriginalShape();

        Area a = new Area(s);

        for (Nucleus n : c.getNuclei()) {
            Area an = new Area(n.toOriginalShape());
            a.subtract(an);
        }

        return calculateAverageRGB(a, comp.getRGBImage());
    }

    /**
     * Get the average RGB values within the area.
     * 
     * @param a
     * @param ip
     *            a color immage processor
     * @return
     */
    private static Color calculateAverageRGB(Area a, ImageProcessor ip) {

        if (!(ip instanceof ColorProcessor)) {
            throw new IllegalArgumentException("Must be a colour image processor");
        }

        byte[] R = new byte[ip.getPixelCount()];
        byte[] G = new byte[ip.getPixelCount()];
        byte[] B = new byte[ip.getPixelCount()];

        ((ColorProcessor) ip).getRGB(R, G, B);

        Rectangle r = a.getBounds();

        List<Integer> red = new ArrayList<Integer>(100);
        List<Integer> green = new ArrayList<Integer>(100);
        List<Integer> blue = new ArrayList<Integer>(100);

        for (int x = 0; x < ip.getWidth(); x++) {

            if (x < r.getMinX() || x > r.getMaxX()) {
                continue;
            }

            for (int y = 0; y < ip.getHeight(); y++) {
                if (y < r.getMinY() || y > r.getMaxY()) {
                    continue;
                }

                if (a.contains(x, y)) {
                    int index = x * y;
                    red.add(R[index] & 0xFF);
                    green.add(G[index] & 0xFF);
                    blue.add(B[index] & 0xFF);

                    // log(x+" - "+y+": "+ip.getPixel(x, y));
                    // log("R: "+R[index]+"; G: "+G[index]+"; B: "+B[index]);
                    // int rgba = ip.getPixel(x, y);

                }

            }
        }
        int aR = calculateAverage(red);
        int aG = calculateAverage(green);
        int aB = calculateAverage(blue);

        return new Color(aR, aG, aB);

    }

    private static int calculateAverage(List<Integer> list) {
    	DescriptiveStatistics ds = new DescriptiveStatistics();
        for(int d : list){
        	ds.addValue(d);
        }
        return (int) ds.getPercentile(Quartile.MEDIAN);
//        Quartile q = new Quartile(list, Quartile.MEDIAN);
//        return q.intValue();
    }

}
