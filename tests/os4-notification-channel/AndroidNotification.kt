package android.service.notification
class Notification(val channelId: String, val shortcutId: String?)
class StatusBarNotification(val packageName: String, val notification: Notification, val uid: Int)
