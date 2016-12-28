package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.util.Set;

public interface ImageSet {

	int size();
	
	ImageType getType(int i);

	int getPosition(ImageType s);
	
	Set<ImageType> values();
}
