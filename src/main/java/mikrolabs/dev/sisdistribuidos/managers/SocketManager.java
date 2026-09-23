package mikrolabs.dev.sisdistribuidos.managers;


import com.google.gson.Gson;
import mikrolabs.dev.sisdistribuidos.DTOs.Request;
import mikrolabs.dev.sisdistribuidos.DTOs.Response;
import mikrolabs.dev.sisdistribuidos.exceptions.ServerConnectionError;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class SocketManager {
    private static final Gson gson = new Gson();
    public static Response sendRequest(Request request) throws ServerConnectionError {

        String ip = ConfigManager.getServerIp() !=null ? ConfigManager.getServerIp() : "127.0.0.1";
        int port = ConfigManager.getServerPort() != 0 ? ConfigManager.getServerPort() : 34345;


        try (
                Socket socket = new Socket(ip, port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
        ) {
            System.out.println("Connected to server at " + ip + ":" + port);

            String jsonToSend = gson.toJson(request);
            out.println(jsonToSend);
            System.out.println("Sent:     " + jsonToSend);


            String jsonReceived = in.readLine();
            Response response;
            if (jsonReceived == null || jsonReceived.isEmpty() || jsonReceived.equals("null")) {
                return new ServerConnectionError().toResponse();
            } else {
                response = gson.fromJson(jsonReceived, Response.class);
            }


            System.out.println("Received: " + jsonReceived);
            System.out.println("Parsed:   [Status: " + response.statusCode() + ", Result: " + response.message() + ", Data: "+ response.data() + ", Error: " + response.error() + "]\n");

            return response;
        } catch (IOException e) {
            return  new ServerConnectionError().toResponse();
        }


    }

}
