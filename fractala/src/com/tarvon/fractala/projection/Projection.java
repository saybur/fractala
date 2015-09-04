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

import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

import com.google.common.base.Preconditions;
import com.tarvon.fractala.util.Matrix;

/**
 * Defines an interface for everything that creates fractal projections.
 * <p>
 * A projection is a set of data that contains a particular type of coherent
 * noise data. They wrap on the x axis, and are mapped such that they fit
 * perfectly around a spherical object. As a result, their height is half their
 * width. A projection requires a power parameter, which dictates its size. If
 * the power is given as <i>p</i>, then the width will be 2 ^ <i>p</i> and the
 * height will be 2 ^ (<i>p</i> - 1), in pixels.
 * <p>
 * All projections operate on three-dimensional noise data that expects values
 * in Cartesian space. This class creates its equirectangular projection of the
 * noise values by using a <i>plate carr&eacute;e</i> projection (<a
 * href="http://en.wikipedia.org/wiki/Equirectangular_projection"
 * >http://en.wikipedia.org/wiki/Equirectangular_projection</a>) and a spherical
 * coordinate system (<a
 * href="http://en.wikipedia.org/wiki/Spherical_coordinate_system"
 * >http://en.wikipedia.org/wiki/Spherical_coordinate_system</a>). To generate
 * the noise data, projections will get a random center point in 3D space, then
 * repeatedly calculate the noise at a given azimuth and inclination that
 * correspond to pixel locations within the image. The provided frequency value
 * controls the radius of the sphere, which increases the relative speed at
 * which the noise changes between pixels. Stated another way, this basically
 * &quot;zooms out&quot; the noise set.
 * <p>
 * Instances of this class are assumed to be immutable and thread-safe. Unless
 * specified otherwise, no methods should return <code>null</code>. All methods
 * that accept parameters should throw <code>NullPointerException</code> if a
 * parameter is <code>null</code>, and <code>IllegalArgumentException</code> if
 * a parameter is invalid.
 *
 * @author saybur
 */
public interface Projection extends Callable<Matrix>
{
	/**
	 * Task wrapper for creating the data required from
	 * {@link Projection#call()}.
	 * <p>
	 * This uses the fork/join system introduced in Java 7 to simplify
	 * implementation of the above method. Simply provide the projection to the
	 * constructor of this class, and execute the task. This class'
	 * {@link #call()} method will block until the data is available.
	 * 
	 * @author saybur
	 *
	 */
	public static final class Task implements Callable<Matrix>
	{
		/**
		 * <code>ForkJoinTask</code> for calculating the values of the projection.
		 * 
		 * @author saybur
		 *
		 */
		private final class TaskAction extends RecursiveAction
		{
			private static final long serialVersionUID = -3229497413493883470L;
			private final Matrix.Builder data;
			private final int low;
			
			private final int high;
			
			private TaskAction(Matrix.Builder data, int low, int high)
			{
				this.data = data;
				this.low = low;
				this.high = high;
			}

			@Override
			protected void compute()
			{
				if (high - low < FORK_THRESHOLD)
				{
					computeDirectly();
				}
				else
				{
					int mid = (low + high) >>> 1;
					invokeAll(new TaskAction(data, low, mid),
							new TaskAction(data, mid, high));
				}
				
			}
			
			private void computeDirectly()
			{
				for(int x = low; x < high; x++)
				{
					for(int y = 0; y < data.getHeight(); y++)
					{
						data.set(x, y, projection.calculate(x, y));
					}
				}
			}
		}
		
		private static final int FORK_THRESHOLD = 4;
		
		private final Projection projection;
		
		/**
		 * Constructs a new projection generation task.
		 * 
		 * @param projection
		 *            the projection to base this task on.
		 */
		public Task(Projection projection)
		{
			this.projection = Preconditions.checkNotNull(projection,
					"projection cannot be null");
		}

		@Override
		public Matrix call() throws Exception
		{
			final int power = projection.getPower();
			Preconditions.checkArgument(power > 0, "projection's power value "
					+ "must be equal to or greater than 1");

			// create the result data object
			final int width = (int) Math.pow(2, power);
			final int height = (int) Math.pow(2, power - 1);
			final Matrix.Builder data = Matrix.of(width, height);

			// fill it with data
			TaskAction action = new TaskAction(data, 0, width);
			ForkJoinPool pool = ForkJoinPool.commonPool();
			pool.invoke(action);
			return data.create();
		}
	}
	
	/**
	 * Calculates the projection's value at a given pixel location.
	 * <p>
	 * This should determine the lon/lat equivalent of the location (which is
	 * the azimuth/inclination), convert that to a Cartesian coordinate, get the
	 * noise value from the noise source, and then filter it.
	 * 
	 * @param x
	 *            the x coordinate that noise should be collected for.
	 * @param y
	 *            the y coordinate that noise should be collected for.
	 * @return filtered noise for the coordinate.
	 * @throws IllegalArgumentException
	 *             if the provided points are outside the bounds of the
	 *             projection.
	 */
	public double calculate(int x, int y);

	/**
	 * Provides the power of 2 which dictates the projection's size.
	 * <p>
	 * If the power is given as <i>p</i>, then the width will be 2 ^ <i>p</i>
	 * and the height will be 2 ^ (<i>p</i> - 1), in pixels.
	 * <p>
	 * This value must be equal to or greater than 1.
	 * 
	 * @return the power of 2 for the size of this projection.
	 */
	public int getPower();
}
