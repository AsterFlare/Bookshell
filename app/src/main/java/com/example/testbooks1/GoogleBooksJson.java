package com.example.testbooks1;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

public final class GoogleBooksJson {

    private static final String BOOKS_API_KEY = "AIzaSyAycxqRNFLfOCxktkf3cDcWChAc0Cfvk4Y";

    private GoogleBooksJson() {}

    public static String urlWithBooksApiKey(String urlWithoutKey) {
        return urlWithoutKey + "&key=" + BOOKS_API_KEY.trim();
    }

    public static String pickDisplayCategory(JSONObject volumeInfo) {
        if (volumeInfo == null) {
            return "Unknown";
        }
        JSONArray categories = volumeInfo.optJSONArray("categories");
        if (categories == null || categories.length() == 0) {
            return "Unknown";
        }
        for (int i = 0; i < categories.length(); i++) {
            String c = categories.optString(i, "");
            if (c.isEmpty()) {
                continue;
            }
            String low = c.toLowerCase(Locale.US);
            if (low.contains("subject headings")) {
                continue;
            }
            if (low.contains("cataloging and collection")) {
                continue;
            }
            if ("reference".equals(low)) {
                continue;
            }
            if (low.contains("library science") && !low.contains("fiction")) {
                continue;
            }
            return c;
        }
        for (int i = 0; i < categories.length(); i++) {
            String c = categories.optString(i, "");
            if (c.toLowerCase(Locale.US).contains("fiction")) {
                return c;
            }
        }
        return categories.optString(0, "Unknown");
    }
}
