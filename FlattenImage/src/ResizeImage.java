import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.imageio.ImageIO;

public class ResizeImage {

	public static void main(String[] args) throws IOException {
		int i, j = 0;
		char prevChar = '\0', currentChar;
		File folder = new File("../../images/");
	    File[] listOfFiles = folder.listFiles();
	    Arrays.parallelSort(listOfFiles);
	    System.out.println("Total No. of Files:" + listOfFiles.length);
		Image img = null;
		BufferedImage tempPNG = null;
		File newFilePNG = null;
		for (i = 0;i < listOfFiles.length; i++)
		{
			if (listOfFiles[i].isFile())
			{
				currentChar = listOfFiles[i].getName().charAt(0);
				if (currentChar != prevChar)
				{
					j = 0;
					prevChar = currentChar;
				}
				System.out.println("Resizing file:" + listOfFiles[i].getName());
				img = ImageIO.read(new File("../../images/" + listOfFiles[i].getName()));
				tempPNG = resizeImage(img, 32, 32);
				newFilePNG = new File("./resized/" + prevChar + "_" + String.format("%04d", j) + ".png");
				ImageIO.write(tempPNG, "png", newFilePNG);
				j++;
			}
		}
		System.out.println("Resize job complete.");
	}

	/**
	 * This function resize the image file and returns the BufferedImage object that can be saved to file system.
	 */
	public static BufferedImage resizeImage(final Image image, int width, int height) {
        final BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final Graphics2D graphics2D = bufferedImage.createGraphics();
        graphics2D.setComposite(AlphaComposite.Src);
        
        //below three lines are for RenderingHints for better image quality at cost of higher processing time
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.drawImage(image, 0, 0, width, height, null);
        graphics2D.dispose();
        return bufferedImage;
    }
}