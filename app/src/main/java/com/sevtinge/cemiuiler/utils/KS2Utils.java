package com.sevtinge.cemiuiler.utils;

import android.os.Environment;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class KS2Utils {

    public static String encrypted(String str, String passwd) {
        StringBuilder sb = new StringBuilder();
        Long codekey;
        String hex = str2HexStr(passwd);
        codekey = Long.parseLong(hex, 16);
        long key = Long.parseLong(String.valueOf(codekey));
        char[] chars = str.toCharArray();
        for (char aChar : chars) {
            try {
                long asciiCode = aChar;
                asciiCode -= key;
                char result = (char) asciiCode;
                sb.append(result);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        String ss = null;
        try {
            ss = str2HexStr(encode(string2Unicode(String.valueOf(sb))));
            ss = ss.replace("E", "u");
            ss = ss.replace("5C756361", "E");
            ss = ss.replace("5C756362", "L");
            ss = ss.replace("5C75636", "J");
            ss = ss.replace("5C7563", "j");
            ss = ss.replace("5C756", "I");
            ss = ss.replace("5C75", "i");
            ss = ss.replace("5C", "v");
            ss = ss.replace("33", "t");
            ss = ss.replace("66", "T");
            ss = ss.replace("F", "V");
            ss = ss.replace("0", "O");
            ss = ss.replace("1", "o");
            ss = ss.replace("2", "0");
            ss = ss.replace("3", "f");
            ss = ss.replace("4", "F");
            ss = ss.replace("5", "l");
            ss = ss.replace("6", "1");
            ss = ss.replace("7", "p");
            ss = ss.replace("8", "d");
            ss = ss.replace("9", "q");
            ss = ss.replace("A", "b");
            ss = ss.replace("B", "X");
            ss = ss.replace("C", "x");
            ss = ss.replace("D", "U");
            ss = zip(ss);
            ss = ss.replace("\r", "-");
            ss = ss.replace("\n", "");
            ss = ss.replace("AAAA", "&");
            ss = ss.replace("/", "*");
            ss = "KS2>" + ss;
            ss = ss.substring(0, ss.length() - 1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ss;
    }

    public static String decrypted(String str, String passwd) {
        Long codekey;
        String hex = str2HexStr(passwd);
        codekey = Long.parseLong(hex, 16);
        long key = Long.parseLong(String.valueOf(codekey));
        String charsu;
        String st = str;
        try {
            st = st.replace("KS2>", "");
            st = st.replace("&", "AAAA");
            st = st.replace("-", "\n");
            st = st.replace("*", "/");
            st = unzip(st);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            st = st.replace("E", "5C756361");
            st = st.replace("L", "5C756362");
            st = st.replace("J", "5C75636");
            st = st.replace("j", "5C7563");
            st = st.replace("I", "5C756");
            st = st.replace("i", "5C75");
            st = st.replace("v", "5C");
            st = st.replace("t", "33");
            st = st.replace("T", "66");
            st = st.replace("0", "2");
            st = st.replace("1", "6");
            st = st.replace("O", "0");
            st = st.replace("o", "1");
            st = st.replace("f", "3");
            st = st.replace("F", "4");
            st = st.replace("l", "5");
            st = st.replace("p", "7");
            st = st.replace("d", "8");
            st = st.replace("q", "9");
            st = st.replace("b", "A");
            st = st.replace("X", "B");
            st = st.replace("x", "C");
            st = st.replace("U", "D");
            st = st.replace("u", "E");
            st = st.replace("V", "F");
            charsu = unicode2String(decode(hexStr2Str(st)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        StringBuilder sb = new StringBuilder();
        char[] chars = new char[0];
        if (charsu != null) {
            chars = charsu.toCharArray();
        }
        for (char aChar : chars) {
            try {
                long asciiCode = aChar;
                asciiCode += key;
                char result = (char) asciiCode;
                sb.append(result);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return String.valueOf(sb);
    }

    public static String str2HexStr(String str) {
        char[] chars = "0123456789ABCDEF".toCharArray();
        StringBuilder sb2 = new StringBuilder();
        byte[] bs = str.getBytes();
        int bit;
        for (byte b : bs) {
            bit = (b & 0x0f0) >> 4;
            sb2.append(chars[bit]);
            bit = b & 0x0f;
            sb2.append(chars[bit]);
        }
        return sb2.toString().trim();
    }

    public static String hexStr2Str(String hexStr) {
        String str = "0123456789ABCDEF";
        char[] hexs = hexStr.toCharArray();
        byte[] bytes = new byte[hexStr.length() / 2];
        int n;
        for (int i = 0; i < bytes.length; i++) {
            n = str.indexOf(hexs[2 * i]) * 16;
            n += str.indexOf(hexs[2 * i + 1]);
            bytes[i] = (byte) (n & 0xff);
        }
        return new String(bytes);
    }


    public static boolean isContainIllegal(String str) {
        Pattern p = Pattern.compile("[\u003A-\uFFFF]");
        Matcher m = p.matcher(str);
        return m.find();
    }

    public static String string2Unicode(String string) {
        StringBuilder unicode = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c < 0x20 || c > 0x7E) {
                String tmp = Integer.toHexString(c);
                if (tmp.length() >= 4) {
                    unicode.append("\\u").append(Integer.toHexString(c));
                } else if (tmp.length() == 3) {
                    unicode.append("\\u0").append(Integer.toHexString(c));
                } else if (tmp.length() == 2) {
                    unicode.append("\\u00").append(Integer.toHexString(c));
                } else if (tmp.length() == 1) {
                    unicode.append("\\u000").append(Integer.toHexString(c));
                } else if (tmp.length() == 3) {
                    unicode.append("\\u0000");
                }
            } else {
                unicode.append(c);
            }
        }
        return unicode.toString();
    }

    public static String unicode2String(String unicode) {
        StringBuilder sb = new StringBuilder();
        String[] hex = unicode.split("\\\\u");
        for (int i = 1; i < hex.length; i++) {
            int index = Integer.parseInt(hex[i], 16);
            sb.append((char) index);
        }
        return sb.toString();
    }

    final static Base64.Encoder encoder = Base64.getEncoder();
    final static Base64.Decoder decoder = Base64.getDecoder();

    public static String encode(String text) {
        byte[] textByte = new byte[0];
        textByte = text.getBytes(StandardCharsets.UTF_8);
        return encoder.encodeToString(textByte);
    }

    public static String decode(String encodedText) {
        String text = null;
        text = new String(decoder.decode(encodedText), StandardCharsets.UTF_8);
        return text;
    }

    public static String zip(String str) {
        if (str == null)
            return null;
        byte[] compressed;
        ByteArrayOutputStream out = null;
        ZipOutputStream zout = null;
        String compressedStr = null;
        try {
            out = new ByteArrayOutputStream();
            zout = new ZipOutputStream(out);
            zout.putNextEntry(new ZipEntry("0"));
            zout.write(str.getBytes());
            zout.closeEntry();
            compressed = out.toByteArray();
            compressedStr = org.apache.commons.codec.binary.Base64.encodeBase64String(compressed);
        } catch (IOException e) {
            compressed = null;
        } finally {
            if (zout != null) {
                try {
                    zout.close();
                } catch (IOException ignored) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {
                }
            }
        }
        return compressedStr;
    }

    public static String unzip(String compressedStr) {
        if (compressedStr == null) {
            return null;
        }
        ByteArrayOutputStream out = null;
        ByteArrayInputStream in = null;
        ZipInputStream zin = null;
        String decompressed = null;
        try {
            byte[] compressed = org.apache.commons.codec.binary.Base64.decodeBase64(compressedStr);
            out = new ByteArrayOutputStream();
            in = new ByteArrayInputStream(compressed);
            zin = new ZipInputStream(in);
            zin.getNextEntry();
            byte[] buffer = new byte[1024];
            int offset = -1;
            while ((offset = zin.read(buffer)) != -1) {
                out.write(buffer, 0, offset);
            }
            decompressed = out.toString();
        } catch (IOException ignored) {
        } finally {
            if (zin != null) {
                try {
                    zin.close();
                } catch (IOException ignored) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {
                }
            }
        }
        return decompressed;
    }

}
