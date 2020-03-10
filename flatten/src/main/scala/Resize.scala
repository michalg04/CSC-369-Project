import java.awt.{AlphaComposite, Image, RenderingHints}
import java.awt.image.BufferedImage
import java.io.{File, IOException}
import java.util

import javax.imageio.ImageIO


object Resize {
  @throws[IOException]
  def main(args: Array[String]): Unit = {
    var i = 0
    var j = 0
    var prevChar = '\0'
    var currentChar = '0'
    val folder = new File("./../images/")
    val listOfFiles = folder.listFiles
    util.Arrays.parallelSort(listOfFiles)
    System.out.println("Total No. of Files:" + listOfFiles.length)
    i = 0
    while ( {
      i < listOfFiles.length
    }) {
      if (listOfFiles(i).isFile) {
        currentChar = listOfFiles(i).getName.charAt(0)
        if (currentChar != prevChar) {
          j = 0
          prevChar = currentChar
        }

        System.out.println("Resizing file:" + listOfFiles(i).getName)
        var img = ImageIO.read(new File("./../images/" + listOfFiles(i).getName))
        var tempPNG = resizeImage(img, 32, 32)
        var newFilePNG = new File("./resized/" + prevChar + "_" + j.toString.format("%04d") + ".png")
        ImageIO.write(tempPNG, "png", newFilePNG)
        j += 1
      }

      i += 1
    }
    System.out.println("Resize job complete.")
  }

  /**
   * This function resize the image file and returns the BufferedImage object that can be saved to file system.
   */
  def resizeImage(image: Image, width: Int, height: Int): BufferedImage = {
    val bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val graphics2D = bufferedImage.createGraphics
    graphics2D.setComposite(AlphaComposite.Src)
    //below three lines are for RenderingHints for better image quality at cost of higher processing time
    graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    graphics2D.drawImage(image, 0, 0, width, height, null)
    graphics2D.dispose()
    bufferedImage
  }
}