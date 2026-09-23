package mikrolabs.dev.sisdistribuidos.managers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigManager {

    private static final String CONFIG_FILE = "config.properties";
    private static final Properties properties = new Properties();

    static {
        load();
    }

    public static void load() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file)) {
                properties.load(in);
            } catch (IOException e) {
                System.err.println("Erro ao carregar configurações: " + e.getMessage());
            }
        }
    }

    private static synchronized void persist() {
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            properties.store(out, "Configuracoes do Cliente");
        } catch (IOException e) {
            System.err.println("Erro ao salvar arquivo de configuracoes: " + e.getMessage());
        }
    }

    public static synchronized void save(String ip, String port) {
        properties.setProperty("ServerIp", ip);
        properties.setProperty("ServerPort", port);
        persist();
    }

    public static synchronized void saveToken(String token) {
        if (token == null || token.isBlank()) {
            properties.remove("Token");
            System.out.println("Token removido com sucesso!");
        } else {
            properties.setProperty("Token", token);
            System.out.println("Token salvo com sucesso!");
        }
        persist();
    }

    public static synchronized String getToken() {
        return properties.getProperty("Token", "notloggedin");
    }

    public static synchronized void clearToken() {
        properties.remove("Token");
        System.out.println("Token removido com sucesso!");
        persist();
    }

    public static String getServerIp() {
        return properties.getProperty("ServerIp", "127.0.0.1");
    }

    public static synchronized int getServerPort() {
        String portValue = properties.getProperty("ServerPort", "34345");
        try {
            return Integer.parseInt(portValue.trim());
        } catch (NumberFormatException e) {
            System.err.println("Porta inválida no arquivo de config ('" + portValue + "'). Usando padrão: 34345");
            return 34345;
        }
    }
}
