/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.analysis.profiles;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.bmskinner.nuclear_morphology.analysis.AbstractProgressAction;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.IProfileCollection;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.SegmentedFloatProfile;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;

/**
 * This class divdes segment fitting amongst the nuclei in a dataset
 * 
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class SegmentRecombiningTask extends AbstractProgressAction {

    private final SegmentFitter      fitter;
    private final ISegmentedProfile  medianProfile;
    private final int                low, high;
    private final Nucleus[]          nuclei;
    private static final int         THRESHOLD = 30; // The number of nuclei to
                                                     // split the task on
    private final IProfileCollection pc;

    private SegmentRecombiningTask(ISegmentedProfile medianProfile, IProfileCollection pc, Nucleus[] nuclei, int low,
            int high) throws Exception {

        this.fitter = new SegmentFitter(medianProfile);
        this.low = low;
        this.high = high;
        this.nuclei = nuclei;
        this.pc = pc;
        this.medianProfile = medianProfile;
    }

    public SegmentRecombiningTask(ISegmentedProfile medianProfile, IProfileCollection pc, Nucleus[] nuclei)
            throws Exception {
        this(medianProfile, pc, nuclei, 0, nuclei.length);
    }

    protected void compute() {
        if (high - low < THRESHOLD) {

            try {
                processNuclei();
            } catch (Exception e) {
                error("Error processing nuclei", e);
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
                error("Error dividing task", e);
            }

        }
    }

    private void processNuclei() throws Exception {

        for (int i = low; i < high; i++) {
            processNucleus(nuclei[i]);
            fireProgressEvent();
        }
    }

    private void processNucleus(Nucleus n) throws Exception {

        if (n.isLocked()) {
            finest(n.getNameAndNumber() + " is locked, skipping");
            return;
        }

        fitter.fit(n, pc);

        // recombine the segments to the lengths of the median profile segments

        IProfile recombinedProfile = fitter.recombine(n, Tag.REFERENCE_POINT);

        ISegmentedProfile segmented = new SegmentedFloatProfile(recombinedProfile, medianProfile.getOrderedSegments());
        n.setProfile(ProfileType.FRANKEN, segmented);

        // n.log("Recombined segments:");
        // n.log(segmented.toString());

//        log(Level.FINEST, "Recombined segments for nucleus " + n.getNameAndNumber());
//        log(Level.FINEST, segmented.toString());
    }

}
