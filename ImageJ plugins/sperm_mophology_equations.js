/*
Copyright Ben Skinner 2014

General functions for calculating equations
and handling trigonometry

*/

// -------------------------------------------------
// GENERAL FUNCTIONS
// -------------------------------------------------
function getLength(pointA, pointB) {
	dx = pointB[0]-pointA[0]; 
	dy = pointB[1]-pointA[1];
	length = sqrt(dx*dx+dy*dy);
	return length;
}

function getX(eq, y){
	// x = (y-c)/m
	x = (y - eq[1]) / eq[0];
	return x;
}

function getY(eq, x){
	// y = mx +c
	y = (eq[0] * x) + eq[1];
	return y;
}

function getLineCoordinates(eq, w, h){
	
	// the start and end points of the line may be offscreen for given x coordinates
	// find the best start and end points
	x_start = 0;
	x_end = w - 1;
	
	y_start = getY(eq, x_start); // (eq[0] * 1) + eq[1];
	y_end =  getY(eq, x_end); //(eq[0] * (w - 1)) + eq[1];
	
	// write("Ideal line: "+x_start+","+y_start+" to "+x_end+","+y_end);
		
	if(y_start > h - 1){ // too high
		x_start = getX(eq, h - 1);
		y_start = h - 1;
		
	}
	if(y_start < 0){ 
		x_start = getX(eq, 0);
		y_start = 0;
	}
	
	if(y_end > h - 1){
		x_end = getX(eq, h - 1);
		y_end = h - 1;
	}

	if(y_end < 0){ 
		x_end = getX(eq, 0);
		y_end = 0;
	}
	// write("Final line: "+x_start+","+y_start+" to "+x_end+","+y_end);
	result=newArray(x_start, y_start, x_end, y_end);
	return result;
}

function calculateLineEquation(position_1, position_2){
	
	delta_x = position_1[0] - position_2[0];
	delta_y = position_1[1] - position_2[1];
	
	m = delta_y / delta_x;
	
	// y - y1 = m(x - x1)
	c = position_1[1] -  ( m * position_1[0] );
	
	testy = (m * position_2[0]) + c;
	
	// write("y = "+m+"x + "+c);
	result=newArray(m, c);
	return result;
	
}
