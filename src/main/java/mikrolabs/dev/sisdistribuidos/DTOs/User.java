package mikrolabs.dev.sisdistribuidos.DTOs;

import com.google.gson.JsonObject;

public record User(
        String name,
        String username,
        String password,
        String role,
        String token
) {
    public User(String name, String username, String password, String token) {
        this(name, username, password, null, token);
    }

    public static User user(String name, String username, String token) {
        return new User(name, username, "", null, token);
    }

    public static User user(String name, String username, String role, String token) {
        return new User(name, username, "", role, token);
    }

    public static User user(String token) {
        return new User("", "", "", null, token);
    }

    public static Request loginRequest(String username, String password) {
        JsonObject data = new JsonObject();
        data.addProperty("username", username);
        data.addProperty("password", password);
        return new Request("login", data);
    }

    public static Request registerRequest(String name, String username, String password) {
        JsonObject data = new JsonObject();
        data.addProperty("name", name);
        data.addProperty("username", username);
        data.addProperty("password", password);
        return new Request("register", data);
    }

    public static Request registerWithRoleRequest(String name, String username, String password, String role, String token) {
        JsonObject data = new JsonObject();
        data.addProperty("name", name);
        data.addProperty("username", username);
        data.addProperty("password", password);
        data.addProperty("role", role);
        data.addProperty("token", token);
        return new Request("register", data);
    }

    public static Request logoutRequest(String token) {
        JsonObject data = new JsonObject();
        data.addProperty("token", token);
        return new Request("logout", data);
    }

    public static Request getUserRequest(String token, String username) {
        JsonObject data = new JsonObject();
        data.addProperty("token", token);
        data.addProperty("username", username);
        return new Request("getuser", data);
    }

    public static Request updateUserNameRequest(String token, String username, String name) {
        JsonObject data = new JsonObject();
        data.addProperty("token", token);
        data.addProperty("username", username);
        data.addProperty("name", name);
        return new Request("updateusername", data);
    }

    public static Request updateUserPasswordRequest(String token, String username, String oldPassword, String newPassword) {
        JsonObject data = new JsonObject();
        data.addProperty("token", token);
        data.addProperty("username", username);
        data.addProperty("oldPassword", oldPassword);
        data.addProperty("newPassword", newPassword);
        return new Request("updateuserpassword", data);
    }

    public static Request deleteUserRequest(String token, String username) {
        JsonObject data = new JsonObject();
        data.addProperty("token", token);
        data.addProperty("username", username);
        return new Request("deleteuser", data);
    }
}
