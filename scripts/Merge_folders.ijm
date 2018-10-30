
//Grey signal to blue signal converting macro for ImageJ.

//(C) 2015 Ben Skinner

/**
This macro will convert greyscale images to blue signals. Images are saved to a new folder in the source directory called "RGB". This folder can then be used in the "Domain Analysis" macro.
*/

macro "Merge folders" {
setBatchMode(true);
dirBlue = getDirectory("Choose a blue directory");
dirRed = getDirectory("Choose a red directory");
dirGreen = getDirectory("Choose a green directory");

topDir = File.getParent(dirBlue);
path = topDir+"\/RGB";
File.makeDirectory(path);
openfiles();
setBatchMode(false);
}

function openfiles() {
list = getFileList(dirBlue);
	count = 1;
	for (i=0; i<list.length; i++) {

		imageName = list[i];

		if(startsWith(imageName, "._")){ // skip resource fork
			continue;
		}

		if (
		  endsWith(imageName, ".jpg") ||
		  endsWith(imageName, ".tif") ||
		  endsWith(imageName, ".tiff")
		) {
			print("image " + (count++) + ": " + dirBlue + imageName);

			open(dirBlue + imageName);
			run("Rename...", "title=Blue");

			open(dirRed + imageName);
			run("Rename...", "title=Red");

			open(dirGreen + imageName);
			run("Rename...", "title=Green");

			mergeImages(imageName);
		} else {
			// print("non image " + (count++) + ": " + dir + list[i]);
		}
	}

function mergeImages(imageName) {
	run("Merge Channels...", "c1=Red c2=Green c3=Blue");
	saveAs("tiff",path+"\/"+imageName);
	close();
}
