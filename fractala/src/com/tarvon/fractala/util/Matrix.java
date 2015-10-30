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

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.function.DoubleToIntFunction;
import java.util.function.ToDoubleFunction;
import java.util.stream.DoubleStream;
import java.util.stream.StreamSupport;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Doubles;

/**
 * Defines an immutable grid of <code>double</code> data backed by an array.
 * <p>
 * This is guaranteed to match the size provided by {@link #getWidth()} and
 * {@link #getHeight()}, and assumes that the matrix is <i>dense</i>, with all
 * values treated as if they were significant.
 * <p>
 * The internal backing array is limited by Java to a maximum size, declared in
 * {@link #MAX_SIZE}. This is enforced by the construction methods. For the
 * expected uses of this class, this isn't likely a big deal, but users should
 * be aware of it.
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
public class Matrix
{
	/**
	 * Builder class for the matrix.
	 * 
	 * @author saybur
	 *
	 */
	public static final class Builder
	{
		private final int width;
		private final int height;
		private final double[] data;

		private Builder(int width, int height)
		{
			Preconditions.checkArgument(width > 0,
					"width must be greater than zero");
			Preconditions.checkArgument(height > 0,
					"height must be greater than zero");
			final long size = width * height;
			Preconditions.checkArgument(size < MAX_SIZE,
					"source array is larger than the maximum size allowed");
			
			this.width = width;
			this.height = height;
			this.data = new double[(int) size];
		}
		
		/**
		 * Creates a new instance of {@link Matrix} from the builder state.
		 * <p>
		 * The created object is immutable, and further changes to
		 * {@link #set(int, int, double)} will not be reflected in the new
		 * object.
		 * 
		 * @return the created table.
		 */
		public Matrix create()
		{
			double[] safeCopy = new double[data.length];
			System.arraycopy(data, 0, safeCopy, 0, data.length);
			return new Matrix(width, height, safeCopy);
		}
		
		public int getHeight()
		{
			return height;
		}

		public int getWidth()
		{
			return width;
		}

		/**
		 * Sets the matrix to the given value at the given index.
		 * <p>
		 * This is a simple assignment to the internal array, so this is
		 * threadsafe so long as all threads are good about not updating the
		 * same indices at the same time.
		 * 
		 * @param x
		 *            the X coordinate of the matrix to set.
		 * @param y
		 *            the Y coordinate of the matrix to set.
		 * @param v
		 *            the value to set.
		 * @return the builder, for chaining.
		 * @throws ArrayIndexOutOfBoundsException
		 *             if the provided x, y values are outside the bounds of the
		 *             array.
		 */
		public Builder set(int x, int y, double v)
		{
			data[y * width + x] = v;
			return this;
		}
	}
	
	/**
	 * Defines the maximum number of values that a {@link Matrix} can store.
	 * <p>
	 * Per https://stackoverflow.com/questions/3038392
	 */
	public static final int MAX_SIZE = Integer.MAX_VALUE - 5;
	
	/**
	 * Averages the values in each cell of the given matrices.
	 * 
	 * @param matrices
	 *            the matrices to work with, per
	 *            {@link #merge(ToDoubleFunction, Matrix...)}.
	 * @return the result.
	 */
	public static Matrix averageOf(Matrix... matrices)
	{
		return merge(v -> v.average().getAsDouble(), matrices);
	}
	
	/**
	 * Merges the provided matrices together using the given function.
	 * <p>
	 * The function provides flexibility about the type of merging operation
	 * desired. The stream it is provided with will always be non-empty.
	 * <p>
	 * This version, as opposed to
	 * {@link #mergeParallel(ToDoubleFunction, Matrix...)}, executes
	 * sequentially. For simple operations it is typically faster. Use the other
	 * method when the function is complicated and slow.
	 * 
	 * @param function
	 *            the function, which provides the result based on whatever
	 *            algorithm is desired by the user.
	 * @param matricesPassed
	 *            the matrices to work with, which must have at least a single
	 *            value that is not <code>null</code>.
	 * @return the result.
	 */
	public static Matrix merge(final ToDoubleFunction<DoubleStream> function,
			final Matrix... matricesPassed)
	{
		Preconditions.checkNotNull(function,
				"function cannot be null");
		Preconditions.checkNotNull(matricesPassed,
				"passed matrices cannot be null");
		Preconditions.checkArgument(matricesPassed.length > 0,
				"passed matrices cannot be empty");
		
		// make a safe reference copy of the matrices
		// and make sure nothing is null
		Matrix[] matrices = new Matrix[matricesPassed.length];
		System.arraycopy(matricesPassed, 0, matrices, 0, matrices.length);
		for(Matrix t : matrices)
		{
			Preconditions.checkNotNull(t,
					"passed matrices cannot contain a null member");
		}
		
		// get and validate the width/height values
		final int width = matrices[0].width;
		final int height = matrices[0].height;
		for(int i = 1; i < matrices.length; i++)
		{
			Preconditions.checkArgument(width == matrices[i].width,
					"widths not equal: one matrix was %s, another was %s",
					width, matrices[i].width);
			Preconditions.checkArgument(height == matrices[i].height,
					"heights not equal: one matrix was %s, another was %s",
					height, matrices[i].height);
		}
		
		// perform the calculation on the new data
		double[] buffer = new double[matrices.length];
		double[] result = new double[width * height];
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
	 * Merges the provided matrices together using the given function.
	 * <p>
	 * The function provides flexibility about the type of merging operation
	 * desired. The stream it is provided with will always be non-empty.
	 * <p>
	 * This executes in parallel, so the provided function must be
	 * non-interfering. This is often slower than the simpler sequential
	 * operation of {@link #merge(ToDoubleFunction, Matrix...)}, which may be
	 * more appropriate in some situations.
	 * 
	 * @param function
	 *            the function, which provides the result based on whatever
	 *            algorithm is desired by the user.
	 * @param matricesPassed
	 *            the matrices to work with, which must have at least a single
	 *            value that is not <code>null</code>.
	 * @return the result.
	 */
	public static Matrix mergeParallel(
			final ToDoubleFunction<DoubleStream> function,
			final Matrix... matricesPassed)
	{
		Preconditions.checkNotNull(function,
				"function cannot be null");
		Preconditions.checkNotNull(matricesPassed,
				"passed matrices cannot be null");
		Preconditions.checkArgument(matricesPassed.length > 0,
				"passed matrices cannot be empty");
		
		// make a safe reference copy of the matrices
		// and make sure nothing is null
		Matrix[] matrices = new Matrix[matricesPassed.length];
		System.arraycopy(matricesPassed, 0, matrices, 0, matrices.length);
		for(Matrix t : matrices)
		{
			Preconditions.checkNotNull(t,
					"passed matrices cannot contain a null member");
		}
		
		// get and validate the width/height values
		final int width = matrices[0].width;
		final int height = matrices[0].height;
		for(int i = 1; i < matrices.length; i++)
		{
			Preconditions.checkArgument(width == matrices[i].width,
					"widths not equal: one matrix was %s, another was %s",
					width, matrices[i].width);
			Preconditions.checkArgument(height == matrices[i].height,
					"heights not equal: one matrix was %s, another was %s",
					height, matrices[i].height);
		}
		
		// perform the calculation on the new data
		@SuppressWarnings("serial")
		final class MergeAction extends RecursiveAction
		{
			private double[] result;
			private Matrix[] matrices;
			private int low;
			private int high;
			
			private MergeAction(double[] result, Matrix[] matrices,
					int low, int high)
			{
				this.result = result;
				this.matrices = matrices;
				this.low = low;
				this.high = high;
			}

			@Override
			protected void compute()
			{
				if (high - low <= 20000)
				{
					computeDirectly();
				}
				else
				{
					int mid = (low + high) >>> 1;
					invokeAll(new MergeAction(result, matrices, low, mid),
							new MergeAction(result, matrices, mid, high));
				}
			}
			
			private void computeDirectly()
			{
				// set up the data, both what this accesses and what it
				// presents to the function for processing
				double[] cell = new double[matrices.length];
				
				// then process
				for(int i = low; i < high; i++)
				{
					// fill data
					for(int t = 0; t < matrices.length; t++)
					{
						cell[t] = matrices[t].data[i];
					}
					// then process
					result[i] = function.applyAsDouble(DoubleStream.of(cell));
				}
			}
		}
		double[] result = new double[width * height];
		MergeAction action = new MergeAction(
				result, matrices, 0, width * height);
		ForkJoinPool.commonPool().invoke(action);
		
		// then provide the result back
		return new Matrix(width, height, result);
	}
	
	/**
	 * Creates a matrix builder of the given size. All values will be
	 * initialized to 0.0.
	 * 
	 * @param width
	 *            the width of the table.
	 * @param height
	 *            the height of the table.
	 * @return the <code>Builder</code>.
	 */
	public static Builder of(int width, int height)
	{
		return new Builder(width, height);
	}
	
	/**
	 * Sums the values in each cell of the given matrices.
	 * 
	 * @param matrices
	 *            the matrices to work with, per
	 *            {@link #merge(ToDoubleFunction, Matrix...)}.
	 * @return the result.
	 */
	public static Matrix sumOf(Matrix... matrices)
	{
		return merge(v -> v.sum(), matrices);
	}
	
	private final int width;
	private final int height;
	private final double[] data;

	/**
	 * Direct constructor. This must be provided with non-<code>null</code>
	 * values that cannot be modified by anyone outside this class to satisfy
	 * the class contract.
	 * 
	 * @param width
	 * @param height
	 * @param data
	 */
	private Matrix(int width, int height, double[] data)
	{
		this.width = width;
		this.height = height;
		this.data = data;
	}
	
	/**
	 * Provides the value at the given coordinate.
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
	 * @return the height of this table.
	 */
	public int getHeight()
	{
		return height;
	}
	
	/**
	 * @return the width of this table.
	 */
	public int getWidth()
	{
		return width;
	}

	/**
	 * Provides the array index of a particular two-dimensional grid coordinate.
	 * <p>
	 * Internally, this class uses <a
	 * href="https://en.wikipedia.org/wiki/Row-major_order">row-major order</a>
	 * to store the values.
	 * <p>
	 * This is primarily for use with {@link #view()}.
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
	 * Provides a copy of this matrix with every data value normalized on [0.0,
	 * 1.0].
	 * 
	 * @return the normalized copy of this matrix.
	 */
	public Matrix normalize()
	{
		final double max = max();
		final double min = min();
		final double dataRange = max - min;
		
		double[] copy = this.stream()
				.map(v -> (v - min) / dataRange)
				.toArray();
		return new Matrix(width, height, copy);
	}
	
	/**
	 * Provides a copy of this matrix with the minimum data value equal to the
	 * provided low value, the maximum equal to the provided high value, and
	 * each value in between appropriately scaled.
	 * 
	 * @param low
	 *            the low value.
	 * @param high
	 *            the high value.
	 * @return the copy of this matrix.
	 */
	public Matrix normalize(double low, double high)
	{
		Preconditions.checkArgument(Double.isFinite(low),
				"low value must be finite");
		Preconditions.checkArgument(Double.isFinite(high),
				"high value must be finite");
		Preconditions.checkArgument(low <= high,
				"low value must be less than or equal to the high value");
		final double max = max();
		final double min = min();
		final double dataRange = max - min;
		final double outRange = high - low;
		
		double[] copy = this.stream()
				.map(v -> (v - min) / dataRange * outRange + low)
				.toArray();
		return new Matrix(width, height, copy);
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
	 * Provides an immutable stream-based view of the underlying data.
	 * <p>
	 * This is a call-through to
	 * {@link StreamSupport#doubleStream(java.util.Spliterator.OfDouble, boolean)}
	 * using {@link #spliterator()} and <code>false</code> for default
	 * parallelism. See that method for details.
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
	 * {@link BufferedImage#TYPE_BYTE_GRAY}) of the same dimensions as the input
	 * width/height with every value scaled for the image, where the lowest
	 * value is 0 and the highest 255. Individual values are floored off, which
	 * may result in banding, depending on the underlying data.
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
		Preconditions.checkNotNull(converter, "converter cannot be null");
		
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
	 * Provides an immutable, <code>List</code> based view of the underlying
	 * matrix data.
	 * <p>
	 * See {@link #indexOf(int, int)} for how the data is organized. This
	 * operation uses wrapper classes, so it should be relatively
	 * memory-efficient.
	 * 
	 * @return the immutable view of the underlying data.
	 */
	public List<Double> view()
	{
		return Collections.unmodifiableList(Doubles.asList(data));
	}
}
