/*
Copyright Ben Skinner 2014

Functions for creating and mamaging ROIs

*/

// -------------------------------------------------
// ROI HANDLING FUNCTIONS
// -------------------------------------------------

// convert an ROI selection to array of x coordinates
// Input: ROI
// Output: array of x values 
function ROI_to_x(array){
	length = array[0];
	x_selection = newArray();		
	for (i=1; i<=length; i++){
		x_selection = Array.concat(x_selection, array[i]);
	}
	return x_selection;
}

// convert an ROI selection to array of y coordinates
// Input: ROI
// Output: array of y values 
function ROI_to_y(array){
	length = array[0];
	y_selection = newArray();		
	for (i=1; i<=length; i++){
		y_selection = Array.concat(y_selection, array[i+length]);
	}
	return y_selection;
}

// Create an ROI 
// Input: array of x, array of y
// Output: ROI format array
function createROI(x, y){
	// transfer x and y arrays to ROI format
	// 0 - length of array
	// 1 - first x
	// [0]+1 - first y
	length = x.length;
	full_array =  Array.concat(x,y);
	full_array =  Array.concat(length,full_array);
	return full_array;
}

// Look for largest discontinuity between points
// Set these coordinates to the ROI end
// Input: ROI
// Output: ROI
function shuffleROI(roi){
		
	
	x_points = ROI_to_x(roi);
	y_points = ROI_to_y(roi);
	
	last_x = x_points[x_points.length-1];
	last_y = y_points[y_points.length-1];
	
	max_distance = 0;
	max_i = 0;
	
	if(showDebug){
		print("Shuffling ROI:");
	}

	// find the two most divergent points
	for(i=0;i<x_points.length;i++){
	
		pointA = newArray(x_points[i],y_points[i]);
		
		if(i==x_points.length-1){ // check last against first
			pointB = newArray(x_points[0],y_points[0]);
			distance = getLength( pointA, pointB);
		} else { // otherwise check to the next in array
			pointB = newArray(x_points[i+1],y_points[i+1]);
			distance = getLength( pointA, pointB);
		}
		
		if(distance > max_distance){
			max_distance = distance;
			max_i = i;
		}
		if(showDebug){
			print(i+": "+pointA[0]+"  "+pointA[1]+" to "+pointB[0]+"  "+pointB[1]+": "+distance);
		}
	}
	if(showDebug){
		print("Max distance: "+max_distance+" at "+max_i);
	}
	// we now have the position just before the discontinuity
	// chop the array in two and reassemble in the correct order
	x1 = Array.slice(x_points,max_i+1); // max_i to end
	y1 = Array.slice(y_points,max_i+1);
		
	x2 = Array.slice(x_points,0,max_i+1);
	y2 = Array.slice(y_points,0,max_i+1);
	
	if(showDebug){
		
		x1roi = createROI(x1, y1);
		print("Array x1 size:"+x1roi[0]);
		if(x1roi[0] > 0 ){
			selectWindow("draw");
			drawROI(x1roi, "freeline");
			printROI(x1roi);
		}

		x2roi = createROI(x2, y2);
		print("Array x2 size:"+x2roi[0]);
		if(x2roi[0] > 0 ){
			selectWindow("draw");
			drawROI(x2roi, "freeline");
			printROI(x2roi);
		}
		
	}
	
	x_final = Array.concat(x1,x2);
	y_final = Array.concat(y1,y2);

	// transfer to ROI format
	full_array = createROI(x_final, y_final);
	
	if(showDebug){
		print("Combined array:");
		drawROI(full_array, "freeline");
		printROI(full_array);
	}
	return full_array;
}

// Keep the first z% of the roi
// Input: ROI, number between 0-1
// Output: ROI
function trimROI(roi, amount_to_keep){
	x_points = ROI_to_x(roi);
	y_points = ROI_to_y(roi);
	roi_length = roi[0];
	
	end = floor(roi_length*amount_to_keep); // fetch first x% of signals
	trimmed_x = Array.slice(x_points, 0, end);
	trimmed_y = Array.slice(y_points, 0, end);
	
	trimmed_roi = createROI(trimmed_x, trimmed_y);
	return trimmed_roi;
	
}

// Flip the ROI on the given axis
// Input: ROI, axis
// Output: ROI
function flipROI(roi, axis){

	x = ROI_to_x(roi);
	y = ROI_to_y(roi);
	
	if(axis == "x"){
		// horizontal flip
		midpoint = (x[0] + x[x.length-1]) / 2;
		flipped_x = newArray(x.length);
		
		for(i=0;i<x.length;i++){
		
			if(x[i] > midpoint){
				new_x = midpoint - (x[i]-midpoint);
			} else {
				new_x = midpoint + (midpoint - x[i]);
			}
			
			flipped_x[i] = new_x;
		}
		flipped_array = createROI(flipped_x, y);
	}
	if(axis == "y"){
		//vertical flip
		midpoint = (y[0] + y[y.length-1]) / 2;
		flipped_y = newArray(y.length);
		
		for(i=0;i<y.length;i++){
		
			if(y[i] > midpoint){
				new_y = midpoint - (y[i]-midpoint);
			} else {
				new_y = midpoint + (midpoint - y[i]);
			}
			
			flipped_y[i] = new_y;
		}
		flipped_array = createROI(x, flipped_y);
	}
	return flipped_array;
}

// Add the ROI to the ROI manager
// Input: ROI, type (e.g. freeline, polygon...)
// Output: None
function drawROI(roi, type){
	x = ROI_to_x(roi);
	y = ROI_to_y(roi);
	makeSelection(type, x, y);
	roiManager("Add");
}

// Add the smoothed ROI to the ROI manager
// Input: ROI, type (e.g. freeline, polygon...)
// Output: None
function drawSmoothROI(roi, type){
	x = ROI_to_x(roi);
	y = ROI_to_y(roi);
	makeSelection(type, x, y);
	run("Interpolate", "interval=1 smooth");
	roiManager("Add");
}

// Write the ROi to file
// Input: ROI, open file handle
// Output: None
function writeROI(roi, file){
	x = ROI_to_x(roi);
	y = ROI_to_y(roi);
	
	for(i=0;i<x.length;i++){
		print(file, x[i]+"\t"+y[i]);
	}
}

// Write the ROi to screen
// Input: ROI
// Output: None
function printROI(roi){
	x = ROI_to_x(roi);
	y = ROI_to_y(roi);
	
	for(i=0;i<x.length;i++){
		print(x[i]+"   "+y[i]);
	}
}

// Swap x and y coordinates in ROI
// Input: ROI
// Output: ROI
function swapXandYinROI(roi){
	x = ROI_to_x(roi);
	y = ROI_to_y(roi);
	newROI = createROI(y, x);
	return newROI;
}

// Scale the x coordinates to fit a given length
// Input: ROI, number
// Output: ROI
function normaliseROI(roi, newLength){
	// stretch the x coordinates of the given roi to fit from 
	// 0 - length allowing comparison between different curves
	x = ROI_to_x(roi);
	y = ROI_to_y(roi);
	old_x_max = 0;
	
	max_y = 0; // for logging
	
	for(i=0;i<x.length;i++){
		if(x[i] > old_x_max){
			old_x_max = x[i];
		}
		if(y[i] > max_y){ // for logging
			max_y = y[i];
		}
	}
	
	addToLog("MY",max_y);
	
	for(i=0;i<x.length;i++){
		x[i] = (x[i] / old_x_max) * newLength;
	}
	newROI = createROI(x,y);
	return newROI;	
}

// Find the area enclosed by the ROI
// Input: ROI
// Output: area in pixels
function getROIArea(roi){
	
	drawROI(roi, "polygon");

	run("Set Measurements...", "area");
	run("Measure");
	
	if(nResults > 0){
		a = getResult("area"); // centre of mass
	}
	return a;
}

// Rotate the ROI so the first and last elments are aligned on the 
// horizontal (x) axis
// Input: ROI
// Output: ROI
function alignFirstAndLastElementOnAxisX(roi){

	// For a given roi, rotate it until the horizontal
	// (x-axis) distance between the first and last elements
	// is minimised. The ROI is now considered to be aligned
	// vertically.
	x_points = ROI_to_x(roi);
	y_points = ROI_to_y(roi);
	min_x = 100;
	theta = 0;
	
	for(i=1;i<180;i++){
		// always do the rotation fresh on original coordinates
		// avoids distortion from integer positions building up
		makeSelection("freeline", x_points, y_points);
		run("Rotate...", "angle="+i);
		getSelectionCoordinates(rot_x, rot_y);
		first_to_last_x = abs(rot_x[0] - rot_x[rot_x.length-1]);
		if(first_to_last_x < min_x){
			min_x_roi = createROI(rot_x, rot_y);
			theta = i;
			min_x = first_to_last_x;
		}
	}
	if(showDebug) { print("Minimum distance: "+min_x);}
	if(showDebug) { print("Rotated by "+theta+" degrees");}
	return min_x_roi;
}

// Rotate the ROI so the first and last elments are aligned on the 
// vertical (y) axis
// Input: ROI
// Output: ROI
function alignFirstAndLastElementOnAxisY(roi){

	// For a given roi, rotate it until the vertical
	// (y-axis) distance between the first and last elements
	// is minimised. The ROI is now considered to be aligned
	// horizontally.
	x_points = ROI_to_x(roi);
	y_points = ROI_to_y(roi);
	min_y = 100;
	theta = 0;
	
	for(i=1;i<180;i++){
		// always do the rotation fresh on original coordinates
		// avoids distortion from integer positions building up
		makeSelection("freeline", x_points, y_points);
		run("Rotate...", "angle="+i);
		getSelectionCoordinates(rot_x, rot_y);
		first_to_last_y = abs(rot_y[0] - rot_y[rot_y.length-1]);
		if(first_to_last_y < min_y){
			min_y_roi = createROI(rot_x, rot_y);
			theta = i;
			min_y = first_to_last_y;
		}
	}
	if(showDebug) { print("Minimum distance: "+min_y);}
	if(showDebug) { print("Rotated by "+theta+" degrees");}
	return min_y_roi;
}

// Translate the ROI to the 0,0 coordinates
// Input: ROI
// Output: ROI
function offsetToZeroROI(roi){
	x = ROI_to_x(roi);
	y = ROI_to_y(roi);
	
	min_x = 1000;
	min_y = 1000;
	
	for(i=0;i<x.length;i++){
		
		if(x[i] < min_x){
			min_x = x[i];
		}
		if(y[i] < min_y){
			min_y = y[i];
		}
	}
	for(i=0;i<x.length;i++){
		x[i] = x[i] - min_x;
		y[i] = y[i] - min_y;
	}
	new_roi = createROI(x, y);
	return new_roi;
}

// -------------------------------------------------
// ANGLE ROI FUNCTIONS
// -------------------------------------------------
// Create an AngleROI - extension to ROI
// Input: array of x, array of y, array of angles
// Output: AngleROI format array
function createAngleROI(x, y, a){
	// transfer x and y arrays to ROI format
	// 0 - length of array
	// 1 - first x
	// [0]+1 - first y
	// [0]*2+1 - first angle
	length = x.length;
	full_array = createMultiDimensionalArray(length, 3);
	full_array = populateMultiDimensionalArray(full_array, x, 1);
	full_array = populateMultiDimensionalArray(full_array, y, 2);
	full_array = populateMultiDimensionalArray(full_array, a, 3);
	return full_array;
}

// convert an AngleROI selection to array of y coordinates
// Input: AngleROI
// Output: array of y values 
function AngleROI_to_x(array){
	result = readMultiDimensionalArray(array, 1); 
	return result;
}

function AngleROI_to_y(array){
	result = readMultiDimensionalArray(array, 2); 
	return result;
}

function AngleROI_to_angle(array){
	result = readMultiDimensionalArray(array, 3); 
	return result;
}

// given an AngleROI and an index, reorder the array
// so that the index is the first entry
// Input: AngleROI
//		  index (integer)
// Returns: AngleROI
function shuffleAngleROI(array, index){

	x = AngleROI_to_x(array);
	y = AngleROI_to_y(array);
	a = AngleROI_to_angle(array);

	x1 = Array.slice(x,0,index);
	x2 = Array.slice(x,index);

	y1 = Array.slice(y,0,index);
	y2 = Array.slice(y,index);

	a1 = Array.slice(a,0,index);
	a2 = Array.slice(a,index);

	x_new = Array.concat(x2,x1);
	y_new = Array.concat(y2,y1);
	a_new = Array.concat(a2,a1);

	roi = createAngleROI(x_new, y_new, a_new);

	return roi;
}

// -------------------------------------------------
// MULTIDIMENSIONAL ARRAY FUNCTIONS
// -------------------------------------------------

function createMultiDimensionalArray(length, dimensions){
	// transfer x and y arrays to ROI format
	// 0 - length of array
	// 1 - number of dimensions
	full_length = length * dimensions + 2;

	array = newArray(full_length);
	array[0] = length;
	array[1] = dimensions;
	return array;
}

function populateMultiDimensionalArray(multiarray, inputarray, dimension){
	// ([0]*0)+2 - first dimension
	// ([0]*1)+2 - second dimension
	// ([0]*2)+2 - third dimension

	for(i=0;i<inputarray.length;i++){
		position = (multiarray[0] * (dimension-1)) + 2 + i;
		multiarray[position] = inputarray[i];
	}
	return multiarray;
}

function readMultiDimensionalArray(multiarray, dimension){

	length = multiarray[0];
	array = newArray(length);
	for(i=0;i<length;i++){
		position = ( length *( dimension-1)) + 2 + i;
		array[i] = multiarray[position];
	}
	return array;
}

function printMultiDimensionalArray(multiarray, file){

	length = multiarray[0];
	dimensions = multiarray[1];

	if(showDebug){
		print("Output: "+file);
	}

	for(i=0;i<length;i++){

		line = "";
		for(j=0; j<dimensions; j++){

			position = (length*j) + 2 + i;
			if(j==0){
				line = ""+multiarray[position];
			} else {
				line = line+"\t"+multiarray[position];
			}
		}
		if(file != ""){
			File.append(line, file); 
		} else {
			print(line);
		}
	
	}
}

function getValuesAtMultiDimensionalArrayIndex(multiarray, index){

	length = multiarray[0];
	dimensions = multiarray[1];
	result = newArray(dimensions);

	for(j=0; j<dimensions; j++){

		position = (length*j) + 2 + index;
		result[j] = multiarray[position];
	}
	return result;
}