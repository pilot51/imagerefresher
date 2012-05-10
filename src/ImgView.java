import java.awt.Graphics;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JComponent;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;

public class ImgView extends JComponent {
	private static final long serialVersionUID = 1L;
	private String address, user, pass;
	protected Image img;
	protected int scaledWidth, scaledHeight;
	private Thread thread;
	private Scanner in = new Scanner(System.in);
	private HttpClient httpclient;
	private HttpGet request;
	private HttpResponse response;

	public ImgView() {
		System.out.println("Address?");
		address = in.nextLine();
		System.out.println("Username?");
		user = in.nextLine();
		System.out.println("Password?");
		pass = in.nextLine();
		setRequest();
		run();
	}
	
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (img == null) return;
		else if (scaledWidth == 0 | scaledHeight == 0) {
			scaledWidth = getSize().width;
			scaledHeight = getSize().height;
		}
		g.drawImage(img, 0, 0, scaledWidth, scaledHeight, null);
	}
	
	protected void aspect(int width, int height) {
		float ratio = (float)img.getWidth(null) / (float)img.getHeight(null);
		if (width > height * ratio) {
			scaledWidth = Math.round(height * ratio);
			scaledHeight = height;
		} else if (height > width / ratio) {
			scaledWidth = width;
			scaledHeight = Math.round(width / ratio);
		}
	}
	
	private void run() {
		if (address == null || thread != null && thread.isAlive()) return;
		thread = new Thread(new Runnable() {
			@Override
			public void run() {
				InputStream is;
				while (Main.isRunning) {
					is = fetch();
					if (is != null) {
						try {
							img = ImageIO.read(is);
						} catch (IOException e) {
							e.printStackTrace();
						}
						repaint();
					}
				}
			}
		});
		thread.start();
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (Main.isRunning) {
					try {
						audio();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}
	
	private void audio() throws Exception {
		AudioInputStream stream = AudioSystem.getAudioInputStream(new URL(address + "/audio.cgi"));
		AudioFormat format = stream.getFormat();
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
		SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
		line.open(format);
		line.start();
		int numRead = 0;
		byte[] buf = new byte[line.getBufferSize()];
		while ((numRead = stream.read(buf, 0, buf.length)) >= 0) {
			int offset = 0;
			while (offset < numRead) {
				offset += line.write(buf, offset, numRead - offset);
			}
		}
		line.drain();
		line.stop();
	}
	
	private void setRequest() {
		if (httpclient == null) {
			httpclient = new DefaultHttpClient();
			httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
		}
		request = new HttpGet(address + "/image.jpg");
		request.setHeader("User-Agent", "ImageRefresher");
		if (user != null | pass != null) {
			try {
				request.addHeader(new BasicScheme().authenticate(
					new UsernamePasswordCredentials(user, pass), request));
			} catch (AuthenticationException e) {
				e.printStackTrace();
			}
		}
	}
	
	private InputStream fetch() {
		try {
			response = httpclient.execute(request);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
				return response.getEntity().getContent();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			try {
				EntityUtils.consume(response.getEntity());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
		return null;
	}
}