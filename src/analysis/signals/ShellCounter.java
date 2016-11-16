/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package analysis.signals;

import ij.IJ;
import logging.Loggable;
import stats.Mean;
import stats.Stats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.math3.stat.inference.ChiSquareTest;


public class ShellCounter implements Loggable {
	
	private int numberOfShells;
	private Map<Integer, List<Double>> shellValues = new HashMap<Integer, List<Double>>(0); // store the values
	private Map<Integer, List<Double>> normValues  = new HashMap<Integer, List<Double>>(0); // store the values
	private Map<Integer, Integer> counts = new HashMap<Integer, Integer>(0); // store the pixel counts
		
	public ShellCounter(int numberOfShells){
		
		this.numberOfShells = numberOfShells;
		for(int i=0;i<numberOfShells;i++){
			shellValues.put(i, new ArrayList<Double>(0));
			normValues.put(i, new ArrayList<Double>(0));
			counts.put(i, 0);
		}
		
	}
	
	/**
	 * Add an array of values to the current shell
	 * @param values
	 * @throws IllegalArgumentException
	 */
	public void addValues(double[] proportions, int[] counts) {
		if(proportions.length!=numberOfShells || counts.length!=numberOfShells){
			throw new IllegalArgumentException("Input array is wrong size");
		}
		
		// check the first entry in the list is a value
		Double firstShell = new Double(proportions[0]);
        if(firstShell.isNaN()){
        	throw new IllegalArgumentException("Argument is not a number");
        }
        
        
		for(int i=0;i<numberOfShells;i++){
			List<Double> shell = shellValues.get(i);
			shell.add(proportions[i]);
			
			int shellTotal = this.counts.get(i);
			this.counts.put(i, shellTotal+counts[i]);
		}
	}
	
	public void addNormalisedValues(double[] proportions){
		for(int i=0;i<numberOfShells;i++){
			List<Double> shell = normValues.get(i);
			shell.add(proportions[i]);
		}
	}
	
	public List<Double> getMeans() {
		List<Double> result = new ArrayList<Double>(0);
		for(int i=0;i<numberOfShells;i++){
			double[] values = getShell(i);
			result.add( new Mean(values).doubleValue() ); //Stats.mean(values)
			
		}
		return result;
	}
	
	public List<Double> getNormalisedMeans() {
		List<Double> result = new ArrayList<Double>(0);
		for(int i=0;i<numberOfShells;i++){
			double[] values = getNormShell(i);
			result.add( new Mean(values).doubleValue() ); //Stats.mean(values)
			
		}
		return result;
	}
	
	public List<Double> getStandardErrors() {
		List<Double> result = new ArrayList<Double>(0);
		for(int i=0;i<numberOfShells;i++){
			result.add(Stats.stderr(getShell(i)));
		}
		return result;
	}
	
	public List<Double> getNormalisedStandardErrors() {
		List<Double> result = new ArrayList<Double>(0);
		for(int i=0;i<numberOfShells;i++){
			result.add(Stats.stderr(getNormShell(i)));
		}
		return result;
	}
	
	public List<Integer> getCounts() {
		List<Integer> result = new ArrayList<Integer>(0);
		for(int i=0;i<numberOfShells;i++){
			result.add(counts.get(i));
		}
		return result;
	}
			
	public double getPValue(){
		double pvalue = 1;
		try{
			long[]   observed = getObserved();
			double[] expected = getExpected();
						
			ChiSquareTest test = new ChiSquareTest();
			pvalue = test.chiSquareTest(expected, observed);
//			IJ.log("    Chi test: p="+pvalue);
		
		} catch(Exception e){
			IJ.log("    Error getting p-values: "+e.getMessage());
		}
		return pvalue;
	}
	
	public double getChiSquare(){
		double chi = 0;
		try{
			long[]   observed = getObserved();
			double[] expected = getExpected();
			ChiSquareTest test = new ChiSquareTest();
			chi = test.chiSquare(expected, observed);
			} catch(Exception e){
				error( "Error getting chi square values", e);
				this.print();
		}
		return chi;
	}
	
	/**
	 * Get the values within the current shell. If a value
	 * is NaN, return 0
	 * @param shell the shell to return
	 * @return
	 */
	private double[] getShell(int shell){
		List<Double> values = shellValues.get(shell);
		double[] array = new double[values.size()];
		
		try{
			for(int i=0;i<array.length;i++){

				// if the value is not a number, put zero
				array[i] = values.get(i).isNaN() 
						? 0
								: values.get(i);
			}
		} catch(Exception e){
			error( "Error getting shell values", e);
			this.print();
		}
		return array;
	}
	
	/**
	 * Get the values within the current shell. If a value
	 * is NaN, return 0
	 * @param shell the shell to return
	 * @return
	 */
	private double[] getNormShell(int shell){
		List<Double> values = normValues.get(shell);
		double[] array = new double[values.size()];
		
		try{
			for(int i=0;i<array.length;i++){

				// if the value is not a number, put zero
				array[i] = values.get(i).isNaN() 
						? 0
						: values.get(i);
			}
		} catch(Exception e){
			error( "Error getting shell values", e);
			this.print();
		}
		return array;
	}

	/**
	 * Get the observed values as a long array. Long is needed
	 * for the chi-square test
	 * @return the observerd shell values
	 * @throws Exception
	 */
	private long[] getObserved() throws Exception{
		long[] observed = new long[numberOfShells];
		int count = shellValues.get(0).size();
		List<Double> means = getMeans();
		for(int i=0;i<numberOfShells; i++){
			double mean = means.get(i);
			observed[i] = (long) (mean*count);
		}
		return observed;
	}
	
 	/**
 	 * Get the expected values for chi-sqare test, assuming
 	 * an equal proportion of signal per shell
 	 * @return the expected values
 	 */
 	private double[] getExpected(){
		double[] expected = new double[numberOfShells];
		double count = shellValues.get(0).size();
		for(int i=0;i<numberOfShells; i++){
			expected[i] = ((double)1/(double)numberOfShells) * count;
		}
		return expected;
	}
		
 	/**
 	 * Get the number of signals measured
 	 * @return
 	 */
 	public int size(){
 		return shellValues.get(0).size();
 	}
 	
 	
	/**
	 * For debugging - print the contents of the dataset to log
	 */
	public void print(){
		if(this.size()==0){ // don't make empty log files
			return;
		}
		for(int i = 0; i< shellValues.get(0).size();i++){ // go through each signal
			String line = "";
	    	for(int j = 0; j<numberOfShells; j++){ // each shell for signal
	    		List<Double> list = shellValues.get(j);
	    		line += list.get(i)+"\t";
	    	}
	    	log(Level.INFO, line);
	    }
		log(Level.INFO, "");
	}
	
}
