import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.EventQueue;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class Main {
    private static final int DEPTH = 255;
    private static final int WIDTH = 1921;
    private static final int HEIGHT = 1201;
    private static final double REAL_RANGE = 3.2;
    private static double screenY = -1.0;
    private static double screenX = -2.2;
    private static double zoom = 1.0 / (WIDTH / REAL_RANGE);
    private static BufferedImage image;
    private static JFrame frame;
    private static Executor executor = Executors.newSingleThreadExecutor();

    public static void main(String[] args) {

        EventQueue.invokeLater(() -> {
            System.out.println("Welcome " + System.getProperty("user.name"));

            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();

            image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
            JLabel label = new JLabel(new ImageIcon(image));
            label.addMouseListener(mouseListener());
            frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.getContentPane().add(label);
            frame.setUndecorated(true);
            frame.setVisible(true);
            frame.pack();
            frame.setLocationRelativeTo(null);
            gd.setFullScreenWindow(frame);
            computeMandelbrot();
            try {
                saveMandelbrot();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static MouseListener mouseListener() {
        return new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent event) {
                if (event.getButton() == MouseEvent.BUTTON1) {
                    // zoom in 2x
                    screenX = (event.getX() - WIDTH / 4.0) * zoom + screenX;
                    screenY = (event.getY() - HEIGHT / 4.0) * zoom + screenY;
                    zoom = zoom / 2.0;
                } else {
                    // zoom out 2x
                    screenX = (event.getX() - WIDTH) * zoom + screenX;
                    screenY = (event.getY() - HEIGHT) * zoom + screenY;
                    zoom = zoom * 2.0;
                }

                computeMandelbrot();
                try {
                    saveMandelbrot();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
    }

    public static void computeMandelbrot() {
        executor.execute(() -> {
            long start = System.nanoTime();
            IntStream.range(0, HEIGHT).parallel().forEach(y -> {

                double ci = y * zoom + screenY;

                for (int x = 0; x < WIDTH; x++) {

                    double cr = x * zoom + screenX;
                    double zi = 0, zr = 0;

                    int iterations = 0;

                    while (iterations++ < DEPTH) {
                        double zizi = zi * zi;
                        double zrzr = zr * zr;

                        if (zizi * zrzr <= 4) {
                            zi = (zi * zr + zi * zr) + ci;
                            zr = zrzr - zizi + cr;
                        } else {
                            break;
                        }

                    }
                    if (iterations >= DEPTH) {
                        image.setRGB(x, y, 0x000000FF);
                    } else {
                        image.setRGB(x, y, 0xFAFAFAFA * iterations);
                    }
                }
                frame.repaint();
            });
            long duration = System.nanoTime() - start;
            try {
                saveMandelbrot();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Drawtime: " + duration / 1000000 + "ms");
        });
    }

    public static void saveMandelbrot() throws IOException {
        File output = new File("pic.png");
        ImageIO.write(image, "png", output);
    }

}