package analysis.nucleus;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import analysis.AbstractProgressAction;
import analysis.nucleus.DatasetSegmenter.MorphologyAnalysisMode;
//import analysis.nucleus.DatasetSegmenter.SegmentFitter;
import components.generic.BorderTag;
import components.generic.Profile;
import components.generic.ProfileCollection;
import components.generic.ProfileType;
import components.generic.SegmentedProfile;
import components.nuclei.Nucleus;

@SuppressWarnings("serial")
public class SegmentRecombiningTask extends AbstractProgressAction  {
	
	SegmentFitter fitter;
	SegmentedProfile medianProfile;
	final int low, high;
	final Nucleus[] nuclei;
	private static final int THRESHOLD = 30;
	private final ProfileCollection pc;
	
	public SegmentRecombiningTask(SegmentedProfile medianProfile, ProfileCollection pc, Nucleus[] nuclei, int low, int high) throws Exception{
		
//		DatasetSegmenter segmenter = new DatasetSegmenter(null, MorphologyAnalysisMode.NEW, null);
		fitter = new SegmentFitter(medianProfile);
		this.low = low;
		this.high = high;
		this.nuclei = nuclei;
		this.pc = pc;
		this.medianProfile = medianProfile;
	}
	
	public SegmentRecombiningTask(SegmentedProfile medianProfile, ProfileCollection pc, Nucleus[] nuclei) throws Exception{
		this(medianProfile, pc, nuclei, 0, nuclei.length);
	}

	protected void compute() {
	     if (high - low < THRESHOLD)
			try {
				processNuclei(low, high);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	     else {
	    	 int mid = (low + high) >>> 1;

	    	 List<SegmentRecombiningTask> tasks = new ArrayList<SegmentRecombiningTask>();
	    	 SegmentRecombiningTask task1;
	    	 try {
	    		 task1 = new SegmentRecombiningTask(medianProfile, pc, nuclei, low, mid);

	    		 task1.addProgressListener(this);


	    		 SegmentRecombiningTask task2 = new SegmentRecombiningTask(medianProfile, pc, nuclei, mid, high);
	    		 task2.addProgressListener(this);

	    		 tasks.add(task1);
	    		 tasks.add(task2);

	    		 this.invokeAll(tasks);
	    	 } catch (Exception e) {
	    		 // TODO Auto-generated catch block
	    		 e.printStackTrace();
	    	 }

	     }
	}
	
	private void processNuclei(int lo, int hi) throws Exception {
		
		for(int i=low; i<high; i++){
			processNucleus(nuclei[i]);
			fireProgressEvent();
		}
		
	}
	
	private void processNucleus(Nucleus n) throws Exception{
//		int count = 1;
//		for(Nucleus n : collection.getNuclei()){ 
//			log(Level.FINER, "Fitting nucleus "+n.getPathAndNumber()+" ("+count+" of "+collection.cellCount()+")");
			fitter.fit(n, pc);

			// recombine the segments at the lengths of the median profile segments
			Profile recombinedProfile = fitter.recombine(n, BorderTag.REFERENCE_POINT);
			try{
				n.setProfile(ProfileType.FRANKEN, new SegmentedProfile(recombinedProfile));
			} catch(Exception e){
//				log(Level.SEVERE, recombinedProfile.toString());
//				logError("Error setting nucleus profile", e);
				throw new Exception("Error setting nucleus profile");
			}
			
//			frankenProfiles.add(recombinedProfile);
//			count++;
//			publish(++progressCount); // publish the progress to gui
//		}
	}
	

}
