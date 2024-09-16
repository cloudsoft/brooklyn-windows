package io.cloudsoft.winrm4j.client;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import jakarta.xml.ws.WebServiceException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import io.cloudsoft.winrm4j.client.retry.RetryPolicy;
import io.cloudsoft.winrm4j.client.retry.SimpleCounterRetryPolicy;
import io.cloudsoft.winrm4j.client.shell.CommandLine;
import io.cloudsoft.winrm4j.client.shell.Receive;
import io.cloudsoft.winrm4j.client.shell.ReceiveResponse;
import io.cloudsoft.winrm4j.client.shell.Shell;
import io.cloudsoft.winrm4j.client.transfer.ResourceCreated;
import io.cloudsoft.winrm4j.client.wsman.CommandResponse;
import io.cloudsoft.winrm4j.client.wsman.Locale;
import io.cloudsoft.winrm4j.client.wsman.OptionSetType;
import io.cloudsoft.winrm4j.client.wsman.SelectorSetType;
import io.cloudsoft.winrm4j.client.wsman.Signal;
import io.cloudsoft.winrm4j.client.wsman.SignalResponse;

public class RetryingProxyHandlerTest {

    private final AtomicReference<Function<RecordedCall, Object>> handler = 
    		new AtomicReference<>((v) -> { return null; });

    private RetryPolicy failureRetryPolicy;
    private RetryingProxyHandler retryingHandler;
    private RecordingWinRm recordingWinrm;
    private WinRm winrm;

	@BeforeMethod(alwaysRun=true)
	public void setUp() throws Exception {
	    recordingWinrm = new RecordingWinRm((v) -> handler.get().apply(v));
        failureRetryPolicy = new SimpleCounterRetryPolicy(1, 1, TimeUnit.MILLISECONDS);
		retryingHandler = new RetryingProxyHandler(recordingWinrm, failureRetryPolicy );
        winrm = (WinRm) Proxy.newProxyInstance(WinRm.class.getClassLoader(),
                new Class[] {WinRm.class},
                retryingHandler);
	}

	@Test
	public void testFailsAfterMaxRetries() throws Exception {
		AtomicInteger counter = new AtomicInteger(0);
        handler.set((v) -> {
        	int count = counter.getAndIncrement();
        	throw new WebServiceException("simulated failure " + count, new IOException("simulated failure " + count));
        });
        
        try {
        	winrm.create((Shell)null, "myResourceUri", 0, "myOperationTimeout", (Locale)null, (OptionSetType)null);
        	fail("Should have propagated exception");
        } catch (Exception e) {
        	WebServiceException cause = findCause(e, WebServiceException.class);
        	assertEquals(cause.getMessage(), "simulated failure 0");
        }
        
        assertEquals(recordingWinrm.calls.size(), 2);
        assertEquals(recordingWinrm.calls.get(0).method, "create");
        assertEquals(recordingWinrm.calls.get(1).method, "create");
	}

	@Test
	public void testReturnsWhenRetrySucceeds() throws Exception {
		ResourceCreated result = new ResourceCreated();
		AtomicInteger counter = new AtomicInteger(0);
        handler.set((v) -> {
        	int count = counter.getAndIncrement();
        	if (count > 0) {
        		return result;
        	} else {
        		throw new WebServiceException("simulated failure " + count, new IOException("simulated failure " + count));
        	}
        });
        
    	ResourceCreated actual = winrm.create((Shell)null, "myResourceUri", 0, "myOperationTimeout", (Locale)null, (OptionSetType)null);
    	
        assertSame(actual, result);
        assertEquals(recordingWinrm.calls.size(), 2);
        assertEquals(recordingWinrm.calls.get(0).method, "create");
        assertEquals(recordingWinrm.calls.get(1).method, "create");
	}

	@Test
	public void testExecutesOnlyOnceOnSuccess() throws Exception {
		ResourceCreated result = new ResourceCreated();
        handler.set((v) -> {
    		return result;
        });
        
    	ResourceCreated actual = winrm.create((Shell)null, "myResourceUri", 0, "myOperationTimeout", (Locale)null, (OptionSetType)null);
    	
        assertSame(actual, result);
        assertEquals(recordingWinrm.calls.size(), 1);
        assertEquals(recordingWinrm.calls.get(0).method, "create");
	}

	@Test
	public void testDoesNotRetryOnOtherExceptions() throws Exception {
        handler.set((v) -> {
    		throw new NullPointerException("simulated failure");
        });

        try {
        	winrm.create((Shell)null, "myResourceUri", 0, "myOperationTimeout", (Locale)null, (OptionSetType)null);
        	fail("Should have propagated exception");
        } catch (Exception e) {
        	NullPointerException cause = findCause(e, NullPointerException.class);
        	assertEquals(cause.getMessage(), "simulated failure");
        }
        
        assertEquals(recordingWinrm.calls.size(), 1);
        assertEquals(recordingWinrm.calls.get(0).method, "create");
	}

	@SuppressWarnings("unchecked")
	private static <T extends Throwable> T findCause(Exception e, Class<T> expected) throws Exception {
    	Optional<Throwable> cause = Iterables.tryFind(Throwables.getCausalChain(e), Predicates.instanceOf(expected));
    	if (cause.isPresent()) {
    		return (T) cause.get();
    	} else {
    		throw e;
    	}
	}
	
	static class RecordedCall {
		public final String method;
		public final List<?> args;
		
		RecordedCall(String method, List<?> args) {
			this.method = method;
			this.args = args;
		}
	}
	
	static class RecordingWinRm implements WinRm {

		public final List<RecordedCall> calls = Lists.newCopyOnWriteArrayList();
		
		final Function<RecordedCall, Object> handler;
		
		RecordingWinRm(Function<RecordedCall, Object> handler) {
			this.handler = Objects.requireNonNull(handler, "handler");
		}
		
		@Override
		public void delete(String resourceURI, int maxEnvelopeSize, String operationTimeout, Locale locale, SelectorSetType selectorSet) {
			RecordedCall call = new RecordedCall("delete", Arrays.asList(resourceURI, maxEnvelopeSize, operationTimeout, locale, selectorSet));
			calls.add(call);
			handler.apply(call);
		}

		@Override
		public ReceiveResponse receive(Receive receive, String resourceURI, int maxEnvelopeSize, String operationTimeout, Locale locale, SelectorSetType selectorSet) {
			RecordedCall call = new RecordedCall("receive", Arrays.asList(receive, resourceURI, maxEnvelopeSize, operationTimeout, locale, selectorSet));
			calls.add(call);
			return (ReceiveResponse) handler.apply(call);
		}

		@Override
		public SignalResponse signal(Signal signal, String resourceURI, int maxEnvelopeSize, String operationTimeout, Locale locale, SelectorSetType selectorSet) {
			RecordedCall call = new RecordedCall("signal", Arrays.asList(signal, resourceURI, maxEnvelopeSize, operationTimeout, locale, selectorSet));
			calls.add(call);
			return (SignalResponse) handler.apply(call);
		}

		@Override
		public CommandResponse command(CommandLine body, String resourceURI, int maxEnvelopeSize, String operationTimeout, Locale locale, SelectorSetType selectorSet, OptionSetType optionSet) {
			RecordedCall call = new RecordedCall("command", Arrays.asList(body, resourceURI, maxEnvelopeSize, operationTimeout, locale, selectorSet, optionSet));
			calls.add(call);
			return (CommandResponse) handler.apply(call);
		}

		@Override
		public ResourceCreated create(Shell shell, String resourceURI, int maxEnvelopeSize, String operationTimeout, Locale locale, OptionSetType optionSet) {
			RecordedCall call = new RecordedCall("create", Arrays.asList(shell, resourceURI, maxEnvelopeSize, operationTimeout, locale, optionSet));
			calls.add(call);
			return (ResourceCreated) handler.apply(call);
		}
	}
}
