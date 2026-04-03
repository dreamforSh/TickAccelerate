package com.xinian.tickaccelerated.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import com.xinian.tickaccelerated.TickAccelerate;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Map;

/**
 * Server-side translation resolver.
 * <p>Loads lang JSON files from mod resources and resolves keys to literal text,
 * so that {@code Component.literal()} can be used instead of {@code Component.translatable()}.
 * This ensures proper text display on clients that do not have the mod installed.</p>
 */
public final class ServerI18n {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    private static final String LANG_PATH_TEMPLATE = "/assets/%s/lang/%s.json";
    private static final String FALLBACK_LOCALE = "en_us";

    private static volatile String currentLocale = FALLBACK_LOCALE;
    private static volatile Map<String, String> translations = new HashMap<>();
    private static volatile Map<String, String> fallback = new HashMap<>();

    private ServerI18n() {}

    /**
     * Initializes or reloads translations for the given locale.
     * Falls back to {@code en_us} if the requested locale file is not found.
     *
     * @param locale locale code (e.g. "en_us", "zh_cn")
     */
    public static void load(String locale) {
        currentLocale = locale != null ? locale.toLowerCase() : FALLBACK_LOCALE;
        fallback = loadLangFile(FALLBACK_LOCALE);
        if (!currentLocale.equals(FALLBACK_LOCALE)) {
            translations = loadLangFile(currentLocale);
        } else {
            translations = fallback;
        }
        LOGGER.info("Tick Accelerate server locale set to '{}' ({} keys loaded)", currentLocale, translations.size());
    }

    /**
     * Resolves a translation key with optional format arguments.
     * Returns the formatted string, or the raw key if not found.
     *
     * @param key  translation key (e.g. "tickaccelerate.cmd.title")
     * @param args format arguments for {@code String.format}
     * @return resolved text
     */
    public static String get(String key, Object... args) {
        String pattern = translations.getOrDefault(key, fallback.getOrDefault(key, key));
        if (args == null || args.length == 0) {
            return pattern;
        }
        try {
            return String.format(pattern, args);
        } catch (IllegalFormatException e) {
            return pattern;
        }
    }

    /**
     * Returns the currently active server locale.
     */
    public static String getLocale() {
        return currentLocale;
    }

    private static Map<String, String> loadLangFile(String locale) {
        String path = String.format(LANG_PATH_TEMPLATE, TickAccelerate.MODID, locale);
        try (InputStream is = ServerI18n.class.getResourceAsStream(path)) {
            if (is == null) {
                LOGGER.warn("Lang file not found: {}", path);
                return new HashMap<>();
            }
            Map<String, String> map = GSON.fromJson(
                    new InputStreamReader(is, StandardCharsets.UTF_8), MAP_TYPE);
            return map != null ? map : new HashMap<>();
        } catch (Exception e) {
            LOGGER.error("Failed to load lang file: {}", path, e);
            return new HashMap<>();
        }
    }
}

