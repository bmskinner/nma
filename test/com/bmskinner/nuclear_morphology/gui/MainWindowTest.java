/*******************************************************************************
 *      Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.bmskinner.nuclear_morphology.gui;

import static org.junit.Assert.*;

import java.awt.Frame;
import java.io.IOException;

import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.core.BasicRobot;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.Robot;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.image.ScreenshotTaker;
import org.assertj.swing.image.ScreenshotTakerIF;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.core.Nuclear_Morphology_Analysis;

import static org.assertj.core.util.Files.currentFolder;
import static org.assertj.swing.launcher.ApplicationLauncher.application;
import static org.assertj.swing.finder.WindowFinder.findFrame;
/**
 * Test the main GUI 
 * @author bms41
 *
 */
public class MainWindowTest extends AssertJSwingJUnitTestCase {

    private static String SCREENSHOT_FOLDER = "/screens/";

    @Override
    public void onSetUp(){
        application(Nuclear_Morphology_Analysis.class).start();
    }
    

    @Test @GUITest
    public void test() throws IOException {

        FrameFixture frame = findFrame(new GenericTypeMatcher<Frame>(Frame.class) {
            protected boolean isMatching(Frame frame) {
                boolean isMatching = "Nuclear Morphology Analysis v1.14.0".equals(frame.getTitle()) && frame.isShowing();
                return isMatching;
            }
        }).using(robot());
        
        
        ScreenshotTakerIF st = new ScreenshotTaker();
        String folder = currentFolder().getCanonicalPath()+SCREENSHOT_FOLDER;
        System.out.println("Saving to: "+ folder+"Main.png");
        st.saveDesktopAsPng(folder+"Desk.png");
        
        
//        st.saveComponentAsPng(frame., folder+"Main.png");
    }

}
