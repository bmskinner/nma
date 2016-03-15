/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
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
package gui.tabs;

import gui.tabs.nuclear.NuclearBoxplotsPanel;
import gui.tabs.nuclear.NuclearHistogramsPanel;
import gui.tabs.nuclear.NucleusMagnitudePanel;
import gui.tabs.nuclear.WilcoxonDetailPanel;
import java.awt.BorderLayout;
import java.util.logging.Level;

import javax.swing.JTabbedPane;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import charting.options.ChartOptions;
import charting.options.TableOptions;

public class NuclearStatisticsPanel extends DetailPanel {
	
	private static final long serialVersionUID = 1L;
	
	private NuclearBoxplotsPanel 	boxplotPanel;
	private NuclearHistogramsPanel histogramsPanel;
	private WilcoxonDetailPanel wilcoxonPanel;
	private NucleusMagnitudePanel nucleusMagnitudePanel;
	
	private JTabbedPane 	tabPane;

	public NuclearStatisticsPanel() throws Exception {
		super();
		this.setLayout(new BorderLayout());
		tabPane = new JTabbedPane(JTabbedPane.TOP);
		
		boxplotPanel = new NuclearBoxplotsPanel();
		this.addSubPanel(boxplotPanel);
		tabPane.addTab("Boxplots", boxplotPanel);
		
		histogramsPanel = new NuclearHistogramsPanel();
		this.addSubPanel(histogramsPanel);
		tabPane.addTab("Histograms", histogramsPanel);
		
		wilcoxonPanel 	= new WilcoxonDetailPanel();
		tabPane.addTab("Stats", null, wilcoxonPanel, null);
		
		nucleusMagnitudePanel 	= new NucleusMagnitudePanel();
		tabPane.addTab("Magnitude", null, nucleusMagnitudePanel, null);
		
		this.add(tabPane, BorderLayout.CENTER);
	}
	
	@Override
	protected void updateSingle() throws Exception {
		boxplotPanel.update(getDatasets());
		log(Level.FINEST, "Updated nuclear boxplots panel");
		
		histogramsPanel.update(getDatasets());
		log(Level.FINEST, "Updated nuclear histograms panel");
		
		wilcoxonPanel.update(getDatasets());
		log(Level.FINEST, "Updating Wilcoxon panel");
		
		nucleusMagnitudePanel.update(getDatasets());
		log(Level.FINEST, "Updating magnitude panel");
	}
	
	@Override
	protected void updateMultiple() throws Exception {
		updateSingle();
	}
	
	@Override
	protected void updateNull() throws Exception {
		updateSingle();
	}
	
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception{
		return null;
	}

}
