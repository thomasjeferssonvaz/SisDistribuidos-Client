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
}
