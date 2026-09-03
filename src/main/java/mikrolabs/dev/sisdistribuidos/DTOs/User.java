package mikrolabs.dev.sisdistribuidos.DTOs;

public record User(
        String username,
        String password,
        String token
) {
    public static User user(String username, String token){
        return new User(username, "", token);
    }
}
