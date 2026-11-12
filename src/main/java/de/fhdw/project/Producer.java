package de.fhdw.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Dies ist ein Produzent, der Elemente zu unserer eigenen {@link Queue} hinzufügen kann.
 */
public class Producer extends Thread {

    private final Queue queue;
    private final List<Integer> history = new ArrayList<>();
    private final int actionAmount;

    public Producer(String name, Queue queue, int actionAmount){
        super(name);
        this.queue = queue;
        this.actionAmount = actionAmount;
    }

    /**
     * Führt den Producer mit der gewünschten Anzahl der Aktionen aus.
     * Dabei fügt er diese Aktionen auch in seine Historie ein.
     */
    @Override
    public void run() {
        try {
            Random random = new Random();

            for (int i = 0; i < actionAmount; i++) {
                int inserted = random.nextInt();
                this.queue.put(inserted);

                synchronized (history) {
                    history.add(inserted);
                }

                System.out.println(getName() + " produziert: " + inserted);
            }
        } catch (InterruptedException e) {
            System.out.println("Producer thread interrupted");
        }
    }

    /**
     * Funktion zum Ausgeben der Historie dieses Producers.
     *
     * @return Historie als Integer Array
     */
    public Integer[] getProduced() {
        synchronized (history) {
            return this.history.toArray(new Integer[0]);
        }
    }

}
