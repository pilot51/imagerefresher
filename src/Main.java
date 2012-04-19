import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

public class Main extends JFrame {
	private static final long serialVersionUID = 1L;
	private ImgView imgView;
	protected static boolean isRunning = true;
	
	public static void main(String[] args) {
		new Main();
	}

	Main() {
		super("ImageRefresher");
		imgView = new ImgView();
		add(imgView);
		setSize(642, 510);
		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
		    public void windowClosing(WindowEvent e) {
		        isRunning = false;
		    }
		});
	}
}