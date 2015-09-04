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
package com.tarvon.fractala.util;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * Provides a cache of trigonometry tables for projections to use.
 * <p>
 * Each projection will often be made multiple times throughout the lifecycle of
 * the program. To prevent projections from having to constantly recalculate
 * sine/cosine values for converting spherical coordinate data to Cartesian
 * points suitable for the noise methods, this class caches tables of that
 * information. For more, see {@link ProjectionUtils#createCosineTable(int)} and
 * {@link ProjectionUtils#createSineTable(int)}.
 * <p>
 * The generated tables are for various powers of 2. The pool must be populated
 * for each value that needs to be retrieved later. If not populated for a given
 * value, this will fail-fast with <code>NoSuchElementException</code>.
 * <p>
 * This object is threadsafe.
 * 
 * @author saybur
 * 
 */
public class ProjectionPool
{
	/**
	 * Creates a new, empty pool.
	 * 
	 * @return the new pool object.
	 */
	public static ProjectionPool create()
	{
		return new ProjectionPool();
	}

	private final ConcurrentHashMap<Integer, ImmutableList<Double>> sin;
	private final ConcurrentHashMap<Integer, ImmutableList<Double>> cos;
	
	private ProjectionPool()
	{
		sin = new ConcurrentHashMap<>();
		cos = new ConcurrentHashMap<>();
	}
	
	/**
	 * Indicates whether or not the pool contains values for the given power of
	 * 2.
	 * <p>
	 * Once <code>true</code>, this will never be <code>false</code>.
	 * 
	 * @param power
	 *            the power of 2 to check.
	 * @return <code>true</code> if contained, <code>false</code> if not.
	 */
	public boolean contains(int power)
	{
		return sin.containsKey(power) && cos.containsKey(power);
	}
	
	/**
	 * Provides the cosine table for the given power of 2.
	 * 
	 * @param power
	 *            the power of 2 to retrieve the result collection for.
	 * @return the cosine table for the given power.
	 * @throws NoSuchElementException
	 *             if the pool is not populated for the given power.
	 */
	public ImmutableList<Double> cosTable(int power)
	{
		ImmutableList<Double> cosTable = cos.get(power);
		if(cosTable == null)
		{
			throw new NoSuchElementException(String.format(
					"Cosine table unpopulated for power %d", power));
		}
		return cosTable;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(obj instanceof ProjectionPool)
		{
			ProjectionPool o = (ProjectionPool) obj;
			return Objects.equals(sin, o.sin)
					&& Objects.equals(cos, o.cos);
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(sin, cos);
	}

	/**
	 * Populates the pool for the given dimension.
	 * <p>
	 * This will fill the pool for the given power of 2. For example, if given
	 * the value 12, the pool will have sine and cosine tables for an
	 * equirectangular projection of width 4096.
	 * 
	 * @param power
	 *            the given power of 2 to populate the pool with.
	 */
	synchronized public void populate(int power)
	{
		Preconditions.checkArgument(power > 0,
				"power argument must be equal to or greater than 1");
		Preconditions.checkArgument(power <= 15,
				"power argument must be equal to or less than 15: "
				+ "higher values are outside the range of Integer.MAX_VALUE");
		int width = (int) Math.pow(2, power);
		if(! sin.containsKey(power))
		{
			sin.put(power, ProjectionUtils.createSineTable(width));
		}
		if(! cos.containsKey(power))
		{
			cos.put(power, ProjectionUtils.createCosineTable(width));
		}
	}

	/**
	 * Provides the sine table for the given power of 2.
	 * 
	 * @param power
	 *            the power of 2 to retrieve the result collection for.
	 * @return the sine table for the given power.
	 * @throws NoSuchElementException
	 *             if the pool is not populated for the given power.
	 */
	public ImmutableList<Double> sinTable(int power)
	{
		ImmutableList<Double> sinTable = sin.get(power);
		if(sinTable == null)
		{
			throw new NoSuchElementException(String.format(
					"Sine table unpopulated for power %d", power));
		}
		return sinTable;
	}
}
