package com.bmskinner.nma.utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

/**
 * File handling utility methods
 * 
 * @author bms41
 * @since 1.15.1
 *
 */
public class FileUtils {

	private static final Logger LOGGER = Logger.getLogger(FileUtils.class.getName());

	/**
	 * Private constructor, all methods are static
	 */
	private FileUtils() {
	}

	/**
	 * Get the most recent common ancestor of the dataset save file paths
	 * 
	 * @param datasets the list of datasets.
	 * @return a file for the common directory. Check that the path exists and is a
	 *         directory before using this.
	 */
	public static File commonPathOfDatasets(@NonNull Collection<IAnalysisDataset> datasets) {
		List<File> files = new ArrayList<>(datasets.size());
		for (IAnalysisDataset d : datasets) {
			files.add(d.getSavePath().getParentFile());
		}
		return FileUtils.commonPathOfFiles(files);

	}

	/**
	 * Given a list of files, find the component of their paths that is shared
	 * amongst them; i.e. their most recent common ancestor.
	 * 
	 * @param files the files to compare
	 * @return the section of their path in common, or the default system directory
	 *         if there is no common path
	 */
	public static File commonPathOfFiles(@NonNull Collection<File> files) {

		String[][] folders = new String[files.size()][];

		int k = 0;

		// Split out the path elements to an array
		for (File f : files) {
			Path p = f.toPath();
			if (p != null) {
				Iterator<Path> it = p.iterator();
				List<String> s = new ArrayList<>();
				s.add(p.getRoot().toString());
				while (it.hasNext()) {
					Path n = it.next();
					s.add(n.toString());
				}
				folders[k++] = s.toArray(new String[0]);
			}
		}

		boolean breakLoop = false;
		List<String> common = new ArrayList<>();
		if (folders.length == 0 || folders[0] == null)
			return new File(System.getProperty("user.home"));

		for (int col = 0; col < folders[0].length; col++) {

			if (breakLoop)
				break;

			String s = folders[0][col];
			// Compare this column in every row to the first row
			for (int row = 1; row < files.size(); row++) {
				if (!s.equals(folders[row][col])) {
					breakLoop = true;
					break;
				}
			}
			if (!breakLoop)
				common.add(s);
		}

		// Make the final path from the common elements
		StringBuilder commonPath = new StringBuilder();
		for (int i = 0; i < common.size(); i++) {
			commonPath.append(common.get(i));
			if (i > 0 && i < common.size() - 1) { // don't add separator after root or at the end
				commonPath.append(File.separator);
			}
		}
		return new File(commonPath.toString());
	}

	/**
	 * Given a file path, return the element of the path that exist. If the file
	 * exists, the same file is returned. Otherwise, the most complete existing
	 * directory path is returned. If none of the path exists, or the input is null,
	 * the user home directory is returned.
	 * 
	 * @param file the file path to test
	 * @return the most complete existing portion of the path, otherwise the user
	 *         home directory
	 */
	public static File extantComponent(File file) {
		if (file == null)
			return new File(System.getProperty("user.home"));
		if (file.exists())
			return file;
		return extantComponent(file.getParentFile());
	}

	public static boolean copyFile(final File toCopy, final File destFile) {
		try {
			return FileUtils.copyStream(new FileInputStream(toCopy),
					new FileOutputStream(destFile));
		} catch (final FileNotFoundException e) {
			LOGGER.fine(String.format("Unable to copy file from %s to %s: %s",
					toCopy.getAbsolutePath(), destFile.getAbsolutePath(), e.getMessage()));
		}
		return false;
	}

	public static String removeStart(String str, String remove) {
		if (isEmpty(str) || isEmpty(remove)) {
			return str;
		}
		if (str.startsWith(remove)) {
			return str.substring(remove.length());
		}
		return str;
	}

	public static boolean isEmpty(CharSequence cs) {
		return cs == null || cs.length() == 0;
	}

	private static boolean copyFilesRecusively(final File toCopy,
			final File destDir) {
		assert destDir.isDirectory();

		if (!toCopy.isDirectory()) {
			return FileUtils.copyFile(toCopy, new File(destDir, toCopy.getName()));
		} else {
			final File newDestDir = new File(destDir, toCopy.getName());
			if (!newDestDir.exists() && !newDestDir.mkdir()) {
				return false;
			}
			for (final File child : toCopy.listFiles()) {
				if (!FileUtils.copyFilesRecusively(child, newDestDir)) {
					return false;
				}
			}
		}
		return true;
	}

	public static boolean copyJarResourcesRecursively(final File destDir,
			final JarURLConnection jarConnection) throws IOException {

		jarConnection.setUseCaches(false);
		final JarFile jarFile = jarConnection.getJarFile();

		for (final Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements();) {
			final JarEntry entry = e.nextElement();
			if (entry.getName().startsWith(jarConnection.getEntryName())) {
				final String filename = removeStart(entry.getName(), //
						jarConnection.getEntryName());

				final File f = new File(destDir, filename);
				if (!entry.isDirectory()) {
					final InputStream entryInputStream = jarFile.getInputStream(entry);
					if (!FileUtils.copyStream(entryInputStream, f)) {
						return false;
					}
					entryInputStream.close();
				} else {
					if (!FileUtils.ensureDirectoryExists(f)) {
						throw new IOException("Could not create directory: "
								+ f.getAbsolutePath());
					}
				}
			}
		}
		return true;
	}

	public static boolean copyResourcesRecursively( //
			final URL originUrl, final File destination) {
		try {
			final URLConnection urlConnection = originUrl.openConnection();
			if (urlConnection instanceof JarURLConnection jarUrlConnection) {
				return FileUtils.copyJarResourcesRecursively(destination,
						jarUrlConnection);
			} else {
				return FileUtils.copyFilesRecusively(new File(originUrl.getPath()),
						destination);
			}
		} catch (final IOException e) {
			LOGGER.fine(String.format("Unable to copy files from %s to %s: %s",
					originUrl.toString(), destination.getAbsolutePath(), e.getMessage()));
		}
		return false;
	}

	private static boolean copyStream(final InputStream is, final File f) {
		try {
			return FileUtils.copyStream(is, new FileOutputStream(f));
		} catch (final FileNotFoundException e) {
			LOGGER.fine(String.format("Unable to copy stream to %s: %s",
					f.getAbsolutePath(), e.getMessage()));
		}
		return false;
	}

	private static boolean copyStream(final InputStream is, final OutputStream os) {
		try {
			final byte[] buf = new byte[1024];

			int len = 0;
			while ((len = is.read(buf)) > 0) {
				os.write(buf, 0, len);
			}
			is.close();
			os.close();
			return true;
		} catch (final IOException e) {
			LOGGER.fine(String.format("Error copying stream: %s", e.getMessage()));
		}
		return false;
	}

	private static boolean ensureDirectoryExists(final File f) {
		return f.exists() || f.mkdir();
	}

}
