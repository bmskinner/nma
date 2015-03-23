package no.components;

import ij.IJ;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import no.utility.Stats;
import no.utility.Utils;

public class ProfileAggregate {
	
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

	public void addValues(double[] xvalues, double[] yvalues){

		for(int i=0;i<length;i++){ // cover all the bin positions across the profile

			double x = xPositions[i];
			for(int j=0;j<xvalues.length;j++){

				if( xvalues[j] >= x && xvalues[j] < x+profileIncrement){

					Collection<Double> values = aggregate.get(x);

					if (values==null) { // this this profile increment has not yet been encountered, create it
						values = new ArrayList<Double>();
						aggregate.put(x, values);
					}
					values.add(yvalues[j]);
				}
			}
		}        
	}

	public Profile getXPositions(){
		return new Profile(xPositions);
	}

	public Profile getMedian(){
		return new Profile(getQuartile(50));
	}

	public Profile getQuartile(double quartile){

		Double[] medians = new Double[length];

		for(int i=0;i<length;i++){
			double x = xPositions[i];

			try{
				Collection<Double> values = aggregate.get(x);

				if(values.size()> 0){
					Double[] d = values.toArray(new Double[0]);

					Arrays.sort(d);
					double median = Stats.quartile(d, quartile);

					medians[i] = median;
				}
			} catch(Exception e){
				IJ.log("Cannot calculate median for "+x+": "+e.getMessage());
				medians[i] = 0.0;
			}
		}

		for(int i=0;i<medians.length;i++){
			if(medians[i] == 0){
				int replacementIndex = 0;

				if(i<1)
					replacementIndex = i+1;
				if(i>99)
					replacementIndex = i-1;

				medians[i] = medians[replacementIndex];

				IJ.log("\tRepaired medians at "+i+" with values from  "+replacementIndex);
			}
		}
		return new Profile(Utils.getdoubleFromDouble(medians));
	}


}
