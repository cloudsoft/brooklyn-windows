package io.cloudsoft.winrm4j.client;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceFeature;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.w3c.dom.Element;

import io.cloudsoft.winrm4j.client.shell.CommandLine;
import io.cloudsoft.winrm4j.client.shell.CommandStateType;
import io.cloudsoft.winrm4j.client.shell.DesiredStreamType;
import io.cloudsoft.winrm4j.client.shell.EnvironmentVariable;
import io.cloudsoft.winrm4j.client.shell.EnvironmentVariableList;
import io.cloudsoft.winrm4j.client.shell.Receive;
import io.cloudsoft.winrm4j.client.shell.ReceiveResponse;
import io.cloudsoft.winrm4j.client.shell.Shell;
import io.cloudsoft.winrm4j.client.shell.StreamType;
import io.cloudsoft.winrm4j.client.transfer.ResourceCreated;
import io.cloudsoft.winrm4j.client.wsman.CommandResponse;
import io.cloudsoft.winrm4j.client.wsman.Locale;
import io.cloudsoft.winrm4j.client.wsman.OptionSetType;
import io.cloudsoft.winrm4j.client.wsman.OptionType;
import io.cloudsoft.winrm4j.client.wsman.SelectorSetType;
import io.cloudsoft.winrm4j.client.wsman.SelectorType;
import io.cloudsoft.winrm4j.client.wsman.Signal;

/**
 * TODO confirm if parallel commands can be called in parallel in one shell (probably not)!
 */
public class WinRmClient {
    private static final String COMMAND_STATE_DONE = "http://schemas.microsoft.com/wbem/wsman/1/windows/shell/CommandState/Done";
    private static final int MAX_ENVELOPER_SIZE = 153600;
    private static final String RESOURCE_URI = "http://schemas.microsoft.com/wbem/wsman/1/windows/shell/cmd";

    private URL endpoint;
    private String username;
    private String password;
    private String workingDirectory;
    private Locale locale;
    private String operationTimeout;
    private Map<String, String> environment;

    private WinRm winrm;
    private String shellId;
    private SelectorSetType shellSelector;

    public static Builder builder(URL endpoint) {
        return new Builder(endpoint);
    }

    public static Builder builder(String endpoint) {
        return new Builder(endpoint);
    }

    public static class Builder {
        private static final java.util.Locale DEFAULT_LOCALE = java.util.Locale.US;
        private static final int DEFAULT_OPERATION_TIMEOUT = 60000;
        private WinRmClient client;
        public Builder(URL endpoint) {
            client = new WinRmClient(endpoint);
        }
        public Builder(String endpoint) {
            this(toUrlUnchecked(checkNotNull(endpoint, "endpoint")));
        }
        public Builder credentials(String username, String password) {
            client.username = checkNotNull(username, "username");
            client.password = checkNotNull(password, "password");
            return this;
        }
        public Builder locale(java.util.Locale locale) {
            Locale l = new Locale();
            l.setLang(checkNotNull(locale, "locale").toLanguageTag());
            client.locale = l;
            return this;
        }
        public Builder operationTimeout(long operationTimeout) {
            client.operationTimeout = toDuration(operationTimeout);
            return this;
        }
        public Builder workingDirectory(String workingDirectory) {
            client.workingDirectory = checkNotNull(workingDirectory, "workingDirectory");
            return this;
        }
        public Builder environment(Map<String, String> environment) {
            client.environment = checkNotNull(environment, "environment");
            return this;
        }
        public WinRmClient build() {
            if (client.locale == null) {
                locale(DEFAULT_LOCALE);
            }
            if (client.operationTimeout == null) {
                operationTimeout(DEFAULT_OPERATION_TIMEOUT);
            }
            WinRmClient ret = client;
            client = null;
            return ret;
        }
        private static URL toUrlUnchecked(String endpoint) {
            try {
                return new URL(endpoint);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(e);
            }
        }
        private static String toDuration(long operationTimeout) {
            BigDecimal bdMs = BigDecimal.valueOf(operationTimeout);
            BigDecimal bdSec = bdMs.divide(BigDecimal.valueOf(1000));
            DecimalFormat df = new DecimalFormat("PT#.###S", new DecimalFormatSymbols(java.util.Locale.ROOT));
            return df.format(bdSec);
        }
    }

    private WinRmClient(URL endpoint) {
        this.endpoint = endpoint;
    }

    public int command(String cmd, Writer out, Writer err) {
        checkNotNull(cmd, "command");
        WinRm service = getService();

        CommandLine cmdLine = new CommandLine();
        cmdLine.setCommand(cmd);
        OptionSetType optSetCmd = new OptionSetType();
        OptionType optConsolemodeStdin = new OptionType();
        optConsolemodeStdin.setName("WINRS_CONSOLEMODE_STDIN");
        optConsolemodeStdin.setValue("TRUE");
        optSetCmd.getOption().add(optConsolemodeStdin);
        OptionType optSkipCmdShell = new OptionType();
        optSkipCmdShell.setName("WINRS_SKIP_CMD_SHELL");
        optSkipCmdShell.setValue("FALSE");
        optSetCmd.getOption().add(optSkipCmdShell);

        CommandResponse cmdResponse = service.command(cmdLine, RESOURCE_URI, MAX_ENVELOPER_SIZE, operationTimeout, locale, shellSelector, optSetCmd);
        String commandId = cmdResponse.getCommandId();

        try {
            return receiveCommand(commandId, out, err);
        } finally {
            releaseCommand(commandId);
        }
    }

    private void releaseCommand(String commandId) {
        Signal signal = new Signal();
        signal.setCommandId(commandId);
        signal.setCode("http://schemas.microsoft.com/wbem/wsman/1/windows/shell/signal/terminate");
        winrm.signal(signal, RESOURCE_URI, MAX_ENVELOPER_SIZE, operationTimeout, locale, shellSelector);
    }

    private int receiveCommand(String commandId, Writer out, Writer err) {
        while(true) {
            Receive receive = new Receive();
            DesiredStreamType stream = new DesiredStreamType();
            stream.setCommandId(commandId);
            stream.setValue("stdout stderr");
            receive.setDesiredStream(stream);
            ReceiveResponse receiveResponse = winrm.receive(receive, RESOURCE_URI, MAX_ENVELOPER_SIZE, operationTimeout, locale, shellSelector);
            List<StreamType> streams = receiveResponse.getStream();
            for (StreamType s : streams) {
                byte[] value = s.getValue();
                if (value == null) continue;
                if (out != null && "stdout".equals(s.getName())) {
                    try {
                        //TODO use passed locale?
                        if (value.length > 0) {
                            out.write(new String(value));
                        }
                        if (Boolean.TRUE.equals(s.isEnd())) {
                            out.close();
                        }
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
                if (err != null && "stderr".equals(s.getName())) {
                    try {
                        //TODO use passed locale?
                        if (value.length > 0) {
                            err.write(new String(value));
                        }
                        if (Boolean.TRUE.equals(s.isEnd())) {
                            err.close();
                        }
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
            CommandStateType state = receiveResponse.getCommandState();
            if (COMMAND_STATE_DONE.equals(state.getState())) {
                return state.getExitCode().intValue();
            }
        }
    }
    
    private WinRm getService() {
        if (winrm != null) {
            return winrm;
        } else {
            return createService();
        }
    }

    private synchronized WinRm createService() {
        if (winrm != null) return winrm;

        WinRmService service = new WinRmService();
//        JaxWsDynamicClientFactory dcf = JaxWsDynamicClientFactory.newInstance();
//        Client client = dcf.createClient("people.wsdl", classLoader);
        winrm = service.getWinRmPort(
                // * Adds WS-Addressing headers and uses the submission spec namespace
                //   http://schemas.xmlsoap.org/ws/2004/08/addressing
                newMemberSubmissionAddressingFeature());

        // Needed to be async according to http://cxf.apache.org/docs/asynchronous-client-http-transport.html
//        Bus bus = BusFactory.getDefaultBus();
//        bus.setProperty(AsyncHTTPConduit.USE_ASYNC, Boolean.TRUE);

        Client client = ClientProxy.getClient(winrm);

        BindingProvider bp = (BindingProvider)winrm;

        Map<String, Object> requestContext = bp.getRequestContext();
        AddressingProperties maps = new AddressingProperties("http://schemas.xmlsoap.org/ws/2004/08/addressing");
        requestContext.put(JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES, maps);

        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint.toExternalForm());
        if (username != null && password != null) {
            bp.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, username);
            bp.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, password);

            // if NTLM transport
//            bp.getRequestContext().put(AsyncHTTPConduit.USE_ASYNC, Boolean.TRUE);
//            HttpAuthHeader.AUTH_TYPE_NEGOTIATE;

            // Needed to be async according to http://cxf.apache.org/docs/asynchronous-client-http-transport.html
//            bp.getRequestContext().put(AsyncHTTPConduit.USE_ASYNC, Boolean.TRUE);
//            Credentials creds = new NTCredentials(username, password, null, null);
//            bp.getRequestContext().put(Credentials.class.getName(), creds);
        }

        Shell shell = new Shell();
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

        OptionSetType optSetCreate = new OptionSetType();
        OptionType optNoProfile = new OptionType();
        optNoProfile.setName("WINRS_NOPROFILE");
        optNoProfile.setValue("FALSE");
        optSetCreate.getOption().add(optNoProfile);
        OptionType optCodepage = new OptionType();
        optCodepage.setName("WINRS_CODEPAGE");
        optCodepage.setValue("437");
        optSetCreate.getOption().add(optCodepage);
        
        ResourceCreated resourceCreated = winrm.create(shell, RESOURCE_URI, MAX_ENVELOPER_SIZE, operationTimeout, locale, optSetCreate);
        shellId = getShellId(resourceCreated);

        shellSelector = new SelectorSetType();
        SelectorType sel = new SelectorType();
        sel.setName("ShellId");
        sel.getContent().add(shellId);
        shellSelector.getSelector().add(sel);
        
        return winrm;
    }

    // TODO
    private static WebServiceFeature newMemberSubmissionAddressingFeature() {
        /*
         * Requires the following dependency so the feature is visible to maven.
         * But is it included in the IBM dist?
<dependency>
    <groupId>com.sun.xml.ws</groupId>
    <artifactId>jaxws-rt</artifactId>
    <version>2.2.10</version>
</dependency>
         */
        try {
            // com.ibm.websphere.wsaddressing.jaxws21.SubmissionAddressingFeature for IBM java (available only in WebSphere?)

            WSAddressingFeature webServiceFeature = new WSAddressingFeature();
//            webServiceFeature.setResponses(WSAddressingFeature.AddressingResponses.ANONYMOUS);
            webServiceFeature.setAddressingRequired(true);

            return webServiceFeature;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // TODO use typed output
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

    public void disconnect() {
        if (winrm != null) {
            winrm.delete(null, RESOURCE_URI, MAX_ENVELOPER_SIZE, operationTimeout, locale, shellSelector);
        }
    }

    private static <T> T checkNotNull(T check, String msg) {
        if (check == null) {
            throw new NullPointerException(msg);
        }
        return check;
    }
}
