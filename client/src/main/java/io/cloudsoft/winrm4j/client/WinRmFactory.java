package io.cloudsoft.winrm4j.client;

import io.cloudsoft.winrm4j.client.encryption.AsyncHttpEncryptionAwareConduitFactory;
import io.cloudsoft.winrm4j.client.encryption.DecryptAndVerifyInInterceptor;
import io.cloudsoft.winrm4j.client.encryption.SignAndEncryptOutInterceptor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.xml.ws.spi.Provider;
import jakarta.xml.ws.spi.ServiceDelegate;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WinRmFactory {
    private static final Logger LOG = LoggerFactory.getLogger(WinRmClient.class.getName());

    public static WinRm newInstance(Bus bus) {
        return newInstance(bus, null);
    }

    public static WinRm newInstance(Bus bus, WinRmClientBuilder builder) {
        Bus prevBus = BusFactory.getAndSetThreadDefaultBus(bus);
        try {
            // The default thread bus is set on the ClientImpl and used for further requests
            return createService(bus, builder);
        } finally {
            if (BusFactory.getThreadDefaultBus(false) != prevBus) {
                BusFactory.setThreadDefaultBus(prevBus);
            }
        }
    }

    private static WinRm createService(Bus bus, WinRmClientBuilder builder) {
        RuntimeException lastException = null;

        try {
            return doCreateServiceWithBean(bus, builder);
        } catch (RuntimeException e) {
            LOG.warn("Error creating WinRm service with mbean strategy (trying other strategies): "+e, e);
            lastException = e;
            if (builder.payloadEncryptionMode().isRequired()) {
                throw new IllegalStateException("MBean strategy must work to enable encryption, and failed with: "+e, e);
            }
        }

        /*
         * It's tedious getting the right Provider esp in OSGi.
         * 
         * We've tried a bunch of strategies, with the most promising tried here,
         * and detailed notes below.
         */

        try {
            return doCreateServiceWithReflectivelySetDelegate();
        } catch (RuntimeException e) {
            LOG.warn("Error creating WinRm service with reflective delegate (trying other strategies): "+e, e);
            lastException = e;
        }

        try {
            return doCreateServiceNormal();
        } catch (RuntimeException e) {
            LOG.warn("Error creating WinRm service with many strategies (giving up): "+e, e);
            lastException = e;
        }

        throw lastException;
    }

    // normal approach
    private static WinRm doCreateServiceNormal() {
        WinRmService service = doCreateService_1_CreateMinimalServiceInstance();
        return doCreateService_2_GetClient(service);
    }

    // force delegate
    // based on http://stackoverflow.com/a/31892206/109079
    private static WinRm doCreateServiceWithReflectivelySetDelegate() {
        WinRmService service = doCreateService_1_CreateMinimalServiceInstance();

        try {
            Field delegateField = jakarta.xml.ws.Service.class.getDeclaredField("delegate"); //ALLOW CXF SPECIFIC SERVICE DELEGATE ONLY!
            delegateField.setAccessible(true);
            ServiceDelegate previousDelegate = (ServiceDelegate) delegateField.get(service);
            if (!previousDelegate.getClass().getName().contains("cxf")) {
                ServiceDelegate serviceDelegate = ((Provider) Class.forName("org.apache.cxf.jaxws.spi.ProviderImpl").newInstance())
                        .createServiceDelegate(WinRmService.WSDL_LOCATION, WinRmService.SERVICE, service.getClass());
                delegateField.set(service, serviceDelegate);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reflectively setting CXF WS service delegate", e);
        }
        return doCreateService_2_GetClient(service);
    }
    
    // approach using JaxWsProxyFactoryBean
    
    private static WinRm doCreateServiceWithBean(Bus bus, WinRmClientBuilder builder) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.getClientFactoryBean().getServiceFactory().setWsdlURL(WinRmService.WSDL_LOCATION);
        factory.setServiceName(WinRmService.SERVICE);
        factory.setEndpointName(WinRmService.WinRmPort);
        factory.setBus(bus);

        if (builder!=null) {
            Map<String, Object> properties = new LinkedHashMap<String, Object>();
            List<Interceptor<? extends Message>> outInterceptors = new ArrayList<>();
            List<Interceptor<? extends Message>> inInterceptors = new ArrayList<>();

            if (builder.payloadEncryptionMode().isPermitted()) {
                // asymmetry - we need to subclass the conduit factory to get correct encryption after re-auth;
                // interceptors are _not_ re-run eg after thread timeout and reuth needed (see previous version in commit history);
                // but we can use the cleaner intercept pattern to decrypt results

                outInterceptors.add(new SignAndEncryptOutInterceptor(builder.payloadEncryptionMode()));
                inInterceptors.add(new DecryptAndVerifyInInterceptor(builder.payloadEncryptionMode()));

                // in addition to the out interceptor, we need a special factory to handle switching the payload
                // during/after authentication

                // also note that although the factory.setProperties are passed to the endpoint,
                // it doesn't work to override the conduit factory, ie this doesn't work:
//                properties.put(HTTPConduitFactory.class.getName(), new HttpEncryptingConduitFactory(builder.payloadEncryptionMode(), (Map) null));
                // we need to set it explicitly on the input, which this special property does:
                builder.endpointConduitFactory = new AsyncHttpEncryptionAwareConduitFactory(builder.payloadEncryptionMode(), builder.targetAuthSchemes(), (Map) null);
            }

            factory.setInInterceptors(inInterceptors);
            factory.setOutInterceptors(outInterceptors);
            factory.setProperties(properties);
        }

        return factory.create(WinRm.class);
    }

    private static WinRmService doCreateService_1_CreateMinimalServiceInstance() {
        return new WinRmService();
    }

    private static WinRm doCreateService_2_GetClient(WinRmService service) {
        return service.getWinRmPort();
    }

}
