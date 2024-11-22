import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.ThreadLocalRandom;
import java.time.LocalTime;

public class task2 {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final int openingHour = 9;
    private final int closingHour = 24;
    private final java.util.Map<String, Integer> stock = new java.util.concurrent.ConcurrentHashMap<>();

    public void addItem(String itemName, int quantity) {
        lock.writeLock().lock();
        try {
            stock.merge(itemName, quantity, Integer::sum);
            System.out.printf("Added %d of %s. Current stock: %d%n", quantity, itemName, stock.get(itemName));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void buyItem(String itemName, int quantity) {
        if (!isOpen()) {
            System.out.println("Store is closed!");
            return;
        }

        lock.writeLock().lock();
        try {
            stock.computeIfPresent(itemName, (key, value) -> {
                if (value >= quantity) {
                    System.out.printf("%d %s(s) purchased. Remaining stock: %d%n", quantity, itemName, value - quantity);
                    return value - quantity;
                } else {
                    System.out.printf("%s is not available or insufficient stock.%n", itemName);
                    return value;
                }
            });
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void viewStock() {
        lock.readLock().lock();
        try {
            System.out.println("Current stock: " + stock);
        } finally {
            lock.readLock().unlock();
        }
    }

    private boolean isOpen() {
        int currentHour = LocalTime.now().getHour();
        return currentHour >= openingHour && currentHour < closingHour;
    }

    public static void adminTask(task2 shop) {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 5000));
                shop.addItem("Laptop", ThreadLocalRandom.current().nextInt(1, 6));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void customerTask(task2 shop, int customerId) {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 5000));
                shop.buyItem("Laptop", ThreadLocalRandom.current().nextInt(1, 4));
                System.out.printf("Customer %d tried to buy.%n", customerId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        task2 shop = new task2();

        Thread adminThread = new Thread(() -> adminTask(shop));
        adminThread.start();

        int numCustomers = 5;
        Thread[] customerThreads = new Thread[numCustomers];
        for (int i = 0; i < numCustomers; i++) {
            int customerId = i + 1;
            customerThreads[i] = new Thread(() -> customerTask(shop, customerId));
            customerThreads[i].start();
        }

        Thread.sleep(10000);

        adminThread.interrupt();
        for (Thread customerThread : customerThreads) {
            customerThread.interrupt();
        }
        adminThread.join();
        for (Thread customerThread : customerThreads) {
            customerThread.join();
        }

        System.out.println("Simulation finished.");
    }
}
