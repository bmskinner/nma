package com.bmskinner.nuclear_morphology.components;

/**
 * The interface for acrosomes
 * @author bms41
 * @since 1.13.3
 *
 */
public interface IAcrosome extends CellularComponent, Comparable<IAcrosome> {

	void alignVertically();

	IAcrosome duplicate();

}