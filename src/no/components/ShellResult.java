package no.components;

import java.io.Serializable;
import java.util.List;

public class ShellResult implements Serializable {

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
}
