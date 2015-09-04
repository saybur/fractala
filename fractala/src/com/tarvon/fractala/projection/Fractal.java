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

import java.util.Objects;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.tarvon.fractala.noise.NoiseSource;
import com.tarvon.fractala.util.Point3D;
import com.tarvon.fractala.util.ProjectionPool;
import com.tarvon.fractala.util.Matrix;

/**
 * Class that creates a fractal data set.
 * <p>
 * &quot;Fractals,&quot; for the purposes of Fractala, are layered projections.
 * Provided with a source of noise and a desired persistance value, the
 * generated data set should make a reasonable fractal data set that can be
 * turned into an image.
 * <p>
 * Hugo Elias's article about fractals has the definitions that I use for
 * persistance, frequency, amplitude, and octaves. Refer to his writeup at <a
 * href="http://freespace.virgin.net/hugo.elias/models/m_perlin.htm"
 * >http://freespace.virgin.net/hugo.elias/models/m_perlin.htm</a>
 * 
 * @author saybur
 * 
 */
public class Fractal implements Projection
{
	public static final class Builder
	{
		private NoiseSource source;
		private Point3D origin;
		private ProjectionPool pool;
		private ImmutableList<ProjectionFilter> filters;
		private int power;
		private double persistance;
		private int octaves;
		
		private Builder()
		{
			power = 10;
			persistance = 0.5;
			octaves = 6;
		}

		public Fractal create()
		{
			// verify inputs
			final NoiseSource sourceLocal = Preconditions.checkNotNull(source,
					"noise source must be set");
			final Point3D originLocal = Preconditions.checkNotNull(origin,
					"origin point within noise must be set");
			final int powerLocal = power;
			Preconditions.checkArgument(powerLocal > 0,
					"power must be equal to or greater than 1");
			final double persistenceLocal = persistance;
			Preconditions.checkArgument(persistenceLocal >= 0.0
					&& persistenceLocal <= 1.0,
					"persistence must be on [0.0, 1.0]");
			final int octavesLocal = octaves;
			Preconditions.checkArgument(octaves > 0,
					"octaves must be equal to or greater than 1");
			
			// see if there is a pool, and if not, we need to make one
			// and ensure it has what we need it to have
			ProjectionPool poolLocal = pool;
			if(poolLocal == null)
			{
				poolLocal = ProjectionPool.create();
			}
			if(! poolLocal.contains(powerLocal))
			{
				poolLocal.populate(power);
			}
			
			// and get a possibly null filters reference
			final ImmutableList<ProjectionFilter> filtersLocal = filters;
			
			// construct projections
			ImmutableList.Builder<Projection> projections =
					ImmutableList.builder();
			for(int i = 0; i < octavesLocal; i++)
			{
				// calculate frequency/amplitude for each projection
				int frequency = (int) Math.pow(2, i);
				double amplitude = Math.pow(persistenceLocal, i);
				// build the projection's base values
				Layer.Builder layer = Layer.builder()
						.useNoise(sourceLocal)
						.useOrigin(originLocal)
						.usePower(powerLocal)
						.usePool(poolLocal)
						.useFrequency(frequency)
						.useAmplitude(amplitude);
				// add the filters, if we have any
				if(filtersLocal != null)
				{
					layer.useFilters(filtersLocal);
				}
				// then add
				projections.add(layer.create());
			}
			
			// then we're good to go
			return new Fractal(projections.build());
		}

		public Builder useFilters(ImmutableList<ProjectionFilter> filters)
		{
			this.filters = filters;
			return this;
		}

		public Builder useNoiseSource(NoiseSource source)
		{
			this.source = source;
			return this;
		}

		public Builder useOctaves(int octaves)
		{
			this.octaves = octaves;
			return this;
		}

		public Builder useOrigin(Point3D origin)
		{
			this.origin = origin;
			return this;
		}
		
		public Builder usePersistance(double persistance)
		{
			this.persistance = persistance;
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
	 * @return a new builder for this class.
	 */
	public static Builder builder()
	{
		return new Builder();
	}
	
	/**
	 * Creates an arbitrary fractal based on the provided projections.
	 * 
	 * @param projections the projections to build from.
	 * @return the constructed fractal object.
	 */
	public static Fractal of(ImmutableList<Projection> projections)
	{
		return new Fractal(projections);
	}
	
	private final ImmutableList<Projection> projections;
	private final int power;

	private Fractal(ImmutableList<Projection> projectionsPassed)
	{
		// shallow copy provided projections to prevent list meddling
		projections = Preconditions.checkNotNull(projectionsPassed,
				"projections cannot be null");
		// verify that the projections make sense
		Preconditions.checkArgument(projections.size() > 0,
				"List of projections must have at least 1 projection in it");
		power = projections.get(0).getPower();
		for(int i = 1; i < projections.size(); i++)
		{
			if(projections.get(i).getPower() != power)
			{
				throw new IllegalArgumentException("The power of the provided "
						+ "projections must all be identical");
			}
		}
	}

	@Override
	public double calculate(int x, int y)
	{
		double v = 0.0;
		for(Projection p : projections)
		{
			v += p.calculate(x, y);
		}
		return v;
	}

	@Override
	public Matrix call() throws Exception
	{
		Projection.Task task = new Projection.Task(this);
		return task.call();
	}

	@Override
	public boolean equals(Object obj)
	{
		if(obj instanceof Fractal)
		{
			Fractal o = (Fractal) obj;
			return Objects.equals(this.projections, o.projections)
					&& Objects.equals(this.power, o.power);
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
		return Objects.hash(projections, power);
	}

	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this)
				.add("projections", projections)
				.add("power", power)
				.toString();
	}
}
