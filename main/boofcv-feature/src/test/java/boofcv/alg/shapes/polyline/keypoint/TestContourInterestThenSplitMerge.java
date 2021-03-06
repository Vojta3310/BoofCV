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

package boofcv.alg.shapes.polyline.keypoint;

import boofcv.struct.ConfigLength;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.Test;

import java.util.List;

import static boofcv.alg.shapes.polyline.splitmerge.TestSplitMergeLineFitLoop.matchSplitsToExpected;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestContourInterestThenSplitMerge {
	@Test
	public void simpleLoop() {
		List<Point2D_I32> contour = TestContourInterestPointDetector.rect(10,12,20,22);

		ContourInterestThenSplitMerge alg = new ContourInterestThenSplitMerge(
				true, ConfigLength.fixed(5),1,0.1,10);

		GrowQueue_I32 found = new GrowQueue_I32();

		assertTrue(alg.process(contour,found));

		matchSplitsToExpected(new int[]{0, 10, 20, 30}, found);
	}

	@Test
	public void simpleSegment() {
		fail("implement");
	}

}