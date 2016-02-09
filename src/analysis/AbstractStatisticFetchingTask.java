package analysis;

import java.util.concurrent.RecursiveTask;

import stats.PlottableStatistic;
import components.generic.MeasurementScale;
import components.nuclei.Nucleus;

@SuppressWarnings("serial")
public abstract class AbstractStatisticFetchingTask extends RecursiveTask<double[]>{
	
	final int low, high;
	final Nucleus[] nuclei;
	public static final int THRESHOLD = 30;
	final PlottableStatistic stat;
	final MeasurementScale scale;
	
	public AbstractStatisticFetchingTask(Nucleus[] nuclei, PlottableStatistic stat, MeasurementScale scale){
		this(nuclei, stat, scale, 0,nuclei.length );
	}
	
	protected AbstractStatisticFetchingTask(Nucleus[] nuclei, PlottableStatistic stat, MeasurementScale scale, int low, int high) {
		this.low = low;
		this.high = high;
		this.nuclei = nuclei;
		this.stat =stat;
		this.scale = scale;
	}
	
	public double[] concat(double[] a, double[] b) {
		   int aLen = a.length;
		   int bLen = b.length;
		   double[] c= new double[aLen+bLen];
		   System.arraycopy(a, 0, c, 0, aLen);
		   System.arraycopy(b, 0, c, aLen, bLen);
		   return c;
		}

}
