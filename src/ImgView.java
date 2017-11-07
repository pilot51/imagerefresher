/*
 * Copyright 2017 Mark Injerd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
