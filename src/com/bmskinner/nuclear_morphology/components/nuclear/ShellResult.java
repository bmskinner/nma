/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.components.nuclear;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.logging.Loggable;

@Deprecated
public class ShellResult implements Serializable, IShellResult {

    private static final long serialVersionUID = 1L;
    private List<Double>      means;                // percent signal pre-DAPI
                                                    // normalisation
    private List<Double>      stderrs;
    private double            chisquare;
    private double            pvalue;

    private List<Integer> counts;
    private List<Double>  normalisedMeans; // percent signal post-DAPI
                                           // normalisation

    public ShellResult(List<Double> means, List<Double> stderrs) {
        this(means, stderrs, 0, 0);
    }

    public ShellResult(List<Double> means, List<Double> stderrs, double chi, double pvalue) {
        this.means = means;
        this.stderrs = stderrs;
        this.chisquare = chi;
        this.pvalue = pvalue;
        this.counts = new ArrayList<Integer>();
        this.normalisedMeans = new ArrayList<Double>();
    }

    public ShellResult(ShellResult s) {
        if (s == null) {
            throw new IllegalArgumentException("Template shell result is null");
        }
        this.means = new ArrayList<Double>();
        for (Double d : s.means) {
            means.add(d.doubleValue());
        }

        this.stderrs = new ArrayList<Double>();
        for (Double d : s.stderrs) {
            stderrs.add(d.doubleValue());
        }

        this.chisquare = s.chisquare;
        this.pvalue = s.pvalue;

        for (int d : s.counts) {
            counts.add(d);
        }

        for (double d : s.normalisedMeans) {
            normalisedMeans.add(d);
        }

    }

    public void setCounts(List<Integer> counts) {
        this.counts = counts;
    }

    public void setNormalisedMeans(List<Double> means) {
        this.normalisedMeans = means;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IShellResult#getMeans()
     */
    private double[] getRawMeans(CountType type, Aggregation agg) {
    	double[] arr = new double[means.size()];
    	for(int i=0; i<arr.length; i++) {
    		arr[i] = means.get(i);
    	}
        return arr;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IShellResult#getNormalisedMeans()
     */
    private double[] getNormalisedMeans(CountType type, Aggregation agg) {
    	double[] arr = new double[normalisedMeans.size()];
    	for(int i=0; i<arr.length; i++) {
    		arr[i] = normalisedMeans.get(i);
    	}
        return arr;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IShellResult#getStandardErrors()
     */
//    @Override
//    public List<Double> getRawStandardErrors(CountType type) {
//        return this.stderrs;
//    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IShellResult#getChiSquare()
     */
    private double getRawChiSquare(CountType type) {
        return this.chisquare;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IShellResult#getPValue()
     */
    private double getRawPValue(CountType type) {
        return this.pvalue;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IShellResult#getNumberOfShells()
     */
    @Override
    public int getNumberOfShells() {
        return this.means.size();
    }
    
    @Override
    public ShrinkType getType() {
        return ShrinkType.AREA;
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        if (counts == null) {
            counts = new ArrayList<Integer>();
        }

        if (normalisedMeans == null) {
            normalisedMeans = new ArrayList<Double>();
        }
    }

	@Override
	public double[] getProportions(@NonNull Aggregation agg, @NonNull Normalisation norm) {
		switch(norm) {
		case DAPI: return normalisedMeans.stream().mapToDouble(d->d.doubleValue()).toArray();
		case NONE: return means.stream().mapToDouble(d->d.doubleValue()).toArray();
		default: return means.stream().mapToDouble(d->d.doubleValue()).toArray();			
		}
		
	}

	@Override
	public double[] getStdErrs(@NonNull Aggregation agg, @NonNull Normalisation norm) {
		return stderrs.stream().mapToDouble(d->d.doubleValue()).toArray();			
	}

//	@Override
//	public double getChiSquareValue(@NonNull Aggregation agg, @NonNull Normalisation norm, @NonNull IShellResult expected) {
//		return chisquare;
//	}
//
//	@Override
//	public double getPValue(@NonNull Aggregation agg, @NonNull Normalisation norm, @NonNull IShellResult expected) {
//		return pvalue;
//	}

	@Override
	public double getOverallShell(@NonNull Aggregation agg, @NonNull Normalisation norm) {
		return 0;
	}
	
	@Override
    public long[] getPixelValues(@NonNull CountType type, @NonNull ICell cell, @NonNull Nucleus nucleus,
            @Nullable INuclearSignal signal) {
        long[] result = new long[means.size()];
        Arrays.fill(result, 0);
        return result;
    }

	@Override
	public long[] getAggregateCounts(@NonNull Aggregation agg, @NonNull Normalisation norm) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNumberOfSignals(@NonNull Aggregation agg) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public IShellResult duplicate() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double[] getProportions(@NonNull CountType type, @NonNull ICell cell, @NonNull Nucleus nucleus,
			@Nullable INuclearSignal signal) {
		// TODO Auto-generated method stub
		return null;
	}
}
