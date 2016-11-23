package analysis;

import java.io.Serializable;

import logging.Loggable;

/**
 * The parameters for edge detection using the Canny algorithm, and
 * the image pre-processing options to apply.
 * @author bms41
 * @since 1.13.3
 *
 */
public interface ICannyOptions extends Serializable, Loggable{

	// The default values below work for our mouse sperm images
	// pretty well.
	
	
	static final float   DEFAULT_CANNY_LOW_THRESHOLD       = 0.5f;
	static final float   DEFAULT_CANNY_HIGH_THRESHOLD      = 1.5f;
	static final float   DEFAULT_CANNY_TAIL_LOW_THRESHOLD  = 0.1f;
	static final float   DEFAULT_CANNY_TAIL_HIGH_THRESHOLD = 0.5f;
	static final float   DEFAULT_CANNY_KERNEL_RADIUS       = 3;
	static final int     DEFAULT_CANNY_KERNEL_WIDTH        = 16;
	static final int     DEFAULT_CLOSING_OBJECT_RADIUS     = 5;
	static final int     DEFAULT_TAIL_CLOSING_OBJECT_RADIUS= 3;
	static final int     DEFAULT_KUWAHARA_KERNEL_RADIUS    = 3;
	static final boolean DEFAULT_USE_KUWAHARA              = true;
	static final boolean DEFAULT_FLATTEN_CHROMOCENTRES     = true;
	static final boolean DEFAULT_USE_CANNY                 = true;
	static final boolean DEFAULT_AUTO_THRESHOLD            = false;
	static final int     DEFAULT_FLATTEN_THRESHOLD         = 100;
	static final boolean DEFAULT_ADD_BORDER                = false;

	
	/**
	 * Create a copy of this options
	 * @return
	 */
	ICannyOptions duplicate();
	
	/**
	 * Should edge detection be run?
	 * @return
	 */
	boolean isUseCanny();

	

	boolean isUseFlattenImage();

	

	int getFlattenThreshold();

	

	boolean isUseKuwahara();

	

	int getKuwaharaKernel();

	

	int getClosingObjectRadius();

	

	boolean isCannyAutoThreshold();

	

	float getLowThreshold();

	

	float getHighThreshold();

	
	float getKernelRadius();

	

	int getKernelWidth();

	
	
	boolean isAddBorder();
	

}