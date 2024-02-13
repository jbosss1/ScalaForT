import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataHandler implements Handler {

    private final Client client;
    private final ExecutorService executorService;

    public DataHandler(Client client) {
        this.client = client;
        this.executorService = Executors.newWorkStealingPool();
    }

    @Override
    public Duration timeout() {
        return Duration.ofMillis(500);
    }

    @Override
    public void performOperation() {
        while (true) {
            Optional<Event> optionalEvent = Optional.ofNullable(client.readData());
            if (optionalEvent.isEmpty()) {
                break;
            }
            distributeData(optionalEvent.get());
        }
    }

    private void distributeData(Event event) {
        for (Address recipient : event.recipients()) {
            executorService.execute(() -> sendDataWithRetry(recipient, event.payload()));
        }
    }

    private void sendDataWithRetry(Address recipient, Payload payload) {
        while (true) {
            Optional<Event> optionalEvent = Optional.ofNullable(client.readData());
            if (optionalEvent.isEmpty()) {
                break;
            }
            distributeData(optionalEvent.get());
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
