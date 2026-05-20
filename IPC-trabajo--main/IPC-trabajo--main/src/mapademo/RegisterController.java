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
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.swing.plaf.FileChooserUI;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.User;

/**
 * FXML Controller class
 *
 * @author Jack
 */
public class RegisterController implements Initializable {
    private String rutaAvatar = null;
    
    
    @FXML
    private TextField userReg;
    @FXML
    private PasswordField passwordReg;
    @FXML
    private DatePicker birthDate;
    @FXML
    private Button register;
    @FXML
    private Button cancelReg;
    @FXML
    private Label ErrorReg;
    @FXML
    private TextField email;
    @FXML
    private Button avatarSel;
    @FXML
    private ImageView avatarPreview;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        register.setOnAction(this::handleRegister);
        cancelReg.setOnAction(this::cancel);
        avatarSel.setOnAction(this::avatar);
    }    
    
     private void handleRegister(ActionEvent event) {
        String password=passwordReg.getText();
        String username=userReg.getText();
        String emailUser=email.getText();
        LocalDate birthD=birthDate.getValue();
        
        if(password.isEmpty()||username.isEmpty()||emailUser.isEmpty()||birthD==null){
        ErrorReg.setText("Please fill all mandatory fields");
        return;
        }
        
        if(!User.checkNickName(username)){
        ErrorReg.setText("Invalid Nickname");
        return;
        }
        
        
        if(!User.checkPassword(password)){
        ErrorReg.setText("Invalid password");
        return;
        }
        
        if(!User.checkEmail(emailUser)){
        ErrorReg.setText("Invalid email");
        return;
        }
        
        if(!User.isOlderThan(birthD,12)){
        ErrorReg.setText("Must be at least 12");
        return;
        }
        
         SportActivityApp app=SportActivityApp.getInstance();
         
         boolean reg=app.registerUser(username, emailUser, password, birthD, rutaAvatar);
         
         if(reg){
         ErrorReg.setText("Successful operation");
         try{
             //load the log in screen 
             Parent mapaRoot = FXMLLoader.load(getClass().getResource("logIn.fxml"));
             //get the stage from the button
             Stage stage = (Stage) register.getScene().getWindow();
             //change scene
            Scene scene = new Scene(mapaRoot);
            stage.setScene(scene);
            stage.show();
         }catch(IOException e){
         System.out.println("Error while laoding main screen");
         e.printStackTrace();
         }
         }else{
         ErrorReg.setText("Problem with registration");
         }
     }
     
     private void avatar(ActionEvent event) {
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
        avatarPreview.setImage(image);
        }
     }

    @FXML
    private void initialize(ActionEvent event) {
    }
    
    private void cancel(ActionEvent event){
    try{
             //load the log in screen 
             Parent mapaRoot = FXMLLoader.load(getClass().getResource("logIn.fxml"));
             //get the stage from the button
             Stage stage = (Stage) register.getScene().getWindow();
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
