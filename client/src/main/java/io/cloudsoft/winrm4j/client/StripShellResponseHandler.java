package io.cloudsoft.winrm4j.client;

import java.util.Iterator;
import java.util.Set;

import javax.xml.namespace.QName;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;

public class StripShellResponseHandler implements SOAPHandler<SOAPMessageContext> {

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        boolean isResponse = Boolean.FALSE.equals(context.get (MessageContext.MESSAGE_OUTBOUND_PROPERTY));
        if (isResponse) {
            QName action = (QName) context.get(SOAPMessageContext.WSDL_OPERATION);
            if ("Create".equals(action.getLocalPart())) {
                Iterator<?> childIter = getBodyChildren(context);
                while(childIter.hasNext()) {
                    Object node = childIter.next();
                    if (node instanceof SOAPElement) {
                        SOAPElement el = (SOAPElement) node;
                        if ("Shell".equals(el.getLocalName())) {
                            childIter.remove();
                        }
                    }
                }
            }
        }
        return true;
    }

    private Iterator<?> getBodyChildren(SOAPMessageContext context) {
        try {
            SOAPEnvelope envelope = context.getMessage().getSOAPPart().getEnvelope();
            SOAPBody body = envelope.getBody();
            return body.getChildElements();
        } catch (SOAPException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    @Override
    public void close(MessageContext context) {
    }

    @Override
    public Set<QName> getHeaders() {
        return null;
    }

}
