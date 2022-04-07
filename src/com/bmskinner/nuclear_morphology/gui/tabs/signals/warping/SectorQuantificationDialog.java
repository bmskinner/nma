package com.bmskinner.nuclear_morphology.gui.tabs.signals.warping;

import java.awt.BorderLayout;
import java.text.DecimalFormat;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.Nucleus;
import com.bmskinner.nuclear_morphology.components.mesh.DefaultMesh;
import com.bmskinner.nuclear_morphology.components.mesh.DefaultMeshImage;
import com.bmskinner.nuclear_morphology.components.mesh.Mesh;
import com.bmskinner.nuclear_morphology.components.mesh.MeshFace;
import com.bmskinner.nuclear_morphology.components.mesh.MeshImage;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.tabs.signals.warping.SignalWarpingModel.ImageCache.WarpedImageKey;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.process.ImageProcessor;

public class SectorQuantificationDialog extends JDialog {
	
	private static final Logger LOGGER = Logger.getLogger(SectorQuantificationDialog.class.getName());
	
	private static final String DIALOG_TITLE = "Quantification scores";
	
	public SectorQuantificationDialog(SignalWarpingModel model) {

		try {
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
		} catch(Exception e) {
			LOGGER.log(Loggable.STACK, e.getMessage(), e);
		}
	}

	private TableModel createTableModel(SignalWarpingModel model) throws Exception {
		DecimalFormat df = new DecimalFormat("0.000");
		DefaultTableModel compModel = new DefaultTableModel();
		Object[] columns = { "Key", "Segment sector", "Fraction of signal"};
		compModel.setColumnIdentifiers(columns);
		for(CellularComponent c : model.getTargets()) {
			for(WarpedImageKey k1 : model.getKeys(c)) {

//				c.offset(-c.getBounds().getMinX(), -c.getBounds().getMinY()); // set x and y positions to zero
				
				ImageProcessor ip1 = model.getImage(k1);

				Mesh mesh = new DefaultMesh((Nucleus)c);
				MeshImage meshImage = new DefaultMeshImage(mesh, ip1);

				List<IProfileSegment> segs = ((Nucleus)c).getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT).getSegments();
				for(int i=0; i<segs.size(); i++) {
					IProfileSegment s = segs.get(i);
					double total = 0;
					for(MeshFace f : mesh.getFaces(s))
						total += meshImage.quantifySignalProportion(f);
					
					LOGGER.fine("Sector: "+total);
					Object[] rowData = { k1, s.getName(),  df.format(total) };
					compModel.addRow(rowData);
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
