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
import javafx.application.Platform;
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
    @FXML
    private Button newActivity;
    @FXML
    private Button loadMap;
    @FXML
    private Button history;
    @FXML
    private Button MonStats;

    /**
     * Initializes the controller class.
     */ 
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        logOut.setOnAction(this::logOut);
        edit.setOnAction(this::editProfile);
        loadMap.setOnAction(this::handleLoadMap);
        MonStats.setOnAction(this::handleMonStats);
        
        // Load the profile pic to main menu
        SportActivityApp app = SportActivityApp.getInstance();
        User currentUser = app.getCurrentUser();
        
        if (currentUser != null) {
            Image avatar = currentUser.getAvatar();
            if (avatar != null) {
                profilePic.setImage(avatar);
            }else{
                try {
        
                    String imagePath = getClass().getResource("/resources/default_avatar.png").toExternalForm();
        
                    Image defaultAvatar = new Image(imagePath);
                    profilePic.setImage(defaultAvatar);
                }catch (Exception e) {
                    System.out.println("ERROR");
                }
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
                    // Instantly pull the exact object from memory, no O(n) string searching
                    Activity activityData = selectedActivity.getActivityRef();
                    
                    if (activityData != null) {
                        openGpxViewerScreen(activityData);
                    } else {
                        System.err.println("Critical Error: Raw Activity reference lost.");
                    }
                }
            }
        });
        
        Platform.runLater(() -> {
        Stage stage = (Stage) logOut.getScene().getWindow();
        stage.setMinWidth(615);
        stage.setMinHeight(440);
    });
    }    
    private void handleLoadMap(ActionEvent event){
        
        try {
            Parent mapaRoot = FXMLLoader.load(getClass().getResource("MapUpload.fxml"));
            Stage stage = (Stage) loadMap.getScene().getWindow();
            stage.setScene(new Scene(mapaRoot));
            stage.show();
        } catch (IOException e) {
            System.out.println("Error while loading MapUpload screen");
        }
    }
    /**
     * Pulls historical activities from the SQLite database file, filters out duplicates,
     * and populates the visible TableView dashboard.
     */
    private void refreshMenuActivityTable() {
        SportActivityApp app = SportActivityApp.getInstance();
        activityTable.getItems().clear();
        
        if (app.getCurrentUser() != null) {
            for (Activity act : app.getActivitiesByUser(app.getCurrentUser())) {
                double distKm = act.getTotalDistance() / 1000.0;
                String formattedDistance = String.format("%.2f km", distKm);
                
                long s = 0;
                if (act.getDuration() != null) {
                    s = act.getDuration().getSeconds();
                }
                String formattedTime = String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
                
                String formattedDate = "--/--/----";
                if (act.getStartTime() != null) {
                    Date startDate = Date.from(act.getStartTime().atZone(java.time.ZoneId.systemDefault()).toInstant());
                    formattedDate = new SimpleDateFormat("dd/MM/yyyy").format(startDate);
                }
                
                // Add row and pass the raw Activity object directly
                GpxActivity historicalRow = new GpxActivity(
                    act.getName(),
                    formattedDistance, 
                    formattedTime, 
                    formattedDate, 
                    null,
                    act 
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
        }
    }
    private void handleMonStats(ActionEvent event){
        try {
            Parent mapaRoot = FXMLLoader.load(getClass().getResource("CumulativeTotals.fxml"));
            Stage stage = (Stage) logOut.getScene().getWindow();
            stage.setScene(new Scene(mapaRoot));
            stage.show();
        } catch (IOException e) {
            System.out.println("Error while loading cumulative totals screen");
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
        if (selectedGpx == null) return;

        String validationError = validateGpxFile(selectedGpx);
        if (validationError != null) {
            showInvalidGpxAlert(validationError);
            return;
        }

        Activity registered = SportActivityApp.getInstance().importActivity(selectedGpx);
        
        if (registered != null) {
            // Nuke the duplicate check. Always refresh to reflect the database reality.
            refreshMenuActivityTable();
        } else {
            showInvalidGpxAlert("Could not parse or persist the GPX activity data.");
        }
    }

    private String validateGpxFile(File file) {
        if (!file.exists() || !file.isFile()) return "The selected GPX file does not exist.";
        if (!file.canRead()) return "The selected GPX file cannot be read.";
        if (!file.getName().toLowerCase().endsWith(".gpx")) return "The selected file must be a GPX file.";

        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setExpandEntityReferences(false);

            org.w3c.dom.Document doc = factory.newDocumentBuilder().parse(file);
            doc.getDocumentElement().normalize();

            org.w3c.dom.Element root = doc.getDocumentElement();
            if (root == null || !"gpx".equalsIgnoreCase(root.getNodeName())) {
                return "The selected file is not a valid GPX document.";
            }

            org.w3c.dom.NodeList trackPoints = doc.getElementsByTagName("trkpt");
            if (trackPoints == null || trackPoints.getLength() < 2) {
                return "The GPX file does not contain enough track points.";
            }

            for (int i = 0; i < trackPoints.getLength(); i++) {
                org.w3c.dom.Element trkpt = (org.w3c.dom.Element) trackPoints.item(i);
                String latText = trkpt.getAttribute("lat");
                String lonText = trkpt.getAttribute("lon");

                if (latText == null || latText.isBlank() || lonText == null || lonText.isBlank()) {
                    return "The GPX file contains a track point without latitude or longitude.";
                }

                double lat = Double.parseDouble(latText);
                double lon = Double.parseDouble(lonText);

                if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) {
                    return "The GPX file contains coordinates outside the valid range.";
                }
            }

            return null;
        } catch (Exception ex) {
            return "The GPX file is corrupted or is not valid XML.";
        }
    }

    private void showInvalidGpxAlert(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Invalid GPX file");
        alert.setHeaderText("The selected GPX file cannot be imported");
        alert.setContentText(message);
        alert.showAndWait();
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
        }
    }

    @FXML
    private void logOutHandle(ActionEvent event) {
        SportActivityApp app= SportActivityApp.getInstance();
        
        app.logout();
        try{
            //load screen 
        Parent loginRoot = FXMLLoader.load(getClass().getResource("logIn.fxml"));
            //get stage 
        Stage stage = (Stage) logOut.getScene().getWindow();
            //change scene
        Scene scene = new Scene(loginRoot);
        stage.setScene(scene);
        stage.show();
        }catch(IOException e){}
    }

    @FXML
    private void openHistory(ActionEvent event) {
        try{
            //load screen 
        Parent loginRoot = FXMLLoader.load(getClass().getResource("History.fxml"));
            //get stage 
        Stage stage = (Stage) history.getScene().getWindow();
            //change scene
        Scene scene = new Scene(loginRoot);
        stage.setScene(scene);
        stage.show();
        }catch(IOException e){}
    }
    
    /**
     * Wrapper class for table row display elements.
     */
    public static class GpxActivity {
        private final String name;
        private final String distance;
        private final String time;
        private final String date;
        private final File file;
        private final Activity activityRef; 

        public GpxActivity(String name, String distance, String time, String date, File file, Activity activityRef) {
            this.name = name;
            this.distance = distance;
            this.time = time;
            this.date = date;
            this.file = file;
            this.activityRef = activityRef;
        }

        public String getName() { return name; }
        public String getDistance() { return distance; }
        public String getTime() { return time; }
        public String getDate() { return date; }
        public File getFile() { return file; }
        public Activity getActivityRef() { return activityRef; }
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