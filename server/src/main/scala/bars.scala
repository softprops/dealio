package dealio

import java.io._
import java.awt.image.BufferedImage
import com.google.zxing._
import com.google.zxing.oned.Code128Writer
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode._
import javax.imageio._
import java.io.OutputStream

object Bars {
  def apply(txt: String, height: Int, width: Int)(os: OutputStream) = {
    val data = new String(txt.getBytes("utf-8"), "utf-8")
    val writer = new Code128Writer()
    val matrix: BitMatrix = writer.encode(
      data, BarcodeFormat.CODE_128, width, height
    )
    val (w, h) = (matrix.getWidth(), matrix.getHeight())
    val img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
    for (y <- 0 until h; x <- 0 until w) {
      img.setRGB(x, y, if(matrix.get(x, y)) 0 else 0xFFFFFF)
    }
    ImageIO.write(img, "png", os)
  }
}
