package fixture;
import io.github.libxposed.api.XposedInterface;
import io.github.lingqiqi5211.ezhooktool.xposed.common.AfterChainStage;
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookChain;
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import java.util.List;
import java.util.function.Consumer;
import kotlin.Unit;
/** Actual dependency's pre-fix forwarding; Java accesses its public JVM classes. */
public final class LegacyForwarding {
    public static XposedInterface.Hooker after(Consumer<HookParam> callback) {
        HookChain chain=new HookChain(List.of(new AfterChainStage(p->{callback.accept(p);return Unit.INSTANCE;})));
        return chain::invoke;
    }
}
