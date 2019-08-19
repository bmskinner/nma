/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.analysis.mesh;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;

import ij.process.ImageProcessor;

/**
 * This is an image based on NucleusMeshFace coordinates.
 * 
 * @author bms41
 * @param <E>
 *
 */
public class DefaultMeshImage<E extends CellularComponent> implements MeshImage<E> {
	
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    final private Map<MeshFace, List<MeshPixel>> map = new HashMap<>();

    final private Mesh<E> template;

    /**
     * Create based on a template mesh and image. Each pixel within the template
     * is converted to a mesh face coordinate.
     * 
     * @param mesh
     * @param ip the image to fetch pixels from
     * @throws MeshImageCreationException
     */
    public DefaultMeshImage(@NonNull final Mesh<E> mesh, @NonNull final ImageProcessor ip) throws MeshImageCreationException {
        template = mesh;

        // Create MeshPixels from the image processor for the region described
        // by the mesh
        makeFaceCoordinates(ip);
    }

    @Override
    public List<MeshPixel> getMeshPixels(@NonNull MeshFace f) {
        if (!template.contains(f))
            throw new IllegalArgumentException("Face is not present within template mesh");

        MeshFace target = template.getFace(f); // ensure we have the exact face in the template
        return map.get(target);
    }


    @Override
    public ImageProcessor drawImage(@NonNull Mesh<E> mesh) throws UncomparableMeshImageException {

        if (!mesh.isComparableTo(template))
            throw new UncomparableMeshImageException("Meshes do not match");
        
        LOGGER.finest( "Drawing image onto mesh");

        Rectangle r = mesh.toPath().getBounds();

        // The new image size
        int w = r.width;
        int h = r.height;

        int xBase = (int) mesh.toPath().getBounds().getX();
        int yBase = (int) mesh.toPath().getBounds().getY();

        ImageProcessor ip = ImageFilterer.createBlackByteProcessor(w, h);

        // Adjust from absolute position in original target image
        // Note that the consensus will have a position from its template
        // nucleus
        int missingPixels = 0;
        int missingFaces = 0;
        for (MeshFace templateFace : map.keySet()) {

            // Fetch the equivalent face in the target mesh
            MeshFace targetFace = mesh.getFace(templateFace);

            if (targetFace == null) {
                LOGGER.finer( "Cannot find template face in target mesh");
                missingFaces++;
                continue;
            }

            missingPixels += drawFaceToImage(templateFace, targetFace, ip, -xBase, -yBase);

        }

        LOGGER.finest( missingFaces + " faces could not be found in the target mesh");
        LOGGER.finest( missingPixels + " points lay outside the new image bounds");

        interpolateMissingPixels(ip);

        return ip;
    }

    /**
     * Add the pixels in the given face to an image processor.
     * 
     * @param templateFace
     * @param targetFace
     * @param ip
     * @param xOffset
     * @param yOffset
     * @return
     */
    private int drawFaceToImage(MeshFace templateFace, MeshFace targetFace, ImageProcessor ip, int xOffset,
            int yOffset) {

        int missingPixels = 0;

        List<MeshPixel> faceMap = map.get(templateFace);
        LOGGER.finest( "Found " + faceMap.size() + " pixels in face");

        for (MeshPixel c : faceMap) {
            int pixelValue = c.getValue();
            LOGGER.finest( "\t" + c.toString());
            IPoint p = c.getCoordinate().getCartesianCoordinate(targetFace);

            int x = p.getXAsInt() + xOffset;
            int y = p.getYAsInt() + yOffset;

            LOGGER.finest( "\tCoordinate in target face is " + p.toString());
            LOGGER.finest( "\tMoving point to " + x + ", " + y);
            // Handle array out of bounds errors from consensus nuclei.
            // This is because the consensus has -ve x and y positions
            try {
                ip.set(x, y, pixelValue);
            } catch (ArrayIndexOutOfBoundsException e) {
                LOGGER.finest( "\tPoint outside image bounds: " + x + ", " + y);
                missingPixels++;
            }

        }
        return missingPixels;
    }

    /**
     * Find black pixels surrounded by filled pixels, and set them to the
     * average value. Must have <=3 black pixels touching.
     * 
     * @param ip
     */
    private void interpolateMissingPixels(ImageProcessor ip) {

        for (int x = 0; x < ip.getWidth(); x++) {
            for (int y = 0; y < ip.getHeight(); y++) {

                if (ip.get(x, y) > 0)
                    continue; // skip non-black pixels

                int black = countSurroundingBlackPixels(x, y, ip);

                // We can't interpolate unless there are a decent number of valid pixels
                if (black <= 3) {
                    // interpolate from not black pixels
                    int pixelsToUse = 0;
                    int pixelValue = 0;

                    for (int i = x - 1; i <= x + 1; i++) {
                        for (int j = y - 1; j <= y + 1; j++) {

                            int value = 0;
                            try {
                                value = ip.get(i, j);
                            } catch (ArrayIndexOutOfBoundsException e) {
                                continue;
                            }
                            if (value > 0) {
                                pixelsToUse++;
                                pixelValue += value;
                            }
                        }
                    }

                    if (pixelsToUse > 0) {
                        int newValue = pixelValue / pixelsToUse;
                        ip.set(x, y, newValue);
                    }

                }

            }
        }

    }

    /**
     * Count the number of black pixels within the 8-connected pixels surrounding the given pixel
     * 
     * @param x the x position
     * @param y the y position
     * @param ip the image
     * @return the number of 8-connected pixels with value of zero
     */
    private int countSurroundingBlackPixels(int x, int y, ImageProcessor ip) {

        int black = 0;
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                if (i == x && j == y)
                    continue;
                try {
                    if (ip.get(i, j) == 0)
                    	black++;
                } catch (ArrayIndexOutOfBoundsException e) {
                    continue;
                }
            }
        }
        return black;
    }

    @Override
	public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("Nucleus mesh image \n");
        b.append("Image has " + map.keySet().size() + " faces\n");
        b.append("Listing faces:\n");
        for (MeshFace f : map.keySet()) {

            List<MeshPixel> faceMap = map.get(f);
            b.append(f.toString() + "\n");
            b.append("Listing coordinates in face:\n");
            for (MeshPixel c : faceMap) {

                int pixelValue = c.getValue();
                b.append(c.toString() + pixelValue + "\n");
            }

        }
        return b.toString();
    }

    /**
     * Given an image, find the pixels within the nucleus, and convert them to
     * face coordinates.
     * 
     * @param ip the image
     * @throws MeshImageCreationException
     */
    private void makeFaceCoordinates(final ImageProcessor ip) throws MeshImageCreationException {

        int missedCount = 0;

        LOGGER.finest( "Creating MeshPixels for the template mesh based on the image processor");

        // Add an empty list of MeshPixels to each face
        for (MeshFace face : template.getFaces()) {
            map.put(face, new ArrayList<>());
        }

        Rectangle bounds = template.getComponent().toOriginalShape().getBounds();

        for (int x = 0; x < ip.getWidth(); x++) {
            if (x < bounds.getMinX() || x > bounds.getMaxX()) // skip pixels outside the object bounds
                continue;

            for (int y = 0; y < ip.getHeight(); y++) {

                if (y < bounds.getMinY() || y > bounds.getMaxY()) {
                    continue;
                }

                // The pixel
                IPoint pixel = IPoint.makeNew(x, y);
                if (!template.getComponent().containsOriginalPoint(pixel))
                    continue;

                if (!template.contains(pixel)) {
                    missedCount++;
                    continue;
                }

                MeshFace face = template.getFace(pixel); // the face containing
                                                         // the pixel

                if (face == null) {
                    missedCount++;
                    continue;
                }

                // The ImageJ ByteProcessor assumes unsigned values 0-255 in
                // image processing,
                // but the Java type 'byte' is signed, with a range -128...+127.
                // It's the same for a ShortProcessor, there you have to use a
                // mask of 0xffff.
                // Apparently also true of a ColorProcessor (32 bit RGB
                // processor), since imported converted greyscale
                // images fail without this conversion

                int value = ip.get(x, y);

                if (value < 0)
                    value = value & 0xff;

                if (value < 0)
                    throw new MeshImageCreationException("Pixel value is negative at " + x + ", " + y + ": " + value);

                List<MeshPixel> pixels = map.get(face);

                try {

                    try {
                        MeshFaceCoordinate c = face.getFaceCoordinate(pixel);
                        pixels.add(new DefaultMeshPixel(c, value));
                    } catch (IllegalArgumentException e) {
                        throw new MeshImageCreationException("Pixel value is negative");
                    }

                } catch (PixelOutOfBoundsException e) {
                    missedCount++;
                }

            }
        }

        if (missedCount > 0) {
            LOGGER.finest( "Faces could not be found for " + missedCount + " points");
        }
    }

    @Override
    public double quantifySignalProportion(@NonNull MeshFace f) {
    	long total = calculateTotalPixelIntensity();
    	long faceTotal = 0;
    	List<MeshPixel> faceMap = map.get(f);
    	for (MeshPixel c : faceMap)
    		faceTotal += c.getValue();

    	double fraction = ((double)faceTotal/(double)total);
    	LOGGER.fine("Face: "+fraction+" of "+total);
    	return fraction;
    }
	
	private long calculateTotalPixelIntensity() {
		long total = 0;
		for(MeshFace f : map.keySet())
			for (MeshPixel c : map.get(f))
				total += c.getValue();
		return total;
	}

}
