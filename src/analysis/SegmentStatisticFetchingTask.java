package analysis;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ForkJoinTask;

import stats.NucleusStatistic;
import stats.PlottableStatistic;
import components.generic.BorderTag;
import components.generic.MeasurementScale;
import components.generic.ProfileType;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;

@SuppressWarnings("serial")
public class SegmentStatisticFetchingTask extends AbstractStatisticFetchingTask {

	private final UUID id;
	
	public SegmentStatisticFetchingTask(Nucleus[] nuclei, PlottableStatistic stat, MeasurementScale scale, UUID id){
		this(nuclei, stat, scale, id, 0,nuclei.length );

	}

	protected SegmentStatisticFetchingTask(Nucleus[] nuclei, PlottableStatistic stat, MeasurementScale scale, UUID id, int low, int high) {
		super(nuclei, stat, scale, low, high);
		this.id = id;
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

			List<SegmentStatisticFetchingTask> tasks = new ArrayList<SegmentStatisticFetchingTask>();

			try {
				SegmentStatisticFetchingTask task1 = new SegmentStatisticFetchingTask(nuclei, stat, scale, id, low, mid);


				SegmentStatisticFetchingTask task2 = new SegmentStatisticFetchingTask(nuclei, stat, scale, id, mid, high);

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
			
			
			NucleusBorderSegment segment = nuclei[j].getProfile(ProfileType.REGULAR, BorderTag.REFERENCE_POINT).getSegment(id);

			  double perimeterLength = 0;
			  if(segment!=null){
				  int indexLength = segment.length();
				  double fractionOfPerimeter = (double) indexLength / (double) segment.getTotalLength();
				  perimeterLength = fractionOfPerimeter * nuclei[j].getStatistic(NucleusStatistic.PERIMETER, scale);
			  }
			  result[i] = perimeterLength;
		}

		return result;


	}



}

