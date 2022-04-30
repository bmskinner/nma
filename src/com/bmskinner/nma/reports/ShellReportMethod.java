/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nma.reports;

import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.logging.Loggable;

/**
 * Method containined the shell report generator
 * @author ben
 * @since 1.14.0
 *
 */
public class ShellReportMethod extends SingleDatasetAnalysisMethod {
	
	private static final Logger LOGGER = Logger.getLogger(ShellReportMethod.class.getName());

	public ShellReportMethod(@NonNull IAnalysisDataset dataset) {
		super(dataset);
	}

	@Override
	public IAnalysisResult call() throws Exception {
		run();
		return new DefaultAnalysisResult(dataset);
	}
	
	public void run() {
		
		try {
			DemoReportGenerator generator = new DemoReportGenerator();
			generator.generateShellReport(dataset);
			fireProgressEvent();
		} catch(Exception e) {
			LOGGER.warning("Could not generate report for dataset "+dataset.getName()+": "+e.getMessage());
			LOGGER.log(Loggable.STACK, "Could not generate report for dataset "+dataset.getName(),  e);
		}
		
	}
	

}
