package ru.tripcost.model.data;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Репозиторий шаблонов в файле на диске (формат properties, UTF-8).
 *
 * <p>При создании читает файл, если он есть. Каждое изменение сразу записывается:
 * сначала во временный файл, затем временный файл заменяет основной - так файл
 * не окажется записанным наполовину. Повреждённые записи пропускаются
 * и не мешают прочитать остальные.</p>
 */
public class FileTripTemplateRepository implements TripTemplateRepository {

    private final Path file;
    private final Map<String, SavedTrip> items = new LinkedHashMap<>();
    private int skipped;

    public FileTripTemplateRepository(Path file) {
        this.file = file;
        load();
    }

    @Override
    public List<SavedTrip> findAll() {
        return new ArrayList<>(items.values());
    }

    @Override
    public Optional<SavedTrip> findById(String name) {
        return Optional.ofNullable(items.get(name));
    }

    @Override
    public void save(SavedTrip entity) {
        items.put(entity.name(), entity);
        flush();
    }

    @Override
    public boolean deleteById(String name) {
        boolean removed = items.remove(name) != null;
        if (removed) {
            flush();
        }
        return removed;
    }

    @Override
    public long count() {
        return items.size();
    }

    public Path file() {
        return file;
    }

    /** Сколько повреждённых записей пропущено при чтении. */
    public int skippedOnLoad() {
        return skipped;
    }

    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            props.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать " + file, e);
        }
        int count = Integer.parseInt(props.getProperty("count", "0"));
        for (int i = 1; i <= count; i++) {
            try {
                SavedTrip saved = SavedTripMapper.read(props, i);
                items.put(saved.name(), saved);
            } catch (RuntimeException e) {
                skipped++;               // повреждённая запись: пропускаем, остальные читаем
            }
        }
    }

    private void flush() {
        Properties props = new Properties();
        props.setProperty("count", Integer.toString(items.size()));
        int index = 1;
        for (SavedTrip saved : items.values()) {
            SavedTripMapper.write(props, index++, saved);
        }
        try {
            Path dir = file.toAbsolutePath().getParent();
            Files.createDirectories(dir);
            Path tmp = Files.createTempFile(dir, "templates", ".tmp");
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                props.store(writer, "Шаблоны поездок пользователя");
            }
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось сохранить " + file, e);
        }
    }
}
