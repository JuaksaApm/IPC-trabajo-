/*
 * ============================================================
 *  PROYECTO EJEMPLO – IPC 2026
 *  Asignatura: Interfaces Persona-Computador
 *  Universitat Politècnica de València
 * ============================================================
 *
 *  DESCRIPCIÓN GENERAL
 *  -------------------
 *  Este controlador gestiona la vista principal de la aplicación
 *  de puntos de interés (POI) sobre un mapa.
 *
 *  Funcionalidades implementadas:
 *   1. Carga y visualización de una imagen de mapa.
 *   2. Zoom interactivo mediante un Slider.
 *   3. Añadir POIs (texto) y anotaciones (círculos) con clic derecho.
 *   4. Listado de POIs en un ListView con CellFactory personalizada.
 *   5. Centrado animado del mapa al seleccionar un POI de la lista.
 *   6. Modo inserción: activar con botón y colocar POI con siguiente clic.
 *
 *  PATRÓN UTILIZADO: MVC (Model-View-Controller)
 *   - Modelo : clase Poi  (datos del punto de interés)
 *   - Vista  : FXMLDocument.fxml  (layout declarativo)
 *   - Control: esta clase (lógica de interacción)
 *
 * ============================================================
 */
package mapademo;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
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
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
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
import javafx.util.Duration;
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

    /**
     * Botón de pin visible sobre el mapa.
     * Se desplaza hasta la posición del POI seleccionado en la lista.
     */
    private MenuButton map_pin;

    // FIX 5 — Eliminadas las variables sin uso:
    //   · 'mousePosistion' (errata + duplicado de mousePosition)
    //   · 'pin_info'       (inyectada pero nunca actualizada)

    /** Etiqueta en la barra de estado que muestra las coordenadas del ratón. */
    private int annotationIndexCounter = 1;
    private Activity currentActivity;
    
    @FXML
    private Label mousePosition;
    @FXML
    private SplitPane splitPane;
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
                // Clic derecho → mostrar menú contextual
                onMapRightClick(e.getX(), e.getY());

            } else if (e.getButton() == MouseButton.PRIMARY && insertionMode) {
                // FIX 2: clic izquierdo en modo inserción → añadir POI y desactivar modo
                insertionMode = false;
                mapPane.setStyle(""); // Restauramos el cursor normal
                addPoi(e.getX(), e.getY());
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
        // FIX 6: cerramos el menú si ya estaba visible (evita instancias flotantes)
        mapContextMenu.hide();

        // Actualizamos las acciones de los items con las coordenadas actuales.
        // Usamos variables final para que el lambda pueda capturarlas.
        final double clickX = x;
        final double clickY = y;
        mapContextMenu.getItems().get(0).setOnAction(e -> promptForAnnotation(clickX, clickY));
        mapContextMenu.getItems().get(1).setOnAction(e -> promptForAnnotation(clickX, clickY));

        // Mostramos el menú en coordenadas de pantalla
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

        // Los items se crean aquí sin acción; las acciones se asignan
        // en onMapRightClick() con las coordenadas correctas de cada clic.
        MenuItem miText   = new MenuItem("📝 Añadir texto");
        MenuItem miCircle = new MenuItem("⭕ Añadir círculo");
        mapContextMenu = new ContextMenu(miText, miCircle);

               //  setCellFactory() define cómo se renderiza cada celda
        //  de forma independiente al modelo Poi.
        //  Aquí mostramos "CÓDIGO – Nombre" en cada fila.

        // ── Carga del mapa inicial ─────────────────────────────────────
        // El fichero se busca relativo al directorio de trabajo del proyecto.
        buildMap(new File("maps/upv.jpg"));
        
        map_annotations_listview.setCellFactory(listView -> new ListCell<Annotation>() {
            @Override
            protected void updateItem(Annotation ann, boolean empty) {
                super.updateItem(ann, empty);
                if (empty || ann == null) {
                    setText(null);
                } else {
                    // Display the layout marker index alongside the descriptive text
                    setText("[" + ann.getId() + "] " + ann.getType() + ": " + ann.getText());
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
    //  AÑADIR UN POI (texto) AL MAPA
    // =========================================================

    /**
     * Muestra un diálogo para introducir el nombre del nuevo POI,
     * lo añade al ListView y dibuja su etiqueta sobre el mapa.
     *
     * @param x coordenada X del clic en el sistema local del mapPane
     * @param y coordenada Y del clic en el sistema local del mapPane
     */
    private void addPoi(double x, double y) {

        // ── Construcción del diálogo personalizado ────────────────────
        Dialog<Poi> poiDialog = new Dialog<>();
        poiDialog.setTitle("Nuevo POI");
        poiDialog.setHeaderText("Introduce un nuevo POI");

        // Personalizamos el icono de la ventana del diálogo
        Stage dialogStage = (Stage) poiDialog.getDialogPane().getScene().getWindow();
        dialogStage.getIcons().add(
            new Image(getClass().getResourceAsStream("/resources/logo.png"))
        );

        // Botones del diálogo: Aceptar y Cancelar
        ButtonType okButton = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
        poiDialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        // Campo de texto para el nombre del POI
        TextField nameField = new TextField();
        nameField.setPromptText("Nombre del POI");

        // Layout del contenido del diálogo (VBox con espaciado de 10 px)
        VBox vbox = new VBox(10, new Label("Nombre:"), nameField);
        poiDialog.getDialogPane().setContent(vbox);

        // ResultConverter: transforma la selección del botón en un objeto Poi.
        // FIX 1: ya no usamos coordenadas provisionales (0,0); pasamos (x,y)
        // directamente al constructor para que el modelo sea coherente desde el inicio.
        poiDialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButton) {
                return new Poi(nameField.getText().trim(), x, y);
            }
            return null;
        });

        // Mostramos el diálogo y esperamos la respuesta del usuario
        Optional<Poi> result = poiDialog.showAndWait();

        if (result.isPresent()) {
            Poi poi = result.get();

            // FIX 1: confirmamos la posición como Point2D para compatibilidad
            // con getPosition(), usando las mismas coordenadas (x, y).
            poi.setPosition(new Point2D(x, y));

            // Añadimos el POI al ListView (la CellFactory mostrará nombre y código)
            //map_annotations_listview.getItems().add(poi);

            // FIX 1: usamos (x, y) tanto para el modelo como para el Text,
            // garantizando que la etiqueta aparezca exactamente donde se hizo clic.
            Text text = new Text(poi.getCode());
            text.setX(x);
            text.setY(y);
            mapPane.getChildren().add(text);
        }
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

    // =========================================================
    //  AÑADIR UN CÍRCULO AL MAPA
    // =========================================================

    /**
     * Dibuja un círculo rojo de radio 10 px en la posición indicada.
     *
     * Ejemplo sencillo de cómo añadir formas vectoriales (Shape) sobre el mapa.
     * Los alumnos pueden extenderlo para:
     *  - Elegir color dinámicamente.
     *  - Asociar información al círculo (tooltip, popup, etc.).
     *  - Permitir moverlo con arrastrar y soltar (drag and drop).
     *
     * @param x coordenada X en el sistema local del mapPane
     * @param y coordenada Y en el sistema local del mapPane
     */
    private void addCircle(double x, double y) {
        Circle circle = new Circle(10, Color.RED); // radio = 10 px, color = rojo
        circle.setCenterX(x);
        circle.setCenterY(y);
        mapPane.getChildren().add(circle); // Se añade sobre el mapa como cualquier nodo
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
        this.annotationIndexCounter = 1;

        // 12. Scenario 4.3 Compliance: Extract historical records and trace them dynamically onto the canvas view
        for (Annotation ann : activity.getAnnotations()) {
            renderSingleAnnotation(ann);
            map_annotations_listview.getItems().add(ann);
        }
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
     * Plots a clean, visually structured number index symbol marker on the map canvas area.
     */
    private void drawMarkerSymbolOnCanvas(double x, double y, int indexNumber) {
        // Build a background base layout bubble wrapper
        Circle badge = new Circle(11, Color.web("#E74C3C"));
        badge.setCenterX(x);
        badge.setCenterY(y);
        badge.setStroke(Color.WHITE);
        badge.setStrokeWidth(1.5);

        // Center the textual index directly within the coordinate badge circle
        Text text = new Text(String.valueOf(indexNumber));
        text.setFill(Color.WHITE);
        text.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        text.setX(x - 4); // Micro horizontal adjustment spacing
        text.setY(y + 4); // Micro vertical adjustment spacing

        // Superimpose visual nodes cleanly onto the canvas Pane layer
        mapPane.getChildren().addAll(badge, text);
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
                // Scenario 4.2 Point Specification: Plot a distinct filled tracking circle indicator node
                Circle pointMarker = new Circle(p1.getX(), p1.getY(), 7, renderColor);
                pointMarker.setStroke(Color.WHITE);
                pointMarker.setStrokeWidth(1.5);
                mapPane.getChildren().add(pointMarker);
                break;

            case TEXT:
                // Scenario 4.2 Text Specification: Render a clean descriptive context text node anchored at pixels
                Text textLabel = new Text(p1.getX(), p1.getY() - 8, ann.getText());
                textLabel.setFill(renderColor);
                textLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
                mapPane.getChildren().add(textLabel);
                break;

            case LINE:
                // Scenario 4.2 Line Specification: Render a linear path connector segment between points 
                if (ann.getGeoPoints().size() >= 2) {
                    Point2D p2 = proj.project(ann.getGeoPoints().get(1));
                    Polyline lineSegment = new Polyline(p1.getX(), p1.getY(), p2.getX(), p2.getY());
                    lineSegment.setStroke(renderColor);
                    lineSegment.setStrokeWidth(ann.getStrokeWidth());
                    mapPane.getChildren().add(lineSegment);
                }
                break;

            case CIRCLE:
                // Scenario 4.2 Circle Specification: Measure distance radius offset to anchor edge boundaries 
                double radiusPx = 25.0; // Default rendering boundary width offset context fallback
                if (ann.getGeoPoints().size() >= 2) {
                    Point2D p2 = proj.project(ann.getGeoPoints().get(1));
                    radiusPx = p1.distance(p2); // Compute pixel distance between center and edge tracking anchors
                }
                Circle geographicArea = new Circle(p1.getX(), p1.getY(), radiusPx, Color.TRANSPARENT);
                geographicArea.setStroke(renderColor);
                geographicArea.setStrokeWidth(ann.getStrokeWidth());
                mapPane.getChildren().add(geographicArea);
                break;
        }
    }

}
