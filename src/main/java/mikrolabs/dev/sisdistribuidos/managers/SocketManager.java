package mikrolabs.dev.sisdistribuidos.managers;


import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import java.math.BigDecimal;
import mikrolabs.dev.sisdistribuidos.DTOs.Request;
import mikrolabs.dev.sisdistribuidos.DTOs.Response;
import mikrolabs.dev.sisdistribuidos.exceptions.ServerConnectionError;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class SocketManager {
    private static final Gson gson = new Gson();
    public static Response sendRequest(Request request) throws ServerConnectionError {

        String ip = ConfigManager.getServerIp() !=null ? ConfigManager.getServerIp() : "127.0.0.1";
        int port = ConfigManager.getServerPort() != 0 ? ConfigManager.getServerPort() : 34345;


        try (Socket socket = new Socket()) {

            socket.connect(new InetSocketAddress(ip, port), 3000);
            socket.setSoTimeout(5000);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            System.out.println("Connected to server at " + ip + ":" + port);

            String jsonToSend = gson.toJson(request);
            out.println(jsonToSend);
            System.out.println("Sent:     " + jsonToSend);
            if (out.checkError()) throw new IOException("Falha ao enviar a requisição.");


            String jsonReceived = in.readLine();
            if (jsonReceived == null || jsonReceived.isBlank()) {
                throw new ServerConnectionError("O servidor encerrou a conexão sem uma resposta válida.", null);
            }
            Response response = parseResponse(jsonReceived);

            System.out.println("Received: " + jsonReceived);
            System.out.println("Parsed:   [Status: " + response.statusCode() + ", Result: " + response.message() + ", Data: "+ response.data() + "\n");

            return response;
        } catch (SocketTimeoutException e) {
            throw new ServerConnectionError("Tempo limite de comunicação com o servidor excedido.", e);
        } catch (JsonParseException e) {
            throw new ServerConnectionError("O servidor enviou uma resposta JSON inválida.", e);
        } catch (IOException e) {
            throw new ServerConnectionError("Não foi possível comunicar com o servidor.", e);
        }
    }

    static Response parseResponse(String json) {
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonObject()) throw new JsonParseException("Resposta deve ser um objeto.");
        JsonObject object = root.getAsJsonObject();
        JsonElement status = object.get("statusCode");
        JsonElement message = object.get("message");
        if (status == null || !status.isJsonPrimitive() || !status.getAsJsonPrimitive().isNumber()
                || message == null || !message.isJsonPrimitive() || !message.getAsJsonPrimitive().isString()) {
            throw new JsonParseException("Resposta sem statusCode inteiro ou message string.");
        }
        int code;
        try {
            code = new BigDecimal(status.getAsString()).intValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            throw new JsonParseException("statusCode deve ser um inteiro.", e);
        }
        JsonElement data = object.get("data");
        return new Response(code, message.getAsString(), data == null || data.isJsonNull() ? null : data);
    }
}
