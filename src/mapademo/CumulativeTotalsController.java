package mapademo;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;
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
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        goBack.setOnAction(this::handleGoBack);
        SportActivityApp app = SportActivityApp.getInstance();
        List<Activity> list = app.getActivitiesByUser(app.getCurrentUser());
        
        double asc = 0.0;
        double timeSeconds = 0.0;
        double dist = 0.0;
        double des = 0.0;
        
        // Boundaries for the current month to satisfy Scenario 4.4
        LocalDateTime curr = LocalDateTime.now(ZoneId.of("Europe/Madrid"));
        LocalDateTime bef = curr.minusMonths(1);

        for(Activity a : list) {
            var a_time = a.getEndTime();
            
            // Protect against NPEs for bad GPX headers AND enforce the 1-month boundary
            if (a.getDuration() == null || a_time == null || a_time.compareTo(bef) < 0 || a_time.compareTo(curr) > 0) {
                continue; 
            }
            
            asc += a.getElevationGain();
            des += a.getElevationLoss();
            dist += a.getTotalDistance();
            timeSeconds += a.getDuration().getSeconds();
        }

        DecimalFormat df = new DecimalFormat("0.00");
        TotAsc.setText(df.format(asc) + " m.");
        TotDesc.setText(df.format(des) + " m.");
        CumDist.setText(df.format(dist / 1000) + " Km.");
        TotTime.setText(df.format(timeSeconds / 60.0) + " min.");
        
        Platform.runLater(() -> {
            Stage stage = (Stage) goBack.getScene().getWindow();
            if (stage != null) {
                stage.setMinWidth(630);
                stage.setMinHeight(240);
            }
        });
    }
    
    private void handleGoBack(ActionEvent event) {
        try {
            Parent mapaRoot = FXMLLoader.load(getClass().getResource("Menu.fxml"));
            Stage stage = (Stage) goBack.getScene().getWindow();
            stage.setScene(new Scene(mapaRoot));
            stage.show();
        } catch (IOException e) {
            System.out.println("Error while loading menu screen");
        }
    }
}