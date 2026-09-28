package ru.tripcost.view;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import ru.tripcost.model.domain.TransportType;
import ru.tripcost.model.journey.Journey;
import ru.tripcost.viewmodel.JourneyViewModel;
import ru.tripcost.viewmodel.JourneyViewModel.LegRow;

/**
 * View вкладки «Маршрут» (code-behind для journey-view.fxml).
 * Кнопки привязаны к командам ViewModel ({@link CommandBinder}): во View нет ни
 * обработчиков действий, ни логики доступности кнопок.
 */
public class JourneyView {

    private final JourneyViewModel viewModel = new JourneyViewModel();

    @FXML private ComboBox<String> fromBox;
    @FXML private ComboBox<String> toBox;
    @FXML private ComboBox<TransportType> transportBox;
    @FXML private Spinner<Integer> passengersSpinner;
    @FXML private Button addButton;
    @FXML private Button addReturnButton;
    @FXML private Button saveVersionButton;
    @FXML private Button removeButton;
    @FXML private Button upButton;
    @FXML private Button downButton;
    @FXML private Button changeTransportButton;
    @FXML private Button undoButton;
    @FXML private Button redoButton;
    @FXML private Button openInCalculatorButton;
    @FXML private TextField versionNameField;
    @FXML private ComboBox<Journey.Memento> versionBox;
    @FXML private Button restoreButton;
    @FXML private TableView<LegRow> table;
    @FXML private TableColumn<LegRow, String> numberColumn;
    @FXML private TableColumn<LegRow, String> fromColumn;
    @FXML private TableColumn<LegRow, String> toColumn;
    @FXML private TableColumn<LegRow, String> transportColumn;
    @FXML private TableColumn<LegRow, String> kmColumn;
    @FXML private TableColumn<LegRow, String> costColumn;
    @FXML private TableColumn<LegRow, String> noteColumn;
    @FXML private ListView<String> historyList;
    @FXML private Label totalLabel;
    @FXML private Label perPassengerLabel;
    @FXML private Label summaryLabel;
    @FXML private Label errorLabel;

    @FXML
    private void initialize() {
        fromBox.setItems(viewModel.getCities());
        fromBox.getEditor().textProperty().bindBidirectional(viewModel.fromProperty());
        toBox.setItems(viewModel.getCities());
        toBox.getEditor().textProperty().bindBidirectional(viewModel.toProperty());
        transportBox.setItems(viewModel.getTransportTypes());
        transportBox.valueProperty().bindBidirectional(viewModel.transportProperty());

        SpinnerValueFactory.IntegerSpinnerValueFactory factory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 200, 1);
        passengersSpinner.setValueFactory(factory);
        factory.valueProperty().bindBidirectional(viewModel.passengersProperty());
        factory.valueProperty().addListener((obs, o, n) -> viewModel.recalculate());

        numberColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().number()));
        fromColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().from()));
        toColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().to()));
        transportColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().transport()));
        kmColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().km()));
        costColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().cost()));
        noteColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().note()));
        table.setItems(viewModel.getRows());

        // выбор строки: таблица -> ViewModel и обратно (после команды ViewModel выбирает строку сама)
        table.getSelectionModel().selectedIndexProperty().addListener(
                (obs, o, n) -> viewModel.selectedIndexProperty().set(n.intValue()));
        viewModel.selectedIndexProperty().addListener((obs, o, n) -> {
            if (table.getSelectionModel().getSelectedIndex() != n.intValue()) {
                if (n.intValue() < 0) {
                    table.getSelectionModel().clearSelection();
                } else {
                    table.getSelectionModel().select(n.intValue());
                }
            }
        });

        // кнопки привязаны к командам ViewModel: действие и доступность задаёт ViewModel
        CommandBinder.bind(addButton, viewModel.addLegCommand());
        CommandBinder.bind(removeButton, viewModel.removeCommand());
        CommandBinder.bind(upButton, viewModel.moveUpCommand());
        CommandBinder.bind(downButton, viewModel.moveDownCommand());
        CommandBinder.bind(changeTransportButton, viewModel.changeTransportCommand());
        CommandBinder.bind(addReturnButton, viewModel.addReturnCommand());
        CommandBinder.bind(undoButton, viewModel.undoCommand());
        CommandBinder.bind(redoButton, viewModel.redoCommand());
        CommandBinder.bind(openInCalculatorButton, viewModel.openInCalculatorCommand());
        CommandBinder.bind(saveVersionButton, viewModel.saveVersionCommand());
        CommandBinder.bind(restoreButton, viewModel.restoreVersionCommand());

        Tooltip undoTip = new Tooltip();
        undoTip.textProperty().bind(viewModel.undoTextProperty());
        undoButton.setTooltip(undoTip);
        Tooltip redoTip = new Tooltip();
        redoTip.textProperty().bind(viewModel.redoTextProperty());
        redoButton.setTooltip(redoTip);

        historyList.setItems(viewModel.getHistoryItems());
        versionNameField.textProperty().bindBidirectional(viewModel.versionNameProperty());
        versionBox.setItems(viewModel.getVersions());
        versionBox.valueProperty().bindBidirectional(viewModel.selectedVersionProperty());
        totalLabel.textProperty().bind(viewModel.totalProperty());
        perPassengerLabel.textProperty().bind(viewModel.perPassengerProperty());
        summaryLabel.textProperty().bind(viewModel.summaryProperty());
        errorLabel.textProperty().bind(viewModel.errorMessageProperty());
        errorLabel.visibleProperty().bind(viewModel.errorMessageProperty().isNotEmpty());
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());
    }


    /** ViewModel вкладки - главное окно связывает вкладки через посредника. */
    public JourneyViewModel getViewModel() {
        return viewModel;
    }
}
