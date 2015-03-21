/**
 * 
 */
package no.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	
	public List<Double> getMedians(){
		List<Double> result = new ArrayList<Double>(0);
		for(int i=0;i<numberOfShells;i++){
			result.add(calculateMedianOfShell(i));
		}
		return result;
	}
	
	private double calculateMedianOfShell(int shell){
		List<Double> values = shellValues.get(shell);
		Double[] array = values.toArray(new Double[values.size()]);
		return NuclearOrganisationUtility.quartile(array, 50);
	}

}
