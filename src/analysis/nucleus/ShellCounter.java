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

package analysis.nucleus;

import ij.IJ;
import stats.Stats;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.math3.stat.inference.ChiSquareTest;

import analysis.AbstractLoggable;

public class ShellCounter extends AbstractLoggable {
	
	int numberOfShells;
	Map<Integer, ArrayList<Double>> shellValues = new LinkedHashMap<Integer, ArrayList<Double>>(0); // store the values
		
	public ShellCounter(int numberOfShells){
		
//		this.logger = new Logger(log, "ShellCounter");
		
		this.numberOfShells = numberOfShells;
		for(int i=0;i<numberOfShells;i++){
			shellValues.put(i, new ArrayList<Double>(0));
		}
		
	}
	
	/**
	 * Add an array of values to the current shell
	 * @param values
	 * @throws IllegalArgumentException
	 */
	public void addValues(double[] values) throws IllegalArgumentException{
		if(values.length!=this.numberOfShells){
			throw new IllegalArgumentException("Input array is wrong size");
		}
		
		// check the first entry in the list is a value
		Double firstShell = new Double(values[0]);
        if(firstShell.isNaN()){
        	throw new IllegalArgumentException("Argument is not a number");
        }
        
        
		for(int i=0;i<numberOfShells;i++){
			List<Double> shell = shellValues.get(i);
			shell.add(values[i]);
		}
	}
	
	public List<Double> getMeans() throws Exception{
		List<Double> result = new ArrayList<Double>(0);
		for(int i=0;i<numberOfShells;i++){
			double[] values = getShell(i);
			result.add(Stats.mean(values));
		}
		return result;
	}
	
	public List<Double> getStandardErrors() throws Exception{
		List<Double> result = new ArrayList<Double>(0);
		for(int i=0;i<numberOfShells;i++){
			result.add(Stats.stderr(getShell(i)));
		}
		return result;
	}
	
	public void export(File f){
		
		if(this.size()==0){ // don't make empty log files
			return;
		}
		
//		IJ.log("    Counter Size: "+this.size());
		if(f.exists()){
			f.delete();
		}
		NumberFormat formatter = new DecimalFormat("#0.000");     
		StringBuilder log = new StringBuilder();
		
		// export the mean and SE for each shell
	    try{
	    	log.append("--------\r\nMEAN\r\n--------\r\n");
		    List<Double> means = getMeans();
		    for(Double d : means){
		    	log.append(formatter.format(d)+"\t");
		    }
		    log.append("\r\n");
		    log.append("--------\r\nSTANDARD ERROR OF THE MEAN\r\n--------\r\n");
		    List<Double> se = getStandardErrors();
		    for(Double d : se){
		    	log.append(formatter.format(d)+"\t");
		    }
		    log.append("\r\n");
	    } catch(Exception e){
	    	IJ.log("Error exporting stats: "+e.getMessage());
	    }
	    
	    // export chi square stats
//	    log.append("--------\r\nCHI SQUARE\r\n--------\r\n");
//	    log.append("Signals   :\t"+this.size()+"\r\n");
//	    log.append("Chi square:\t"+getChiSquare()+"\r\n");
//	    log.append("p-value   :\t"+getPValue()+"\r\n");
	    
	    
	    log.append("--------\r\nOUTER <- SHELLS -> INNER\r\n--------\r\n");
	    // Export the individual values
	    for(int i=0; i<5; i++){
	      log.append("SHELL_"+i+"\t");
	    }

	    log.append("\r\n");
	    for(int i = 0; i< shellValues.get(0).size();i++){ // go through each signal
	    	for(int j = 0; j<numberOfShells; j++){ // each shell for signal
	    		List<Double> list = shellValues.get(j);
	    		log.append(formatter.format(list.get(i))+"\t");
	    	}
	    	log.append("\r\n");
	    }

	    IJ.append(log.toString(), f.getAbsolutePath());
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
				logError( "Error getting chi square values", e);
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
			logError( "Error getting shell values", e);
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
