package android.content;
public class Context {
 public static final String DEVICE_POLICY_SERVICE="device_policy";
 public Object service;
 public boolean restricted;
 public Context(Object service) { this.service=service; }
 public Object getSystemService(String name) { return service; }
}
