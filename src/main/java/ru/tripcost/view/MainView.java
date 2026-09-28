package ru.tripcost.view;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import ru.tripcost.viewmodel.AppTab;
import ru.tripcost.viewmodel.MainViewModel;

/**
 * View главного окна (code-behind для main-view.fxml).
 * Вкладки подключаются через fx:include и имеют собственные View и ViewModel;
 * главное окно только связывает их модели представления через посредника.
 */
public class MainView {

    private final MainViewModel viewModel = new MainViewModel();

    @FXML private Label statusLabel;
    @FXML private TabPane tabPane;
    /** Контроллеры вложенных FXML: имя поля = fx:id + "Controller". */
    @FXML private TripCalculatorView calculatorController;
    @FXML private JourneyView journeyController;
    @FXML private DirectionsView directionsController;
    @FXML private RequestsView requestsController;

    @FXML
    private void initialize() {
        statusLabel.textProperty().bind(viewModel.statusProperty());
        viewModel.connect(calculatorController.getViewModel(), journeyController.getViewModel(),
                directionsController.getViewModel(), requestsController.getViewModel());

        // выбранная вкладка: ViewModel -> TabPane и обратно
        viewModel.selectedTabProperty().addListener((obs, o, tab) ->
                tabPane.getSelectionModel().select(tab.ordinal()));
        tabPane.getSelectionModel().selectedIndexProperty().addListener((obs, o, n) ->
                viewModel.selectedTabProperty().set(AppTab.values()[n.intValue()]));
    }
}
