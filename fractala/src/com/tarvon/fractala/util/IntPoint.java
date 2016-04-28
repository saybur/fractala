package com.tarvon.fractala.util;

/**
 * Defines a two-dimensional point in <code>int</code> precision.
 * 
 * @author saybur
 *
 */
public final class IntPoint
{
	private final int x, y;
	
	/**
	 * Creates a new <code>IntPoint</code> of the given values.
	 * 
	 * @param x
	 *            the X coordinate.
	 * @param y
	 *            the Y coordinate.
	 */
	public IntPoint(int x, int y)
	{
		this.x = x;
		this.y = y;
	}

	@Override
	public boolean equals(Object obj)
	{
		if(this == obj)
			return true;
		if(this instanceof IntPoint)
		{
			IntPoint o = (IntPoint) obj;
			return x == o.x && y == o.y;
		}
		else
			return false;
	}

	/**
	 * @return the X value.
	 */
	public int getX()
	{
		return x;
	}

	/**
	 * @return the Y value.
	 */
	public int getY()
	{
		return y;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + x;
		result = prime * result + y;
		return result;
	}

	@Override
	public String toString()
	{
		return String.format("IntPoint:{%d, %d}", x, y);
	}
}
