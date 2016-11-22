package analysis;

import java.io.Serializable;

import logging.Loggable;

public interface ICannyOptions extends Serializable, Loggable{

	double DEFAULT_CANNY_LOW_THRESHOLD = 0.5;
	double DEFAULT_CANNY_HIGH_THRESHOLD = 1.5;
	double DEFAULT_CANNY_TAIL_LOW_THRESHOLD = 0.1;
	double DEFAULT_CANNY_TAIL_HIGH_THRESHOLD = 0.5;
	double DEFAULT_CANNY_KERNEL_RADIUS = 3;
	int DEFAULT_CANNY_KERNEL_WIDTH = 16;
	int DEFAULT_CLOSING_OBJECT_RADIUS = 5;
	int DEFAULT_TAIL_CLOSING_OBJECT_RADIUS = 3;
	int DEFAULT_KUWAHARA_KERNEL_RADIUS = 3;
	boolean DEFAULT_USE_KUWAHARA = true;
	boolean DEFAULT_FLATTEN_CHROMOCENTRES = true;
	int DEFAULT_FLATTEN_THRESHOLD = 100;
	boolean DEFAULT_ADD_BORDER = false;

	boolean isUseCanny();

	void setUseCanny(boolean useCanny);

	boolean isUseFlattenImage();

	void setFlattenImage(boolean flattenImage);

	int getFlattenThreshold();

	void setFlattenThreshold(int flattenThreshold);

	boolean isUseKuwahara();

	void setUseKuwahara(boolean b);

	int getKuwaharaKernel();

	void setKuwaharaKernel(int radius);

	int getClosingObjectRadius();

	void setClosingObjectRadius(int closingObjectRadius);

	boolean isCannyAutoThreshold();

	void setCannyAutoThreshold(boolean cannyAutoThreshold);

	float getLowThreshold();

	void setLowThreshold(float lowThreshold);

	float getHighThreshold();

	void setHighThreshold(float highThreshold);

	float getKernelRadius();

	void setKernelRadius(float kernelRadius);

	int getKernelWidth();

	void setKernelWidth(int kernelWidth);
	
	boolean isAddBorder();
	
	void setAddBorder(boolean b);

}