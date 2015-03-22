/**
 * 
 */
package no.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.inference.ChiSquareTest;

import no.utility.NuclearOrganisationUtility;

/**
 *
 */
public class ShellCounter {
	
	int numberOfShells;
	Map<Integer, ArrayList<Double>> shellValues = new HashMap<Integer, ArrayList<Double>>(0); // store the values
	
	public ShellCounter(int numberOfShells){
		this.numberOfShells = numberOfShells;
		for(int i=0;i<numberOfShells;i++){
			shellValues.put(i, new ArrayList<Double>(0));
		}
	}
	
	public void addValues(double[] values) throws Exception{
		if(values.length!=this.numberOfShells){
			throw new Exception("Input array is wrong size");
		}
		for(int i=0;i<numberOfShells;i++){
			List<Double> shell = shellValues.get(i);
			shell.add(values[i]);
		}
	}
	
	private List<Double> getQuartiles(double quartile){
		List<Double> result = new ArrayList<Double>(0);
		for(int i=0;i<numberOfShells;i++){
			result.add(calculateQuartileOfShell(i, quartile));
		}
		return result;
	}
	
	public void export(File f){
		if(f.exists()){
			f.delete();
		}
		
		// export the median and IQR for each shell, plus any stats, plus charts
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
		List<Double> medians = getQuartiles(50);
		for(int i=0;i<numberOfShells; i++){
			observed[i] = medians.get(i).longValue();
		}
		return observed;
	}
	
 	private double[] getExpected(){
		double[] expected = new double[numberOfShells];
		for(int i=0;i<numberOfShells; i++){
			expected[i] = (double)1/(double)numberOfShells;
		}
		return expected;
	}
	
	private double calculateQuartileOfShell(int shell, double quartile){
		List<Double> values = shellValues.get(shell);
		Double[] array = values.toArray(new Double[values.size()]);
		return NuclearOrganisationUtility.quartile(array, quartile);
	}
	
}
