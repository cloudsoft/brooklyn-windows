package io.cloudsoft.winrm4j.client;

import io.cloudsoft.winrm4j.client.encryption.AsyncHttpEncryptionAwareConduitFactory;
import io.cloudsoft.winrm4j.client.ntlm.NTCredentialsWithEncryption;
import io.cloudsoft.winrm4j.client.spnego.WsmanViaSpnegoSchemeFactory;
import java.io.Writer;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.net.URL;
import java.security.PrivilegedAction;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;

import java.util.function.Supplier;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.handler.Handler;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.cxf.Bus.BusState;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.security.NamePasswordCallbackHandler;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.http.HTTPConduitFactory;
import org.apache.cxf.transport.http.asyncclient.AsyncHTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.policy.MetadataConstants;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.KerberosCredentials;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.impl.auth.KerberosSchemeFactory;
import org.apache.http.impl.auth.NTLMSchemeFactory;
import org.apache.http.impl.client.TargetAuthenticationStrategy;
import org.apache.neethi.Policy;
import org.apache.neethi.builders.PrimitiveAssertion;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import io.cloudsoft.winrm4j.client.ntlm.NtlmMasqAsSpnegoSchemeFactory;
import io.cloudsoft.winrm4j.client.shell.EnvironmentVariable;
import io.cloudsoft.winrm4j.client.shell.EnvironmentVariableList;
import io.cloudsoft.winrm4j.client.shell.Shell;
import io.cloudsoft.winrm4j.client.transfer.ResourceCreated;
import io.cloudsoft.winrm4j.client.wsman.Locale;
import io.cloudsoft.winrm4j.client.wsman.OptionSetType;
import io.cloudsoft.winrm4j.client.wsman.OptionType;
import sun.awt.image.ImageWatched.Link;

/**
 * TODO confirm if commands can be called in parallel in one shell (probably not)!
 */
public class WinRmClient implements AutoCloseable {


    static final int MAX_ENVELOPER_SIZE = 153600;
    static final String RESOURCE_URI = "http://schemas.microsoft.com/wbem/wsman/1/windows/shell/cmd";

    private final String workingDirectory;
    private final Locale locale;
    private final Map<String, String> environment;
    private final PayloadEncryptionMode payloadEncryptionMode;
    private final WinRm service;
    private AsyncHttpEncryptionAwareConduitFactory factoryToCleanup;

    // Can be changed throughout object's lifetime, but deprecated
    private String operationTimeout;
    private Predicate<String> retryReceiveAfterOperationTimeout;

    private final WinRmClientContext context;
    private final boolean cleanupContext;

    private final WinRm winrm;
    private final RetryingProxyHandler retryingHandler;

    private ShellCommand shellCommand;

    private static final Logger LOG = LoggerFactory.getLogger(WinRmClient.class.getName());

    /**
     * Object identifier of Kerberos as mechanism used by GSS for obtain the TGT.
     *
     * http://oid-info.com/get/1.2.840.113554.1.2.2
     */
    private static final String KERBEROS_OID = "1.2.840.113554.1.2.2";

    /**
     * Default JAAS configuration for Kerberos authentication.
     */
    private static final Configuration JAAS_KERB_LOGIN_CONF = new KerberosJaasConfiguration();

    /**
     * Create a WinRmClient builder
     *
     * @param endpoint - the url of the WSMAN service in the format https://machine:5986/wsman
     */
    public static WinRmClientBuilder builder(URL endpoint) {
        return new WinRmClientBuilder(endpoint);
    }

    /**
     * Create a WinRmClient builder
     *
     * @param endpoint - the url of the WSMAN service in the format https://machine:5986/wsman
     */
    public static WinRmClientBuilder builder(String endpoint) {
        return new WinRmClientBuilder(endpoint);
    }

    /**
     * Create a WinRmClient builder
     *
     * @param endpoint - the url of the WSMAN service in the format https://machine:5986/wsman
     * @param authenticationScheme - one of Basic, NTLM, Kerberos. Default is NTLM (with Negotiate).
     * 
     * @deprecated since 0.6.0. Use {@link #builder(URL)} and {@link Builder#authenticationScheme(String)}.
     */
    @Deprecated
    public static Builder builder(URL endpoint, String authenticationScheme) {
        return new Builder(endpoint, authenticationScheme);
    }

    /**
     * Create a WinRmClient builder
     *
     * @param endpoint - the url of the WSMAN service in the format https://machine:5986/wsman
     * @param authenticationScheme - one of Basic, NTLM, Kerberos. Default is NTLM (with Negotiate).
     * 
     * @deprecated since 0.5.0. Use {@link #builder(String)} and {@link Builder#authenticationScheme(String)}.
     */
    @Deprecated
    public static Builder builder(String endpoint, String authenticationScheme) {
        return new Builder(endpoint, authenticationScheme);
    }

    /**
     * @deprecated since 0.6.0. Use {@link WinRmClientBuilder} instead.
     */
    @Deprecated
    public static class Builder extends WinRmClientBuilder {
        /** @deprecated since 0.6.0; will change to private */
        @Deprecated
        public static final Long DEFAULT_OPERATION_TIMEOUT = 60l * 1000l;

        /** @deprecated since 0.6.0. Use {@link WinRmClient#builder(URL, String)} instead. */
        @Deprecated
        public Builder(URL endpoint, String authenticationScheme) {
            this(endpoint);
            authenticationScheme(authenticationScheme);
        }

        /** @deprecated since 0.6.0. Use {@link WinRmClient#builder(String, String)} instead. */
        public Builder(String endpoint, String authenticationScheme) {
            this(WinRmClientBuilder.toUrlUnchecked(checkNotNull(endpoint, "endpoint")),
                    checkNotNull(authenticationScheme, "authenticationScheme"));
        }

        Builder(String endpoint) {
            super(endpoint);
        }

        Builder(URL endpoint) {
            super(endpoint);
        }
    }

    WinRmClient(WinRmClientBuilder builder) {
        boolean cleanupFactory = builder.endpointConduitFactory == null;

        this.workingDirectory = builder.workingDirectory;
        this.locale = builder.locale;
        this.operationTimeout = toDuration(builder.operationTimeout);
        this.retryReceiveAfterOperationTimeout = builder.retryReceiveAfterOperationTimeout;
        this.environment = builder.environment;

        if (builder.context != null) {
            this.context = builder.context;
            this.cleanupContext = false;
        } else {
            this.context = WinRmClientContext.newInstance();
            this.cleanupContext = true;
        }

        service = getService(builder);
        retryingHandler = new RetryingProxyHandler(service, builder.failureRetryPolicy);
        this.winrm = (WinRm) Proxy.newProxyInstance(WinRm.class.getClassLoader(),
                new Class[] {WinRm.class, BindingProvider.class},
                retryingHandler);
        this.payloadEncryptionMode = builder.payloadEncryptionMode();

        if (cleanupFactory) {
            this.factoryToCleanup = builder.endpointConduitFactory;
        }
    }

    /** 
     * @deprecated since 0.6.0. Re-build the client for a new operation timeout.
     * Note that the {@code receiveTimeout} is not changed.
     * {@code operationTimeout} is not a command time out but the polling
     * timeout so doesn't make much sense to change it mid-use.
     */
    @Deprecated
    public void setOperationTimeout(long timeout) {
        this.operationTimeout = toDuration(timeout);
    }

    private WinRm getService(WinRmClientBuilder builder) {
        WinRm service = WinRmFactory.newInstance(context.getBus(), builder);
        initializeClientAndService(service, builder);
        return service;
    }

    /**
     * @deprecated since 0.6.0. Use {@link #createShell()} and {@link ShellCommand#execute(String, Writer, Writer)} instead.
     */
    @Deprecated
    public int command(String cmd, Writer out, Writer err) {
        return initInstanceShell().execute(cmd, out, err);
    }

    /** @deprecated since 0.6.0. Implementation detail, access will be removed in future versions. */
    @Deprecated
    public int getNumberOfReceiveCalls() {
        if (shellCommand == null) {
            return 0;
        } else {
            return shellCommand.getNumberOfReceiveCalls();
        }
    }

    private static void initializeClientAndService(WinRm winrm, WinRmClientBuilder builder) {
        String endpoint = builder.endpoint.toExternalForm();
        String authenticationScheme = builder.authenticationScheme;
        String username = builder.username;
        String password = builder.password;
        String domain = builder.domain;
        boolean disableCertificateChecks = builder.disableCertificateChecks;
        HostnameVerifier hostnameVerifier = builder.hostnameVerifier;
        SSLSocketFactory sslSocketFactory = builder.sslSocketFactory;
        SSLContext sslContext = builder.sslContext;
        long connectionTimeout = builder.connectionTimeout;
        long connectionRequestTimeout = builder.connectionRequestTimeout;
        long receiveTimeout;
        if (builder.receiveTimeout != null) {
            receiveTimeout = builder.receiveTimeout;
        } else {
            receiveTimeout = operationToReceiveTimeout(builder.operationTimeout);
        }

        Client client = ClientProxy.getClient(winrm);

        if (builder.endpointConduitFactory!=null) {
            // this is different to endpoint properties
            client.getEndpoint().getEndpointInfo().setProperty(
                    HTTPConduitFactory.class.getName(), builder.endpointConduitFactory);
        }

        ServiceInfo si = client.getEndpoint().getEndpointInfo().getService();
        // when client.command is executed if doclit.bare is not set then this exception occurs:
        // Unexpected element {http://schemas.microsoft.com/wbem/wsman/1/windows/shell}CommandResponse found.
        // Expected {http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd}CommandResponse
        si.setProperty("soap.force.doclit.bare", true);
//        si.setProperty("soap.no.validate.parts", true);

        BindingProvider bp = (BindingProvider)winrm;
        
        @SuppressWarnings("rawtypes")
        List<Handler> handlerChain = Arrays.<Handler>asList(new StripShellResponseHandler());
        bp.getBinding().setHandlerChain(handlerChain);

        Policy policy = new Policy();
        policy.addAssertion(new PrimitiveAssertion(MetadataConstants.USING_ADDRESSING_2004_QNAME));
        bp.getRequestContext().put(PolicyConstants.POLICY_OVERRIDE, policy);

        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);
        boolean advancedHttpConfigNeeded = false;

        Supplier<Credentials> creds = () -> new NTCredentialsWithEncryption(username, password, null, domain);

        Map<String,AuthSchemeProvider> authSchemeRegistry = null;
        Set<String> authSchemes = null;

        switch (authenticationScheme) {
            case AuthSchemes.BASIC:
                if (builder.payloadEncryptionMode().isRequired()) {
                    throw new IllegalStateException("Encryption is required, which is not compatible with auth");
                }
                bp.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, username);
                bp.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, password);
                authSchemes = Collections.singleton(AuthSchemes.BASIC);

                break;

            case AuthSchemes.NTLM:
                authSchemeRegistry = new LinkedHashMap<>();
                authSchemeRegistry.put(AuthSchemes.NTLM, new NTLMSchemeFactory());
                authSchemeRegistry.put(AuthSchemes.SPNEGO, new NtlmMasqAsSpnegoSchemeFactory(builder.payloadEncryptionMode()));

                advancedHttpConfigNeeded = true;
                break;

            case AuthSchemes.KERBEROS:
                /*
                 * If Kerberos authentication is requested two modes can be used:
                 * 1) with SSO : if a JAAS configuration file is defined the authentication is done with an external
                 *    TGT get from the cache or a keyTab file or created after input credentials from prompt
                 *    (depending the configuration set in JAAS login configuration file).
                 * 2) login : if requested by the builder configuration, a Kerberos authentication will be done with the
                 *    credentials provided by the builder. The TGT obtained will be stored in the request context
                 *    in order to be used by the HttpAuthenticator to generate the Spnego token.
                 */
                if (builder.payloadEncryptionMode().isRequired()) {
                    throw new IllegalStateException("Encryption is required, but not implemented here for Kerberos");
                    // might not be too hard to do -- get the sealing keys from kerberos by extending CredentialsWithEncryption
                }
                if (builder.requestNewKerberosTicket) {
                    KerberosCredentials newCreds = getKerberosCreds(username, password);
                    creds = () -> newCreds;
                }

                authSchemeRegistry = new LinkedHashMap<>();
                authSchemeRegistry.put(AuthSchemes.KERBEROS, new KerberosSchemeFactory());

                advancedHttpConfigNeeded = true;
                break;

            case AuthSchemes.SPNEGO:
                authSchemeRegistry = new LinkedHashMap<>();
                authSchemeRegistry.put(AuthSchemes.SPNEGO, new WsmanViaSpnegoSchemeFactory());

                advancedHttpConfigNeeded = true;
                break;
            default:
                throw new UnsupportedOperationException("No such authentication scheme " + authenticationScheme+"; " +
                        "options are "+Arrays.asList(AuthSchemes.BASIC, AuthSchemes.NTLM, AuthSchemes.SPNEGO, AuthSchemes.KERBEROS));
        }

        if (authSchemeRegistry!=null) {
            if (authSchemes==null) authSchemes = authSchemeRegistry.keySet();
            RegistryBuilder<AuthSchemeProvider> rb = RegistryBuilder.<AuthSchemeProvider>create();
            authSchemeRegistry.forEach(rb::register);
            bp.getRequestContext().put(AuthSchemeProvider.class.getName(), rb.build());
        }

        if (authSchemes!=null) {
            if (builder.endpointConduitFactory==null) {
                // set this again mainly so we can set the target auth schemes; but also so we can fail if interceptors did not apply
                builder.endpointConduitFactory = new AsyncHttpEncryptionAwareConduitFactory(builder.payloadEncryptionMode(), builder.targetAuthSchemes(), null);
            } else {
                builder.endpointConduitFactory.targetAuthSchemes(authSchemes);
            }
        }

        if (advancedHttpConfigNeeded) {
            bp.getRequestContext().put(Credentials.class.getName(), creds.get());
            bp.getRequestContext().put("http.autoredirect", true);

            AsyncHTTPConduit httpClient = (AsyncHTTPConduit) client.getConduit();

            if (disableCertificateChecks) {
                TLSClientParameters tlsClientParameters = new TLSClientParameters();
                tlsClientParameters.setDisableCNCheck(true);
                tlsClientParameters.setTrustManagers(new TrustManager[]{new X509TrustManager() {
                    @Override public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {}
                    @Override public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {}
                    @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                }});
                httpClient.setTlsClientParameters(tlsClientParameters);
            }
            if (hostnameVerifier != null || sslSocketFactory != null || sslContext != null) {
                TLSClientParameters tlsClientParameters = new TLSClientParameters();
                tlsClientParameters.setHostnameVerifier(hostnameVerifier);
                tlsClientParameters.setSSLSocketFactory(sslSocketFactory);
                tlsClientParameters.setSslContext(sslContext);
                httpClient.setTlsClientParameters(tlsClientParameters);
            }
            HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
            httpClientPolicy.setAllowChunking(false);
            httpClientPolicy.setConnectionTimeout(connectionTimeout);
            httpClientPolicy.setConnectionRequestTimeout(connectionRequestTimeout);
            httpClientPolicy.setReceiveTimeout(receiveTimeout);

            httpClient.setClient(httpClientPolicy);
            httpClient.getClient().setAutoRedirect(true);
        }
    }

    /**
     * Get new Kerberos credentials (i.e a TGT) with the username and password provided.
     *
     * @param username	name of the user to authenticate (format is UPN@DOMAIN)
     * @param password	password of the user account
     * @return credentials wrapping the TGT which will be used for obtaining the SPNego token
     */
    private static KerberosCredentials getKerberosCreds(String username, String password) {
        // If the Kerberos Realm is in uppercases (which is the norm) and the domain in the UPN is in lowercases
        // a KrbException: "Message stream modified" is thrown. To avoid this exception we force the UPN in uppercases
        // Maybe this should be customizable with a parameter?
        String canonizedUsername = username.trim().toUpperCase();
        Subject subject = kerberosLogin(canonizedUsername, password);
        GSSCredential userCred = Subject.doAs(subject, new PrivilegedAction<GSSCredential>() {
            public GSSCredential run() {
                try {
                    GSSManager manager = GSSManager.getInstance();
                    GSSName principal = manager.createName(canonizedUsername, null);
                    Oid mechOid = new Oid(KERBEROS_OID);
                    return manager.createCredential(principal, GSSContext.DEFAULT_LIFETIME, mechOid,
                            GSSCredential.INITIATE_ONLY);
                } catch (GSSException e) {
                    throw new RuntimeException("Unable to create credential for user \"" //
                            + username + "\" after login", e);
                }
            }
        });
        return new KerberosCredentials(userCred);
    }

    /**
     * Authenticate the user with the provided password. The login send a request AS-REQ to the Authentication Server.
     * The response will contain the TGT which will be store in the Subject.
     *
     * @param username	name of the user to authenticate (format is UPN@DOMAIN)
     * @param password	password of the user account
     * @return subject of the authenticated user
     */
    private static Subject kerberosLogin(String username, String password) {
        CallbackHandler callbackHandler = new NamePasswordCallbackHandler(username, password);
        Subject subject;
        try {
            LoginContext lc = new LoginContext("", null, callbackHandler, JAAS_KERB_LOGIN_CONF);
            lc.login();
            subject = lc.getSubject();
        } catch (LoginException e) {
            throw new RuntimeException("Exception occured while authenticate the user \"" //
                    + username + "\" on the KDC", e);
        }
        LOG.debug("After kerberos login: subject=" + subject);
        return subject;
    }

    /**
     * Configuration for Kerberos login.<br>
     * When this configuration is used (instead of the static JAAS config file) the purpose is to obtain a new TGT from
     * the AS for the credentials provided to the {@link WinRmClientBuilder} and not to use an existing TGT from the
     * cache. Thus this configuration disable the cache and the prompt in order to force the use of the credentials
     * stored in the Subject after the login.
     */
    private static class KerberosJaasConfiguration extends Configuration {
        private final AppConfigurationEntry[] appConfigurationEntries;

        KerberosJaasConfiguration() {
            Map<String, String> options = new HashMap<>();
            options.put("doNoPrompt", "true");
            options.put("client", "true");
            options.put("isInitiator", "true");
            options.put("useTicketCache", "false");
            appConfigurationEntries = new AppConfigurationEntry[] { new AppConfigurationEntry(
                    "com.sun.security.auth.module.Krb5LoginModule", LoginModuleControlFlag.REQUIRED, options) };
        }

        @Override
        public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
            return appConfigurationEntries;
        }

    }

    /**
     * Creates a Shell resource on the server, available for executing commands through the {@link ShellCommand} object.
     * {@link ShellCommand#close()} the returned object after usage.
     */
    public ShellCommand createShell() {
        final Shell shell = new Shell();
        shell.getInputStreams().add("stdin");
        shell.getOutputStreams().add("stdout");
        shell.getOutputStreams().add("stderr");
        if (workingDirectory != null) {
            shell.setWorkingDirectory(workingDirectory);
        }
        if (environment != null && !environment.isEmpty()) {
            EnvironmentVariableList env = new EnvironmentVariableList();
            List<EnvironmentVariable> vars = env.getVariable();
            for (Entry<String, String> entry : environment.entrySet()) {
                EnvironmentVariable var = new EnvironmentVariable();
                var.setName(entry.getKey());
                var.setValue(entry.getValue());
                vars.add(var);
            }
            shell.setEnvironment(env);
        }

        final OptionSetType optSetCreate = new OptionSetType();
        OptionType optNoProfile = new OptionType();
        optNoProfile.setName("WINRS_NOPROFILE");
        optNoProfile.setValue("FALSE");
        optSetCreate.getOption().add(optNoProfile);
        OptionType optCodepage = new OptionType();
        optCodepage.setName("WINRS_CODEPAGE");
        optCodepage.setValue("437");
        optSetCreate.getOption().add(optCodepage);

        ResourceCreated resourceCreated = null;
        try {
            resourceCreated = winrm.create(shell, RESOURCE_URI, MAX_ENVELOPER_SIZE, operationTimeout, locale, optSetCreate);
        } catch (RuntimeException e) {
            RetryingProxyHandler.checkForRootErrorAuthorizationLoopAndPropagateAnnotated(e);
            throw e;
        }
        String shellId = getShellId(resourceCreated);

        return new ShellCommand(winrm, shellId, operationTimeout, retryReceiveAfterOperationTimeout, locale);
    }

    private static String getShellId(ResourceCreated resourceCreated) {
        XPath xpath = XPathFactory.newInstance().newXPath();
        for (Element el : resourceCreated.getAny()) {
            String shellId;
            try {
                shellId = xpath.evaluate("//*[local-name()='Selector' and @Name='ShellId']", el);
            } catch (XPathExpressionException e) {
                throw new IllegalStateException(e);
            }
            if (shellId != null && !shellId.isEmpty()) {
                return shellId;
            }
        }
        throw new IllegalStateException("Shell ID not fount in " + resourceCreated);
    }

    /**
     * @deprecated since 0.6.0. Use {@link ShellCommand#close()} instead.
     */
    @Deprecated
    public void disconnect() {
        if (context == null) return;
        boolean isBusRunning = context.getBus().getState() != BusState.SHUTDOWN;
        if (!isBusRunning) return;
        try {
            ShellCommand oldShellCmd = cleanupInstanceShell();
            if (oldShellCmd != null) {
                oldShellCmd.close();
            }
        } finally {
            if (cleanupContext) {
                context.getBus().shutdown(true);
            }
        }
    }

    @Override
    public void close() {
        if (factoryToCleanup!=null && !factoryToCleanup.isShutdown()) {
            factoryToCleanup.shutdown();
            factoryToCleanup = null;
        }

        if (context!=null && cleanupContext) {
            boolean isBusRunning = context.getBus().getState() != BusState.SHUTDOWN;
            if (isBusRunning) {
                context.getBus().shutdown(true);
            }
        }
    }

    private synchronized ShellCommand initInstanceShell() {
        if (shellCommand == null) {
            shellCommand = createShell();
        }
        return shellCommand;
    }

    private synchronized ShellCommand cleanupInstanceShell() {
        ShellCommand cmd = shellCommand;
        shellCommand = null;
        return cmd;
    }

    private static long operationToReceiveTimeout(long operationTimeout) {
        return operationTimeout + 60l * 1000l;
    }

    private static String toDuration(long operationTimeout) {
        BigDecimal bdMs = BigDecimal.valueOf(operationTimeout);
        BigDecimal bdSec = bdMs.divide(BigDecimal.valueOf(1000));
        DecimalFormat df = new DecimalFormat("PT#.###S", new DecimalFormatSymbols(java.util.Locale.ROOT));
        return df.format(bdSec);
    }

    public static <T> T checkNotNull(T check, String msg) {
        if (check == null) {
            throw new NullPointerException(msg);
        }
        return check;
    }

}
