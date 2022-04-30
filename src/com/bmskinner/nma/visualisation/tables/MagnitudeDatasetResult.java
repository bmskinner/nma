package com.bmskinner.nma.visualisation.tables;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

/**
 * Store comparisons of magnitude between datasets
 * 
 * @author ben
 *
 */
public record MagnitudeDatasetResult(IAnalysisDataset numerator, IAnalysisDataset denominator, double value) {

}