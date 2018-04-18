import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;

public class main extends Application {

	@Override
	public void start(Stage stage) throws Exception {
		Parent root = FXMLLoader.load(getClass().getResource("MainLayaut.fxml"));
		
		Scene scene = new Scene(root);
		stage.setTitle("Image Converter");
		stage.setMinHeight(440);
		stage.setMaxHeight(440);
		stage.setMinWidth(655);
		stage.setMaxWidth(655);
		stage.setScene(scene);
		stage.show();
	}

	public static void main(String[] args) {
		launch(args);
	}
}
