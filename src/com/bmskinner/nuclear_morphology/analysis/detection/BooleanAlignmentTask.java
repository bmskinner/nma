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


package com.bmskinner.nuclear_morphology.analysis.detection;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinTask;

import com.bmskinner.nuclear_morphology.analysis.AbstractProgressAction;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;

@SuppressWarnings("serial")
public class BooleanAlignmentTask extends AbstractProgressAction {

    final BooleanAligner     aligner;
    final Mask               reference;
    final int                low , high;
    final Nucleus[]          nuclei;
    private static final int THRESHOLD = 30;

    protected BooleanAlignmentTask(Mask reference, Nucleus[] nuclei, int low, int high) 
            throws Exception {

        this.reference = reference;
        this.low = low;
        this.high = high;
        this.nuclei = nuclei;
        this.aligner = new BooleanAligner(reference);
    }

    public BooleanAlignmentTask(Mask reference, Nucleus[] nuclei) throws Exception {
        this(reference, nuclei, 0, nuclei.length);
    }

    protected void compute() {
        if (high - low < THRESHOLD){
            try {
                processNuclei();
            } catch (Exception e) {
                warn("Error processing nuclei");
                stack("Error processing nuclei", e);
            }
        } else {
            int mid = (low + high) >>> 1;

            List<BooleanAlignmentTask> tasks = new ArrayList<BooleanAlignmentTask>();

            try {

                BooleanAlignmentTask task1 = new BooleanAlignmentTask(reference, nuclei, low, mid);
                BooleanAlignmentTask task2 = new BooleanAlignmentTask(reference, nuclei, mid, high);

                task1.addProgressListener(this);
                task2.addProgressListener(this);

                tasks.add(task1);
                tasks.add(task2);

                ForkJoinTask.invokeAll(tasks);
            } catch (Exception e) {
                warn("Error processing nuclei");
                stack("Error processing nuclei", e);
            }

        }
    }

    /**
     * From the calculated median profile segments, assign segments to each
     * nucleus based on the best offset fit of the start and end indexes.
     */
    private void processNuclei() throws Exception {

        for (int i = low; i < high; i++) {
            Nucleus verticalNucleus = nuclei[i].getVerticallyRotatedNucleus();
            Mask test = verticalNucleus.getBooleanMask(200, 200);
            int[] offsets = aligner.align(test);
            verticalNucleus.moveCentreOfMass(IPoint.makeNew(offsets[1], offsets[0]));
            fireProgressEvent();
        }

    }

}
