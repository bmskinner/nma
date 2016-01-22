package gui.tabs;

import gui.components.WilcoxonTableCellRenderer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

@SuppressWarnings("serial")
public abstract class AbstractWilcoxonDetailPanel extends DetailPanel {

	protected JPanel tablePanel;
	protected JScrollPane scrollPane = new JScrollPane();
				
		public AbstractWilcoxonDetailPanel(Logger logger){
			super(logger);

			this.setLayout(new BorderLayout());
			
			tablePanel = createTablePanel();
			scrollPane.setViewportView(tablePanel);

			this.add(createInfoPanel(), BorderLayout.NORTH);
			this.add(scrollPane, BorderLayout.CENTER);
		}
		
		/**
		 * Create the info panel
		 * @return
		 */
		private JPanel createInfoPanel(){
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
