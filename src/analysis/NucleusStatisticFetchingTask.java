package analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

import stats.NucleusStatistic;
import components.generic.MeasurementScale;
import components.nuclei.Nucleus;

@SuppressWarnings("serial")
public class NucleusStatisticFetchingTask extends RecursiveTask<double[]>{

	
	final int low, high;
	final Nucleus[] nuclei;
	private static final int THRESHOLD = 30;
	final NucleusStatistic stat;
	final MeasurementScale scale;
	
	public NucleusStatisticFetchingTask(Nucleus[] nuclei, NucleusStatistic stat, MeasurementScale scale){
		this(nuclei, stat, scale, 0,nuclei.length );
	}
	
	
	
	protected NucleusStatisticFetchingTask(Nucleus[] nuclei, NucleusStatistic stat, MeasurementScale scale, int low, int high) {
		this.low = low;
		this.high = high;
		this.nuclei = nuclei;
		this.stat =stat;
		this.scale = scale;

	}
	
	@Override
	protected double[] compute() {
		double[] result = new double[0];
		
		 if (high - low < THRESHOLD)
				try {

					result = getStatistics();
					return result;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		     else {
		    	 int mid = (low + high) >>> 1;

		    	 List<NucleusStatisticFetchingTask> tasks = new ArrayList<NucleusStatisticFetchingTask>();
		    	 
		    	 try {
		    		 NucleusStatisticFetchingTask task1 = new NucleusStatisticFetchingTask(nuclei, stat, scale, low, mid);


		    		 NucleusStatisticFetchingTask task2 = new NucleusStatisticFetchingTask(nuclei, stat, scale, mid, high);

		    		 tasks.add(task1);
		    		 tasks.add(task2);

		    		 
		    		 ForkJoinTask.invokeAll(tasks);
		    		 
		    		 result = concat(task1.join(), task2.join());
		    		 return result;
		    		 
		    	 } catch (Exception e) {
		    		 // TODO Auto-generated catch block
		    		 e.printStackTrace();
		    	 }

		     }
		 return result;
	}
	
	public double[] concat(double[] a, double[] b) {
		   int aLen = a.length;
		   int bLen = b.length;
		   double[] c= new double[aLen+bLen];
		   System.arraycopy(a, 0, c, 0, aLen);
		   System.arraycopy(b, 0, c, aLen, bLen);
		   return c;
		}
	
	  /**
	   * Get the stats of the nuclei in this collection as
	   * an array
	   * @return
	 * @throws Exception 
	   */
	  private double[] getStatistics() throws Exception{

		  double[] result = new double[high-low];

		  for(int i=0, j=low; j<high; i++, j++){
			  result[i] = nuclei[j].getStatistic(stat, scale);
		  }

		  return result;

	  }
	
	

}
