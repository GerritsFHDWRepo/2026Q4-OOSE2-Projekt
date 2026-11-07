import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.fhdw.project.ProjectApplication;
import de.fhdw.project.Queue;
import de.fhdw.project.Producer;
import de.fhdw.project.Consumer;
import de.fhdw.project.Viewer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test-First-Spezifikation für das Erzeuger-Verbraucher-System.
 *
 * Die Klassen existieren noch nicht. Diese Tests legen fest, was die
 * Implementierung leisten muss. Solange die Klassen fehlen, kompiliert
 * die Datei nicht – das ist der erwartete "Red"-Zustand.
 *
 * Als Warteschlange wird java.util.concurrent.LinkedBlockingQueue verwendet
 * (immer mit Kapazität!). Die Queue selbst wird nicht getestet, nur ihre
 * Benutzung durch das System.
 *
 * ERWARTETE SCHNITTSTELLE (Package de.fhdw.project)
 *
 *   Producer extends Thread
 *     + N : int                                    (static final) Anzahl Werte pro Producer
 *     + Producer(name : String, queue : BlockingQueue<Integer>)
 *     + run()                                      legt N Werte per put() ab
 *     + getProduced() : List<Integer>              erfolgreich abgelegte Werte in Reihenfolge
 *
 *   Consumer extends Thread
 *     + Consumer(name : String, queue : BlockingQueue<Integer>)
 *     + run()                                      entnimmt N Werte per take()
 *     + getConsumed() : List<Integer>              entnommene Werte in Reihenfolge
 *
 *   Viewer extends Thread
 *     + Viewer(queue : BlockingQueue<Integer>)
 *     + run()                                      schaut regelmäßig per peek() in die Queue,
 *                                                  bis er per interrupt() beendet wird
 *
 *   ProjectApplication
 *     + runSystem(capacity : int, producerCount : int, consumerCount : int) : BlockingQueue<Integer>
 *                                                  startet alles, wartet auf das Ende,
 *                                                  beendet den Viewer, gibt die Queue zurück
 *
 * VERHALTEN BEI interrupt(): Producer und Consumer beenden sich, statt weiterzulaufen.
 */
public class ProjectTests {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    // ------------------------------------------------------------------
    // Abschnitt 1: Producer
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Producer puts exactly N values into the queue")
    void producerPutsExactlyNValues() throws InterruptedException {
        Queue queue = new Queue(Producer.N);
        Producer producer = new Producer("P", queue);

        producer.start();
        producer.join(TIMEOUT.toMillis());

        assertFalse(producer.isAlive());
        assertEquals(Producer.N, queue.size());
        assertArrayEquals(producer.getProduced(), queue.toArrayList());
    }

    @Test
    @DisplayName("Producer waits when the queue is full")
    void producerWaitsWhenQueueIsFull() throws InterruptedException {
        Queue queue = new Queue(1);
        Producer producer = new Producer("P", queue);   // kein Consumer vorhanden

        producer.start();

        // warten (max. TIMEOUT), bis der Producer beim 2. Ablegen hängt
        long deadline = System.currentTimeMillis() + TIMEOUT.toMillis();
        while (producer.getState() != Thread.State.WAITING && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        assertEquals(Thread.State.WAITING, producer.getState());
        assertEquals(1, queue.size());

        producer.interrupt();
        producer.join(TIMEOUT.toMillis());
        assertFalse(producer.isAlive());
        assertEquals(1, producer.getProduced().length);
    }

    // ------------------------------------------------------------------
    // Abschnitt 2: Consumer
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Consumer takes exactly N values in FIFO order")
    void consumerTakesExactlyNValuesInFifoOrder() throws InterruptedException {
        Queue queue = new Queue(Producer.N);
        List<Integer> expected = new ArrayList<>();
        for (int i = 0; i < Producer.N; i++) {
            queue.put(i);
            expected.add(i);
        }
        Consumer consumer = new Consumer("C", queue);

        consumer.start();
        consumer.join(TIMEOUT.toMillis());

        assertFalse(consumer.isAlive());
        assertArrayEquals(expected.toArray(), consumer.getConsumed());
        assertEquals(0, queue.size());
    }

    @Test
    @DisplayName("Consumer waits when the queue is empty")
    void consumerWaitsWhenQueueIsEmpty() throws InterruptedException {
        Queue queue = new Queue(1);
        Consumer consumer = new Consumer("C", queue);   // kein Producer vorhanden

        consumer.start();

        // warten (max. TIMEOUT), bis der Consumer beim 1. Entnehmen hängt
        long deadline = System.currentTimeMillis() + TIMEOUT.toMillis();
        while (consumer.getState() != Thread.State.WAITING && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        assertEquals(Thread.State.WAITING, consumer.getState());

        consumer.interrupt();
        consumer.join(TIMEOUT.toMillis());
        assertFalse(consumer.isAlive());
        assertEquals(0, consumer.getConsumed().length);
    }

    // ------------------------------------------------------------------
    // Abschnitt 3: Ein Producer und ein Consumer zusammen
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Values arrive in the order they were produced")
    void valuesArriveInProducedOrder() throws InterruptedException {
        Queue queue = new Queue(2);
        Producer producer = new Producer("P", queue);
        Consumer consumer = new Consumer("C", queue);

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
    @DisplayName("Capacity 1 behaves like the buffer from the lecture")
    void capacityOneBehavesLikeBuffer() throws InterruptedException {
        Queue queue = new Queue(1);
        Producer producer = new Producer("P", queue);
        Consumer consumer = new Consumer("C", queue);

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
    // Abschnitt 4: Viewer
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Viewer does not modify the queue")
    void viewerDoesNotModifyQueue() throws InterruptedException {
        Queue queue = new Queue(5);
        queue.put(1);
        queue.put(2);
        queue.put(3);
        Viewer viewer = new Viewer(queue);

        viewer.start();
        Thread.sleep(200);                              // Viewer arbeitet eine Weile
        viewer.interrupt();
        viewer.join(1000);

        assertArrayEquals(new Integer[]{1, 2, 3}, queue.toArrayList());
    }
    @Test
    @DisplayName("Viewer handles an empty queue and stops when interrupted")
    void viewerStopsWhenInterrupted() throws InterruptedException {
        Queue queue = new Queue(5);
        Viewer viewer = new Viewer(queue);

        viewer.start();
        Thread.sleep(200);
        assertTrue(viewer.isAlive());                   // leere Queue führt nicht zum Absturz

        viewer.interrupt();
        viewer.join(1000);
        assertFalse(viewer.isAlive());
    }

    // ------------------------------------------------------------------
    // Abschnitt 5: Gesamtsystem
    // ------------------------------------------------------------------

    @Test
    @DisplayName("System rejects a capacity below 1")
    void systemRejectsCapacityBelowOne() {
        assertThrows(IllegalArgumentException.class, () -> ProjectApplication.runSystem(0, 1, 1));
    }

    @Test
    @DisplayName("System rejects unequal numbers of producers and consumers")
    void systemRejectsUnequalProducerConsumerCount() {
        assertThrows(IllegalArgumentException.class, () -> ProjectApplication.runSystem(5, 2, 3));
    }

    @Test
    @DisplayName("Multiple producers and consumers: every value arrives exactly once")
    void multipleProducersAndConsumersLoseNoValues() {
        Queue queue = new Queue(2);
        List<Producer> producers = new ArrayList<>();
        List<Consumer> consumers = new ArrayList<>();
        // Erstellt jeweils 3 Producer und 3 Consumer.
        for (int i = 1; i <= 3; i++) {
            producers.add(new Producer("P" + i, queue));
            consumers.add(new Consumer("C" + i, queue));
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
        assertEquals(3 * Producer.N, allConsumed.size());

        // Vergleicht die Listen:
        // Es darf kein Wert verloren gegangen oder doppelt konsumiert worden sein.
        assertEquals(allProduced, allConsumed);
        assertEquals(0, queue.size());
    }

    @Test
    @DisplayName("Whole system terminates and leaves the queue empty")
    void wholeSystemTerminates() {

        // Startet das komplette Producer-Consumer-System mit:
        // 5 produzierten Werten pro Producer,
        // 3 Produzern und
        // 3 Consumern.
        // Der Test schlägt fehl, falls das System nicht rechtzeitig beendet wird.
        Queue queue = assertTimeoutPreemptively(TIMEOUT, () -> {
            return ProjectApplication.runSystem(5, 3, 3);
        });

        assertEquals(0, queue.size());
    }

}
