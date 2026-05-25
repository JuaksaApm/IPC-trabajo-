/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package mapademo;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import upv.ipc.sportlib.SportActivityApp;

/**
 * FXML Controller class
 *
 * @author Jack
 */
public class LogInController implements Initializable {

    
    @FXML
    private Button clickLogIn;
    @FXML
    private PasswordField passwordIn;
    @FXML
    private TextField userIn;
    @FXML
    private Hyperlink regUser;
    @FXML
    private Label idkerr;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        clickLogIn.setOnAction(this::handleLogIn);
        regUser.setOnAction(this::goToReg);
        Platform.runLater(() -> {
        Stage stage = (Stage) clickLogIn.getScene().getWindow();
        stage.setWidth(285);
        stage.setHeight(420);
        stage.setMinWidth(285);
        stage.setMinHeight(420);
    });
    }    
    
    private void handleLogIn(ActionEvent event) {
        String password=passwordIn.getText();
        String username=userIn.getText();
        
        SportActivityApp app=SportActivityApp.getInstance();
        
        boolean SucessfulLogIn= app.login(username,password);
        if(SucessfulLogIn){
        System.out.println("¡Login exitoso!");
        try{
            //load the map if successful
        Parent mapaRoot = FXMLLoader.load(getClass().getResource("Menu.fxml"));
            //get the stage from the button
        Stage stage = (Stage) clickLogIn.getScene().getWindow();    
            //change scene
        Scene scene = new Scene(mapaRoot);
        stage.setScene(scene);
        stage.show();
        }catch(IOException e){
        System.out.println("error IOException en LogInController.java");
        }
        }else{
        idkerr.setText("Invalid nickname or password");
        passwordIn.clear();
        }
    }
    
    @FXML
    private void goToReg(ActionEvent event) {
       try{
            //load Reg screen 
        Parent mapaRoot = FXMLLoader.load(getClass().getResource("Register.fxml"));
            //get the stage from the button
        Stage stage = (Stage) regUser.getScene().getWindow();
            //change scene
        Scene scene = new Scene(mapaRoot);
        stage.setScene(scene);
        stage.show();
       }catch(IOException e){
       System.out.println("Error while loading map");
       }
    }

    
}
