/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package mapademo;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.input.MouseEvent;

/**
 * FXML Controller class
 *
 * @author Jack
 */
public class FXMLDocumentController implements Initializable {

    @FXML
    private Slider zoom_slider;
    @FXML
    private Label mousePosition;
    @FXML
    private SplitPane splitPane;
    @FXML
    private ListView<?> map_listview;
    @FXML
    private ScrollPane map_scrollpane;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }    

    @FXML
    private void about(ActionEvent event) {
    }

    @FXML
    private void cambiarMapa(ActionEvent event) {
    }

    @FXML
    private void zoomOut(ActionEvent event) {
    }

    @FXML
    private void zoomIn(ActionEvent event) {
    }

    @FXML
    private void listClicked(MouseEvent event) {
    }

    @FXML
    private void showPosition(MouseEvent event) {
    }
    
}
