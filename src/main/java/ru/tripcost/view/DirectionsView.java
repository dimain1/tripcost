package ru.tripcost.view;

import javafx.beans.property.SimpleStringProperty;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import ru.tripcost.model.domain.TransportType;
import ru.tripcost.viewmodel.DirectionsViewModel;
import ru.tripcost.viewmodel.DirectionsViewModel.DirectionRow;

/**
 * View вкладки «Направления» (code-behind для directions-view.fxml).
 */
public class DirectionsView {

    private final DirectionsViewModel viewModel = new DirectionsViewModel();

    @FXML private ComboBox<String> originBox;
    @FXML private ComboBox<TransportType> transportBox;
    @FXML private Spinner<Integer> passengersSpinner;
    @FXML private CheckBox roundTripBox;
    @FXML private TableView<DirectionRow> table;
    @FXML private TableColumn<DirectionRow, String> cityColumn;
    @FXML private TableColumn<DirectionRow, String> regionColumn;
    @FXML private TableColumn<DirectionRow, String> kmColumn;
    @FXML private TableColumn<DirectionRow, String> totalColumn;
    @FXML private TableColumn<DirectionRow, String> perPassengerColumn;
    @FXML private TableColumn<DirectionRow, String> approvalColumn;
    @FXML private Label statsLabel;
    @FXML private Button addToJourneyButton;
    @FXML private Button showButton;
    @FXML private Button clearCacheButton;
    @FXML private Button cancelButton;
    @FXML private ProgressBar progressBar;
    @FXML private Label proxyStatsLabel;
    @FXML private Label errorLabel;

    @FXML
    private void initialize() {
        originBox.setItems(viewModel.getCities());
        originBox.valueProperty().bindBidirectional(viewModel.originProperty());
        transportBox.setItems(viewModel.getTransportTypes());
        transportBox.valueProperty().bindBidirectional(viewModel.transportProperty());
        roundTripBox.selectedProperty().bindBidirectional(viewModel.roundTripProperty());

        SpinnerValueFactory.IntegerSpinnerValueFactory factory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 200, 1);
        passengersSpinner.setValueFactory(factory);
        factory.valueProperty().bindBidirectional(viewModel.passengersProperty());

        cityColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().city()));
        regionColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().region()));
        kmColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().km()));
        totalColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().total()));
        perPassengerColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().perPassenger()));
        approvalColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().approval()));
        table.setItems(viewModel.getRows());
        table.getSelectionModel().selectedIndexProperty().addListener(
                (obs, o, n) -> viewModel.selectedIndexProperty().set(n.intValue()));
        CommandBinder.bind(addToJourneyButton, viewModel.addToJourneyCommand());
        CommandBinder.bind(clearCacheButton, viewModel.clearCacheCommand());
        CommandBinder.bind(cancelButton, viewModel.cancelCommand());
        // результаты фонового расчёта приходят в поток интерфейса JavaFX
        viewModel.setUiExecutor(Platform::runLater);
        progressBar.progressProperty().bind(viewModel.progressProperty());
        progressBar.visibleProperty().bind(viewModel.busyProperty());
        showButton.disableProperty().bind(viewModel.busyProperty());
        // «Показать»: сначала принять введённое в спиннер число, затем выполнить команду
        showButton.setOnAction(e -> {
            commitPassengers();
            viewModel.showCommand().execute();
        });

        statsLabel.textProperty().bind(viewModel.statsProperty());
        proxyStatsLabel.textProperty().bind(viewModel.proxyStatsProperty());
        errorLabel.textProperty().bind(viewModel.errorMessageProperty());
        errorLabel.visibleProperty().bind(viewModel.errorMessageProperty().isNotEmpty());
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());
    }

    private void commitPassengers() {
        try {
            passengersSpinner.commitValue();
        } catch (NumberFormatException e) {
            passengersSpinner.cancelEdit();
        }
    }

    /** ViewModel вкладки - главное окно связывает вкладки через посредника. */
    public DirectionsViewModel getViewModel() {
        return viewModel;
    }
}
