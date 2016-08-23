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

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import org.jfree.chart.JFreeChart;

import charting.options.ChartOptions;

@SuppressWarnings("serial")
public abstract class AbstractPairwiseDetailPanel extends DetailPanel {

	protected JPanel tablePanel;
	protected JScrollPane scrollPane = new JScrollPane();
				
		public AbstractPairwiseDetailPanel(){
			super();

			this.setLayout(new BorderLayout());
			
			tablePanel = createTablePanel();
			scrollPane.setViewportView(tablePanel);

			this.add(createInfoPanel(), BorderLayout.NORTH);
			this.add(scrollPane, BorderLayout.CENTER);
		}
		
		protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
			return null;
		}
				
		/**
		 * Create the info panel
		 * @return
		 */
		protected JPanel createInfoPanel(){
			JPanel infoPanel = new JPanel();
			infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
			infoPanel.add(new JLabel("Pairwise comparisons between populations using Mann-Whitney U test"));
			infoPanel.add(new JLabel("Above the diagonal: Mann-Whitney U statistics"));
			infoPanel.add(new JLabel("Below the diagonal: p-values"));
			infoPanel.add(new JLabel("Significant values at 5% and 1% levels after Bonferroni correction are highlighted in yellow and green"));
			return infoPanel;
		}
		
		/**
		 * Create a new panel to hold tables
		 * @return
		 */
		protected JPanel createTablePanel(){
			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

			Dimension minSize = new Dimension(10, 10);
			Dimension prefSize = new Dimension(10, 10);
			Dimension maxSize = new Dimension(Short.MAX_VALUE, 10);
			panel.add(new Box.Filler(minSize, prefSize, maxSize));
			return panel;
		}
		
		/**
		 * Prepare a wilcoxon table
		 * @param panel the JPanel to add the table to
		 * @param table the table to add
		 * @param model the model to provide
		 * @param label the label for the table
		 */
		protected void addWilconxonTable(JPanel panel, JTable table, String label){
			Dimension minSize = new Dimension(10, 10);
			Dimension prefSize = new Dimension(10, 10);
			Dimension maxSize = new Dimension(Short.MAX_VALUE, 10);
			panel.add(new Box.Filler(minSize, prefSize, maxSize));
			panel.add(new JLabel(label));
			panel.add(table);
			table.setEnabled(false);
		}		
	}
