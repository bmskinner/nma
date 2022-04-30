package com.bmskinner.nma.components.cells;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ CellularComponentTest.class, 
	DefaultCellCollectionTest.class, 
	DefaultCellTest.class,
	DefaultCellularComponentTest.class, 
	ICellCollectionTest.class,
	ProfilableCellularComponentTest.class})
public class ComponentCellsTestSuite {

}
