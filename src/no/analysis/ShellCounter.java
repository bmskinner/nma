/**
 * 
 */
package no.analysis;

import ij.IJ;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.inference.ChiSquareTest;

import no.utility.Utils;
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
        if(!firstShell.isNaN() ){
			for(int i=0;i<numberOfShells;i++){
				List<Double> shell = shellValues.get(i);
				shell.add(values[i]);
			}
        }
	}
	
	private List<Double> getQuartiles(double quartile){
		List<Double> result = new ArrayList<Double>(0);
		for(int i=0;i<numberOfShells;i++){
			result.add(calculateQuartileOfShell(i, quartile));
		}
		return result;
	}

	private List<Double> getMeans(){
		List<Double> result = new ArrayList<Double>(0);
		for(int i=0;i<numberOfShells;i++){
			result.add(calculateMeanOfShell(i));
		}
		return result;
	}
	
	
	public void export(File f){
		if(f.exists()){
			f.delete();
		}
		StringBuilder log = new StringBuilder();

	    // create the log file header
	    for(int i=0; i<5; i++){
	      log.append("SHELL_"+i+"\t");
	    }

	    log.append("\r\n");
	    for(int i = 0; i< shellValues.get(0).size();i++){ // go through each signal
	    	for(int j = 0; j<numberOfShells; j++){ // each shell for signal
	    		List<Double> list = shellValues.get(j);
	    		log.append(list.get(i)+"\t");
	    	}
	    	log.append("\r\n");
	    }
	    log.append("--------\r\nMEDIAN AND IQRs\r\n--------\r\n");
	    
	 // export the median and IQR for each shell
	    List<Double> medians = getQuartiles(50);
	    for(Double d : medians){
	    	log.append(d.toString()+"\t");
	    }
	    List<Double> q1 = getQuartiles(25);
	    for(Double d : q1){
	    	log.append(d.toString()+"\t");
	    }
	    List<Double> q3 = getQuartiles(75);
	    for(Double d : q3){
	    	log.append(d.toString()+"\t");
	    }
	    log.append("\r\n");
	    
		// , plus any stats, plus charts
	    log.append("--------\r\nCHI SQUARE\r\n--------\r\n");
	    log.append("Chi square: "+getChiSquare()+"\r\n");
	    log.append("p-value   : "+getPValue()+"\r\n");
	    IJ.append(log.toString(), f.getAbsolutePath());
	}
	
	public double getPValue(){
		long[]   observed = getObserved();
		double[] expected = getExpected();
		
		ChiSquareTest test = new ChiSquareTest();
		double pvalue = test.chiSquareTest(expected, observed);
		return pvalue;
	}
	
	public double getChiSquare(){
		long[]   observed = getObserved();
		double[] expected = getExpected();
		
		ChiSquareTest test = new ChiSquareTest();
		double chi = test.chiSquare(expected, observed);
		return chi;
	}
	
	private long[] getObserved(){
		long[] observed = new long[numberOfShells];
		double count = shellValues.get(0).size();
		// List<Double> medians = getQuartiles(50);
		// for(int i=0;i<numberOfShells; i++){
		// 	observed[i] = (long) (medians.get(i).longValue()*count);
		// }
		List<Double> means = getMeans();
		for(int i=0;i<numberOfShells; i++){
			observed[i] = (long) (means.get(i).longValue()*count);
		}
		return observed;
	}
	
 	private double[] getExpected(){
		double[] expected = new double[numberOfShells];
		double count = shellValues.get(0).size();
		for(int i=0;i<numberOfShells; i++){
			expected[i] = ((double)1/(double)numberOfShells) * count;
		}
		return expected;
	}
	
	private double calculateQuartileOfShell(int shell, double quartile){
		List<Double> values = shellValues.get(shell);
		double[] array = new double[values.size()];
		for(int i=0;i<array.length;i++){
			array[i] = values.get(i);
		}
//		Double[] array = values.toArray(new Double[values.size()]);
		if(array.length>0){
			return Stats.quartile(array, quartile);
		} else {
			return 0;
		}
	}

	private double calculateMeanOfShell(int shell){
		List<Double> values = shellValues.get(shell);
		double[] array = new double[values.size()];
		for(int i=0;i<array.length;i++){
			array[i] = values.get(i);
		}
		if(array.length>0){
			return Stats.mean(array);
		} else {
			return 0;
		}
	}
	
	public void print(){
		for(int i = 0; i< shellValues.get(0).size();i++){ // go through each signal
			String line = "";
	    	for(int j = 0; j<numberOfShells; j++){ // each shell for signal
	    		List<Double> list = shellValues.get(j);
	    		line += list.get(i)+"\t";
	    	}
	    	IJ.log(line);
	    }
	}
	
}
