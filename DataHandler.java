import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataHandler implements Handler {

    private final Client client;
    private final ExecutorService executorService;

    public DataHandler(Client client) {
        this.client = client;
        this.executorService = Executors.newFixedThreadPool(10);
    }

    @Override
    public Duration timeout() {
        return Duration.ofMillis(500);
    }

    @Override
    public void performOperation() {
        while (true) {
            Event event = client.readData();
            distributeData(event);
        }
    }

    private void distributeData(Event event) {
        for (Address recipient : event.recipients()) {
            executorService.execute(() -> sendDataWithRetry(recipient, event.payload()));
        }
    }

    private void sendDataWithRetry(Address recipient, Payload payload) {
        while (true) {
            Result result = client.sendData(recipient, payload);
            if (result == Result.ACCEPTED) {
                break;
            } else if (result == Result.REJECTED) {
                sleep(timeout());
            }
        }
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
