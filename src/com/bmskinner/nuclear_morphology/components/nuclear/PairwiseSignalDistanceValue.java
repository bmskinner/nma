package com.bmskinner.nuclear_morphology.components.nuclear;

import java.util.UUID;

/**
 * Holds the shortest distance between signals in two signal groups within a
 * nucleus.
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public class PairwiseSignalDistanceValue {
    private UUID group1;
    private UUID group2;

    private double value;

    public PairwiseSignalDistanceValue(UUID id1, UUID id2, double value) {
        group1 = id1;
        group2 = id2;
        this.value = value;
    }

    public UUID getGroup1() {
        return group1;
    }

    public UUID getGroup2() {
        return group2;
    }

    public double getValue() {
        return value;
    }

}
