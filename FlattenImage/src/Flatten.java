import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

public class Flatten {

	public static void main(String[] args) throws IOException {
		File[] files = new File("./resized/").listFiles();
		File output = new File("flattened.out");
		BufferedWriter bw = new BufferedWriter(new FileWriter(output));
		char prevChar = 'C';
		Arrays.parallelSort(files);
		
		for (File file : files)
		{
			if (file.getName().charAt(0) != prevChar)
			{
				prevChar = file.getName().charAt(0);
			}
	        if (file.isFile())
	        {
	        	System.out.println("Processing image: " + file.getName());
	        	BufferedImage image = ImageIO.read(file);
	        	
	        	for (int y = 2; y < (image.getHeight() - 2); y++)
	        	{
    				if (y == 2)
    				{
    					bw.write(prevChar);
    					bw.write(' ');
    				}
	    			for (int x = 2; x < (image.getWidth() - 2); x++)
	    			{
	    				if (Math.abs(image.getRGB(x, y)) > 1)
	    					bw.write('1');
	    				else
	    					bw.write('0');
	    			}
	    		}
	        	image.flush();
	        	bw.write('\n');
	        }
	        bw.flush();
	    }
		bw.write('\0');
		bw.close();
		System.out.println("Image flattening complete");
	}

}