package io.cloudsoft.winrm4j.client.retry;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.xml.ws.WebServiceException;

/**
 * Retry policy that uses max number of retries.
 * 
 * @since 0.8.0
 */
public class SimpleCounterRetryPolicy implements RetryPolicy {
    private final int maxRetries;
    private final long pauseTimeMillis;

    /**
     * @param maxRetries  total number of retries (e.g. {@code 1} means two attempts).
     * @param pauseTime   duration of the sleep between each retries
     * @param pauseUnit   unit of the {@code pauseTime} duration
     */
    public SimpleCounterRetryPolicy(int maxRetries, long pauseTime, TimeUnit pauseUnit) {
        this.maxRetries = maxRetries;
        this.pauseTimeMillis = pauseUnit.toMillis(pauseTime);
    }

	@Override
	public RetryDecision onWebServiceException(WebServiceException exception, int numAttempts) {
        Optional<String> reason = Optional.of("Attempt " + numAttempts + " of " + (maxRetries + 1));
		return new BasicRetryDecision(numAttempts <= maxRetries, pauseTimeMillis, reason);
	}
}
