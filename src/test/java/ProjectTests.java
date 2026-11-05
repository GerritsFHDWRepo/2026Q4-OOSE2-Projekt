import static org.junit.jupiter.api.Assertions.*;

        import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(Producer.N);
        Producer producer = new Producer("P", queue);

        producer.start();
        producer.join(TIMEOUT.toMillis());

        assertFalse(producer.isAlive());
        assertEquals(Producer.N, queue.size());
        assertEquals(producer.getProduced(), new ArrayList<>(queue));
    }

    @Test
    @DisplayName("Producer waits when the queue is full")
    void producerWaitsWhenQueueIsFull() throws InterruptedException {
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(1);
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
        assertEquals(1, producer.getProduced().size());
    }

    // ------------------------------------------------------------------
    // Abschnitt 2: Consumer
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Consumer takes exactly N values in FIFO order")
    void consumerTakesExactlyNValuesInFifoOrder() throws InterruptedException {
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(Producer.N);
        List<Integer> expected = new ArrayList<>();
        for (int i = 0; i < Producer.N; i++) {
            queue.put(i);
            expected.add(i);
        }
        Consumer consumer = new Consumer("C", queue);

        consumer.start();
        consumer.join(TIMEOUT.toMillis());

        assertFalse(consumer.isAlive());
        assertEquals(expected, consumer.getConsumed());
        assertEquals(0, queue.size());
    }

    @Test
    @DisplayName("Consumer waits when the queue is empty")
    void consumerWaitsWhenQueueIsEmpty() throws InterruptedException {
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(1);
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
        assertTrue(consumer.getConsumed().isEmpty());
    }

    // ------------------------------------------------------------------
    // Abschnitt 3: Ein Producer und ein Consumer zusammen
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Values arrive in the order they were produced")
    void valuesArriveInProducedOrder() throws InterruptedException {
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(2);
        Producer producer = new Producer("P", queue);
        Consumer consumer = new Consumer("C", queue);

        producer.start();
        consumer.start();
        producer.join(TIMEOUT.toMillis());
        consumer.join(TIMEOUT.toMillis());

        assertFalse(producer.isAlive());
        assertFalse(consumer.isAlive());
        assertEquals(producer.getProduced(), consumer.getConsumed());
        assertEquals(0, queue.size());
    }

    @Test
    @DisplayName("Capacity 1 behaves like the buffer from the lecture")
    void capacityOneBehavesLikeBuffer() throws InterruptedException {
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(1);
        Producer producer = new Producer("P", queue);
        Consumer consumer = new Consumer("C", queue);

        producer.start();
        consumer.start();
        producer.join(TIMEOUT.toMillis());
        consumer.join(TIMEOUT.toMillis());

        assertFalse(producer.isAlive());
        assertFalse(consumer.isAlive());
        assertEquals(producer.getProduced(), consumer.getConsumed());
        assertEquals(0, queue.size());
    }