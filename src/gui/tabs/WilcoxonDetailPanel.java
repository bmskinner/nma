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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;

import charting.NucleusStatsTableOptions;
import charting.TableOptions;
import charting.datasets.NucleusTableDatasetCreator;
import gui.components.ExportableTable;
import stats.NucleusStatistic;

@SuppressWarnings("serial")
public class WilcoxonDetailPanel extends AbstractWilcoxonDetailPanel {
		
	public WilcoxonDetailPanel(Logger programLogger) throws Exception {
		super(programLogger);
	}

		
	/**
	 * Update the wilcoxon panel with data from the given datasets
	 * @param list the datasets
	 * @throws Exception 
	 */
	@Override
	public void updateDetail() {
		programLogger.log(Level.FINE, "Updating Wilcoxon panel");

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
									programLogger.log(Level.FINEST, "Fetched cached Wilcoxon table: "+stat);
									model = getTableCache().getTable(options);
								} else {
									model = NucleusTableDatasetCreator.createWilcoxonNuclearStatTable(getDatasets(), stat);
									programLogger.log(Level.FINEST, "Added cached Wilcoxon table: "+stat);
								}


								ExportableTable table = new ExportableTable(model);
								setRenderer(table);
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
					programLogger.log(Level.FINEST, "Updated Wilcoxon panel");
				} catch (Exception e) {
					programLogger.log(Level.SEVERE, "Error making Wilcoxon table", e);
					tablePanel = createTablePanel();
				} finally {
					scrollPane.setViewportView(tablePanel);;
					tablePanel.repaint();
					setUpdating(false);
				}
				
			}});
	}

}
