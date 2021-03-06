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

import java.util.Arrays;

/**
 * Stores a set of bits inside of a byte array
 *
 * @author Peter Abeles
 */
public class PackedBits8 implements PackedBits {
	/**
	 * Integer array used to store bits
	 */
	public byte data[] = new byte[1];
	/**
	 * Number of bits stored
	 */
	public int size;

	public PackedBits8(int totalBits ) {
		resize(totalBits);
	}

	public PackedBits8() {
	}

	public int get( int which ) {
		int index = which/8;
		int offset = which%8;

		return (data[index] & (1 << offset)) >> offset;
	}

	public void set( int which , int value ) {
		int index = which/8;
		int offset = which%8;

		data[index] ^= (-value ^ data[index]) & (1 << offset);
	}

	public int getArray( int index ) {
		return data[index]&0xFF;
	}

	public void resize(int totalBits ) {
		this.size = totalBits;
		int N = arrayLength();
		if( data.length < N ) {
			data = new byte[ N ];
		}
	}

	public void zero() {
		Arrays.fill(data,0,arrayLength(),(byte)0);
	}

	public int length() {
		return size;
	}

	public int arrayLength() {
		if( (size%8) == 0 )
			return size/8;
		else
			return size/8 + 1;
	}

	@Override
	public int elementBits() {
		return 8;
	}
}
