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
import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.chrono.ChronoLocalDateTime;
import java.util.List;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.SportActivityApp;

/**
 * FXML Controller class
 *
 * @author estau
 */
public class CumulativeTotalsController implements Initializable {

    @FXML
    private Label TotAsc;
    @FXML
    private Label TotDesc;
    @FXML
    private Label TotTime;
    @FXML
    private Label CumDist;
    @FXML
    private Button goBack;
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        goBack.setOnAction(this::handleGoBack);
        SportActivityApp app = SportActivityApp.getInstance();
        List<Activity> list = app.getActivitiesByUser(app.getCurrentUser());
        double asc = 0.0;
        double time = 0.0;
        double dist = 0.0;
        double des = 0.0;
        LocalDateTime curr = LocalDateTime.now(ZoneId.of("Europe/Madrid"));
        LocalDateTime bef = curr.minusMonths(1);
        for(Activity a : list){
            var a_time = a.getEndTime();
            if(a_time.compareTo(bef)<0||a_time.compareTo(curr)>0)continue;
            asc+=a.getElevationGain();
            des+=a.getElevationLoss();
            dist+=a.getTotalDistance();
            time+=a.getAveragePace()*a.getTotalDistance();
        }
        DecimalFormat df = new DecimalFormat("0.00");
        TotAsc.setText(df.format(asc)+" m.");
        TotDesc.setText(df.format(des)+" m.");
        CumDist.setText(df.format(dist/1000)+" Km.");
        TotTime.setText(df.format(time/1000)+" min.");
    }    
    private void handleGoBack(ActionEvent event){
        try {
            Parent mapaRoot = FXMLLoader.load(getClass().getResource("Menu.fxml"));
            Stage stage = (Stage) goBack.getScene().getWindow();
            stage.setScene(new Scene(mapaRoot));
            stage.show();
        } catch (IOException e) {
            System.out.println("Error while loading menu screen");
            e.printStackTrace();
        }
    }
}
