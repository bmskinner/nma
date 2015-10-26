/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package analysis.tail;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.filter.Binary;
import ij.process.BinaryProcessor;
import ij.process.ByteProcessor;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import io.ImageImporter;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import mmorpho.MorphoProcessor;
import mmorpho.StructureElement;
import skeleton_analysis.AnalyzeSkeleton_;
import skeleton_analysis.Edge;
import skeleton_analysis.Graph;
import skeleton_analysis.SkeletonResult;
import utility.Constants;
import utility.Utils;
import Skeletonize3D_.Skeletonize3D_;
import analysis.AnalysisDataset;
import analysis.AnalysisOptions;
import analysis.AnalysisOptions.CannyOptions;
import analysis.CannyEdgeDetector;
import analysis.Detector;
import components.Cell;
import components.SpermTail;
import components.generic.XYPoint;
import components.nuclei.Nucleus;



/**
 * This class is to test ideas for detecting sperm tails stained with
 * anti-tubulin. It can be consetucted as SwingWorker for GUI incorpoation,
 * or the static method run() can be called directly for scripted use. When used
 * as a SwingWorker, the reported progress will be the number of cells processed, and
 * upon completion, a "Finished" property change event is fired.
 */
public class TubulinTailDetector extends SwingWorker<Boolean, Integer> {
	
	private static Logger logger;

	private final AnalysisDataset dataset;
	private final File folder;
	private final int channel;
	
	private static final int WHITE = 255;
	
	public TubulinTailDetector(AnalysisDataset dataset, File folder, int channel){
		this.dataset = dataset;
		this.folder = folder;
		this.channel = channel;
		
		logger = Logger.getLogger(TubulinTailDetector.class.getName());
		logger.addHandler(dataset.getLogHandler());
	}
	
	@Override
	protected void process( List<Integer> integers ) {
		//update the number of entries added
		int amount = integers.get( integers.size() - 1 );
		int totalCells = dataset.getCollection().getNucleusCount();
		int percent = (int) ( (double) amount / (double) totalCells * 100);
		setProgress(percent); // the integer representation of the percent
	}
	
	@Override
	protected  Boolean doInBackground() {
		boolean result = true;
		
		
//		logger = new Logger(dataset.getDebugFile(), "TubulinTailDetector");
		logger.log(Level.INFO, "Beginning tail detection");

		try{
			int progress = 0;
			for(Cell c : dataset.getCollection().getCells()){

				Nucleus n = c.getNucleus();
				logger.log(Level.INFO, "Looking for tails associated with nucleus "+n.getImageName()+"-"+n.getNucleusNumber());
				
				// get the image in the folder with the same name as the
				// nucleus source image
				File imageFile = new File(folder + File.separator + n.getImageName());
				logger.log(Level.FINE, "Tail in: "+imageFile.getAbsolutePath());
//				SpermTail tail = null;
				
				
				// attempt to detect the tails in the image
				try{
					List<SpermTail> tails = detectTail(imageFile, channel, n, dataset.getAnalysisOptions());
					
					for(SpermTail tail : tails){
						c.addTail(tail);
					}
					
				} catch(Exception e){
					logger.log(Level.SEVERE, "Error detecting tail", e);
				}
				
				progress++;
				publish(progress);
			}
		} catch (Exception e){
			logger.log(Level.SEVERE, "Error in tubulin tail detection", e);
			return false;
		}

		return result;
	}

	@Override
	public void done() {

		try {
			if(this.get()){
				firePropertyChange("Finished", getProgress(), Constants.Progress.FINISHED.code());
			} else {
				firePropertyChange("Error", getProgress(), Constants.Progress.ERROR.code());
			}
		} catch (InterruptedException e) {
			logger.log(Level.SEVERE, "Error in tubulin tail detection", e);
		} catch (ExecutionException e) {
			logger.log(Level.SEVERE, "Error in tubulin tail detection", e);
		}

	} 
	
	/**
	 * Run the analysis on a dataset with nuclei defined. Take tubulin-stained
	 * images from a folder, and analyse the given channel
	 * @param dataset the cells to add a tail to
	 * @param folder the images with the tubulin stain
	 * @param channel the channel with the stain
	 * @return success or failure
	 */
	public static boolean run(AnalysisDataset dataset, File folder, int channel){

		boolean result = true;
		logger = Logger.getLogger(TubulinTailDetector.class.getName());
		logger.addHandler(dataset.getLogHandler());
//		logger = new Logger(dataset.getDebugFile(), "TubulinTailDetector");
		logger.log(Level.INFO, "Beginning tail detection");

		try{

			for(Cell c : dataset.getCollection().getCells()){

				Nucleus n = c.getNucleus();
				logger.log(Level.INFO, "Looking for tails associated with nucleus "+n.getImageName()+"-"+n.getNucleusNumber());
				
				// get the image in the folder with the same name as the
				// nucleus source image
				File imageFile = new File(folder + File.separator + n.getImageName());
				logger.log(Level.FINE, "Tail in: "+imageFile.getAbsolutePath());
//				SpermTail tail = null;
				
				
				// attempt to detect the tails in the image
				try{
					List<SpermTail> tails = detectTail(imageFile, channel, n, dataset.getAnalysisOptions());
					
					for(SpermTail tail : tails){
						c.addTail(tail);
					}
					
				} catch(Exception e){
					logger.log(Level.SEVERE, "Error detecting tail", e);
				}
				
			}
		} catch (Exception e){
			logger.log(Level.SEVERE, "Error in tubulin tail detection", e);
			return false;
		}

		return result;
	}
	
	
	/**
	 * Given a file, try to find a sperm tail in the given channel.
	 * The nucleus is used to orient the tail, and separate from other
	 * tails in the image
	 * @param f the file
	 * @param channel the channel with the tail signal
	 * @param n the nucleus to which the tail should attach
	 * @return a SpermTail object
	 */
	public static List<SpermTail> detectTail(File tubulinFile, int channel, Nucleus n, AnalysisOptions options){
		
//		logger = Logger.
//		logger = new Logger(log, "TubulinTailDetector");
		List<SpermTail> tails = new ArrayList<SpermTail>(0);
		logger.log(Level.INFO, "Running on image: "+tubulinFile.getAbsolutePath());
		// import image with tubulin in  channel
		ImageStack stack = null;
		try{
			stack = ImageImporter.importImage(tubulinFile, logger);
		} catch (Exception e){
			
			logger.log(Level.SEVERE, "Error importing image as stack", e);
			return tails; // return the empty list
		}
		
		// the stack in the ImageStack must be converted from the given rgb channel 
		int stackNumber = channel == Constants.RGB_BLUE 
						? Constants.COUNTERSTAIN
						: channel == Constants.RGB_RED
							? Constants.FIRST_SIGNAL_CHANNEL
							: Constants.FIRST_SIGNAL_CHANNEL+1;
		
		
		// file must match dimensions of existing nucleus image file
		if( checkDimensions(stack, n)){
			
			// edge / threshold to find tubulin stain
			ImageStack edges = runEdgeDetector(stack, options, stackNumber);
			
			// get objects found by edge detector
			List<Roi> borderRois = getROIs(edges, true, 1);
			logger.log(Level.INFO, "Found "+borderRois.size()+" potential tails in image");
			
			// create the skeletons of the detected objects
			ImageStack skeletonStack = skeletoniseStack(edges, borderRois);
			
			// prune the shortest branches of the stack, ready for graph creation
//			ImageStack prunedStack = pruneStack(skeletonStack);
			
			// detect the skeletons as graph objects, and prune shortest branches
			List<Graph> skeletonGraphs = makeGraphsFromStack(skeletonStack);
			
			// turn the graphs into line rois
			List<Roi> skeletons = getSkeletons(skeletonGraphs);
			
			// Get rois of the correct length
//			List<Roi> usableSkeletons = filterSkeletons(skeletons, 1000);
			
			// print the roi details to the log
			logger.log(Level.INFO, "Found "+skeletons.size()+" potential tails in image");
			
			
			// get the complete tails matching border to skeleton
			tails = buildSpermTails(skeletons, borderRois, tubulinFile, channel);
			
			// from the list of possible tails in the image, 
			// find the ones that overlap the current nucleus
			tails = getTailsMatchingNucleus(tails, n);
			
			
		} else {
			logger.log(Level.SEVERE, "Dimensions of image do not match");
		}

		return tails;
	}
	
	/**
	 * Run the skeletonising on the given image stack. Return a new stack with
	 * skeletons only
	 * @param stack the  stack to skeletonise
	 * @param objects the listof rois to skeletonise
	 * @return a new stack
	 */
	private static ImageStack skeletoniseStack(ImageStack stack, List<Roi> objects){

		// fill rois with white so they are properly skeletonised
		ByteProcessor bp = (ByteProcessor) stack.getProcessor(1);
		bp.setColor(Color.WHITE);
		for(Roi r : objects){
			logger.log(Level.FINE, "Filling roi");
			bp.fill(r);
		}

		// create duplicate images for debugging
		// only the skeletonised image is used 
		ImagePlus filledByteImage = new ImagePlus("filled byte image", bp);
		filledByteImage.show();
		
		// create a copy of the filled processor and display for debugging
		BinaryProcessor binaryProcessor = new BinaryProcessor((ByteProcessor) bp.duplicate());
		ImagePlus binaryImage = new ImagePlus("binary image", binaryProcessor);
//		binaryImage.show();
		
		AnalysisOptions options = new AnalysisOptions(); 
		options.getCannyOptions("tail").setClosingObjectRadius(5);
		morphologyClose(binaryProcessor, options.getCannyOptions("tail"));
		
		// fill remaining holes in the image
		Binary binary = new Binary();
		binary.setup("fill", binaryImage);
		binary.run(binaryProcessor);


		// create a copy of the filled processor and convert to a binary processor
		ByteProcessor duplicateBinaryProcessor = (ByteProcessor) binaryProcessor.duplicate();
		BinaryProcessor skeletonisableProcessor = new BinaryProcessor((ByteProcessor) duplicateBinaryProcessor);
		ImagePlus skeletonisableImage = new ImagePlus("binary image", skeletonisableProcessor);
		skeletonisableImage.show();

		// skeletonise the binary image
		Skeletonize3D_ skelly = new Skeletonize3D_();
		skelly.setup("", binaryImage);
		skelly.run(skeletonisableProcessor);

		logger.log(Level.FINE, "Skeletonized the image");

		// make a new imageStack from the skeletonised images
		ImageStack skeletonStack = ImageStack.create(binaryProcessor.getWidth(), binaryProcessor.getHeight(), 0, 8);
		skeletonStack.addSlice("skeleton", binaryProcessor);
		skeletonStack.deleteSlice(1); // remove the blank first slice
		ImagePlus skeletonImage = new ImagePlus("skeleton image", skeletonStack.getProcessor(1));
		skeletonImage.show();
		
		return skeletonStack;
	}
	

	/**
	 * Analyse the skeletons drawn in the given stack,and return skeleton graphs
	 * @param stack the skeletonised image
	 * @return a list of graphs
	 */
	private static List<Graph> makeGraphsFromStack(ImageStack stack){
		AnalyzeSkeleton_ an = new AnalyzeSkeleton_();
		
//		public SkeletonResult run(
//				int pruneIndex,
//				boolean pruneEnds,			
//				boolean shortPath,
//				ImagePlus origIP,
//				boolean silent,
//				boolean verbose)
		
		// copy the input - not changing it for now
		ByteProcessor ip = (ByteProcessor) stack.getProcessor(1).duplicate();
		
		// remove shading from annotation
		ip.threshold(1);
		ImagePlus image = new ImagePlus("make graph", ip);
//		image.show();
		an.setup("", image);
		
		// 0 - do not prune
		// 1- prune shortest
		SkeletonResult result = an.run(0, false, true, image, true, false);

		
		// an array of all the graphs in the image (skeletons)		
		Graph[] graphs = result.getGraph();
		List<Graph> potentialTails = new ArrayList<Graph>(0);
		
		logger.log(Level.FINE, "Image has "+graphs.length+" graphs");
		
		for (Graph g : graphs){
						
			for(Edge e : g.getEdges()){
				
					potentialTails.add(g);
			}
		}
		
		for(Graph g : potentialTails){
			logger.log(Level.FINE, "Potential tail graph found");
			for(Edge e : g.getEdges()){
				logger.log(Level.FINE, "\tEdge length: "+e.getLength());

			}

		}
		return potentialTails;
	}
	
	/**
	 * Given a graph vertex and a list of edges, find the next longest edge
	 * @param v the vertex
	 * @param list the edges
	 * @return the list updated with the next longest branch from the vertex
	 */
//	private static List<Edge> findNextLongestBranch(Vertex v, List<Edge> list){
//
//		double maxLength = 0;
//		Edge longestEdge = null;
//		Vertex longestEnd = null;
////		logger.log("\tBeginning traversal");
//		
//		if(v==null){ // stop when no more unvisited vertices are added
////			logger.log("\tEndpoint reached");
//			return list;
//		}
//		
////		logger.log("\tVertex has "+v.getBranches().size()+" branches");
//
//		for(Edge e : v.getBranches()){
//			
//			Vertex end = e.getOppositeVertex(v);
//			
//			// only consider the branch if the far end leads somewhere new
//			if(!end.isVisited()){
//				
////				logger.log("\t\tEdge length: "+e.getLength());
//							
//				maxLength 	= e.getLength() > maxLength
//							? e.getLength()
//							: maxLength;
//							
//				longestEdge = e.getLength() > maxLength
//							? e
//							: longestEdge;
//				
//				longestEnd = e.getLength() > maxLength
//						? end
//						: longestEnd;
//				
//				end.setVisited(true);
//			}
//			
//		}
////		longestEnd.setVisited(true);
//		list.add(longestEdge);
//		list = findNextLongestBranch(longestEnd, list);
//		return list;
//		
//	}
	
	/**
	 * Find skeletons witin the given stack and border rois
	 * @param borderRois the tail borders in the image
	 * @param skeletonStack the imagestack with skeletons
	 * @param stackNumber the position in the stack of the skeleton
	 * @return a list of skeletons as Polyline rois
	 */
//	private static List<Roi> getSkeletons(List<Roi> borderRois, ImageStack skeletonStack, int stackNumber){
//		
//		List<Roi> result = new ArrayList<Roi>(0);
//		
//		for(Roi borderRoi : borderRois){
//			
//			// get the endpoints of the skeleton
//			List<XYPoint> ends = findSkeletonEndpoints(borderRoi, skeletonStack, stackNumber);
//			
//			// pruning step when needed
//			if(ends.size()==0){
//				logger.log("No skeleton endpoints found");
//			} else {
//
//				if(ends.size()>2){
//					logger.log("Too many endpoints: pruning needed");
//				} else {
//					// pick a random end
//					XYPoint end1 = ends.get(0);
//					Roi line = buildPolyLine(end1, skeletonStack, stackNumber);
//					result.add(line);
//					logger.log("Added polyline roi");
//				}
//			}
//			
//		}
//		
//		return result;
//	}
	
	/**
	 * Prune the skeletons in the given stack
	 * @param stack the imagestack
	 * @return a pruned imagestack
	 */
	private static ImageStack pruneStack(ImageStack stack){
		
		logger.log(Level.INFO, "Pruning skeletons to new stack");
		AnalyzeSkeleton_ an = new AnalyzeSkeleton_();
		
//		public SkeletonResult run(
//				int pruneIndex,
//				boolean pruneEnds,			
//				boolean shortPath,
//				ImagePlus origIP,
//				boolean silent,
//				boolean verbose)
		
		// copy the input - not changing it for now
		ImagePlus image = new ImagePlus(null, stack.getProcessor(1).duplicate());
		
		an.setup("", image);
		
		// 0 - do not prune
		// 1- prune shortest
		SkeletonResult result = an.run(1, false, true, image, true, false);

		ImageStack labelledStack = an.getLabeledSkeletons();
		ImagePlus labelledImage = new ImagePlus("pruned image", labelledStack.getProcessor(1));
//		labelledImage.show();
		return labelledStack;
	}
	
	/**
	 * Get the skeletons from the given graphs as polyline rois
	 * @param graphs the AnalyzeSkeleton_ graphs containing skeletons
	 * @return a list of rois
	 */
	private static List<Roi> getSkeletons(List<Graph> graphs){
		List<Roi> result = new ArrayList<Roi>(0);
		
		for(Graph g : graphs){
			for(Edge e : g.getEdges()){
				
					
					List<Integer> xpoints = new ArrayList<Integer>(0);
					List<Integer> ypoints = new ArrayList<Integer>(0);
					
					for(skeleton_analysis.Point p : e.getSlabs()){

						xpoints.add(p.x);
						ypoints.add(p.y);

					}
					
					Roi potentialSkeleton = new PolygonRoi(Utils.getFloatArrayFromIntegerList(xpoints), 
							Utils.getFloatArrayFromIntegerList(ypoints),
							PolygonRoi.POLYLINE);
					
					if(potentialSkeleton.getLength()>0){
						result.add(potentialSkeleton);
					}
					
			}
		}
		
		
		return result;
	}
	
	
	/**
	 * From a list of skeleton rois, get only those greater than or equal to the given length
	 * @param skeletons the roi list
	 * @param minLength the minumum length
	 * @return a list of passing rois
	 */
// 	private static List<Roi> filterSkeletons(List<Roi> skeletons, int minLength){
//		List<Roi> result = new ArrayList<Roi>(0);
//
//		for(Roi r : skeletons){
//			if(r.getLength()>=minLength){
//				result.add(r);
//			}
//		}
//		return result;
//	}
	
	
	/**
	 * From a list of skeletons, find the skeleton overlapping a given nucleus. 
	 * Find the border outline associated with that skeleton, and create a new 
	 * sperm tail
	 * @param usableSkeletons the list of skeleton rois
	 * @param borderRois the list of tail border rois
	 * @param n the nucleus
	 * @param tubulinFile the image file with the tubulin stain
	 * @param channel the rgb channel with the tubulin stain (0 if greyscale)
	 * @return a sperm tail or null if none found
	 */
	private static List<SpermTail> getTailsMatchingNucleus(List<SpermTail> tails, Nucleus n){
		
		// ensure that the positions of the nucleus are corrected to
		// match the original image
		List<SpermTail> result = new ArrayList<SpermTail>(0);
		FloatPolygon nucleusOutline = Utils.createOriginalPolygon(n);
		
		for(SpermTail tail : tails){
			
			List<XYPoint> skeleton = tail.getSkeleton();
			for(XYPoint p : skeleton){
				if(nucleusOutline.contains( (float) p.getX(), (float) p.getY())){
					result.add(tail);
					logger.log(Level.FINE, "Found tail matching nucleus");
				}
			}
		}
			

//			for(Roi skeleton : usableSkeletons){
//
//				boolean tailOverlap = false;
//
//				logger.log("Assessing skeleton: Length : "+skeleton.getLength());
//
//				float[] xpoints = skeleton.getFloatPolygon().xpoints;
//				float[] ypoints = skeleton.getFloatPolygon().ypoints;
//
//				// objects that overlap with the nucleus at some point are kept
//				for(int i=0;i<xpoints.length;i++){
//
//					if(nucleusOutline.contains(xpoints[i], ypoints[i])){
//						tailOverlap = true;
//					}
//				}
//
//
//				if(tailOverlap){
//					// if a tail overlaps the nucleus, get the corresponding border roi
//					Roi tailBorder = null;
//					//				position = null;
//					for(Roi border : borderRois){
//						if (border.getFloatPolygon().contains(xpoints[0], ypoints[0])
//								&& border.getFloatPolygon().contains(xpoints[xpoints.length-1], ypoints[ypoints.length-1])){
//							tailBorder = border;
//
//						}
//					}
//					if(tailBorder!=null){
//
//						tail = new SpermTail(tubulinFile, channel, skeleton, tailBorder);
//						logger.log("Found matching tail");
//					}
//				}
//			}
//		}
		return result;
	}
	
	/**
	 * Match skeletons to border rois, and build sperm tail objects
	 * @param usableSkeletons the list of skeleton rois
	 * @param borderRois the list of tail border rois
	 * @param tubulinFile the image file with the tubulin stain
	 * @param channel the rgb channel with the tubulin stain (0 if greyscale)
	 * @return a list of sperm tails (empty if none found)
	 */
	private static List<SpermTail> buildSpermTails(List<Roi> usableSkeletons, List<Roi> borderRois, File tubulinFile, int channel){
		
		List<SpermTail> tails = new ArrayList<SpermTail>(0);

		
		for(Roi skeleton : usableSkeletons){

			logger.log(Level.FINE, "Assessing skeleton: Length : "+skeleton.getLength());
			
			//only process skeletons with lenght
			if(skeleton.getLength()>0){

				// the skeleton positions
				float[] xpoints = skeleton.getFloatPolygon().xpoints;
				float[] ypoints = skeleton.getFloatPolygon().ypoints;

				//find the corresponding border roi
				Roi tailBorder = null;

				for(Roi border : borderRois){

					// find the border roi that contains the first and last points of the skeleton
					if (border.getFloatPolygon().contains(xpoints[0], ypoints[0])
							&& border.getFloatPolygon().contains(xpoints[xpoints.length-1], ypoints[ypoints.length-1])){
						tailBorder = border;
					}
				}

				// add the tail and skeleton to a new tail object
				if(tailBorder!=null){

					tails.add( new SpermTail(tubulinFile, channel, skeleton, tailBorder)  );
					logger.log(Level.FINE, "Found outline matching skeleton");
				}
			}
		}
		return tails;
	}
	
	/**
	 * Find potential endpoints within the given skeleton and roi
	 * @param borderPolygon the roi with the tail border
	 * @param skeletonStack the ImageStack containing the skeleton
	 * @param stackNumber the stack in the ImageStack with the skeleton
	 * @return a list of XYPoints representing skeleton branch ends
	 */
//	private static List<XYPoint> findSkeletonEndpoints(Roi borderPolygon, ImageStack skeletonStack, int stackNumber){
//		
//		List<XYPoint> result = new ArrayList<XYPoint>(0);
//		// go around the roi. If the next point
//		try {
//			
//			logger.log("Identifying skeleton endpoints");
//						
//			ByteProcessor p = (ByteProcessor) skeletonStack.getProcessor(stackNumber);
//			
//			// loop through pixels in image
//			for(int x=0; x<p.getWidth(); x++){
//				
//				for(int y=0; y<p.getHeight(); y++){
//					
//					if(borderPolygon.contains(x, y)){ // only consider pixels within the roi
//												 
//						 if(p.getPixel(x, y)==WHITE){ // if the pixel is white
//							 
//							// if only one contact, add as endpoint
//							 
//							 XYPoint potentialEndpoint = new XYPoint(x,y);
////							 logger.log("Potential endpoint at "+x+","+y);
//							 List<XYPoint> connectedPoints = getJoiningPoints(p, x, y);
//							 logger.log("\tTotal: "+connectedPoints.size()+" connections");
////							 IJ.log(x+","+y+" connects to "+connectedPoints.size()+" points");
//							 if(connectedPoints.size()==1){
//								 result.add(potentialEndpoint);
//							 }
//						 }
//					}
//					
//				}
//			}
//			
//
//		} catch (Exception e) {
//			logger.log("Error getting tail border region as stack: "+e.getMessage());
//			for(StackTraceElement el : e.getStackTrace()){
//				logger.log(el.toString(), Logger.STACK);
//			}
//		}
//	
//		return result;
//	}
	
	/**
	 * Create a polyline from a given endpoint of a skeleton. Looks for adjacent white
	 * pixels, and continues until another endpoint is reached. Does not work on unpruned
	 * skeletons - i.e only two endpoints are expected.
	 * @param startPoint the pixel from which to begin building
	 * @param skeletonStack the stack with the skeleton
	 * @param stackNumber the position of the skeleton in the stack
	 * @return
	 */
//	private static PolygonRoi buildPolyLine(XYPoint startPoint, ImageStack skeletonStack, int stackNumber){
//		
//		PolygonRoi result = null;
//		logger.log("Building polyline");
//		
//		// the lists from which to make the line
//		List<Integer> xpoints = new ArrayList<Integer>(0);
//		List<Integer> ypoints = new ArrayList<Integer>(0);
//		
//		ByteProcessor processor = (ByteProcessor) skeletonStack.getProcessor(stackNumber);
//		
//		List<XYPoint> previousPoints = new ArrayList<XYPoint>(0);
//		previousPoints.add(startPoint);
////		XYPoint prevPoint = startPoint;
//		XYPoint currentPoint = startPoint;
//		boolean extend = true;
//				
//		while(extend){
//
//			xpoints.add(currentPoint.getXAsInt());
//			ypoints.add(currentPoint.getYAsInt());
////			logger.log("\tAdded point: "+currentPoint.toString());
//			
//			List<XYPoint> list = getJoiningPoints(processor, currentPoint.getXAsInt(), currentPoint.getYAsInt());
//			
//			// get the number of joining points
//			// stop building the chain when an endpoint is reached
//			if(list.size()==1 && currentPoint != startPoint){
//				extend = false;
//				break;
//			} else {			
//
//				// add only point that is NOT previously seen
//				for(XYPoint p : list){
//					boolean ok = true;
//					for(XYPoint prevPoint : previousPoints){
//						if(p.overlaps(prevPoint)){
//							ok = false;
//						}
//					}
//					
//					if(ok){
//						previousPoints.add(currentPoint);
//						currentPoint = p;
//					}
//				}
//			}
//		}
//		
//		logger.log("\tLine created of length: "+xpoints.size());
//
//		// convert the list to float for the polygon roi constructor
//		result = new PolygonRoi(Utils.getFloatArrayFromIntegerList(xpoints), 
//								Utils.getFloatArrayFromIntegerList(ypoints),
//								PolygonRoi.POLYLINE);
//		return result;
//	}
	
	/**
	 * Find the points in the 3x3 pixel box surrounding the given point
	 * that have a value of 255 (white)
	 * @param processor the image processor (binary processor)
	 * @param p the point to search from
	 * @return a list of white connected points
	 */
//	private static List<XYPoint> getJoiningPoints(ImageProcessor processor, int x, int y){
//		
//		List<XYPoint> result = new ArrayList<XYPoint>(0);
//		XYPoint centre = new XYPoint(x,y);
//		
//		for(int i=x-1; i<=x+1; i++){
//			
//			for(int j=y-1; j<=y+1; j++){
//				
////				logger.log("\t"+i+","+j+" : "+processor.getPixel(i, j));
//				
//				XYPoint p = new XYPoint(i,j);
//				
//				if(processor.getPixel(i, j)==WHITE && !p.overlaps(centre)){ // ensure we don't count the centre pixel
//					result.add(p);
//				}
//			}
//		}
//		return result;
//	}
	
	/**
	 * Check that the dimensions of the input image are the same as the 
	 * image used to detect the nucleus. 
	 * @param stack the imported stack
	 * @param n the source nucleus
	 * @return an ok
	 */
	private static boolean checkDimensions(ImageStack stack, Nucleus n ){
		
		File baseFile = n.getSourceFile();
		logger.log(Level.FINE, "Nucleus in "+baseFile.getAbsolutePath());
		ImageStack baseStack = ImageImporter.importImage(baseFile, logger);
		
		boolean ok = true;
		if(stack.getHeight() != baseStack.getHeight()){
			ok = false;
			logger.log(Level.FINE, "Fail on height check");
		}
		if(stack.getWidth() != baseStack.getWidth()){
			logger.log(Level.FINE, "Fail on width check");
			ok = false;
		}
		return ok;
	}
	
	/**
	 * Use Canny edge detection to produce an image with potential edges highlighted
	 * for the detector
	 * @param image the stack to process
	 * @return a stack with edges highlighted
	 */
	private static ImageStack runEdgeDetector(ImageStack image, AnalysisOptions options, int stackNumber){

		//		bi.show();
		ImageStack searchStack = null;
		try {
			// using canny detector
			CannyOptions tailCannyOptions = options.getCannyOptions("tail");

			logger.log(Level.FINE, "Creating edge detector");
			CannyEdgeDetector canny = new CannyEdgeDetector();
			canny.setSourceImage(image.getProcessor(stackNumber).getBufferedImage());
			canny.setLowThreshold( tailCannyOptions.getLowThreshold() );
			canny.setHighThreshold( tailCannyOptions.getHighThreshold());
			canny.setGaussianKernelRadius(tailCannyOptions.getKernelRadius());
			canny.setGaussianKernelWidth(tailCannyOptions.getKernelWidth());

			canny.process();
			BufferedImage edges = canny.getEdgesImage();
			ImagePlus searchImage = new ImagePlus(null, edges);


			// add morphological closing
			ByteProcessor bp = searchImage.getProcessor().convertToByteProcessor();

			morphologyClose( bp, tailCannyOptions);
			ImagePlus bi= new ImagePlus(null, bp);
			searchStack = ImageImporter.convert(bi);

			bi.close();
//			searchImage.show();
			searchImage.close();

			logger.log(Level.FINE, "Edge detection complete");
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error in edge detection", e);
		}
		return searchStack;
	}
	
	/**
	 * Run a morphological closing on the given image processor. Radius is
	 * provided by the CannyOptions
	 * @param ip the image processor to close
	 * @param options the options for this canny operation
	 */
	private static void morphologyClose(ImageProcessor ip, CannyOptions options){
		try {
			
			int shift=1;
			int radius = options.getClosingObjectRadius();
			int[] offset = {0,0};
			int eltype = 0; //circle
			logger.log(Level.FINE, "Closing objects with circle of radius "+radius);
			
			StructureElement se = new StructureElement(eltype,  shift,  radius, offset);
//			IJ.log("Made se");
			MorphoProcessor mp = new MorphoProcessor(se);
//			IJ.log("Made mp");
			mp.fclose(ip);
			logger.log(Level.FINE, "Objects closed");
//			IJ.log("Closed");
		} catch (Exception e) {
			IJ.log("Error in closing: "+e.getMessage());
			for(StackTraceElement el : e.getStackTrace()){
				IJ.log(el.toString());
			}
		}
		
	}
	
	/**
	 * Detects objects within the given image.
	 *
	 * @param image the ImagePlus to be analysed
	 * @param closed should the detector get only closed polygons, or open lines
	 * @param channel the stack of the image to search
	 * @return the Map linking an roi to its stats
	 */
	private static List<Roi> getROIs(ImageStack image, boolean closed, int channel){
		Detector detector = new Detector();
		detector.setMaxSize(100000);
		
		if(closed){
			detector.setMinSize(1000); // get polygon rois
		} else {
			detector.setMinSize(0); // get line rois
		}
		detector.setMinCirc(0);
		detector.setMaxCirc(0.5);
		detector.setThreshold(30);
		detector.setStackNumber(channel);
		try{
			detector.run(image);
		} catch(Exception e){
			logger.log(Level.SEVERE, "Error in tail detection", e);
		}
		return detector.getRoiList();
	}
	

}
