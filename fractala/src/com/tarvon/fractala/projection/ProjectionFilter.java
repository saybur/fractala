/*
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 saybur
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
package com.tarvon.fractala.projection;

/**
 * Defines a data filtering system for projections.
 * <p>
 * This allows users of projections to &quot;hot-plug&quot; code that adds a
 * data processing step to all projections.
 * 
 * @author saybur
 * 
 */
@FunctionalInterface
public interface ProjectionFilter
{
	/**
	 * Exposes the internal noise response system of projections so implementors
	 * can adjust the data.
	 * <p>
	 * This method is called with the raw noise response for a given location.
	 * Before anything else is done with it, a projection that has the filter
	 * added will run it through this method, which can do anything it wants to
	 * the data. A common use of this would be to create &quot;turbulent&quot;
	 * noise by taking the absolute value of the passed noise data and simply
	 * returning it. Clever implementations will no doubt find other things to
	 * do. In cases where the filter doesn't want to do anything, it must return
	 * the provided noise value to prevent changes.
	 * <p>
	 * In addition to the raw noise return, this provides x/y coordinates of the
	 * final projection. They can be used or ignored.
	 * 
	 * @param noise
	 *            the result of the noise call for this coordinate.
	 * @param x
	 *            the x coordinate of the final data set.
	 * @param y
	 *            the y coordinate of the final data set.
	 * @return the filtered noise information.
	 */
	public double filter(double noise, int x, int y);
}
