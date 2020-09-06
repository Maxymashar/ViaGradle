package server;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import static server.WebServer.*;

public class ViaServer {
    public static ArrayList<UpdateRequest> updateRequestArrayList = new ArrayList<>();
    private static PendingRequest pendingRequest = null;
    private static EventStream uploadEventStream = null;
    private static EventStream downloadEventStream = null;

    private static void getUploadedFileData(HttpExchange httpExchange) {
        if (!httpExchange.getRequestHeaders().containsKey("Type")) {
            try {
                httpExchange.sendResponseHeaders(200, 0);
                httpExchange.getRequestBody().close();
                httpExchange.getResponseBody().close();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
//          Get the uploaded file data
            InputStream dataIn = httpExchange.getRequestBody();
            BufferedReader fdrReader = new BufferedReader(new InputStreamReader(dataIn));
            StringBuilder sb = new StringBuilder();
            while (true) {
                String line = fdrReader.readLine();
                if (line == null) {
                    break;
                }
                sb.append(line);
            }
//            Set the file data array
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<UpdateRequest>>() {
            }.getType();
            ArrayList<UpdateRequest> newUpdateRequests = gson.fromJson(sb.toString(), type);
            updateRequestArrayList.addAll(newUpdateRequests);
            // send the data to the downloader if the downloader is live
            try {
                downloadEventStream.sendEvent("update_list", gson.toJson(newUpdateRequests));
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
            System.out.println("UpdateRequest >> " + sb.toString());
            httpExchange.getResponseHeaders().add("Content-Type", getMimeType(".json"));
            httpExchange.sendResponseHeaders(200, sb.toString().getBytes().length);
            OutputStream dataOut = httpExchange.getResponseBody();
            dataOut.write(sb.toString().getBytes());
            dataOut.flush();

            dataOut.close();
            dataIn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendUploadedFileData(HttpExchange httpExchange) {
        if (!httpExchange.getRequestHeaders().containsKey("Type")) {
            try {
                httpExchange.getRequestBody().close();
                httpExchange.getResponseBody().close();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        new Thread(() -> {
            try {
                if (uploadEventStream != null && !uploadEventStream.isConnected()) {
                    updateRequestArrayList.clear();
                }
                Gson gson = new Gson();
                String response = gson.toJson(updateRequestArrayList);

//          Send the response headers
                httpExchange.getResponseHeaders().add("Content-Type", getMimeType(".json"));
                httpExchange.sendResponseHeaders(200, response.getBytes().length);

                OutputStream dataOut = httpExchange.getResponseBody();
                dataOut.write(response.getBytes());
                dataOut.flush();

                httpExchange.getRequestBody().close();
                dataOut.close();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }).start();
    }

    private static void setEventStream(HttpExchange httpsExchange) {
        String[] path = httpsExchange.getRequestURI().getPath().split("/");
        String eventClient = path[2];
        if (!eventClient.equals("upload") && !eventClient.equals("download")) {
            try {
                httpsExchange.sendResponseHeaders(200, 0);
                httpsExchange.getRequestBody().close();
                httpsExchange.getResponseBody().close();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (eventClient.equals("upload")) {
            if (uploadEventStream != null) {
                try {
                    uploadEventStream.sendEvent("close_connection", "null");
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
                uploadEventStream = null;
            }
            uploadEventStream = new EventStream(httpsExchange);
            System.out.println("Added new upload event stream");
            updateRequestArrayList.clear();
            if (downloadEventStream != null) {
                try {
                    downloadEventStream.sendEvent("update_list", "[{\"file_name\":\"null\",\"file_size\":\"null\",\"action\":\"delete_all\"}]");
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            }
        } else {
            if (downloadEventStream != null) {
                try {
                    downloadEventStream.sendEvent("close_connection", "null");
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
                downloadEventStream = null;
            }
            downloadEventStream = new EventStream(httpsExchange);
            System.out.println("Added new download event stream");
        }
    }

    private static void setPendingRequest(HttpExchange httpExchange) {
        String[] path = httpExchange.getRequestURI().getPath().split("/");
        System.out.println("Received Download Path >> " + httpExchange.getRequestURI().getPath());
        String requestedFile = path[2];
        String requestId = path[3];
        System.out.println("Received upload request of the file >> " + requestedFile + " of file Id >> " + requestId);
        pendingRequest = new PendingRequest(httpExchange);

        /*forward the file-upload-request to the uploader*/
        try {
            uploadEventStream.sendEvent("upload_file", "{\"filename\":\"" + requestedFile + "\",\"request_id\":\"" + requestId + "\"}");
        } catch (IOException | NullPointerException e) {
            System.err.println(e.getMessage());
            try {
                httpExchange.getRequestBody().close();
                httpExchange.getResponseBody().close();
            } catch (IOException ex) {
                System.err.println(e.getMessage());
            }
            pendingRequest = null;
        }
    }

    private static void completePendingRequest(HttpExchange httpExchange) {
        if (pendingRequest != null) {
            try {
                HttpExchange pendingHttpExchange = pendingRequest.getHttpExchange();
                try {
                    sendFile(httpExchange, pendingHttpExchange);
                } catch (IOException e) {
                    /*The download or the upload was cancelled*/
                    String requestedFile = pendingHttpExchange.getRequestURI().getPath().split("/")[2].trim();
                    System.err.println("There was an error with  transfer of the file " + requestedFile);
                }
                /*Close the Connections*/
                try {
                    httpExchange.getRequestBody().close();
                } catch (IOException e) {
                    System.err.println("Connection closed when there was an ongoing download >> " + e.getMessage());
                }
                httpExchange.getResponseBody().close();
                pendingHttpExchange.getRequestBody().close();
                try {
                    pendingHttpExchange.getResponseBody().close();
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
                pendingRequest = null;

                /*Send the request-next-file request*/
                Headers uploadRequestHeaders = httpExchange.getRequestHeaders();
                String uploadedFileName = uploadRequestHeaders.get("FileName").get(0);
                String requestId = uploadRequestHeaders.get("RequestId").get(0);
                String contentLength = uploadRequestHeaders.get("Content-Length").get(0);

                String message = "{\"comp_file_name\":\"" + uploadedFileName + "\",\"comp_request_id\":\"" + requestId + "\",\"comp_file_size\":\"" + contentLength + "\"}";
                try {
                    downloadEventStream.sendEvent("download_completed", message);
                } catch (IOException | NullPointerException e) {
                    System.err.println(e.getMessage());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void sendFile(HttpExchange upload, HttpExchange download) throws IOException {
        /*Send the response headers*/
        /*1) To the downloader*/
        Headers uploadRequestHeaders = upload.getRequestHeaders();
        String uploadedFileName = uploadRequestHeaders.get("FileName").get(0);
        String contentLength = uploadRequestHeaders.get("Content-Length").get(0);

        Headers downloadResponseHeaders = download.getResponseHeaders();
        downloadResponseHeaders.add("Content-Disposition", "attachment; filename=\"" + uploadedFileName + "\"");
        download.sendResponseHeaders(200, Integer.parseInt(contentLength));

        /*Send the response headers to the uploader*/
        String response = "{\"file_name\":\"" + uploadedFileName + "\",\"content-length\":\"" + contentLength + "\"}";
        Headers uploadResponseHeaders = upload.getResponseHeaders();
        uploadResponseHeaders.add("Content-Type", getMimeType(".json"));
        upload.sendResponseHeaders(200, response.getBytes().length);

        BufferedWriter responseWriter = new BufferedWriter(new OutputStreamWriter(upload.getResponseBody()));
        responseWriter.write(response);
        responseWriter.flush();

        /*Send the file to the downloader*/
        InputStream dataIn = upload.getRequestBody();
        OutputStream dataOut = download.getResponseBody();
        BufferedPipeStream bufferedPipeStream = new BufferedPipeStream(dataIn, dataOut, Integer.parseInt(contentLength));
        bufferedPipeStream.transferData();
        System.out.println("Successfully Transferred file >> " + uploadedFileName + " of length >> " + contentLength + " bytes");
    }


    public static void main(String[] args) throws IOException {
        HttpServer viaServer = HttpServer.create(new InetSocketAddress(PORT), 0);
        viaServer.createContext("/", httpExchange -> sendRequestedFile(httpExchange, getRequestedFileName(httpExchange, uploadEventStream, downloadEventStream)));
        viaServer.createContext("/upload_file_data", ViaServer::getUploadedFileData);
        viaServer.createContext("/download_file_data", ViaServer::sendUploadedFileData);
        viaServer.createContext("/events", ViaServer::setEventStream);
        viaServer.createContext("/request_file_upload", ViaServer::setPendingRequest);
        viaServer.createContext("/request_file_download", ViaServer::completePendingRequest);

//        System.out.println(URLDecoder.decode("Arrow%20function%20and%20this%20keyword%20in%20javascript%F0%9F%98%83", StandardCharsets.UTF_8.name()));

        viaServer.start();
        System.out.println("ViaServer : Started @ port" + viaServer.getAddress().getPort());
    }
}
