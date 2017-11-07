import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

public class Main extends JFrame {
	private static final long serialVersionUID = 1L;
	private final JMenuBar menuBar = new JMenuBar();
	private String address, user, pass;
	private Thread thread;
	private ImgView imgView;
	private boolean isRunning = true;
	
	public static void main(String[] args) {
		new Main();
	}

	private Main() {
		super("ImageRefresher");
		createMenu();
		loadConfig();
		imgView = new ImgView();
		if (address != null) {
			setAuth();
			streamCamera();
		}
		add(imgView);
		setJMenuBar(menuBar);
		addComponentListener(new ResizeListener());
		setSize(642, 510);
		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				isRunning = false;
			}
		});
	}
	
	private class ResizeListener extends ComponentAdapter {
		public void componentResized(ComponentEvent e) {
			if (imgView.img == null) return;
			imgView.fitToSize(getWidth() - 2, getHeight() - 30);
			setSize(imgView.scaledWidth + 2, imgView.scaledHeight + 30);
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
				String addr = (String)JOptionPane.showInputDialog(Main.this, "Address", "Connect", JOptionPane.QUESTION_MESSAGE);
				if (addr != null) {
					address = addr;
					user = (String)JOptionPane.showInputDialog(Main.this, "Login: Username", "Connect", JOptionPane.QUESTION_MESSAGE);
					pass = (String)JOptionPane.showInputDialog(Main.this, "Login: Password", "Connect", JOptionPane.QUESTION_MESSAGE);
					saveConfig();
					setAuth();
					streamCamera();
				}
			}
		});
		menu.add(menuItem);
		menuBar.add(menu);
	}
	
	private void setAuth() {
		if (user != null | pass != null) {
			Authenticator.setDefault(new Auth(user, pass));
		}
	}
	
	private void streamCamera() {
		if (address == null || thread != null && thread.isAlive()) return;
		thread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (isRunning) {
					try {
						imgView.displayImage(new URL(address + "/image.jpg"));
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
				}
			}
		});
		thread.start();
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (isRunning) {
					playAudioStream();
				}
			}
		}).start();
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
	
	private void saveConfig() {
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
	
	private void loadConfig() {
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
