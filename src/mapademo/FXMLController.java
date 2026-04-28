/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package mapademo;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.User;

/**
 * FXML Controller class
 *
 * @author Jack
 */
public class FXMLController implements Initializable {

    @FXML
    private Button logOut;
    @FXML
    private Button edit;
    @FXML
    private ImageView profilePic;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        logOut.setOnAction(this::logOut);
        edit.setOnAction(this::editProfile);
        
        //get the profile pic loaded to main menu
        SportActivityApp app=SportActivityApp.getInstance();
        
        User currentUser= app.getCurrentUser();
        
        if(currentUser!=null){
        Image avatar=currentUser.getAvatar();
        
        if(avatar!=null){
        profilePic.setImage(avatar);
        }else{}
        //end of profile pic code
        }
    }    

    @FXML
    private void initialize(ActionEvent event) {
    }
    
    private void logOut(ActionEvent event){
    try{
        //load the log in screen 
        Parent mapaRoot = FXMLLoader.load(getClass().getResource("logIn.fxml"));
        //get the stage from the button
        Stage stage = (Stage) logOut.getScene().getWindow();
        //change scene
        Scene scene = new Scene(mapaRoot);
        stage.setScene(scene);
        stage.show();
        }catch(IOException e){
        System.out.println("Error while laoding main screen");
        e.printStackTrace();
        }
    }
    
    private void editProfile(ActionEvent event){
        try{
        //load the log in screen 
        Parent mapaRoot = FXMLLoader.load(getClass().getResource("Edit.fxml"));
        //get the stage from the button
        Stage stage = (Stage) logOut.getScene().getWindow();
        //change scene
        Scene scene = new Scene(mapaRoot);
        stage.setScene(scene);
        stage.show();
        }catch(IOException e){
        System.out.println("Error while laoding main screen");
        e.printStackTrace();
        }
    }
}
