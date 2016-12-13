package com.bmskinner.nuclear_morphology.analysis.mesh;

import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.awt.Rectangle;
import java.util.HashMap;
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
		
	private Map< MeshFace, Map<MeshFaceCoordinate, Integer> > map = new HashMap<MeshFace, Map<MeshFaceCoordinate, Integer> >(); 
	
	private Mesh<Nucleus> template;
	
	/**
	 * Create based on a template mesh and image. Each pixel within the 
	 * nucleus is converted to a mesh face coordinate.
	 * @param mesh
	 * @param ip the image to fetch pixels from
	 */
	public NucleusMeshImage(Mesh<Nucleus> mesh, ImageProcessor ip){
		template = mesh;
		makeFaceCoordinates(mesh, ip);
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.MeshImage#meshToImage(com.bmskinner.nuclear_morphology.analysis.mesh.NucleusMesh)
	 */
	@Override
	public ImageProcessor createImage(Mesh<Nucleus> mesh) throws UncomparableMeshImageException{
		
		finer("Mesh has "+mesh.getFaceCount()+" faces");
		if( ! mesh.isComparableTo(template)){
			throw new UncomparableMeshImageException("Meshes do not match");
		}
		
						
		Rectangle r = mesh.toPath().getBounds();
		finest("Target mesh bounds are "+r.getWidth()+" x "+r.getHeight()+"  : "+r.getX()+", "+r.getY());

		
		// The new image size
		int w = r.width  ;
		int h = r.height ;
		
		// Find the centre of each axis in the bounding rectangle
		int xCentre = w >>1;
		int yCentre = h >>1;
		
		int xBase = (int) mesh.toPath().getBounds().getX();
		int yBase = (int) mesh.toPath().getBounds().getY();
		
		finest("New image dimensions are "+w+" x "+h);
		finest("New image centre point is at "+xCentre+", "+yCentre);

		// Create a blank image processor with an appropriate size
		// to hold the new image. Note that if the height or width is
		// not sufficient, the image will wrap
		ImageProcessor ip = createWhiteProcessor(w, h);
				
		// Adjust from absolute position in original target image
		// Note that the consensus will have a position from it's template nucleus

		finest("Target nucleus original x,y base in image is "+xBase+", "+yBase);
		finest("The pixels should be moved by: -"+xBase+", -"+yBase);
		int missingPixels = 0;
		for(MeshFace f : map.keySet()){
			
			// Fetch the equivalent face in the target mesh
			MeshFace targetFace = mesh.getFace(f);

			missingPixels += addFaceToImage(f, targetFace, ip, -xBase, -yBase);
			
		}
		
		if(missingPixels >0){
			fine(missingPixels+" points lay outside the new image bounds");			
		}
		
		interpolateMissingPixels(ip);
		
		return ip;
	}
	
	/**
	 * Add the pixels in the given face to an image processor. 
	 * @param f
	 * @param mesh
	 * @param ip
	 * @param xOffset
	 * @param yOffset
	 * @return
	 */
	private int addFaceToImage(MeshFace templateFace, MeshFace targetFace, ImageProcessor ip, int xOffset, int yOffset){
		
		int missingPixels = 0;
		Map<MeshFaceCoordinate, Integer> faceMap = map.get(templateFace);
		finest("Getting pixels from face");
		
		for(MeshFaceCoordinate c : faceMap.keySet() ){
			
			
			int pixelValue = faceMap.get(c);
			finest(c.toString()+" : Value: "+pixelValue);
			IPoint p = c.getPixelCoordinate(targetFace);

			
			int x = p.getXAsInt() + xOffset;
			int y = p.getYAsInt() + yOffset;
			
			finest("Coordinate in target face is "+p.toString());
			finest("Moving point to "+x+", "+y);
			// Handle array out of bounds errors from consensus nuclei. 
			// This is because the consensus has -ve x and y positions			
			try {
				ip.set(x, y, pixelValue);
				finest("Pixel set at "+x+", "+y);
			} catch (ArrayIndexOutOfBoundsException e){
				finer("Point outside image bounds: "+x+", "+y);
				missingPixels++;
			}
			
		}
		return missingPixels;
	}
	
	/**
	 * Get the size of the resulting image for the given mesh
	 * @param mesh
	 * @return
	 */
	private int[] findImageDimensions(Mesh<Nucleus> mesh){
		
		int maxX = 0;
		int minX = Integer.MAX_VALUE;
		
		int maxY = 0;
		int minY = Integer.MAX_VALUE;
		
		for(MeshFace f : map.keySet()){
			
			Map<MeshFaceCoordinate, Integer> faceMap = map.get(f);

			for(MeshFaceCoordinate c : faceMap.keySet() ){

				IPoint p = c.getPixelCoordinate(mesh.getFace(f));
				
				
				maxX = p.getXAsInt() > maxX ? p.getXAsInt() : maxX;
				minX = p.getXAsInt() < minX ? p.getXAsInt() : minX;
				
				maxY = p.getYAsInt() > maxY ? p.getYAsInt() : maxY;
				minY = p.getYAsInt() < minY ? p.getYAsInt() : minY;
						
			}
			
		}
		
		int xRange = maxX - minX;
		int yRange = maxY - minY;
		int[] result =  {xRange, yRange};
		return result;
	}
	
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
			
			Map<MeshFaceCoordinate, Integer> faceMap = map.get(f);
			b.append(f.toString()+"\n");
			b.append("Listing coordinates in face:\n");
			for(MeshFaceCoordinate c : faceMap.keySet() ){
				
				int pixelValue = faceMap.get(c);
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
	 */
	private void makeFaceCoordinates(Mesh<Nucleus> mesh, ImageProcessor ip){

		int missedCount = 0;
		
		for(int x=0; x<ip.getWidth(); x++){
			
			for(int y=0; y<ip.getHeight(); y++){
				IPoint p = IPoint.makeNew(x, y);

				if(mesh.getComponent().containsOriginalPoint(p)){
					
					if(mesh.contains(p)){
						int pixel = ip.get(x, y);
						MeshFace f = mesh.getFace(p);
					
						finest("Found face in target mesh for point "+p.toString());
						Map<MeshFaceCoordinate, Integer> faceMap = map.get(f);

						if(faceMap==null){ // create the facemap if not present
							map.put(f, new HashMap<MeshFaceCoordinate, Integer>());
							faceMap = map.get(f);
						}
						
						if(f==null){
							fine("Error fetching face from mesh at "+p.toString());
						}

						MeshFaceCoordinate c = f.getFaceCoordinate(p);
						faceMap.put(c, pixel);
					} else {
						missedCount++;
						finest("Cannot find face in target mesh for point "+p.toString() + " (Total "+missedCount+")");
						
					}
				}
				
			}
		}
		if(missedCount >0){
			fine("Faces could not be found for "+missedCount+" points");
			finer(mesh.toString());
			
		}
	}

}
