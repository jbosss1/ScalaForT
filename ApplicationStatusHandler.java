import java.time.Duration;

public class ApplicationStatusHandler implements Handler {

    private final Client client;

    public ApplicationStatusHandler(Client client) {
        this.client = client;
    }

    @Override
    public ApplicationStatusResponse performOperation(String id) {
        if (id == null) {
            return new ApplicationStatusResponse.Failure(null, 0);
        }

        long startTime = System.currentTimeMillis();
        int retriesCount = 0;

        while (System.currentTimeMillis() - startTime < 15000) {
            Response response1 = client.getApplicationStatus1(id);
            Response response2 = client.getApplicationStatus2(id);

            ApplicationStatusResponse result = processResponses(response1, response2);
            if (result != null) {
                return result;
            }

            retriesCount++;
        }

        return new ApplicationStatusResponse.Failure(null, retriesCount);
    }

private ApplicationStatusResponse processResponses(Response response1, Response response2) {
    if (response1 instanceof Response.Success success1) {
        return new ApplicationStatusResponse.Success(success1.applicationId(), success1.applicationStatus());
    }

    if (response2 instanceof Response.Success success2) {
        return new ApplicationStatusResponse.Success(success2.applicationId(), success2.applicationStatus());
    }

    if (response1 instanceof Response.RetryAfter retryAfter1 && response2 instanceof Response.RetryAfter retryAfter2) {
        Duration maxDelay = retryAfter1.delay().compareTo(retryAfter2.delay()) > 0 ? retryAfter1.delay() : retryAfter2.delay();
        sleep(maxDelay);
        return null;
    }

    if (response1 instanceof Response.Failure && response2 instanceof Response.Failure) {
        return new ApplicationStatusResponse.Failure(Duration.ofMillis(System.currentTimeMillis()), 0);
    }

    return null;
}


    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
