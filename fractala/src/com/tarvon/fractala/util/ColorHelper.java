package com.tarvon.fractala.util;

import java.awt.EventQueue;
import java.awt.Graphics;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
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

import com.google.common.base.Strings;
import com.tarvon.fractala.Fractals;

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
			setPreferredSize(new Dimension(1024, 512));
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
			
			// get seed
			final int seed = ((Integer) seedSpinner.getValue())
					.intValue();
			
			// draw
			final BufferedImage image = Fractals
					.createSimplexFractal(seed, 10)
					.call()
					.normalize(0, MAX)
					.toImage(chooser);
			g.drawImage(image, 0, 0, null);
		}
	}
	
	private static final int ROWS = 16;
	private static final double MAX = 100.0;
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
	private final JSpinner seedSpinner;
	private final List<JSpinner> spinners;
	private final List<JTextField> fields;
	private final ColorPanel colorPanel;
	
	private final JDialog colorChooserDialog;
	private final JColorChooser colorChooser;
	private boolean colorChooserOk;
	
	public ColorHelper()
	{
		frame = new JFrame("Color Helper");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		
		// make the color panel, and the chooser
		colorPanel = new ColorPanel();
		frame.getContentPane().add(colorPanel, BorderLayout.EAST);
		colorChooser = new JColorChooser();
		colorChooserDialog = JColorChooser.createDialog(frame,
				"Choose Color",
				true,
				colorChooser,
				e -> colorChooserOk = true,
				null);
		colorChooserOk = false;
		
		// create GUI elements
		spinners = IntStream.range(0, ROWS).boxed()
				.map(i ->
				{
					final JSpinner spinner = new JSpinner(new SpinnerNumberModel(
							0.0, 0.0, MAX, 0.1));
					spinner.setBackground(ERROR_COLOR);
					spinner.setPreferredSize(new Dimension(
							80,
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
							80,
							field.getPreferredSize().height));
					return field;
				})
				.collect(Collectors.toList());
		final List<JButton> buttons = fields.stream()
				.map(f ->
				{
					final JButton button = new JButton("...");
					button.addActionListener(e -> chooseColor(f));
					return button;
				})
				.collect(Collectors.toList());
		
		// put a few defaults in
		spinners.get(0).setValue(0.0);
		fields.get(0).setText("#000000");
		spinners.get(1).setValue(MAX);
		fields.get(1).setText("#FFFFFF");
		
		// create table of entry objects
		final JPanel entryPanel = new JPanel();
		entryPanel.setLayout(new GridLayout(ROWS, 3, 2, 2));
		for(int i = 0; i < ROWS; i++)
		{
			entryPanel.add(spinners.get(i));
			entryPanel.add(fields.get(i));
			entryPanel.add(buttons.get(i));
		}
		frame.getContentPane().add(entryPanel);
		
		// the seed spinner
		seedSpinner = new JSpinner(new SpinnerNumberModel(
				1000, 1, Integer.MAX_VALUE, 1));
		seedSpinner.addChangeListener(e -> update());
		frame.getContentPane().add(seedSpinner, BorderLayout.NORTH);
		
		// redrawing button
		final JButton redrawButton = new JButton("Redraw");
		redrawButton.addActionListener(e -> update());
		frame.getContentPane().add(redrawButton, BorderLayout.SOUTH);
		
		update();
		frame.pack();
	}
	
	private void chooseColor(JTextField f)
	{
		// pick existing field color
		Color prev;
		try
		{
			prev = Color.decode(f.getText());
		}
		catch(Exception ex)
		{
			prev = Color.BLACK;
		}
		
		// let the user pick a color
		colorChooser.setColor(prev);
		colorChooserOk = false;
		colorChooserDialog.setVisible(true);
		Color picked = colorChooser.getColor();
		
		// then assign
		if(colorChooserOk)
		{
			String c = Integer.toHexString(picked.getRGB());
			if(c.length() == 7)
			{
				c = c.substring(1, 7);
			}
			else if(c.length() > 7)
			{
				c = c.substring(2, 8);
			}
			else if(c.length() < 6)
			{
				c = Strings.padStart(c, 6, '0');
			}
			f.setText("#" + c);
		}
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
