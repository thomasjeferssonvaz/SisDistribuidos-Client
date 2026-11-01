package mikrolabs.dev.sisdistribuidos.utils;

public final class FieldValidation {
    private FieldValidation() {}

    public static boolean validName(String value) {
        return value != null && !value.isBlank()
                && value.codePointCount(0, value.length()) <= 60
                && value.matches("[\\p{L} ]+");
    }

    public static boolean validUsername(String value) {
        return value != null && value.matches("[a-z0-9._]{3,20}");
    }

    public static boolean validPassword(String value) {
        return value != null && value.matches("[A-Za-z0-9#.*&%$@!()_+=-]{8,20}")
                && value.matches(".*[A-Z].*") && value.matches(".*[a-z].*")
                && value.matches(".*[0-9].*") && value.matches(".*[#.*&%$@!()_+=-].*");
    }

    public static boolean validRole(String value) {
        return "admin".equals(value) || "user".equals(value);
    }

    public static boolean validSpotName(String value) {
        if (value == null) return false;
        String trimmed = value.trim();
        return !trimmed.isEmpty() && trimmed.codePointCount(0, trimmed.length()) <= 4;
    }

    public static boolean validFloor(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            Integer.parseInt(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static Integer parseFloor(String value) {
        if (!validFloor(value)) return null;
        return Integer.parseInt(value.trim());
    }

    public static boolean validVagaStatus(String value) {
        return "disponivel".equals(value)
                || "ocupada".equals(value)
                || "fora_de_servico".equals(value);
    }

    public static boolean validType(String value) {
        return "eletrico".equals(value) || "combustao".equals(value);
    }

    public static boolean validLicensePlate(String value) {
        if (value == null) return false;
        String trimmed = value.trim();
        return trimmed.length() == 7 && !trimmed.contains(" ");
    }

    public static boolean validBrand(String value) {
        if (value == null) return false;
        String trimmed = value.trim();
        return !trimmed.isEmpty() && trimmed.codePointCount(0, trimmed.length()) <= 20;
    }

    public static boolean validModel(String value) {
        if (value == null || value.isBlank()) return true;
        String trimmed = value.trim();
        return trimmed.codePointCount(0, trimmed.length()) <= 20;
    }

    public static boolean validIp(String value) {
        return value != null && !value.isBlank();
    }

    public static boolean validPort(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            int port = Integer.parseInt(value.trim());
            return port >= 1 && port <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
