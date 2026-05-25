/*
 * ============================================================
 *  Running la Safor – IPC 2026
 *  Asignatura: Interfaces Persona-Computador
 *  Universitat Politècnica de València
 * ============================================================
 *
 *  DESCRIPCIÓN GENERAL
 *  -------------------
 *  Controlador de la vista de actividad. Gestiona:
 *   1. Carga y visualización del mapa de la actividad.
 *   2. Zoom interactivo mediante un Slider.
 *   3. Trazado de la ruta GPX sobre el mapa.
 *   4. Anotaciones geográficas con clic derecho (texto, punto, línea, círculo).
 *   5. Listado de anotaciones con centrado del mapa al seleccionar.
 *   6. Estadísticas de la actividad (distancia, duración, velocidad…).
 *   7. Perfil de elevación interactivo con resaltado sobre el mapa.
 *   8. Visualización de la velocidad por tramos codificada en color.
 *
 *  PATRÓN UTILIZADO: MVC (Model-View-Controller)
 *   - Modelo : Activity, TrackPoint, Annotation  (sportlib)
 *   - Vista  : ActivityMenu.fxml
 *   - Control: esta clase
 *
 * ============================================================
 */
package mapademo;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.Annotation;
import upv.ipc.sportlib.GeoPoint;
import upv.ipc.sportlib.MapProjection;
import upv.ipc.sportlib.MapRegion;
import upv.ipc.sportlib.TrackPoint;
import java.util.List;
import upv.ipc.sportlib.AnnotationType;
import upv.ipc.sportlib.SportActivityApp;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.collections.FXCollections;
import java.util.ArrayList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.application.Platform;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

/**
 * Controlador principal de la aplicación de mapa con POIs.
 *
 * La anotación @FXML conecta automáticamente los campos de esta clase
 * con los elementos declarados en el fichero FXML mediante su atributo fx:id.
 *
 * Implementa {@link Initializable} para poder ejecutar código de
 * inicialización una vez que el FXML ha sido cargado completamente.
 */
public class ActivityMenuController implements Initializable {

    // =========================================================
    //  ESTRUCTURA DE NODOS PARA ZOOM
    // =========================================================
    //
    //  El zoom se consigue escalando un Group (zoomGroup).
    //  Escalar un Group NO desplaza los nodos que contiene,
    //  lo que evita el "salto" visual al hacer zoom.
    //
    //  Jerarquía de nodos:
    //
    //  ScrollPane (map_scrollpane)
    //   └─ contentGroup          ← Group raíz dentro del ScrollPane
    //       └─ zoomGroup         ← se escala para el zoom
    //           └─ mapPane       ← Pane con la imagen y los POIs
    //               ├─ ImageView ← imagen del mapa
    //               ├─ Text      ← etiquetas de POIs
    //               └─ Circle    ← anotaciones circulares
    //
    // =========================================================

    /** Group que se escala para aplicar el zoom. */
    private Group zoomGroup;

    /**
     * Pane que actúa como lienzo del mapa.
     * Contiene la imagen de fondo y todos los elementos superpuestos
     * (textos, círculos, etc.). Sus dimensiones coinciden con las de
     * la imagen cargada.
     */
    private Pane mapPane;

    
    /** Menú contextual reutilizable para el clic derecho sobre el mapa. */
    private ContextMenu mapContextMenu;


    // =========================================================
    //  ELEMENTOS FXML  (inyectados automáticamente por el cargador)
    // =========================================================

    /** Lista lateral que muestra todos los POIs añadidos al mapa. */
    @FXML
    private ListView<Annotation> map_annotations_listview;

    /** ScrollPane que envuelve el mapa y permite desplazarlo. */
    @FXML
    private ScrollPane map_scrollpane;
    

    /**
     * Slider de zoom.
     * Rango: [0.5 – 1.5]. Valor inicial: 1.0 (sin zoom).
     * Cada cambio de valor llama al método zoom().
     */
    @FXML
    private Slider zoom_slider;

    private Activity currentActivity;

    @FXML
    private Label mousePosition;
    
    @FXML
    private Label lblDistance;
    
    @FXML
    private Label lblDuration;
    
    @FXML
    private Label lblSpeed;
    
    @FXML
    private Label lblPace;
    
    @FXML
    private Label lblGain;
    @FXML
    private Label lblLoss;
    
    @FXML
    private Label lblMaxAltitude;
    @FXML
    private Label lblMinAltitude;
    
    // =========================================================
    // Implementation for the point 6
    // =========================================================

    // 6.1 – Elevation profile
    @FXML
    private LineChart<Number, Number> elevationChart;

    @FXML
    private Label elevationLabel;

    // 6.2 – Speed over the route
    @FXML
    private CheckBox showSpeedOverlay;

    @FXML
    private HBox speedLegendBox;

    @FXML
    private Label segmentDetailsLabel;

    // Internal state
    private final List<RouteSample> elevationSamples = new ArrayList<>();
    private final List<Double> segmentSpeeds = new ArrayList<>();
    private double minSegmentSpeedKmh = Double.POSITIVE_INFINITY;
    private double maxSegmentSpeedKmh = Double.NEGATIVE_INFINITY;
    private Circle elevationHoverMarker;
    private final List<Line> speedOverlayLines = new ArrayList<>();
    private final List<javafx.scene.Node> annotationNodes = new ArrayList<>();
    private AnnotationType pendingTwoPointType;
    private String pendingAnnotationText;
    private String pendingAnnotationColor;
    private double pendingAnnotationStrokeWidth;
    private GeoPoint pendingFirstPoint;


    // =========================================================
    //  MANEJADORES DE ZOOM
    // =========================================================

    /**
     * Aumenta el zoom en 0.1 unidades al pulsar el botón "+".
     *
     * @param event evento de acción del botón
     */
    @FXML
    void zoomIn(ActionEvent event) {
        double sliderVal = zoom_slider.getValue();
        zoom_slider.setValue(sliderVal + 0.1);
    }

    /**
     * Reduce el zoom en 0.1 unidades al pulsar el botón "–".
     *
     * @param event evento de acción del botón
     */
    @FXML
    void zoomOut(ActionEvent event) {
        double sliderVal = zoom_slider.getValue();
        zoom_slider.setValue(sliderVal - 0.1);
    }

    /**
     * Aplica el factor de escala al {@code zoomGroup}.
     *
     * Este método es invocado automáticamente cada vez que cambia el
     * valor del slider, gracias al listener registrado en {@link #initialize}.
     *
     * Truco: guardamos y restauramos los valores de scroll para que el
     * contenido visible no salte al cambiar la escala.
     *
     * @param scaleValue nuevo factor de escala (p. ej. 1.2 → 120 %)
     */
    private void zoom(double scaleValue) {
        // Guardamos la posición del scroll antes de escalar
        double scrollH = map_scrollpane.getHvalue();
        double scrollV = map_scrollpane.getVvalue();

        // Aplicamos el zoom escalando el Group en ambos ejes
        zoomGroup.setScaleX(scaleValue);
        zoomGroup.setScaleY(scaleValue);

        // Restauramos la posición del scroll para que el centro visual
        // permanezca estable durante el zoom
        map_scrollpane.setHvalue(scrollH);
        map_scrollpane.setVvalue(scrollV);
    }

    // =========================================================
    //  CONSTRUCCIÓN DEL MAPA
    // =========================================================

    /**
     * Carga una imagen y construye la jerarquía de nodos del mapa.
     *
     * Este método puede llamarse varias veces (p. ej. al cambiar el mapa),
     * ya que sustituye completamente el contenido del ScrollPane.
     *
     * @param imgFile fichero de imagen a cargar como fondo del mapa
     */
    private void buildMap(File imgFile) {
        // Comprobación defensiva: si el fichero no existe mostramos un aviso
        if (!imgFile.exists()) {
            map_scrollpane.setContent(
                new Label("Imagen no encontrada: " + imgFile.getPath()));
            return;
        }

        // Cargamos la imagen y obtenemos sus dimensiones reales en píxeles
        Image img = new Image(imgFile.toURI().toString());
        double W = img.getWidth();
        double H = img.getHeight();

        // ── mapPane: lienzo del mapa ───────────────────────────────────
        // Usamos un Pane (y no un Group) para poder posicionar los nodos
        // hijos con coordenadas absolutas (setLayoutX / setLayoutY).
        mapPane = new Pane();
        mapPane.setPrefSize(W, H); // tamaño preferido = tamaño de la imagen
        mapPane.setMinSize(W, H);  // impedimos que el layout lo encoja
        mapPane.setMaxSize(W, H);  // impedimos que el layout lo agrande

        // Añadimos la imagen como fondo del Pane
        ImageView iv = new ImageView(img);
        iv.setFitWidth(W);
        iv.setFitHeight(H);
        mapPane.getChildren().add(iv);

        // ── Manejador de clics sobre el mapa ──────────────────────────
        // Gestionamos el clic derecho (menú contextual) y el clic izquierdo
        // en modo inserción (FIX 2).
        mapPane.setOnMouseClicked(this::handleMapClick);

        // ── Jerarquía de Groups para el zoom ──────────────────────────
        // contentGroup es el nodo raíz que recibe el ScrollPane.
        // zoomGroup es el que se escala; anidar un Group dentro de otro
        // evita que el ScrollPane reajuste su contenido durante el escalado.
        zoomGroup = new Group();
        Group contentGroup = new Group();
        zoomGroup.getChildren().add(mapPane);
        contentGroup.getChildren().add(zoomGroup);

        // Aplicamos el zoom actual (valor actual del slider)
        double zoom = zoom_slider.getValue();
        zoomGroup.setScaleX(zoom);
        zoomGroup.setScaleY(zoom);

        // Asignamos el contentGroup como contenido del ScrollPane
        map_scrollpane.setContent(contentGroup);

    }

    // =========================================================
    //  MENÚ CONTEXTUAL (clic derecho sobre el mapa)
    // =========================================================



    private void handleMapClick(MouseEvent event) {
        if (pendingTwoPointType != null) {
            if (event.getButton() == MouseButton.PRIMARY) {
                finishPendingTwoPointAnnotation(event.getX(), event.getY());
            }
            return;
        }

        if (event.getButton() == MouseButton.SECONDARY) {
            onMapRightClick(event.getX(), event.getY());
        }
    }

    private void finishPendingTwoPointAnnotation(double x, double y) {
        if (currentActivity == null || mapPane == null || pendingFirstPoint == null || pendingTwoPointType == null) return;

        MapRegion region = currentActivity.getSuggestedMap();
        if (region == null) return;

        MapProjection proj = new MapProjection(region, mapPane.getWidth(), mapPane.getHeight());
        GeoPoint secondPoint = proj.unproject(x, y);

        List<GeoPoint> pointsList = new ArrayList<>();
        pointsList.add(pendingFirstPoint);
        pointsList.add(secondPoint);

        Annotation completedAnnotation = new Annotation(
            pendingTwoPointType,
            pendingAnnotationText,
            pendingAnnotationColor,
            pendingAnnotationStrokeWidth,
            pointsList
        );

        pendingTwoPointType = null;
        pendingAnnotationText = null;
        pendingAnnotationColor = null;
        pendingAnnotationStrokeWidth = 0.0;
        pendingFirstPoint = null;

        Annotation savedAnnotation = SportActivityApp.getInstance().addAnnotation(currentActivity, completedAnnotation);
        if (savedAnnotation != null) {
            renderSingleAnnotation(savedAnnotation);
            map_annotations_listview.getItems().add(savedAnnotation);
        }
    }


    /**
     * Muestra el menú contextual reutilizable en la posición del clic.
     *
     * Las acciones de los MenuItem se actualizan con las coordenadas
     * del clic actual antes de mostrar el menú.
     *
     * @param x coordenada X del clic en el sistema local del mapPane
     * @param y coordenada Y del clic en el sistema local del mapPane
     */
    private void onMapRightClick(double x, double y) {
        mapContextMenu.hide();
        mapContextMenu.getItems().get(0).setOnAction(e -> promptForAnnotation(x, y));
        mapContextMenu.show(
            mapPane.getScene().getWindow(),
            mapPane.localToScreen(x, y).getX(),
            mapPane.localToScreen(x, y).getY()
        );
    }

    // =========================================================
    //  INICIALIZACIÓN DEL CONTROLADOR
    // =========================================================

    /**
     * Método llamado automáticamente por el FXMLLoader tras inyectar
     * todos los elementos {@code @FXML}.
     *
     * Aquí configuramos:
     *  - El slider de zoom y su listener.
     *  - El ContextMenu reutilizable (FIX 6).
     *  - La CellFactory del ListView (FIX 4).
     *  - La carga del mapa inicial.
     *
     * @param url  URL del documento FXML (no usado aquí)
     * @param rb   paquete de recursos de internacionalización (no usado aquí)
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        zoom_slider.setMin(0.5);
        zoom_slider.setMax(1.5);
        zoom_slider.setValue(1.0);


        zoom_slider.valueProperty().addListener(
            (observable, oldVal, newVal) -> zoom((Double) newVal)
        );

        MenuItem miText = new MenuItem("📝 Add annotation");
        mapContextMenu = new ContextMenu(miText);

        MenuItem miRemove = new MenuItem("🗑 Remove annotation");
        miRemove.setOnAction(e -> {
            Annotation selected = map_annotations_listview.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            SportActivityApp.getInstance().removeAnnotation(selected);
            map_annotations_listview.getItems().remove(selected);
            map_annotations_listview.getSelectionModel().clearSelection();
            reloadAnnotationsOnMap();
        });
        map_annotations_listview.setContextMenu(new ContextMenu(miRemove));

        buildMap(new File("maps/upv.jpg"));

        map_annotations_listview.setCellFactory(listView -> new ListCell<Annotation>() {
            @Override
            protected void updateItem(Annotation ann, boolean empty) {
                super.updateItem(ann, empty);
                if (empty || ann == null) {
                    setText(null);
                } else {
                    setText(ann.getType() + ": " + ann.getText());
                }
            }
        });

        map_annotations_listview.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.getGeoPoints().isEmpty()
                    && currentActivity != null && mapPane != null && zoomGroup != null) {

                GeoPoint geo = newVal.getGeoPoints().get(0);

                MapRegion currentRegion = currentActivity.getSuggestedMap();
                if (currentRegion == null) return;
                MapProjection proj = new MapProjection(currentRegion, mapPane.getWidth(), mapPane.getHeight());
                Point2D pixel = proj.project(geo);

                centerMapOnCoordinates(pixel.getX(), pixel.getY());
            }
        });



        if (elevationChart != null) {
            elevationChart.setCreateSymbols(false);
            elevationChart.setLegendVisible(false);
        }

        if (elevationLabel != null) {
            elevationLabel.setText("Move the mouse over the profile to inspect the route");
        }



        if (showSpeedOverlay != null) {
            showSpeedOverlay.selectedProperty().addListener((obs, wasOn, isOn) -> {
                speedOverlayLines.forEach(line -> line.setVisible(isOn));
                if (speedLegendBox != null) speedLegendBox.setVisible(isOn);
                if (segmentDetailsLabel != null) {
                    segmentDetailsLabel.setText(isOn ? "Place the mouse cursor in a segment of the route to see the speed for that segment" : "");
                }
            });
        }    

        Platform.runLater(() -> {
            Stage stage = (Stage) elevationLabel.getScene().getWindow();
            stage.setWidth(1315);
            stage.setHeight(900);
            stage.setMinWidth(1315);
            stage.setMinHeight(900);
        });
}

    // =========================================================
    //  INDICADOR DE POSICIÓN DEL RATÓN
    // =========================================================

    @FXML
    private void showPosition(MouseEvent event) {
        mousePosition.setText(
            "sceneX: " + (int) event.getSceneX() +
            ", sceneY: " + (int) event.getSceneY() + "\n" +
            "         X: " + (int) event.getX() +
            ",          Y: " + (int) event.getY()
        );
    }

    // =========================================================
    //  DIÁLOGO "ACERCA DE"
    // =========================================================

    @FXML
    private void about(ActionEvent event) {
        Alert mensaje = new Alert(Alert.AlertType.INFORMATION);

        // Personalizamos el icono de la ventana del diálogo
        Stage dialogStage = (Stage) mensaje.getDialogPane().getScene().getWindow();
        dialogStage.getIcons().add(
            new Image(getClass().getResourceAsStream("/resources/logo.png"))
        );

        mensaje.setTitle("Acerca de");
        mensaje.setHeaderText("IPC - 2026");
        mensaje.showAndWait(); // Bloquea hasta que el usuario cierra el diálogo
    }

    

    @FXML
    private void handleGoBack(ActionEvent event) {
        try {
            Parent menuRoot = FXMLLoader.load(getClass().getResource("/mapademo/Menu.fxml"));
            Stage stage = (Stage) map_scrollpane.getScene().getWindow();
            stage.setScene(new Scene(menuRoot));
            stage.show();
        } catch (IOException e) {
            System.err.println("Error reloading Menu view: " + e.getMessage());
        }
    }

    public void loadActivityTrack(Activity activity) {
        if (activity == null) return;

        MapRegion region = activity.getSuggestedMap();
        if (region == null) {
            System.err.println("No matching map region found for this activity coordinates.");
            return;
        }

        File imgFile = new File(region.getImagePath());
        if (!imgFile.exists()) {
            map_scrollpane.setContent(
                new Label("Map image file not found at: " + region.getImagePath()));
            return;
        }

        Image img = new Image(imgFile.toURI().toString());
        double W = img.getWidth();
        double H = img.getHeight();

        mapPane = new Pane();
        mapPane.setPrefSize(W, H);
        mapPane.setMinSize(W, H);
        mapPane.setMaxSize(W, H);

        ImageView iv = new ImageView(img);
        iv.setFitWidth(W);
        iv.setFitHeight(H);
        mapPane.getChildren().add(iv);

        mapPane.setOnMouseClicked(this::handleMapClick);

        MapProjection proj = new MapProjection(region, W, H);

        Polyline route = new Polyline();
        route.setStroke(Color.BLUE);
        route.setStrokeWidth(3.0);
        route.setFill(Color.TRANSPARENT);

        for (TrackPoint tp : activity.getTrackPoints()) {
            Point2D p = proj.project(tp);
            route.getPoints().addAll(p.getX(), p.getY());
        }
        mapPane.getChildren().add(route);

        zoomGroup = new Group();
        Group contentGroup = new Group();
        zoomGroup.getChildren().add(mapPane);
        contentGroup.getChildren().add(zoomGroup);

        double currentZoom = zoom_slider.getValue();
        zoomGroup.setScaleX(currentZoom);
        zoomGroup.setScaleY(currentZoom);

        map_scrollpane.setContent(contentGroup);

        if (!activity.getTrackPoints().isEmpty()) {
            Point2D startPixel = proj.project(activity.getStartPoint());
            Point2D endPixel = proj.project(activity.getEndPoint());

            Circle startMarker = new Circle(startPixel.getX(), startPixel.getY(), 6, Color.GREEN);
            Circle endMarker = new Circle(endPixel.getX(), endPixel.getY(), 6, Color.RED);

            mapPane.getChildren().addAll(startMarker, endMarker);

            centerMapOnCoordinates(startPixel.getX(), startPixel.getY());
        }

        displayActivityStatistics(activity);

        this.currentActivity = activity;

        map_annotations_listview.getItems().clear();
        annotationNodes.clear();

        for (Annotation ann : activity.getAnnotations()) {
            renderSingleAnnotation(ann);
            map_annotations_listview.getItems().add(ann);
        }

        speedOverlayLines.clear();
        if (showSpeedOverlay != null) showSpeedOverlay.setSelected(false);
        if (segmentDetailsLabel != null) segmentDetailsLabel.setText("");


        final Activity activityRef = activity;
        Platform.runLater(() -> {
            initializeElevationProfile(activityRef.getTrackPoints());
            initializeSpeedOverlay(activityRef.getTrackPoints());
        });
    }

    /**
     * Repositions the scroll bars smoothly over the targeted absolute map pixels.
     */

    private void centerMapOnCoordinates(double x, double y) {
        double mapWidth  = mapPane.getWidth()  * zoomGroup.getScaleX();
        double mapHeight = mapPane.getHeight() * zoomGroup.getScaleY();
        double viewW = map_scrollpane.getViewportBounds().getWidth();
        double viewH = map_scrollpane.getViewportBounds().getHeight();

        double denominatorH = mapWidth - viewW;
        double denominatorV = mapHeight - viewH;

        double scrollH = denominatorH <= 0 ? 0.5 : (x * zoomGroup.getScaleX() - viewW / 2) / denominatorH;
        double scrollV = denominatorV <= 0 ? 0.5 : (y * zoomGroup.getScaleY() - viewH / 2) / denominatorV;

        map_scrollpane.setHvalue(Math.max(0, Math.min(1, scrollH)));
        map_scrollpane.setVvalue(Math.max(0, Math.min(1, scrollV)));
    }
    
    //Extracts geographic and performance metrics from the activity and populates the statistics GridPane layout labels.

    private void displayActivityStatistics(Activity activity) {
        if (activity == null) return;

        double distanceKm = activity.getTotalDistance() / 1000.0;
        lblDistance.setText(String.format("%.2f km", distanceKm));

        long totalSeconds = activity.getDuration().getSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long secs = totalSeconds % 60;
        lblDuration.setText(String.format("%02d:%02d:%02d", hours, minutes, secs));

        lblSpeed.setText(String.format("%.2f km/h", activity.getAverageSpeed()));

        lblPace.setText(String.format("%.2f min/km", activity.getAveragePace()));

        lblGain.setText(String.format("+%.0f m", activity.getElevationGain()));
        lblLoss.setText(String.format("-%.0f m", activity.getElevationLoss()));

        lblMaxAltitude.setText(String.format("%.0f m", activity.getMaxElevation()));
        lblMinAltitude.setText(String.format("%.0f m", activity.getMinElevation()));
    }
    private int requiredGeoPointCount(AnnotationType type) {
        if (type == AnnotationType.LINE || type == AnnotationType.CIRCLE) {
            return 2;
        }
        return 1;
    }

    
    //Scenario 4.2 Compliance: Prompts the user to select an annotation type, description, and color, then registers the object to the database.



    private void promptForAnnotation(double x, double y) {
        if (currentActivity == null || mapPane == null) return;

        Dialog<Annotation> dialog = new Dialog<>();
        dialog.setTitle("New Geographic Annotation");
        dialog.setHeaderText("Configure your route annotation details:");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        ChoiceBox<AnnotationType> typeSelector = new ChoiceBox<>(
            FXCollections.observableArrayList(AnnotationType.values())
        );
        typeSelector.setValue(AnnotationType.TEXT);

        TextField textInput = new TextField();
        textInput.setPromptText("Enter notes or label text...");

        ColorPicker colorSelector = new ColorPicker(Color.RED);

        VBox dialogContent = new VBox(12);
        dialogContent.getChildren().addAll(
            new Label("Annotation Type:"), typeSelector,
            new Label("Description / Label Text:"), textInput,
            new Label("Display Color:"), colorSelector
        );
        dialog.getDialogPane().setContent(dialogContent);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                AnnotationType selectedType = typeSelector.getValue();
                String enteredText = textInput.getText().trim();
                if (selectedType == AnnotationType.TEXT && enteredText.isEmpty()) return null;

                Color c = colorSelector.getValue();
                String hexColor = String.format("#%02X%02X%02X",
                    (int)(c.getRed() * 255),
                    (int)(c.getGreen() * 255),
                    (int)(c.getBlue() * 255)
                );

                MapRegion region = currentActivity.getSuggestedMap();
                if (region == null) return null;
                MapProjection proj = new MapProjection(region, mapPane.getWidth(), mapPane.getHeight());
                GeoPoint primaryPoint = proj.unproject(x, y);

                List<GeoPoint> pointsList = new ArrayList<>();
                pointsList.add(primaryPoint);


                return new Annotation(
                    selectedType,
                    enteredText,
                    hexColor,
                    3.0,
                    pointsList
                );
            }
            return null;
        });

        Optional<Annotation> result = dialog.showAndWait();
        if (result.isPresent()) {
            Annotation createdAnnotation = result.get();

            if (requiredGeoPointCount(createdAnnotation.getType()) == 2) {
                pendingTwoPointType = createdAnnotation.getType();
                pendingAnnotationText = createdAnnotation.getText();
                pendingAnnotationColor = createdAnnotation.getColor();
                pendingAnnotationStrokeWidth = createdAnnotation.getStrokeWidth();
                pendingFirstPoint = createdAnnotation.getGeoPoints().get(0);

                Alert secondPointAlert = new Alert(Alert.AlertType.INFORMATION);
                secondPointAlert.setTitle("Second point required");
                secondPointAlert.setHeaderText("Select the second point on the map");
                secondPointAlert.setContentText("Click on the map to choose the "
                    + (createdAnnotation.getType() == AnnotationType.LINE ? "end point of the line." : "edge of the circle."));
                secondPointAlert.showAndWait();
                return;
            }

            Annotation savedAnnotation = SportActivityApp.getInstance().addAnnotation(currentActivity, createdAnnotation);
            if (savedAnnotation != null) {

                renderSingleAnnotation(savedAnnotation);
                map_annotations_listview.getItems().add(savedAnnotation);
            }
        }
    }

    //Evaluates an annotation object data profile and renders its corresponding vector shapes or text labels dynamically onto the map canvas layout.

    private void renderSingleAnnotation(Annotation ann) {
        if (ann == null || ann.getType() == null || ann.getGeoPoints() == null) return;
        if (ann.getGeoPoints().size() < requiredGeoPointCount(ann.getType())) return;
        if (currentActivity == null || mapPane == null) return;

        MapRegion region = currentActivity.getSuggestedMap();
        if (region == null) return;
        MapProjection proj = new MapProjection(region, mapPane.getWidth(), mapPane.getHeight());

        Point2D p1 = proj.project(ann.getGeoPoints().get(0));
        Color renderColor = Color.web(ann.getColor());

        switch (ann.getType()) {
            case POINT:
                Circle pointMarker = new Circle(p1.getX(), p1.getY(), 7, renderColor);
                pointMarker.setStroke(Color.WHITE);
                pointMarker.setStrokeWidth(1.5);
                mapPane.getChildren().add(pointMarker);
                annotationNodes.add(pointMarker);
                break;

            case TEXT:
                Text textLabel = new Text(p1.getX(), p1.getY() - 8, ann.getText());
                textLabel.setFill(renderColor);
                textLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
                mapPane.getChildren().add(textLabel);
                annotationNodes.add(textLabel);
                break;

            case LINE:
                Point2D lineEnd = proj.project(ann.getGeoPoints().get(1));
                Polyline lineSegment = new Polyline(p1.getX(), p1.getY(), lineEnd.getX(), lineEnd.getY());
                lineSegment.setStroke(renderColor);
                lineSegment.setStrokeWidth(ann.getStrokeWidth());
                mapPane.getChildren().add(lineSegment);
                annotationNodes.add(lineSegment);
                break;

            case CIRCLE:
                Point2D edge = proj.project(ann.getGeoPoints().get(1));
                double radiusPx = p1.distance(edge);
                Circle geographicArea = new Circle(p1.getX(), p1.getY(), radiusPx, Color.TRANSPARENT);
                geographicArea.setStroke(renderColor);
                geographicArea.setStrokeWidth(ann.getStrokeWidth());
                mapPane.getChildren().add(geographicArea);
                annotationNodes.add(geographicArea);
                break;
        }
    }
    
    //Removes all annotation visuals from the map and redraws them from the current activity's annotation list.

    private void reloadAnnotationsOnMap() {
        if (mapPane == null) return;
        mapPane.getChildren().removeAll(annotationNodes);
        annotationNodes.clear();
        for (Annotation ann : map_annotations_listview.getItems()) {
            renderSingleAnnotation(ann);
        }
    }

    private void ensureElevationHoverMarker() {
        if (mapPane == null) return;

        if (elevationHoverMarker == null) {
            elevationHoverMarker = new Circle(6, Color.ORANGE);
            elevationHoverMarker.setStroke(Color.BLACK);
            elevationHoverMarker.setStrokeWidth(1.5);
            elevationHoverMarker.setVisible(false);
            mapPane.getChildren().add(elevationHoverMarker);
        } else if (!mapPane.getChildren().contains(elevationHoverMarker)) {
            elevationHoverMarker.setVisible(false);
            mapPane.getChildren().add(elevationHoverMarker);
        }
    }

    private static class RouteSample {
        private final Point2D pixel;
        private final double cumulativeKm;
        private final double altitudeMeters;

        private RouteSample(Point2D pixel,
                            double cumulativeKm,
                            double altitudeMeters) {
            this.pixel = pixel;
            this.cumulativeKm = cumulativeKm;
            this.altitudeMeters = altitudeMeters;
        }
    }

    /**
     * Build the elevation samples by converting the list of TrackPoints in a RouteSample list computing:
     * - coordinates in the map
     * - the accumulated distance
     * - altitude
     */

    private void buildElevationSamples(List<TrackPoint> points, MapProjection projection) {
        elevationSamples.clear();
        segmentSpeeds.clear();
        minSegmentSpeedKmh = Double.POSITIVE_INFINITY;
        maxSegmentSpeedKmh = Double.NEGATIVE_INFINITY;

        if (points == null || points.size() < 2 || projection == null) {
            return;
        }

        double cumulativeKm = 0.0;
        TrackPoint previous = null;

        for (TrackPoint current : points) {

            Point2D pixel = projection.project(current);
            double altitude = current.getElevation();

            if (previous != null) {
                double speedKmh = segmentSpeedKmh(previous, current);
                cumulativeKm += previous.distanceTo(current) / 1000.0;

                minSegmentSpeedKmh = Math.min(minSegmentSpeedKmh, speedKmh);
                maxSegmentSpeedKmh = Math.max(maxSegmentSpeedKmh, speedKmh);
                segmentSpeeds.add(speedKmh);
            }

            elevationSamples.add(
                new RouteSample(pixel, cumulativeKm, altitude)
            );

            previous = current;
        }

        if (minSegmentSpeedKmh == Double.POSITIVE_INFINITY) {
            minSegmentSpeedKmh = 0.0;
            maxSegmentSpeedKmh = 0.0;
        }
    }

    private void populateElevationChart() {
        if (elevationChart == null) return;

        elevationChart.getData().clear();

        XYChart.Series<Number, Number> series = new XYChart.Series<>();

        for (RouteSample sample : elevationSamples) {
            series.getData().add(
                new XYChart.Data<>(sample.cumulativeKm, sample.altitudeMeters)
            );
        }

        elevationChart.getData().add(series);

        elevationChart.setOnMouseMoved(this::handleElevationChartHover);
        elevationChart.setOnMouseExited(event -> {
            if (elevationHoverMarker != null) {
                elevationHoverMarker.setVisible(false);
            }

            if (elevationLabel != null) {
                elevationLabel.setText("Move the mouse over the profile to inspect the route");
            }
        });
    }

    //Busca la muestra cuya distancia acumulada esté más cerca del valor X del gráfico.

    private RouteSample findNearestSampleByKm(double km) {
        if (elevationSamples.isEmpty()) return null;

        int lo = 0;
        int hi = elevationSamples.size() - 1;

        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (elevationSamples.get(mid).cumulativeKm < km) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }

        if (lo == 0) return elevationSamples.get(0);
        if (lo >= elevationSamples.size()) return elevationSamples.get(elevationSamples.size() - 1);

        RouteSample right = elevationSamples.get(lo);
        RouteSample left = elevationSamples.get(lo - 1);

        return (Math.abs(left.cumulativeKm - km) <= Math.abs(right.cumulativeKm - km))
                ? left : right;
    }

    //Get the mouse position and translate to coordinates over the map.
    private void handleElevationChartHover(MouseEvent event) {
        if (elevationSamples.isEmpty()) return;
        if (!(elevationChart.getXAxis() instanceof NumberAxis)) return;

        NumberAxis xAxis = (NumberAxis) elevationChart.getXAxis();

        Point2D axisLocal = xAxis.sceneToLocal(event.getSceneX(), event.getSceneY());
        Number xValue = xAxis.getValueForDisplay(axisLocal.getX());

        if (xValue == null) return;

        RouteSample nearest = findNearestSampleByKm(xValue.doubleValue());
        if (nearest == null) return;

        ensureElevationHoverMarker();

        elevationHoverMarker.setCenterX(nearest.pixel.getX());
        elevationHoverMarker.setCenterY(nearest.pixel.getY());
        elevationHoverMarker.setVisible(true);

        if (elevationLabel != null) {
            elevationLabel.setText(String.format(
                "Distancia: %.2f km | Altitud: %.0f m",
                nearest.cumulativeKm, nearest.altitudeMeters
            ));
        }
    }

    private void initializeElevationProfile(List<TrackPoint> trackPoints) {
        if (currentActivity == null || mapPane == null || elevationChart == null
                || trackPoints == null || trackPoints.size() < 2) {
            return;
        }



        if (!(elevationChart.getXAxis() instanceof NumberAxis)) {
            NumberAxis numX = new NumberAxis();
            numX.setLabel("Distance (km)");
            numX.setTickLabelFill(Color.WHITE);
            numX.setAutoRanging(true);

            NumberAxis numY = new NumberAxis();
            numY.setLabel("Altitude (m)");
            numY.setTickLabelFill(Color.WHITE);
            numY.setAutoRanging(true);

            LineChart<Number, Number> fixed = new LineChart<>(numX, numY);
            fixed.setCreateSymbols(false);
            fixed.setLegendVisible(false);
            fixed.setPrefWidth(elevationChart.getPrefWidth());
            fixed.setPrefHeight(elevationChart.getPrefHeight());

            javafx.scene.layout.VBox.setVgrow(fixed,
                javafx.scene.layout.VBox.getVgrow(elevationChart));

            if (elevationChart.getParent() instanceof javafx.scene.layout.Pane) {
                javafx.scene.layout.Pane parent =
                    (javafx.scene.layout.Pane) elevationChart.getParent();
                int idx = parent.getChildren().indexOf(elevationChart);
                parent.getChildren().set(idx, fixed);
                elevationChart = fixed;
            } else {
                return;
            }


            Platform.runLater(() ->
                elevationChart.lookupAll(".axis-label").forEach(node ->
                    node.setStyle("-fx-text-fill: white;"))
            );
        }

        MapRegion region = currentActivity.getSuggestedMap();
        if (region == null) return;
        MapProjection projection = new MapProjection(region, mapPane.getWidth(), mapPane.getHeight());

        buildElevationSamples(trackPoints, projection);
        populateElevationChart();
    }

    // =========================================================
    // Speed over the route
    // =========================================================

    /**
     * Calculates speed in km/h between two consecutive TrackPoints.
     * Reuse the TrackPoint already provided by the library.
     */


    private double segmentSpeedKmh(TrackPoint a, TrackPoint b) {
        if (a == null || b == null || a.getTime() == null || b.getTime() == null) return 0.0;
        try {
            return Math.max(0.0, a.speedTo(b));
        } catch (RuntimeException ex) {
            return 0.0;
        }
    }

    // Rojo es lento, amarillo es una velocidad media y verde es rápido.

    private Color speedToColor(double speed, double minSpeed, double maxSpeed) {
        if (maxSpeed <= minSpeed) return Color.YELLOW;
        double t = (speed - minSpeed) / (maxSpeed - minSpeed);
        double r = Math.max(0, 1.0 - 2 * t);
        double g = Math.min(1.0, 2 * t);
        return new Color(r, g, 0, 1.0);
    }
    
    // El color es según la velocidad relativa al rango de la actividad.
    // Si no se ven es porque están ocultos hasta que activéis la checkbox

    private void initializeSpeedOverlay(List<TrackPoint> points) {
        if (currentActivity == null || mapPane == null || points == null || points.size() < 2) return;
        if (elevationSamples.size() < 2 || segmentSpeeds.isEmpty()) return;

        if (!speedOverlayLines.isEmpty()) {
            mapPane.getChildren().removeAll(speedOverlayLines);
            speedOverlayLines.clear();
        }

        final double finalMin = minSegmentSpeedKmh;
        final double finalMax = maxSegmentSpeedKmh;

        for (int i = 0; i < segmentSpeeds.size() && (i + 1) < elevationSamples.size(); i++) {
            RouteSample from = elevationSamples.get(i);
            RouteSample to = elevationSamples.get(i + 1);

            Line seg = new Line(from.pixel.getX(), from.pixel.getY(), to.pixel.getX(), to.pixel.getY());
            seg.setStrokeWidth(4.0);
            seg.setStroke(speedToColor(segmentSpeeds.get(i), finalMin, finalMax));
            seg.setVisible(showSpeedOverlay != null && showSpeedOverlay.isSelected());

            final double segSpeed = segmentSpeeds.get(i);
            final int segIdx = i + 1;
            seg.setOnMouseEntered(e -> {
                if (segmentDetailsLabel != null)
                    segmentDetailsLabel.setText(
                        String.format("Segment %d — %.1f km/h", segIdx, segSpeed));
            });
            seg.setOnMouseExited(e -> {
                if (segmentDetailsLabel != null)
                    segmentDetailsLabel.setText("Move your mouse over the route to see the speed for each segment");
            });

            mapPane.getChildren().add(seg);
            speedOverlayLines.add(seg);
        }

        if (speedLegendBox != null) {
            speedLegendBox.getChildren().clear();
            speedLegendBox.setSpacing(0);
            speedLegendBox.setVisible(showSpeedOverlay != null && showSpeedOverlay.isSelected());
            int swatches = 20;
            Label slowLbl = new Label(String.format(" %.1f", finalMin));
            slowLbl.setStyle("-fx-font-size: 10px;");
            slowLbl.setTextFill(Color.WHITE);
            speedLegendBox.getChildren().add(slowLbl);
            for (int s = 0; s < swatches; s++) {
                double t = (double) s / (swatches - 1);
                Rectangle swatch = new Rectangle(9, 14);
                swatch.setFill(speedToColor(finalMin + t * (finalMax - finalMin), finalMin, finalMax));
                speedLegendBox.getChildren().add(swatch);
            }
            Label fastLbl = new Label(String.format(" %.1f km/h", finalMax));
            fastLbl.setStyle("-fx-font-size: 10px;");
            fastLbl.setTextFill(Color.WHITE);
            speedLegendBox.getChildren().add(fastLbl);
        }
    }

}