import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.fhdw.project.ProjectApplication;
import de.fhdw.project.Queue;
import de.fhdw.project.Producer;
import de.fhdw.project.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests für das Erzeuger-Verbraucher-System.

 * Die eigene Klasse Queue ist ein Wrapper um eine LinkedBlockingQueue. Die
 * LinkedBlockingQueue selbst wird nicht getestet, nur ihre Benutzung durch das System.

 * GETESTETE SCHNITTSTELLE (Package de.fhdw.project)
 *   Queue
 *     + Queue(capacity : int)
 *     + put(value : int) : void / get() : int      blockieren bei voller bzw. leerer Queue
 *     + size() : int
 *     + toArray() : Integer[]                      Inhalt in FIFO-Reihenfolge

 *   Producer extends Thread
 *     + Producer(name : String, queue : Queue, actionAmount : int)
 *     + run()                                      legt actionAmount Werte per put() ab
 *     + getProduced() : Integer[]                  abgelegte Werte dieses Producers in Reihenfolge

 *   Consumer extends Thread
 *     + Consumer(name : String, queue : Queue, actionAmount : int)
 *     + run()                                      entnimmt actionAmount Werte per get()
 *     + getConsumed() : Integer[]                  entnommene Werte dieses Consumers in Reihenfolge

 *   ProjectApplication
 *     + runSystem(capacity, producerCount, consumerCount,
 *                 actionAmountProducer, actionAmountConsumer) : Queue
 *                                                  startet alles, wartet auf das Ende,
 *                                                  gibt die Queue zurück

 * VERHALTEN BEI interrupt(): Producer und Consumer beenden sich, statt weiterzulaufen.
 */
public class ProjectTests {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    // Dieselben Werte, die main() an runSystem übergibt: runSystem(3, 4, 2, 2, 4)
    // Voraussetzungen für die Tests:
    // - PRODUCER_COUNT * ACTIONS_PER_PRODUCER == CONSUMER_COUNT * ACTIONS_PER_CONSUMER
    // - ACTIONS_PER_PRODUCER <= CAPACITY
    private static final int CAPACITY = 3;
    private static final int PRODUCER_COUNT = 4;
    private static final int CONSUMER_COUNT = 2;
    private static final int ACTIONS_PER_PRODUCER = 2;
    private static final int ACTIONS_PER_CONSUMER = 4;

    // ------------------------------------------------------------------
    // Abschnitt 1: Producer
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Producer puts exactly as many values as specified")
    void producerPutsAllItsValues() throws InterruptedException {
        Queue queue = new Queue(CAPACITY);
        Producer producer = new Producer("P", queue, ACTIONS_PER_PRODUCER);

        producer.start();
        producer.join(TIMEOUT.toMillis());

        assertFalse(producer.isAlive());
        assertEquals(ACTIONS_PER_PRODUCER, queue.size());
        assertArrayEquals(producer.getProduced(), queue.toArray());
    }

    @Test
    @DisplayName("Producer waits when the queue is full")
    void producerWaitsWhenQueueIsFull() throws InterruptedException {
        Queue queue = new Queue(CAPACITY);
        Producer producer = new Producer("P", queue, CAPACITY + 1);   // ein Wert mehr, als hineinpasst

        producer.start();
        Thread.sleep(200);                  // dem Producer Zeit geben, die Queue zu füllen

        assertTrue(producer.isAlive());           // er ist nicht fertig, sondern wartet
        assertEquals(CAPACITY, queue.size());     // die Queue ist voll, aber nicht übervoll

        producer.interrupt();                     // aufräumen: wartenden Producer beenden
        producer.join(TIMEOUT.toMillis());
    }

    // ------------------------------------------------------------------
    // Abschnitt 2: Consumer
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Consumer takes exactly as many values as specified in FIFO order")
    void consumerTakesAllItsValuesInFifoOrder() throws InterruptedException {
        Queue queue = new Queue(ACTIONS_PER_CONSUMER);   // groß genug, um alle Werte vorab abzulegen
        for (int i = 0; i < ACTIONS_PER_CONSUMER; i++) {
            queue.put(i);                                // Werte 0, 1, 2, ...
        }
        Consumer consumer = new Consumer("C", queue, ACTIONS_PER_CONSUMER);

        consumer.start();
        consumer.join(TIMEOUT.toMillis());

        assertFalse(consumer.isAlive());
        Integer[] consumed = consumer.getConsumed();
        assertEquals(ACTIONS_PER_CONSUMER, consumed.length);
        for (int i = 0; i < consumed.length; i++) {
            assertEquals(i, consumed[i]);                // an Position i muss der Wert i stehen (FIFO)
        }
        assertEquals(0, queue.size());
    }

    @Test
    @DisplayName("Consumer waits when the queue is empty")
    void consumerWaitsWhenQueueIsEmpty() throws InterruptedException {
        Queue queue = new Queue(CAPACITY);
        Consumer consumer = new Consumer("C", queue, ACTIONS_PER_CONSUMER);   // kein Producer vorhanden

        consumer.start();
        Thread.sleep(200);                  // dem Consumer Zeit geben, zu entnehmen

        assertTrue(consumer.isAlive());           // er ist nicht fertig, sondern wartet

        consumer.interrupt();                     // aufräumen: wartenden Consumer beenden
        consumer.join(TIMEOUT.toMillis());
    }

    // ------------------------------------------------------------------
    // Abschnitt 3: Ein Producer und ein Consumer zusammen
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Producer and consumer running in parallel: all values arrive in the order they were produced")
    void producerAndConsumerInParallelTransferAllValuesInOrder() throws InterruptedException {
        // Da ACTIONS_PER_PRODUCER <= CAPACITY ist, passen alle Werte gleichzeitig in die Queue.
        // Die Queue wird also nie voll und der Producer muss nie warten.
        // Geprüft wird hier die Übertragung, wenn genug Platz vorhanden ist.
        Queue queue = new Queue(CAPACITY);
        Producer producer = new Producer("P", queue, ACTIONS_PER_PRODUCER);
        Consumer consumer = new Consumer("C", queue, ACTIONS_PER_PRODUCER);   // holt genau so viele, wie abgelegt werden

        producer.start();
        consumer.start();
        producer.join(TIMEOUT.toMillis());
        consumer.join(TIMEOUT.toMillis());

        assertFalse(producer.isAlive());
        assertFalse(consumer.isAlive());
        assertArrayEquals(producer.getProduced(), consumer.getConsumed());
        assertEquals(0, queue.size());
    }

    @Test
    @DisplayName("Capacity 1 (buffer from the lecture): all values still arrive in the order they were produced")
    void capacityOneStillTransfersAllValuesInOrder() throws InterruptedException {
        // Bei Kapazität 1 ist die Queue nach jedem abgelegten Wert voll.
        // Der Producer muss deshalb nach jedem Wert warten, bis der Consumer ihn entnommen hat.
        // Producer und Consumer wechseln sich so ab: ablegen, entnehmen, ablegen, entnehmen, ...
        Queue queue = new Queue(1);
        Producer producer = new Producer("P", queue, ACTIONS_PER_PRODUCER);
        Consumer consumer = new Consumer("C", queue, ACTIONS_PER_PRODUCER);

        producer.start();
        consumer.start();
        producer.join(TIMEOUT.toMillis());
        consumer.join(TIMEOUT.toMillis());

        assertFalse(producer.isAlive());
        assertFalse(consumer.isAlive());
        assertArrayEquals(producer.getProduced(), consumer.getConsumed());
        assertEquals(0, queue.size());
    }

    // ------------------------------------------------------------------
    // Abschnitt 4: Gesamtsystem
    // ------------------------------------------------------------------

    @Test
    @DisplayName("System rejects a capacity below 1")
    void systemRejectsCapacityBelowOne() {
        assertThrows(IllegalArgumentException.class, () -> ProjectApplication.runSystem(
                0, PRODUCER_COUNT, CONSUMER_COUNT, ACTIONS_PER_PRODUCER, ACTIONS_PER_CONSUMER));
    }

    @Test
    @DisplayName("System rejects unequal totals of produced and consumed values")
    void systemRejectsUnequalTotals() {
        // ein Consumer mehr -> es sollen mehr Werte entnommen als abgelegt werden
        assertThrows(IllegalArgumentException.class, () -> ProjectApplication.runSystem(
                CAPACITY, PRODUCER_COUNT, CONSUMER_COUNT + 1, ACTIONS_PER_PRODUCER, ACTIONS_PER_CONSUMER));
    }

    @Test
    @DisplayName("Multiple producers and consumers: every value arrives exactly once")
    void multipleProducersAndConsumersLoseNoValues() {
        Queue queue = new Queue(CAPACITY);
        List<Producer> producers = new ArrayList<>();
        List<Consumer> consumers = new ArrayList<>();
        // Erstellt Producer und Consumer wie in main().
        for (int i = 1; i <= PRODUCER_COUNT; i++) {
            producers.add(new Producer("P" + i, queue, ACTIONS_PER_PRODUCER));
        }
        for (int i = 1; i <= CONSUMER_COUNT; i++) {
            consumers.add(new Consumer("C" + i, queue, ACTIONS_PER_CONSUMER));
        }

        // Stellt sicher, dass der Test innerhalb des vorgegebenen Timeouts endet.
        // Alle Producer und Consumer werden gestartet und anschließend beendet.
        assertTimeoutPreemptively(TIMEOUT, () -> {

            for (Producer p : producers) p.start();
            for (Consumer c : consumers) c.start();

            // Wartet auf die Beendigung aller Producer.
            for (Producer p : producers) p.join();

            // Wartet auf die Beendigung aller Consumer.
            for (Consumer c : consumers) c.join();
        });

        // Sammelt alle von den Producern erzeugten Werte.
        List<Integer> allProduced = new ArrayList<>();

        // Sammelt alle von den Consumern konsumierten Werte.
        List<Integer> allConsumed = new ArrayList<>();

        for (Producer p : producers)
            Collections.addAll(allProduced, p.getProduced());

        for (Consumer c : consumers)
            Collections.addAll(allConsumed, c.getConsumed());

        // Sortieren ermöglicht einen einfachen Vergleich der Inhalte.
        Collections.sort(allProduced);
        Collections.sort(allConsumed);

        // Erwartung:
        // Alle erzeugten Werte wurden konsumiert.
        assertEquals(PRODUCER_COUNT * ACTIONS_PER_PRODUCER, allConsumed.size());

        // Vergleicht die Listen:
        // Es darf kein Wert verloren gegangen oder doppelt konsumiert worden sein.
        assertEquals(allProduced, allConsumed);
        assertEquals(0, queue.size());
    }

    @Test
    @DisplayName("Whole system terminates and leaves the queue empty")
    void wholeSystemTerminates() {

        // Startet das komplette Producer-Consumer-System mit denselben Werten wie main().
        // Der Test schlägt fehl, falls das System nicht rechtzeitig beendet wird.
        Queue queue = assertTimeoutPreemptively(TIMEOUT, () -> {
            return ProjectApplication.runSystem(
                    CAPACITY, PRODUCER_COUNT, CONSUMER_COUNT, ACTIONS_PER_PRODUCER, ACTIONS_PER_CONSUMER);
        });

        assertEquals(0, queue.size());
    }
}