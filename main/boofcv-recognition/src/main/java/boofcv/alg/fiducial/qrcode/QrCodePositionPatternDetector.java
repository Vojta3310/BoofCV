/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.qrcode;

import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.alg.distort.RemovePerspectiveDistortion;
import boofcv.alg.fiducial.calib.squares.SquareGraph;
import boofcv.alg.fiducial.calib.squares.SquareNode;
import boofcv.alg.filter.binary.LinearContourLabelChang2004;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.alg.shapes.polygon.DetectPolygonFromContour;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.line.LineSegment2D_F64;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.struct.FastQueue;

import java.util.List;

/**
 * Detects position patterns for a QR code inside an image and forms a graph of ones which can potentially
 * be connected together. Squares are detected in the image and position patterns are found based on their appearance.
 *
 *
 * @author Peter Abeles
 */
public class QrCodePositionPatternDetector<T extends ImageGray<T>> {

	InterpolatePixelS<T> interpolate;

	// maximum QR code version that it can detect
	int maxVersionQR;

	// Detects squares inside the image
	DetectPolygonBinaryGrayRefine<T> squareDetector;

	FastQueue<PositionPatternNode> positionPatterns = new FastQueue<>(PositionPatternNode.class,true);
	SquareGraph graph = new SquareGraph();

	// Nearst Neighbor Search related variables
	private NearestNeighbor<PositionPatternNode> search = FactoryNearestNeighbor.kdtree();
	private FastQueue<double[]> searchPoints;
	private FastQueue<NnData<PositionPatternNode>> searchResults = new FastQueue(NnData.class,true);

	// Computes a mapping to remove perspective distortion
	private RemovePerspectiveDistortion<?> removePerspective = new RemovePerspectiveDistortion(70,70);

	// Workspace for checking to see if two squares should be connected
	protected LineSegment2D_F64 lineA = new LineSegment2D_F64();
	protected LineSegment2D_F64 lineB = new LineSegment2D_F64();
	protected LineSegment2D_F64 connectLine = new LineSegment2D_F64();
	protected Point2D_F64 intersection = new Point2D_F64();

	// runtime profiling
	protected double milliGraph = 0;

	// storage for nearest neighbor
	double point[] = new double[2];

	/**
	 * Configures the detector
	 *
	 * @param squareDetector Square detector
	 * @param maxVersionQR Maximum QR code version it can detect.
	 */
	public QrCodePositionPatternDetector(DetectPolygonBinaryGrayRefine<T> squareDetector , int maxVersionQR) {

		// verify and configure polygon detector
		if( squareDetector.getMinimumSides() != 4 || squareDetector.getMaximumSides() != 4 )
			throw new IllegalArgumentException("Must detect 4 and only 4 sided polygons");
		if( squareDetector.getDetector().isOutputClockwise() )
			throw new IllegalArgumentException("Must be CCW");
		this.squareDetector = squareDetector;
		this.maxVersionQR = maxVersionQR;

		// set up nearest neighbor search for 2-DOF
		search.init(2);
		searchPoints = new FastQueue<double[]>(double[].class,true) {
			@Override
			protected double[] createInstance() {
				return new double[2];
			}
		};

		interpolate = FactoryInterpolation.bilinearPixelS(squareDetector.getInputType(), BorderType.EXTENDED);
	}

	public void resetRuntimeProfiling() {
		squareDetector.resetRuntimeProfiling();
		milliGraph = 0;
	}

	/**
	 * Detects position patterns inside the image and forms a graph.
	 * @param gray Gray scale input image
	 * @param binary Thresholed version of gray image.
	 */
	public void process(T gray, GrayU8 binary ) {
		configureContourDetector(gray);
		recycleData();
		positionPatterns.reset();
		interpolate.setImage(gray);

		// detect squares
		squareDetector.process(gray,binary);

		long time0 = System.nanoTime();
		squaresToPositionList();

		long time1 = System.nanoTime();

		// Create graph of neighboring squares
		createPositionPatternGraph();
//		long time2 = System.nanoTime();  // doesn't take very long

		double milli = (time1-time0)*1e-6;

		if( milliGraph == 0 ) {
			milliGraph = milli;
		} else {
			milliGraph = 0.95*milliGraph + 0.5*milli;
		}

		DetectPolygonFromContour<T> detectorPoly = squareDetector.getDetector();
		System.out.printf(" contour %5.1f shapes %5.1f adjust_bias %5.2f PosPat %6.2f\n",
				detectorPoly.getMilliContour(),detectorPoly.getMilliShapes(),squareDetector.getMilliAdjustBias(),
				milliGraph);
	}

	/**
	 * Configures the contour detector based on the image size. Setting a maximum contour and turning off recording
	 * of inner contours and improve speed and reduce the memory foot print significantly.
	 */
	private void configureContourDetector(T gray) {
		// determine the maximum possible size of a position pattern
		// contour size is maximum when viewed head one. Assume the smallest qrcode is 3x this width
		// 4 side in a square
		int maxContourSize = Math.min(gray.width,gray.height)*4/3;
		LinearContourLabelChang2004 contourFinder = squareDetector.getDetector().getContourFinder();
		contourFinder.setMaxContourSize(maxContourSize);
		contourFinder.setSaveInternalContours(false);
	}

	protected void recycleData() {
		for (int i = 0; i < positionPatterns.size(); i++) {
			SquareNode n = positionPatterns.get(i);
			for (int j = 0; j < n.edges.length; j++) {
				if (n.edges[j] != null) {
					graph.detachEdge(n.edges[j]);
				}
			}
		}
		positionPatterns.reset();
	}

	/**
	 * Takes the detected squares and turns it into a list of {@link PositionPatternNode}.
	 */
	private void squaresToPositionList() {
		this.positionPatterns.reset();
		List<DetectPolygonFromContour.Info> infoList = squareDetector.getPolygonInfo();
		for (int i = 0; i < infoList.size(); i++) {
			DetectPolygonFromContour.Info info = infoList.get(i);

			// squares with no internal contour cannot possibly be a finder pattern
			if( !info.hasInternal() )
				continue;

			// See if the appearance matches a finder pattern
			double grayThreshold = (info.edgeInside+info.edgeOutside)/2;
			if( !checkPositionPatternAppearance(info.polygon,(float)grayThreshold))
				continue;

			// refine the edge estimate
			squareDetector.refine(info);

			PositionPatternNode pp = this.positionPatterns.grow();
			pp.reset();
			pp.square = info.polygon;
			pp.grayThreshold = grayThreshold;

			graph.computeNodeInfo(pp);
		}
	}

	/**
	 * Connects together position patterns. For each square, finds all of its neighbors based on center distance.
	 * Then considers them for connections
	 */
	private void createPositionPatternGraph() {
		// Add items to NN search
		searchPoints.resize(positionPatterns.size());
		for (int i = 0; i < positionPatterns.size(); i++) {
			PositionPatternNode f = positionPatterns.get(i);
			double[] p = searchPoints.get(i);
			p[0] = f.center.x;
			p[1] = f.center.y;
		}
		search.setPoints(searchPoints.toList(),positionPatterns.toList());

		for (int i = 0; i < positionPatterns.size(); i++) {
			PositionPatternNode f = positionPatterns.get(i);

			// The QR code version specifies the number of "modules"/blocks across the marker is
			// A position pattern is 7 blocks. A version 1 qr code is 21 blocks. Each version past one increments
			// by 4 blocks. The search is relative to the center of each position pattern, hence the - 7
			double maximumQrCodeWidth = f.largestSide*(17+4*maxVersionQR-7.0)/7.0;
			double searchRadius = 1.2*maximumQrCodeWidth; // search 1/2 the width + some fudge factor
			searchRadius*=searchRadius;

			point[0] = f.center.x;
			point[1] = f.center.y;

			// Connect all the finder patterns which are near by each other together in a graph
			search.findNearest(point,searchRadius,Integer.MAX_VALUE,searchResults);

			if( searchResults.size > 1) {
				for (int j = 0; j < searchResults.size; j++) {
					NnData<PositionPatternNode> r = searchResults.get(j);

					if( r.data == f ) continue; // skip over if it's the square that initiated the search

					considerConnect(f,r.data);
				}
			}
		}
	}

	/**
	 * Connects the 'candidate' node to node 'n' if they meet several criteria.  See code for details.
	 */
	void considerConnect(SquareNode node0, SquareNode node1) {
		// Find the side on each line which intersects the line connecting the two centers
		lineA.a = node0.center;
		lineA.b = node1.center;

		int intersection0 = graph.findSideIntersect(node0,lineA,intersection,lineB);
		connectLine.a.set(intersection);
		int intersection1 = graph.findSideIntersect(node1,lineA,intersection,lineB);
		connectLine.b.set(intersection);

		if( intersection1 < 0 || intersection0 < 0 ) {
			return;
		}

		double side0 = node0.sideLengths[intersection0];
		double side1 = node1.sideLengths[intersection1];

		// it should intersect about in the middle of the line

		double sideLoc0 = connectLine.a.distance(node0.square.get(intersection0))/side0;
		double sideLoc1 = connectLine.b.distance(node1.square.get(intersection1))/side1;

		if( Math.abs(sideLoc0-0.5)>0.35 || Math.abs(sideLoc1-0.5)>0.35 )
			return;

		// see if connecting sides are of similar size
		if( Math.abs(side0-side1)/Math.max(side0,side1) > 0.25 ) {
			return;
		}

		// Checks to see if the two sides selected above are closest to being parallel to each other.
		// Perspective distortion will make the lines not parallel, but will still have a smaller
		// acute angle than the adjacent sides
		if( !graph.almostParallel(node0, intersection0, node1, intersection1)) {
			return;
		}

		double ratio = Math.max(node0.smallestSide/node1.largestSide ,
				node1.smallestSide/node0.largestSide);

//		System.out.println("ratio "+ratio);
		if( ratio > 1.3 )
			return;

		graph.checkConnect(node0,intersection0,node1,intersection1,lineA.getLength2());
	}

	float lineX[] = new float[7];
	float lineY[] = new float[7];
	Point2D_F32 imagePixel = new Point2D_F32();
	/**
	 * Determines if the found polygon looks like a position pattern. A horizontal and vertical line are sampled.
	 * At each sample point it is marked if it is above or below the binary threshold for this square. Location
	 * of sample points is found by "removing" perspective distortion.
	 */
	boolean checkPositionPatternAppearance( Polygon2D_F64 square , float grayThreshold ) {

		// TODO Improve accuracy for small shapes
		// lines can be inaccurate. That appears to cause a significant number of false negatives
		// maybe sample outside of the line and see if it's black. If it is add an offset.
		// seems to rarely extend outside beyound the true square.
		// Also increasing number of samples and having a soft threshold seems to help
		// need to test against a more exhaustive set of images before doing these optimizations

		// create a mapping assuming perspective distortion
		// NOTE: Order doesn't matter here as long as the square is CW or CCW
		if( !removePerspective.createTransform(square.get(0),square.get(1),square.get(2),square.get(3)) )
			return false;

		// with Perspective removed to Image coordinates.
		PointTransformHomography_F32 p2i = removePerspective.getTransform();

		// Sample horizontal nad vertical scan lines which are approximately in the middle of the shape.
		for (int i = 0; i < 7; i++) {
			float location = 10*i+5;
			p2i.compute(location,35,imagePixel);
			lineX[i] = interpolate.get(imagePixel.x,imagePixel.y);
			p2i.compute(35,location,imagePixel);
			lineY[i] = interpolate.get(imagePixel.x,imagePixel.y);
		}

		// see if the change in intensity matched the expected pattern
		if( !positionSquareIntensityCheck(lineX,grayThreshold))
			return false;

		return positionSquareIntensityCheck(lineY,grayThreshold);
	}

	/**
	 * Checks to see if the array of sampled intensity values follows the expected pattern for a position pattern.
	 * X.XXX.X where x = black and . = white.
	 */
	static boolean positionSquareIntensityCheck(float values[] , float threshold ) {
		if( values[0] > threshold || values[1] < threshold )
			return false;
		if( values[2] > threshold || values[3] > threshold || values[4] > threshold  )
			return false;
		if( values[5] < threshold || values[6] > threshold )
			return false;
		return true;
	}

	/**
	 * Returns a list of all the detected position pattern squares and the other PP that they are connected to.
	 * @return List of PP
	 */
	public FastQueue<PositionPatternNode> getPositionPatterns() {
		return positionPatterns;
	}

	public DetectPolygonBinaryGrayRefine<T> getSquareDetector() {
		return squareDetector;
	}

	public SquareGraph getGraph() {
		return graph;
	}
}
