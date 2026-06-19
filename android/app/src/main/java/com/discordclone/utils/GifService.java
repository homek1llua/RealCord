package com.discordclone.utils;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class GifService {
    private static final String TAG = "GifService";
    private static final String API_KEY = "LIVDSRZULELA";
    private static final String SEARCH_URL = "https://g.tenor.com/v1/search?q=%s&key=" + API_KEY + "&limit=30";
    private static final String TRENDING_URL = "https://g.tenor.com/v1/trending?key=" + API_KEY + "&limit=30";

    public static class GifResult {
        public String id;
        public String previewUrl;
        public String gifUrl;
        public int width;
        public int height;
    }

    public interface GifCallback {
        void onResult(List<GifResult> gifs);
        void onError(String error);
    }

    public static void search(final String query, final GifCallback callback) {
        new Thread(() -> {
            try {
                String urlStr;
                if (query == null || query.trim().isEmpty()) {
                    urlStr = TRENDING_URL;
                } else {
                    urlStr = String.format(SEARCH_URL, URLEncoder.encode(query.trim(), "UTF-8"));
                }

                Log.d(TAG, "Fetching: " + urlStr);
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("User-Agent", "DiscordClone/1.0");

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Response code: " + responseCode);

                if (responseCode != 200) {
                    BufferedReader errReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    StringBuilder errSb = new StringBuilder();
                    String errLine;
                    while ((errLine = errReader.readLine()) != null) errSb.append(errLine);
                    errReader.close();
                    Log.e(TAG, "Error response: " + errSb.toString());
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onError("API error " + responseCode));
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                Log.d(TAG, "Response: " + sb.substring(0, Math.min(200, sb.length())));
                JSONObject json = new JSONObject(sb.toString());
                JSONArray results = json.getJSONArray("results");
                List<GifResult> gifList = new ArrayList<>();

                for (int i = 0; i < results.length(); i++) {
                    JSONObject item = results.getJSONObject(i);
                    GifResult r = new GifResult();
                    r.id = item.getString("id");
                    JSONArray media = item.getJSONArray("media");
                    if (media.length() > 0) {
                        JSONObject gifObj = media.getJSONObject(0).getJSONObject("gif");
                        r.gifUrl = gifObj.getString("url");
                        JSONObject tinyGif = media.getJSONObject(0).getJSONObject("tinygif");
                        r.previewUrl = tinyGif.getString("url");
                        JSONArray dims = gifObj.getJSONArray("dims");
                        r.width = dims.getInt(0);
                        r.height = dims.getInt(1);
                        gifList.add(r);
                    }
                }

                final List<GifResult> finalResults = gifList;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onResult(finalResults));
            } catch (Exception e) {
                Log.e(TAG, "Search failed", e);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }
}
