package ru.tripcost.model.journey;

import org.junit.jupiter.api.Test;
import ru.tripcost.model.domain.TransportType;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JourneyIteratorTest {

    private static Journey sample() {
        Journey j = new Journey();
        j.add(new Leg("Ставрополь", "Москва", TransportType.TRAIN));
        j.add(new Leg("Москва", "Казань", TransportType.PLANE));
        j.add(new Leg("Казань", "Самара", TransportType.BUS));
        j.add(new Leg("Самара", "Волгоград", TransportType.TRAIN));
        j.add(new Leg("Волгоград", "Элиста", TransportType.BUS));
        return j;
    }

    @Test
    void forwardIteratorVisitsLegsInOrderWithNumbers() {
        LegIterator it = sample().iterator();
        List<String> seen = new ArrayList<>();
        while (it.hasNext()) {
            Leg leg = it.next();
            seen.add(it.number() + ":" + leg.to());
        }
        assertEquals(List.of("1:Москва", "2:Казань", "3:Самара", "4:Волгоград", "5:Элиста"), seen);
    }

    @Test
    void journeyWorksInForEach() {
        int count = 0;
        for (Leg ignored : sample()) {
            count++;
        }
        assertEquals(5, count);
    }

    @Test
    void returnIteratorGoesBackwardsAndReversesLegs() {
        LegIterator it = sample().returnIterator();
        Leg first = it.next();
        assertEquals(new Leg("Элиста", "Волгоград", TransportType.BUS), first);
        assertEquals(1, it.number());
        Leg last = null;
        while (it.hasNext()) {
            last = it.next();
        }
        assertEquals(new Leg("Москва", "Ставрополь", TransportType.TRAIN), last);
        assertEquals(5, it.number());
    }

    @Test
    void filterIteratorReturnsOnlyGivenTransportWithOriginalNumbers() {
        LegIterator it = sample().iterator(TransportType.TRAIN);
        List<Integer> numbers = new ArrayList<>();
        while (it.hasNext()) {
            it.next();
            numbers.add(it.number());
        }
        assertEquals(List.of(1, 4), numbers);
        assertFalse(sample().iterator(TransportType.TAXI).hasNext());
    }

    @Test
    void exhaustedIteratorThrows() {
        LegIterator it = new Journey().iterator();
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void modificationDuringTraversalIsDetected() {
        Journey j = sample();
        LegIterator it = j.iterator();
        it.next();
        j.remove(0);
        assertThrows(ConcurrentModificationException.class, it::next);
    }

    @Test
    void internalArrayGrowsBeyondInitialCapacity() {
        Journey j = new Journey();
        for (int i = 0; i < 20; i++) {
            j.add(new Leg("Сочи", "Анапа", TransportType.BUS));
        }
        assertEquals(20, j.size());
        assertEquals(20, j.toList().size());
    }
}
