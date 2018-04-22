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


package com.bmskinner.nuclear_morphology.components.nuclear;

import java.util.List;

/**
 * A default implementation of the IShellResult interface. It uses a builder
 * pattern for setting values.
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public class DefaultShellResult implements IShellResult {

    private static final long serialVersionUID = 1L;

    private final int shellCount;

    private double[] signalRawMeans;
    private double[] signalNormMeans;
    private double[] signalRawStderrs;
    private double[] signalNormStderrs;

    private double signalRawChi   = 0;
    private double signalRawPval  = 1;
    private double signalNormChi  = 0;
    private double signalNormPval = 1;

    private double[] nucleusRawMeans;
    private double[] nucleusNormMeans;
    private double[] nucleusRawStderrs;
    private double[] nucleusNormStderrs;

    private double nucleusRawChi   = 0;
    private double nucleusRawPval  = 1;
    private double nucleusNormChi  = 0;
    private double nucleusNormPval = 1;

    /**
     * Construct with a given shell count
     * 
     * @param shellCount the shell count
     */
    public DefaultShellResult(int shellCount) {

        if (shellCount < 1) 
            throw new IllegalArgumentException("Shell count must be greater than 1");

        this.shellCount = shellCount;

        signalRawMeans = new double[shellCount];
        signalNormMeans = new double[shellCount];
        signalRawStderrs = new double[shellCount];
        signalNormStderrs = new double[shellCount];

        nucleusRawMeans = new double[shellCount];
        nucleusNormMeans = new double[shellCount];
        nucleusRawStderrs = new double[shellCount];
        nucleusNormStderrs = new double[shellCount];
    }

    /**
     * Set the raw mean proportional values for the given type
     * 
     * @param type
     *            the counting type
     * @param means
     *            the mean values
     * @return this shell result
     */
    public DefaultShellResult setRawMeans(CountType type, double[] means) {

        if (type == null || means == null) {
            throw new IllegalArgumentException("Type or list is null");
        }

        if (means.length != shellCount) {
            throw new IllegalArgumentException("List size does not match shell count");
        }

        double[] target;
        switch (type) {
        case SIGNAL: {
            target = signalRawMeans;
            break;
        }
        case COUNTERSTAIN: {
            target = nucleusRawMeans;
            break;
        }
        default: {
            target = signalRawMeans;
            break;
        }
        }

        for (int i = 0; i < means.length; i++) {
            target[i] = means[i];
        }

        return this;

    }

    /**
     * Set the normalised mean proportional values for the given type
     * 
     * @param type
     *            the counting type
     * @param means
     *            the mean values
     * @return this shell result
     */
    public DefaultShellResult setNormalisedMeans(CountType type, double[] means) {

        if (type == null || means == null) {
            throw new IllegalArgumentException("Type or list is null");
        }

        if (means.length != shellCount) {
            throw new IllegalArgumentException("List size does not match shell count");
        }

        double[] target;
        switch (type) {
        case SIGNAL: {
            target = signalNormMeans;
            break;
        }
        case COUNTERSTAIN: {
            target = nucleusNormMeans;
            break;
        }
        default: {
            target = signalNormMeans;
            break;
        }
        }

        for (int i = 0; i < means.length; i++) {
            target[i] = means[i];
        }

        return this;

    }

    /**
     * Set the normalised mean proportional values for the given type
     * 
     * @param type
     *            the counting type
     * @param means
     *            the mean values
     * @return this shell result
     */
    public DefaultShellResult setRawStandardErrors(CountType type, List<Double> errs) {

        if (type == null || errs == null) {
            throw new IllegalArgumentException("Type or list is null");
        }

        if (errs.size() != shellCount) {
            throw new IllegalArgumentException("Standard error list size does not match shell count");
        }

        double[] target;
        switch (type) {
        case SIGNAL: {
            target = signalRawStderrs;
            break;
        }
        case COUNTERSTAIN: {
            target = nucleusRawStderrs;
            break;
        }
        default:
            target = signalRawStderrs;
            break;
        }

        for (int i = 0; i < errs.size(); i++) {
            target[i] = errs.get(i);
        }

        return this;

    }

    /**
     * Set the normalised mean proportional values for the given type
     * 
     * @param type
     *            the counting type
     * @param means
     *            the mean values
     * @return this shell result
     */
    public DefaultShellResult setNormalisedStandardErrors(CountType type, List<Double> errs) {

        if (type == null || errs == null) {
            throw new IllegalArgumentException("Type or list is null");
        }

        if (errs.size() != shellCount) {
            throw new IllegalArgumentException("List size does not match shell count");
        }

        double[] target;
        switch (type) {
        case SIGNAL: {
            target = signalNormStderrs;
            break;
        }
        case COUNTERSTAIN: {
            target = nucleusNormStderrs;
            break;
        }
        default: {
            target = signalNormStderrs;
            break;
        }
        }

        for (int i = 0; i < errs.size(); i++) {
            target[i] = errs.get(i);
        }

        return this;

    }

    /**
     * Set the raw chi square results for the given count type
     * 
     * @param type
     *            the counting type
     * @param chi
     *            the chi test result
     * @param pval
     *            the chi test pvalue
     * @return this shell result
     */
    public DefaultShellResult setRawChiResult(CountType type, double chi, double pval) {

        if (type == null) {
            throw new IllegalArgumentException("Type is null");
        }

        switch (type) {
        case SIGNAL: {
            signalRawChi = chi;
            signalRawPval = pval;
            break;
        }
        case COUNTERSTAIN: {
            nucleusRawChi = chi;
            nucleusRawPval = pval;
            break;
        }
        default: {
            signalRawChi = chi;
            signalRawPval = pval;
            break;
        }

        }

        return this;

    }

    /**
     * Set the normalised chi square results for the given count type
     * 
     * @param type
     *            the counting type
     * @param chi
     *            the chi test result
     * @param pval
     *            the chi test pvalue
     * @return this shell result
     */
    public DefaultShellResult setNormalisedChiResult(CountType type, double chi, double pval) {

        if (type == null) {
            throw new IllegalArgumentException("Type is null");
        }

        switch (type) {
        case SIGNAL: {
            signalNormChi = chi;
            signalNormPval = pval;
            break;
        }
        case COUNTERSTAIN: {
            nucleusNormChi = chi;
            nucleusNormPval = pval;
            break;
        }
        default: {
            signalNormChi = chi;
            signalNormPval = pval;
            break;
        }

        }

        return this;

    }

    /**
     * Create from an existing shell result
     * 
     * @param s
     */
    public DefaultShellResult(IShellResult s) {
        this(s.getNumberOfShells());
        this.setRawMeans(CountType.SIGNAL, s.getProportions(Aggregation.BY_SIGNAL, Normalisation.NONE))
                .setRawMeans(CountType.COUNTERSTAIN, s.getProportions(Aggregation.BY_NUCLEUS, Normalisation.NONE))
                .setNormalisedMeans(CountType.SIGNAL, s.getProportions(Aggregation.BY_SIGNAL, Normalisation.DAPI))
                .setNormalisedMeans(CountType.COUNTERSTAIN, s.getProportions(Aggregation.BY_NUCLEUS, Normalisation.DAPI))
//                .setRawStandardErrors(CountType.SIGNAL, s.getRawStandardErrors(CountType.SIGNAL))
//                .setRawStandardErrors(CountType.COUNTERSTAIN, s.getRawStandardErrors(CountType.COUNTERSTAIN))
                // .setPixelCounts(CountType.SIGNAL,
                // s.getPixelCounts(CountType.SIGNAL))
                // .setPixelCounts(CountType.NUCLEUS,
                // s.getPixelCounts(CountType.NUCLEUS))
                .setRawChiResult(CountType.SIGNAL, s.getChiSquareValue(Aggregation.BY_SIGNAL, Normalisation.NONE), s.getPValue(Aggregation.BY_SIGNAL, Normalisation.NONE))
                .setRawChiResult(CountType.COUNTERSTAIN, s.getChiSquareValue(Aggregation.BY_NUCLEUS, Normalisation.NONE), s.getPValue(Aggregation.BY_NUCLEUS, Normalisation.NONE))
                .setNormalisedChiResult(CountType.SIGNAL, s.getChiSquareValue(Aggregation.BY_SIGNAL, Normalisation.DAPI), s.getPValue(Aggregation.BY_SIGNAL, Normalisation.DAPI))
                .setNormalisedChiResult(CountType.COUNTERSTAIN, s.getChiSquareValue(Aggregation.BY_NUCLEUS, Normalisation.DAPI), s.getPValue(Aggregation.BY_NUCLEUS, Normalisation.DAPI));

    }
    
    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IShellResult#getNumberOfShells()
     */
    @Override
    public int getNumberOfShells() {
        return shellCount;
    }

	@Override
	public double[] getProportions(Aggregation agg, Normalisation norm) {
		switch (norm) {
	        case NONE: return getRawMeans(agg);
	        case DAPI: return getNormalisedMeans(agg);
	        default:   return getRawMeans(agg);
	    }
	}
	
	@Override
	public double[] getStdErrs(Aggregation agg, Normalisation norm) {
		switch (norm) {
	        case NONE: return getRawStdErrs(agg);
	        case DAPI: return getNormalisedStdErrs(agg);
	        default:   return getRawStdErrs(agg);
	    }
	}

	@Override
	public double getChiSquareValue(Aggregation agg, Normalisation norm) {
		switch (norm) {
	        case NONE: return getRawChiSquare(agg);
	        case DAPI: return getNormalisedChiSquare(agg);
	        default:   return getRawChiSquare(agg);
	    }
	}

	@Override
	public double getPValue(Aggregation agg, Normalisation norm) {
		switch (norm) {
	        case NONE: return getRawPValues(agg);
	        case DAPI: return getNormalisedPValues(agg);
	        default:   return getRawPValues(agg);
	    }
	}

	@Override
	public double getOverallShell(Aggregation agg, Normalisation norm) {
		switch (norm) {
	        case NONE: return getRawMeanShell(agg);
	        case DAPI: return getNormalisedMeanShell(agg);
	        default:   return getRawMeanShell(agg);
	    }
	}
    
    

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IShellResult#getMeans()
     */
    private double[] getRawMeans(Aggregation agg) {
        switch (agg) {
	        case BY_SIGNAL:  return signalRawMeans;
	        case BY_NUCLEUS: return nucleusRawMeans;
	        default:           return signalRawMeans;
        }
    }
    
    private double getRawMeanShell(Aggregation agg){
        switch (agg) {
	        case BY_SIGNAL: {
	        	double mean = 0;
	        	for(int i=0; i<shellCount; i++){
	        		mean += i*signalRawMeans[i];
	        	}
	        	return mean;
	        }
	        case BY_NUCLEUS: {
	        	double mean = 0;
	        	for(int i=0; i<shellCount; i++){
	        		mean += i*nucleusRawMeans[i];
	        	}
	        	return mean;
	        }
	        default: {
	        	double mean = 0;
	        	for(int i=0; i<shellCount; i++){
	        		mean += i*signalRawMeans[i];
	        	}
	        	return mean;
	        }
        }

    }
    
    private double getNormalisedMeanShell(Aggregation agg){
        switch (agg) {
	        case BY_SIGNAL: {
	        	double mean = 0;
	        	for(int i=0; i<shellCount; i++){
	        		mean += i*signalNormMeans[i];
	        	}
	        	return mean;
	        }
	        case BY_NUCLEUS: {
	        	double mean = 0;
	        	for(int i=0; i<shellCount; i++){
	        		mean += i*nucleusNormMeans[i];
	        	}
	        	return mean;
	        }
	        default: {
	        	double mean = 0;
	        	for(int i=0; i<shellCount; i++){
	        		mean += i*signalNormMeans[i];
	        	}
	        	return mean;
	        }
        }

    }


    private double[] getNormalisedMeans(Aggregation agg) {
    	switch (agg) {
	        case BY_SIGNAL:  return signalNormMeans;
	        case BY_NUCLEUS: return nucleusNormMeans;
	        default:           return signalNormMeans;
    	}
    }


    private double getRawChiSquare(Aggregation agg) {
    	switch (agg) {
	    	case BY_SIGNAL:  return signalRawChi;
	    	case BY_NUCLEUS: return nucleusRawChi;
	    	default:         return signalRawChi;
    	}

    }
    
    private double getNormalisedChiSquare(Aggregation agg) {
    	switch (agg) {
	    	case BY_SIGNAL:  return signalNormChi;
	    	case BY_NUCLEUS: return nucleusNormChi;
	    	default:         return signalNormChi;
    	}

    }
    
    private double getRawPValues(Aggregation agg) {
    	switch (agg) {
	    	case BY_SIGNAL:  return signalRawPval;
	    	case BY_NUCLEUS: return nucleusRawPval;
	    	default:         return signalRawPval;
    	}

    }
    
    private double getNormalisedPValues(Aggregation agg) {
    	switch (agg) {
	    	case BY_SIGNAL:  return signalNormPval;
	    	case BY_NUCLEUS: return nucleusNormPval;
	    	default:         return signalNormPval;
    	}

    }
    
    private double[] getRawStdErrs(Aggregation agg) {
    	switch (agg) {
	    	case BY_SIGNAL:  return signalRawStderrs;
	    	case BY_NUCLEUS: return nucleusRawStderrs;
	    	default:         return signalRawStderrs;
    	}

    }
    
    private double[] getNormalisedStdErrs(Aggregation agg) {
    	switch (agg) {
	    	case BY_SIGNAL:  return signalNormStderrs;
	    	case BY_NUCLEUS: return nucleusNormStderrs;
	    	default:         return signalNormStderrs;
    	}

    }
}
