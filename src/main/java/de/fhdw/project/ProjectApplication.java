package de.fhdw.project;

import java.util.ArrayList;
import java.util.List;

public class ProjectApplication {

    public static void main(String[] args) throws InterruptedException {
        runSystem(3, 4, 2, 2, 4);

        System.out.println("Simulation beendet.");
    }

    /**
     * Funktion zum Simulieren des Erzeuger-Verbraucher-Systems.
     *
     * @param capacity Kapazität der Queue
     * @param producerCount Anzahl der Producer
     * @param consumerCount Anzahl der Consumer
     * @param actionAmountProducer Anzahl der Aktionen jedes Producers
     * @param actionAmountConsumer Anzahl der Aktionen jedes Consumers
     * @return Die überbleibende Queue nach der Simulation
     * @throws InterruptedException Deckt den Spezialfall des Unterbrechens der Producer und Consumer ab
     */
    public static Queue runSystem(int capacity, int producerCount, int consumerCount,
                                  int actionAmountProducer, int actionAmountConsumer)
            throws InterruptedException {

        if (capacity <= 0 || producerCount <= 0 || consumerCount <= 0
                || actionAmountProducer <= 0 || actionAmountConsumer <= 0) {
            throw new IllegalArgumentException("Die durchgegebenen Parameter müssen größer als 0 sein.");
        }

        long totalProduced = (long) producerCount * actionAmountProducer;
        long totalConsumed = (long) consumerCount * actionAmountConsumer;

        if (totalProduced != totalConsumed) {
            throw new IllegalArgumentException("Die Producer und Consumer interagieren unterschiedlich oft mit der Queue.");
        }

        Queue queue = new Queue(capacity);
        List<Thread> workers = new ArrayList<>();

        for (int i = 1; i <= producerCount; i++) {
            String producerName = "p" + i;
            workers.add(new Producer(producerName, queue, actionAmountProducer));
        }

        for (int i = 1; i <= consumerCount; i++) {
            String consumerName = "c" + i;
            workers.add(new Consumer(consumerName, queue, actionAmountConsumer));
        }

        try {
            for (Thread worker : workers) {
                worker.start();
            }

            for (Thread worker : workers) {
                worker.join();
            }
        } finally {
            for (Thread worker : workers) {
                worker.interrupt();
            }

            for (Thread worker : workers) {
                worker.join();
            }
        }

        return queue;
    }

}
