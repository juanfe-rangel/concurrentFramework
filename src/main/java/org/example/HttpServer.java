package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.example.Anotaciones.RequestParam;

public class HttpServer {

    private final int port;
    private final Map<String, Method> controllerMethods;
    private final Map<String, Object> controllerInstances;
    private final ConcurrentRequest concurrentRequest;
    private ServerSocket serverSocket;

    public HttpServer(int port, Map<String, Method> controllerMethods, Map<String, Object> controllerInstances, ConcurrentRequest concurrentRequest) {
        this.port = port;
        this.controllerMethods = controllerMethods;
        this.controllerInstances = controllerInstances;
        this.concurrentRequest = concurrentRequest;
    }

    public void start() throws IOException {
        this.serverSocket = new ServerSocket(port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Apagando sv");
            try {
                this.serverSocket.close();
                concurrentRequest.executor.shutdown();
                if (!concurrentRequest.executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    concurrentRequest.executor.shutdownNow();
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }));

        while (!concurrentRequest.isShuttingDown()) {
            Socket client = this.serverSocket.accept();
            concurrentRequest.executor.execute(() -> {
                try {
                    handleRequest(client);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        try {
            concurrentRequest.awaitTermination(10, TimeUnit.SECONDS);
            System.out.println("Servidor apagado elegantemente");
        } catch (InterruptedException e) {
            System.err.println("Interrupción durante el apagado elegante");
            concurrentRequest.executor.shutdownNow();
        }
    }

    private void handleRequest(Socket client) throws IOException {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(client.getInputStream()));
        OutputStream rawOut = client.getOutputStream();
        PrintWriter out = new PrintWriter(rawOut, true);

        String firstLine = in.readLine();
        if (firstLine == null || firstLine.isEmpty()) {
            client.close();
            return;
        }

        while (in.ready()) in.readLine();

        String[] tokens = firstLine.split(" ");
        String rawPath = tokens.length > 1 ? tokens[1] : "/";

        URI uri;
        try {
            uri = new URI(rawPath);
        } catch (URISyntaxException e) {
            sendError(out, 400, "Bad Request");
            client.close();
            return;
        }

        String path = uri.getPath();
        String queryString = uri.getQuery();

        // Manejo especial para /shutdown
        if ("/shutdown".equals(path)) {
            sendResponse(out, 200, "OK", "text/html", 
                "<html><body><h1>Servidor apagándose elegantemente...</h1></body></html>");
            out.flush();
            client.close();
            concurrentRequest.initiateGracefulShutdown();
            try {
                this.serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        Map<String, String> queryParams = new HashMap<>();
        if (queryString != null) {
            for (String pair : queryString.split("&")) {
                String[] kv = pair.split("=", 2);
                queryParams.put(kv[0], kv.length > 1 ? kv[1] : "");
            }
        }

        if (controllerMethods.containsKey(path)) {
            try {
                Method m = controllerMethods.get(path);
                Object instance = controllerInstances.get(path);

                Parameter[] params = m.getParameters();
                Object[] args = new Object[params.length];
                for (int i = 0; i < params.length; i++) {
                    if (params[i].isAnnotationPresent(RequestParam.class)) {
                        RequestParam rp = params[i].getAnnotation(RequestParam.class);
                        args[i] = queryParams.getOrDefault(rp.value(), rp.defaultValue());
                    }
                }

                Object result = m.invoke(instance, args);
                String body = "<html><body><h1>" + result + "</h1></body></html>";
                sendResponse(out, 200, "OK", "text/html", body);

            } catch (Exception e) {
                sendError(out, 500, "Error: " + e.getMessage());
            }
        } else {
            sendError(out, 404, "Not Found: " + path);
        }

        out.flush();
        client.close();
    }

    private void sendResponse(PrintWriter out, int code, String status,
                              String contentType, String body) {
        out.print("HTTP/1.1 " + code + " " + status + "\r\n");
        out.print("Content-Type: " + contentType + "\r\n");
        out.print("Content-Length: " + body.getBytes().length + "\r\n");
        out.print("\r\n");
        out.print(body);
    }

    private void sendError(PrintWriter out, int code, String msg) {
        String body = "<html><body><h2>" + code + " - " + msg + "</h2></body></html>";
        sendResponse(out, code, msg, "text/html", body);
    }
}