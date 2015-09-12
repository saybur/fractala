Fractala
========

Java library for creating spherical coherent noise.

This library is in an early stage of development, but should hopefully be usable as-is.  Be aware that significant API changes are still likely.  Feedback is appreciated.

Example Usage
-------------

```java
Fractal fractal = Fractals.createSimplexFractal(10);
Matrix data = simplex.call().normalize();
ColorChooser chooser = ColorChooser.builder()
		.add(0.45,	ColorChooser.parseColor("#001a4b"))
		.add(0.5,	ColorChooser.parseColor("#003584"))
		.add(0.51,	ColorChooser.parseColor("#003000"))
		.add(0.95,	ColorChooser.parseColor("d0c1a2"))
		.add(1.0,	ColorChooser.parseColor("c5c5c5"))
		.create();
BufferedImage image = data.toImage(chooser);
ImageIO.write(image, "png", new File("test.png"));
```

The above code will produce something like [this](https://imgur.com/gallery/11MnYMs).

Requirements
------------

- Java 8
- [Google Guava](https://github.com/google/guava)
