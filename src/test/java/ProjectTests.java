import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ProjectTests {
    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    // ------------------------------------------------------------------
    // Abschnitt 1: Konstruktor / Kapazität
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Kapazität 0 wirft IllegalArgumentException")
    void kapazitaetNullWirftException() {
        assertThrows(IllegalArgumentException.class, () -> new Queue(0));
    }

    @Test
    @DisplayName("Negative Kapazität wirft IllegalArgumentException")
    void negativeKapazitaetWirftException() {
        assertThrows(IllegalArgumentException.class, () -> new Queue(-5));
    }

    @Test
    @DisplayName("Kapazität 1 ist erlaubt (Grenzwert)")
    void kapazitaetEinsIstErlaubt() {
        Queue queue = new Queue(1);
        assertEquals(1, queue.capacity());
    }

    @Test
    @DisplayName("Neue Queue ist leer")
    void neueQueueIstLeer() {
        assertEquals(0, new Queue(5).size());
    }

    // ------------------------------------------------------------------
    // Abschnitt 2: Ein Wert hinein, ein Wert heraus
    // ------------------------------------------------------------------

    @Test
    @DisplayName("put erhöht die Größe um 1")
    void putErhoehtGroesse() throws InterruptedException {
        Queue queue = new Queue(5);
        queue.put(42);
        assertEquals(1, queue.size());
    }

    @Test
    @DisplayName("get liefert den abgelegten Wert und verringert die Größe")
    void getLiefertAbgelegtenWert() throws InterruptedException {
        Queue queue = new Queue(5);
        queue.put(42);
        assertEquals(42, queue.get());
        assertEquals(0, queue.size());
    }

    // ------------------------------------------------------------------
    // Abschnitt 3: FIFO und Ringpuffer
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Werte kommen in korrekten Reihenfolge heraus (FIFO)")
    void fifoReihenfolge() throws InterruptedException {
        Queue queue = new Queue(3);
        queue.put(1);
        queue.put(2);
        queue.put(3);
        assertEquals(1, queue.get());
        assertEquals(2, queue.get());
        assertEquals(3, queue.get());
    }

    @Test
    @DisplayName("Queue kann bis zur Kapazität befüllt werden, ohne zu blockieren")
    void befuellenBisKapazitaetBlockiertNicht() {
        Queue queue = new Queue(5);
        assertTimeoutPreemptively(TIMEOUT, () -> {
            for (int i = 0; i < 5; i++) {
                queue.put(i);
            }
        });
        assertEquals(5, queue.size());
    }

    @Test
    @DisplayName("FIFO bleibt erhalten, wenn die Queue mehrfach durchläuft")
    void queueDurchlauf() throws InterruptedException {
        Queue queue = new Queue(3);
        int next = 0;
        int expected = 0;
        for (int round = 0; round < 10; round++) { // 10 Runden: immer 2 rein, 2 raus
            queue.put(next++);
            queue.put(next++);
            assertEquals(expected++, queue.get());
            assertEquals(expected++, queue.get());
        }
        assertEquals(0, queue.size());
    }
}
