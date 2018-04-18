import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;


public class MainControler implements Initializable {
	@FXML Button startConvertionButton;
	@FXML Button saveSelectButton;
	@FXML Button imageSelectButton;
	@FXML Button convertionTypeSelectButton;
	@FXML Label timeLabel;
	@FXML Label convertionLabel;
	
	@FXML TableView mainTableView;
	@FXML TableColumn<ImageProcessingJob, String> imageNameCol;
	@FXML TableColumn<ImageProcessingJob, Double> progressCol;
	@FXML TableColumn<ImageProcessingJob, String> statusCol;
	
	File saveDirectory;
	ObservableList<ImageProcessingJob> convertions;
	ExecutorService executor;
	String stringPom;
	int executorType;
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		imageNameCol.setCellValueFactory(fac -> new SimpleStringProperty(fac.getValue().getFile().getName()));
		progressCol.setCellFactory(ProgressBarTableCell.<ImageProcessingJob>forTableColumn());
		progressCol.setCellValueFactory(fac -> fac.getValue().progressProperty().asObject());
		statusCol.setCellValueFactory(fac -> fac.getValue().messageProperty());
		
		convertions = FXCollections.observableList(new ArrayList<>());
		mainTableView.setItems(convertions);
		saveDirectory = null;
		stringPom = "";
		executorType = 1;
		setExecutor();
	}
	
	@FXML void startConvertion(ActionEvent event) {
		if(!convertions.isEmpty()) {
			if(saveDirectory != null) {
				startConvertionButton.setDisable(true);
				saveSelectButton.setDisable(true);
				imageSelectButton.setDisable(true);
				convertionTypeSelectButton.setDisable(true);
				new Thread(this::ConvertImages).start();
				
			} else {
			    Alert alert = new Alert(AlertType.ERROR);
	            alert.setHeaderText("MissingDirectory");
	            alert.setContentText("Add directory as a place for convrted images.\nUse Select Save Directory Button to add one.");
	            alert.showAndWait();
	            return;
			}
		} else{
			 Alert alert = new Alert(AlertType.ERROR);
			 alert.setHeaderText("MissingItems");
	         alert.setContentText("Add pictures to convert.\nUse Select Image To Convert Button to add ones.");
	         alert.showAndWait();
	         return;
		}
	}
	
	@FXML
	private void selectSavePlace(ActionEvent event) {
		Window stage = ((Node)event.getSource()).getScene().getWindow();
		saveDirectory = new DirectoryChooser().showDialog(stage);
	}
	
	@FXML
	private void selectImageToConvert(ActionEvent event) {
		Window stage = ((Node)event.getSource()).getScene().getWindow();
		FileChooser chooser = new FileChooser();
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JPG images", "*.jpg"));
		
		List<File> files = chooser.showOpenMultipleDialog(stage);
		if(files == null) return;
		else if(files.isEmpty()) return;
		
		convertions.clear();
		files.stream().forEach(fileL -> {
			convertions.add(new ImageProcessingJob(fileL));
		});
	}
	
	@FXML
	private void selectConvertionType(ActionEvent event) {
		
		UnaryOperator<Change> filter = change -> {
			stringPom = stringPom + (change.getText() != null ? change.getText() : "");
			if(change.isDeleted())
				if(stringPom.length()>0) {
					stringPom = stringPom.substring(0, stringPom.length()-1);
					return change;
				}
		    if (stringPom.matches("^(1[0-6]|[0-9])$")) {
				return change;
		    } else if(stringPom.length()>0) {
		    	stringPom = stringPom.substring(0, stringPom.length()-1);
			    return null;
			}
		    return null;
		};
		TextField numberFeild = new TextField();
		numberFeild.setTextFormatter(new TextFormatter<>(filter));
		
		VBox choiseBox = new VBox();
		choiseBox.getChildren().add(numberFeild);
		choiseBox.setMaxWidth(320);
		choiseBox.setMinWidth(320);
		choiseBox.setMaxHeight(40);
		choiseBox.setMinHeight(40);
		
		Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setHeaderText("0           - Common Pool\n(1 - 16) - Thread Pool");
        alert.getDialogPane().setContent(choiseBox);
        alert.showAndWait();
        
        if(!(stringPom = numberFeild.getText()).isEmpty())
        	executorType = Integer.parseInt(numberFeild.getText());
        stringPom = "";
        setExecutor();
    }
	
	private void ConvertImages() {
		long start = System.currentTimeMillis();
		Platform.runLater(() -> timeLabel.setText("Convertion Time: Counting"));
		convertions.stream().forEach(convertion -> {
			convertion.setSaveDirectory(saveDirectory);
			executor.submit(convertion);
		});
		
		try {
			executor.shutdown();
			executor.awaitTermination(1, TimeUnit.HOURS);	// til end of convertions or one hour
			long stop = System.currentTimeMillis() - start;
			
			Platform.runLater(() -> { 
				timeLabel.setText("Convertion Time: " + stop + "ms");
				setExecutor();
				startConvertionButton.setDisable(false);
				saveSelectButton.setDisable(false);
				imageSelectButton.setDisable(false);
				convertionTypeSelectButton.setDisable(false);
			});
		} catch(InterruptedException e) {
			Alert alert = new Alert(AlertType.ERROR);
			 alert.setHeaderText("MissingItems");
	         alert.setContentText("Expected time passed, runn app again");
	         alert.showAndWait();
	         return;
	    	}
	}
	
	private void setExecutor() {
		if(executorType == 0) {
			executor = new ForkJoinPool();
			convertionLabel.setText("Common Pool");
		}
		else {
			executor = Executors.newFixedThreadPool(executorType);
			convertionLabel.setText("Threads: " + executorType);
			
		}
	}

}
