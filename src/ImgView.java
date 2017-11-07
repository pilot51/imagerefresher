import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

public class ImgView extends JComponent {
	private static final long serialVersionUID = 1L;
	protected JMenuBar menuBar = new JMenuBar();
	private String address, user, pass;
	protected Image img;
	protected int scaledWidth, scaledHeight;
	private Thread thread;

	public ImgView() {
		createMenu();
		load();
		if (address != null) {
			setAuth();
			run();
		}
	}
	
	private void createMenu() {
		JMenu menu;
		JMenuItem menuItem;
		menu = new JMenu("Connect");
		menu.setMnemonic(KeyEvent.VK_C);
		menuItem = new JMenuItem("New", KeyEvent.VK_N);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, 0));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String addr = (String)JOptionPane.showInputDialog(ImgView.this, "Address", "Connect", JOptionPane.QUESTION_MESSAGE);
				if (addr != null) {
					address = addr;
					user = (String)JOptionPane.showInputDialog(ImgView.this, "Login: Username", "Connect", JOptionPane.QUESTION_MESSAGE);
					pass = (String)JOptionPane.showInputDialog(ImgView.this, "Login: Password", "Connect", JOptionPane.QUESTION_MESSAGE);
					save();
					setAuth();
					run();
				}
			}
		});
		menu.add(menuItem);
		menuBar.add(menu);
	}
	
	protected void paintComponent(Graphics g) {
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
				while (Main.isRunning) {
					displayImage();
				}
			}
		});
		thread.start();
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (Main.isRunning) {
					playAudioStream();
				}
			}
		}).start();
	}
	
	private void setAuth() {
		if (user != null | pass != null) {
			Authenticator.setDefault(new Auth(user, pass));
		}
	}
	
	private void displayImage() {
		try {
			img = ImageIO.read(new URL(address + "/image.jpg"));
			repaint();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void playAudioStream() {
		SourceDataLine line = null;
		try {
			AudioInputStream stream = AudioSystem.getAudioInputStream(new URL(address + "/audio.cgi"));
			AudioFormat format = stream.getFormat();
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
			line = (SourceDataLine)AudioSystem.getLine(info);
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
		} catch (UnsupportedAudioFileException | LineUnavailableException | IOException e) {
			e.printStackTrace();
		}
		if (line != null) {
			line.drain();
			line.stop();
		}
	}
	
	private void save() {
		try {
			ObjectOutputStream save = new ObjectOutputStream(new FileOutputStream("imgrefresher.sav"));
			save.writeObject(address);
			save.writeObject(user);
			save.writeObject(pass);
			save.flush();
			save.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void load() {
		try {
			ObjectInputStream save = new ObjectInputStream(new FileInputStream("imgrefresher.sav"));
			address = (String)save.readObject();
			user = (String)save.readObject();
			pass = (String)save.readObject();
			save.close();
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
	}
	
	private static class Auth extends Authenticator {
		private String username, password;
		
		private Auth(String user, String pass) {
			username = user;
			password = pass;
		}
		
		protected PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(username, password.toCharArray());
		}
	}
}
