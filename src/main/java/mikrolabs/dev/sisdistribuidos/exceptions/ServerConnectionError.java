package mikrolabs.dev.sisdistribuidos.exceptions;
public class ServerConnectionError extends RuntimeException {
    public ServerConnectionError() { super("O servidor não enviou uma resposta válida."); }
    public ServerConnectionError(String message, Throwable cause) { super(message, cause); }
}
