package utility;

import ij.measure.CurveFitter;

// carry out cubic interpolations on curves
public class Interpolator {
	
//	private String formula;
	private double[] parameters;
		
	public Interpolator(double[] xvalues, double[] yvalues) {
		
		if(xvalues==null || yvalues == null){
			throw new IllegalArgumentException("Arrays are null or empty");
		}
		if(xvalues.length!=yvalues.length){
			throw new IllegalArgumentException("Array lengths do not match");
		}
		CurveFitter fitter = new CurveFitter(xvalues, yvalues);
		fitter.doFit(CurveFitter.POLY3);
//		this.formula = fitter.getFormula();
		this.parameters = fitter.getParams();
	}

	public double find(double xToFind){
		
		double y = 0;
		
		for(int i=0;i<parameters.length;i++){
			double param = parameters[i];
			y+= Math.pow(xToFind, i) * param;
		}
		return y;
	}

}
