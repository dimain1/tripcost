package ru.tripcost.view;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import ru.tripcost.model.request.RequestAction;
import ru.tripcost.viewmodel.RequestsViewModel;
import ru.tripcost.viewmodel.RequestsViewModel.RequestRow;

/**
 * View вкладки «Заявки» (code-behind для requests-view.fxml).
 * Кнопка на каждое действие; доступность кнопок задаёт состояние выбранной заявки.
 */
public class RequestsView {

    private final RequestsViewModel viewModel = new RequestsViewModel();

    @FXML private FlowPane actionsPane;
    @FXML private TextField commentField;
    @FXML private TableView<RequestRow> table;
    @FXML private TableColumn<RequestRow, String> idColumn;
    @FXML private TableColumn<RequestRow, String> routeColumn;
    @FXML private TableColumn<RequestRow, String> totalColumn;
    @FXML private TableColumn<RequestRow, String> approverColumn;
    @FXML private TableColumn<RequestRow, String> stateColumn;
    @FXML private ListView<String> journalList;
    @FXML private ListView<String> notificationsList;
    @FXML private Label errorLabel;
    @FXML private CheckBox ticketsFailBox;
    @FXML private CheckBox hotelsFailBox;
    @FXML private CheckBox insuranceFailBox;
    @FXML private Label budgetLabel;
    @FXML private ListView<String> sagaList;

    @FXML
    private void initialize() {
        for (RequestAction action : RequestAction.values()) {
            Button button = new Button(action.title());
            CommandBinder.bind(button, viewModel.command(action));
            actionsPane.getChildren().add(button);
        }
        commentField.textProperty().bindBidirectional(viewModel.commentProperty());

        idColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().id()));
        routeColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().route()));
        totalColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().total()));
        approverColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().approver()));
        stateColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().state()));
        table.setItems(viewModel.getRows());

        table.getSelectionModel().selectedIndexProperty().addListener(
                (obs, o, n) -> viewModel.selectedIndexProperty().set(n.intValue()));
        viewModel.selectedIndexProperty().addListener((obs, o, n) -> {
            if (n.intValue() >= 0 && table.getSelectionModel().getSelectedIndex() != n.intValue()) {
                table.getSelectionModel().select(n.intValue());
            }
        });

        ticketsFailBox.selectedProperty().bindBidirectional(viewModel.ticketsFailingProperty());
        hotelsFailBox.selectedProperty().bindBidirectional(viewModel.hotelsFailingProperty());
        insuranceFailBox.selectedProperty().bindBidirectional(viewModel.insuranceFailingProperty());
        budgetLabel.textProperty().bind(viewModel.budgetProperty());
        sagaList.setItems(viewModel.getSagaLog());

        journalList.setItems(viewModel.getJournal());
        notificationsList.setItems(viewModel.getNotifications());
        errorLabel.textProperty().bind(viewModel.errorMessageProperty());
        errorLabel.visibleProperty().bind(viewModel.errorMessageProperty().isNotEmpty());
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());
    }

    /** ViewModel вкладки - главное окно связывает вкладки через посредника. */
    public RequestsViewModel getViewModel() {
        return viewModel;
    }
}
