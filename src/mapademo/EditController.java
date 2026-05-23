/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package mapademo;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.User;

/**
 * FXML Controller class
 *
 * @author Jack
 */
public class EditController implements Initializable {

    private String rutaAvatar=null;
    
    @FXML
    private TextField email;
    @FXML
    private DatePicker birthD;
    @FXML
    private TextField password;
    @FXML
    private ImageView imageAv;
    @FXML
    private Button selAvatar;
    @FXML
    private Button confirm;
    @FXML
    private Button cancel;
    @FXML
    private Label errPass;
    @FXML
    private Label errEmail;
    @FXML
    private Label errBDay;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        SportActivityApp app= SportActivityApp.getInstance();
        Image image=app.getCurrentUser().getAvatar();
        
        imageAv.setImage(image);
        
        cancel.setOnAction(this::cancelEdit);
        selAvatar.setOnAction(this::avatar);
        confirm.setOnAction(this::con);
        
        
    }    

    @FXML
    private void initialize(ActionEvent event) {
    }
    
    private void cancelEdit(ActionEvent event){
        try{
            //load the map if successful
        Parent mapaRoot = FXMLLoader.load(getClass().getResource("Menu.fxml"));
            //get the stage from the button
        Stage stage = (Stage) cancel.getScene().getWindow();    
            //change scene
        Scene scene = new Scene(mapaRoot);
        stage.setScene(scene);
        stage.show();
        }catch(IOException e){
        System.out.println("Error while loading");
        e.printStackTrace();
        }
    }
    
    private void avatar(ActionEvent event){
         FileChooser fileChooser=new FileChooser();
         fileChooser.setTitle("Select profile picture");
         
         fileChooser.getExtensionFilters().addAll(
         new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));
        
         //selection window
         File archivoSeleccionado = fileChooser.showOpenDialog(null);
        
         if (archivoSeleccionado != null) {
        // Guardamos la ruta absoluta que pide la librería
        rutaAvatar = archivoSeleccionado.getAbsolutePath();
        
        // Opcional: mostrar la imagen en el ImageView para que quede bonito
        Image image = new Image(archivoSeleccionado.toURI().toString());
        imageAv.setImage(image);
        }
     }
    
    private void con(ActionEvent event){
        
        SportActivityApp app=SportActivityApp.getInstance();
        User currentUser=app.getCurrentUser();
        
        String newPassword=password.getText();
        
        if(newPassword.isEmpty()){
        newPassword=currentUser.getPassword();
        }else if(!User.checkPassword(newPassword)){
        errPass.setText("Invalid Password");
        return;
        }
        
        String newEmail=email.getText();
        
        if(newEmail.isEmpty()){
        newEmail=currentUser.getEmail();
        }else if(!User.checkEmail(newEmail)){
        errEmail.setText("Invalid Email");
        return;
        }
        
        LocalDate newBDay=birthD.getValue();
        if(newBDay==null){
        newBDay = currentUser.getBirthDate();
        }else if (!User.isOlderThan(newBDay, 12)) {
        errBDay.setText("You must be ove 12 years old");
        return;
        }
        
        String newAv=rutaAvatar;
        if(newAv==null){
        newAv = currentUser.getAvatarPath();
        }
        
        app.updateCurrentUser(newEmail, newPassword, newBDay, newAv);
        
        cancelEdit(event);
    }

    
}
