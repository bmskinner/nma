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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.image.BufferedImage;
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

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.title.Title;

import com.bmskinner.nuclear_morphology.charting.charts.NuclearSignalChartFactory;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.Aggregation;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.CountType;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.Normalisation;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.Io;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.ShellDistributionTester;
import com.bmskinner.nuclear_morphology.stats.SignificanceTest;

/**
 * This is a test class to play around with exporting charts and
 * tables.
 * @author ben
 * @since 1.14.0
 *
 */
public class DemoReportGenerator implements Loggable {
	
	private static final String NEWLINE = System.getProperty("line.separator");
	private static final String P_VALUE_FORMAT = "0.##E0";
	private static final String DOMAIN_FORMAT = "0.0000";
	
	public void generateShellReport(@NonNull final IAnalysisDataset dataset) throws IOException {
		
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
				
				IDetectionOptions signalOptions = dataset.getAnalysisOptions().get().getNuclearSignalOptions(signalGroupId);
				
				JFreeChart chart = new NuclearSignalChartFactory(barChartOptions).createShellChart();
				chart.setTitle(String.format("%s", dataset.getName()));
				Title channelSubtitle = new TextTitle(String.format("Signal group: %s (%s)", group.getGroupName(), ImageImporter.channelIntToName(signalOptions.getChannel())));
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
//				BufferedImage buffImg = chart.createBufferedImage(300, 200);
				OutputStream out = new FileOutputStream(chartFile);
				ChartUtilities.writeChartAsPNG(out, chart, 800, 600);
				
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
	 */
	private void generateDomainAnalysisOutputFormat(@NonNull final IAnalysisDataset dataset) {

		DecimalFormat formatter = new DecimalFormat(DOMAIN_FORMAT);

		Map<Integer, UUID> channelMap = new HashMap<>();
		for(UUID id : dataset.getAnalysisOptions().get().getNuclearSignalGroups()) {
			int channel = dataset.getAnalysisOptions().get().getNuclearSignalOptions(id).getChannel();
			channelMap.put(channel, id);
		}
		
		StringBuilder builder = new StringBuilder("Domain Analysis 2.1.2 compatible file from Nuclear Morphology Analysis "+Version.currentVersion()+NEWLINE);
		builder.append(String.format("Raw pixel intensity proportions; not normalised"+NEWLINE));
		builder.append("Band 1 innermost. Band 5 outermost."+NEWLINE);
		builder.append("Chan\tBand 1\tBand 2\tBand 3\tBand 4\tBand 5"+NEWLINE);

		String zeroString = "\t0.0000\t0.0000\t0.0000\t0.0000\t0.0000";	
		
		for(ICell cell : dataset.getCollection().getCells()) {

			nucleusLoop: for(Nucleus n :cell.getNuclei()) {

				String cellLine = String.format("x=%s y=%s"+NEWLINE, n.getCentreOfMass().getXAsInt(), n.getCentreOfMass().getYAsInt());

				// Red
				String redLine = "Red";
				if(channelMap.containsKey(0)) {
					ISignalGroup red = dataset.getCollection().getSignalGroup(channelMap.get(0)).get();
					if(red.hasShellResult()) {
						double[] reds = red.getShellResult().get().getProportions(CountType.SIGNAL, cell, n, null);	
						for(int i=reds.length-1; i>=0; i--) // Domain analysis shells are opposite way round to mine
							redLine+="\t"+formatter.format(reds[i]);	
					}else {
						redLine+=zeroString;	
					}
				} else {
					redLine+=zeroString;	
				}
				redLine+=NEWLINE;
				

				// Green
				String greenLine = "Green";
				if(channelMap.containsKey(0)) {
					ISignalGroup green = dataset.getCollection().getSignalGroup(channelMap.get(1)).get();
					if(green.hasShellResult()) {
						double[] greens = green.getShellResult().get().getProportions(CountType.SIGNAL, cell, n, null);	
						for(int i=greens.length-1; i>=0; i--) // Domain analysis shells are opposite way round to mine		
							greenLine+="\t"+formatter.format(greens[i]);	
					} else {
						greenLine+=zeroString;	
					}
				}else {
					greenLine+=zeroString;	
				}
				greenLine+=NEWLINE;

				// Blue
				String blueLine = "Blue";
				ISignalGroup blue = null;
				if(channelMap.containsKey(0)) {
					blue = dataset.getCollection().getSignalGroup(channelMap.get(0)).get();
					if(!blue.hasShellResult() && channelMap.containsKey(1))
						blue = dataset.getCollection().getSignalGroup(channelMap.get(1)).get();
				} else {
					if(channelMap.containsKey(1))
						blue = dataset.getCollection().getSignalGroup(channelMap.get(1)).get();
				}
				
				if(blue==null || !blue.hasShellResult())
					continue nucleusLoop;
				
				double[] blues = blue.getShellResult().get().getProportions(CountType.COUNTERSTAIN, cell, n, null);	
				for(int i=blues.length-1; i>=0; i--) // Domain analysis shells are opposite way round to mine
					blueLine+="\t"+formatter.format(blues[i]);	
				blueLine+=NEWLINE;
				
				// Sanity check - can't export something with no counterstain signal
				for(double d : blues)
					if(d==0)
						continue nucleusLoop;
				
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
			warn("Unable to write domain file");
		}

	}

}
