
//Green signal to red signal converting macro for ImageJ.

//(C) 2007 Ben Skinner

/**
This macro will convert green signals to red signals. Red signals are discarded. Images are saved to a new folder in the source directory called "Red". This folder can then be used in the "Domain Analysis" macro.
*/

macro "Greyscale to blue" {
setBatchMode(true);
dir = getDirectory("Choose a directory");
path = dir+"\/RGB";
File.makeDirectory(path);
openfiles();
setBatchMode(false);
}

function openfiles() {
list = getFileList(dir);
	count = 1;
	for (i=0; i<list.length; i++) {
		if (
		  endsWith(list[i], ".jpg") ||
		  endsWith(list[i], ".tif") ||
		  endsWith(list[i], ".tiff")
		) {
			print("image " + (count++) + ": " + dir + list[i]);
			open(dir + list[i]);
			imageID = getImageID();
			makeBlue();
		} else {
			// print("non image " + (count++) + ": " + dir + list[i]);
		}
	}

function makeBlue() {
run("Duplicate...", "title=Split");
run("RGB Color");
run("Merge Channels...", "c3=Split");
saveAs("tiff",dir+"\/RGB\/"+list[i]);
close();
close();
//saveAs("Tiff", "/Users/fishuser/Ben/Analysis/RGB/P1.tif");
//close();
//selectWindow("P1.tiff");
//close();


//run("RGB Split");
//run("RGB Merge...", "red=[Split (green)] green=*None* blue=[Split (blue)]");

}
