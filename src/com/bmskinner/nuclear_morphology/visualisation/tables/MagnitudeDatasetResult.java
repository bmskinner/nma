package com.bmskinner.nuclear_morphology.visualisation.tables;

import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;

/**
 * Store comparisons of magnitude between datasets
 * 
 * @author ben
 *
 */
public record MagnitudeDatasetResult(IAnalysisDataset numerator, IAnalysisDataset denominator, double value) {

}