package com.bmskinner.nuclear_morphology.gui.tabs.signals.warping;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.analysis.image.MultiScaleStructuralSimilarityIndex;
import com.bmskinner.nuclear_morphology.analysis.image.MultiScaleStructuralSimilarityIndex.MSSIMScore;
import com.bmskinner.nuclear_morphology.analysis.mesh.DefaultMesh;
import com.bmskinner.nuclear_morphology.analysis.mesh.DefaultMeshImage;
import com.bmskinner.nuclear_morphology.analysis.mesh.Mesh;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshCreationException;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshImage;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.tabs.signals.warping.SignalWarpingModel.ImageCache.WarpedImageKey;

import ij.process.ImageProcessor;

public class StructuralSimilarityComparisonDialog extends JDialog {
	
	private static final String DIALOG_TITLE = "MS-SSIM* scores";
	
	private SignalWarpingModel model;
	
	public StructuralSimilarityComparisonDialog(SignalWarpingModel model) {
		this.model = model;
		TableModel compModel = createTableModel();
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

	private TableModel createTableModel() {
		DecimalFormat df = new DecimalFormat("0.000");
		DefaultTableModel compModel = new DefaultTableModel();
		MultiScaleStructuralSimilarityIndex msi = new MultiScaleStructuralSimilarityIndex();
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
					MSSIMScore score = msi.calculateMSSIM(ip1, ip2);

					Object[] reverseData = { k2, k1, df.format(score.luminance), df.format(score.contrast),  df.format(score.structure),  df.format(score.msSsimIndex) };
					if(!containsRow(reverseData, compModel)) {
						Object[] rowData = { k1, k2, df.format(score.luminance), df.format(score.contrast),  df.format(score.structure),  df.format(score.msSsimIndex) };
						compModel.addRow(rowData);
					}
				}
			}
		}
		return compModel;
	}
	
	private boolean containsRow(Object[] rowData, TableModel tableModel) {
		for(int r=0; r<tableModel.getRowCount(); r++) {
			boolean matches = true;
			for(int c=0; c<tableModel.getColumnCount(); c++)
				matches &= tableModel.getValueAt(r, c).equals(rowData[c]);
			if(matches)
				return true;
		}
		return false;
	}
	
	private Map<String, List<MSSIMScore>> calculatePerCellMSSSIMs() {
		MultiScaleStructuralSimilarityIndex msi = new MultiScaleStructuralSimilarityIndex();
		Map<String, List<MSSIMScore>> scores = new HashMap();
		for(IAnalysisDataset d : model.getTemplates()) {

			Mesh<Nucleus> meshConsensus;
			try {
				meshConsensus = new DefaultMesh<Nucleus>(d.getCollection().getConsensus());
			} catch (MeshCreationException e2) {
				return scores;
			}

			Rectangle r = meshConsensus.toPath().getBounds();

			for(Nucleus n : d.getCollection().getNuclei()) {
				if(n.getSignalCollection().getSignalGroupIds().size()==2) {
					List<UUID> signalIds = new ArrayList<>(n.getSignalCollection().getSignalGroupIds());
					ImageProcessor ip1 = generateNucleusImage(meshConsensus, r.width, r.height, n, signalIds.get(0));
					ImageProcessor ip2 = generateNucleusImage(meshConsensus, r.width, r.height, n, signalIds.get(1));
					MSSIMScore score =  msi.calculateMSSIM(ip1, ip2);
					
					String key = d.getName()+"_"+signalIds.get(0).toString()+"_"+signalIds.get(1).toString();
					if(!scores.containsKey(key))
						scores.put(key, new ArrayList<>());
					scores.get(key).add(score);
				}
			}
		}
		return scores;
	}
	
	private ImageProcessor generateNucleusImage(@NonNull Mesh<Nucleus> meshConsensus, int w, int h, @NonNull Nucleus n, UUID signalGroup) {

		try {
			Mesh<Nucleus> cellMesh = new DefaultMesh<>(n, meshConsensus);

		    // Get the image with the signal
		    ImageProcessor ip;
		    if(n.getSignalCollection().hasSignal(signalGroup)){ // if there is no signal, getImage will throw exception
		    	ip = n.getSignalCollection().getImage(signalGroup);
		    	ip.invert();
		    } else {
		    	return ImageFilterer.createBlackByteProcessor(w, h);
		    }

		    MeshImage<Nucleus> meshImage = new DefaultMeshImage<>(cellMesh, ip);

		    // Draw NucleusMeshImage onto consensus mesh.
		    return meshImage.drawImage(meshConsensus);

		} catch (Exception e) {
			return ImageFilterer.createBlackByteProcessor(w, h);
		} 
	}

}
