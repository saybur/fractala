/*
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015-2016 saybur
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.tarvon.fractala.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * Utility class for projection convenience methods.
 * <p>
 * Projections handle producing 2D maps of 3D spherical data from the noise
 * classes. This library of static helpers implements several commonly-used
 * algorithms common to most projections.
 * 
 * @author saybur
 * 
 */
public class ProjectionUtils
{
	/**
	 * Stored constant for 2 * PI
	 */
	private static final double PI_2 = 2 * Math.PI;

	/**
	 * Calculates the angle per pixel of a returned data set.
	 * <p>
	 * The width of an image is an arbitrary number, but by the definition of
	 * the projections they will create 2D data of a 3D sphere as an
	 * equirectangular projection. This method calculates the angle per pixel
	 * that is needed elsewhere to determine where image data should be drawn.
	 * <p>
	 * This value can be used for both height and width, since height is defined
	 * as 1/2 width.
	 * 
	 * @param width
	 *            the width of the image that will be created.
	 * @return the angle measurement described (in radians).
	 */
	public static double calculateAnglePerPixel(int width)
	{
		Preconditions.checkArgument(width > 0,
				"Width must be greater than zero");
		return PI_2 / width;
	}

	/**
	 * Creates a cosine lookup table for use in equirectangular projections.
	 * <p>
	 * The returned array will be the same length as the provided width, and
	 * will have the cosine value of each pixel at each location within the
	 * array.
	 * 
	 * @param width
	 *            the width of the image that will be created.
	 * @return the table of cosine values for each width.
	 * @see #createSineTable(int)
	 */
	public static ImmutableList<Double> createCosineTable(int width)
	{
		double factor = calculateAnglePerPixel(width);
		ImmutableList.Builder<Double> cos = ImmutableList.builder();
		for(int i = 0; i < width; i++)
		{
			cos.add(Math.cos(i * factor));
		}
		return cos.build();
	}

	/**
	 * Creates a sine lookup table for use in equirectangular projections.
	 * <p>
	 * The returned array will be the same length as the provided width, and
	 * will have the sine value of each pixel at each location within the array.
	 * 
	 * @param width
	 *            the width of the image that will be created.
	 * @return the table of sine values for each width.
	 * @see #createCosineTable(int)
	 */
	public static ImmutableList<Double> createSineTable(int width)
	{
		double factor = calculateAnglePerPixel(width);
		ImmutableList.Builder<Double> sin = ImmutableList.builder();
		for(int i = 0; i < width; i++)
		{
			sin.add(Math.sin(i * factor));
		}
		return sin.build();
	}
}
