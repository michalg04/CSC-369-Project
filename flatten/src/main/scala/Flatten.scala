import java.io.{BufferedWriter, File, FileWriter, IOException}
import java.util

import javax.imageio.ImageIO


object Flatten {
  @throws[IOException]
  def main(args: Array[String]): Unit = {
    val files = new File("./resized/").listFiles
    val output = new File("./flattened.out")
    val bw = new BufferedWriter(new FileWriter(output))
    var prevChar = 'C'
    util.Arrays.parallelSort(files)
    for (file <- files) {
      if (file.getName.charAt(0) != prevChar) {
        bw.write(file.getName.charAt(0) + "\n")
        prevChar = file.getName.charAt(0)
      }
      if (file.isFile) {
        System.out.println("Processing image: " + file.getName)
        val image = ImageIO.read(file)
        for (y <- 2 until (image.getHeight - 2)) {
          for (x <- 2 until (image.getWidth - 2)) {
            if (Math.abs(image.getRGB(x, y)) > 1) bw.write('1')
            else bw.write('0')
          }
        }
        image.flush()
        bw.write('\n')
      }
      bw.flush()
    }
    bw.write('\0')
    bw.close()
    System.out.println("Image flattening complete")
  }
}