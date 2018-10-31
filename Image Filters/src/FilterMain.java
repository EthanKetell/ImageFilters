import java.awt.image.BufferedImage;

public class FilterMain {

	public static void main(String[] args) {
		BufferedImage image = ImageHandler.readImage("flower.jpg");
		
		BufferedImage edges = ImageHandler.filterEdgeDetect(image);
		edges = ImageHandler.filterMaxRange(edges);
		ImageHandler.showImage(edges);
		ImageHandler.writeImage("flower_edge", edges);
	}

}
