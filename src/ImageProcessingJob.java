import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;

public class ImageProcessingJob extends Task<Void> {
	protected File file;
	protected File saveDirectory;
	protected DoubleProperty progress;
	protected StringProperty status;
	
	public ImageProcessingJob(File file) {
		this.file = file;
		updateMessage("waiting for start");
	}
	
	public void setSaveDirectory(File saveDirectory) {
		this.saveDirectory = saveDirectory;
	}

	public File getFile() {
		return file;
	}
	
	@Override
	protected Void call() throws Exception {
		updateProgress(0, 1);
		updateMessage("still converting");
		try {
			BufferedImage orginal = ImageIO.read(file);
			BufferedImage grayscale = new BufferedImage(orginal.getWidth(),orginal.getHeight(),orginal.getType());
			
			for (int i = 0; i < orginal.getWidth(); i++) {
				for (int j = 0; j < orginal.getHeight(); j++) {
					int red = new Color(orginal.getRGB(i, j)).getRed();
					int green = new Color(orginal.getRGB(i, j)).getGreen();
					int blue = new Color(orginal.getRGB(i, j)).getBlue();
					int luminosity = (int) (0.21*red + 0.71*green + 0.07*blue);
					int newPixel = new Color(luminosity, luminosity, luminosity).getRGB();
					grayscale.setRGB(i, j, newPixel);
				}
				double progress = (1.0 + i) / orginal.getWidth();
				Platform.runLater(() -> updateProgress(progress, 1));
			}
			Path outputPath = Paths.get(saveDirectory.getAbsolutePath(), file.getName());
			ImageIO.write(grayscale, "jpg", outputPath.toFile());
		} catch (IOException ex) {
			updateMessage("something went wrong");
			throw new RuntimeException(ex);
		}
		updateProgress(1, 1);
		updateMessage("converted");
		return null;
	}

}
