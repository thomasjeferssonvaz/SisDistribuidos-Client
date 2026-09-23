package mikrolabs.dev.sisdistribuidos.DTOs;

import com.google.gson.JsonElement;

public record Request(
        String method,
        JsonElement data
) {}
