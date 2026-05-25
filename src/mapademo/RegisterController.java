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
import javafx.application.Platform;
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
    @FXML
    private Label user_err;
    @FXML
    private Label pass_err;
    @FXML
    private Label email_err;
    @FXML
    private Label birth_err;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
        
                    String imagePath = getClass().getResource("/resources/default_avatar.png").toExternalForm();
        
                    Image defaultAvatar = new Image(imagePath);
                    avatarPreview.setImage(defaultAvatar);
                }catch (Exception e) {
                    System.out.println("ERROR");
                }
        register.setOnAction(this::handleRegister);
        cancelReg.setOnAction(this::cancel);
        avatarSel.setOnAction(this::avatar);
        
        Platform.runLater(() -> {
        Stage stage = (Stage) avatarSel.getScene().getWindow();
        stage.setWidth(430);
        stage.setHeight(440);
        stage.setMinWidth(430);
        stage.setMinHeight(440);
    });
    }    
    
     private void handleRegister(ActionEvent event) {
        ErrorReg.setText("");
        user_err.setText("");
        pass_err.setText("");
        email_err.setText("");
        birth_err.setText("");
        
        String password = passwordReg.getText();
        String username = userReg.getText();
        String emailUser = email.getText();
        LocalDate birthD = birthDate.getValue();
        
        if(password.isEmpty() || username.isEmpty() || emailUser.isEmpty() || birthD == null){
            ErrorReg.setText("Please fill all mandatory fields");
            return;
        }

        boolean hasError = false;
        
        if(!User.checkNickName(username)){
            user_err.setText("Nick must be 6-15 characters, letters, digits, hyphen or underscore only");
            hasError = true;
        }
        
        if(!User.checkPassword(password)){
            pass_err.setText("8 to 20 characters, with at least one uppercase & lowercase letter, one digit and one symbol");
            hasError = true;
        }
        
        if(!User.checkEmail(emailUser)){
            email_err.setText("Use valid user@domain format");
            hasError = true;
        }
        
        if(!User.isOlderThan(birthD, 12)){
            birth_err.setText("The user must be at least 12 years old");
            hasError = true;
        }
        
        if (hasError) return;

        SportActivityApp app = SportActivityApp.getInstance();
        boolean reg = app.registerUser(username, emailUser, password, birthD, rutaAvatar);
         
        if(reg){
            ErrorReg.setText("Successful operation");
            try{
                Parent mapaRoot = FXMLLoader.load(getClass().getResource("logIn.fxml"));
                Stage stage = (Stage) register.getScene().getWindow();
                Scene scene = new Scene(mapaRoot);
                stage.setScene(scene);
                stage.show();
            } catch(IOException e){
                System.out.println("Error while loading main screen");
            }
        } else {
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
         }
         
    }
    
}
