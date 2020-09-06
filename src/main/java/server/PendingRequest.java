package server;

import com.sun.net.httpserver.HttpExchange;

public class PendingRequest {
    private HttpExchange httpExchange;

    public PendingRequest(HttpExchange httpExchange) {
        this.httpExchange = httpExchange;

    }

    public HttpExchange getHttpExchange() {
        return httpExchange;
    }
}
