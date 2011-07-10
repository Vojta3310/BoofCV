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

package gecv.core.image.border;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestBorderIndex1D_Wrap {

	int length = 10;

	@Test
	public void simple() {
		BorderIndex1D_Wrap alg = new BorderIndex1D_Wrap();
		alg.setLength(length);

		for( int i = 0; i < 10; i++ ) {
			assertEquals(i,alg.getIndex(i));
		}

		assertEquals(9,alg.getIndex(-1));
		assertEquals(8,alg.getIndex(-2));
		assertEquals(0,alg.getIndex(length));
		assertEquals(1,alg.getIndex(length+1));
	}
}