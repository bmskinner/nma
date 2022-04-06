package com.bmskinner.nuclear_morphology.gui.painters;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.bmskinner.nuclear_morphology.components.ComponentOrienter;
import com.bmskinner.nuclear_morphology.components.Imageable;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.UnavailableBorderPointException;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;

public class OrientedCellImagePainter implements ImagePainter {
	
private static final Logger LOGGER = Logger.getLogger(CellImagePainter.class.getName());
	
	private ICell cell;
	
	/** The width of the image before resizsing */
	private double originalWidth;
	
	/**
	 * Create with cell to paint
	 * @param cell
	 */
	public OrientedCellImagePainter(ICell cell) {
		this.cell = cell;

		try { 
			// TODO: we need to calculate the new canvas width after rotation
			double nativeWidth = cell.getPrimaryNucleus().getWidth()+ (Imageable.COMPONENT_BUFFER*2);

			// The angle needed to rotate the image into orientation
			double angle = ComponentOrienter.calcAngleToAlignVertically(cell.getPrimaryNucleus());

			double orientedWidth = cell.getPrimaryNucleus().getOrientedNucleus().getWidth();

			this.originalWidth = cell.getPrimaryNucleus().getWidth()+ (Imageable.COMPONENT_BUFFER*2);
		} catch(MissingLandmarkException | ComponentCreationException e) {

		}
	}
	
	
	/**
	 * Paint the given cell over an input image
	 * @param input the image to paint on
	 * @param cell the cell to be painted
	 * @return a new image with the cell painted over the input image
	 */
	@Override
	public BufferedImage paint(BufferedImage input) {
		
		double ratio = input.getWidth()/originalWidth;
		
		BufferedImage output = new BufferedImage( input.getWidth(), input.getHeight(),  BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = output.createGraphics();
		g2.drawImage(input, 0, 0, null);
		
		Object saved = g2.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
        
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BICUBIC);

		g2.setStroke(new BasicStroke(3));
		
		
		try {
			Nucleus n = cell.getPrimaryNucleus().getOrientedNucleus();
			n.flipVertical(); // because image Y coordinates are reversed
			ISegmentedProfile sp = n.getProfile(ProfileType.ANGLE);

			List<IProfileSegment> segs = sp.getSegments();
			for (int i = 0; i < segs.size(); i++) {
				g2.setColor(ColourSelecter.getColor(i));
				
                IProfileSegment seg = segs.get(i);

                for (int j = 0; j <= seg.length(); j++) {
                    int k = n.wrapIndex(seg.getStartIndex() + j + n.getBorderIndex(Landmark.REFERENCE_POINT) -1);
                    IPoint p = n.getBorderPoint(k)
                    		.minus(n.getBase())
                    		.plus(CellularComponent.COMPONENT_BUFFER);
                                                            
                    double x = p.getX()*ratio;
                    double y = p.getY()*ratio;
                    g2.drawLine((int)x, (int)y, (int)x, (int)y);
                }

            }
			
			// Draw centre of mass
			IPoint com = n.getCentreOfMass().minus(n.getBase()).plus(CellularComponent.COMPONENT_BUFFER);
			double x = com.getX()*ratio;
            double y = com.getY()*ratio;
            g2.setColor(Color.PINK);
			g2.drawLine((int)x, (int)y, (int)x, (int)y);
			
			// Draw the landmarks
			g2.setStroke(new BasicStroke(5));
			
			for(Landmark lm : n.getLandmarks().keySet()) {
				IPoint lp = n.getBorderPoint(lm).minus(n.getBase()).plus(CellularComponent.COMPONENT_BUFFER);
				x = lp.getX()*ratio;
	            y = lp.getY()*ratio;
	            g2.setColor(ColourSelecter.getColour(lm));
				g2.drawLine((int)x, (int)y, (int)x, (int)y);
			}
			
			
		} catch (MissingProfileException | ProfileException | UnavailableBorderPointException | MissingLandmarkException | ComponentCreationException e) {
			LOGGER.log(Level.FINE, "Unable to paint cell", e);
		}

        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, saved);
		return output;
	}
	
	
	
	@Override
	public BufferedImage paintMagnified(BufferedImage smallInput, BufferedImage largeInput, int cx, int cy, 
			int smallRadius, int bigRadius){

		// the destination rectangle that will be painted to in the output
		int dx1 = cx-bigRadius;
		int dy1 = cy-bigRadius;
		int dx2 = cx+bigRadius;
		int dy2 = cy+bigRadius;

		// the source rectangle that will be sampled from the large input image
		double largeRatio = largeInput.getWidth()/smallInput.getWidth();
		int sx = (int) (cx*largeRatio);
		int sy = (int) (cy*largeRatio);
		
		int sx1 = sx-smallRadius; 
		int sy1 = sy-smallRadius;
		int sx2 = sx+smallRadius;
		int sy2 = sy+smallRadius;
		
		// Find the clicked point in the original cell image
		double ratio = smallInput.getWidth()/originalWidth;
		double cellX = cx/ratio;
		double cellY = cy/ratio;
		
		// Find the cell border point under the cursor 
		Optional<IPoint> point = cell.getPrimaryNucleus().getBorderList()
				.stream().
				map(p->p.minus(cell.getPrimaryNucleus().getBase()).plus(CellularComponent.COMPONENT_BUFFER))
				.filter(p->{
					return cellX>=p.getX()-0.4 && 
							cellX<=p.getX()+0.4 &&
							cellY>=p.getY()-0.4 && 
							cellY<=p.getY()+0.4;

				})
				.findFirst();

		// Create the output image, copying the small input
		BufferedImage output = new BufferedImage( smallInput.getWidth(), smallInput.getHeight(),  BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = output.createGraphics();
		g2.drawImage(smallInput, 0, 0, null);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		
		// Choose the region of the large image to copy
		g2.drawImage(largeInput, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
		
		
		// Save old colours
		Color c = g2.getColor();
		Stroke s = g2.getStroke();
		
		
		try {
			// Annotate the borders of the rectangle based on underlying content

			if(point.isPresent()) {
				g2.setColor(Color.CYAN);

				// Highlight the border depending on what border tags are present
				for(Landmark lm : cell.getPrimaryNucleus().getLandmarks().keySet()) {
					IPoint lp = cell.getPrimaryNucleus()
							.getBorderPoint(lm)
							.minus(cell.getPrimaryNucleus().getBase())
							.plus(CellularComponent.COMPONENT_BUFFER);
					if(cellX>=lp.getX()-0.4 && 
							cellX<=lp.getX()+0.4 &&
							cellY>=lp.getY()-0.4 && 
							cellY<=lp.getY()+0.4) {
						g2.setColor(ColourSelecter.getColour(lm));
					}
				}

				g2.setStroke(new BasicStroke(3));
			} else {
				g2.setColor(Color.BLACK);
				g2.setStroke(new BasicStroke(2));
			}
		} catch (MissingLandmarkException e) {
			// no action needed, colour remains cyan
		}

		g2.drawRect(dx1, dy1, bigRadius*2, bigRadius*2);

		g2.setColor(c);
		g2.setStroke(s);
		return output;
	}

}
