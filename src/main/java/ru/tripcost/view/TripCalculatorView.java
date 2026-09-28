package ru.tripcost.view;

import javafx.beans.property.SimpleStringProperty;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import ru.tripcost.model.domain.TransportType;
import ru.tripcost.model.pricing.TransportChoiceStrategy;
import ru.tripcost.model.domain.TripOption;
import ru.tripcost.model.report.ReportFormatType;
import ru.tripcost.model.report.ReportType;
import ru.tripcost.viewmodel.TripCalculatorViewModel.CostRow;
import ru.tripcost.viewmodel.TripCalculatorViewModel.VariantRow;
import ru.tripcost.viewmodel.TripCalculatorViewModel;

import java.io.File;

/**
 * View экрана расчёта (code-behind для trip-calculator-view.fxml).
 * Только привязывает элементы к свойствам ViewModel и передаёт ей нажатия кнопок.
 */
public class TripCalculatorView {

    private static final PseudoClass REJECTED = PseudoClass.getPseudoClass("rejected");

    private final TripCalculatorViewModel viewModel = new TripCalculatorViewModel();

    @FXML private ComboBox<String> templateBox;
    @FXML private TextField templateNameField;
    @FXML private TextField originField;
    @FXML private TextField destinationField;
    @FXML private TextField distanceField;
    @FXML private ComboBox<TransportType> transportBox;
    @FXML private Spinner<Integer> passengersSpinner;
    @FXML private TextField fuelField;
    @FXML private CheckBox roundTripBox;
    @FXML private TextField tollsField;
    @FXML private TextField nightsField;
    @FXML private TextField priceNightField;

    @FXML private Label errorLabel;
    @FXML private Label infoLabel;
    @FXML private Label summaryLabel;
    @FXML private FlowPane optionsPane;
    @FXML private TreeTableView<CostRow> costTable;
    @FXML private TreeTableColumn<CostRow, String> costNameColumn;
    @FXML private TreeTableColumn<CostRow, String> costAmountColumn;
    @FXML private Label totalCostLabel;
    @FXML private Label perPassengerLabel;
    @FXML private Label approvalLabel;
    @FXML private Label approvalDetailsLabel;

    @FXML private Button lookupButton;
    @FXML private ComboBox<ReportType> reportTypeBox;
    @FXML private ComboBox<ReportFormatType> reportFormatBox;
    @FXML private Button previewButton;
    @FXML private Button saveReportButton;
    @FXML private Button addToJourneyButton;
    @FXML private Button createRequestButton;
    @FXML private Button deleteTemplateButton;
    @FXML private Label templatesHintLabel;

    @FXML private TableView<VariantRow> variantsTable;
    @FXML private TableColumn<VariantRow, String> variantTransportColumn;
    @FXML private TableColumn<VariantRow, String> variantTotalColumn;
    @FXML private TableColumn<VariantRow, String> variantTimeColumn;
    @FXML private ComboBox<TransportChoiceStrategy> choiceBox;
    @FXML private Label travelTimeLabel;
    @FXML private TableColumn<VariantRow, String> variantNoteColumn;

    @FXML
    private void initialize() {
        // списки
        templateBox.setItems(viewModel.getTemplateNames());
        templateBox.valueProperty().bindBidirectional(viewModel.selectedTemplateProperty());
        templateNameField.textProperty().bindBidirectional(viewModel.newTemplateNameProperty());
        transportBox.setItems(viewModel.getTransportTypes());
        transportBox.valueProperty().bindBidirectional(viewModel.transportProperty());

        // поля ввода
        originField.textProperty().bindBidirectional(viewModel.originProperty());
        destinationField.textProperty().bindBidirectional(viewModel.destinationProperty());
        distanceField.textProperty().bindBidirectional(viewModel.distanceProperty());
        fuelField.textProperty().bindBidirectional(viewModel.fuelConsumptionProperty());
        roundTripBox.selectedProperty().bindBidirectional(viewModel.roundTripProperty());
        tollsField.textProperty().bindBidirectional(viewModel.tollsProperty());
        nightsField.textProperty().bindBidirectional(viewModel.nightsProperty());
        priceNightField.textProperty().bindBidirectional(viewModel.pricePerNightProperty());

        SpinnerValueFactory.IntegerSpinnerValueFactory factory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 200, 1);
        passengersSpinner.setValueFactory(factory);
        factory.valueProperty().bindBidirectional(viewModel.passengersProperty());

        // доступность полей
        fuelField.disableProperty().bind(viewModel.fuelEditableBinding().not());
        tollsField.disableProperty().bind(viewModel.tollsEditableBinding().not());

        // результат
        errorLabel.textProperty().bind(viewModel.errorMessageProperty());
        errorLabel.visibleProperty().bind(viewModel.errorMessageProperty().isNotEmpty());
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());
        infoLabel.textProperty().bind(viewModel.infoMessageProperty());
        infoLabel.visibleProperty().bind(viewModel.infoMessageProperty().isNotEmpty());
        infoLabel.managedProperty().bind(infoLabel.visibleProperty());
        summaryLabel.textProperty().bind(viewModel.tripSummaryProperty());

        // дерево статей расходов
        costNameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getValue().name()));
        costAmountColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getValue().amount()));
        viewModel.costTreeProperty().addListener((obs, old, root) ->
                costTable.setRoot(root == null ? null : toTreeItem(root)));

        // дополнительные услуги: флажок на каждую, недоступные для транспорта выключены
        for (TripOption option : viewModel.getOptions()) {
            CheckBox box = new CheckBox(option.getDisplayName());
            box.selectedProperty().bindBidirectional(viewModel.optionProperty(option));
            box.disableProperty().bind(viewModel.optionAvailableBinding(option).not());
            optionsPane.getChildren().add(box);
        }
        totalCostLabel.textProperty().bind(viewModel.totalCostProperty());
        perPassengerLabel.textProperty().bind(viewModel.perPassengerCostProperty());
        travelTimeLabel.textProperty().bind(viewModel.travelTimeProperty());
        Tooltip timeTip = new Tooltip();
        timeTip.textProperty().bind(viewModel.travelTimeDetailsProperty());
        travelTimeLabel.setTooltip(timeTip);
        approvalLabel.textProperty().bind(viewModel.approvalProperty());
        approvalDetailsLabel.textProperty().bind(viewModel.approvalDetailsProperty());
        approvalDetailsLabel.visibleProperty().bind(viewModel.approvalDetailsProperty().isNotEmpty());
        approvalDetailsLabel.managedProperty().bind(approvalDetailsLabel.visibleProperty());
        viewModel.approvalRejectedProperty().addListener((obs, o, rejected) ->
                approvalLabel.pseudoClassStateChanged(REJECTED, rejected));

        // отчёт
        reportTypeBox.setItems(viewModel.getReportTypes());
        reportTypeBox.valueProperty().bindBidirectional(viewModel.reportTypeProperty());
        reportFormatBox.setItems(viewModel.getReportFormats());
        reportFormatBox.valueProperty().bindBidirectional(viewModel.reportFormatProperty());
        previewButton.disableProperty().bind(viewModel.hasResultProperty().not());
        saveReportButton.disableProperty().bind(viewModel.hasResultProperty().not());
        addToJourneyButton.disableProperty().bind(viewModel.hasResultProperty().not());
        createRequestButton.disableProperty().bind(viewModel.hasResultProperty().not());
        CommandBinder.bind(deleteTemplateButton, viewModel.deleteTemplateCommand());
        templatesHintLabel.textProperty().bind(viewModel.templatesHintProperty());

        // сравнение видов транспорта
        variantTransportColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().transport()));
        variantTotalColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().total()));
        variantTimeColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().time()));
        choiceBox.setItems(viewModel.getChoiceStrategies());
        choiceBox.valueProperty().bindBidirectional(viewModel.choiceStrategyProperty());
        variantNoteColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().note()));
        variantsTable.setItems(viewModel.getVariants());
    }

    @FXML
    private void onApplyTemplate() {
        viewModel.applyTemplate();
    }

    @FXML
    private void onSaveTemplate() {
        commitPassengers();
        viewModel.saveTemplate();
    }

    @FXML
    private void onCompare() {
        commitPassengers();
        viewModel.compareTransport();
    }

    @FXML
    private void onCalculate() {
        commitPassengers();
        viewModel.calculate();
    }

    @FXML
    private void onLookupDistance() {
        viewModel.lookupDistance();
    }

    @FXML
    private void onCreateRequest() {
        viewModel.createRequest();
    }

    @FXML
    private void onAddToJourney() {
        viewModel.addToJourney();
    }

    @FXML
    private void onPreviewReport() {
        String text = viewModel.buildReport();
        if (text == null) {
            return;
        }
        TextArea area = new TextArea(text);
        area.setEditable(false);
        area.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace;");
        Stage stage = new Stage();
        stage.initOwner(window());
        stage.setTitle("Отчёт: " + reportTypeBox.getValue() + ", " + reportFormatBox.getValue());
        stage.setScene(new Scene(new BorderPane(area), 680, 520));
        stage.show();
    }

    @FXML
    private void onSaveReport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить отчёт");
        chooser.setInitialFileName(viewModel.reportFileName());
        String ext = viewModel.reportExtension();
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(reportFormatBox.getValue() + " (*." + ext + ")", "*." + ext));
        File file = chooser.showSaveDialog(window());
        if (file != null) {
            viewModel.saveReport(file);
        }
    }

    /** Строит узлы TreeTableView из дерева строк ViewModel (рекурсивно). */
    private static TreeItem<CostRow> toTreeItem(CostRow row) {
        TreeItem<CostRow> item = new TreeItem<>(row);
        item.setExpanded(true);
        for (CostRow child : row.children()) {
            item.getChildren().add(toTreeItem(child));
        }
        return item;
    }

    private Window window() {
        return previewButton.getScene().getWindow();
    }

    @FXML
    private void onClear() {
        viewModel.clear();
    }

    /** Принимает число, введённое в спиннер с клавиатуры. */
    private void commitPassengers() {
        try {
            passengersSpinner.commitValue();
        } catch (NumberFormatException e) {
            passengersSpinner.cancelEdit();
        }
    }

    /** ViewModel вкладки - главное окно связывает вкладки через посредника. */
    public TripCalculatorViewModel getViewModel() {
        return viewModel;
    }
}
