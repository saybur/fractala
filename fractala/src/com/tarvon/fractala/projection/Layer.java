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

import java.util.Iterator;
import java.util.Objects;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.tarvon.fractala.noise.NoiseSource;
import com.tarvon.fractala.util.Point3D;
import com.tarvon.fractala.util.ProjectionPool;
import com.tarvon.fractala.util.Matrix;

import static com.google.common.base.Preconditions.*;

/**
 * Defines the default implementation for handling transforms between noise
 * sources and projections.
 * <p>
 * For this default implementation, the amplitude controls how much significance
 * is given to the produced noise. This can be set to 1.0 for projections being
 * built that are not part of a fractal. Fractals will control this as part of
 * the iterative process of generating a fractal image.
 * <p>
 * Instances of this class are immutable and thread-safe. Unless specified
 * otherwise, no methods will return <code>null</code>. All methods that accept
 * parameters will throw <code>NullPointerException</code> if a parameter is
 * <code>null</code>, and <code>IllegalArgumentException</code> if a parameter
 * is invalid.
 *
 * @author saybur
 */
public final class Layer implements Projection
{
	/**
	 * Builder for the {@link Layer} class.
	 * <p>
	 * This does not implement <code>equals(Object)</code> or
	 * <code>hashCode(Object)</code>. Instances of this object should not be
	 * shared between different threads, or away from the thread that calls
	 * {@link #create()}.
	 * <p>
	 * This allows any value in the <code>useX()</code> methods (including
	 * <code>null</code>) and checks for correctness in {@link #create()}.
	 *
	 * @author saybur
	 */
	public static final class Builder
	{
		private NoiseSource noise;
		private int power;
		private Point3D origin;
		private int frequency;
		private double amplitude;
		private ImmutableList<ProjectionFilter> filters;
		private ProjectionPool pool;
		
		private Builder()
		{
			power = 10;
			frequency = 1;
			amplitude = 1;
		}
		
		private Builder(Layer o)
		{
			checkNotNull(o, "cannot create a Builder from a null object.");

			noise = o.noise;
			power = o.power;
			origin = o.origin;
			frequency = o.frequency;
			amplitude = o.amplitude;
			filters = o.filters;
			pool = o.pool;
		}
		
		/**
		 * Constructs an instance of the object represented in the builder using
		 * the provided values.
		 * 
		 * @return the constructed object using the builder values.
		 * @throws NullPointerException
		 *             if a required value was <code>null</code>.
		 * @throws IllegalArgumentException
		 *             if a provided value was invalid.
		 */
		public Layer create()
		{
			return new Layer(this);
		}
		
		public Builder useAmplitude(double amplitude)
		{
			this.amplitude = amplitude;
			return this;
		}
		
		public Builder useFilters(ImmutableList<ProjectionFilter> filters)
		{
			this.filters = filters;
			return this;
		}
		
		public Builder useFrequency(int frequency)
		{
			this.frequency = frequency;
			return this;
		}
		
		public Builder useNoise(NoiseSource noise)
		{
			this.noise = noise;
			return this;
		}
		
		public Builder useOrigin(Point3D origin)
		{
			this.origin = origin;
			return this;
		}
		
		public Builder usePool(ProjectionPool pool)
		{
			this.pool = pool;
			return this;
		}
		
		public Builder usePower(int power)
		{
			this.power = power;
			return this;
		}
	}

	/**
	 * Creates and returns a new <code>Builder</code> for this class,
	 * initialized with default values.
	 * 
	 * @return the new builder for this class.
	 */
	public static Builder builder()
	{
		return new Builder();
	}
	
	/**
	 * Creates and returns a new <code>Builder</code> for this class,
	 * initialized with the values in the given object.
	 * 
	 * @param other
	 *            the other object to initialize values from.
	 * @return the new builder for this class.
	 */
	public static Builder builderOf(Layer other)
	{
		return new Builder(other);
	}
	
	private final NoiseSource noise;
	private final int power;
	private final Point3D origin;
	private final int frequency;
	private final double amplitude;
	private final ImmutableList<ProjectionFilter> filters;
	private final ProjectionPool pool;
	
	private Layer(Builder b)
	{
		super();
		
		noise = checkNotNull(b.noise, "noise");
		power = b.power;
		origin = checkNotNull(b.origin, "origin");
		frequency = b.frequency;
		amplitude = b.amplitude;
		if(b.filters == null)
		{
			filters = ImmutableList.of();
		}
		else
		{
			filters = checkNotNull(b.filters,
					"filters");
		}
		pool = checkNotNull(b.pool, "pool");
		
		checkArgument(power >= 2,
				"power must be equal to or greater than 2");
		checkArgument(pool.contains(power),
				"pool must contain information for power %s", power);
	}
	
	/**
	 * Calculates the projection's value at a given pixel location.
	 * <p>
	 * This will determine the lon/lat equivalent of the location (which is the
	 * azimuth/inclination), convert that to a Cartesian coordinate, get the
	 * noise value from the noise source, and then filter it.
	 * <p>
	 * This method has no explicit bounds checking. Generally speaking,
	 * requesting points outside the size of the projection will generate an
	 * <code>ArrayOutOfBoundsException</code>, but that is not guaranteed.
	 * 
	 * @param x
	 *            the x coordinate that noise should be collected for.
	 * @param y
	 *            the y coordinate that noise should be collected for.
	 * @return filtered noise for the coordinate.
	 */
	public double calculate(int x, int y)
	{
		// 1. determine 3D coordinate within set
		Point3D c = Point3D.ofSpherical(pool.sinTable(power),
				pool.cosTable(power), frequency, x, y);
		// 2. get noise for location
		double n = noise.coherentNoise(c.getX() + origin.getX(), c.getY()
				+ origin.getY(), c.getZ() + origin.getZ())
				* amplitude;
		// 3. filter
		Iterator<ProjectionFilter> itr = filters.iterator();
		while(itr.hasNext())
		{
			n = itr.next().filter(n, x, y);
		}
		// 4. provide result
		return n;
	}
	
	@Override
	public Matrix call()
	{
		Projection.Task task = new Projection.Task(this);
		return task.call();
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(obj instanceof Layer)
		{
			Layer o = (Layer) obj;
			return Objects.equals(this.noise, o.noise)
					&& Objects.equals(this.power, o.power)
					&& Objects.equals(this.origin, o.origin)
					&& Objects.equals(this.frequency, o.frequency)
					&& Objects.equals(this.amplitude, o.amplitude)
					&& Objects.equals(this.filters, o.filters)
					&& Objects.equals(this.pool, o.pool);
		}
		else
		{
			return false;
		}
	}

	@Override
	public int getPower()
	{
		return power;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(noise, power, origin, frequency, amplitude, filters,
				pool);
	}

	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this)
				.add("noise", noise)
				.add("power", power)
				.add("origin", origin)
				.add("frequency", frequency)
				.add("amplitude", amplitude)
				.add("filters", filters)
				.add("pool", pool)
				.toString();
	}
}
