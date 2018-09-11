package com.bmskinner.nuclear_morphology.reports;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;

/**
 * Method containined the shell report generator
 * @author ben
 * @since 1.14.0
 *
 */
public class ShellReportMethod extends SingleDatasetAnalysisMethod {

	public ShellReportMethod(@NonNull IAnalysisDataset dataset) {
		super(dataset);
	}

	@Override
	public IAnalysisResult call() throws Exception {
		run();
		return new DefaultAnalysisResult(dataset);
	}
	
	public void run() throws Exception {
		
		DemoReportGenerator generator = new DemoReportGenerator();
		generator.generateShellReport(dataset);
		fireProgressEvent();
		
	}
	

}
