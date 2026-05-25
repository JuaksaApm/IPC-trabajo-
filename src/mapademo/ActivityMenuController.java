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
import javafx.stage.FileChooser;
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


    /**
     * Indica si el controlador está en modo inserción de POI.
     * {@code true} → el próximo clic izquierdo sobre el mapa abre el diálogo.
     */
    private boolean insertionMode = false;

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
    //  SELECCIÓN EN EL LISTVIEW → CENTRADO EN EL MAPA
    // =========================================================

    /**
     * Se ejecuta cuando el usuario hace clic en un elemento del ListView.
     *
     * Objetivo: centrar el ScrollPane sobre la posición del POI seleccionado
     * con una animación suave de 500 ms, y mover el pin al punto.
     *
     * Cálculo del scroll
     * ------------------
     * El ScrollPane expresa su posición como valores normalizados [0, 1]:
     *   · hValue = 0 → extremo izquierdo
     *   · hValue = 1 → extremo derecho
     *
     * Para centrar el POI necesitamos:
     *
     *   scrollH = (poiX_escalado - viewportAncho / 2)
     *             ─────────────────────────────────────
     *             (mapaAncho_escalado - viewportAncho)
     *
     * Aplicamos clamp para no salir del rango [0, 1].
     *
     * @param event evento de ratón sobre el ListView
     */
    @FXML
    void listClicked(MouseEvent event) {
        // 1. Fetch the annotation object from the list row selection
        Annotation itemSelected = map_annotations_listview.getSelectionModel().getSelectedItem();
        if (itemSelected == null || itemSelected.getGeoPoints().isEmpty()) return;

        // 2. Unpack the geographic GPS anchor coordinates 
        GeoPoint geo = itemSelected.getGeoPoints().get(0);

        // 3. Translate the coordinates into absolute map pixels 
        MapRegion region = currentActivity.getSuggestedMap();
        MapProjection proj = new MapProjection(region, mapPane.getWidth(), mapPane.getHeight());
        Point2D pixel = proj.project(geo);

        // 4. Center your viewport scrollbars right over the calculated location
        centerMapOnCoordinates(pixel.getX(), pixel.getY());
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
        mapPane.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                onMapRightClick(e.getX(), e.getY());
            }
        });

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

        // ── Configuración del slider de zoom ──────────────────────────
        zoom_slider.setMin(0.5);   // zoom mínimo: 50 %
        zoom_slider.setMax(1.5);   // zoom máximo: 150 %
        zoom_slider.setValue(1.0); // valor inicial: 100 %

        // Listener que invoca zoom() cada vez que el slider cambia de valor.
        // Usamos una expresión lambda en lugar de una clase anónima por brevedad.
        zoom_slider.valueProperty().addListener(
            (observable, oldVal, newVal) -> zoom((Double) newVal)
        );

        MenuItem miText = new MenuItem("📝 Add annotation");
        mapContextMenu = new ContextMenu(miText);

        // ── ListView: right-click to remove an annotation ──────────────
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

        // ── Carga del mapa inicial ─────────────────────────────────────
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

        // Add a click listener to the list to center the map viewport on the selected marker
        map_annotations_listview.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.getGeoPoints().isEmpty()) {
                // Fetch the first geographic anchor point of the saved marker
                GeoPoint geo = newVal.getGeoPoints().get(0);
                
                // Reconstruct or access the active projection matrix to map pixels
                MapRegion currentRegion = currentActivity.getSuggestedMap();
                MapProjection proj = new MapProjection(currentRegion, mapPane.getWidth(), mapPane.getHeight());
                Point2D pixel = proj.project(geo);
                
                // Smoothly focus the layout scroll bars right over the target coordinate area
                centerMapOnCoordinates(pixel.getX(), pixel.getY());
            }
        });
        
        // =========================================================
        // 6.1 - Elevation profile 
        // =========================================================

        if (elevationChart != null) {
            elevationChart.setCreateSymbols(false);
            elevationChart.setLegendVisible(false);
        }

        if (elevationLabel != null) {
            elevationLabel.setText("Move the mouse over the profile to inspect the route");
        }

        // =========================================================
        // 6.2 - Speed over the route
        // =========================================================

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
        stage.setMinWidth(1315);
        stage.setMinHeight(900);
    });
    }

    // =========================================================
    //  INDICADOR DE POSICIÓN DEL RATÓN
    // =========================================================

    /**
     * Actualiza la etiqueta {@code mousePosition} con las coordenadas
     * actuales del ratón, tanto en el sistema de la escena como en el
     * sistema local del nodo sobre el que se mueve.
     *
     * Útil para depuración y para que los alumnos comprendan la diferencia
     * entre coordenadas de escena y coordenadas locales.
     *
     * @param event evento de movimiento del ratón
     */
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

    /**
     * Muestra un diálogo informativo con datos de la asignatura.
     *
     * Nota: accedemos al Stage del diálogo para poder personalizar
     * su icono, ya que Alert no expone directamente esa propiedad.
     *
     * @param event evento de acción del menú
     */
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

    // =========================================================
    //  CAMBIAR EL MAPA (selector de fichero)
    // =========================================================

    /**
     * Abre un selector de fichero para que el usuario elija una imagen
     * diferente como mapa y reconstruye toda la vista.
     *
     * FIX 3: se comprueba que imgFile no sea null antes de usarlo,
     * evitando NullPointerException cuando el usuario cierra el FileChooser
     * sin seleccionar ningún fichero.
     *
     * @param event evento de acción del menú
     * @throws IOException si hay un problema al obtener la ruta canónica
     */
    @FXML
    private void cambiarMapa(ActionEvent event) throws IOException {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File(".")); // Empezamos en el directorio del proyecto

        File imgFile = fc.showOpenDialog(zoom_slider.getScene().getWindow());

        // FIX 3: showOpenDialog() devuelve null si el usuario cancela la selección
        if (imgFile != null) {
            System.out.println("Mapa seleccionado: " + imgFile.getCanonicalPath());
            buildMap(imgFile); // Reconstruimos la vista con la nueva imagen
            map_annotations_listview.getItems().clear(); // Borramos los datos del mapa anterior
        }
    }

    @FXML
    private void handleGoBack(ActionEvent event) {
        try {
            // 1. Load the Menu FXML layout file
            // Note: Ensure the string path matches your file structure capitalization perfectly (e.g., "Menu.fxml")
            Parent menuRoot = FXMLLoader.load(getClass().getResource("/mapademo/Menu.fxml"));
            
            // 2. Get the current active Stage (window) using the button's scene reference
            Stage stage = (Stage) map_scrollpane.getScene().getWindow();
            
            // 3. Swap the root scene view to return to your main dashboard
            Scene scene = new Scene(menuRoot);
            stage.setScene(scene);
            stage.show();
            
        } catch (IOException e) {
            System.err.println("Error reloading Menu view: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void loadActivityTrack(Activity activity) {
        if (activity == null) return;

        // 1. Fetch the recommended map region computed by the library database
        MapRegion region = activity.getSuggestedMap();
        if (region == null) {
            System.err.println("No matching map region found for this activity coordinates.");
            return;
        }

        // 2. Verify that the map background image file exists locally
        File imgFile = new File(region.getImagePath());
        if (!imgFile.exists()) {
            map_scrollpane.setContent(
                new Label("Map image file not found at: " + region.getImagePath()));
            return;
        }

        // 3. Load the image and extract its precise dimensions
        Image img = new Image(imgFile.toURI().toString());
        double W = img.getWidth();
        double H = img.getHeight();

        // 4. Initialize the map container Pane with the exact image bounds
        mapPane = new Pane();
        mapPane.setPrefSize(W, H);
        mapPane.setMinSize(W, H);  
        mapPane.setMaxSize(W, H);  

        ImageView iv = new ImageView(img);
        iv.setFitWidth(W);
        iv.setFitHeight(H);
        mapPane.getChildren().add(iv);

        // Reattach your mouse canvas click handlers
        mapPane.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                onMapRightClick(e.getX(), e.getY());
            } else if (e.getButton() == MouseButton.PRIMARY && insertionMode) {
                insertionMode = false;
                mapPane.setStyle(""); 
                // FIXED: Points to your multi-type prompt system instead of legacy POI text
                promptForAnnotation(e.getX(), e.getY());
            }
        });

        // 5. Initialize the library projection engine mapping GPS coordinates to image pixels
        MapProjection proj = new MapProjection(region, W, H);

        // 6. Project and trace the complete route path using a JavaFX Polyline
        Polyline route = new Polyline();
        route.setStroke(Color.BLUE);     // Route trace outline styling
        route.setStrokeWidth(3.0);
        route.setFill(Color.TRANSPARENT); // FIXED: Stops JavaFX from drawing a solid black shape blob

        for (TrackPoint tp : activity.getTrackPoints()) {
            Point2D p = proj.project(tp); // Web Mercator conversion projection algorithm
            route.getPoints().addAll(p.getX(), p.getY());
        }
        mapPane.getChildren().add(route); // Add the complete vector path onto the canvas 

        // 7. Re-assemble the zoom group structural architecture node tree hierarchy
        zoomGroup = new Group();
        Group contentGroup = new Group();
        zoomGroup.getChildren().add(mapPane);
        contentGroup.getChildren().add(zoomGroup);

        // Scale the graphic layout node using the current active zoom slider value
        double currentZoom = zoom_slider.getValue();
        zoomGroup.setScaleX(currentZoom);
        zoomGroup.setScaleY(currentZoom);

        map_scrollpane.setContent(contentGroup);

        // 8. Place visual start (green) and end (red) position markers on the canvas map trace
        if (!activity.getTrackPoints().isEmpty()) {
            Point2D startPixel = proj.project(activity.getStartPoint());
            Point2D endPixel = proj.project(activity.getEndPoint());

            Circle startMarker = new Circle(startPixel.getX(), startPixel.getY(), 6, Color.GREEN);
            Circle endMarker = new Circle(endPixel.getX(), endPixel.getY(), 6, Color.RED);

            mapPane.getChildren().addAll(startMarker, endMarker);

            // Automatically focus the scroll bars over the track starting point
            centerMapOnCoordinates(startPixel.getX(), startPixel.getY());
        }

        // 9. Update your right sidebar GridPane with the data values
        displayActivityStatistics(activity);

        // 10. Track globally which activity is active inside your class instance
        this.currentActivity = activity; 

        // 11. Wipe previous layout records from the side panel view before rendering anew
        map_annotations_listview.getItems().clear();
        annotationNodes.clear();

        // 12. Scenario 4.3 Compliance: Extract historical records and trace them dynamically onto the canvas view
        for (Annotation ann : activity.getAnnotations()) {
            renderSingleAnnotation(ann);
            map_annotations_listview.getItems().add(ann);
        }

        // Reset speed overlay state for the new activity
        speedOverlayLines.clear();
        if (showSpeedOverlay != null) showSpeedOverlay.setSelected(false);
        if (segmentDetailsLabel != null) segmentDetailsLabel.setText("");

        // 6.1 & 6.2 – Defer until JavaFX has done a layout pass so
        // mapPane.getWidth()/getHeight() return real values (not 0.0).
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

        double scrollH = (x * zoomGroup.getScaleX() - viewW / 2) / (mapWidth  - viewW);
        double scrollV = (y * zoomGroup.getScaleY() - viewH / 2) / (mapHeight - viewH);

        map_scrollpane.setHvalue(Math.max(0, Math.min(1, scrollH)));
        map_scrollpane.setVvalue(Math.max(0, Math.min(1, scrollV)));
    }
    
    /**
     * Extracts geographic and performance metrics from the activity 
     * and populates the statistics GridPane layout labels.
     */
    private void displayActivityStatistics(Activity activity) {
        if (activity == null) return;

        // 1. Convert total distance from meters to kilometers
        double distanceKm = activity.getTotalDistance() / 1000.0;
        lblDistance.setText(String.format("%.2f km", distanceKm));

        // 2. Format the duration into a standard HH:MM:SS time string
        long totalSeconds = activity.getDuration().getSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long secs = totalSeconds % 60;
        lblDuration.setText(String.format("%02d:%02d:%02d", hours, minutes, secs));

        // 3. Display average speed directly in km/h
        lblSpeed.setText(String.format("%.2f km/h", activity.getAverageSpeed()));

        // 4. Display average pace directly in minutes per kilometer
        lblPace.setText(String.format("%.2f min/km", activity.getAveragePace()));

        // 5. Display cumulative positive ascent and negative descent meters
        lblGain.setText(String.format("+%.0f m", activity.getElevationGain()));
        lblLoss.setText(String.format("-%.0f m", activity.getElevationLoss()));

        // 6. Display maximum and minimum recorded altitudes above sea level
        lblMaxAltitude.setText(String.format("%.0f m", activity.getMaxElevation()));
        lblMinAltitude.setText(String.format("%.0f m", activity.getMinElevation()));
    }
    
    /**
     * Scenario 4.2 Compliance: Prompts the user to select an annotation type,
     * description, and color, then registers the object to the database .
     */
    private void promptForAnnotation(double x, double y) {
        if (currentActivity == null) return;

        // 1. Create a custom multi-field Dialog layout container
        Dialog<Annotation> dialog = new Dialog<>();
        dialog.setTitle("New Geographic Annotation");
        dialog.setHeaderText("Configure your route annotation details:");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // 2. Dropdown choice selector for Annotation Types 
        ChoiceBox<AnnotationType> typeSelector = new ChoiceBox<>(
            FXCollections.observableArrayList(AnnotationType.values())
        );
        typeSelector.setValue(AnnotationType.TEXT); // Default selection fallback

        // 3. Text field for descriptions 
        TextField textInput = new TextField();
        textInput.setPromptText("Enter notes or label text...");

        // 4. Color selection tool 
        ColorPicker colorSelector = new ColorPicker(Color.RED);

        // Assemble fields vertically using a standard spacing VBox layout container
        VBox dialogContent = new VBox(12);
        dialogContent.getChildren().addAll(
            new Label("Annotation Type:"), typeSelector,
            new Label("Description / Label Text:"), textInput,
            new Label("Display Color:"), colorSelector
        );
        dialog.getDialogPane().setContent(dialogContent);

        // 5. Convert dialog input data into a concrete library Annotation instance on click
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                AnnotationType selectedType = typeSelector.getValue();
                String enteredText = textInput.getText().trim();
                
                // Convert JavaFX Color object into standard CSS HEX String format (#RRGGBB)
                Color c = colorSelector.getValue();
                String hexColor = String.format("#%02X%02X%02X", 
                    (int)(c.getRed() * 255), 
                    (int)(c.getGreen() * 255), 
                    (int)(c.getBlue() * 255)
                );

                // Unproject layout click pixels back to real geographic coordinates
                MapRegion region = currentActivity.getSuggestedMap();
                MapProjection proj = new MapProjection(region, mapPane.getWidth(), mapPane.getHeight());
                GeoPoint primaryPoint = proj.unproject(x, y);

                // Generate the required list array of GeoPoints based on specification criteria 
                List<GeoPoint> pointsList = new ArrayList<>();
                pointsList.add(primaryPoint); // First anchor point 

                // If type is LINE or CIRCLE, it strictly requires 2 distinct points 
                if (selectedType == AnnotationType.LINE || selectedType == AnnotationType.CIRCLE) {
                    // Create a secondary point shifted slightly offset so it renders cleanly
                    GeoPoint secondaryPoint = new GeoPoint(
                        primaryPoint.getLatitude() - 0.002, 
                        primaryPoint.getLongitude() + 0.002
                    );
                    pointsList.add(secondaryPoint); // Second anchor point 
                }

                // Instantiate and return the formal collection instance
                return new Annotation(
                    selectedType,
                    enteredText,
                    hexColor,
                    3.0, // Standard trace outline pixel thickness scale
                    pointsList
                );
            }
            return null;
        });

        // 6. Execute dialog and save valid outputs directly to the SQLite persistence database
        Optional<Annotation> result = dialog.showAndWait();
        if (result.isPresent()) {
            Annotation savedAnnotation = SportActivityApp.getInstance().addAnnotation(currentActivity, result.get());
            if (savedAnnotation != null) {
                // Refresh the layout elements immediately on successful storage save
                renderSingleAnnotation(savedAnnotation);
                map_annotations_listview.getItems().add(savedAnnotation);
            }
        }
    }

    /**
     * Evaluates an annotation object data profile and renders its corresponding 
     * vector shapes or text labels dynamically onto the map canvas layout.
     */
    private void renderSingleAnnotation(Annotation ann) {
        if (ann == null || ann.getGeoPoints().isEmpty()) return;

        // Fetch active workspace projection metrics matrices
        MapRegion region = currentActivity.getSuggestedMap();
        MapProjection proj = new MapProjection(region, mapPane.getWidth(), mapPane.getHeight());

        // Project the primary point coordinate down to screen space absolute pixels
        Point2D p1 = proj.project(ann.getGeoPoints().get(0));
        Color renderColor = Color.web(ann.getColor()); // Parse saved hex color string

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
                if (ann.getGeoPoints().size() >= 2) {
                    Point2D p2 = proj.project(ann.getGeoPoints().get(1));
                    Polyline lineSegment = new Polyline(p1.getX(), p1.getY(), p2.getX(), p2.getY());
                    lineSegment.setStroke(renderColor);
                    lineSegment.setStrokeWidth(ann.getStrokeWidth());
                    mapPane.getChildren().add(lineSegment);
                    annotationNodes.add(lineSegment);
                }
                break;

            case CIRCLE:
                double radiusPx = 25.0;
                if (ann.getGeoPoints().size() >= 2) {
                    Point2D p2 = proj.project(ann.getGeoPoints().get(1));
                    radiusPx = p1.distance(p2);
                }
                Circle geographicArea = new Circle(p1.getX(), p1.getY(), radiusPx, Color.TRANSPARENT);
                geographicArea.setStroke(renderColor);
                geographicArea.setStrokeWidth(ann.getStrokeWidth());
                mapPane.getChildren().add(geographicArea);
                annotationNodes.add(geographicArea);
                break;
        }
    }
    
    /**
     * Removes all annotation visuals from the map and redraws them
     * from the current activity's annotation list. Called after a deletion.
     */
    private void reloadAnnotationsOnMap() {
        mapPane.getChildren().removeAll(annotationNodes);
        annotationNodes.clear();
        for (Annotation ann : map_annotations_listview.getItems()) {
            renderSingleAnnotation(ann);
        }
    }

    // =========================================================
    // =========================================================
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
     *
     * In the same pass, the segment speeds are cached so the speed overlay
     * does not need to recompute route geometry again.
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
                long seconds = java.time.Duration.between(previous.getTime(), current.getTime()).getSeconds();

                if (seconds > 0 && speedKmh > 0.0) {
                    cumulativeKm += speedKmh * (seconds / 3600.0);
                    minSegmentSpeedKmh = Math.min(minSegmentSpeedKmh, speedKmh);
                    maxSegmentSpeedKmh = Math.max(maxSegmentSpeedKmh, speedKmh);
                }

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

    private void populateElevationChart() { // to draw the samples in the graph
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
                elevationLabel.setText("");
            }
        });
    }

    /**
     * Busca la muestra cuya distancia acumulada esté más cerca
     * del valor X del gráfico.
     *
     * The samples are generated in route order, so a binary search is enough.
     */
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

    /**
     * Get the mouse position and translate to coordinates over the map.
     */
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
        if (currentActivity == null || mapPane == null || trackPoints == null || trackPoints.size() < 2) {
            return;
        }

        // Scene Builder always creates a LineChart with CategoryAxis on X.
        // We need NumberAxis on both axes for numeric km/altitude data.
        // Fix it at runtime so the FXML never needs to be hand-edited.
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

            // Preserve layout constraints (VBox.vgrow, etc.)
            javafx.scene.layout.VBox.setVgrow(fixed,
                javafx.scene.layout.VBox.getVgrow(elevationChart));

            // Swap in the parent container
            javafx.scene.layout.Pane parent =
                (javafx.scene.layout.Pane) elevationChart.getParent();
            int idx = parent.getChildren().indexOf(elevationChart);
            parent.getChildren().set(idx, fixed);
            elevationChart = fixed;

            // Axis title labels ("Distance (km)", "Altitude (m)") are internal
            // Text nodes only reachable via CSS lookup after the chart is in the scene.
            Platform.runLater(() ->
                elevationChart.lookupAll(".axis-label").forEach(node ->
                    node.setStyle("-fx-text-fill: white;"))
            );
        }

        MapRegion region = currentActivity.getSuggestedMap();
        MapProjection projection = new MapProjection(region, mapPane.getWidth(), mapPane.getHeight());

        buildElevationSamples(trackPoints, projection);
        populateElevationChart();
    }

    // =========================================================
    // Speed over the route
    // =========================================================

    /**
     * Calculates speed in km/h between two consecutive TrackPoints.
     * Reuses the TrackPoint helper already provided by the library.
     */
    private double segmentSpeedKmh(TrackPoint a, TrackPoint b) {
        if (a == null || b == null) return 0.0;
        return Math.max(0.0, a.speedTo(b));
    }

    /**
     * Interpola un color entre rojo (lento) → amarillo → verde (rápido)
     * según la velocidad relativa al rango de la actividad.
     */
    private Color speedToColor(double speed, double minSpeed, double maxSpeed) {
        if (maxSpeed <= minSpeed) return Color.YELLOW;
        double t = (speed - minSpeed) / (maxSpeed - minSpeed); // 0 = slow, 1 = fast
        double r = Math.max(0, 1.0 - 2 * t);
        double g = Math.min(1.0, 2 * t);
        return new Color(r, g, 0, 1.0);
    }

    /**
     * Construye los segmentos de línea coloreados por velocidad,
     * los superpone al mapa, y rellena la leyenda y el resumen.
     *
     * Los segmentos empiezan ocultos; el CheckBox los muestra/oculta.
     */
    private void initializeSpeedOverlay(List<TrackPoint> points) {
        if (currentActivity == null || mapPane == null || points == null || points.size() < 2) return;
        if (elevationSamples.size() < 2 || segmentSpeeds.isEmpty()) return;

        if (!speedOverlayLines.isEmpty()) {
            mapPane.getChildren().removeAll(speedOverlayLines);
            speedOverlayLines.clear();
        }

        final double finalMin = minSegmentSpeedKmh;
        final double finalMax = maxSegmentSpeedKmh;

        // 2. Dibujar un Line coloreado por tramo usando la geometría ya calculada
        for (int i = 0; i < segmentSpeeds.size() && (i + 1) < elevationSamples.size(); i++) {
            RouteSample from = elevationSamples.get(i);
            RouteSample to = elevationSamples.get(i + 1);

            Line seg = new Line(from.pixel.getX(), from.pixel.getY(), to.pixel.getX(), to.pixel.getY());
            seg.setStrokeWidth(4.0);
            seg.setStroke(speedToColor(segmentSpeeds.get(i), finalMin, finalMax));
            seg.setVisible(false);

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

        // 3. Legend (hidden until the checkbox is enabled)
        if (speedLegendBox != null) {
            speedLegendBox.getChildren().clear();
            speedLegendBox.setSpacing(0);
            speedLegendBox.setVisible(false);
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