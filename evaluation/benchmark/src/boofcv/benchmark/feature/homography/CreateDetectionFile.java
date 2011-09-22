/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.benchmark.feature.homography;

import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_I32;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;


/**
 * Detects the locations of features inside each image and saves to a file.  All the files
 * in a directory are processed that have the appropriate extension.
 *
 * @author Peter Abeles
 */
public class CreateDetectionFile<T extends ImageBase> {

	// algorithm that detects the features
	InterestPointDetector<T> alg;
	// type of input image
	Class<T> imageType;
	// name of the detector
	String detectorName;

	/**
	 * Configures detector
	 *
	 * @param alg Algorithm used to detect interest points.
	 * @param imageType Primitive of input image that is processed.
	 * @param detectorName Name of the detector.  Put into output file name.
	 */
	public CreateDetectionFile(InterestPointDetector<T> alg, Class<T> imageType, String detectorName) {
		this.alg = alg;
		this.imageType = imageType;
		this.detectorName = detectorName;
	}

	/**
	 * Processes all images found inside a directory.
	 *
	 * @param directoryPath Path to directory containing input images.
	 * @param imageSuffix Type of input image.
	 * @throws FileNotFoundException
	 */
	public void directory( String directoryPath , String imageSuffix ) throws FileNotFoundException {
		File dir = new File(directoryPath);
		if( !dir.isDirectory() )
			throw new IllegalArgumentException("Path does not point to a directory!");

		int totalProcessed = 0;
		File[] files = dir.listFiles();
		for( File f : files ) {
			if( !f.isFile() || !f.getName().endsWith(imageSuffix))
				continue;

			System.out.println("Detecting features inside of: "+f.getName());

			BufferedImage image = UtilImageIO.loadImage(f.getPath());

			String imageName = f.getName();
			directoryPath = f.getParent();
			imageName = imageName.substring(0,imageName.length()-imageSuffix.length());

			process(image,directoryPath+"/DETECTED_"+imageName+"_"+detectorName+".txt");
			totalProcessed++;
		}
		System.out.println("Total Processed: "+totalProcessed);
	}

	/**
	 * Detects features in the specified image and saves the output to the specified file.
	 * @param input Input image that features are detected inside of.
	 * @param outputName Name of output file.
	 * @throws FileNotFoundException
	 */
	public void process( BufferedImage input , String outputName ) throws FileNotFoundException {
		T image = ConvertBufferedImage.convertFrom(input,null,imageType);

		alg.detect(image);

		FileOutputStream fos = new FileOutputStream(outputName);
		PrintStream out = new PrintStream(fos);

		for( int i = 0; i < alg.getNumberOfFeatures(); i++ ) {
			Point2D_I32 pt = alg.getLocation(i);
			double scale = alg.getScale(i);
			out.printf("%d %d %.5f\n",pt.getX(),pt.getY(),scale);
		}
		out.close();
	}

	public static <T extends ImageBase>
	void doStuff( String directory , String suffix , Class<T> imageType ) throws FileNotFoundException {
		InterestPointDetector<T> alg = FactoryInterestPoint.fromFastHessian(-1,9,4,4);
		CreateDetectionFile<T> cdf = new CreateDetectionFile<T>(alg,imageType,"FH");
		cdf.directory(directory,suffix);
	}

	public static void main( String args[] ) throws FileNotFoundException {
//		doStuff("data/mikolajczk/bikes/",".png",ImageFloat32.class);
//		doStuff("data/mikolajczk/boat/",".png",ImageFloat32.class);
//		doStuff("data/mikolajczk/graf/",".png",ImageFloat32.class);
//		doStuff("data/mikolajczk/leuven/",".png",ImageFloat32.class);
//		doStuff("data/mikolajczk/ubc/",".png",ImageFloat32.class);
		doStuff("data/mikolajczk/trees/",".png",ImageFloat32.class);
	}

}