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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.DoubleToIntFunction;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

/**
 * Class that helps pick out colors for a {@link Matrix} object.
 * <p>
 * Instances of this class are immutable and thread-safe. Unless specified
 * otherwise, no methods will return <code>null</code>. All methods that accept
 * parameters will throw <code>NullPointerException</code> if a parameter is
 * <code>null</code>, and <code>IllegalArgumentException</code> if a parameter
 * is invalid.
 * 
 * @author saybur
 *
 */
public final class ColorChooser implements DoubleToIntFunction
{
	/**
	 * Builder class for creating instances of the color chooser.
	 * <p>
	 * At least one color stop must be specified prior to building.
	 * 
	 * @author saybur
	 *
	 */
	public static final class Builder
	{
		private List<ColorStop> colorStops;
		
		private Builder()
		{
			colorStops = new ArrayList<ColorStop>();
		}
		
		/**
		 * Adds a new color stop to the chooser.
		 * <p>
		 * Each color stop represents a position along the number space for a
		 * set of data. Data exactly at the stop will be exactly the same as the
		 * color. Data along the continuum between two color stops will be
		 * interpolated between the colors.
		 * 
		 * @param value
		 *            the number value corresponding to the color.
		 * @param color
		 *            the color value at this stop.
		 * @return the builder, for chaining.
		 */
		public Builder add(double value, Color color)
		{
			colorStops.add(new ColorStop(value, color));
			return this;
		}
		
		/**
		 * Adds a set of color stops to the chooser.
		 * <p>
		 * This is the same as {@link #add(double, Color)}, but for multiple
		 * entries in a <code>Map</code>. No members of the given
		 * <code>Map</code> are allowed to be <code>null</code>.
		 * 
		 * @param map
		 *            a mapping of number values to colors to insert into this
		 *            chooser.
		 * @return the builder, for chaining.
		 */
		public Builder addAll(Map<Double, Color> map)
		{
			Preconditions.checkNotNull(map, "map cannot be null");
			List<ColorStop> newStops = new ArrayList<ColorStop>();
			for(Map.Entry<Double, Color> e : map.entrySet())
			{
				Double key = Preconditions.checkNotNull(e.getKey(),
						"key in map cannot be null");
				Color color = Preconditions.checkNotNull(e.getValue(),
						"value in map cannot be null");
				newStops.add(new ColorStop(key, color));
			}
			colorStops.addAll(newStops);
			return this;
		}
		
		/**
		 * @return constructs and returns the new color chooser object.
		 */
		public ColorChooser create()
		{
			return new ColorChooser(this);
		}
	}
	
	/**
	 * Internal class for storing color stop information.
	 * <p>
	 * This does not implement <code>equals(Object)</code> or
	 * <code>hashCode(Object)</code>. Instances of this object should not be
	 * shared between different threads, or away from the thread that calls
	 * {@link #create()}.
	 * 
	 * @author saybur
	 *
	 */
	private static final class ColorStop
	{
		private final double value;
		private final Color color;
		
		private ColorStop(double value, Color color)
		{
			this.color = Preconditions.checkNotNull(color,
					"color must not be null");
			this.value = value;
			Preconditions.checkArgument(Double.isFinite(value),
					"value must be finite");
		}
	}
	
	/**
	 * Bitmask for the alpha in ARGB color data.
	 */
	private static final int ABM = 0xff000000;
	/**
	 * Bitmask for red in ARGB color data.
	 */
	private static final int RBM = 0x00ff0000;
	/**
	 * Bitmask for green in ARGB color data.
	 */
	private static final int GBM = 0x0000ff00;
	/**
	 * Bitmask for blue in ARGB color data.
	 */
	private static final int BBM = 0x000000ff;
	
	/**
	 * Linear interpolation that blends two <code>Color</code> objects together.
	 * <p>
	 * Unless there is a good reason to work with <code>Color</code> objects,
	 * use {@link #blend(int, int, double)} instead: it involves less object
	 * manipulation, and should thus be faster.
	 * 
	 * @param c1
	 *            the first color to blend.
	 * @param c2
	 *            the second color to blend.
	 * @param w1
	 *            weight to assign to the first color, as described.
	 * @return the blended color.
	 */
	public static Color blend(Color c1, Color c2, double w1)
	{
		// get RGB values for colors as integers
		int c1h = c1.getRGB();
		int c2h = c2.getRGB();
		// reverse weighting
		double w2 = 1.0 - w1;
		// each of these pulls the byte value for the color, weights it,
		// and fast rounds to the nearest integer.
		int a = (int)((((c1h & ABM) >>> 24) * w1
				+ ((c2h & ABM) >>> 24) * w2)
				+ 0.5);
		int r = (int)((((c1h & RBM) >>> 16) * w1
				+ ((c2h & RBM) >>> 16) * w2)
				+ 0.5);
		int g = (int)((((c1h & GBM) >>> 8) * w1
				+ ((c2h & GBM) >>> 8) * w2)
				+ 0.5);
		int b = (int)((((c1h & BBM)) * w1
				+ ((c2h & BBM)) * w2)
				+ 0.5);
		return new Color(r, g, b, a);
	}
	
	/**
	 * Linear interpolation that blends two <code>int</code> colors together.
	 * <p>
	 * The values provided should be the same as those provided by
	 * {@link Color#getRGB()}, where bits 24-31 are alpha, 16-23 are red, 8-15
	 * are green, and 0-7 are blue.
	 * <p>
	 * For the version that operates on {@link Color} objects use
	 * {@link #blend(Color, Color, double)}.
	 * 
	 * @param c1
	 *            the RGB value of the first color to blend.
	 * @param c2
	 *            the RGB value of the second color to blend.
	 * @param w1
	 *            weight to assign to the first color, between 0.0 and 1.0.
	 * @return the blended color.
	 */
	public static int blend(int c1, int c2, double w1)
	{
		// inverse weighting
		double w2 = 1.0 - w1;
		// each of these pulls the byte value for the color, weights it,
		// and fast rounds to the nearest integer.
		int a = (int)((((c1 & ABM) >>> 24) * w1
				+ ((c2 & ABM) >>> 24) * w2)
				+ 0.5);
		int r = (int)((((c1 & RBM) >>> 16) * w1
				+ ((c2 & RBM) >>> 16) * w2)
				+ 0.5);
		int g = (int)((((c1 & GBM) >>> 8) * w1
				+ ((c2 & GBM) >>> 8) * w2)
				+ 0.5);
		int b = (int)((((c1 & BBM)) * w1
				+ ((c2 & BBM)) * w2)
				+ 0.5);
		return (a << 24) + (r << 16) + (g << 8) + b;
	}
	
	/**
	 * @return a new builder for this class.
	 */
	public static Builder builder()
	{
		return new Builder();
	}
	
	/**
	 * Parses a <code>Color</code> from a <code>String</code>.
	 * <p>
	 * This is very similar to {@link Color#decode(String)}, but supports
	 * optional alpha channel information. The input for this method should be a
	 * hexadecimal <code>String</code> of format AARRGGBB. Any of
	 * <code>0x</code>, <code>0X</code>, or <code>#</code> may precede the
	 * number, but they are not required.
	 * 
	 * @param colorStr
	 *            the input to read.
	 * @return the read <code>Color</code>.
	 * @throws NumberFormatException
	 *             when the input <code>String</code> cannot be rendered as a
	 *             number.
	 */
	public static Color parseColor(String colorStr)
	{
		Preconditions.checkNotNull(colorStr,
				"input color String cannot be null");
		
		// strip prefixes
		colorStr = colorStr.replaceAll("(0x)|(0X)|\\#", "");

		// ban the - sign
		if(colorStr.contains("-"))
		{
			throw new IllegalArgumentException("Cannot parse negative "
					+ "hex data for a color (or any string containing "
					+ "the - symbol).");
		}

		// if the length is 6 or less, we know that this will not
		// contain alpha information
		boolean hasAlpha = colorStr.length() > 6;

		// attempt to parse
		int value = Integer.parseInt(colorStr, 16);

		// get the color data out
		int a = (int) ((value & 0xFF000000) >> 24);
		int r = (int) ((value & 0xFF0000) >> 16);
		int g = (int) ((value & 0xFF00) >> 8);
		int b = (int) ((value & 0xFF));
		
		// then return, switching based on whether or not alpha
		// data is present
		if(hasAlpha)
		{
			return new Color(r, g, b, a);
		}
		else
		{
			return new Color(r, g, b);
		}
	}
	
	private final int count;
	private final double[] keys;
	private final int[] colors;

	private ColorChooser(Builder b)
	{
		ArrayList<ColorStop> stops = new ArrayList<ColorStop>(b.colorStops);
		Preconditions.checkArgument(stops.size() > 0,
				"must provide at least one color stop to be valid");
		Collections.sort(stops, (l, r) -> Double.compare(l.value, r.value));
		count = stops.size();
		keys = new double[count];
		colors = new int[count];
		for(int i = 0; i < count; i++)
		{
			keys[i] = stops.get(i).value;
			colors[i] = stops.get(i).color.getRGB();
		}
	}

	@Override
	public int applyAsInt(double value)
	{
		// get the appropriate index by binary search for log(n) performance
		final int i = Arrays.binarySearch(keys, value);
		
		if(i >= 0)
		{
			// dead-on match, just return
			return colors[i];
		}
		else
		{
			// non direct match, adjust index
			final int ai = -1 * (i + 1);
			
			if(ai == 0)
			{
				// below all colors
				return colors[0];
			}
			if(ai >= count)
			{
				// above all colors
				return colors[count - 1];
			}
			else
			{
				// in between, blend between neighbors
				double pKey = keys[ai - 1];
				double nKey = keys[ai];
				int pColor = colors[ai - 1];
				int nColor = colors[ai];
				double ratio = (value - pKey) / (nKey - pKey);
				return blend(nColor, pColor, ratio);
			}
		}
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(obj instanceof ColorChooser)
		{
			ColorChooser o = (ColorChooser) obj;
			return count == o.count
					&& Objects.equals(keys, o.keys)
					&& Objects.equals(colors, o.colors);
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(count, keys, colors);
	}
	
	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this)
				.add("count", count)
				.add("keys", Arrays.toString(keys))
				.add("colors", Arrays.toString(colors))
				.toString();
	}
}
