package com.bmskinner.nuclear_morphology.charting.datasets;

import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.data.KeyedObjects2D;

import com.bmskinner.nuclear_morphology.charting.ExportableBoxAndWhiskerCategoryDataset;

/**
 * Store signal group information on top of a boxplot dataset
 * @author bms41
 * @since 1.15.0
 *
 */
public class NuclearSignalBoxAndWhiskerDataset extends ExportableBoxAndWhiskerCategoryDataset{

	private KeyedObjects2D signalGroups;

	public NuclearSignalBoxAndWhiskerDataset() {
		super();
		signalGroups = new KeyedObjects2D();
	}

	 /**
     * Add a series to this dataset, with a signal group id
     * 
     * @param signalGroup
     * @param values
     * @param rowKey
     * @param columnKey
     */
    public void add(@NonNull UUID signalGroup, @NonNull List<Double> values, Comparable<?> rowKey, Comparable<?> columnKey) {
        super.add(values, rowKey, columnKey);
        signalGroups.addObject(signalGroup, rowKey, columnKey);
    }

    /**
     * Get the signal group for the given row and column
     * 
     * @param rowKey
     * @param columnKey
     * @return
     */
    public UUID getSignalGroup(Comparable<?> rowKey, Comparable<?> columnKey) {
        return (UUID) signalGroups.getObject(rowKey, columnKey);
    }

}
