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
package com.tarvon.fractala;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import com.google.common.base.Preconditions;
import com.tarvon.fractala.noise.CellularNoise;
import com.tarvon.fractala.noise.SimplexNoise;
import com.tarvon.fractala.projection.Fractal;
import com.tarvon.fractala.util.Point3D;
import com.tarvon.fractala.util.ProjectionPool;

/**
 * Class for generating simple fractals.
 * 
 * @author saybur
 *
 */
public class Fractals
{
	/**
	 * Projection pool used by all fractals created in this object.
	 */
	private static ProjectionPool pool = ProjectionPool.create();
	
	/**
	 * Defines the default octaves for fractals made by this class.
	 * <p>
	 * For details, see {@link Fractal.Builder#useOctaves(int)}.
	 */
	public static final int DEFAULT_OCTAVES = 6;
	/**
	 * Defines the default value for persistence used by this class.
	 * <p>
	 * For details, see {@link Fractal.Builder#usePersistance(double)}.
	 */
	public static final double DEFAULT_PERSISTENCE = 0.5;
	
	/**
	 * Creates a new fractal sourced from {@link SimplexNoise}, using default
	 * values.
	 * 
	 * @param power
	 *            the power of 2 that controls the dimensions of the result
	 *            fractal.
	 * @return the fractal.
	 */
	public static Fractal createCellularFractal(int power)
	{
		pool.populate(power);
		final Point3D origin = randomOrigin();
		return Fractal.builder()
				.usePool(pool)
				.usePower(power)
				.useNoiseSource(CellularNoise.getInstance())
				.useOrigin(origin)
				.usePersistance(DEFAULT_PERSISTENCE)
				.useOctaves(DEFAULT_OCTAVES)
				.create();
	}
	
	/**
	 * Creates a new fractal sourced from {@link CellularNoise}, using default
	 * values, and a seed value to control output.
	 * 
	 * @param seed
	 *            the seed value to use for this fractal.
	 * @param power
	 *            the power of 2 that controls the dimensions of the result
	 *            fractal.
	 * @return the fractal.
	 */
	public static Fractal createCellularFractal(long seed, int power)
	{
		pool.populate(power);
		final Random random = new Random();
		final Point3D origin = randomOrigin(random);
		return Fractal.builder()
				.usePool(pool)
				.usePower(power)
				.useNoiseSource(CellularNoise.getInstance())
				.useOrigin(origin)
				.usePersistance(DEFAULT_PERSISTENCE)
				.useOctaves(DEFAULT_OCTAVES)
				.create();
	}
	
	/**
	 * Creates a new fractal sourced from {@link SimplexNoise}, using default
	 * values.
	 * 
	 * @param power
	 *            the power of 2 that controls the dimensions of the result
	 *            fractal.
	 * @return the fractal.
	 */
	public static Fractal createSimplexFractal(int power)
	{
		pool.populate(power);
		final Point3D origin = randomOrigin();
		return Fractal.builder()
				.usePool(pool)
				.usePower(power)
				.useNoiseSource(SimplexNoise.getInstance())
				.useOrigin(origin)
				.usePersistance(DEFAULT_PERSISTENCE)
				.useOctaves(DEFAULT_OCTAVES)
				.create();
	}
	
	/**
	 * Creates a new fractal sourced from {@link SimplexNoise}, using default
	 * values, and a seed value to control output.
	 * 
	 * @param seed
	 *            the seed value to use for this fractal.
	 * @param power
	 *            the power of 2 that controls the dimensions of the result
	 *            fractal.
	 * @return the fractal.
	 */
	public static Fractal createSimplexFractal(long seed, int power)
	{
		pool.populate(power);
		final Random random = new Random();
		final Point3D origin = randomOrigin(random);
		return Fractal.builder()
				.usePool(pool)
				.usePower(power)
				.useNoiseSource(SimplexNoise.getInstance())
				.useOrigin(origin)
				.usePersistance(DEFAULT_PERSISTENCE)
				.useOctaves(DEFAULT_OCTAVES)
				.create();
	}
	
	/**
	 * Provides a random origin {@link Point3D}.
	 * 
	 * @return a random origin.
	 */
	public static Point3D randomOrigin()
	{
		return Point3D.of(
				ThreadLocalRandom.current().nextDouble() * 100.0 - 50.0,
				ThreadLocalRandom.current().nextDouble() * 100.0 - 50.0,
				ThreadLocalRandom.current().nextDouble() * 100.0 - 50.0);
	}
	
	/**
	 * Provides a random origin {@link Point3D} using the provided random number
	 * generator.
	 * 
	 * @param random
	 *            the source of randomness.
	 * @return a random origin.
	 */
	public static Point3D randomOrigin(Random random)
	{
		Preconditions.checkNotNull(random, "random cannot be null");
		return Point3D.of(
				random.nextDouble() * 100.0 - 50.0,
				random.nextDouble() * 100.0 - 50.0,
				random.nextDouble() * 100.0 - 50.0);
	}
}
