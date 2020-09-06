package server;

import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class EventStream {
    private HttpExchange httpExchange;
    private BufferedWriter eventWriter;
    private boolean isConnected = true;

    public EventStream(HttpExchange httpExchange) {
        this.httpExchange = httpExchange;
        sendHeaders();
        eventWriter = new BufferedWriter(new OutputStreamWriter(httpExchange.getResponseBody()));
        sendPingEvents();
    }

    private void sendHeaders() {
        try {
            httpExchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            httpExchange.sendResponseHeaders(200, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendEvent(String event, String data) throws IOException {
        eventWriter.write("event: " + event + "\n");
        eventWriter.write("data: " + data + "\n");
        eventWriter.write("\n\n");
        eventWriter.flush();
    }

    public boolean isConnected() {
        return isConnected;
    }

    private void sendPingEvents() {
        new Thread(() -> {
            while (true) {
                if(!isConnected){
                    break;
                }
                try {
                    sendEvent("ping", "pong");
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    System.err.println(e.getMessage());
                    break;
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                    isConnected = false;
                }
            }
        }).start();
    }
}


