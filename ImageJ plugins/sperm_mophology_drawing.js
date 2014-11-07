/*
Copyright Ben Skinner 2014

*/

// -------------------------------------------------
// DRAWING FUNCTIONS
// -------------------------------------------------
function drawRectangleAtPoint(point, colour){
	// a rectangle at x-1, y-1, 3x3 square so centred on x,y
	setColor(colour);
	drawRect(point[0]-1, point[1]-1, 3, 3); 
}

function drawLineBetweenPoints(pointA, pointB, colour){
	
	setColor(colour);
	drawLine(pointA[0], pointA[1], pointB[0], pointB[1]);
}