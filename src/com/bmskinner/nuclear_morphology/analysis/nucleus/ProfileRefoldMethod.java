/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.analysis.nucleus;

import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.UnavailableBorderPointException;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclei.Consensus;
import com.bmskinner.nuclear_morphology.components.nuclei.DefaultConsensusNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.profiles.IProfile;
import com.bmskinner.nuclear_morphology.components.profiles.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.profiles.UnprofilableObjectException;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * The method that refolds a median angle profile into a shape.
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public class ProfileRefoldMethod extends SingleDatasetAnalysisMethod {
	
	private static final Logger LOGGER = Logger.getLogger(ProfileRefoldMethod.class.getName());
    
	private IProfile targetCurve;
	private Consensus refoldNucleus;
    private ICellCollection collection;

    private CurveRefoldingMode mode = CurveRefoldingMode.FAST;

    private int pointUpdateCounter = 0;

    public enum CurveRefoldingMode {

        FAST("Fast", 50), 
        INTENSIVE("Intensive", 1000), 
        BRUTAL("Brutal", 10000);

        private int    iterations;
        private String name;

        CurveRefoldingMode(String name, int iterations) {
            this.name = name;
            this.iterations = iterations;
        }

        @Override
		public String toString() {
            return this.name;
        }

        public int maxIterations() {
            return this.iterations;
        }
    }

    /**
     * Construct from a collection of cells and the mode of refolding
     * 
     * @param dataset the dataset to be refolded
     * @param refoldMode the type of refolding
     * @throws Exception
     */
    public ProfileRefoldMethod(@NonNull IAnalysisDataset dataset, @NonNull CurveRefoldingMode refoldMode) throws Exception {
        super(dataset);
        collection = dataset.getCollection();
        this.setMode(refoldMode);
    }

    @Override
    public IAnalysisResult call() throws Exception {
        run();
        return new DefaultAnalysisResult(dataset);
    }

    private void run() {

        try {
        	// make an entirely new nucleus to play with
            Nucleus n = collection.getNucleusMostSimilarToMedian(Landmark.REFERENCE_POINT);

            refoldNucleus = new DefaultConsensusNucleus(n);
            
            IProfile targetProfile = collection.getProfileCollection().getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT,
                    Stats.MEDIAN);
            IProfile q25 = collection.getProfileCollection().getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT,
                    Stats.LOWER_QUARTILE);
            IProfile q75 = collection.getProfileCollection().getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT,
                    Stats.UPPER_QUARTILE);

            if (targetProfile == null) {
                throw new Exception("Null reference to target profile");
            }
            if (q25 == null || q75 == null) {
                throw new Exception("Null reference to q25 or q75 profile");
            }

            targetCurve = targetProfile;
            refoldNucleus.moveCentreOfMass(IPoint.makeNew(0, 0));

            LOGGER.finer( "Result: template at " + refoldNucleus.getCentreOfMass());

            if (collection.size() > 1) 
                refoldCurve(); // carry out the refolding

            collection.setConsensus(refoldNucleus);

            LOGGER.fine("Updated " + pointUpdateCounter + " border points");

        } catch (Exception e) {
            LOGGER.warning("Unable to refold nucleus");
            LOGGER.log(Loggable.STACK, "Unable to refold nucleus", e);
        }
    }

    /*
     * The main function to be called externally; all other functions will hang
     * off this
     */
    public void refoldCurve() throws Exception {

        try {
            double score = refoldNucleus.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT)
                    .absoluteSquareDifference(targetCurve);

            LOGGER.fine("Refolding curve: initial score: " + (int) score);
            int i = 0;
            while (i < mode.maxIterations()) { // iterate until converging
                score = this.iterateOverNucleus();
                fireProgressEvent();
                LOGGER.fine("Iteration " + i + ": " + (int) score);
                i++;
            }
            LOGGER.fine("Refolded curve: final score: " + (int) score);

        } catch (Exception e) {
            throw new Exception("Cannot calculate scores: " + e.getMessage());
        }
    }

    public void setMode(CurveRefoldingMode s) {
        this.mode = s;
    }

    /**
     * Smooth jagged edges in the refold nucleus
     * 
     * @throws Exception
     */
//    private void smoothCurve(int offset) throws Exception {
//
//        // Get the median distance between each border point in the refold
//        // candidate nucleus.
//        // Use this to establish the max and min distances a point can migrate
//        // from its neighbours
//        double medianDistanceBetweenPoints = refoldNucleus.component().getMedianDistanceBetweenPoints();
//        double minDistance = medianDistanceBetweenPoints * 0.5;
//        double maxDistance = medianDistanceBetweenPoints * 1.2;
//
//        /*
//         * Draw a line between the next and previous point Move the point to the
//         * centre of the line Move ahead two points
//         * 
//         */
//        for (int i = offset; i < refoldNucleus.component().getBorderLength(); i += 2) {
//
//            IPoint thisPoint = refoldNucleus.component().getBorderPoint(i);
//            IPoint prevPoint = thisPoint.prevPoint();
//            IPoint nextPoint = thisPoint.nextPoint();
//
//            /*
//             * get the point o, half way between the previous point p and next
//             * point n
//             */
//
//            LineEquation eq = new DoubleEquation(prevPoint, nextPoint);
//            double distance = prevPoint.getLengthTo(nextPoint) / 2;
//            IPoint newPoint = eq.getPointOnLine(prevPoint, distance);
//
//            /*
//             * get the point r, half way between o and this point x
//             * This should smooth the curve without completely blunting corners
//             */
//            LineEquation eq2 = new DoubleEquation(newPoint, thisPoint);
//            double distance2 = newPoint.getLengthTo(thisPoint) / 2;
//            IPoint replacementPoint = eq2.getPointOnLine(newPoint, distance2);
//
//            boolean ok = checkPositionIsOK(newPoint, refoldNucleus.component(), i, minDistance, maxDistance);
//
//            if (ok) 
//                refoldNucleus.component().updateBorderPoint(i, replacementPoint.getX(), replacementPoint.getY());
//        }
//    }

    /*
     * Go over the target nucleus, adjusting each point. Keep the change if it
     * helps get closer to the target profile
     * 
     * Changes to make: Random mutation to the X and Y position. Must remain
     * within a certain range of neighbours
     */
    private double iterateOverNucleus() throws ProfileException, MissingLandmarkException,
            MissingProfileException, UnavailableBorderPointException {

        ISegmentedProfile refoldProfile = refoldNucleus.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT);

        // Get the difference between the candidate nucleus profile and the
        // median profile
        double similarityScore = refoldProfile.absoluteSquareDifference(targetCurve);

        // Get the median distance between each border point in the refold
        // candidate nucleus.
        // Use this to establish the max and min distances a point can migrate
        // from its neighbours
        // This is the 'habitable zone' a point can occupy
        double medianDistanceBetweenPoints = refoldNucleus.getMedianDistanceBetweenPoints();
        double minDistance = medianDistanceBetweenPoints * 0.5;
        double maxDistance = medianDistanceBetweenPoints * 1.2;

        // make all changes to a fresh nucleus before buggering up the real one
        LOGGER.finest( "Creating test nucleus based on refold candidate");

        Nucleus testNucleus;
        try {

            testNucleus = new DefaultConsensusNucleus(refoldNucleus);

            // When errors occur, the testNucleus CoM is offset at 0,-15 (may
            // not be constant).
            // Probably due to the correction in the DefaultConsensusNucleus
            // constructor.
            // Hence, put it back again to zero.
            testNucleus.moveCentreOfMass(IPoint.makeNew(0, 0));

            LOGGER.finer( "Test nucleus COM: " + testNucleus.getCentreOfMass());
            LOGGER.finest( "Beginning border tests");
            for (int i = 0; i < refoldNucleus.getBorderLength(); i++) {
                similarityScore = improveBorderPoint(i, minDistance, maxDistance, similarityScore, testNucleus);
            }

        } catch (Error e) {
            LOGGER.warning("Error making new consensus");
            LOGGER.log(Loggable.STACK, "Error in construction", e);
        } catch (UnprofilableObjectException e) {
            LOGGER.warning("Cannot create the test nucleus");
            LOGGER.log(Loggable.STACK, "Error in nucleus constructor", e);
        }

        testNucleus = null;
        return similarityScore;
    }

    /**
     * Try a random modification to the given border point position, and measure
     * the effect on the similarity score to the median profile
     * 
     * @param index
     * @param minDistance
     * @param maxDistance
     * @param similarityScore
     * @return
     * @throws ProfileException
     * @throws MissingLandmarkException
     * @throws MissingProfileException
     * @throws UnavailableBorderPointException
     */
    private double improveBorderPoint(int index, double minDistance, double maxDistance, double similarityScore,
    		 @NonNull Nucleus testNucleus) throws ProfileException, MissingLandmarkException,
            MissingProfileException, UnavailableBorderPointException {
        // // make all changes to a fresh nucleus before buggering up the real
        // one
        LOGGER.finest( "Testing point " + index);
        double score = testNucleus.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT)
                .absoluteSquareDifference(targetCurve);

        // Get a copy of the point at this index
        IPoint p = testNucleus.getBorderPoint(index);

        // Save the old position
        double oldX = p.getX();
        double oldY = p.getY();

        // Make a random adjustment to the x and y positions. Move them more
        // extensively when the score is high
        double xDelta = 0.5 - Math.min(Math.random() * (similarityScore / 1000), 1);
        double yDelta = 0.5 - Math.min(Math.random() * (similarityScore / 1000), 1);

        // Apply the calculated deltas to the x and y positions
        double newX = oldX + xDelta;
        double newY = oldY + yDelta;

        // Check the new point is valid
        IPoint newPoint = IPoint.makeNew(newX, newY);

        boolean ok = checkPositionIsOK(newPoint, testNucleus, index, minDistance, maxDistance);

        if (ok) {
            // Update the test nucleus and recalculate the profiles
            testNucleus.updateBorderPoint(index, newPoint);

            LOGGER.finer( "Testing profiles");
            try {

                testNucleus.calculateProfiles();

                // Get the new score
                score = testNucleus.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT)
                        .absoluteSquareDifference(targetCurve);

                // If the change to the test nucleus improved the score, apply
                // the change to the
                // real consensus nucleus

                if (score < similarityScore) {
                    IPoint exisiting = refoldNucleus.getBorderPoint(index);
                    LOGGER.fine("Updating " + exisiting.toString() + " to " + newPoint.toString() + ": "
                            + exisiting.getLengthTo(newPoint));
                    refoldNucleus.updateBorderPoint(index, newPoint);
                    pointUpdateCounter++;

                    refoldNucleus.calculateProfiles();

                    similarityScore = score;
                }
            } catch (ProfileException e) {
                LOGGER.warning("Cannot calculate profiles in either test or consensus");
                LOGGER.log(Loggable.STACK, "Error calculating profiles", e);
            }
        }

        return similarityScore;
    }

    /**
     * // Do not apply a change if the distance from the surrounding points
     * changes too much
     * 
     * @param point the new point to test
     * @param n the nucleus
     * @param index the point position in the nucleus
     * @param min the min acceptable distance between points
     * @param max the max acceptable distance between points
     * @return
     * @throws UnavailableBorderPointException
     */
    private boolean checkPositionIsOK(@NonNull IPoint point, @NonNull Nucleus n, int index, double min, double max)
            throws UnavailableBorderPointException {
        double distanceToNext = point.getLengthTo(n.getBorderPoint(n.wrapIndex(index + 1)));
        if (distanceToNext > max)
        	return false;
        if (distanceToNext < min)
        	return false;
        double distanceToPrev = point.getLengthTo(n.getBorderPoint(n.wrapIndex(index - 1)));
        if (distanceToPrev > max)
        	return false;
        if (distanceToPrev < min)
        	return false;
        return true;
    }
}
