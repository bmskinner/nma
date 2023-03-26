package com.bmskinner.nma.io.conversion;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.ComponentTester;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.io.DatasetImportMethod.UnloadableDatasetException;
import com.bmskinner.nma.io.PackageReplacementObjectInputStream;

/**
 * This class tests the conversions between component
 * classes
 * @author bms41
 * @since 1.14.0
 *
 */
public class CellularComponentConversionTest extends ComponentTester {

	/**
	 * Deserialise the given file to a dataset. Mimics the dataset importer, but
	 * without validation or monitoring the input stream.
	 * @param inputFile the file to import
	 * @return
	 * @throws UnloadableDatasetException
	 */
	private IAnalysisDataset deserialiseDataset(@NonNull File inputFile) {
		IAnalysisDataset dataset = null;

		try(FileInputStream fis     = new FileInputStream(inputFile.getAbsolutePath());
			BufferedInputStream bis = new BufferedInputStream(fis);	
			ObjectInputStream ois   = new PackageReplacementObjectInputStream(bis);) {

			dataset = (IAnalysisDataset) ois.readObject();

		} catch(Exception e) {
			e.printStackTrace();
		}
		return dataset;
	}

}
