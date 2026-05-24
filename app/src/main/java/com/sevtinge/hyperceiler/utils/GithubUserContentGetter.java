package com.sevtinge.hyperceiler.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.sevtinge.hyperceiler.common.log.AndroidLog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class GithubUserContentGetter {

    public static ArrayList<String> contributors = new ArrayList<>();

    public static Bitmap getUserAvatar(String userId, String s) throws IOException {
        String url = "https://avatars.githubusercontent.com/u/" + userId + "?s=" + s;
        InputStream input = new java.net.URL(url).openStream();
        return BitmapFactory.decodeStream(input);
    }

    public static String getUserName(String user) {
        HttpURLConnection conn = null;
        try {
            String apiUrl = "https://api.github.com/users/" + user;

            URL url = new URL(apiUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("GET");

            conn.setRequestProperty("User-Agent", "Android");

            int code = conn.getResponseCode();
            if (code != 200) {
                AndroidLog.w("GithubUserContentGetter", "Get user name failed with HTTP CODE " + code + ".");
                return null;
            }

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream())
            );

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            reader.close();

            JSONObject json = new JSONObject(sb.toString());

            String name = json.optString("name", null);
            String login = json.optString("login", user);

            if (name != null && !name.isEmpty()) {
                return name;
            } else {
                return login;
            }

        } catch (Exception e) {
            AndroidLog.w("GithubUserContentGetter", "Failed when get user name: " + e);
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public static ArrayList<String> getContributors(String owner, String repo) {
        if (!contributors.isEmpty()) return contributors;
        HttpURLConnection conn = null;

        try {
            String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/contributors";

            URL url = new URL(apiUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Android");

            int code = conn.getResponseCode();
            if (code != 200) {
                AndroidLog.w("GithubUserContentGetter", "Get contributors failed with HTTP CODE " + code + ".");
                return contributors;
            }

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream())
            );

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            JSONArray array = new JSONArray(sb.toString());

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String login = obj.optString("login", null);
                if (login != null && !login.isEmpty()) {

                    String name = getUserName(login);
                    if (name == null || name.isEmpty() || name.equals("null")) {
                        name = login;
                    }

                    contributors.add("@" + login + "," + name);
                }
            }

        } catch (Exception e) {
            AndroidLog.w("GithubUserContentGetter", "Failed when get contributors: " + e);
        } finally {
            if (conn != null) conn.disconnect();
        }

        return contributors;
    }
}
