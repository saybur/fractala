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

import java.util.Objects;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * Defines a coordinate in three-dimensional Cartesian space.
 * 
 * @author saybur
 *
 */
public final class Point3D
{
	/**
	 * Converts the provided spherical coordinate to a Cartesian point.
	 * <p>
	 * The result will be a point in 3D Cartesian space represented by an
	 * instance of this class.
	 * <p>
	 * For details of the method used for conversion see <a
	 * href="http://en.wikipedia.org/wiki/Spherical_coordinate_system"
	 * >http://en.wikipedia.org/wiki/Spherical_coordinate_system</a>
	 * 
	 * @param r
	 *            the distance from the origin point.
	 * @param phi
	 *            the azimuth, in radians, on the interval [0, 2&pi;)
	 * @param theta
	 *            the inclination, in radians, on the interval [0, &pi;)
	 * @return the created point.
	 */
	public static Point3D ofSpherical(double r, double phi, double theta)
	{
		double x = r * Math.cos(phi) * Math.sin(theta);
		double y = r * Math.sin(phi) * Math.sin(theta);
		double z = r * Math.cos(theta);
		return new Point3D(x, y, z);
	}

	/**
	 * Converts the provided spherical coordinate to a Cartesian point using the
	 * provided sine/cosine tables.
	 * <p>
	 * This method uses the cached tables to improve speed of translation. The
	 * provided x,y coordinate indicates the relative phi/theta values in the
	 * sin/cos tables, which should be of the form generated in
	 * {@link ProjectionPool}.
	 * <p>
	 * The result will be a point in 3D Cartesian space represented by an
	 * instance of this class.
	 * <p>
	 * For details of the method used for conversion see <a
	 * href="http://en.wikipedia.org/wiki/Spherical_coordinate_system"
	 * >http://en.wikipedia.org/wiki/Spherical_coordinate_system</a>
	 * 
	 * @param sin
	 *            the sine table.
	 * @param cos
	 *            the cosine table.
	 * @param r
	 *            the distance from the origin point.
	 * @param x
	 *            the phi coordinate location within the provided arrays.
	 * @param y
	 *            the theta coordinate location within the provided arrays.
	 * @return the created point.
	 */
	public static Point3D ofSpherical(ImmutableList<Double> sin,
			ImmutableList<Double> cos, double r, int x, int y)
	{
		double nx = r * cos.get(x) * sin.get(y);
		double ny = r * sin.get(x) * sin.get(y);
		double nz = r * cos.get(y);
		return new Point3D(nx, ny, nz);
	}
	
	/**
	 * Constructs three-dimensional point from the provided points.
	 * <p>
	 * Each value must be finite (not be either infinite or {@link Double#NaN}).
	 * 
	 * @param x the X value.
	 * @param y the Y value.
	 * @param z the Z value.
	 */
	public static Point3D of(double x, double y, double z)
	{
		return new Point3D(x, y, z);
	}
	
	private final double x, y, z;
	
	private Point3D(double x, double y, double z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		
		Preconditions.checkArgument(Double.isFinite(x),
				"x value must be finite");
		Preconditions.checkArgument(Double.isFinite(y),
				"y value must be finite");
		Preconditions.checkArgument(Double.isFinite(z),
				"z value must be finite");
	}

	@Override
	public boolean equals(Object obj)
	{
		if(obj instanceof Point3D)
		{
			Point3D o = (Point3D) obj;
			return Objects.equals(x, o.x)
					&& Objects.equals(y, o.y)
					&& Objects.equals(z, o.z);
		}
		else
		{
			return false;
		}
	}

	/**
	 * Provides the X component of the point.
	 * 
	 * @return the X component.
	 */
	public double getX()
	{
		return x;
	}
	
	/**
	 * Provides the Y component of the point.
	 * 
	 * @return the Y component.
	 */
	public double getY()
	{
		return y;
	}
	
	/**
	 * Provides the Z component of the point.
	 * 
	 * @return the Z component.
	 */
	public double getZ()
	{
		return z;
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(x, y, z);
	}

	/**
	 * Multiplies this point by the given scalar.
	 * 
	 * @param scalar
	 *            the scalar to multiply against.
	 * @return the result vector.
	 * @throws IllegalArgumentException
	 *             if the scalar value is invalid.
	 */
	public Point3D scalar(double scalar) throws IllegalArgumentException
	{
		Preconditions.checkArgument(Double.isFinite(scalar),
				"scalar value cannot be non-finite.");
		
		double nx = x * scalar;
		double ny = y * scalar;
		double nz = z * scalar;
		return new Point3D(nx, ny, nz);
	}
	
	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this)
				.add("x", x)
				.add("y", y)
				.add("z", z)
				.toString();
	}
	
	/**
	 * Adds the given point to this one and provides the result.
	 * 
	 * @param o
	 *            the other point to add to this one.
	 * @return the new point formed by the operation.
	 */
	public Point3D translate(Point3D o) throws IllegalArgumentException
	{
		Preconditions.checkNotNull(o, "other vector cannot be null");
		
		final double nx = x + o.x;
		final double ny = y * o.y;
		final double nz = z * o.z;
		return new Point3D(nx, ny, nz);
	}
}
