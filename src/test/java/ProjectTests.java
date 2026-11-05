

import com.sun.tools.javac.util.List;
import de.fhdw.project.ProjectApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


import static org.junit.jupiter.api.Assertions.*;


public class ProjectTests {

    @Test
    @DisplayName("Viewer does not modify the queue")
    void viewerDoesNotModifyQueue() throws InterruptedException {
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(5);
        queue.put(1);
        queue.put(2);
        queue.put(3);
        Viewer viewer = new Viewer(queue);

        viewer.start();
        Thread.sleep(200);                              // Viewer arbeitet eine Weile
        viewer.interrupt();
        viewer.join(1000);

        assertEquals(List.of(1, 2, 3), new ArrayList<>(queue));
    }
    @Test
    @DisplayName("Viewer handles an empty queue and stops when interrupted")
    void viewerStopsWhenInterrupted() throws InterruptedException {
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(5);
        Viewer viewer = new Viewer(queue);

        viewer.start();
        Thread.sleep(200);
        assertTrue(viewer.isAlive());                   // leere Queue führt nicht zum Absturz

        viewer.interrupt();
        viewer.join(1000);
        assertFalse(viewer.isAlive());
    }
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
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(2);
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
            allProduced.addAll(p.getProduced());

        for (Consumer c : consumers)
            allConsumed.addAll(c.getConsumed());

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
        BlockingQueue<Integer> queue = assertTimeoutPreemptively(TIMEOUT, () -> {
            return ProjectApplication.runSystem(5, 3, 3);
        });

        assertEquals(0, queue.size());
    }

}
