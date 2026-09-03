package mikrolabs.dev.sisdistribuidos.exceptions;

public class ServerOff extends RuntimeException {
    public ServerOff(String message) {
        super("Server Offline");
    }
}
