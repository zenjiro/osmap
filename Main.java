import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
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
	 * 縮尺
	 */
	static int zoom = 5;
	/**
	 * 列
	 */
	static int col = 27;
	/**
	 * 行
	 */
	static int row = 12;
	/**
	 * x座標
	 */
	static int offsetX = 20;
	/**
	 * y座標
	 */
	static int offsetY = 150;
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
		final Map<String, Image> images = new HashMap<String, Image>();
		final JFrame frame = new JFrame("OSMap");
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
		panel.setPreferredSize(new Dimension(640, 480));
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
		frame.pack();
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		JFrame.setDefaultLookAndFeelDecorated(true);
		frame.setLocationByPlatform(true);
		frame.setVisible(true);
		new java.util.Timer().scheduleAtFixedRate(new TimerTask() {
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
