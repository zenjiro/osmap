import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

/**
 * OpenStreetMapのビューア
 */
public class Main {
	/**
	 * タイルの大きさ[px]
	 */
	public static int TILE_SIZE = 256;
	/**
	 * キャッシュファイルを置くディレクトリ
	 */
	public static String CACHE_DIRECTORY = "cache";
	/**
	 * 設定ファイルの名前
	 */
	public static String CONFIGURATION_FILE = "osmap.ini";
	/**
	 * 縮尺
	 */
	static int zoom;
	/**
	 * 列
	 */
	static int col;
	/**
	 * 行
	 */
	static int row;
	/**
	 * x座標
	 */
	static int offsetX;
	/**
	 * y座標
	 */
	static int offsetY;
	/**
	 * マウスドラッグが開始されたx座標
	 */
	static int lastX = 0;
	/**
	 * マウスドラッグが開始されたy座標
	 */
	static int lastY = 0;

	/**
	 * メインメソッド
	 * @param args コマンドライン引数
	 * @throws IOException 入出力例外
	 */
	public static void main(final String[] args) throws IOException {
		final Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(CONFIGURATION_FILE));
		} catch (final FileNotFoundException exception) {
			// do nothing
		}
		zoom = Integer.parseInt(properties.getProperty("zoom", "5"));
		col = Integer.parseInt(properties.getProperty("col", "27"));
		row = Integer.parseInt(properties.getProperty("row", "12"));
		offsetX = Integer.parseInt(properties.getProperty("offsetX", "20"));
		offsetY = Integer.parseInt(properties.getProperty("offsetY", "150"));
		final Map<String, Image> images = new HashMap<String, Image>();
		final JFrame frame = new JFrame("OSMap");
		final Timer timer = new Timer();
		frame.setLayout(new BorderLayout());
		final JLabel label = new JLabel(" ");
		final JPanel panel = new JPanel() {
			@Override
			protected void paintComponent(final Graphics graphics) {
				super.paintComponent(graphics);
				final Graphics2D g = (Graphics2D) graphics;
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				for (int i = -1; TILE_SIZE * i + offsetY < this.getHeight(); i++) {
					if (row + i < 0 || row + i >= 1 << zoom) {
						continue;
					}
					for (int j = -1; TILE_SIZE * j + offsetX < this.getWidth(); j++) {
						if (col + j < 0 || col + j >= 1 << zoom) {
							continue;
						}
						g.drawImage(images.get(zoom + "_" + (col + j) + "_" + (row + i)), TILE_SIZE * j + offsetX,
								TILE_SIZE * i + offsetY, this);
						g.drawRect(TILE_SIZE * j + offsetX, TILE_SIZE * i + offsetY, TILE_SIZE, TILE_SIZE);
						g.drawString(zoom + "_" + (col + j) + "_" + (row + i), TILE_SIZE * j + offsetX, TILE_SIZE * i
								+ offsetY + TILE_SIZE - 2);
					}
				}
				label.setText(new Formatter().format("zoom = %d, col = %d, row = %d, offsetX = %d, offsetY = %d\n",
						zoom, col, row, offsetX, offsetY).toString());
			}
		};
		frame.add(panel, BorderLayout.CENTER);
		frame.add(label, BorderLayout.SOUTH);
		panel.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(final MouseWheelEvent event) {
				final int clickedCol = (event.getX() - offsetX + TILE_SIZE) / TILE_SIZE - 1;
				final int clickedRow = (event.getY() - offsetY + TILE_SIZE) / TILE_SIZE - 1;
				final int diffX = (event.getX() - offsetX + TILE_SIZE) % TILE_SIZE;
				final int diffY = (event.getY() - offsetY + TILE_SIZE) % TILE_SIZE;
				if (event.getWheelRotation() < 0) {
					if (zoom < 18) {
						zoom++;
						col = col * 2 + clickedCol + (diffX < TILE_SIZE / 2 ? 0 : 1);
						row = row * 2 + clickedRow + (diffY < TILE_SIZE / 2 ? 0 : 1);
						offsetX = offsetX - diffX % TILE_SIZE + (diffX < TILE_SIZE / 2 ? 0 : TILE_SIZE);
						offsetY = offsetY - diffY % TILE_SIZE + (diffY < TILE_SIZE / 2 ? 0 : TILE_SIZE);
					}
				} else {
					if (zoom > 0) {
						zoom--;
						offsetX = offsetX + diffX / 2 - (col % 2 == 0 ? 0 : TILE_SIZE / 2) + clickedCol * TILE_SIZE / 2;
						offsetY = offsetY + diffY / 2 - (row % 2 == 0 ? 0 : TILE_SIZE / 2) + clickedRow * TILE_SIZE / 2;
						col /= 2;
						row /= 2;
					}
				}
				panel.repaint();
			}
		});
		panel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent event) {
				if (event.getButton() == MouseEvent.BUTTON1) {
				} else if (event.getButton() == MouseEvent.BUTTON3) {
				}
				panel.repaint();
			}

			@Override
			public void mousePressed(final MouseEvent event) {
				lastX = event.getX();
				lastY = event.getY();
			}
		});
		panel.addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseMoved(final MouseEvent event) {
			}

			@Override
			public void mouseDragged(final MouseEvent event) {
				offsetX += event.getX() - lastX;
				offsetY += event.getY() - lastY;
				lastX = event.getX();
				lastY = event.getY();
				panel.repaint();
			}
		});
		frame.setExtendedState(Integer.parseInt(properties.getProperty("frame.extendedState", "0")));
		frame.setSize(Integer.parseInt(properties.getProperty("frame.width", "800")),
				Integer.parseInt(properties.getProperty("frame.height", "600")));
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(final WindowEvent event) {
				timer.cancel();
				try {
					properties.put("zoom", Integer.toString(zoom));
					properties.put("col", Integer.toString(col));
					properties.put("row", Integer.toString(row));
					properties.put("offsetX", Integer.toString(offsetX));
					properties.put("offsetY", Integer.toString(offsetY));
					properties.put("frame.extendedState", Integer.toString(frame.getExtendedState()));
					if (frame.getExtendedState() == 0) {
						properties.put("frame.width", Integer.toString(frame.getWidth()));
						properties.put("frame.height", Integer.toString(frame.getHeight()));
					}
					properties.store(new FileWriter(CONFIGURATION_FILE), "OSMap configuration");
				} catch (final IOException exception) {
					exception.printStackTrace();
				}
			}
		});
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		JFrame.setDefaultLookAndFeelDecorated(true);
		frame.setLocationByPlatform(true);
		frame.setVisible(true);
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (offsetX < 0) {
					offsetX += TILE_SIZE;
					col++;
				} else if (offsetX >= TILE_SIZE) {
					offsetX -= TILE_SIZE;
					col--;
				}
				if (offsetY < 0) {
					offsetY += TILE_SIZE;
					row++;
				} else if (offsetY >= TILE_SIZE) {
					offsetY -= TILE_SIZE;
					row--;
				}
				final int zoom = Main.zoom;
				final int col = Main.col;
				final int row = Main.row;
				final File cacheDirectory = new File(CACHE_DIRECTORY);
				if (!cacheDirectory.isDirectory()) {
					cacheDirectory.mkdir();
				}
				for (int i = -1; TILE_SIZE * i + offsetY < panel.getHeight(); i++) {
					if (row + i < 0 || row + i >= 1 << zoom) {
						continue;
					}
					for (int j = -1; TILE_SIZE * j + offsetX < panel.getWidth(); j++) {
						if (col + j < 0 || col + j >= 1 << zoom) {
							continue;
						}
						if (!images.containsKey(zoom + "_" + (col + j) + "_" + (row + i))) {
							final File file = new File(new Formatter().format("%s%s%d_%d_%d.png", CACHE_DIRECTORY,
									File.separator, zoom, col + j, row + i).toString());
							try {
								if (!file.isFile()) {
									WebUtilities.copy(
											new URL(new Formatter().format(
													"http://tile.openstreetmap.org/%d/%d/%d.png", zoom, col + j,
													row + i).toString()).openStream(), new FileOutputStream(file));
								}
								images.put(zoom + "_" + (col + j) + "_" + (row + i), ImageIO.read(file));
								panel.repaint();
							} catch (final IIOException exception) {
								file.delete();
							} catch (final MalformedURLException exception) {
								exception.printStackTrace();
							} catch (final IOException exception) {
								exception.printStackTrace();
							}
						}
					}
				}
				panel.repaint();
			}
		}, 200, 200);
	}
}
