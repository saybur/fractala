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

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.Spliterator;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
import java.util.stream.DoubleStream;
import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.*;

/**
 * Defines a grid of <code>double</code> data backed by a one-dimensional array.
 * <p>
 * This is guaranteed to match the size provided by {@link #getWidth()} and
 * {@link #getHeight()}, and is <i>dense</i>, with all values populated.
 * <p>
 * The internal backing array is limited by Java to a maximum size, declared in
 * {@link #MAX_SIZE}. This is enforced by the construction methods.
 * <p>
 * Instances of this class are <b>mutable</b> and <b>are not thread-safe</b>.
 * Unless specified otherwise, no methods will return <code>null</code>. All
 * methods that accept parameters will throw <code>NullPointerException</code>
 * if a parameter is <code>null</code>.
 * 
 * @author saybur
 *
 */
public class Matrix
{
	/**
	 * Defines the maximum number of values that a {@link Matrix} can store.
	 * <p>
	 * Per https://stackoverflow.com/questions/3038392
	 */
	public static final int MAX_SIZE = Integer.MAX_VALUE - 5;
	
	/**
	 * Applies the given function to the provided matrices, providing the result
	 * as a new <code>Matrix</code>. The source <code>Matrix</code> objects will
	 * not be modified.
	 * <p>
	 * The function provides flexibility about the type of merging operation
	 * desired. The stream it is provided with will always be non-empty.
	 * 
	 * @param function
	 *            the function, applied to each matrix.
	 * @param first
	 *            the first <code>Matrix</code>.
	 * @param others
	 *            the other objects.
	 * @return the result.
	 */
	public static Matrix apply(final ToDoubleFunction<DoubleStream> function,
			final Matrix first, final Matrix... others)
	{
		checkNotNull(function, "function");
		checkNotNull(first, "first");
		checkNotNull(others, "others");
		
		/*
		 * Make a new array of the matricies, including the first one, verifying
		 * that array members are non-null
		 */
		final Matrix[] matrices = new Matrix[others.length + 1];
		matrices[0] = first;
		System.arraycopy(others, 0, matrices, 1, others.length);
		for(Matrix m : matrices)
		{
			checkNotNull(m, "passed matrices cannot contain a null member");
		}
		
		// get and validate the width/height values
		final int width = matrices[0].width;
		final int height = matrices[0].height;
		for(int i = 1; i < matrices.length; i++)
		{
			checkArgument(width == matrices[i].width,
					"widths not equal: one matrix was %s, another was %s",
					width, matrices[i].width);
			checkArgument(height == matrices[i].height,
					"heights not equal: one matrix was %s, another was %s",
					height, matrices[i].height);
		}
		
		// perform the calculation on the new data
		final double[] buffer = new double[matrices.length];
		final double[] result = new double[width * height];
		for(int i = 0; i < result.length; i++)
		{
			for(int n = 0; n < buffer.length; n++)
			{
				buffer[n] = matrices[n].data[i];
			}
			result[i] = function.applyAsDouble(DoubleStream.of(buffer));
		}
		
		// then provide the result back
		return new Matrix(width, height, result);
	}
	
	/**
	 * Averages the values in each cell of the given matrices.
	 * 
	 * @param first
	 *            the first matrix.
	 * @param others
	 *            any additional matrices to average.
	 * @return the result.
	 */
	public static Matrix average(Matrix first, Matrix... others)
	{
		return apply(v -> v.average().getAsDouble(), first, others);
	}
	
	/**
	 * Creates a matrix of the given size. All values will be initialized to
	 * 0.0.
	 * 
	 * @param width
	 *            the width of the table.
	 * @param height
	 *            the height of the table.
	 * @return the new <code>Matrix</code>.
	 */
	public static Matrix of(int width, int height)
	{
		final long size = width * height;
		checkArgument(size < MAX_SIZE,
				"specified size %s is larger than maximum allowed [%s]",
				size, MAX_SIZE);
		
		return new Matrix(width, height, new double[(int) size]);
	}
	
	/**
	 * Creates a matrix of the given size, using the given backing array.
	 * 
	 * @param width
	 *            the width of the table.
	 * @param height
	 *            the height of the table.
	 * @param data
	 *            the data.
	 * @return the new <code>Matrix</code>.
	 */
	public static Matrix of(int width, int height, double[] data)
	{
		return new Matrix(width, height, data);
	}
	
	/**
	 * Sums the values in each cell of the given matrices.
	 * 
	 * @param first
	 *            the first matrix.
	 * @param others
	 *            any additional matrices to average.
	 * @return the result.
	 */
	public static Matrix sum(Matrix first, Matrix... others)
	{
		return apply(v -> v.sum(), first, others);
	}
	
	private final int width;
	private final int height;
	private final int size;
	private final double[] data;
	
	private Matrix(int widthPassed, int heightPassed, double[] dataPassed)
	{
		width = widthPassed;
		height = heightPassed;
		data = checkNotNull(dataPassed, "data");
		
		// verify width/height sanity
		checkArgument(width > 0,
				"width must be greater than zero");
		checkArgument(height > 0,
				"height must be greater than zero");
		
		size = width * height;
		checkArgument(data.length == size,
				"data array given not of appropriate size, was %s, "
						+ "but width/height indicated size should be %s",
				data.length, size);
	}

	/**
	 * Applies the given operator to each value of the <code>Matrix</code>, in
	 * order.
	 * 
	 * @param operator
	 *            the operator to apply.
	 * @return this object, for chaining calls.
	 */
	public Matrix apply(DoubleUnaryOperator operator)
	{
		checkNotNull(operator, "operator");
		
		for(int i = 0; i < data.length; i++)
			data[i] = operator.applyAsDouble(data[i]);
		return this;
	}
	
	/**
	 * Applies the given operator to each value of the <code>Matrix</code>, in
	 * order.
	 * <p>
	 * The function is given a <code>double</code> value that is the current
	 * value at the given position, and a <code>int</code> array index position.
	 * The index-less version of this method is
	 * {@link #apply(DoubleUnaryOperator)}, which avoids boxing values. The
	 * return value of the function is saved to each index of the backing array.
	 * 
	 * @param function
	 *            the function to apply, as described.
	 * @return this object, for chaining calls.
	 */
	public Matrix apply(ToDoubleBiFunction<Double, Integer> function)
	{
		checkNotNull(function, "function");
		
		for(int i = 0; i < data.length; i++)
			data[i] = function.applyAsDouble(data[i], i);
		return this;
	}
	
	/**
	 * Provides the value at the given coordinate.
	 * <p>
	 * For details, see {@link #indexOf(int, int)}.
	 * 
	 * @param x
	 *            the X coordinate.
	 * @param y
	 *            the Y coordinate.
	 * @return the value.
	 * @throws ArrayIndexOutOfBoundsException
	 *             if the coordinate is out of bounds.
	 */
	public double get(int x, int y)
	{
		return data[indexOf(x, y)];
	}
	
	/**
	 * @return the height of this <code>Matrix</code>.
	 */
	public int getHeight()
	{
		return height;
	}
	
	/**
	 * @return the width of this <code>Matrix</code>.
	 */
	public int getWidth()
	{
		return width;
	}
	
	/**
	 * Provides the array index of a particular two-dimensional grid coordinate.
	 * <p>
	 * Internally, this class uses
	 * <a href="https://en.wikipedia.org/wiki/Row-major_order">row-major
	 * order</a> to store the values.
	 * 
	 * @param x
	 *            the X coordinate.
	 * @param y
	 *            the Y coordinate.
	 * @return the index position in the array, which may be out of bounds if
	 *         the input values are out of bounds.
	 */
	public int indexOf(int x, int y)
	{
		return y * width + x;
	}
	
	/**
	 * Provides the maximum value of this table.
	 * <p>
	 * For more details, see {@link DoubleStream#max()}.
	 * 
	 * @return the maximum value of this table.
	 */
	public double max()
	{
		return stream().max().getAsDouble();
	}
	
	/**
	 * Provides the minimum value of this table.
	 * <p>
	 * For more details, see {@link DoubleStream#min()}.
	 * 
	 * @return the minimum value of this table.
	 */
	public double min()
	{
		return stream().min().getAsDouble();
	}

	/**
	 * Adjusts this matrix such that every data value is normalized on [0.0,
	 * 1.0].
	 * 
	 * @return this object, for chaining calls.
	 */
	public Matrix normalize()
	{
		final double max = max();
		final double min = min();
		final double dataRange = max - min;
		
		for(int i = 0; i < size; i++)
			data[i] = (data[i] - min) / dataRange;
		return this;
	}

	/**
	 * Adjusts this matrix such that the minimum data value is equal to the
	 * provided low value, the maximum data value is equal to the provided high
	 * value, and each value in between appropriately scaled.
	 * 
	 * @param low
	 *            the low value.
	 * @param high
	 *            the high value.
	 * @return this object, for chaining calls.
	 */
	public Matrix normalize(double low, double high)
	{
		checkArgument(Double.isFinite(low),
				"low value must be finite");
		checkArgument(Double.isFinite(high),
				"high value must be finite");
		checkArgument(low <= high,
				"low value must be less than or equal to the high value");
		
		final double max = max();
		final double min = min();
		final double dataRange = max - min;
		final double outRange = high - low;
		
		for(int i = 0; i < size; i++)
			data[i] = (data[i] - min) / dataRange * outRange + low;
		return this;
	}

	/**
	 * Provides the two-dimensional position of the given array index.
	 * 
	 * @param index
	 *            the index to retrieve.
	 * @return the position of the given index.
	 */
	public IntPoint position(int index)
	{
		checkArgument(index >= 0 && index < size,
				"index not in bounds [0, %d], was [%s]",
				size, index);
		final int y = index / width;
		final int x = index - width * y;
		return new IntPoint(x, y);
	}
	
	/**
	 * Sets the matrix to the given value at the given index.
	 * 
	 * @param x
	 *            the X coordinate of the matrix to set.
	 * @param y
	 *            the Y coordinate of the matrix to set.
	 * @param v
	 *            the value to set.
	 * @return the previous value at the position.
	 * @throws ArrayIndexOutOfBoundsException
	 *             if the provided x, y values are outside the bounds of the
	 *             array.
	 */
	public double set(int x, int y, double v)
	{
		final int index = y * width + x;
		final double old = data[index];
		data[index] = v;
		return old;
	}
	
	/**
	 * @return the size of this <code>Matrix</code>.
	 */
	public int size()
	{
		return size;
	}
	
	/**
	 * Provides a {@link Spliterator} of the individual values in this matrix.
	 * <p>
	 * This is a call-through to {@link Arrays#spliterator(double[])}: see that
	 * method for details.
	 * 
	 * @return the spliterator of the data.
	 */
	public Spliterator.OfDouble spliterator()
	{
		return Arrays.spliterator(data);
	}

	/**
	 * Provides a stream-based view of the underlying data.
	 * <p>
	 * This is a call-through to
	 * <code>StreamSupport.doubleStream(Spliterator.OfDouble, boolean)</code>
	 * using {@link #spliterator()} as the data source and <code>false</code>
	 * for a sequential stream. See that method for details.
	 * 
	 * @return the stream of the data.
	 */
	public DoubleStream stream()
	{
		return StreamSupport.doubleStream(spliterator(), false);
	}
	
	/**
	 * Decodes the data into a grayscale image.
	 * <p>
	 * This is mainly for testing purposes and is not designed to be
	 * configurable. The output is an 8-bit grayscale image (
	 * <code>BufferedImage.TYPE_BYTE_GRAY</code>) of the same dimensions as the
	 * width/height with every value scaled for the image, where the
	 * lowest value is 0 and the highest 255. Individual values are floored off,
	 * which may result in banding, depending on the underlying data.
	 * 
	 * @return the constructed image.
	 */
	public BufferedImage toImage()
	{
		// generate an int based version of the input data, scaling as we go
		final double max = max();
		final double min = min();
		final double range = max - min;
		final int[] scaled = stream()
				.mapToInt(d -> (int) ((d - min) / range * 255))
				.toArray();
	
		// write to the raster
		final BufferedImage img = new BufferedImage(width, height,
				BufferedImage.TYPE_BYTE_GRAY);
		final WritableRaster raster = img.getRaster();
		raster.setPixels(0, 0, width, height, scaled);
		img.setData(raster);
		return img;
	}

	/**
	 * Decodes the data into a multicolor image of
	 * {@link BufferedImage#TYPE_INT_ARGB}.
	 * <p>
	 * This is similar to {@link #toImage()}, but is designed to allow greater
	 * flexibility in the final image configuration. This processes data values
	 * in parallel, providing the matrix value to the provided color function,
	 * which responds with a packed integer in 0xAARRGGBB order that is applied
	 * to the underlying image. This process is generally quite fast, even on
	 * large images.
	 * <p>
	 * For a basic color picker implementation that hides some of the gory
	 * details of this method, see {@link ColorChooser}.
	 * 
	 * @param converter
	 *            the color generation function as described, which must be
	 *            non-interfering for parallel usage (as described).
	 * @return the constructed image.
	 */
	public BufferedImage toImage(DoubleToIntFunction converter)
	{
		checkNotNull(converter, "converter cannot be null");
		
		// generate an int based version of the input data, scaling as we go
		final int[] scaled = stream()
				.mapToInt(converter)
				.toArray();
		
		// then make the image based on the streamed data
		// thanks to https://stackoverflow.com/questions/6319465#12062505
		// for the idea of directly bypassing the Java weirdness around this
		// stuff and just copy the array contents directly
		BufferedImage image = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_ARGB);
		int[] imageData = ((DataBufferInt) image.getRaster().getDataBuffer())
				.getData();
		System.arraycopy(scaled, 0, imageData, 0, imageData.length);
		return image;
	}

	/**
	 * Decodes the data into a multicolor image of type
	 * {@link BufferedImage#TYPE_INT_RGB}.
	 * <p>
	 * This is identical to {@link #toImage(DoubleToIntFunction)} except for the
	 * result image type. The converter function should be appropriately
	 * modified, if needed.
	 * 
	 * @param type
	 *            the desired data type of the result image.
	 * @param converter
	 *            the color generation function as described, which must be
	 *            non-interfering for parallel usage (as described).
	 * @return the constructed image.
	 */
	public BufferedImage toOpaqueImage(DoubleToIntFunction converter)
	{
		checkNotNull(converter, "converter cannot be null");
		
		final int[] scaled = stream()
				.mapToInt(converter)
				.toArray();
		
		BufferedImage image = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_RGB);
		int[] imageData = ((DataBufferInt) image.getRaster().getDataBuffer())
				.getData();
		System.arraycopy(scaled, 0, imageData, 0, imageData.length);
		return image;
	}
}
