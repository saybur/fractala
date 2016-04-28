package com.tarvon.fractala.util;

import java.awt.EventQueue;
import java.awt.Graphics;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/**
 * Graphical utility to help select {@link ColorChooser} values.
 * 
 * @author saybur
 *
 */
public class ColorHelper
{
	@SuppressWarnings("serial")
	private final class ColorPanel extends JPanel
	{
		private ColorChooser chooser;
		
		public ColorPanel()
		{
			setOpaque(true);
			setPreferredSize(new Dimension(100, 600));
		}
		
		@Override
		public void paint(Graphics g)
		{
			// get draw size
			final Dimension size = this.getSize();
			if(size == null)
				return;
			final int width = size.width;
			final int height = size.height;
			if(width == 0 || height == 0)
				return;
			
			// alias chooser
			final ColorChooser chooser = this.chooser;
			if(chooser == null)
			{
				g.setColor(Color.BLACK);
				g.fillRect(0, 0, width, height);
				return;
			}
			
			// get maximum draw
			final double maxValue = ((Double) maxSpinner.getValue())
					.doubleValue();
			
			// create drawing image
			final BufferedImage image = new BufferedImage(width, height,
					BufferedImage.TYPE_INT_RGB);
			for(int y = 0; y < height; y++)
			{
				double n = y / (double) height;
				n *= maxValue;
				
				int color = chooser.applyAsInt(n);
				for(int x = 0; x < width; x++)
					image.setRGB(x, y, color);
			}
			
			// then draw
			g.drawImage(image, 0, 0, null);
		}
	}
	
	private static final int ROWS = 16;
	private static final double MAX_RANGE = 10.0;
	private static final Color ERROR_COLOR = new Color(255, 200, 200);
	private static final Color GOOD_COLOR = new Color(255, 255, 255);
	
	public static void main(String[] args)
	{
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				try
				{
					ColorHelper window = new ColorHelper();
					window.frame.setVisible(true);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		});
	}
	
	private final JFrame frame;
	private final JSpinner maxSpinner;
	private final List<JSpinner> spinners;
	private final List<JTextField> fields;
	private final ColorPanel colorPanel;
	
	public ColorHelper()
	{
		frame = new JFrame("Color Helper");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		
		// make the color panel
		colorPanel = new ColorPanel();
		frame.getContentPane().add(colorPanel, BorderLayout.EAST);
		
		// create GUI elements
		spinners = IntStream.range(0, ROWS).boxed()
				.map(i ->
				{
					final JSpinner spinner = new JSpinner(new SpinnerNumberModel(
							0.0, 0.0, MAX_RANGE, 0.001));
					spinner.setBackground(ERROR_COLOR);
					spinner.addChangeListener(e -> update());
					spinner.setPreferredSize(new Dimension(
							100,
							spinner.getPreferredSize().height));
					return spinner;
				})
				.collect(Collectors.toList());
		fields = IntStream.range(0, ROWS).boxed()
				.map(i ->
				{
					final JTextField field = new JTextField();
					field.setBackground(ERROR_COLOR);
					field.addActionListener(e -> update());
					field.setPreferredSize(new Dimension(
							100,
							field.getPreferredSize().height));
					return field;
				})
				.collect(Collectors.toList());
		
		// put a few defaults in
		spinners.get(0).setValue(0.0);
		fields.get(0).setText("#000000");
		spinners.get(1).setValue(1.0);
		fields.get(1).setText("#FFFFFF");
		
		// create table of entry objects
		final JPanel entryPanel = new JPanel();
		entryPanel.setLayout(new GridLayout(ROWS, 2, 2, 2));
		for(int i = 0; i < ROWS; i++)
		{
			entryPanel.add(spinners.get(i));
			entryPanel.add(fields.get(i));
		}
		frame.getContentPane().add(entryPanel);
		
		// the maximum spinner
		maxSpinner = new JSpinner(new SpinnerNumberModel(
				1.0, 1.0, MAX_RANGE, 0.1));
		maxSpinner.addChangeListener(e -> update());
		frame.getContentPane().add(maxSpinner, BorderLayout.NORTH);
		
		// redrawing button
		final JButton redrawButton = new JButton("Redraw");
		redrawButton.addActionListener(e -> update());
		frame.getContentPane().add(redrawButton, BorderLayout.SOUTH);
		
		update();
		frame.pack();
	}
	
	private void update()
	{
		final ColorChooser.Builder b = ColorChooser.builder();
		for(int i = 0; i < ROWS; i++)
		{
			final JSpinner spinner = spinners.get(i);
			final JTextField field = fields.get(i);
			
			// fetch color or error
			final Color color;
			try
			{
				color = Color.decode(field.getText());
				field.setBackground(GOOD_COLOR);
			}
			catch(Exception e)
			{
				field.setBackground(ERROR_COLOR);
				continue;
			}
						
			// fetch value or error
			final double value;
			try
			{
				value = ((Double) spinner.getValue()).doubleValue();
				spinner.setBackground(GOOD_COLOR);
			}
			catch(Exception e)
			{
				spinner.setBackground(ERROR_COLOR);
				continue;
			}
			
			// insert into chooser
			b.add(value, color);
		}
		
		// assign
		try
		{
			ColorChooser chooser = b.create();
			colorPanel.chooser = chooser;
		}
		catch(Exception e)
		{
			colorPanel.chooser = null;
		}
		colorPanel.repaint();
	}
}
