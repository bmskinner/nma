package com.bmskinner.nuclear_morphology.gui.tabs.cells_detail;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.analysis.image.AbstractImageFilterer;
import com.bmskinner.nuclear_morphology.analysis.image.ImageAnnotator;
import com.bmskinner.nuclear_morphology.analysis.mesh.DefaultMesh;
import com.bmskinner.nuclear_morphology.analysis.mesh.DefaultMeshImage;
import com.bmskinner.nuclear_morphology.analysis.mesh.Mesh;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshCreationException;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshImage;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshImageCreationException;
import com.bmskinner.nuclear_morphology.analysis.mesh.UncomparableMeshImageException;
import com.bmskinner.nuclear_morphology.charting.image.MeshAnnotator;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.Imageable;
import com.bmskinner.nuclear_morphology.components.Statistical;
import com.bmskinner.nuclear_morphology.components.generic.BorderTagObject;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.DatasetEventHandler;
import com.bmskinner.nuclear_morphology.gui.EventListener;
import com.bmskinner.nuclear_morphology.gui.components.BorderTagEvent;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.process.ImageProcessor;

/**
 * Show annotated cell images, and allow selection of tags
 * or other elements of the cell.
 * 
 * The 'bulging' code was adapted from https://stackoverflow.com/questions/22824041/explanation-for-the-bulge-effect-algorithm
 * @author ben
 * @since 1.14.0
 *
 */
public class InteractiveAnnotatedCellPanel extends JPanel implements Loggable {

	private JLabel imageLabel;
	
	private DatasetEventHandler dh = new DatasetEventHandler(this);
	
	private IAnalysisDataset dataset = null;
	private ICell cell = null;
	private CellularComponent component = null;
	private boolean isShowMesh;
	private boolean isWarpImage;
	
	private BufferedImage output;
    private double bulgeStrength = 1.0;
    private double bulgeRadius = 50;

	public InteractiveAnnotatedCellPanel(){
		setLayout(new BorderLayout());
		imageLabel = new JLabel();
		imageLabel.setHorizontalAlignment(JLabel.CENTER);
		imageLabel.setVerticalAlignment(JLabel.CENTER);
		imageLabel.setHorizontalTextPosition(JLabel.CENTER);
		imageLabel.setVerticalTextPosition(JLabel.CENTER);
		add(imageLabel, BorderLayout.CENTER);
	}

	/**
	 * Set the panel to a null state with no cell showing
	 */
	public void setNull() {
		setCell(null, null, null, false, false);
	}

	public void setCell(@Nullable IAnalysisDataset dataset, @Nullable ICell cell, @Nullable CellularComponent component, boolean isShowMesh, boolean isWarpImage) {
		if(dataset==null || cell==null || component==null) {
			imageLabel.setIcon(null);
			return;
		}
		this.dataset     = dataset;
		this.cell        = cell;
		this.component   = component;
		this.isShowMesh  = isShowMesh;
		this.isWarpImage = isWarpImage;
		createImage();
	}
	
	@Override
	public void repaint() {
		super.repaint();
//		createImage(); // ensure the imaage is always scaled properly to the panel
	}
	
	public synchronized void addDatasetEventListener(EventListener l) {
        dh.addListener(l);
    }

	public synchronized void removeDatasetEventListener(EventListener l) {
        dh.removeListener(l);
    }
	
	private void createImage() {
		if(isShowMesh) {
			createMeshImage();
			return;
		}
		if(isWarpImage) {
			createWarpImage();
			return;
		}
		createCellImage();
	}
	
	@Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }
	
	/**
	 * Create the default annotated cell image, with border, segments and border tags highlighted
	 * @param dataset
	 * @param cell
	 * @param component
	 */
	private void createCellImage() {
		ThreadManager.getInstance().submit(() ->{
			output= null;
			ImageProcessor ip;
			try{
				ip = component.getImage();
			} catch(UnloadableImageException e){
				ip = AbstractImageFilterer.createBlankColorProcessor( 1500, 1500); //TODO make based on cell location
			}

			ImageAnnotator an = new ImageAnnotator(ip);

			if(cell.hasCytoplasm()){
				an.crop(cell.getCytoplasm());
			} else{
				an.crop(cell.getNuclei().get(0));
			}
			ImageAnnotator an2 = new ImageAnnotator(an.toProcessor(), getWidth(), getHeight());

			for(Nucleus n : cell.getNuclei()){
				an2.annotateCroppedNucleus(n.duplicate());
			}    
			
			imageLabel.setIcon(an2.toImageIcon());
			
			
			for(MouseListener l : imageLabel.getMouseListeners()) {
				imageLabel.removeMouseListener(l);
			}
			for(MouseMotionListener l : imageLabel.getMouseMotionListeners()) {
				imageLabel.removeMouseMotionListener(l);
			}
			
			imageLabel.addMouseMotionListener(new MouseAdapter() {

				private final BufferedImage input = an2.toProcessor().getBufferedImage();
				
				@Override
				public void mouseMoved(MouseEvent e){
					
					// The rescaled dimensions
					int iconWidth = imageLabel.getIcon().getIconWidth();
					int iconHeight = imageLabel.getIcon().getIconHeight();
					
					// The image panel dimensions
					int panelWidth = getWidth();
					int panelHeight = getHeight();
					
					// The position of the click relative to the icon
					int iconX = e.getX()-((panelWidth-iconWidth)/2);
					int iconY = e.getY()-((panelHeight-iconHeight)/2);
					updateImage(iconX, iconY);
				}
				
				private void updateImage(int x, int y) {
					if (output == null) 
						output = new BufferedImage( input.getWidth(), input.getHeight(),  BufferedImage.TYPE_INT_ARGB);
					
					computeBulgeImage(input, x, y, bulgeStrength, bulgeRadius, output);
					imageLabel.setIcon(new ImageIcon(output));
					repaint();
					
				}
			});

			imageLabel.addMouseListener(new MouseAdapter() {
				
				// the undistorted image
				

				private IPoint getPositionInComponent(MouseEvent e) {
					// The original image dimensions
					int w = an.toProcessor().getWidth();
					int h = an.toProcessor().getHeight();
					
					// The rescaled dimensions
					int iconWidth = imageLabel.getIcon().getIconWidth();
					int iconHeight = imageLabel.getIcon().getIconHeight();
					
					// The image panel dimensions
					int panelWidth = getWidth();
					int panelHeight = getHeight();
					
					// The position of the click relative to the icon
					int iconX = e.getX()-((panelWidth-iconWidth)/2);
					int iconY = e.getY()-((panelHeight-iconHeight)/2);
					
					// The position  of the click within the original image
					double xPositionInImage = (((double)iconX/(double) iconWidth)*w)-Imageable.COMPONENT_BUFFER;
					double yPositionInImage = (((double)iconY/(double) iconHeight)*h)-Imageable.COMPONENT_BUFFER;
					return IPoint.makeNew(xPositionInImage, yPositionInImage);
				}
				
				private void updateTag(Tag tag, int newIndex) {
					boolean wasLocked = cell.getNucleus().isLocked();
					cell.getNucleus().setLocked(false);

					cell.getNucleus().setBorderTag(tag, newIndex);
					cell.getNucleus().updateVerticallyRotatedNucleus();

					if(tag.equals(Tag.ORIENTATION_POINT) || tag.equals(Tag.REFERENCE_POINT)) {
						cell.getNucleus().setStatistic(PlottableStatistic.OP_RP_ANGLE, Statistical.STAT_NOT_CALCULATED);
					}
					cell.getNucleus().updateDependentStats();
					cell.getNucleus().setLocked(wasLocked);
					dataset.getCollection().clear(PlottableStatistic.OP_RP_ANGLE, CellularComponent.NUCLEUS);
					dh.fireDatasetEvent(DatasetEvent.REFRESH_CACHE, dataset);
				}

				private JPopupMenu createPopup(IBorderPoint point) {
					List<Tag> tags = dataset.getCollection().getProfileCollection().getBorderTags();
					JPopupMenu popupMenu = new JPopupMenu("Popup");
					Collections.sort(tags);

					for (Tag tag : tags) {

						if (tag.equals(Tag.INTERSECTION_POINT))
							continue; // The IP is determined solely by the OP

						JMenuItem item = new JMenuItem(tag.toString());

						item.addActionListener(a -> {
							int index = cell.getNucleus().getBorderIndex(point);
							updateTag(tag, index);
							repaint();
						});
						popupMenu.add(item);
					}

					// Find border tags with rulesets that have not been assigned in the median
					List<Tag> unassignedTags = new ArrayList<Tag>();
					for (Tag tag : BorderTagObject.values()) {
						if (tag.equals(Tag.INTERSECTION_POINT))
							continue;
						if (!tags.contains(tag)) {
							unassignedTags.add(tag);
						}
					}

					if (!unassignedTags.isEmpty()) {
						Collections.sort(unassignedTags);

						popupMenu.addSeparator();

						for (Tag tag : unassignedTags) {
							JMenuItem item = new JMenuItem(tag.toString());
							item.setForeground(Color.DARK_GRAY);

							item.addActionListener(a -> {
								int index = cell.getNucleus().getBorderIndex(point);
								updateTag(tag, index);
							});
							popupMenu.add(item);
						}
					}
					return popupMenu;
				}

				@Override
				public void mouseClicked(MouseEvent e) {

					IPoint clickedPoint = getPositionInComponent(e);
//					System.out.println(String.format("Mouse clicked at %s - %s ", e.getX(), e.getY()));

					Optional<IBorderPoint> point = cell.getNucleus().getBorderList()
							.stream().filter(p->clickedPoint.overlaps(p))
							.findFirst();

					if(point.isPresent()) {
//						System.out.println(String.format("Border point overlaps at %s ", point.get().toString()));
						JPopupMenu popup = createPopup(point.get());
						popup.show(imageLabel, e.getX(), e.getY());
					}

				}

			});

		});
	}

	private void createMeshImage() {
		ThreadManager.getInstance().submit(() ->{
			try {
				
				ImageProcessor ip;
				try{
					ip = component.getImage();
				} catch(UnloadableImageException e){
					ip = AbstractImageFilterer.createBlankColorProcessor( 1500, 1500); //TODO make based on cell location
				}
				ImageAnnotator an = new ImageAnnotator(ip);

				if(cell.hasCytoplasm()){
					an.crop(cell.getCytoplasm());
				} else{
					an.crop(cell.getNuclei().get(0));
				}
				
				Mesh<Nucleus> consensusMesh = new DefaultMesh(dataset.getCollection().getConsensus());
				for(Nucleus n : cell.getNuclei()) {
					Mesh<Nucleus> m = new DefaultMesh(n, consensusMesh);
					Mesh<Nucleus> compMesh = m.comparison(consensusMesh);
					MeshAnnotator an3 = new MeshAnnotator( an.toProcessor(), getWidth(), getHeight(), compMesh);
					an3.annotateNucleusMeshEdges();
					imageLabel.setIcon(an3.toImageIcon());
				}
			} catch (MeshCreationException | IllegalArgumentException e) {
				stack("Error making mesh or loading image", e);
				setNull();
			}
		});
	}
	
	private void createWarpImage() {
		ThreadManager.getInstance().submit(() ->{
			try {
				
				ImageProcessor ip;
				try{
					ip = component.getImage();
				} catch(UnloadableImageException e){
					ip = AbstractImageFilterer.createBlankColorProcessor( 1500, 1500); //TODO make based on cell location
				}
				ImageAnnotator an = new ImageAnnotator(ip);

				if(cell.hasCytoplasm()){
					an.crop(cell.getCytoplasm());
				} else{
					an.crop(cell.getNuclei().get(0));
				}
				
				Mesh<Nucleus> consensusMesh = new DefaultMesh(dataset.getCollection().getConsensus());
        		for(Nucleus n : cell.getNuclei()) {
        			Mesh<Nucleus> m = new DefaultMesh(n, consensusMesh);
        			MeshImage im = new DefaultMeshImage(m, ip.duplicate());
        			ImageProcessor drawn = im.drawImage(consensusMesh);
        			drawn.flipVertical();
        			an = new ImageAnnotator(drawn, getWidth(), getHeight());
        		}
        		imageLabel.setIcon(an.toImageIcon());
			} catch (MeshCreationException | IllegalArgumentException | MeshImageCreationException | UncomparableMeshImageException e) {
				stack("Error making mesh or loading image", e);
				setNull();
			}
		});
	}
	
	private static void computeBulgeImage(BufferedImage input, int cx, int cy, 
	        double bulgeStrength, double bulgeRadius, BufferedImage output){
	        int w = input.getWidth();
	        int h = input.getHeight();
	        for(int x = 0; x < w; x++) {
	            for(int y = 0; y < h; y++) {
	                int dx = x-cx;
	                int dy = y-cy;
	                double distanceSquared = dx * dx + dy * dy;;
	                int sx = x;
	                int sy = y;
	                if (distanceSquared < bulgeRadius * bulgeRadius) {
	                    double distance = Math.sqrt(distanceSquared);
	                    boolean otherMethod = false;
	                    otherMethod = true;
	                    if (otherMethod) {
	                        double r = distance / bulgeRadius;
	                        double a = Math.atan2(dy, dx);
	                        double rn = Math.pow(r, bulgeStrength)*distance; 
	                        double newX = rn*Math.cos(a) + cx; 
	                        double newY = rn*Math.sin(a) + cy;  
	                        sx += (newX - x);
	                        sy += (newY - y);
	                    }
	                    else  {
	                        double dirX = dx / distance;
	                        double dirY = dy / distance;
	                        double alpha = distance / bulgeRadius;
	                        double distortionFactor = 
	                            distance * Math.pow(1-alpha, 1.0 / bulgeStrength);
	                        sx -= distortionFactor * dirX;
	                        sy -= distortionFactor * dirY;
	                    }
	                }
	                if (sx >= 0 && sx < w && sy >= 0 && sy < h) {
	                    int rgb = input.getRGB(sx, sy);
	                    output.setRGB(x, y, rgb);
	                }
	            }
	        }
	    }
}
