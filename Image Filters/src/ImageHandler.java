import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class ImageHandler {
	
	@SuppressWarnings("serial")
	private static class ColorRangeException extends RuntimeException {

		public ColorRangeException(String message) {
			super(message);
		}
		
	}
	
	private static class Color {
		final int a,r,g,b,argb;
		
		Color(int argb) {
			this.argb = argb;
			this.a = (argb >> 24) & 0xff;
			this.r = (argb >> 16) & 0xff;
			this.g = (argb >>  8) & 0xff;
			this.b = (argb >>  0) & 0xff;
		}
		
		Color(int r, int g, int b) {
			this(0xff,r,g,b);
		}
		
		Color(int a, int r, int g, int b){
			checkRange(0,r,g,b);
			this.a = a;
			this.r = r;
			this.g = g;
			this.b = b;
			this.argb = a << 24 | r << 16 | g << 8 | b;
		}
		
		private void checkRange(int a, int r, int g, int b) {
			if((a & 0xffffff00) != 0) {
				throw new ColorRangeException(a+" is not a valid value, must be 0 <= a <= 255");
			}
			if((r & 0xffffff00) != 0) {
				throw new ColorRangeException(r+" is not a valid value, must be 0 <= r <= 255");
			}
			if((g & 0xffffff00) != 0) {
				throw new ColorRangeException(g+" is not a valid value, must be 0 <= g <= 255");
			}
			if((b & 0xffffff00) != 0) {
				throw new ColorRangeException(b+" is not a valid value, must be 0 <= b <= 255");
			}
		}
	}
	
	private static String imagePath = "res"+File.separator+"images"+File.separator;
	
	static {
		File imageFolder = new File(imagePath);
		if(!imageFolder.exists()) imageFolder.mkdirs();
	}
	
	public static BufferedImage readImage(String name) {
		File f = new File(imagePath+name);
		try {
			return ImageIO.read(f);
		} catch (IOException e) {
			System.err.println("Failed to load image '"+name+"'");
			return null;
		}
	}
	
	public static void writeImage(String name, BufferedImage image) {
		if(name.contains(".")) name = name.substring(0, name.lastIndexOf('.'));
		File f = new File(imagePath+name+".png");
		if(f.exists()) {
			int response = JOptionPane.showConfirmDialog(null, "File "+f.getPath()+" exists, overwrite?","Confirm Overwrite", JOptionPane.YES_NO_OPTION);
			if(response == JOptionPane.NO_OPTION) return;
		}
		try {
			ImageIO.write(image, "PNG", f);
		} catch (IOException e) {
			System.err.println("Failed to write "+f.getPath());
			e.printStackTrace();
		}
	}
	
	public static void showImage(BufferedImage image) {
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		@SuppressWarnings("serial")
		JPanel panel = new JPanel() {
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(image.getWidth(), image.getHeight());
			}
			
			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.drawImage(image, 0, 0, null);
			}
		};

		frame.add(panel);
		frame.pack();
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	public static BufferedImage filterGrayscale(BufferedImage image) {
		BufferedImage out = new BufferedImage(image.getWidth(),image.getHeight(),BufferedImage.TYPE_INT_ARGB);
		
		for(int y = 0; y < image.getHeight(); y++) {
			for(int x = 0; x < image.getWidth(); x++) {
				Color color = new Color(image.getRGB(x, y));
				int gray = (int)(color.r * 0.299 + color.g * 0.587 + color.b * 0.114);
				Color grayScale = new Color(color.a, gray, gray, gray);
				out.setRGB(x, y, grayScale.argb);
			}
		}
		return out;
	}
	
	public static BufferedImage filterMaxRange(BufferedImage image) {
		int min = 255, max = 0;
		for(int y = 0; y < image.getHeight(); y++) {
			for(int x = 0; x < image.getWidth(); x++) {
				int val = image.getRGB(x, y) & 0xff;
				if(val < min) {
					min = val;
				}
				if(val > max) {
					max = val;
				}
			}
		}
		double scale = 255/(max-min);
		
		BufferedImage out = new BufferedImage(image.getWidth(),image.getHeight(),BufferedImage.TYPE_INT_ARGB);
		for(int y = 0; y < image.getHeight(); y++) {
			for(int x = 0; x < image.getWidth(); x++) {
				int val = image.getRGB(x, y) & 0xff;
				val -= min;
				val *= scale;
				Color color = new Color(val, val, val);
				out.setRGB(x, y, color.argb);
			}
		}
		return out;
	}

	public static BufferedImage filterVerticalEdgeDetect(BufferedImage image) {
		BufferedImage gray = filterGrayscale(image);
		BufferedImage out = new BufferedImage(image.getWidth(),image.getHeight(),BufferedImage.TYPE_INT_ARGB);
		
		for(int y = 0; y < image.getHeight(); y++) {
			for(int x = 0; x < image.getWidth(); x++) {
				int[] pixels = getPixelValues(gray,x-1,y-1,3,3,EdgeType.STRETCH);
				int[] weights = {
						-1,0,1,
						-2,0,2,
						-1,0,1
				};
				int value = (int)Math.abs(getWeightedAverage(pixels, weights));
				Color edge = new Color(value,value,value);
				out.setRGB(x, y, edge.argb);
			}
		}
		return out;
	}
	
	public static BufferedImage filterHorizontalEdgeDetect(BufferedImage image) {
		BufferedImage gray = filterGrayscale(image);
		BufferedImage out = new BufferedImage(image.getWidth(),image.getHeight(),BufferedImage.TYPE_INT_ARGB);
		
		for(int y = 0; y < image.getHeight(); y++) {
			for(int x = 0; x < image.getWidth(); x++) {
				int[] pixels = getPixelValues(gray,x-1,y-1,3,3,EdgeType.STRETCH);
				int[] weights = {
						 1, 2, 1,
						 0, 0, 0,
						-1,-2,-1
				};
				int value = (int)Math.abs(getWeightedAverage(pixels, weights));
				Color edge = new Color(value,value,value);
				out.setRGB(x, y, edge.argb);
			}
		}
		return out;
	}
	
	public static BufferedImage filterEdgeDetect(BufferedImage image) {
		return add(filterHorizontalEdgeDetect(image), filterVerticalEdgeDetect(image));
	}
	
	public static BufferedImage add(BufferedImage image1, BufferedImage image2) {
		BufferedImage out = new BufferedImage(
				Math.max(image1.getWidth(),image2.getWidth()),
				Math.max(image1.getHeight(), image2.getHeight()),
				BufferedImage.TYPE_INT_ARGB);
		
		for(int y = 0; y < image1.getHeight(); y++) {
			for(int x = 0; x < image1.getWidth(); x++) {
				Color color1, color2;
				try {
					color1 = new Color(image1.getRGB(x, y));
				} catch(ArrayIndexOutOfBoundsException e) {
					color1 = new Color(0,0,0);
				}
				try {
					color2 = new Color(image2.getRGB(x, y));
				} catch(ArrayIndexOutOfBoundsException e) {
					color2 = new Color(0,0,0);
				}
				Color sum = new Color(
						Math.min(255, color1.r + color2.r),
						Math.min(255, color1.g + color2.g),
						Math.min(255, color1.b + color2.b));
				out.setRGB(x, y, sum.argb);
			}
		}
		return out;
	}
	
	private static double getWeightedAverage(int[] pixels, int[] weights) {
		if(weights.length != pixels.length) {
			throw new RuntimeException("pixels and weights array size mismatch");
		}
		double total = 0, divisor = 0;
		for(int i = 0; i < weights.length; i++) {
			divisor += Math.abs(weights[i]);
			total += weights[i] * pixels[i];
		}
		return total/divisor;
	}
	
	private static enum EdgeType {
		/** Wrap around, taking pixels from the opposite side of the image */
		WRAP,
		/** Use the edge pixels for everything out of bounds */
		STRETCH,
		/** Every pixel out of bounds will be treated as black */
		BLACK,
		/** Every pixel out of bounds will be treated as white */
		WHITE,
		/** Every pixel out of bounds will be treated as 50% gray */
		GRAY;
	}
	private static int[] getPixelValues(BufferedImage image, int x, int y, int width, int height, EdgeType edgeType) {
		int[] out = new int[width * height];
		int i = 0;
		for(int py = y; py < y + height; py++) {
			for(int px = x; px < x + width; px++) {
				if(px >= 0 && px < image.getWidth() && py >= 0 && py < image.getHeight()) {
					out[i] = 0xff&image.getRGB(px, py);
				} else {
					switch(edgeType) {
					case BLACK:
						out[i] = 0x00;
						break;
					case GRAY:
						out[i] = 0x80;
						break;
					case WHITE:
						out[i] = 0xff;
						break;
					case STRETCH:
						int nx = px;
						int ny = py;
						if(nx < 0) {
							nx = 0;
						}
						if(nx >= image.getWidth()) {
							nx = image.getWidth()-1;
						}
						if(ny < 0) {
							ny = 0;
						}
						if(ny >= image.getHeight()) {
							ny = image.getHeight()-1;
						}
						out[i] = 0xff & image.getRGB(ny, ny);
						break;
					case WRAP:
						px %= image.getWidth();
						py %= image.getHeight();
						if(px < 0) {
							px += image.getWidth();
						}
						if(py < 0) {
							py += image.getHeight();
						}
						out[i] = 0xff & image.getRGB(px, py);
						break;
					}
				}
				i++;
			}
		}
		return out;
	}
	
}
