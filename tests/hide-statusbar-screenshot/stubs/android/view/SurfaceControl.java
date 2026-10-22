package android.view;

public class SurfaceControl {
    private boolean valid = true;
    private float alpha = 1.0f;

    public boolean isValid() {
        return valid;
    }

    /** 模拟窗口 Surface 被重建：旧引用失效，ViewRootImpl 会换一个新的。 */
    public void invalidateForTest() {
        valid = false;
    }

    /** 供测试断言窗口 alpha 是否真的被改过。 */
    public float getAlpha() {
        return alpha;
    }

    public static class Transaction {
        public Transaction setAlpha(SurfaceControl surface, float value) {
            surface.alpha = value;
            return this;
        }

        public void apply() {
        }

        /** 与真机一致：规则优先反射调用 apply(boolean) 走同步提交。 */
        private void apply(boolean synchronous) {
        }

        public void close() {
        }
    }
}
