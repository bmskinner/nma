/**
 * 
 */
package no.analysis;

import ij.IJ;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.inference.ChiSquareTest;

import no.utility.Stats;

/**
 *
 */
public class ShellCounter {
	
	int numberOfShells;
	Map<Integer, ArrayList<Double>> shellValues = new LinkedHashMap<Integer, ArrayList<Double>>(0); // store the values
	
	public ShellCounter(int numberOfShells){
		this.numberOfShells = numberOfShells;
		for(int i=0;i<numberOfShells;i++){
			shellValues.put(i, new ArrayList<Double>(0));
		}
	}
	
	public void addValues(double[] values) throws IllegalArgumentException{
		if(values.length!=this.numberOfShells){
			throw new IllegalArgumentException("Input array is wrong size");
		}
		Double firstShell = new Double(values[0]);
        if(firstShell.isNaN()){
        	throw new IllegalArgumentException("Argument is not a number");
        }
        
		for(int i=0;i<numberOfShells;i++){
			List<Double> shell = shellValues.get(i);
			shell.add(values[i]);
		}
	}
	
	private List<Double> getMeans() throws Exception{
		List<Double> result = new ArrayList<Double>(0);
		for(int i=0;i<numberOfShells;i++){
			result.add(Stats.mean(getShell(i)));
		}
		return result;
	}
	
	private List<Double> getStandardErrors() throws Exception{
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
				IJ.log("    Error getting chi-square value: "+e.getMessage());
		}
		return chi;
	}
	
	private double[] getShell(int shell){
		List<Double> values = shellValues.get(shell);
		double[] array = new double[values.size()];
		for(int i=0;i<array.length;i++){
			array[i] = values.get(i);
		}
		return array;
	}

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
	
 	private double[] getExpected(){
		double[] expected = new double[numberOfShells];
		double count = shellValues.get(0).size();
		for(int i=0;i<numberOfShells; i++){
			expected[i] = ((double)1/(double)numberOfShells) * count;
//			IJ.log("E: "+expected[i]);
		}
		return expected;
	}
		
 	public int size(){
 		return shellValues.get(0).size();
 	}
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
	    	IJ.log(line);
	    }
		IJ.log("");
	}
	
}
