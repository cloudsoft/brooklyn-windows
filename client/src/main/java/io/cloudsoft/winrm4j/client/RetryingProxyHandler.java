package io.cloudsoft.winrm4j.client;

import io.cloudsoft.winrm4j.client.retry.RetryDecision;
import io.cloudsoft.winrm4j.client.retry.RetryPolicy;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RetryingProxyHandler implements InvocationHandler {
    private static final Logger LOG = LoggerFactory.getLogger(RetryingProxyHandler.class);

    private final WinRm winrm;
    private RetryPolicy failureRetryPolicy;

    public RetryingProxyHandler(WinRm winrm, RetryPolicy failureRetryPolicy) {
        this.winrm = winrm;
        this.failureRetryPolicy = failureRetryPolicy;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Don't retry the "command" - could lead to unexpected side effects of having the script run multiple times.
        if (method.getName().equals("command")) {
            return method.invoke(winrm, args);
        } else {
            return invokeWithRetry(method, args);
        }
    }

    public Object invokeWithRetry(Method method, Object[] args)
            throws IllegalAccessException, InvocationTargetException {
        Throwable firstException = null;
        boolean tryAgain = true;
        int attempt = 0;

        do {
        	attempt++;
            try {
                return method.invoke(winrm, args);
            } catch (InvocationTargetException targetException) {
                Throwable e = targetException.getTargetException();
                checkForRootErrorAuthorizationLoopAndPropagateAnnotated(e);
                if (e instanceof SOAPFaultException) {
                    throw (SOAPFaultException) e;
                }
                if (!(e instanceof WebServiceException)) {
                    throw new IllegalStateException("Failure when calling " + method + args, e);
                }
                WebServiceException wsException = (WebServiceException) e;
                if (!(wsException.getCause() instanceof IOException)) {
                    throw new RuntimeException("Exception occurred while making winrm call", wsException);
                }
                if (firstException == null) {
                    firstException = wsException;
                }
                RetryDecision retryDecision = failureRetryPolicy.onWebServiceException(wsException, attempt);
                if (retryDecision.shouldRetry()) {
                    LOG.debug("On attempt " + attempt + ", ignoring exception and retrying (" + retryDecision.reason() + ")", 
                            wsException);
                    try {
                        Thread.sleep(retryDecision.pauseTimeMillis());
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Exception occured while making winrm call",
                                targetException);
                    }
                } else {
                    tryAgain = false;
                }
            }
        } while (tryAgain);
        
        LOG.debug("failed task \"" + method.getName() + "\" after " + attempt + " attempt(s), rethrowing first exception");
        throw new RuntimeException("failed task \"" + method.getName() + "\" after " + attempt + " attempt(s)", firstException);
    }

    public static void checkForRootErrorAuthorizationLoopAndPropagateAnnotated(Throwable e0) {
        Throwable e = e0;

        while (e!=null) {
            String es = e.toString();
            if (es.contains("ncompatible authentication schemes")) {
                // don't loop over ourselves
                if (e0 instanceof RuntimeException) throw (RuntimeException)e0;
                throw new RuntimeException(e0);
            }

            if (e.toString().contains("Authorization loop detected on Conduit")) {
                throw new IllegalStateException(
                        "Invalid credentials or incompatible authentication schemes",
                        e0);
            }

            if (e.getCause()==null || e.getCause()==e) {
                // some exceptions appear to be caused by themselves :(
                return;
            }
            e = e.getCause();
        }
    }

    /**
     * @deprecated instated use {@link WinRmClientBuilder#failureRetryPolicy(RetryPolicy)}
     */
    @Deprecated
    public void setRetriesForConnectionFailures(int retries) {
        LOG.warn("method RetryingProxyHandler#setRetriesForConnectionFailures has been deprecated,"
                + " please use instead WinRmClientBuilder#failureRetryPolicy");
        failureRetryPolicy = WinRmClientBuilder.simpleCounterRetryPolicy(retries);
    }

}
