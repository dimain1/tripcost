package ru.tripcost.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Проверка правил MVVM по исходному коду: какой слой от какого может зависеть.
 *
 * <pre>
 * View  --->  ViewModel  --->  Model
 * </pre>
 * Model не знает ни о JavaFX, ни о ViewModel, ни о View; ViewModel использует из JavaFX
 * только свойства и коллекции и не знает о View; View обращается к модели только
 * через ViewModel (допускаются лишь типы значений для отображения в списках).
 */
class MvvmArchitectureTest {

    private static final Path SRC = Path.of("src/main/java/ru/tripcost");

    /** Типы модели, которые View может показывать в выпадающих списках и кнопках. */
    private static final Set<String> VIEW_ALLOWED_MODEL_TYPES = Set.of(
            "ru.tripcost.model.domain.TransportType",
            "ru.tripcost.model.domain.TripOption",
            "ru.tripcost.model.journey.Journey",
            "ru.tripcost.model.pricing.TransportChoiceStrategy",
            "ru.tripcost.model.report.ReportFormatType",
            "ru.tripcost.model.report.ReportType",
            "ru.tripcost.model.request.RequestAction");

    private static List<String[]> imports(String layer) {
        List<String[]> result = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SRC.resolve(layer))) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    if (line.startsWith("import ")) {
                        String type = line.substring(7).replace("static ", "").replace(";", "").trim();
                        result.add(new String[] {file.getFileName().toString(), type});
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return result;
    }

    private static List<String> violations(String layer, java.util.function.Predicate<String> forbidden) {
        return imports(layer).stream().filter(i -> forbidden.test(i[1]))
                .map(i -> i[0] + ": " + i[1]).toList();
    }

    @Test
    void modelDoesNotDependOnUiOrOtherLayers() {
        List<String> bad = violations("model", t -> t.startsWith("javafx.")
                || t.startsWith("ru.tripcost.viewmodel") || t.startsWith("ru.tripcost.view."));
        assertTrue(bad.isEmpty(), "Model зависит от интерфейса: " + bad);
    }

    @Test
    void viewModelDoesNotDependOnView() {
        List<String> bad = violations("viewmodel", t -> t.startsWith("ru.tripcost.view.")
                || t.startsWith("javafx.scene") || t.startsWith("javafx.fxml") || t.startsWith("javafx.stage"));
        assertTrue(bad.isEmpty(), "ViewModel зависит от View или элементов интерфейса: " + bad);
    }

    @Test
    void viewUsesModelOnlyThroughViewModel() {
        List<String> bad = violations("view", t -> t.startsWith("ru.tripcost.model.")
                && !VIEW_ALLOWED_MODEL_TYPES.contains(t));
        assertTrue(bad.isEmpty(), "View обращается к модели в обход ViewModel: " + bad);
    }

    @Test
    void viewHasNoBusinessServices() {
        List<String> bad = violations("view", t -> t.contains(".service.") || t.contains(".pricing.TripCostCalculator"));
        assertTrue(bad.isEmpty(), "View создаёт сервисы модели: " + bad);
    }
}
