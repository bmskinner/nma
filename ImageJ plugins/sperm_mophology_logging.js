/*
Copyright Ben Skinner 2014

Functions for logging nuclear organisation
information

*/
function addToLog(field, value){
	
	logFields = Array.concat(logFields, field);
	logValues = Array.concat(logValues, value);
}

function printLog(file){
	
	print(file, "~ ------------------");
	for(i=0;i<logFields.length;i++){
		print(file, logFields[i]+"~"+logValues[i]);
	}
}


// Empty log assumes global variables have been defined for
// logFields and logValues
function emptyLog(){
	logFields = newArray();
	logValues = newArray();
}

function checkLogFilePath(logDir, logFileName, n){
	
	logFile = logDir + "//" + logFileName + "." + n +".txt";
	if(File.exists(logFile)){
		n++;
		logFile = checkLogFilePath(logDir, logFileName, n);
	}
	return logFile;
}

function getLogValue(field){

	fieldID = 0;
	// if(showDebug){ showLog(); }
	for(i=0;i<logFields.length;i++){
		if(logFields[i] == field){
			fieldID = i;
			// if(showDebug){ print("Set fieldID to "+i);}
		}
	}
	// if(showDebug){ print("Field: "+ fieldID);}
	return logValues[fieldID];
}

function showLog(){
	
	print("------------------");
	print("Output of current log:");
	for(i=0;i<logFields.length;i++){
		print(logFields[i]+"    "+logValues[i]);
	}
}