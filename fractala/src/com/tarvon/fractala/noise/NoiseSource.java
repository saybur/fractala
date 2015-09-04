/*
 * This code is public domain.
 */
package com.tarvon.fractala.noise;

/**
 * Interface for three-dimensional sources of coherent noise.
 * 
 * @author saybur
 * 
 */
public interface NoiseSource
{
	/**
	 * Provides coherent noise data for a given coordinate in three-dimensional
	 * space.
	 * <p>
	 * Noise classes lack state, so this is usually just a thin layer to the
	 * static version of this method.
	 * 
	 * @param x
	 *            the x coordinate of the point to get noise for.
	 * @param y
	 *            the y coordinate of the point to get noise for.
	 * @param z
	 *            the z coordinate of the point to get noise for.
	 * @return the noise at the provided coordinate.
	 */
	public double coherentNoise(double x, double y, double z);
}
