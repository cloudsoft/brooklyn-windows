package io.cloudsoft.winrm4j.client.retry;

import jakarta.xml.ws.WebServiceException;

/**
 * Policy to decide if a retry should be done.
 * 
 * Those implementing this interface are encouraged to do so in a thread-safe way.
 * 
 * @since 0.8.0
 */
public interface RetryPolicy {

	RetryDecision onWebServiceException(WebServiceException exception, int numAttempts);
}
