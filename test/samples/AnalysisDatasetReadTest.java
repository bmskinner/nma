package samples;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.io.DatasetImportMethod;

public class AnalysisDatasetReadTest {
	
	public static final String TEST_PATH_1 = "C:\\Users\\ben\\Documents\\Datasets\\Testing\\2017-05-21_21-02-44\\Testing.nmd";

	@Test
	public void testDatasetCanBeRead() {
		File f = new File(TEST_PATH_1);
		try {
			IAnalysisDataset d = openDataset(f);
//			
			assertEquals(d.getSavePath(), f);
			
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	
	@Test
	public void testHookBodyDataset() {
		File f = new File("C:\\Users\\ben\\Documents\\Borked datasets\\Hook_body_calc\\(PWK X LEW) X LEW (30+ 43).nmd");
		try {
			IAnalysisDataset d = openDataset(f);
//			
			
			
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	private IAnalysisDataset openDataset(File f) throws Exception{
		IAnalysisMethod m = new DatasetImportMethod(f);

		System.out.println("Importing "+f.toString());
		IAnalysisResult r = m.call();

		IAnalysisDataset d = r.getFirstDataset();
		return d;

	}

}
