package com.sevtinge.provision.renderengine.utils;

import android.content.res.Resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ShaderTextReader {
    public static String readTextFileFromResource(int i) {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(PublicParam.getContext().getResources().openRawResource(i)));
            while (true) {
                String readLine = bufferedReader.readLine();
                if (readLine != null) {
                    sb.append(readLine);
                    sb.append('\n');
                } else {
                    return sb.toString();
                }
            }
        } catch (Resources.NotFoundException e) {
            throw new RuntimeException("Resource not found: " + i, e);
        } catch (IOException e2) {
            throw new RuntimeException("Could not open resource: " + i, e2);
        }
    }
}
