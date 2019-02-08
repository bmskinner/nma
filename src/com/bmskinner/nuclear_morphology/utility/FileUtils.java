package com.bmskinner.nuclear_morphology.utility;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

/**
 * File handling utility methods
 * @author bms41
 * @since 1.15.1
 *
 */
public class FileUtils {
	
    /**
     * Given a list of files, find the component of their paths that
     * is shared amongst them; i.e. there most recent common ancestor.
     * @param files the files to compare
     * @return the section of their path in common, or the default system directory if there is no common path
     */
    public static File commonPathOfFiles(@NonNull Collection<File> files) {
    	String[][] folders = new String[files.size()][];

        int k = 0;

        // Split out the path elements to an array
        for (File f : files) {

            Path p = f.toPath();
            
            if(p!=null){

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
        List<String> common = new ArrayList<String>();
        for (int col = 0; col < folders[0].length; col++) {

            if (breakLoop) {
                break;
            }
            // Get first row
            String s = folders[0][col];

            for (int row = 1; row < files.size(); row++) {
                if (!s.equals(folders[row][col])) {
                    breakLoop = true;
                    break;
                }
            }
            if (breakLoop == false)
                common.add(s);

        }

        String commonPath = "";
        for (int i = 0; i < common.size(); i++) {
            commonPath += common.get(i);
            if (i > 0 && i < common.size() - 1) { // don't add separator after
                                                  // root or at the end
                commonPath += File.separator;
            }
        }

        return new File(commonPath);
    }
    
    /**
     * Given a file path, return the component that exists.
     * If the file exists, the same file is returned. Otherwise, the most complete
     * existing directory path is returned. If none of the path exists, the default system
     * directory is returned. 
     * @param file the path to test
     * @return the most complete existing portion of the path
     */
    public static File extantComponent(File file) {
    	if(file.exists())
    		return file;
    	if(file==null)
    		return null;
    	return extantComponent(file.getParentFile());
    }

}
