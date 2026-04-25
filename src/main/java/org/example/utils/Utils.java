package org.example.utils;

import java.io.InputStream;
import java.util.Properties;

public class Utils {
    private static final Properties props = new Properties();

    static {
        // грузим конфиг
        try (InputStream is = Utils.class.getResourceAsStream("/config.properties")) {
            props.load(is);
        } catch (Exception e) {
            throw new RuntimeException("ошибка загрузки конфига", e);
        }
    }

    public static String getProp(String key) {
        return props.getProperty(key);
    }
}