package io.cloudsoft.winrm4j.winrm;

import io.cloudsoft.winrm4j.client.PayloadEncryptionMode;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLContext;

import org.apache.http.client.config.AuthSchemes;
import org.apache.http.conn.util.InetAddressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.cloudsoft.winrm4j.client.ShellCommand;
import io.cloudsoft.winrm4j.client.WinRmClient;
import io.cloudsoft.winrm4j.client.WinRmClientBuilder;
import io.cloudsoft.winrm4j.client.WinRmClientContext;
import io.cloudsoft.winrm4j.client.retry.RetryPolicy;

/**
 * Tool for executing commands over WinRM.
 * 
 * This class is not guaranteed to be thread safe.
 */
// Current implementation is thread safe because it creates a client per execute call
// but future implementations might re-use the client.
// TODO Create a client per WinRmTool
public class WinRmTool {
    private static final Logger LOG = LoggerFactory.getLogger(WinRmTool.class.getName());

    public static final int DEFAULT_WINRM_PORT = 5985;
    public static final int DEFAULT_WINRM_HTTPS_PORT = 5986;

    // TODO consider make them non-final and accessing the properties directly from builder.
    // This impose moving getEndpointUrl() to the WinRmTool.
    private final String address;
    private final String domain;
    private final String username;
    private final String password;
    private final String authenticationScheme;
    private Long operationTimeout;
    private Predicate<String> retryReceiveAfterOperationTimeout;
    private Integer retriesForConnectionFailures;
    private RetryPolicy failureRetryPolicy;
    private Long connectionTimeout;
    private Long receiveTimeout;
    private final boolean disableCertificateChecks;
    private final String workingDirectory;
    private final Map<String, String> environment;
    private final HostnameVerifier hostnameVerifier;
    private final SSLSocketFactory sslSocketFactory;
    private final SSLContext sslContext;
    private final WinRmClientContext context;
    private final boolean requestNewKerberosTicket;
    private PayloadEncryptionMode payloadEncryptionMode;

    public static class Builder {
        private String authenticationScheme = AuthSchemes.NTLM;
        private Boolean useHttps;
        private Integer port = null;
        private boolean disableCertificateChecks = false;
        private String address;
        private String domain;
        private String username;
        private String password;
        private String workingDirectory;
        private Map<String, String> environment;
        private HostnameVerifier hostnameVerifier;
        private SSLSocketFactory sslSocketFactory;
        private SSLContext sslContext;
        private WinRmClientContext context;
        private boolean requestNewKerberosTicket;
        private PayloadEncryptionMode payloadEncryptionMode;

        private static final Pattern matchPort = Pattern.compile(".*:(\\d+)$");

        public static Builder builder(String address, String username, String password) {
            return builder(address, null, username, password);
        }
        public static Builder builder(String address, String domain, String username, String password) {
            return new Builder(address, domain, username, password);
        }

        private Builder(String address, String domain, String username, String password) {
            this.address = address;
            this.domain = domain;
            this.username = username;
            this.password = password;
        }

        public Builder workingDirectory(String workingDirectory) {
            this.workingDirectory = WinRmClient.checkNotNull(workingDirectory, "workingDirectory");
            return this;
        }
        public Builder environment(Map<String, String> environment) {
            this.environment = WinRmClient.checkNotNull(environment, "environment");
            return this;
        }

        /**
         * @deprecated since 0.6.0
         */
        @Deprecated
        public Builder setAuthenticationScheme(String authenticationScheme) {
            return authenticationScheme(authenticationScheme);
        }

        public Builder authenticationScheme(String authenticationScheme) {
            this.authenticationScheme = authenticationScheme;
            return this;
        }

        public Builder disableCertificateChecks(boolean disableCertificateChecks) {
            this.disableCertificateChecks = disableCertificateChecks;
            return this;
        }

        public Builder useHttps(boolean useHttps) {
            this.useHttps = useHttps;
            return this;
        }
        
        public Builder hostnameVerifier(HostnameVerifier hostnameVerifier) {
        	this.hostnameVerifier = hostnameVerifier;
        	return this;
        }

        public Builder sslSocketFactory(SSLSocketFactory sslSocketFactory) {
	        this.sslSocketFactory = sslSocketFactory;
	        return this;
        }
        
        public Builder sslContext(SSLContext sslContext) {
        	this.sslContext = sslContext;
        	return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }
        
        public Builder context(WinRmClientContext context) {
            this.context = context;
            return this;
        }
        
        public Builder requestNewKerberosTicket(boolean requestNewKerberosTicket) {
            this.requestNewKerberosTicket = requestNewKerberosTicket;
            return this;
        }

        public Builder payloadEncryptionMode(PayloadEncryptionMode x) {
            this.payloadEncryptionMode = x;
            return this;
        }

        public WinRmTool build() {
            return new WinRmTool(getEndpointUrl(address, useHttps, port),
                    domain, username, password, authenticationScheme,
                    disableCertificateChecks, workingDirectory,
                    environment, hostnameVerifier, sslSocketFactory, sslContext,
                    context, requestNewKerberosTicket, payloadEncryptionMode);
        }

        // TODO remove arguments when method WinRmTool.connect() is removed
        private static String getEndpointUrl(String address, Boolean useHttps, Integer port) {
            if (address.startsWith("http:") || address.startsWith("https:")) {
                if (useHttps != null) {
                    if (useHttps && address.startsWith("http:"))
                        throw new IllegalArgumentException("Invalid setting useHttps and address starting http://");
                    if (!useHttps && address.startsWith("https:"))
                        throw new IllegalArgumentException("Invalid setting useHttp and address starting https://");
                }
                return address;
            } else {
                if (InetAddressUtils.isIPv6Address(address) && !address.startsWith("[")) {
                    address = "[" + address + "]";
                }

                Matcher matcher = matchPort.matcher(address);

                if (matcher.matches()) {
                    if (useHttps == null) {
                        useHttps = matcher.group(1).equals("5986");
                    }
                    return (useHttps ? "https" : "http") + "://" + address + "/wsman";
                } else {
                    if (useHttps != null) {
                        port = port != null ? port : (useHttps ? DEFAULT_WINRM_HTTPS_PORT : DEFAULT_WINRM_PORT);
                    }
                    if (address==null) throw new IllegalStateException("Address is required, either as host or IP or URL");
                    if (port==null) throw new IllegalStateException("Either port or useHttps is required, or a URL supplied as address");
                    if (useHttps != null && useHttps) {
                        return "https://" + address + ":" + port + "/wsman";
                    } else {
                        return "http://" + address + ":" + port + "/wsman";
                    }
                }
            }
        }
    }

    @Deprecated /** @deprecated use bigger constructor */
    private WinRmTool(String address, String domain, String username,
            String password, String authenticationScheme,
            boolean disableCertificateChecks, String workingDirectory,
            Map<String, String> environment, HostnameVerifier hostnameVerifier,
            SSLSocketFactory sslSocketFactory, SSLContext sslContext, WinRmClientContext context,
            boolean requestNewKerberosTicket) {
        this.disableCertificateChecks = disableCertificateChecks;
        this.address = address;
        this.domain = domain;
        this.username = username;
        this.password = password;
        this.authenticationScheme = authenticationScheme;
        this.workingDirectory = workingDirectory;
        this.environment = environment;
        this.hostnameVerifier = hostnameVerifier;
        this.sslSocketFactory = sslSocketFactory;
        this.sslContext = sslContext;
        this.context = context;
        this.requestNewKerberosTicket = requestNewKerberosTicket;
    }

    private WinRmTool(String address, String domain, String username,
                      String password, String authenticationScheme,
                      boolean disableCertificateChecks, String workingDirectory,
                      Map<String, String> environment, HostnameVerifier hostnameVerifier,
                      SSLSocketFactory sslSocketFactory, SSLContext sslContext, WinRmClientContext context,
                      boolean requestNewKerberosTicket, PayloadEncryptionMode payloadEncryptionMode) {
        this.disableCertificateChecks = disableCertificateChecks;
        this.address = address;
        this.domain = domain;
        this.username = username;
        this.password = password;
        this.authenticationScheme = authenticationScheme;
        this.workingDirectory = workingDirectory;
        this.environment = environment;
        this.hostnameVerifier = hostnameVerifier;
        this.sslSocketFactory = sslSocketFactory;
        this.sslContext = sslContext;
        this.context = context;
        this.requestNewKerberosTicket = requestNewKerberosTicket;
        this.payloadEncryptionMode = payloadEncryptionMode;
    }

    /**
     * Executes a Native Windows commands.
     * 
     * Current implementation is to concatenate the commands using <code>" &amp; "</code>.
     * 
     * Consider instead uploading a script file, and then executing that as a one-line command.
     * 
     * See {@link #executeCommand(String)} for limitations, e.g. about command length.
     * 
     * @since 0.2
     */
    public WinRmToolResponse executeCommand(List<String> commands) {
    	return executeCommand(joinCommands(commands));
    }

    public WinRmToolResponse executeCommand(List<String> commands, Writer out, Writer err) {
        return executeCommand(joinCommands(commands), out, err);
    }
    /**
     * Updates operationTimeout for the next <code>executeXxx</code> call
     *
     * @see <a href="http://www.dmtf.org/sites/default/files/standards/documents/DSP0226_1.2.0.pdf">DSP0226_1.2.0.pdf</a>
     * @param operationTimeout in milliseconds
     *                         default value {@link WinRmClient.Builder#DEFAULT_OPERATION_TIMEOUT}
     *                         If operations cannot be completed in a specified time,
     *                         the service returns a fault so that a client can comply with its obligations.
     */
    public void setOperationTimeout(Long operationTimeout) {
        this.operationTimeout = operationTimeout;
    }

	/**
	 * Update connectionTimeout
	 *
	 * @param connectionTimeout in milliseconds
	 *                         default value {@link WinRmClientBuilder#DEFAULT_CONNECTION_TIMEOUT}
	 */
	public void setConnectionTimeout(Long connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	/**
	 * Update receiveTimeout
	 *
	 * @param receiveTimeout in milliseconds
	 *                         default value {@link WinRmClientBuilder#DEFAULT_RECEIVE_TIMEOUT}
	 */
	public void setReceiveTimeout(Long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

    public void setRetryReceiveAfterOperationTimeout(Predicate<String> retryReceiveAfterOperationTimeout) {
        this.retryReceiveAfterOperationTimeout = retryReceiveAfterOperationTimeout;
    }

    public void alwaysRetryReceiveAfterOperationTimeout() {
        setRetryReceiveAfterOperationTimeout(WinRmClientBuilder.alwaysRetryReceiveAfterOperationTimeout());
    }

    public void neverRetryReceiveAfterOperationTimeout() {
        setRetryReceiveAfterOperationTimeout(WinRmClientBuilder.neverRetryReceiveAfterOperationTimeout());
    }

    /**
     * Convenience method to define a simple retry policy with the default pause.
     */
    public void setRetriesForConnectionFailures(Integer retriesForConnectionFailures) {
        setFailureRetryPolicy(WinRmClientBuilder.simpleCounterRetryPolicy(retriesForConnectionFailures));
    }

    public void setFailureRetryPolicy(RetryPolicy failureRetryPolicy) {
        this.failureRetryPolicy = failureRetryPolicy;
    }

    /**
     * Executes a Native Windows command.
     * It is creating a new Shell on the destination host each time it is being called.
     * @param command The command is limited to 8096 bytes.
     *                Maximum length of the command can be even smaller depending on the platform.
     *                https://support.microsoft.com/en-us/kb/830473
     * @since 0.2
     */
    public WinRmToolResponse executeCommand(String command) {
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        return executeCommand(command, out, err);
    }

    public WinRmToolResponse executeCommand(String command, Writer out, Writer err) {
        WinRmClient.checkNotNull(out, "Out Writer");
        WinRmClient.checkNotNull(err, "Err Writer");
        WinRmClientBuilder builder = WinRmClient.builder(address);
        builder.authenticationScheme(authenticationScheme);
        if (operationTimeout != null) {
            builder.operationTimeout(operationTimeout);
        }
        if (retryReceiveAfterOperationTimeout != null) {
            builder.retryReceiveAfterOperationTimeout(retryReceiveAfterOperationTimeout);
        }
        if (connectionTimeout != null) {
            builder.connectionTimeout(connectionTimeout);
        }
        if (receiveTimeout != null) {
            builder.receiveTimeout(receiveTimeout);
        }
        if (username != null && password != null) {
            builder.credentials(domain, username, password);
        }
        if (disableCertificateChecks) {
            LOG.trace("Disabled check for https connections " + this);
            builder.disableCertificateChecks(disableCertificateChecks);
        }
        if (hostnameVerifier != null) {
        	builder.hostnameVerifier(hostnameVerifier);
        }
        if (sslSocketFactory != null) {
        	builder.sslSocketFactory(sslSocketFactory);
        }
        if (sslContext != null) {
        	builder.sslContext(sslContext);
        }
        if (workingDirectory != null) {
            builder.workingDirectory(workingDirectory);
        }
        if (environment != null) {
            builder.environment(environment);
        }
        if (failureRetryPolicy != null) {
            builder.failureRetryPolicy(failureRetryPolicy);
        }
        if (context != null) {
            builder.context(context);
        }
        if (requestNewKerberosTicket) {
            builder.requestNewKerberosTicket(requestNewKerberosTicket);
        }
        builder.payloadEncryptionMode(payloadEncryptionMode);

        WinRmToolResponse winRmToolResponse;

        try(WinRmClient client = builder.build()) {
            try (ShellCommand shell = client.createShell()) {
                int code = shell.execute(command, out, err);
                winRmToolResponse = new WinRmToolResponse(out.toString(), err.toString(), code);
                winRmToolResponse.setNumberOfReceiveCalls(shell.getNumberOfReceiveCalls());
            }
        }

        return winRmToolResponse;
    }

    /**
     * Executes a Power Shell command.
     * It is creating a new Shell on the destination host each time it is being called.
     * @since 0.2
     */
    public WinRmToolResponse executePs(String psCommand) {
        return executePs(psCommand, new StringWriter(), new StringWriter());
    }

    /**
     * Executes a Power Shell command.
     * It is creating a new Shell on the destination host each time it is being called.
     * @since 0.2
     */
    public WinRmToolResponse executePs(String psCommand, Writer out, Writer err) {
        return executeCommand(compilePs(psCommand), out, err);
    }

    /**
     * Execute a list of Power Shell commands as one command.
     * The method translates the list of commands to a single String command with a 
     * <code>"\r\n"</code> delimiter and a terminating one.
     * 
     * Consider instead uploading a script file, and then executing that as a one-line command.
     */
    public WinRmToolResponse executePs(List<String> commands) {
        return executePs(commands, new StringWriter(), new StringWriter());
    }

    /**
     * Execute a list of Power Shell commands as one command.
     * The method translates the list of commands to a single String command with a
     * <code>"\r\n"</code> delimiter and a terminating one.
     *
     * Consider instead uploading a script file, and then executing that as a one-line command.
     */
    public WinRmToolResponse executePs(List<String> commands, Writer out, Writer err) {
        return executeCommand(compilePs(joinPs(commands)), out, err);
    }

    private String compilePs(String psScript) {
        byte[] cmd = psScript.getBytes(Charset.forName("UTF-16LE"));
        String arg = jakarta.xml.bind.DatatypeConverter.printBase64Binary(cmd);
        return "powershell -encodedcommand " + arg;
    }

    /**
     * Execute a list of Windows Native commands as one command.
     * The method translates the list of commands to a single String command with a <code>" &amp; "</code> 
     * delimiter and a terminating one.
     * 
     * @deprecated since 0.2; instead use {@link #executeCommand(List)} to remove ambiguity
     *             between native commands and powershell.
     */
    @Deprecated
    public WinRmToolResponse executeScript(List<String> commands) {
        return executeCommand(commands);
    }

    /**
     * @deprecated since 0.2; instead use {@link #executeCommand(String)} to remove ambiguity
     *             between native commands and powershell.
     */
    @Deprecated
    public WinRmToolResponse executeScript(String commands) {
        return executeCommand(commands);
    }

    private String joinCommands(List<String> commands) {
        return join(commands, " & ", false);
    }

    /**
     * PS commands are base64 encoded so we can use the normal new line
     * Windows delimiter here.
     */
    private String joinPs(List<String> commands) {
        return join(commands, "\r\n", true);
    }

    private String join(List<String> commands, String delim, boolean endWithDelim) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String command : commands) {
        	if (first) {
        		first = false;
        	} else {
                builder.append(delim);
        	}
            builder.append(command);
        }
        if (endWithDelim) {
        	builder.append(delim);
        }
        return builder.toString();
    }

}
