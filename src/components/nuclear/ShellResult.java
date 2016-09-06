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
package components.nuclear;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import logging.Loggable;

public class ShellResult implements Serializable, Loggable {

	private static final long serialVersionUID = 1L;
	private List<Double> means;
	private List<Double> stderrs;
	private double chisquare;
	private double pvalue;
	
	public ShellResult(List<Double> means, List<Double> stderrs){
		this.means = means;
		this.stderrs = stderrs;
	}
	
	public ShellResult(List<Double> means, List<Double> stderrs, double chi, double pvalue){
		this.means = means;
		this.stderrs = stderrs;
		this.chisquare = chi;
		this.pvalue = pvalue;
	}
	
    public ShellResult(ShellResult s){
        if(s==null){
            throw new IllegalArgumentException("Template shell result is null");
        }
        this.means = new ArrayList<Double>();
        for(Double d : s.means){
            means.add(d.doubleValue());
        }
        
        this.stderrs = new ArrayList<Double>();
        for(Double d : s.stderrs){
            stderrs.add(d.doubleValue());
        }
        
        this.chisquare = s.chisquare;
        this.pvalue    = s.pvalue;
    }
    

	
	public List<Double> getMeans(){
		return this.means;
	}
	
	public List<Double> getStandardErrors(){
		return this.stderrs;
	}
	
	public double getChiSquare(){
		return this.chisquare;
	}
	
	public double getPValue(){
		return this.pvalue;
	}
	
	public int getNumberOfShells(){
		return this.means.size();
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
//		finest("\tReading shell result");
		in.defaultReadObject();
//		finest("\tRead shell result");
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
//		finest("\tWriting shell result");
		out.defaultWriteObject();
//		finest("\tWrote shell result");
	}
}
