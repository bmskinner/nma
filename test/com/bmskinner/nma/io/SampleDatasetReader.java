/*******************************************************************************
 *      Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.bmskinner.nma.io;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.Test;

import com.bmskinner.nma.TestResources;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.components.Version;
import com.bmskinner.nma.components.Version.UnsupportedVersionException;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;

/**
 * Provides a simple access point to open datasets for testing classes
 * 
 * @author bms41
 * @since 1.13.8
 *
 */
public class SampleDatasetReader {

	/**
	 * Open the default mouse testing dataset
	 * 
	 * @return
	 * @throws Exception
	 */
	public static final IAnalysisDataset openTestMouseDataset() throws Exception {
		return openDataset(TestResources.MOUSE_TEST_DATASET);
	}

	/**
	 * Open the mouse testing dataset for the given version (if available)
	 * 
	 * @return
	 * @throws Exception
	 */
	public static final IAnalysisDataset openTestMouseDataset(Version version) throws Exception {
		File testDataset = new File(
				new File(TestResources.MOUSE_INPUT_FOLDER,
						TestResources.unitTestFolderName(version)),
				TestResources.MOUSE + Io.NMD_FILE_EXTENSION);
		return openDataset(testDataset);
	}

	/**
	 * Open the default mouse testing dataset with clusters
	 * 
	 * @return
	 * @throws Exception
	 */
	public static final IAnalysisDataset openTestMouseClusterDataset() throws Exception {
		return openDataset(TestResources.MOUSE_CLUSTERS_DATASET);
	}

	/**
	 * Open the default pig testing dataset
	 * 
	 * @return
	 * @throws Exception
	 */
	public static final IAnalysisDataset openTestPigDataset() throws Exception {
		return openDataset(TestResources.PIG_TEST_DATASET);
	}

	/**
	 * Open the default round testing dataset
	 * 
	 * @return
	 * @throws Exception
	 */
	public static final IAnalysisDataset openTestRoundDataset() throws Exception {
		return openDataset(TestResources.ROUND_TEST_DATASET);
	}

	/**
	 * Open the default mouse signals testing dataset
	 * 
	 * @return
	 * @throws Exception
	 */
	public static final IAnalysisDataset openTestMouseSignalsDataset() throws Exception {
		return openDataset(TestResources.MOUSE_SIGNALS_DATASET);
	}

	/**
	 * Open the dataset in the given file. A convenience wrapper around the
	 * DatasetImportMethod
	 * 
	 * @param f the file to open
	 * @return the dataset
	 * @throws Exception
	 */
	public static IAnalysisDataset openDataset(@NonNull File f) throws Exception {
		return openDataset(f, null);
	}

	public static IAnalysisDataset openXMLDataset(@NonNull File f) throws Exception {
		IAnalysisDataset d = XMLReader.readDataset(f);
		if (!Version.versionIsSupported(d.getVersionCreated()))
			throw new UnsupportedVersionException(d.getVersionCreated());
		return d;
	}

	/**
	 * Open the dataset in the given file. Also provide a map of signal images.
	 * 
	 * @param f         the file to open
	 * @param signalMap a map of signal ids to folders. Can be null,
	 * @return the dataset
	 * @throws Exception
	 */
	public static IAnalysisDataset openDataset(@NonNull File f, @Nullable Map<UUID, File> signalMap)
			throws Exception {
		if (!f.exists())
			throw new IllegalArgumentException("File does not exist: " + f.getAbsolutePath());

		XMLImportMethod g = new XMLImportMethod(f);
		g.call();

		IAnalysisMethod m = signalMap == null ? new DatasetImportMethod(g.getXMLDocument())
				: new DatasetImportMethod(g.getXMLDocument(), signalMap);
		return m.call().getFirstDataset();
	}

	@Test
	public void testUnmarshalledDatasetHasProfilesCreated() throws Exception {
		IAnalysisDataset d = openTestMouseDataset();

		for (ProfileType t : ProfileType.values()) {
			d.getCollection().getProfileCollection().getProfile(t, OrientationMark.REFERENCE, 50);
		}

	}

}
