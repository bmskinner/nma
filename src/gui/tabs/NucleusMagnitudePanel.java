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

import gui.MainWindow;
import gui.components.ExportableTable;
import gui.components.PairwiseTableCellRenderer;
import gui.dialogs.MainOptionsDialog;
import gui.dialogs.RandomSamplingDialog;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;

import analysis.RandomSampler;
import analysis.nucleus.ShellAnalysis;
import stats.NucleusStatistic;
import charting.NucleusStatsTableOptions;
import charting.TableOptions;
import charting.datasets.NucleusTableDatasetCreator;

@SuppressWarnings("serial")
public class NucleusMagnitudePanel extends AbstractPairwiseDetailPanel {
	
	public NucleusMagnitudePanel(Logger programLogger) throws Exception {
		super(programLogger);
	}

	
	/**
	 * Create the info panel
	 * @return
	 */
	@Override
	protected JPanel createInfoPanel(){
		JPanel infoPanel = new JPanel();
		infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
		infoPanel.add(new JLabel("Pairwise magnitude comparisons between populations"));
		infoPanel.add(new JLabel("Row median value as a proportion of column median value"));
		
		JButton button = new JButton("Random sampling");
		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {

				new RandomSamplingDialog(activeDataset(), programLogger);
			
			}
		});		
		
		infoPanel.add(button);
		return infoPanel;
	}
		
	/**
	 * Update the magnitude panel with data from the given datasets
	 * @param list the datasets
	 * @throws Exception 
	 */
	@Override
	public void updateDetail() {
		programLogger.log(Level.FINE, "Updating nucleus magnitude panel");

		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				
				try{

					scrollPane.setColumnHeaderView(null);
					tablePanel = createTablePanel();
					
					if(hasDatasets()){
						
						if(!isSingleDataset()){

							for(NucleusStatistic stat : NucleusStatistic.values()){

								TableModel model;

								TableOptions options = new NucleusStatsTableOptions(getDatasets(), stat);
								if(getTableCache().hasTable(options)){
									programLogger.log(Level.FINEST, "Fetched cached magnitude table: "+stat);
									model = getTableCache().getTable(options);
								} else {
									model = NucleusTableDatasetCreator.createMagnitudeNuclearStatTable(getDatasets(), stat);
									programLogger.log(Level.FINEST, "Added cached magnitude table: "+stat);
								}


								ExportableTable table = new ExportableTable(model);
								setRenderer(table, new PairwiseTableCellRenderer());
								addWilconxonTable(tablePanel, table, stat.toString());
								scrollPane.setColumnHeaderView(table.getTableHeader());


							}
							tablePanel.revalidate();

							
						} else {
							tablePanel.add(new JLabel("Single dataset selected", JLabel.CENTER));
						}
					} else {
						tablePanel.add(new JLabel("No datasets selected", JLabel.CENTER));
					}
					programLogger.log(Level.FINEST, "Updated magnitude panel");
				} catch (Exception e) {
					programLogger.log(Level.SEVERE, "Error making magnitude table", e);
					tablePanel = createTablePanel();
				} finally {
					scrollPane.setViewportView(tablePanel);;
					tablePanel.repaint();
					setUpdating(false);
				}
				
			}});
	}
}
	

	