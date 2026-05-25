/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package mapademo;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import upv.ipc.sportlib.Session;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.User;

/**
 * FXML Controller class
 *
 * @author Jack
 */
public class HistoryController implements Initializable {

    @FXML
    private TableView<Session> table;
    @FXML
    private TableColumn<Session, String> colStart;
    @FXML
    private TableColumn<Session, String> colEnd;
    @FXML
    private TableColumn<Session, String> colDuration;
    @FXML
    private TableColumn<Session, Number> colImported;
    @FXML
    private TableColumn<Session, Number> colViewed;
    @FXML
    private TableColumn<Session, Number> colAnotations;
    @FXML
    private Label lblTotalDuration;
    @FXML
    private Label lblTotalImported;
    
    long totalMinutes = 0;
    int totalImported = 0;
    int totalViewed = 0;
    int totalAnnotations = 0;
    @FXML
    private Button menubutt;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        SportActivityApp app = SportActivityApp.getInstance();
        User currentUser = app.getCurrentUser();
        
        if(currentUser!=null){
        List<Session> sessions = app.getSessionsByUser(currentUser);
        
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    //we get all needed data for the table
    colStart.setCellValueFactory(cellData -> {
    String fechaFormateada = cellData.getValue().getStartTime().format(formatter);
    return new SimpleStringProperty(fechaFormateada);
    });

    colEnd.setCellValueFactory(cellData -> {
    String fechaFormateada = cellData.getValue().getEndTime().format(formatter);
    return new SimpleStringProperty(fechaFormateada);
});
    
    colDuration.setCellValueFactory(cellData -> 
    new SimpleStringProperty(cellData.getValue().getDuration().toMinutes() + " min")
    );
    
    colImported.setCellValueFactory(cellData -> 
    new SimpleIntegerProperty(cellData.getValue().getImportedActivities())
    );
    
    colViewed.setCellValueFactory(cellData -> 
        new SimpleIntegerProperty(cellData.getValue().getViewedActivities())
    );
    
    colAnotations.setCellValueFactory(cellData -> 
        new SimpleIntegerProperty(cellData.getValue().getAnnotationsCreated())
    );
    //we add the data
    table.getItems().addAll(sessions);

    //calculate step by step duration #mins etc
    for(Session s: sessions){
    totalMinutes += s.getDuration().toMinutes();
    totalImported += s.getImportedActivities();
    totalViewed += s.getViewedActivities();
    totalAnnotations += s.getAnnotationsCreated();
    }
        }
    
    lblTotalDuration.setText("Total time: " + totalMinutes + " min");
    lblTotalImported.setText("Total imported: " + totalImported);
    
    
    Platform.runLater(() -> {
        Stage stage = (Stage) menubutt.getScene().getWindow();
        stage.setMinWidth(615);
        stage.setMinHeight(440);
    });
    }    

    @FXML
    private void backToMenu(ActionEvent event) {
        try{
            //load screen 
        Parent loginRoot = FXMLLoader.load(getClass().getResource("Menu.fxml"));
            //get stage 
        Stage stage = (Stage) menubutt.getScene().getWindow();
            //change scene
        Scene scene = new Scene(loginRoot);
        stage.setScene(scene);
        stage.show();
        }catch(IOException e){}
    }
    
    
    
}
