/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.examples.fiducial;

import boofcv.abst.fiducial.FiducialDetector;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.io.UtilIO;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.se.Se3_F64;

/**
 * @author Peter Abeles
 */
public class ExampleFiducialNumber {
	public static void main(String[] args) {

		String directory = "/home/pja/projects/boofcv/evaluation/fiducial/";

		// load the lens distortion parameters and the input image
		IntrinsicParameters param = UtilIO.loadXML(directory + "intrinsic.xml");
		ImageFloat32 original = UtilImageIO.loadImage(directory+"foo.png",ImageFloat32.class);

		// Detect the fiducial
		FiducialDetector<ImageFloat32> detector = FactoryFiducial.squareBinaryRobust(6,4,20,ImageFloat32.class);
		detector.setIntrinsic(param);
		detector.detect(original);

		// print the results
		Se3_F64 targetToSensor = new Se3_F64();
		for (int i = 0; i < detector.totalFound(); i++) {
			System.out.println("Target ID = "+detector.getId(i));
			detector.getFiducialToWorld(i, targetToSensor);
			System.out.println("Location:");
			System.out.println(targetToSensor);
		}
	}
}
