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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.User;
import upv.ipc.sportlib.Activity;

/**
 * FXML Controller class for the Main Dashboard Menu.
 *
 * @author Jack
 */
public class MenuController implements Initializable {

    @FXML
    private Button logOut;
    @FXML
    private Button edit;
    @FXML
    private ImageView profilePic;
    @FXML
    private TableView<GpxActivity> activityTable;
    @FXML
    private TableColumn<GpxActivity, String> nameColumn;
    @FXML
    private TableColumn<GpxActivity, String> distanceColumn;
    @FXML
    private TableColumn<GpxActivity, String> timeColumn;
    @FXML
    private TableColumn<GpxActivity, String> dateColumn;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        logOut.setOnAction(this::logOut);
        edit.setOnAction(this::editProfile);
        
        // Load the profile pic to main menu
        SportActivityApp app = SportActivityApp.getInstance();
        User currentUser = app.getCurrentUser();
        
        if (currentUser != null) {
            Image avatar = currentUser.getAvatar();
            if (avatar != null) {
                profilePic.setImage(avatar);
            }
        }
        
        // Link table data column attributes to GpxActivity properties
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        distanceColumn.setCellValueFactory(new PropertyValueFactory<>("distance"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("time"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        
        // Automatically populate the TableView with historical data on launch 
        refreshMenuActivityTable();
        
        activityTable.setOnMouseClicked((MouseEvent event) -> {
            if (event.getClickCount() == 2) {
                GpxActivity selectedActivity = activityTable.getSelectionModel().getSelectedItem();
                if (selectedActivity != null) {
                    Activity activityData = null;
                    
                    // Match the row selection against database history to prevent duplicate files 
                    if (currentUser != null) {
                        for (Activity act : app.getUserActivities()) {
                            if (act.getName().equals(selectedActivity.getName())) {
                                activityData = act;
                                break;
                            }
                        }
                    }
                    
                    // Fallback to direct library parsing if it's a completely new file 
                    if (activityData == null && selectedActivity.getFile() != null) {
                        activityData = app.importActivity(selectedActivity.getFile());
                    }
                    
                    if (activityData != null) {
                        openGpxViewerScreen(activityData);
                    } else {
                        System.err.println("Could not parse or persist the GPX activity data.");
                    }
                }
            }
        });
    }    

    /**
     * Pulls historical activities from the SQLite database file, filters out duplicates,
     * and populates the visible TableView dashboard.
     */
    private void refreshMenuActivityTable() {
        SportActivityApp app = SportActivityApp.getInstance();
        activityTable.getItems().clear();
        
        if (app.getCurrentUser() != null) {
            // Extract the user activity tracking array list directly from database persistence models 
            for (Activity act : app.getUserActivities()) {
                
                // DUPLICATE GUARD: If this activity name is already visible in the table list, skip it!
                boolean alreadyExists = false;
                for (GpxActivity row : activityTable.getItems()) {
                    if (row.getName().equals(act.getName())) {
                        alreadyExists = true;
                        break;
                    }
                }
                
                if (alreadyExists) {
                    continue; // Skip this loop iteration to prevent visual duplicates
                }
                
                // Format total distance metrics from base meters back to readable UI strings
                double distKm = act.getTotalDistance() / 1000.0;
                String formattedDistance = String.format("%.2f km", distKm);
                
                // Format elapsed time duration models [cite: 209]
                long s = act.getDuration().getSeconds();
                String formattedTime = String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
                
                // Format starting date details cleanly using the localized timezone offset fix
                String formattedDate = "--/--/----";
                if (act.getStartTime() != null) {
                    Date startDate = Date.from(act.getStartTime().atZone(java.time.ZoneId.systemDefault()).toInstant());
                    formattedDate = new SimpleDateFormat("dd/MM/yyyy").format(startDate);
                }
                
                // Wrap data parameters and append row straight into TableView items list
                GpxActivity historicalRow = new GpxActivity(
                    act.getName(),
                    formattedDistance, 
                    formattedTime, 
                    formattedDate, 
                    null // File reference is safely stored inside SQLite
                );
                activityTable.getItems().add(historicalRow);
            }
        }
    }
    
    private void logOut(ActionEvent event) {
        try {
            SportActivityApp.getInstance().logout(); // Saves session statistics automatically to the SQLite database
            Parent mapaRoot = FXMLLoader.load(getClass().getResource("logIn.fxml"));
            Stage stage = (Stage) logOut.getScene().getWindow();
            stage.setScene(new Scene(mapaRoot));
            stage.show();
        } catch (IOException e) {
            System.out.println("Error while loading logIn screen");
            e.printStackTrace();
        }
    }
    
    private void editProfile(ActionEvent event) {
        try {
            Parent mapaRoot = FXMLLoader.load(getClass().getResource("Edit.fxml"));
            Stage stage = (Stage) edit.getScene().getWindow();
            stage.setScene(new Scene(mapaRoot));
            stage.show();
        } catch (IOException e) {
            System.out.println("Error while loading edit profile screen");
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleOpenGpxChooser(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import GPX Track");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("GPS Exchange Format (*.gpx)", "*.gpx")
        );

        Button sourceButton = (Button) event.getSource();
        Stage currentStage = (Stage) sourceButton.getScene().getWindow();
        File selectedGpx = fileChooser.showOpenDialog(currentStage);

        if (selectedGpx != null) {
            // Pass the file to the database library tool to parse and persist it
            Activity registered = SportActivityApp.getInstance().importActivity(selectedGpx);
            
            if (registered != null) {
                // Safeguard against table visual duplicates before appending
                boolean alreadyExists = false;
                for (GpxActivity row : activityTable.getItems()) {
                    if (row.getName().equals(registered.getName())) {
                        alreadyExists = true;
                        break;
                    }
                }
                
                if (!alreadyExists) {
                    refreshMenuActivityTable();
                }
            } else {
                // Local extractor fallback parsing logic if database registration drops
                String fullName = selectedGpx.getName();
                String cleanName = fullName.substring(0, fullName.length() - 4);
                
                boolean alreadyExists = false;
                for (GpxActivity row : activityTable.getItems()) {
                    if (row.getName().equals(cleanName)) {
                        alreadyExists = true;
                        break;
                    }
                }
                
                if (!alreadyExists) {
                    GpxMetadataExtractor.Metadata data = GpxMetadataExtractor.extract(selectedGpx);
                    GpxActivity newActivity = new GpxActivity(cleanName, data.distance, data.time, data.date, selectedGpx);
                    activityTable.getItems().add(newActivity);
                }
            }
        }
    }
    
    /**
     * Transitions seamlessly to the ActivityMenu screen by swapping roots 
     * inside the same active Stage window frame to prevent multiple window overlaps.
     */
    private void openGpxViewerScreen(Activity selectedActivity) {
        if (selectedActivity == null) return;

        try {
            // 1. Point to your map viewer layout file parameters
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ActivityMenu.fxml"));
            Parent root = loader.load();

            // 2. Fetch the existing controller instance object attached to that FXML scene
            ActivityMenuController controller = loader.getController();

            // 3. Extract the active runtime window Stage reference from our existing layout table node tree
            Stage currentStage = (Stage) activityTable.getScene().getWindow();

            // 4. Swap the scene root seamlessly inside our existing workspace frame window container
            currentStage.setScene(new Scene(root));
            currentStage.setTitle("Activity View: " + selectedActivity.getName());
            currentStage.show();

            // 5. Fire your dynamic map tracing polyline and annotation loading functions
            controller.loadActivityTrack(selectedActivity);

        } catch (IOException e) {
            System.err.println("Error launching the Activity Map View Scene root swap transaction.");
            e.printStackTrace();
        }
    }
    
    /**
     * Wrapper class for table row display elements.
     */
    public class GpxActivity {
        private final String name;
        private final String distance;
        private final String time;
        private final String date;
        private final File file;

        public GpxActivity(String name, String distance, String time, String date, File file) {
            this.name = name;
            this.distance = distance;
            this.time = time;
            this.date = date;
            this.file = file;
        }

        public String getName() { return name; }
        public String getDistance() { return distance; }
        public String getTime() { return time; }
        public String getDate() { return date; }
        public File getFile() { return file; }
    }
   
    /**
     * Fallback utility tool to extract track headers locally.
     */
    public static class GpxMetadataExtractor {

        public static class Metadata {
            public String distance = "0.0 km";
            public String time = "00:00:00";
            public String date = "--/--/----";
        }

        public static Metadata extract(File file) {
            Metadata meta = new Metadata();
            try {
                org.w3c.dom.Document doc = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
                org.w3c.dom.NodeList trkptList = doc.getElementsByTagName("trkpt");
                int totalPoints = trkptList.getLength();

                if (totalPoints == 0) return meta;

                double totalDistanceKm = 0.0;
                Instant startTime = null;
                Instant endTime = null;

                for (int i = 0; i < totalPoints; i++) {
                    org.w3c.dom.Element currentPt = (org.w3c.dom.Element) trkptList.item(i);
                    org.w3c.dom.NodeList timeNodes = currentPt.getElementsByTagName("time");
                    if (timeNodes.getLength() > 0) {
                        Instant ptTime = Instant.parse(timeNodes.item(0).getTextContent());
                        if (startTime == null) startTime = ptTime;
                        endTime = ptTime;
                    }

                    if (i > 0) {
                        org.w3c.dom.Element prevPt = (org.w3c.dom.Element) trkptList.item(i - 1);
                        double lat1 = Double.parseDouble(prevPt.getAttribute("lat"));
                        double lon1 = Double.parseDouble(prevPt.getAttribute("lon"));
                        double lat2 = Double.parseDouble(currentPt.getAttribute("lat"));
                        double lon2 = Double.parseDouble(currentPt.getAttribute("lon"));

                        totalDistanceKm += haversine(lat1, lon1, lat2, lon2);
                    }
                }

                meta.distance = String.format("%.2f km", totalDistanceKm);

                if (startTime != null && endTime != null) {
                    Duration duration = Duration.between(startTime, endTime);
                    long s = duration.getSeconds();
                    meta.time = String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);

                    Date startDate = Date.from(startTime);
                    meta.date = new SimpleDateFormat("dd/MM/yyyy").format(startDate);
                }

            } catch (Exception e) {
                System.err.println("Error processing GPX metadata: " + e.getMessage());
            }
            return meta;
        }

        private static double haversine(double lat1, double lon1, double lat2, double lon2) {
            double R = 6371.0;
            double dLat = Math.toRadians(lat2 - lat1);
            double dLon = Math.toRadians(lon2 - lon1);
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                       Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                       Math.sin(dLon / 2) * Math.sin(dLon / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            return R * c;
        }
    }
}