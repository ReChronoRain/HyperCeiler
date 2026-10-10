package android.os; public class Handler {public static boolean rejected;public Handler(Looper l){}public boolean post(Runnable r){if(rejected)return false;r.run();return true;}}
