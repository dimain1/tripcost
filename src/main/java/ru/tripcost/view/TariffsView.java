package ru.tripcost.view;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import ru.tripcost.viewmodel.TariffsViewModel;
import ru.tripcost.viewmodel.TariffsViewModel.TariffRow;

/**
 * View вкладки «Тарифы» (code-behind для tariffs-view.fxml).
 */
public class TariffsView {

    private final TariffsViewModel viewModel = new TariffsViewModel();

    @FXML private TableView<TariffRow> table;
    @FXML private TableColumn<TariffRow, String> transportColumn;
    @FXML private TableColumn<TariffRow, String> baseFeeColumn;
    @FXML private TableColumn<TariffRow, String> perKmColumn;

    @FXML
    private void initialize() {
        transportColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().transport()));
        baseFeeColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().baseFee()));
        perKmColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().perKm()));
        table.setItems(viewModel.getRows());
    }
}
