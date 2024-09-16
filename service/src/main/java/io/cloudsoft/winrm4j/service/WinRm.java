package io.cloudsoft.winrm4j.service;

import java.util.List;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.xml.ws.Action;
import jakarta.xml.ws.BindingType;
import jakarta.xml.ws.RequestWrapper;

import io.cloudsoft.winrm4j.service.shell.Receive;
import io.cloudsoft.winrm4j.service.shell.ReceiveResponse;
import io.cloudsoft.winrm4j.service.shell.Shell;
import io.cloudsoft.winrm4j.service.shell.SignalResponse;
import io.cloudsoft.winrm4j.service.transfer.ResourceCreated;
import io.cloudsoft.winrm4j.service.wsman.Locale;
import io.cloudsoft.winrm4j.service.wsman.OptionSetType;
import io.cloudsoft.winrm4j.service.wsman.SelectorSetType;
import io.cloudsoft.winrm4j.service.wsman.Signal;

//https://msdn.microsoft.com/en-us/library/cc251731.aspx
//https://msdn.microsoft.com/en-us/library/cc251526.aspx

@WebService(targetNamespace="http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd")

// Can't use the annotation without adding an additional dependency, duplicating the built-in functionality
//@MemberSubmissionAddressing

// Soap 1.2 (uses the required application/soap+xml content type)
@BindingType("http://www.w3.org/2003/05/soap/bindings/HTTP/")
public class WinRm {

    @WebMethod(operationName = "Receive", action = "http://schemas.microsoft.com/wbem/wsman/1/windows/shell/Receive")
    @WebResult(name="ReceiveResponse", targetNamespace = "http://schemas.microsoft.com/wbem/wsman/1/windows/shell")
    @Action(input = "http://schemas.microsoft.com/wbem/wsman/1/windows/shell/Receive", output = "http://schemas.microsoft.com/wbem/wsman/1/windows/shell/ReceiveResponse")
    @SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
    public ReceiveResponse receive(
        @WebParam(name = "Receive", targetNamespace = "http://schemas.microsoft.com/wbem/wsman/1/windows/shell")
        Receive receive,
        @WebParam(name = "ResourceURI", targetNamespace = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd", header = true)
        String resourceURI,
        @WebParam(name = "MaxEnvelopeSize", targetNamespace = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd", header = true)
        int maxEnvelopeSize,
        @WebParam(name = "OperationTimeout", targetNamespace = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd", header = true)
        String operationTimeout,
        @WebParam(name = "Locale", targetNamespace = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd", header = true)
        Locale locale,
        @WebParam(name = "SelectorSet", targetNamespace = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd", header = true)
        SelectorSetType selectorSet
    ) {
        return null;
    }

    @WebMethod(operationName = "Delete", action = "http://schemas.xmlsoap.org/ws/2004/09/transfer/Delete")
    @WebResult(name = "DeleteResponse", targetNamespace = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd")
    @Action(input = "http://schemas.xmlsoap.org/ws/2004/09/transfer/Delete", output = "http://schemas.xmlsoap.org/ws/2004/09/transfer/DeleteResponse")
    @SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
    public void delete(
        @WebParam(name = "ResourceURI", targetNamespace = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd", header = true)
        String resourceURI,
        @WebParam(name = "MaxEnvelopeSize", targetNamespace = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd", header = true)
        int maxEnvelopeSize,
        @WebParam(name = "OperationTimeout", targetNamespace = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd", header = true)
        String operationTimeout,
        @WebParam(name = "Locale", targetNamespace = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd", header = true)
        Locale locale,
        @WebParam(name = "SelectorSet", targetNamespace = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd", header = true)
        SelectorSetType selectorSet) {
    }

    @WebMethod(operationName = "Signal", action = "http://schemas.microsoft.com/wbem/wsman/1/windows/shell/Signal")
    @WebResult(name = "SignalResponse", targetNamespace = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd")
    @Action(input = "http://schemas.microsoft.com/wbem/wsman/1/windows/shell/Signal", output = "http://schemas.microsoft.com/wbem/wsman/1/windows/shell/SignalResponse")
    @SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
    public SignalResponse signal(
        @WebParam(name = "Signal", targetNamespace = "http://schemas.microsoft.com/wbem/wsman/1/windows/shell")
        Signal signal,
        @WebParam(name = "ResourceURI", targetNamespace = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd", header = true)
        String resourceURI,
        @WebParam(name = "MaxEnvelopeSize", targetNamespace = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd", header = true)
        int maxEnvelopeSize,
        @WebParam(name = "OperationTimeout", targetNamespace = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd", header = true)
        String operationTimeout,
        @WebParam(name = "Locale", targetNamespace = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd", header = true)
        Locale locale,
        @WebParam(name = "SelectorSet", targetNamespace = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd", header = true)
        SelectorSetType selectorSet
    ) {
        return null;
    }

    @WebMethod(operationName = "Command", action="http://schemas.microsoft.com/wbem/wsman/1/windows/shell/Command")
    @WebResult(name="CommandId", targetNamespace = "http://schemas.microsoft.com/wbem/wsman/1/windows/shell")
    @RequestWrapper(localName="CommandLine", targetNamespace="http://schemas.microsoft.com/wbem/wsman/1/windows/shell", partName = "body")
    @Action(input = "http://schemas.microsoft.com/wbem/wsman/1/windows/shell/Command", output = "http://schemas.microsoft.com/wbem/wsman/1/windows/shell/CommandResponse")
    public String command(
        @WebParam(name = "Command", targetNamespace = "http://schemas.microsoft.com/wbem/wsman/1/windows/shell")
        String command,
        @WebParam(name = "Arguments", targetNamespace = "http://schemas.microsoft.com/wbem/wsman/1/windows/shell")
        List<String> arguments,
        @WebParam(name = "ResourceURI", targetNamespace = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd", header = true)
        String resourceURI,
        @WebParam(name = "MaxEnvelopeSize", targetNamespace = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd", header = true)
        int maxEnvelopeSize,
        @WebParam(name = "OperationTimeout", targetNamespace = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd", header = true)
        String operationTimeout,
        @WebParam(name = "Locale", targetNamespace = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd", header = true)
        Locale locale,
        @WebParam(name = "SelectorSet", targetNamespace = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd", header = true)
        SelectorSetType selectorSet,
        @WebParam(name = "OptionSet", targetNamespace = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd", header = true)
        OptionSetType optionSet
    ) {
        return null;
    }

    @WebMethod(operationName = "Create", action = "http://schemas.xmlsoap.org/ws/2004/09/transfer/Create")
    @Action(input = "http://schemas.xmlsoap.org/ws/2004/09/transfer/Create", output = "http://schemas.xmlsoap.org/ws/2004/09/transfer/CreateResponse")
    @WebResult(name = "ResourceCreated", targetNamespace = "http://schemas.xmlsoap.org/ws/2004/09/transfer", partName = "ResourceCreated")
    @SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
    public ResourceCreated create(
        @WebParam(name = "Shell", targetNamespace = "http://schemas.microsoft.com/wbem/wsman/1/windows/shell")
        Shell shell,
        @WebParam(name = "ResourceURI", targetNamespace = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd", header = true)
        String resourceURI,
        @WebParam(name = "MaxEnvelopeSize", targetNamespace = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd", header = true)
        int maxEnvelopeSize,
        @WebParam(name = "OperationTimeout", targetNamespace = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd", header = true)
        String operationTimeout,
        @WebParam(name = "Locale", targetNamespace = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd", header = true)
        Locale locale,
        @WebParam(name = "OptionSet", targetNamespace = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd", header = true)
        OptionSetType optionSet
    ) {
        return null;
    }

}
