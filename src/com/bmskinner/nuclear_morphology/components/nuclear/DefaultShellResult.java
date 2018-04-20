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

import java.io.IOException;
import java.util.ArrayList;
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
    public DefaultShellResult setRawMeans(CountType type, List<Double> means) {

        if (type == null || means == null) {
            throw new IllegalArgumentException("Type or list is null");
        }

        if (means.size() != shellCount) {
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

        for (int i = 0; i < means.size(); i++) {
            target[i] = means.get(i);
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
    public DefaultShellResult setNormalisedMeans(CountType type, List<Double> means) {

        if (type == null || means == null) {
            throw new IllegalArgumentException("Type or list is null");
        }

        if (means.size() != shellCount) {
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

        for (int i = 0; i < means.size(); i++) {
            target[i] = means.get(i);
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
        this.setRawMeans(CountType.SIGNAL, s.getRawMeans(CountType.SIGNAL))
                .setRawMeans(CountType.COUNTERSTAIN, s.getRawMeans(CountType.COUNTERSTAIN))
                .setNormalisedMeans(CountType.SIGNAL, s.getNormalisedMeans(CountType.SIGNAL))
                .setNormalisedMeans(CountType.COUNTERSTAIN, s.getNormalisedMeans(CountType.COUNTERSTAIN))
//                .setRawStandardErrors(CountType.SIGNAL, s.getRawStandardErrors(CountType.SIGNAL))
//                .setRawStandardErrors(CountType.COUNTERSTAIN, s.getRawStandardErrors(CountType.COUNTERSTAIN))
                // .setPixelCounts(CountType.SIGNAL,
                // s.getPixelCounts(CountType.SIGNAL))
                // .setPixelCounts(CountType.NUCLEUS,
                // s.getPixelCounts(CountType.NUCLEUS))
                .setRawChiResult(CountType.SIGNAL, s.getRawChiSquare(CountType.SIGNAL),
                        s.getRawPValue(CountType.SIGNAL))
                .setRawChiResult(CountType.COUNTERSTAIN, s.getRawChiSquare(CountType.COUNTERSTAIN),
                        s.getRawPValue(CountType.COUNTERSTAIN))
                .setNormalisedChiResult(CountType.SIGNAL, s.getNormalisedChiSquare(CountType.SIGNAL),
                        s.getNormalisedPValue(CountType.SIGNAL))
                .setNormalisedChiResult(CountType.COUNTERSTAIN, s.getNormalisedChiSquare(CountType.COUNTERSTAIN),
                        s.getNormalisedPValue(CountType.COUNTERSTAIN));

    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IShellResult#getCounts()
     */
    // @Override
    // public List<Integer> getPixelCounts(CountType type){
    //
    // List<Integer> result = new ArrayList<Integer>(shellCount);
    // int[] template = null;
    // switch(type){
    // case SIGNAL:{
    // template = signalCounts;
    // break;
    // }
    // case NUCLEUS:{
    // template = nucleusCounts;
    // break;
    // }
    // default: {
    // template = signalCounts;
    // break;
    // }
    // }
    //
    // for(int i : template){
    // result.add(i);
    // }
    // return result;
    // }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IShellResult#getMeans()
     */
    @Override
    public List<Double> getRawMeans(CountType type) {
        List<Double> result = new ArrayList<Double>(shellCount);
        double[] template = null;
        switch (type) {
        case SIGNAL: {
            template = signalRawMeans;
            break;
        }
        case COUNTERSTAIN: {
            template = nucleusRawMeans;
            break;
        }
        default: {
            template = signalRawMeans;
            break;
        }
        }

        for (double i : template) {
            result.add(i);
        }
        return result;
    }
    
    @Override
    public double getRawMeanShell(CountType type){
        switch (type) {
	        case SIGNAL: {
	        	double mean = 0;
	        	for(int i=0; i<shellCount; i++){
	        		mean += i*signalRawMeans[i];
	        	}
	        	return mean;
	        }
	        case COUNTERSTAIN: {
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
    
    @Override
    public double getNormalisedMeanShell(CountType type){
        switch (type) {
	        case SIGNAL: {
	        	double mean = 0;
	        	for(int i=0; i<shellCount; i++){
	        		mean += i*signalNormMeans[i];
	        	}
	        	return mean;
	        }
	        case COUNTERSTAIN: {
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

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IShellResult#getNormalisedMeans()
     */
    @Override
    public List<Double> getNormalisedMeans(CountType type) {
        List<Double> result = new ArrayList<Double>(shellCount);
        double[] template = null;
        switch (type) {
        case SIGNAL: {
            template = signalNormMeans;
            break;
        }
        case COUNTERSTAIN: {
            template = nucleusNormMeans;
            break;
        }
        default: {
            template = signalNormMeans;
            break;
        }
        }

        for (double i : template) {
            result.add(i);
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IShellResult#getStandardErrors()
     */
//    @Override
//    public List<Double> getRawStandardErrors(CountType type) {
//        List<Double> result = new ArrayList<Double>(shellCount);
//        double[] template = null;
//        switch (type) {
//        case SIGNAL: {
//            template = signalRawStderrs;
//            break;
//        }
//        case COUNTERSTAIN: {
//            template = nucleusRawStderrs;
//            break;
//        }
//        default: {
//            template = signalRawStderrs;
//            break;
//        }
//        }
//
//        for (double i : template) {
//            result.add(i);
//        }
//        return result;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see components.nuclear.IShellResult#getStandardErrors()
//     */
//    @Override
//    public List<Double> getNormalisedStandardErrors(CountType type) {
//        List<Double> result = new ArrayList<Double>(shellCount);
//        double[] template = null;
//        switch (type) {
//        case SIGNAL: {
//            template = signalNormStderrs;
//            break;
//        }
//        case COUNTERSTAIN: {
//            template = nucleusNormStderrs;
//            break;
//        }
//        default: {
//            template = signalNormStderrs;
//            break;
//        }
//        }
//
//        for (double i : template) {
//            result.add(i);
//        }
//        return result;
//    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IShellResult#getChiSquare()
     */
    @Override
    public double getRawChiSquare(CountType type) {

        switch (type) {
        case SIGNAL: {
            return signalRawChi;
        }
        case COUNTERSTAIN: {
            return nucleusRawChi;
        }
        default:
            return signalRawChi;
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IShellResult#getChiSquare()
     */
    @Override
    public double getNormalisedChiSquare(CountType type) {

        switch (type) {
        case SIGNAL: {
            return signalNormChi;
        }
        case COUNTERSTAIN: {
            return nucleusNormChi;
        }
        default:
            return signalNormChi;
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IShellResult#getPValue()
     */
    @Override
    public double getRawPValue(CountType type) {
        switch (type) {
        case SIGNAL: {
            return signalRawPval;
        }
        case COUNTERSTAIN: {
            return nucleusRawPval;
        }
        default:
            return signalRawPval;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IShellResult#getPValue()
     */
    @Override
    public double getNormalisedPValue(CountType type) {
        switch (type) {
        case SIGNAL: {
            return signalNormPval;
        }
        case COUNTERSTAIN: {
            return nucleusNormPval;
        }
        default:
            return signalNormPval;
        }
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

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

}
