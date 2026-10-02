package android.net; public final class Uri { private final String s; private Uri(String s){this.s=s;} public static Uri parse(String s){return new Uri(s);} public String toString(){return s;} }
