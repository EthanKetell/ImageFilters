import java.awt.image.BufferedImage;

public class FilterMain {

	public static void main(String[] args) {
		BufferedImage image = ImageHandler.readImage("flower.jpg");
		ImageHandler.showImage(image);
		
		BufferedImage gray = ImageHandler.filterGrayscale(image);
		ImageHandler.showImage(gray);
		
		BufferedImage edges = ImageHandler.filterEdgeDetect(image);
		ImageHandler.showImage(edges);
		ImageHandler.writeImage("flower_edge", edges);
	}

}
