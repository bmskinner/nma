package analysis.mesh;

import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

import logging.Loggable;
import analysis.mesh.NucleusMeshFace.NucleusMeshFaceCoordinate;
import components.generic.XYPoint;

/**
 * This is an image based on NucleusMeshFace coordinates.
 * @author bms41
 *
 */
public class NucleusMeshImage implements Loggable {
		
	private Map< NucleusMeshFace, Map<NucleusMeshFaceCoordinate, Integer> > map = new HashMap< NucleusMeshFace, Map<NucleusMeshFaceCoordinate, Integer> >(); 
	
	private NucleusMesh template;
	
	/**
	 * Create based on a template mesh and image. Each pixel within the 
	 * nucleus is converted to a mesh face coordinate.
	 * @param mesh
	 * @param ip
	 */
	public NucleusMeshImage(NucleusMesh mesh, ImageProcessor ip){
		template = mesh;
		makeFaceCoordinates(mesh, ip);
	}
	
	/**
	 * Draw the image in this object at the coordinates in the given mesh
	 * @param mesh
	 * @return
	 */
	public ImageProcessor meshToImage(NucleusMesh mesh){
		
		finer("Mesh has "+mesh.getFaceCount()+" faces");
		if( ! mesh.isComparableTo(template)){
			warn("Cannot compare meshes");
		}
		
		
		Rectangle r = mesh.nucleus.getBounds();
		if(r==null){
			warn("No bounding rectange in nucleus");
			warn("Using default bounds (200x200)");
			r = new Rectangle(200, 200); 
		}
		ImageProcessor ip = new ByteProcessor(r.width, r.height);
				
		for(NucleusMeshFace f : map.keySet()){
			
			Map<NucleusMeshFaceCoordinate, Integer> faceMap = map.get(f);
			
			
			for(NucleusMeshFaceCoordinate c : faceMap.keySet() ){
				
				int pixelValue = faceMap.get(c);
				
				XYPoint p = c.getPixelCoordinate(mesh.getFace(f));
				
				ip.set(p.getXAsInt(), p.getYAsInt(), pixelValue);
				
			}
			
		}
		return ip;
	}
	
	public String toString(){
		StringBuilder b = new StringBuilder();
		b.append("Nucleus mesh image \n");
		b.append("Image has "+map.keySet().size()+" faces\n");
		b.append("Listing faces:\n");
		for(NucleusMeshFace f : map.keySet()){
			
			Map<NucleusMeshFaceCoordinate, Integer> faceMap = map.get(f);
			b.append(f.toString()+"\n");
			b.append("Listing coordinates in face:\n");
			for(NucleusMeshFaceCoordinate c : faceMap.keySet() ){
				
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
	private void makeFaceCoordinates(NucleusMesh mesh, ImageProcessor ip){

		int missedCount = 0;
		
		for(int x=0; x<ip.getWidth(); x++){
			
			for(int y=0; y<ip.getHeight(); y++){
				XYPoint p = new XYPoint(x, y);

				if(mesh.nucleus.containsOriginalPoint(p)){
					
					if(mesh.hasFaceContaining(p)){
						int pixel = ip.get(x, y);
						NucleusMeshFace f = mesh.getFaceContaining(p);
					
						finer("Found face in target mesh for point "+p.toString());
						Map<NucleusMeshFaceCoordinate, Integer> faceMap = map.get(f);

						if(faceMap==null){ // create the facemap if not present
							map.put(f, new HashMap<NucleusMeshFaceCoordinate, Integer>());
							faceMap = map.get(f);
						}
						
						if(f==null){
							warn("Error fetching face from mesh at "+p.toString());
						}

						NucleusMeshFaceCoordinate c = f.getFaceCoordinate(p);
						faceMap.put(c, pixel);
					} else {
						finer("Cannot find face in target mesh for point "+p.toString() + "(Total "+missedCount+")");
						missedCount++;
					}
				}
				
			}
		}
		if(missedCount >0){
			finer("Faces could not be found for "+missedCount+" points");
			finer(mesh.toString());
			
		}
	}

}
