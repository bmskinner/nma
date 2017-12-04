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


package com.bmskinner.nuclear_morphology.analysis.nucleus;

import com.bmskinner.nuclear_morphology.analysis.profiles.Profileable;
import com.bmskinner.nuclear_morphology.components.generic.FloatEquation;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.LineEquation;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;

/**
 * Test class to make a physical border match a given profile
 * 
 * @author bms41
 *
 */
public class ProfileMatchingMethod {

    public ProfileMatchingMethod() {

    }

    public void refold(Profileable c, IProfile target) throws Exception {

        IProfile p = c.getProfile(ProfileType.ANGLE);
        double epsilon = 0.5;

        int windowSize = c.getWindowSize(ProfileType.ANGLE);

        // Go through the border of the object, and alter the positions of
        // points where the mismatch is greatest

        IProfile iTarget = target.interpolate(p.size());
        int offset = p.getSlidingWindowOffset(iTarget);
        iTarget = iTarget.offset(-offset);

        for (int i = 0; i < p.size(); i++) {

            double t = iTarget.get(i);
            double n = p.get(i);
            double ex = Math.abs(t - n);

            if (ex > epsilon) {

                // need to adjust this point compared to windowsize points
                int prev = c.wrapIndex(i - windowSize);
                int next = c.wrapIndex(i + windowSize);

                /*
                 * Get the orthogonal line to prev and next points:
                 * 
                 * N / | \ / | \ PP--MP--NP
                 * 
                 * ^ Line oq
                 * 
                 * 
                 */

                IPoint pp = c.getBorderPoint(prev);
                IPoint np = c.getBorderPoint(next);
                IPoint mp = IPoint.getMidpoint(pp, np);

                LineEquation oq = new FloatEquation(pp, np).getPerpendicular(mp);

                // Move along the line until the profile matches at this point

                double maxDist = 1;
                double step = 0.1;
                double dist = -maxDist;
                double bestDist = 0;

                while (dist < maxDist) {

                    IPoint rp = oq.getPointOnLine(mp, dist);
                    double angle = rp.findAngle(np, pp);

                    if (Math.abs(t - angle) < ex) {
                        bestDist = dist;
                    }
                    dist += step;

                }
                IPoint rp = oq.getPointOnLine(mp, bestDist);
                c.updateBorderPoint(i, rp);

            }

        }

    }

}
