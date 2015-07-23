package no.components;

import ij.IJ;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import utility.Stats;
import utility.Utils;
import no.exceptions.ProfileException;

public class ProfileAggregate implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Map<Double, Collection<Double>> aggregate = new HashMap<Double, Collection<Double>>();
	private double  profileIncrement;
	private int length;
	private double[] xPositions;
	
	public ProfileAggregate(int length){
		
		this.length = length;
		this.profileIncrement = (double) 100 / (double) length;
		
		xPositions = new double[length];
		double x = 0;
		for(int i=0;i<length;i++){
			xPositions[i] = x;
			x += profileIncrement;
		}
	}
		
	/*
    We need to calculate the median angle profile. This requires binning the normalised profiles
    into bins of size PROFILE_INCREMENT to generate a table such as this:
          k   0.0   0.5   1.0   1.5   2.0 ... 99.5   <- normalised profile bins
    NUCLEUS1  180   185  170    130   120 ... 50     <- angle within those bins
    NUCLEUS2  180   185  170    130   120 ... 50

    The median of each bin can then be calculated. 
    Depending on the length of the profile arrays and the chosen increment, there may
    be >1 or <1 angle within each bin for any given nucleus. We rely on large numbers of 
    nuclei to average this problem away; further methods interpolate values from surrounding
    bins to plug any holes left over

    The data are stored as a Map<Double, Collection<Double>>
    PROFILE_INCREMENT is 100 / the median array length. This ensures > 1 entry for each bin, while not
    pooling too many entries.
  */

	public void addValues(Profile yvalues){
		
		// normalise the profile to the correct x positions
		double[] xvalues = new double[yvalues.size()];
		for(int i=0;i<xvalues.length;i++){
			xvalues[i] = ( (double)i / (double)xvalues.length ) * 100;
		}
				 
		for(int i=0;i<xvalues.length;i++){  // the positions in the input profile
			
			double testX = xvalues[i];

			for(int j=0;j<this.length;j++){ // cover all the bin positions across the profile
				
				double x = xPositions[j]; // the bin to fill
				double maxX = x+profileIncrement;
				
				if( testX >= x && testX < maxX){ // profile xvalue is in bin

					Collection<Double> values = aggregate.get(x);

					if (values==null) { // this this profile increment has not yet been encountered, create it
						values = new ArrayList<Double>();
						aggregate.put(x, values);
					}
					values.add(yvalues.get(i));
					break;
				}
			}
		}        
	}

	/**
	 * Get the x-axis positions of the centre of each bin.
	 * @return the Profile of positions
	 */
	public Profile getXPositions(){
		double[] result = new double[length];
		double x = -this.profileIncrement/2;
		for(int i=0;i<length;i++){
			x += profileIncrement;
			result[i] = x;
		}
		return new Profile(result);
	}

	public Profile getNumberOfPoints(){
		double[] result = new double[length];

		for(int i=0;i<length;i++){
			double x = xPositions[i];
			result[i] = aggregate.containsKey(x) ? aggregate.get(x).size() : 0;
		}
		return new Profile(result);
	}
	
	public Profile getMedian(){
		Profile result = new Profile(new double[0]);
		try{
			result = calculateQuartile(50);
		} catch(ProfileException e){
			// if the profile >200, scale down to 200. Otherwise, reduce stepwise until we get a profile
			int newLength = this.length <= 200 ? this.length-5 : 200;
			IJ.log("    "+e.getMessage()+": rescaling to "+newLength);
			this.rescaleProfile(newLength);
			result = this.getMedian(); // recurse through this function until we get a profile
		}
		return result;
	}

	public Profile getQuartile(double quartile){
		Profile result = new Profile(new double[0]);
		try{
			result = calculateQuartile(quartile);
		} catch(ProfileException e){
			result = this.getMedian(); // median is the only method that rescales, so should have been called before this
//			IJ.log("    Cannot find quartile; falling back on median");
		}
		return result;
	}
	
	private Profile calculateQuartile(double quartile) throws ProfileException{
		Double[] medians = new Double[this.length];
		int missing = 0;

		for(int i=0;i<this.length;i++){
			double x = xPositions[i];

			try{
				if(aggregate.containsKey(x)){
					Collection<Double> values = aggregate.get(x);
					Double[] d = values.toArray(new Double[0]);

					double median = Stats.quartile(d, quartile);

					medians[i] = median;
				} else {
					medians[i] = Double.NaN;
					missing++;
				}
			} catch(Exception e){
//				IJ.log("    Index "+i+": Cannot calculate median for "+x);
				medians[i] = Double.NaN;
			}
		}
		if(missing>(double)length/4){
			throw new ProfileException("    Too many missing values ("+missing+") to calculate median profile");
		}

		Profile result = repairProfile(medians);
		return result;
	}
	
	private Profile repairProfile(Double[] array) throws ProfileException{
		
		for(int i=0;i<array.length;i++){
			if(array[i].isNaN()){
				
				int replacementIndex = Utils.wrapIndex(i+1, array.length);
				if(!array[replacementIndex].isNaN()){
					array[i] = array[replacementIndex];
				} else{
					replacementIndex = Utils.wrapIndex(i-1, array.length);
					if(!array[replacementIndex].isNaN()){
						array[i] = array[replacementIndex];
					} else {
						throw new ProfileException("Unable to repair median profile");
					}
				}	
			}
		}
		return new Profile(Utils.getdoubleFromDouble(array));
	}
	
	// this is for low count profiles, to get some kind of median even if poor
	private void rescaleProfile(int newLength){
		
		double increment = (double) 100 / (double) newLength;
		double newX = 0;
		
		Map<Double, Collection<Double>> rescaled = new HashMap<Double, Collection<Double>>();

		// go through each new position
		for(int i = 0; i<newLength; i++){
			
			// Go through each old position. Find the points that overlap
			for(int j=0;j<length;j++){
				double oldX = xPositions[j];
				if(oldX>=newX && oldX < newX+increment){ // the old x is in the new bin
					// overlap; get all the values into the new bin
					if(aggregate.containsKey(oldX)){
						Collection<Double> oldValues = aggregate.get(oldX);
						Collection<Double> newValues = rescaled.get(newX);
						
						if (newValues==null) { // this this profile increment has not yet been encountered, create it
							newValues = new ArrayList<Double>();
							rescaled.put(newX, newValues);
						}
						for(double d : oldValues){
							newValues.add(d);
						}
					}
				}
			}
			newX += increment; 
		}
		
		this.aggregate = rescaled;
		this.length = newLength;
		this.profileIncrement = increment;
		this.xPositions = new double[newLength];
		double x = 0;
		for(int i=0;i<length;i++){
			this.xPositions[i] = x;
			x += profileIncrement;
		}
	}

//	public void print(){
//		IJ.log("Printing profile aggregate: length: "+this.length+" inc: "+this.profileIncrement);
//		for(int i=0;i<length;i++){
//			double x = xPositions[i];
//
//			try{
//				if(aggregate.containsKey(x)){
//					Collection<Double> values = aggregate.get(x);
//					String line = "    "+x+"\t";
//					for(Double d : values){
//						line += d+"\t";
//					}
//					IJ.log(line);
//				} else {
//					IJ.log(x+"\t");
//				}
//			} catch(Exception e){
//				IJ.log("    Error printing x: "+x);
//			}
//		}
//		IJ.log("");
//	}
	
}
