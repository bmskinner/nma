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
package com.bmskinner.nuclear_morphology.reports;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.title.Title;

import com.bmskinner.nuclear_morphology.charting.charts.ShellChartFactory;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.Version;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult.Aggregation;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult.CountType;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult.Normalisation;
import com.bmskinner.nuclear_morphology.components.signals.ISignalGroup;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.Io;
import com.bmskinner.nuclear_morphology.stats.ShellDistributionTester;
import com.bmskinner.nuclear_morphology.stats.SignificanceTest;

/**
 * This is a test class to play around with exporting charts and
 * tables.
 * @author ben
 * @since 1.14.0
 *
 */
public class DemoReportGenerator {
	
	private static final Logger LOGGER = Logger.getLogger(DemoReportGenerator.class.getName());
	
	private static final String NEWLINE = System.getProperty("line.separator");
	private static final String P_VALUE_FORMAT = "0.##E0";
	private static final String DOMAIN_FORMAT = "0.0000";
	
	private static final String ZERO_STRING = "\t0.0000\t0.0000\t0.0000\t0.0000\t0.0000";	
	
	public void generateShellReport(@NonNull final IAnalysisDataset dataset) throws IOException, MissingOptionException {
		
		// Store visibility states of each signal
		Map<UUID,Boolean> wasVisible = new HashMap<>();
		for(UUID signalGroupId : dataset.getCollection().getSignalGroupIDs()) {
			ISignalGroup group = dataset.getCollection().getSignalGroup(signalGroupId).get();
			wasVisible.put(signalGroupId, group.isVisible());
			group.setVisible(false);
		}
		
		File saveFolder = dataset.getSavePath().getParentFile();
		
		Optional<ISignalGroup> randomGroup = dataset.getCollection().getSignalGroup(IShellResult.RANDOM_SIGNAL_ID);
        Optional<IShellResult> random = randomGroup.isPresent() ? 
        		dataset.getCollection().getSignalGroup(IShellResult.RANDOM_SIGNAL_ID).get().getShellResult() 
                : Optional.empty();

		for(UUID signalGroupId : dataset.getCollection().getSignalGroupIDs()) {
			
			ISignalGroup group = dataset.getCollection().getSignalGroup(signalGroupId).get();
			group.setVisible(true);
			ChartOptions barChartOptions = new ChartOptionsBuilder()
	        		.setDatasets(dataset)
	                .setShowAnnotations(false) // proxy fpr random
	                .setAggregation(Aggregation.BY_NUCLEUS)
	                .setNormalisation(Normalisation.DAPI)
	                .build();

			if(group.hasShellResult()) {
				
				File chartFile = new File(saveFolder, dataset.getName()+"_"+group.getGroupName()+Io.PNG_FILE_EXTENSION);
				
				HashOptions signalOptions = dataset.getAnalysisOptions().get().getNuclearSignalOptions(signalGroupId).orElseThrow(MissingOptionException::new);
				
				JFreeChart chart = new ShellChartFactory(barChartOptions).createShellChart();
				chart.setTitle(String.format("%s", dataset.getName()));
				Title channelSubtitle = new TextTitle(String.format("Signal group: %s (%s)", group.getGroupName(), 
						ImageImporter.channelIntToName(signalOptions.getInt(HashOptions.CHANNEL))));
				chart.addSubtitle(channelSubtitle);
				
				if(random.isPresent()) {
			    	ShellDistributionTester tester = new ShellDistributionTester(group.getShellResult().get(), random.get());
			    	double pval = tester.test(Aggregation.BY_NUCLEUS, Normalisation.DAPI).getPValue();
			    	
			    	Title sigSubtitle;
			    	DecimalFormat pFormat = new DecimalFormat(P_VALUE_FORMAT);
			    	if(pval<SignificanceTest.FIVE_PERCENT_SIGNIFICANCE_LEVEL) {
			    		sigSubtitle = new TextTitle(String.format("Different to a random distribution: p=%s", pFormat.format(pval)));
			    	} else {
			    		sigSubtitle = new TextTitle(String.format("Not different to a random distribution: p=%s", pFormat.format(pval)));
			    	}
			    	chart.addSubtitle(sigSubtitle);
			    }
				chart.getCategoryPlot().setRangeGridlinesVisible(true);
				chart.getCategoryPlot().setRangeGridlinePaint(Color.LIGHT_GRAY);
				chart.getCategoryPlot().getRangeAxis().setRange(0, 60);

				OutputStream out = new FileOutputStream(chartFile);
				ChartUtils.writeChartAsPNG(out, chart, 800, 600);
				
				out.close();
			}
			group.setVisible(false);
		}
		
		// Restore visibility states of each signal
		for(UUID signalGroupId : dataset.getCollection().getSignalGroupIDs()) {
			ISignalGroup group = dataset.getCollection().getSignalGroup(signalGroupId).get();
			group.setVisible(wasVisible.get(signalGroupId));
		}
		
		generateDomainAnalysisOutputFormat(dataset);

	}
	
	/**
	 * Create a file of shell results compatible with Michael's domain analysis
	 * macro format, for analysis in existing Excel templates
	 * @param dataset
	 * @throws MissingOptionException 
	 */
	private void generateDomainAnalysisOutputFormat(@NonNull final IAnalysisDataset dataset) throws MissingOptionException {

		Map<Integer, UUID> channelMap = new HashMap<>();
		for(UUID id : dataset.getAnalysisOptions().get().getNuclearSignalGroups()) {
			int channel = dataset.getAnalysisOptions().get().getNuclearSignalOptions(id).orElseThrow(MissingOptionException::new).getInt(HashOptions.CHANNEL);
			channelMap.put(channel, id);
		}
		
		StringBuilder builder = new StringBuilder("Domain Analysis 2.1.2 compatible file from Nuclear Morphology Analysis "+Version.currentVersion()+NEWLINE);
		builder.append(String.format("Raw pixel intensity proportions; not normalised"+NEWLINE));
		builder.append("Band 1 innermost. Band 5 outermost."+NEWLINE);
		builder.append("Chan\tBand 1\tBand 2\tBand 3\tBand 4\tBand 5"+NEWLINE);
		
		for(ICell cell : dataset.getCollection().getCells()) {

			for(Nucleus n : cell.getNuclei()) {

				String cellLine = String.format("x=%s y=%s"+NEWLINE, n.getCentreOfMass().getXAsInt(), n.getCentreOfMass().getYAsInt());

				String redLine   = createChannelString(0, n, cell, dataset, channelMap);
				String greenLine = createChannelString(1, n, cell, dataset, channelMap);

				ISignalGroup blue = chooseCounterstainGroup(dataset, channelMap);
				if(blue==null || !blue.hasShellResult())
					continue;
				
				String blueLine = createCounterstainString(blue, cell, n);				
				builder.append(cellLine);
				builder.append(redLine);
				builder.append(greenLine);
				builder.append(blueLine);
			}

		}
		File saveFolder = dataset.getSavePath().getParentFile();
		File outFile = new File(saveFolder, dataset.getName()+" Log.txt");

		try(PrintWriter writer = new PrintWriter(outFile)) {
			writer.write(builder.toString());
			
		} catch (FileNotFoundException e) {
			LOGGER.warning("Unable to write domain file");
		}

	}
	
	/**
	 * Create the domain analysis format string for the red or green channel
	 * @param channel the channel to use (0 or 1)
	 * @param n the nucleus
	 * @param cell the cell
	 * @param dataset the dataset
	 * @param channelMap the map of signal group to channel
	 * @return
	 */
	private String createChannelString(int channel, Nucleus n, ICell cell, IAnalysisDataset dataset, Map<Integer, UUID> channelMap) {
		DecimalFormat formatter = new DecimalFormat(DOMAIN_FORMAT);

		StringBuilder line = new StringBuilder(channel==0?"Red":"Green");
		if(channelMap.containsKey(channel)) {
			ISignalGroup sg = dataset.getCollection().getSignalGroup(channelMap.get(channel)).get();
			if(sg.hasShellResult()) {
				double[] values = sg.getShellResult().get().getProportions(CountType.SIGNAL, cell, n, null);	
				for(int i=values.length-1; i>=0; i--) // Domain analysis shells are opposite way round to mine
					line.append("\t"+formatter.format(values[i]));	
			}else {
				line.append(ZERO_STRING);	
			}
		} else {
			line.append(ZERO_STRING);	
		}
		line.append(NEWLINE);
		return line.toString();
	}
	
	public ISignalGroup chooseCounterstainGroup(IAnalysisDataset dataset, Map<Integer, UUID> channelMap) {
		ISignalGroup blue = null;
		if(channelMap.containsKey(0)) {
			blue = dataset.getCollection().getSignalGroup(channelMap.get(0)).get();
			if(!blue.hasShellResult() && channelMap.containsKey(1))
				blue = dataset.getCollection().getSignalGroup(channelMap.get(1)).get();
		} else {
			if(channelMap.containsKey(1))
				blue = dataset.getCollection().getSignalGroup(channelMap.get(1)).get();
		}
		return blue;
	}
	
	private String createCounterstainString(ISignalGroup blue, ICell cell, Nucleus n) {
		DecimalFormat formatter = new DecimalFormat(DOMAIN_FORMAT);
		StringBuilder line = new StringBuilder("Blue");
		double[] blues = blue.getShellResult().get().getProportions(CountType.COUNTERSTAIN, cell, n, null);	
		for(double d : blues) { // Sanity check - can't export something with no counterstain signal
			if(d==0) {
				line.append(ZERO_STRING);
				return line.toString();
			}
		}
				
		for(int i=blues.length-1; i>=0; i--) // Domain analysis shells are opposite way round to mine
			line.append("\t"+formatter.format(blues[i]));	
		line.append(NEWLINE);
		return line.toString();
	}

}
