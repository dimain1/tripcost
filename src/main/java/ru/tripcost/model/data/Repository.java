package ru.tripcost.model.data;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий: коллекция объектов предметной области, скрывающая способ хранения.
 *
 * <p><b>Паттерн «Репозиторий» (Repository).</b> Клиент добавляет, ищет и удаляет
 * объекты так, будто они лежат в коллекции в памяти. Где они на самом деле -
 * в памяти, в файле или в базе данных, - решает реализация.</p>
 *
 * @param <T>  тип хранимых объектов
 * @param <ID> тип идентификатора
 */
public interface Repository<T, ID> {

    List<T> findAll();

    Optional<T> findById(ID id);

    default boolean existsById(ID id) {
        return findById(id).isPresent();
    }

    /** Добавить объект или заменить объект с тем же идентификатором. */
    void save(T entity);

    /** Удалить объект; {@code false}, если его не было. */
    boolean deleteById(ID id);

    long count();
}
