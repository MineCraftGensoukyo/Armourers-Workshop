package moe.plushie.armourers_workshop.utils;

import com.google.common.base.Charsets;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import moe.plushie.armourers_workshop.init.ModLog;

import java.io.*;
import java.nio.charset.Charset;

public final class SerializeHelper {

    private SerializeHelper() {
    }

    public static String readFile(File file, Charset encoding) {
        InputStream inputStream = null;
        String text = null;
        try {
            inputStream = new FileInputStream(file);
            text = StreamUtils.toString(inputStream, encoding);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            StreamUtils.closeQuietly(inputStream);
        }
        return text;
    }

    public static String readFile(InputStream inputStream, Charset encoding) throws IOException {
        return StreamUtils.toString(inputStream, encoding);
    }

    public static JsonElement readJsonFile(File file) {
        return readJsonFile(file, Charsets.UTF_8);
    }

    public static JsonElement readJsonFile(File file, Charset encoding) {
        return stringToJson(readFile(file, encoding));
    }

    public static void writeFile(File file, Charset encoding, String text) {
        OutputStream outputStream = null;
        try {
            SkinFileUtils.forceMkdirParent(file);
            outputStream = new FileOutputStream(file, false);
            byte[] data = text.getBytes(encoding);
            outputStream.write(data);
            outputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            StreamUtils.closeQuietly(outputStream);
        }
    }

    public static void writeJsonFile(File file, Charset encoding, JsonElement json) {
        writeFile(file, encoding, json.toString());
    }

    public static void writeJsonFile(JsonElement json, File file) {
        writeFile(file, Charsets.UTF_8, json.toString());
    }

    public static JsonElement stringToJson(String jsonString) {
        try {
            JsonParser parser = new JsonParser();
            return parser.parse(jsonString);
        } catch (Exception e) {
            ModLog.error("Error parsing json.");
            ModLog.error(e.getMessage());
            return null;
        }
    }
}
