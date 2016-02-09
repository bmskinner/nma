package analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinTask;

import stats.PlottableStatistic;
import components.generic.MeasurementScale;
import components.nuclei.Nucleus;

@SuppressWarnings("serial")
public class NucleusStatisticFetchingTask extends AbstractStatisticFetchingTask {
	
	public NucleusStatisticFetchingTask(Nucleus[] nuclei, PlottableStatistic stat, MeasurementScale scale){
		this(nuclei, stat, scale, 0,nuclei.length );
	}

	protected NucleusStatisticFetchingTask(Nucleus[] nuclei, PlottableStatistic stat, MeasurementScale scale, int low, int high) {
		super(nuclei, stat, scale, low, high );
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
