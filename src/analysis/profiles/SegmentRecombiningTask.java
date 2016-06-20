/*******************************************************************************
 *  	Copyright (C) 2015, 2016 Ben Skinner
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
 *     GNU General Public License for more details. Gluten-free. May contain 
 *     traces of LDL asbestos. Avoid children using heavy machinery while under the
 *     influence of alcohol.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package analysis.profiles;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import analysis.AbstractProgressAction;
//import analysis.nucleus.DatasetSegmenter.SegmentFitter;
import components.generic.BorderTag;
import components.generic.Profile;
import components.generic.ProfileCollection;
import components.generic.ProfileType;
import components.generic.SegmentedProfile;
import components.nuclei.Nucleus;

/**
 * This class divdes segment fitting amongst the nuclei in a dataset
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class SegmentRecombiningTask extends AbstractProgressAction  {
	
	private final SegmentFitter fitter;
	private final SegmentedProfile medianProfile;
	private final int low, high;
	private final Nucleus[] nuclei;
	private static final int THRESHOLD = 30; // The number of nuclei to split the task on
	private final ProfileCollection pc;
	
	private SegmentRecombiningTask(SegmentedProfile medianProfile, ProfileCollection pc, Nucleus[] nuclei, int low, int high) throws Exception{
		
		this.fitter        = new SegmentFitter(medianProfile);
		this.low           = low;
		this.high          = high;
		this.nuclei        = nuclei;
		this.pc            = pc;
		this.medianProfile = medianProfile;
	}
	
	public SegmentRecombiningTask(SegmentedProfile medianProfile, ProfileCollection pc, Nucleus[] nuclei) throws Exception{
		this(medianProfile, pc, nuclei, 0, nuclei.length);
	}

	protected void compute() {
		if (high - low < THRESHOLD){
			
			try {
				processNuclei();
			} catch (Exception e) {
				logError("Error processing nuclei" , e);
			}
			
		} else {
			int mid = (low + high) >>> 1;

			List<SegmentRecombiningTask> tasks = new ArrayList<SegmentRecombiningTask>();

			try {
				SegmentRecombiningTask task1 = new SegmentRecombiningTask(medianProfile, pc, nuclei, low, mid);
				SegmentRecombiningTask task2 = new SegmentRecombiningTask(medianProfile, pc, nuclei, mid, high);
				
				task1.addProgressListener(this);
				task2.addProgressListener(this);

				tasks.add(task1);
				tasks.add(task2);

				SegmentRecombiningTask.invokeAll(tasks);
				
			} catch (Exception e) {
				logError("Error dividing task" , e);
			}

		}
	}
		
	private void processNuclei() throws Exception {
		
		for(int i=low; i<high; i++){
			try {
				processNucleus(nuclei[i]);
			} catch(Exception e){
				// On error, dump the nucleus logs
//				log(Level.SEVERE, nuclei[i].printLog());
				throw e;
			}
			fireProgressEvent();
		}
		
	}
	
	private void processNucleus(Nucleus n) throws Exception {
		log(Level.FINEST, "Recombining segments for nucleus "+n.getNameAndNumber());
//		n.log("Recombining segments");
		fitter.fit(n, pc);

		// recombine the segments to the lengths of the median profile segments

		Profile recombinedProfile = fitter.recombine(n, BorderTag.REFERENCE_POINT);

		SegmentedProfile segmented = new SegmentedProfile(recombinedProfile, medianProfile.getOrderedSegments());
		n.setProfile(ProfileType.FRANKEN, segmented);
		
//		n.log("Recombined segments:");
//		n.log(segmented.toString());
		
		log(Level.FINEST, "Recombined segments for nucleus "+n.getNameAndNumber());
		log(Level.FINEST, segmented.toString());
	}
	

}
