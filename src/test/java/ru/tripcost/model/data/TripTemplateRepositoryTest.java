package ru.tripcost.model.data;

import org.junit.jupiter.api.Test;
import ru.tripcost.model.domain.TransportType;
import ru.tripcost.model.domain.Trip;
import ru.tripcost.model.domain.TripOption;
import ru.tripcost.model.service.CachingDistanceProxy;
import ru.tripcost.model.service.GeoApiDistanceAdapter;
import ru.tripcost.external.geo.WorldGeoApi;
import ru.tripcost.model.service.TripPlanningFacade;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TripTemplateRepositoryTest {

    private static Path tempFile() throws IOException {
        Path dir = Files.createTempDirectory("tripcost-test");
        return dir.resolve("templates.properties");
    }

    private static Trip trip() {
        return Trip.builder().from("Ставрополь").to("Сочи").distance(600).by(TransportType.PLANE)
                .passengers(2).roundTrip(true).accommodation(3, new BigDecimal("5000"))
                .option(TripOption.BAGGAGE).option(TripOption.INSURANCE).build();
    }

    @Test
    void inMemoryRepositoryBehavesLikeCollection() {
        TripTemplateRepository repo = new InMemoryTripTemplateRepository();
        repo.save(new SavedTrip("Сочи", trip()));
        repo.save(new SavedTrip("Сочи", trip()));          // замена по названию
        assertEquals(1, repo.count());
        assertTrue(repo.existsById("Сочи"));
        assertTrue(repo.deleteById("Сочи"));
        assertFalse(repo.deleteById("Сочи"));
        assertTrue(repo.findAll().isEmpty());
    }

    @Test
    void fileRepositoryKeepsDataBetweenInstances() throws IOException {
        Path file = tempFile();
        FileTripTemplateRepository first = new FileTripTemplateRepository(file);
        first.save(new SavedTrip("Отпуск в Сочи", trip()));
        first.save(new SavedTrip("Выходные", Trip.builder().from("Ставрополь").to("Кисловодск")
                .distance(210).by(TransportType.CAR).build()));

        FileTripTemplateRepository second = new FileTripTemplateRepository(file);   // «перезапуск»
        assertEquals(2, second.count());
        Trip restored = second.findById("Отпуск в Сочи").orElseThrow().trip();
        Trip original = trip();
        assertEquals(original.getDestination(), restored.getDestination());
        assertEquals(original.getTransport(), restored.getTransport());
        assertEquals(original.getNights(), restored.getNights());
        assertEquals(0, original.getPricePerNight().compareTo(restored.getPricePerNight()));
        assertEquals(original.getOptions(), restored.getOptions());
        assertTrue(restored.isRoundTrip());
    }

    @Test
    void deleteIsWrittenToFile() throws IOException {
        Path file = tempFile();
        FileTripTemplateRepository repo = new FileTripTemplateRepository(file);
        repo.save(new SavedTrip("Сочи", trip()));
        repo.deleteById("Сочи");
        assertEquals(0, new FileTripTemplateRepository(file).count());
    }

    @Test
    void damagedRecordIsSkipped() throws IOException {
        Path file = tempFile();
        FileTripTemplateRepository repo = new FileTripTemplateRepository(file);
        repo.save(new SavedTrip("Сочи", trip()));
        repo.save(new SavedTrip("Москва", Trip.builder().from("Ставрополь").to("Москва")
                .distance(1540).by(TransportType.TRAIN).build()));
        String text = Files.readString(file, StandardCharsets.UTF_8).replace("trip.1.transport=PLANE", "trip.1.transport=ROCKET");
        Files.writeString(file, text, StandardCharsets.UTF_8);

        FileTripTemplateRepository reread = new FileTripTemplateRepository(file);
        assertEquals(1, reread.count());
        assertEquals(1, reread.skippedOnLoad());
    }

    @Test
    void savedTripRequiresName() {
        assertThrows(IllegalArgumentException.class, () -> new SavedTrip(" ", trip()));
    }

    @Test
    void facadeRestoresUserTemplatesAfterRestart() throws IOException {
        Path file = tempFile();
        TripPlanningFacade first = facade(file);
        first.saveTemplate("Мой отпуск", trip().toBuilder());
        assertTrue(first.isUserTemplate("Мой отпуск"));

        TripPlanningFacade second = facade(file);                             // новый запуск
        List<String> names = second.templateNames();
        assertTrue(names.contains("Мой отпуск"));
        assertEquals("Сочи", second.template("Мой отпуск").build().getDestination());

        assertTrue(second.deleteTemplate("Мой отпуск"));
        assertFalse(facade(file).templateNames().contains("Мой отпуск"));
    }

    @Test
    void presetTemplatesCannotBeReplacedOrDeleted() throws IOException {
        TripPlanningFacade facade = facade(tempFile());
        String preset = facade.templateNames().get(0);
        assertThrows(IllegalArgumentException.class, () -> facade.saveTemplate(preset, trip().toBuilder()));
        assertFalse(facade.deleteTemplate(preset));
        assertTrue(facade.templateNames().contains(preset));
    }

    private static TripPlanningFacade facade(Path file) {
        return new TripPlanningFacade(new CachingDistanceProxy(() -> new GeoApiDistanceAdapter(new WorldGeoApi())),
                new FileTripTemplateRepository(file));
    }
}
