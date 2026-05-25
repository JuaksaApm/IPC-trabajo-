/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package mapademo;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.User;
import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.MapRegion;
/**
 * FXML Controller class
 *
 * @author estau
 */
public class UploadMapController implements Initializable {
    
    @FXML
    private Label NameFile;
    @FXML
    private Label errMinLat;
    @FXML
    private Label errMinLon;
    @FXML
    private Label errMaxLat;
    @FXML
    private Label errMaxLon;
    @FXML
    private TextField MaxLat;
    @FXML
    private TextField MaxLon;
    @FXML
    private TextField MinLat;
    @FXML
    private TextField MinLon;
    @FXML
    private Button CancelBut;
    @FXML
    private Button SelectJPG;
    @FXML
    private Button UploadMapBut;
    
    
    private File currFile;
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        CancelBut.setOnAction(this::handleCancel);
        SelectJPG.setOnAction(this::handleSelect);
        UploadMapBut.setOnAction(this::handleLoad);
    }    
    private void handleCancel(ActionEvent event){
        gotoMainMenu();
    }
    private void gotoMainMenu(){
        try {
            Parent mapaRoot = FXMLLoader.load(getClass().getResource("Menu.fxml"));
            Stage stage = (Stage) CancelBut.getScene().getWindow();
            stage.setScene(new Scene(mapaRoot));
            stage.show();
        } catch (IOException e) {
            System.out.println("Error while loading Menu screen");
            e.printStackTrace();
        }
    }
    private void handleSelect(ActionEvent event){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import JPG Map");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("JPG", "*.jpg")
        );
        Button sourceButton = (Button) event.getSource();
        Stage currentStage = (Stage) sourceButton.getScene().getWindow();
        File selectedJPG = fileChooser.showOpenDialog(currentStage);
        if(selectedJPG == null)return;
        currFile = selectedJPG;
        NameFile.setText(currFile.getName());
    }
    private void handleLoad(ActionEvent event){
        errMaxLat.setText("");
        errMinLat.setText("");
        errMaxLon.setText("");
        errMinLon.setText("");
        NameFile.setText("");
        boolean failed = false;
        double latMin=0;
        double latMax=0;
        double lonMin=0;
        double lonMax=0;
        
        if(MinLat.getText()==null){
            errMinLat.setText("Must input minimum latitude.");
            failed = true;
        }else{
           try{
               latMin = Double.parseDouble(MinLat.getText());
           } catch(NumberFormatException e){
                errMinLat.setText("Must input valid latitude.");
                                failed =true;

           }
        }
        if(MaxLat.getText()==null){
            errMaxLat.setText("Must input maximum latitude.");
                        failed = true;

        }else{
           try{
               latMax = Double.parseDouble(MaxLat.getText());
           } catch(NumberFormatException e){
                errMaxLat.setText("Must input valid latitude.");
                           failed =true;

           }
        }
        if(MinLon.getText()==null){
            errMinLon.setText("Must input minimum longitude.");
                        failed = true;


        }else{
           try{
               lonMin = Double.parseDouble(MinLon.getText());
           } catch(NumberFormatException e){
                errMinLon.setText("Must input valid longitude.");
                                failed =true;

           }
        }
        if(MaxLon.getText()==null){
            errMaxLon.setText("Must input maximum longitude.");
                        failed = true;

        }else{
           try{
               lonMax = Double.parseDouble(MaxLon.getText());
           } catch(NumberFormatException e){
                errMaxLon.setText("Must input valid longitude.");
                failed =true;
           }
        }
        if(currFile == null){
            NameFile.setText("Must input file of type .jpg.");
            failed = true;
        }
        
        if(failed)return;
        SportActivityApp app = SportActivityApp.getInstance();
        app.addMapRegion(currFile.getName(), currFile, latMin, latMax, lonMin, lonMax);
        gotoMainMenu();
    }
}
