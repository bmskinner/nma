/*******************************************************************************
 *  	Copyright (C) 2015, 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details. Gluten-free. May contain 
 *     traces of LDL asbestos. Avoid children using heavy machinery while under the
 *     influence of alcohol.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.bmskinner.nuclear_morphology.analysis.mesh;

import com.bmskinner.nuclear_morphology.logging.Loggable;

public class NucleusMeshBuilder implements Loggable {

    public static final int DIVISION_LENGTH = 10;

    public NucleusMeshBuilder() {
    }

    /**
     * Create a mesh comparing the given nuclei
     * 
     * @param n1
     *            the first nucleus. Used as the mesh template
     * @param n2
     *            the comparison nucleus.
     * @param meshSize
     *            the distance between vertices
     * @return a mesh with the ratios between edge lengths
     * @throws Exception
     */
    // public NucleusMesh createComparisonMesh(Nucleus n1, Nucleus n2, int
    // meshSize) throws Exception{
    //
    // NucleusMesh n1Mesh = buildMesh(n1, meshSize);
    //
    // /*
    // * Ensure input nuclei have a best fit alignment
    // * TODO: determine size of mask
    // */
    // BooleanAligner aligner = new BooleanAligner(n1.getBooleanMask(200, 200));
    // int[] alignment = aligner.align(n2.getBooleanMask(200, 200));
    //
    // n2 = n2.duplicate();
    // n2.moveCentreOfMass( new XYPoint(alignment[BooleanAligner.Y],
    // alignment[BooleanAligner.X]));
    //
    // /*
    // * Create the mesh
    // */
    //
    // NucleusMesh n2Mesh = buildMesh(n2, n1Mesh);
    //
    // n1Mesh.makePairwiseEdges();
    // n2Mesh.makePairwiseEdges();
    //
    //
    // NucleusMesh result = n1Mesh.compare(n2Mesh);
    // return result;
    // }

    /**
     * Create a mesh for the given Nucleus, using the default mesh size
     * 
     * @param nucleus
     * @return
     * @throws Exception
     */
    // public NucleusMesh buildMesh(Nucleus nucleus) throws Exception{
    // return buildMesh(nucleus, DIVISION_LENGTH);
    // }

    /*
     * Go through each segment in the nucleus. Since different nuclei must be
     * compared, segment IDs are not useful here. Base on ordered segments from
     * profiles.
     * 
     * Find the segment length, and subdivide appropriately. Make a vertex.
     * 
     * Vertices should start at the CoM, then go to the reference point, then
     * around the perimeter.
     */
    // public NucleusMesh buildMesh(Nucleus nucleus, int meshSize) throws
    // Exception{
    // log(Level.FINEST, "Creating mesh for "+nucleus.getNameAndNumber());
    // NucleusMesh mesh = new NucleusMesh(nucleus);
    //
    //// mesh.addVertex(nucleus.getCentreOfMass(), false);
    //
    // List<NucleusBorderSegment> list =
    // nucleus.getProfile(ProfileType.REGULAR).getOrderedSegments();
    //
    // int segNumber = 0;
    // for(NucleusBorderSegment seg : list){
    //
    // int divisions = seg.length() / meshSize; // find the number of divisions
    // to make
    //
    // mesh.setDivision(segNumber++, divisions);
    // log(Level.FINEST, "Dividing segment into "+divisions+" parts");
    //
    // double proportion = 1d / (double) divisions;
    //
    // for(double d=0; d<1; d+=proportion){
    // int index = seg.getProportionalIndex(d);
    // log(Level.FINEST, "Fetching point at index "+index);
    // mesh.addVertex(nucleus.getBorderPoint(index), true);
    // }
    // }
    //
    // createEdges(mesh);
    //
    // mesh.makeCentreVertices();
    //
    // log(Level.FINEST, "Created mesh");
    // return mesh;
    // }

    /**
     * Build a mesh for the input nucleus, based on a template mesh containing
     * the divisions required for each segment
     * 
     * @param mesh
     * @return
     * @throws Exception
     */
    // public NucleusMesh buildMesh(Nucleus nucleus, NucleusMesh template)
    // throws Exception{
    // log(Level.FINEST, "Creating mesh for "+nucleus.getNameAndNumber()+" using
    // template "+template.getNucleusName());
    // NucleusMesh mesh = new NucleusMesh(nucleus);
    //
    //// log(Level.FINEST, "Adding centre of mass");
    //// mesh.addVertex(nucleus.getCentreOfMass(), false);
    //
    // log(Level.FINEST, "Getting ordered segments");
    // List<NucleusBorderSegment> list =
    // nucleus.getProfile(ProfileType.REGULAR).getOrderedSegments();
    //
    // log(Level.FINEST, "Checking counts");
    // if(template.getSegmentCount()!=list.size()){
    // log(Level.FINEST, "Segment counts not
    // equal:"+template.getSegmentCount()+" and "+list.size());
    // throw new IllegalArgumentException("Segment counts are not equal");
    // }
    // log(Level.FINEST, "Segment counts equal:"+template.getSegmentCount()+"
    // and "+list.size());
    //
    // int segNumber = 0;
    // log(Level.FINEST, "Iterating over segments");
    // for(NucleusBorderSegment seg : list){
    //
    // int divisions = template.getDivision(segNumber);
    // log(Level.FINEST, "Seg "+segNumber+": "+divisions);
    // segNumber++;
    //
    // log(Level.FINEST, "Dividing segment into "+divisions+" parts");
    //
    // double proportion = 1d / (double) divisions;
    //
    // for(double d=0; d<1; d+=proportion){
    // int index = seg.getProportionalIndex(d);
    // log(Level.FINEST, "Fetching point at index "+index);
    // mesh.addVertex(nucleus.getBorderPoint(index), true);
    // }
    // }
    //
    //
    // createEdges(mesh);
    // mesh.makeCentreVertices();
    //
    // log(Level.FINEST, "Created mesh");
    // return mesh;
    // }

    // private void createEdges(NucleusMesh mesh){
    //
    //
    //// log(Level.FINEST, "Linking edges to CoM");
    //// for(int i=1; i<mesh.getVertexCount(); i++){
    //// NucleusMeshEdge e = new NucleusMeshEdge(mesh.getVertex(0),
    // mesh.getVertex(i), 1);
    //// mesh.addInternalEdge(e);
    //// mesh.getVertex(0).addEdge(e);
    //// mesh.getVertex(i).addEdge(e);
    //// }
    //
    // log(Level.FINEST, "Linking border pairs");
    // for(int i=0, j=1; j<mesh.getVertexCount(); i++, j++){
    // NucleusMeshEdge e = new NucleusMeshEdge(mesh.getVertex(i),
    // mesh.getVertex(j), 1);
    // mesh.addPeripheralEdge( e );
    // mesh.getVertex(i).addEdge(e);
    // mesh.getVertex(j).addEdge(e);
    // }
    //
    // // Link the final perimeter point to the tip
    // NucleusMeshEdge e = new
    // NucleusMeshEdge(mesh.getVertex(mesh.getVertexCount()-1),
    // mesh.getVertex(0), 1);
    // mesh.addPeripheralEdge( e );
    // mesh.getVertex(mesh.getVertexCount()-1).addEdge(e);
    // mesh.getVertex(1).addEdge(e);
    //
    // log(Level.FINEST, "Created edges");
    // }

}
