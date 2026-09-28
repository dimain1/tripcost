package ru.tripcost.model.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Реестр прототипов: именованные шаблоны поездок.
 *
 * <p>Хранит настроенные строители-прототипы. {@link #create(String)} возвращает
 * не сам прототип, а его копию, поэтому изменения копии не портят шаблон.
 * При регистрации тоже сохраняется копия — последующие изменения исходного
 * строителя на шаблон не влияют.</p>
 */
public class TripTemplateRegistry {

    private final Map<String, TripBuilder> prototypes = new LinkedHashMap<>();

    /** Реестр со встроенными шаблонами, которые собирает {@link TripDirector}. */
    public static TripTemplateRegistry withPresets() {
        TripTemplateRegistry registry = new TripTemplateRegistry();
        TripDirector director = new TripDirector(new TripBuilder());
        for (TripPreset preset : TripPreset.values()) {
            registry.register(preset.getDisplayName(), director.configure(preset));
        }
        return registry;
    }

    /** Сохраняет копию строителя под именем. Шаблон с тем же именем заменяется. */
    public void register(String name, TripBuilder prototype) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Не указано название шаблона");
        }
        prototypes.put(name.trim(), prototype.copy());
    }

    /** Возвращает новую копию прототипа. */
    public TripBuilder create(String name) {
        TripBuilder prototype = prototypes.get(name);
        if (prototype == null) {
            throw new IllegalArgumentException("Шаблон «" + name + "» не найден");
        }
        return prototype.copy();
    }

    /** Удаляет шаблон; возвращает {@code false}, если его не было. */
    public boolean remove(String name) {
        return prototypes.remove(name) != null;
    }

    public boolean contains(String name) {
        return prototypes.containsKey(name);
    }

    public List<String> names() {
        return new ArrayList<>(prototypes.keySet());
    }
}
