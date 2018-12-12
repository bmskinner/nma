package com.bmskinner.nuclear_morphology.gui.tabs.signals.warping;

import java.awt.BorderLayout;
import java.text.DecimalFormat;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.analysis.image.MultiScaleStructuralSimilarityIndex;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.tabs.signals.warping.SignalWarpingModel.ImageCache.WarpedImageKey;

import ij.process.ImageProcessor;

public class StructuralSimilarityComparisonDialog extends JDialog {
	
	private static final String DIALOG_TITLE = "MS-SSIM* scores";
	
	public StructuralSimilarityComparisonDialog(SignalWarpingModel model) {

		TableModel compModel = createTableModel(model);
		ExportableTable table = new ExportableTable(compModel);
		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setColumnHeaderView(table.getTableHeader());
		setLocationRelativeTo(null);
		setTitle(DIALOG_TITLE);
		setLayout(new BorderLayout());
		add(scrollPane, BorderLayout.CENTER);
		validate();
		pack();
		setVisible(true);
	}

	private TableModel createTableModel(SignalWarpingModel model) {
		DecimalFormat df = new DecimalFormat("0.000");
		DefaultTableModel compModel = new DefaultTableModel();
		Object[] columns = { "Key 1", "Key 2", "Luminance", "Contrast", "Structure", "MS-SSIM*"};
		compModel.setColumnIdentifiers(columns);
		for(CellularComponent c : model.getTargets()) {
			for(WarpedImageKey k1 : model.getKeys(c)) {
				for(WarpedImageKey k2 : model.getKeys(c)) {
					if(k1==k2)
						continue;

					ImageProcessor ip1 = model.getImage(k1);
					ImageProcessor ip2 = model.getImage(k2);
					System.out.println(k1+" vs "+k2);
					double[] values = MultiScaleStructuralSimilarityIndex.calculateMSSIM(ip1, ip2);

					Object[] reverseData = { k2, k1, df.format(values[0]), df.format(values[1]),  df.format(values[2]),  df.format(values[3]) };
					if(!containsRow(reverseData, compModel)) {
						Object[] rowData = { k1, k2, df.format(values[0]), df.format(values[1]),  df.format(values[2]),  df.format(values[3]) };
						compModel.addRow(rowData);
					}
				}
			}
		}
		return compModel;
	}
	
	private boolean containsRow(Object[] rowData, TableModel model) {
		for(int r=0; r<model.getRowCount(); r++) {
			boolean matches = true;
			for(int c=0; c<model.getColumnCount(); c++)
				matches &= model.getValueAt(r, c).equals(rowData[c]);
			if(matches)
				return true;
		}
		return false;
	}

}
