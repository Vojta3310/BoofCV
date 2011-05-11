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

package gecv.abst.filter.derivative;

import gecv.struct.image.ImageBase;


/**
 * A generic interface for computing image's second derivatives.
 *
 * @author Peter Abeles
 */
public interface HessianXY<Input extends ImageBase, Output extends ImageBase> {

	/**
	 * Computes all the second derivative terms in the image.
	 *
	 * @param inputImage Original image.
	 * @param derivXX Second derivative x-axis x-axis
	 * @param derivYY Second derivative x-axis y-axis
	 * @param derivXY Second derivative x-axis y-axis
	 */
	public void process( Input inputImage , Output derivXX, Output derivYY, Output derivXY  );

	/**
	 * How many pixels wide is the region that is not processed along the outside
	 * border of the image.
	 *
	 * @return number of pixels.
	 */
	public int getBorder();
}
