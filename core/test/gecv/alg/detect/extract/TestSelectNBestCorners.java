/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.detect.extract;

import gecv.struct.QueueCorner;
import gecv.struct.image.ImageFloat32;
import org.junit.Test;
import pja.geometry.struct.point.Point2D_I16;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSelectNBestCorners {

	int width = 20;
	int height = 20;

	int N = 50;
	
	@Test
	public void testLessThanN() {
		ImageFloat32 intensity = new ImageFloat32(width,height);
		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				intensity.set(x,y,y*width+x);
			}
		}

		QueueCorner origCorners = new QueueCorner(N+30);
		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width && origCorners.size() < N-10; x++ ) {
				origCorners.add(x,y);
			}
		}

		// make sure the input features were returned
		SelectNBestCorners alg = new SelectNBestCorners(N);
		alg.process(intensity,origCorners);

		assertEquals(alg.bestCorners.size(),origCorners.size());
	}

	@Test
	public void testMoreThanN() {
		ImageFloat32 intensity = new ImageFloat32(width,height);
		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				intensity.set(x,y,y*width+x);
			}
		}

		QueueCorner origCorners = new QueueCorner(N+30);
		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width && origCorners.size() < N+30; x++ ) {
				origCorners.add(x,y);
			}
		}

		// make sure only N features were found
		SelectNBestCorners alg = new SelectNBestCorners(N);
		alg.process(intensity,origCorners);

		assertEquals(alg.bestCorners.size(),N);

		// make sure only the best features were selectec
		float diff = origCorners.size()-N;
		for( int i = 0; i < alg.bestCorners.size(); i++ ) {
			Point2D_I16 pt = alg.bestCorners.get(i);

			float val = intensity.get(pt.x,pt.y);

			assertTrue( val >= diff);
		}
	}
}
