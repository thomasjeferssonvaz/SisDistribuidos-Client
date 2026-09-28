package mikrolabs.dev.sisdistribuidos.DTOs;

public record User(
        String name,
        String username,
        String password,
        String token
) {
    public static User user(String name, String username, String token){
        return new User(name, username, "", token);
    }
    public static User user(String token){
        return new User("","", "", token);
    }
}
