package com.bmskinner.nuclear_morphology.components;

/**
 * The interface for mitochondria
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public interface IMitochondrion extends CellularComponent, Comparable<IMitochondrion> {

    void alignVertically();

    IMitochondrion duplicate();

}