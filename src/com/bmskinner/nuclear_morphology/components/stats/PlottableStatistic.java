/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.components.stats;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.analysis.image.GLCM.GLCMValue;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;

/**
 * This interface describes statistical measures
 * that can be plotted in charts.
 * 
 * @author ben
 *
 */
public interface PlottableStatistic extends Serializable {
	
	/**
	 * The names of the measured statistics
	 * @author bms41
	 *
	 */
	static class Names {

		static final String AREA             = "Area";
		static final String PERIMETER        = "Perimeter";
		static final String MAX_FERET        = "Max feret";
		static final String MIN_DIAMETER     = "Min diameter";
		static final String ELLIPTICITY      = "Ellipticity";
		static final String ASPECT           = "Aspect ratio";
		static final String CIRCULARITY      = "Circularity";
		static final String ELONGATION       = "Elongation";
		static final String REGULARITY       = "Regularity";
		static final String VARIABILITY      = "Difference from median";
		static final String BOUNDING_HEIGHT  = "Bounding height";
		static final String BOUNDING_WIDTH   = "Bounding width";
		static final String OP_RP_ANGLE      = "Angle between reference points";
		static final String HOOK_LENGTH      = "Length of hook";
		static final String BODY_WIDTH       = "Width of body";
		static final String LOBE_COUNT       = "Number of lobes";
		static final String PATH_LENGTH      = "Path length";
		static final String CELL_NUCLEUS_COUNT   = "Nuclei per cell";
		static final String CELL_NUCLEAR_AREA    = "Nuclear area";
		static final String CELL_NUCLEAR_RATIO   = "Nucleus : Cytoplasm area ratio";
		static final String NUCLEUS_SIGNAL_COUNT = "Signals per nucleus";
		static final String ANGLE                = "Angle";
		static final String DISTANCE_FROM_COM        = "Distance from CoM";
		static final String FRACT_DISTANCE_FROM_COM  = "Fractional distance from CoM";
		static final String RADIUS                   = "Radius";
		static final String LENGTH                   = "Length";
		static final String DISPLACEMENT             = "Displacement";
		static final String TSNE_1             = "t-SNE 1";
		static final String TSNE_2             = "t-SNE 2";
		static final String PCA_N             = "Number of PCs";
		static final String PCA_1             = "PC1";
		static final String PCA_2             = "PC2";
		
	}

    // General component statistics
    static final PlottableStatistic AREA            = new GenericStatistic(Names.AREA,            StatisticDimension.AREA);
    static final PlottableStatistic PERIMETER       = new GenericStatistic(Names.PERIMETER,       StatisticDimension.LENGTH);
    static final PlottableStatistic MAX_FERET       = new GenericStatistic(Names.MAX_FERET,       StatisticDimension.LENGTH);
    static final PlottableStatistic MIN_DIAMETER    = new GenericStatistic(Names.MIN_DIAMETER,    StatisticDimension.LENGTH);
    static final PlottableStatistic ELLIPTICITY     = new GenericStatistic(Names.ELLIPTICITY,     StatisticDimension.DIMENSIONLESS);
    static final PlottableStatistic ASPECT          = new GenericStatistic(Names.ASPECT,          StatisticDimension.DIMENSIONLESS);
    static final PlottableStatistic CIRCULARITY     = new GenericStatistic(Names.CIRCULARITY,     StatisticDimension.DIMENSIONLESS);
    static final PlottableStatistic VARIABILITY     = new GenericStatistic(Names.VARIABILITY,     StatisticDimension.DIMENSIONLESS);
    static final PlottableStatistic ELONGATION      = new GenericStatistic(Names.ELONGATION,      StatisticDimension.DIMENSIONLESS);
    static final PlottableStatistic REGULARITY      = new GenericStatistic(Names.REGULARITY,      StatisticDimension.DIMENSIONLESS);
    static final PlottableStatistic BOUNDING_HEIGHT = new GenericStatistic(Names.BOUNDING_HEIGHT, StatisticDimension.LENGTH);
    static final PlottableStatistic BOUNDING_WIDTH  = new GenericStatistic(Names.BOUNDING_WIDTH,  StatisticDimension.LENGTH);
    static final PlottableStatistic OP_RP_ANGLE     = new GenericStatistic(Names.OP_RP_ANGLE,     StatisticDimension.ANGLE);
    static final PlottableStatistic HOOK_LENGTH     = new GenericStatistic(Names.HOOK_LENGTH,     StatisticDimension.LENGTH);
    static final PlottableStatistic BODY_WIDTH      = new GenericStatistic(Names.BODY_WIDTH,      StatisticDimension.LENGTH);
    static final PlottableStatistic LOBE_COUNT      = new GenericStatistic(Names.LOBE_COUNT,      StatisticDimension.DIMENSIONLESS);
    static final PlottableStatistic PATH_LENGTH     = new GenericStatistic(Names.PATH_LENGTH,     StatisticDimension.DIMENSIONLESS);

    // Stats for the whole cell, aggregated across sub-components
    static final PlottableStatistic CELL_NUCLEUS_COUNT = new GenericStatistic(Names.CELL_NUCLEUS_COUNT, StatisticDimension.DIMENSIONLESS);
    static final PlottableStatistic CELL_NUCLEAR_AREA  = new GenericStatistic(Names.CELL_NUCLEAR_AREA, StatisticDimension.AREA);
    static final PlottableStatistic CELL_NUCLEAR_RATIO = new GenericStatistic(Names.CELL_NUCLEAR_RATIO, StatisticDimension.DIMENSIONLESS);
    
    // Signal count in nuclei
    static final PlottableStatistic NUCLEUS_SIGNAL_COUNT = new GenericStatistic(Names.NUCLEUS_SIGNAL_COUNT, StatisticDimension.DIMENSIONLESS);

    // Signal statistics
    static final PlottableStatistic ANGLE                   = new GenericStatistic(Names.ANGLE,                   StatisticDimension.ANGLE);
    static final PlottableStatistic DISTANCE_FROM_COM       = new GenericStatistic(Names.DISTANCE_FROM_COM,       StatisticDimension.LENGTH);
    static final PlottableStatistic FRACT_DISTANCE_FROM_COM = new GenericStatistic(Names.FRACT_DISTANCE_FROM_COM, StatisticDimension.DIMENSIONLESS);
    static final PlottableStatistic RADIUS                  = new GenericStatistic(Names.RADIUS,                  StatisticDimension.LENGTH);
    static final PlottableStatistic LENGTH                  = new GenericStatistic(Names.LENGTH,                  StatisticDimension.LENGTH);
    static final PlottableStatistic DISPLACEMENT            = new GenericStatistic(Names.DISPLACEMENT,            StatisticDimension.ANGLE);    
    
    // Special stats. These should not be included in default charts - they are used as hidden data stores
    static final PlottableStatistic TSNE_1 = new GenericStatistic(Names.TSNE_1, StatisticDimension.DIMENSIONLESS);
    static final PlottableStatistic TSNE_2 = new GenericStatistic(Names.TSNE_2, StatisticDimension.DIMENSIONLESS);
    
    static final PlottableStatistic PCA_1 = new GenericStatistic(Names.PCA_1, StatisticDimension.DIMENSIONLESS);
    static final PlottableStatistic PCA_2 = new GenericStatistic(Names.PCA_2, StatisticDimension.DIMENSIONLESS);
    static final PlottableStatistic PCA_N = new GenericStatistic(Names.PCA_N, StatisticDimension.DIMENSIONLESS); // Number of PCs 
    

    /**
     * Get stats for the given component. Use the keys in
     * {@link CellularComponent}
     * 
     * @param component the component to get stats for
     * @return applicable stats, or null if the component was not recognised
     */
    static PlottableStatistic[] getStats(String component) {
        if (CellularComponent.NUCLEUS.equals(component))
            return getNucleusStats().toArray(new PlottableStatistic[0]);
        if (CellularComponent.NUCLEAR_SIGNAL.equals(component))
            return getSignalStats().toArray(new PlottableStatistic[0]);
        if (CellularComponent.NUCLEAR_BORDER_SEGMENT.equals(component))
            return getSegmentStats().toArray(new PlottableStatistic[0]);
        return null;
    }
    
    /**
     * Create a statistic for a principal component
     * @param pc the number of the component, from 1 to n
     * @return the stat for the component
     */
    static PlottableStatistic makePrincipalComponent(int pc) {
    	return new GenericStatistic("PC"+pc, StatisticDimension.DIMENSIONLESS);
    }
    
    /**
     * Create a statistic for a principal component with a cluster group
     * @param pc the number of the component, from 1 to n
     * @param id a group id
     * @return the stat for the component
     */
    static PlottableStatistic makePrincipalComponent(int pc, UUID id) {
    	return new GenericStatistic("PC"+pc+"_"+id, StatisticDimension.DIMENSIONLESS);
    }
    
    /**
     * Create a statistic for the number of principal components with a cluster group
     * @param id a group id
     * @return the stat for the component
     */
    static PlottableStatistic makePrincipalComponentNumber(UUID id) {
    	return new GenericStatistic(Names.PCA_N+"_"+id, StatisticDimension.DIMENSIONLESS);
    }
    
    /**
     * All available stats
     * 
     * @return
     */
    static List<PlottableStatistic> getAllStatsTypes() {
    	List<PlottableStatistic> list = new ArrayList<>();
    	list.add(AREA);
    	list.add(PERIMETER);
    	list.add(MAX_FERET);
    	list.add(MIN_DIAMETER);
    	list.add(ELLIPTICITY);
    	list.add(ASPECT);
    	list.add(CIRCULARITY);
    	list.add(VARIABILITY);
    	list.add(ELONGATION);
    	list.add(REGULARITY);
    	list.add(BOUNDING_HEIGHT);
    	list.add(BOUNDING_WIDTH);
    	list.add(OP_RP_ANGLE);
    	list.add(HOOK_LENGTH);
    	list.add(BODY_WIDTH);
    	list.add(LOBE_COUNT);
    	list.add(PATH_LENGTH);
    	list.add(CELL_NUCLEUS_COUNT);
    	list.add(CELL_NUCLEAR_AREA);
    	list.add(CELL_NUCLEAR_RATIO);
    	list.add(NUCLEUS_SIGNAL_COUNT);
    	list.add(ANGLE);
    	list.add(DISTANCE_FROM_COM);
    	list.add(FRACT_DISTANCE_FROM_COM);
    	list.add(RADIUS);
    	list.add(LENGTH);
    	list.add(DISPLACEMENT);
    	
    	 return list;
    }

    /**
     * Fetch the stat with the given name, if available.
     * @param name the name of the stat
     * @return the stat, or null if none is present
     */
    static PlottableStatistic of(String name) {
    	
    	List<PlottableStatistic> all = getAllStatsTypes();
    	
    	for(PlottableStatistic stat : all) {
    		if(stat.name().equals(name))
    			return stat;
    	}
    	return null;
    }

    /**
     * Get stats for generic cellular components.
     * 
     * @return
     */
    static List<PlottableStatistic> getComponentStats() {
        List<PlottableStatistic> list = new ArrayList<>();
        list.add(AREA);
        list.add(PERIMETER);
        list.add(MAX_FERET);
        list.add(CIRCULARITY);
        return list;
    }

    static List<PlottableStatistic> getCellStats() {
        List<PlottableStatistic> list = new ArrayList<>();
        list.add(CELL_NUCLEUS_COUNT);
        list.add(CELL_NUCLEAR_AREA);
        list.add(CELL_NUCLEAR_RATIO);
        list.add(LOBE_COUNT);
        return list;
    }

    /**
     * Get default type of nucleus stats; these are for mouse sperm nuclei
     * 
     * @return
     */
    static List<PlottableStatistic> getNucleusStats() {
        return getRodentSpermNucleusStats();
    }

    /**
     * Get stats for round nuclei
     * 
     * @return
     */
    static List<PlottableStatistic> getNucleusStats(NucleusType type) {

        switch (type) {
        case ROUND:  return getRoundNucleusStats();
        case NEUTROPHIL: return getLobedNucleusStats();
        case PIG_SPERM: return getRoundNucleusStats();
        case RODENT_SPERM: return getRodentSpermNucleusStats();
        default: return getRoundNucleusStats();
        }
    }

    static List<PlottableStatistic> getRoundNucleusStats() {

        List<PlottableStatistic> list = getComponentStats();
        list.add(MIN_DIAMETER);
        list.add(ELLIPTICITY);
        list.add(ASPECT);
        list.add(ELONGATION);
    	list.add(REGULARITY);
        list.add(VARIABILITY);
        list.add(BOUNDING_HEIGHT);
        list.add(BOUNDING_WIDTH);
        
        // Enable when ready to display GLCM in GUI
//        for(PlottableStatistic s : GLCMValue.toStats())
//        	list.add(s);
        	
        return list;
    }

    /**
     * Get stats for round nuclei
     * 
     * @return
     */
    static List<PlottableStatistic> getLobedNucleusStats() {
        List<PlottableStatistic> list = getRoundNucleusStats();
        list.add(LOBE_COUNT);
        return list;
    }

    /**
     * Get stats for rodent sperm nuclei
     * 
     * @return
     */
    static List<PlottableStatistic> getRodentSpermNucleusStats() {
        List<PlottableStatistic> list = getRoundNucleusStats();
//        list.add(OP_RP_ANGLE);
        list.add(HOOK_LENGTH);
        list.add(BODY_WIDTH);
        return list;
    }

    /**
     * Get stats for nuclear signals
     * 
     * @return
     */
    static List<PlottableStatistic> getSignalStats() {
        List<PlottableStatistic> list = getComponentStats();
        list.add(ANGLE);
        list.add(DISTANCE_FROM_COM);
        list.add(FRACT_DISTANCE_FROM_COM);
        list.add(RADIUS);
        return list;
    }

    /**
     * Get stats for nuclear border segments
     * 
     * @return
     */
    static List<PlottableStatistic> getSegmentStats() {
        List<PlottableStatistic> list = new ArrayList<PlottableStatistic>(2);
        list.add(LENGTH);
        list.add(DISPLACEMENT);
        return list;
    }

    /**
     * Get the string representation (name) of the statistic.
     * 
     * @return
     */
    @Override
	String toString();
    
    /**
     * Get the name of the stat
     * @return
     */
    String name();

    /**
     * Test if the statistic has units
     * 
     * @return
     */
    boolean isDimensionless();

    /**
     * Test if the statistic is an angle
     * 
     * @return
     */
    boolean isAngle();

    /**
     * Get the dimension of the statistic (area, length, angle, none)
     * 
     * @return
     */
    StatisticDimension getDimension();

    /**
     * Get the label (name and units) for the stat
     * 
     * @return
     */
    String label(MeasurementScale scale);

    /**
     * Convert the input value (assumed to be pixels) using the given factor (
     * Nucleus.getScale() ) into the appropriate scale
     * 
     * @param value the pixel measure
     * @param factor the conversion factor to microns
     * @param scale the desired scale
     * @return
     */
    double convert(double value, double factor, MeasurementScale scale);

    /**
     * Get the appropriate units label for the statistic, based on its
     * dimension. Eg. square units, units or nothing
     * 
     * @param scale
     * @return
     */
    String units(MeasurementScale scale);

    /**
     * Convert the length in pixels into a length in microns. Assumes that the
     * scale is in pixels per micron
     * 
     * @param pixels the number of pixels
     * @param scale the size of a pixel in microns
     * @return
     */
    static double lengthToMicrons(double pixels, double scale) {
    	return pixels / scale;
    }

    /**
     * Convert the area in pixels into an area in microns. Assumes that the
     * scale is in pixels per micron
     * 
     * @param pixels the number of pixels
     * @param scale the size of a pixel in microns
     * @return
     */
    static double areaToMicrons(double pixels, double scale) {
    	return pixels / (scale * scale);
    }

    /**
     * Convert the input value (assumed to be pixels) using the given factor (
     * CellularComponent.getScale() ) into the appropriate scale
     * 
     * @param value the pixel measure
     * @param factor the conversion factor to microns
     * @param scale the desired scale
     * @param dim the dimension of the statistic
     * @return the converted value
     */
    static double convert(double value, double factor, MeasurementScale scale, StatisticDimension dim) {
        switch (scale) {
	        case MICRONS: {
	            switch (dim) {
		            case AREA:   return PlottableStatistic.areaToMicrons(value, factor);
		            case LENGTH: return PlottableStatistic.lengthToMicrons(value, factor);
		            case DIMENSIONLESS:
		            case ANGLE:
		            default: return value;
	            }
	        }
	
	        case PIXELS: return value;
	        default: return value;
        }

    }

    /**
     * Create a units label for the given scale and dimension
     * 
     * @param scale
     * @param dim
     * @return
     */
    static String units(MeasurementScale scale, StatisticDimension dim) {
        switch (dim) {
	
	        case AREA:   return "square " + scale.toString().toLowerCase();
	        case LENGTH: return scale.toString().toLowerCase();
	        case ANGLE:  return "degrees";
	        case DIMENSIONLESS:
	        default: return "";
	    }
    }

}
