package com.bmskinner.nma.components.datasets;

import java.io.File;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.signals.INuclearSignal;

/**
 * Provide shortcuts to common image file-related tasks in cell collections
 * 
 * @since 2.4.1
 * 
 */
public class ImageManager {
	private static final Logger LOGGER = Logger.getLogger(ImageManager.class.getName());
	private final ICellCollection collection;

	public ImageManager(final ICellCollection collection) {
		this.collection = collection;
	}

	/**
	 * Update image paths for all the cells in the collection to refer to the given
	 * directory
	 * 
	 * @param newDir
	 */
	public void updateImageDirectory(@NonNull File newDir) {

		if (!newDir.isDirectory()) {
			LOGGER.warning("Selected file is not a directory: %s".formatted(newDir.getAbsolutePath()));
			return;
		}

		if (!newDir.exists()) {
			LOGGER.warning("Selected directory does not exist: %s".formatted(newDir.getAbsolutePath()));
			return;
		}

		LOGGER.fine("Updating collection '%s 'image directory to '%s'".formatted(collection.getName(),
				newDir.getAbsolutePath()));


		for (final Nucleus n : collection.getNuclei()) {

			final File oldDir = n.getSourceFolder();
			n.setSourceFolder(newDir);

			// If nuclear signals were in the same directory as the nuclear stain,
			// update signals. Otherwise do not change.
			for (final INuclearSignal s : n.getSignalCollection().getAllSignals()) {
				if (s.getSourceFolder().equals(oldDir)) {
					s.setSourceFolder(newDir);
				}
			}
		}
	}

	/**
	 * Update image paths for all the cells in the collection with the given source
	 * folder to refer to the given directory. Any cells that do not have the given
	 * source folder will not be changed.
	 * 
	 * @param oldDir the original image directory for nuclear images
	 * @param newDir the new image directory for nuclear images
	 */
	public void updateImageDirectory(@NonNull File oldDir, @NonNull File newDir) {

		// Don't check validity of the old directory, it may not exist if files have
		// been moved

		if (!newDir.isDirectory()) {
			LOGGER.warning("Selected new file is not a directory: %s".formatted(newDir.getAbsolutePath()));
			return;
		}

		if (!newDir.exists()) {
			LOGGER.warning("Selected directory does not exist: %s".formatted(newDir.getAbsolutePath()));
			return;
		}

		LOGGER.fine("Updating collection '%s 'image directory from '%s' to '%s'".formatted(collection.getName(),
				oldDir.getAbsolutePath(),
				newDir.getAbsolutePath()));

		for (final Nucleus n : collection.getNuclei()) {

			// Skip any nuclei that are not in the original directory
			if (!oldDir.equals(n.getSourceFolder())) {
				continue;
			}

			n.setSourceFolder(newDir);

			// If nuclear signals were in the same directory as the nuclear stain,
			// update signals. Otherwise do not change.
			for (final INuclearSignal s : n.getSignalCollection().getAllSignals()) {
				if (s.getSourceFolder().equals(oldDir)) {
					s.setSourceFolder(newDir);
				}
			}
		}
	}

}
