package server;

import com.sun.net.httpserver.HttpExchange;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import static server.ViaServer.updateRequestArrayList;

public class WebServer {
    public static final int PORT = 4040;

    public static int getFileSize(String filename) {
        return (int) new File(filename).length();
    }

    public static byte[] getUploadedFileData(String filename) throws IOException {
        byte[] fileData = new byte[getFileSize(filename)];
        FileInputStream fileInputStream = new FileInputStream(filename);
        int readBytes = fileInputStream.read(fileData);
        System.out.println("ViaServer >> Read " + readBytes + " BYTES from file " + filename);
        fileInputStream.close();
        return fileData;
    }

    public static String getMimeType(String filename) {
        if (filename.endsWith(".html")) {
            return "text/html";
        } else if (filename.endsWith(".css")) {
            return "text/css";
        } else if (filename.endsWith(".js")) {
            return "text/javascript";
        } else if (filename.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (filename.endsWith(".json")) {
            return "application/json";
        } else if (filename.endsWith(".ico")) {
            return "image/vnd.microsoft.icon";
        } else if (filename.endsWith("events")) {
            return "text/event-stream";
        } else if (filename.endsWith(".txt")) {
            return "text/plain";
        }
        return "";
    }

    public static void sendRequestedFile(HttpExchange httpExchange, String requestedFileName) {
        if(requestedFileName.equals("/")){
            httpExchange.getResponseHeaders().add("Location","http://127.0.0.1:4040/upload.html");
            try {
                httpExchange.sendResponseHeaders(301,0);
                httpExchange.getRequestBody().close();
                httpExchange.getResponseBody().close();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        new Thread(() -> {
            try {
//            Send the HTTP headers
                httpExchange.getResponseHeaders().add("Content-Type", getMimeType(requestedFileName));
                httpExchange.sendResponseHeaders(200, getFileSize(requestedFileName));
//            Send the file
                OutputStream dataOut = httpExchange.getResponseBody();
                dataOut.write(getUploadedFileData(requestedFileName));
                dataOut.flush();
                dataOut.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }).start();
    }

    public static String getRequestedFileName(HttpExchange httpExchange, EventStream uploadEventStream, EventStream downloadEventStream) {
        String requestPath = httpExchange.getRequestURI().getPath().trim();
        if (requestPath.equals("/upload.html")) {
            /*Check if there is another tab open*/
            if (uploadEventStream != null) {
                if (uploadEventStream.isConnected()) {
                    return "src/main/web/error/tb.html";
                }
            }
            /*Clear the arrayList when there is a page refresh*/
            updateRequestArrayList.clear();
        } else if (requestPath.equals("/download.html")) {
            if (downloadEventStream != null) {
                if (downloadEventStream.isConnected()) {
                    return "src/main/web/error/tb.html";
                }
            }
        }
        String path = "src/main/web" + requestPath;
        File file = new File(path);
        if (!file.exists()) {
            path = "src/main/web/error/404.html";
        }
        return path;
    }

}
