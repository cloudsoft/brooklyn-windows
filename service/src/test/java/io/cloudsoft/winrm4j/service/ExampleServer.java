package io.cloudsoft.winrm4j.service;

import jakarta.xml.ws.Endpoint;

public class ExampleServer {

    protected ExampleServer() throws Exception {
        System.out.println("Starting Server");
        Object implementor = new WinRm();
        String address = "http://localhost:5985/wsman";
        Endpoint.publish(address, implementor);
    }

    public static void main(String args[]) throws Exception {
        System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
        System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true");
        System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
        System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dump", "true");

        new ExampleServer();
        System.out.println("Server ready...");
        System.in.read();
    }
}
