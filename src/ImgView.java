import java.awt.Graphics;
import java.awt.Image;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

public class ImgView extends JComponent {
	private static final long serialVersionUID = 1L;
	protected Image img;
	protected int scaledWidth, scaledHeight;
	
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (img == null) {
			return;
		} else if (scaledWidth == 0 | scaledHeight == 0) {
			scaledWidth = getSize().width;
			scaledHeight = getSize().height;
		}
		g.drawImage(img, 0, 0, scaledWidth, scaledHeight, null);
	}
	
	/**
	 * Sets the dimensions that the image must fit inside.
	 * It is scaled up or down with aspect ratio preserved.
	 */
	protected void fitToSize(int width, int height) {
		float ratio = (float)img.getWidth(null) / (float)img.getHeight(null);
		if (width > height * ratio) {
			scaledWidth = Math.round(height * ratio);
			scaledHeight = height;
		} else if (height > width / ratio) {
			scaledWidth = width;
			scaledHeight = Math.round(width / ratio);
		}
	}
	
	protected void displayImage(URL imgUrl) {
		try {
			img = ImageIO.read(imgUrl);
			repaint();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
