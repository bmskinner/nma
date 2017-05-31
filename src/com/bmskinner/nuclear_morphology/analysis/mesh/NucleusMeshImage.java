package com.bmskinner.nuclear_morphology.analysis.mesh;

import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * This is an image based on NucleusMeshFace coordinates.
 * @author bms41
 *
 */
public class NucleusMeshImage implements Loggable, MeshImage<Nucleus> {
	
	final private Map<MeshFace, List<MeshPixel>> map = new HashMap<MeshFace, List<MeshPixel>>();
	
	final private Mesh<Nucleus> template;
	
	/**
	 * Create based on a template mesh and image. Each pixel within the 
	 * nucleus is converted to a mesh face coordinate.
	 * @param mesh
	 * @param ip the image to fetch pixels from
	 * @throws MeshImageCreationException 
	 */
	public NucleusMeshImage(final Mesh<Nucleus> mesh, final ImageProcessor ip) throws MeshImageCreationException{
		template = mesh;
		
		// Create MeshPixels from the image processor for the region described by the mesh
		makeFaceCoordinates(ip);
	}
	
	@Override
	public List<MeshPixel> getMeshPixels(MeshFace f){
		if( ! template.contains(f)){
			throw new IllegalArgumentException("Face is not present within template mesh");
		}
		
		MeshFace target = template.getFace(f); // ensure we have the exact face in the template
		
		return map.get(target);
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.MeshImage#meshToImage(com.bmskinner.nuclear_morphology.analysis.mesh.NucleusMesh)
	 */
	@Override
	public ImageProcessor drawImage(Mesh<Nucleus> mesh) throws UncomparableMeshImageException{
		
		if( ! mesh.isComparableTo(template)){
			throw new UncomparableMeshImageException("Meshes do not match");
		}
		finer("Drawing image onto mesh");
						
		Rectangle r = mesh.toPath().getBounds();
		
		// The new image size
		int w = r.width  ;
		int h = r.height ;
				
		int xBase = (int) mesh.toPath().getBounds().getX();
		int yBase = (int) mesh.toPath().getBounds().getY();


		// Create a blank image processor with an appropriate size
		// to hold the new image. Note that if the height or width is
		// not sufficient, the image will wrap
		ImageProcessor ip = createWhiteProcessor(w, h);
				
		// Adjust from absolute position in original target image
		// Note that the consensus will have a position from its template nucleus

//		finest("Target nucleus original x,y base in image is "+xBase+", "+yBase);
//		finest("The pixels should be moved by: -"+xBase+", -"+yBase);
		int missingPixels = 0;
		int missingFaces  = 0;
		for(MeshFace templateFace : map.keySet()){
			
			// Fetch the equivalent face in the target mesh
			MeshFace targetFace = mesh.getFace(templateFace);
			
			if(targetFace == null){
				fine("Cannot find template face in target mesh");
				missingFaces++;
				continue;
			}
			
			
			missingPixels += drawFaceToImage(templateFace, targetFace, ip, -xBase, -yBase);
			
		}
		
		fine(missingFaces+" faces could not be found in the target mesh");	
		fine(missingPixels+" points lay outside the new image bounds");			
		
		
		interpolateMissingPixels(ip);
		
		return ip;
	}
	

	/**
	 * Add the pixels in the given face to an image processor. 
	 * @param templateFace
	 * @param targetFace
	 * @param ip
	 * @param xOffset
	 * @param yOffset
	 * @return
	 */
	private int drawFaceToImage(MeshFace templateFace, MeshFace targetFace, ImageProcessor ip, int xOffset, int yOffset){
				
		int missingPixels = 0;
		
		List<MeshPixel> faceMap = map.get(templateFace);
		finer("Found "+faceMap.size()+" pixels in face");
		
		for(MeshPixel c : faceMap ){
			
			finest("Pixel:");
			
			int pixelValue = c.getValue();
			finest("\t"+c.toString());
			IPoint p = c.getCoordinate().getCartesianCoordinate(targetFace);

			
			int x = p.getXAsInt() + xOffset;
			int y = p.getYAsInt() + yOffset;
			
			finest("\tCoordinate in target face is "+p.toString());
			finest("\tMoving point to "+x+", "+y);
			// Handle array out of bounds errors from consensus nuclei. 
			// This is because the consensus has -ve x and y positions			
			try {
				ip.set(x, y, pixelValue);
//				finest("\tPixel set at "+x+", "+y);
			} catch (ArrayIndexOutOfBoundsException e){
				finer("\tPoint outside image bounds: "+x+", "+y);
				missingPixels++;
			}
			
		}
		return missingPixels;
	}
	
//	/**
//	 * Get the size of the resulting image for the given mesh
//	 * @param mesh
//	 * @return
//	 */
//	private int[] findImageDimensions(Mesh<Nucleus> mesh){
//		
//		int maxX = 0;
//		int minX = Integer.MAX_VALUE;
//		
//		int maxY = 0;
//		int minY = Integer.MAX_VALUE;
//		
//		for(MeshFace f : map.keySet()){
//			
//			List<MeshPixel> faceMap = map.get(f);
//
//			for(MeshPixel c : faceMap ){
//
//				IPoint p = c.getCoordinate().getCartesianCoordinate(mesh.getFace(f));
//				
//				
//				maxX = p.getXAsInt() > maxX ? p.getXAsInt() : maxX;
//				minX = p.getXAsInt() < minX ? p.getXAsInt() : minX;
//				
//				maxY = p.getYAsInt() > maxY ? p.getYAsInt() : maxY;
//				minY = p.getYAsInt() < minY ? p.getYAsInt() : minY;
//						
//			}
//			
//		}
//		
//		int xRange = maxX - minX;
//		int yRange = maxY - minY;
//		int[] result =  {xRange, yRange};
//		return result;
//	}
	
	/**
	 * Make a ByteProcessor of the given dimensions with all
	 * pixels at 255
	 * @param w
	 * @param h
	 * @return
	 */
	private ImageProcessor createWhiteProcessor(int w, int h){
		ImageProcessor ip = new ByteProcessor(w, h);
		
		for(int i=0; i<ip.getPixelCount(); i++){
			ip.set(i, 255); // set all to white initially
		}
		return ip;
	}
	
	/**
	 * Find white pixels surrounded by filled pixels, and set
	 * them to the average value. Must have <=3 white pixels touching.
	 * @param ip
	 */
	private void interpolateMissingPixels(ImageProcessor ip){
		
		for(int x=0; x<ip.getWidth(); x++){
			
			for(int y=0; y<ip.getHeight(); y++){
				
				if(ip.get(x, y)<255){
					continue; // skip non-white pixels
				}
				
				int white = countSurroundingWhitePixels(x,y, ip);
				
				// We can't interpolate unless there are a decent number of 
				// valid pixels
				if(white <= 3){
//					log("Found "+white+" whites at "+x+", "+y);
					// interpolate from not white pixels
					
					int pixelsToUse = 0;
					int pixelValue  = 0;
					
					for(int i=x-1;i<=x+1;i++){
						
						for(int j=y-1;j<=y+1;j++){
							
							int value = 255;
							try{
								value = ip.get(i, j);
							} catch(ArrayIndexOutOfBoundsException e){
//								warn("Array out of bounds: "+i+", "+j);
								continue;
							}
							if(value<255){
								pixelsToUse++;
								pixelValue += value;
							}
						}
					}
//					log("\tUsable pixels: "+pixelsToUse);
					if(pixelsToUse>0){
						int newValue = pixelValue / pixelsToUse;
//						log("Interpolated to "+newValue);
						ip.set(x, y, newValue);
					}
					
				}
				
			}
		}
		
	}
	
	/**
	 * Count the number of white pixels surrounding the given pixel
	 * @param x
	 * @param y
	 * @param ip
	 * @return
	 */
	private int countSurroundingWhitePixels(int x, int y, ImageProcessor ip){
		
		int white = 0;
		for(int i=x-1;i<=x+1;i++){
			
			for(int j=y-1;j<=y+1;j++){
				
				if(i==x && j==y){
					continue;
				}
				
				try{
					if(ip.get(i, j)==255){
						white++;
					}
				} catch(ArrayIndexOutOfBoundsException e){
					continue;
				}
				
			}
			
		}
		return white;
	}
	
	public String toString(){
		StringBuilder b = new StringBuilder();
		b.append("Nucleus mesh image \n");
		b.append("Image has "+map.keySet().size()+" faces\n");
		b.append("Listing faces:\n");
		for(MeshFace f : map.keySet()){
			
			List<MeshPixel> faceMap = map.get(f);
			b.append(f.toString()+"\n");
			b.append("Listing coordinates in face:\n");
			for(MeshPixel c : faceMap ){
				
				int pixelValue = c.getValue();
				b.append(c.toString()+pixelValue+"\n");				
			}
			
		}
		return b.toString();
	}
	
	/**
	 * Given an image, find the pixels within the nucleus, and convert them to face
	 * coordinates. Return a map of the face coordinates and their pixel values
	 * @param ip
	 * @return
	 * @throws MeshImageCreationException 
	 */
	private void makeFaceCoordinates(final ImageProcessor ip) throws MeshImageCreationException{

		int missedCount = 0;
		
		fine("Creating MeshPixels for the template mesh based on the image processor");
		
		//Add an empty list of MeshPixels to each face
		for(MeshFace face : template.getFaces()){
			map.put(face, new ArrayList<MeshPixel>());
		}
		
		Rectangle bounds = template.getComponent().toOriginalShape().getBounds();
		
		for(int x=0; x<ip.getWidth(); x++){
			
			if(x<bounds.getMinX() || x>bounds.getMaxX()){
				continue;
			}
			
			for(int y=0; y<ip.getHeight(); y++){
				
				if(y<bounds.getMinY() || y>bounds.getMaxY()){
					continue;
				}
				
				// The pixel
				IPoint pixel = IPoint.makeNew(x, y);
//				finer("Image pixel "+pixel);

				if( ! template.getComponent().containsOriginalPoint(pixel)){
//					finer("\tTemplate component does not contain pixel "+pixel);
					continue;
				}
				
				if( ! template.contains(pixel)){
					missedCount++;
//					finer("\tTemplate mesh does not contain pixel "+pixel);
					continue;
				}
								
				MeshFace face = template.getFace(pixel); // the face containing the pixel

				if(face==null){
					missedCount++;
//					finer("\tTemplate mesh does not contain a face with pixel "+pixel);
					continue;
				}

				// The ImageJ ByteProcessor assumes unsigned values 0-255 in image processing, 
				// but the Java type 'byte' is signed, with a range -128...+127. 
				// It's the same for a ShortProcessor, there you have to use a mask of 0xffff.
				// Apparently also true of a ColorProcessor (32 bit RGB processor), since imported converted greyscale
				// images fail without this conversion

				int value = ip.get(x, y);
								
				if(value<0){
					
//					log(ip.getClass().getName());
					value = value&0xff;
				}
				
				if(value<0){
					throw new MeshImageCreationException("Pixel value is negative at "+x+", "+y+": "+value);
				}
				
				List<MeshPixel> pixels = map.get(face);


				try {
					
					try {
						MeshFaceCoordinate c = face.getFaceCoordinate(pixel);
						pixels.add( new DefaultMeshPixel(c, value) );
					} catch(IllegalArgumentException e){
						throw new MeshImageCreationException("Pixel value is negative");
					}

				} catch (PixelOutOfBoundsException e) {
					missedCount++;
//					finer("Cannot get coordinate within face: "+e.getMessage());
				}
				
			}
		}
		
		
		if(missedCount >0){
			fine("Faces could not be found for "+missedCount+" points");
			
		}
	}
	

}
