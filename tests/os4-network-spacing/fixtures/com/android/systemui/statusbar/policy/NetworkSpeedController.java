package com.android.systemui.statusbar.policy;
import android.os.Handler;
import android.os.Message;
public class NetworkSpeedController {
    // ROM OS4 uses one Handler subclass for both instances after R8 merging.
    public final CombinedHandler mBgHandler = new CombinedHandler(this, true);
    public final CombinedHandler mHandler;
    public boolean mIsStatusBarHidden;
    public final SpeedState mNetworkSpeedState = new SpeedState();
    public NetworkSpeedController() { this(false); }
    public NetworkSpeedController(boolean separateClasses) {
        mHandler = separateClasses ? new SeparateUiHandler(this) : new CombinedHandler(this,false);
    }
    public static class SeparateUiHandler extends CombinedHandler {
        public SeparateUiHandler(NetworkSpeedController owner) { super(owner,false); }
    }
    public static class SpeedState { public boolean visible = true; }
    public static class CombinedHandler extends Handler {
        public final NetworkSpeedController this$0;
        private final boolean background;
        public CombinedHandler(NetworkSpeedController owner, boolean background) { this$0=owner; this.background=background; }
        public void handleMessage(Message message) {
            if (background && message.what==200001 && !this$0.mIsStatusBarHidden) {
                removeMessages(200001);
                sendEmptyMessageDelayed(200001,4000L);
            }
            if (!background && (message.what==100001 || message.what==100002 || message.what==100003 || message.what==100005)) {
                this$0.mBgHandler.removeMessages(200001);
                if(this$0.mNetworkSpeedState.visible && !this$0.mIsStatusBarHidden)
                    this$0.mBgHandler.sendEmptyMessageDelayed(200001,0L);
            }
        }
    }
}
