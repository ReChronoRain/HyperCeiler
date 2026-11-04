/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow;

import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbarfollow.RecentsOpenSession;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Self-contained JVM behavior tests; no Android runtime or third-party test framework. */
public final class BarFollowPureLogicTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        testFeatureState();
        testCallbackSession();
        testReuseSession();
        testModeSession();
        testAppearanceInputs();
        testShadeSelection();
        testFolmeCommands();
        testRecentsOpenSession();
        System.out.println("BarFollowPureLogicTest passed");
    }

    private static void testFeatureState() {
        AppBarFeatureState state = new AppBarFeatureState();
        check(!state.enabled() && state.generation() == 0, "feature starts disabled");
        state.setEnabled(true);
        long first = state.generation();
        check(state.enabled() && state.accepts(first), "enabled generation is current");
        check(!state.accepts(first - 1), "old generation is rejected");
        state.suspend();
        check(!state.enabled() && !state.accepts(first), "suspend invalidates generation");
        state.resumeAfterRejectedReload();
        check(state.enabled() && state.accepts(state.generation()), "resume starts a new generation");
        state.setEnabled(false);
        check(!state.enabled(), "disable wins over requested state");
    }

    private static void testCallbackSession() {
        Object owner = new Object();
        Object callback = new Object();
        RecentsBarCallbackSession session = new RecentsBarCallbackSession(owner);
        check(!session.accept(owner, callback, 1000, true, 1), "callback must bind at state 12");
        check(session.accept(owner, callback, 1000, true, 12), "state 12 binds callback");
        check(!session.accept(owner, new Object(), 1000, true, 1), "different callback is rejected");
        check(!session.accept(owner, callback, 1000, false, 1), "unauthenticated delivery is rejected");
        check(!session.accept(owner, callback, 1000, true, 6), "snapshot does not terminate lease");
        check(session.accept(owner, callback, 1000, true, 1), "state 1 accepted");
        check(!session.accept(owner, callback, 1000, true, 12), "ended lease rejects later delivery");
    }

    private static void testReuseSession() {
        Object owner = new Object(), controller = new Object(), transition = new Object();
        Object info = new Object(), callback = new Object(), original = new Object();
        Object endpoint = new Object(), task = new Object(), source = new Object();
        RecentsBarReuseSession session = new RecentsBarReuseSession(
            owner, controller, transition, info, callback, original, endpoint, task, 42);
        session.delivered(source, true, 1);
        check(!session.awaitingAppShow(), "wrong source cannot select Home target");
        session.delivered(callback, true, 3);
        check(!session.awaitingAppShow(), "bookkeeping state cannot select target");
        session.delivered(callback, true, 1);
        check(session.awaitingAppShow(), "authenticated state 1 selects Home target");

        check(!session.consume(controller, transition, info, callback, new Object(), false, true),
            "different task cannot consume");
        check(session.consume(controller, transition, info, callback, task, false, true),
            "exact live task consumes once");
        check(!session.consume(controller, transition, info, callback, task, false, true),
            "target is consumed only once");

        session.delivered(callback, true, 1);
        Object update = session.beginTargetUpdate();
        check(update != null && session.currentTarget() == null, "target update hides current target");
        check(!session.acceptTarget(update, new RecentsBarReuseSession.Target(new Object(), -1, new Object())),
            "invalid target receipt is rejected");
        check(session.currentTarget() == null, "failed target update is cancelled");
        update = session.beginTargetUpdate();
        check(update != null, "a new target update can start after rejection");
        Object proof = new Object(), nextTask = new Object();
        RecentsBarReuseSession.Target target = new RecentsBarReuseSession.Target(nextTask, 7, proof);
        check(session.acceptTarget(update, target), "valid target receipt is accepted");
        check(session.currentTarget() == target, "accepted receipt becomes current target");
        check(session.consumeTarget(controller, transition, info, callback, target, nextTask, false, true),
            "matching target consumes");
        session.close();
        check(session.currentTarget() == null && !session.awaitingAppShow(), "close clears ownership");
    }

    private static void testModeSession() {
        AtomicInteger applied = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();
        AppBarModeSession session = new AppBarModeSession((mode, awake) -> {
            check("mode".equals(mode) && awake, "mode packet reaches owner");
            applied.incrementAndGet();
        }, failure -> failures.incrementAndGet());
        session.mode("mode");
        check(applied.get() == 0, "mode waits for wakefulness");
        session.awake(true);
        check(applied.get() == 1, "complete packet applies once");
        Runnable guarded = session.guard(value -> { throw new IllegalStateException("expected"); }).getClass() != null
            ? () -> { } : null;
        check(guarded != null, "guard returns a consumer");
        session.guard(value -> { throw new IllegalStateException("expected"); }).accept("x");
        check(failures.get() == 1, "guard reports failure once");
        session.guard(value -> applied.incrementAndGet()).accept("ignored");
        check(applied.get() == 1, "closed session ignores callbacks");
    }

    private static void testAppearanceInputs() {
        class Attributes { }
        Class<?> type = Attributes.class;
        Object[] packet = {new Attributes(), true, true, false, true, false};
        Object corrected = StatusBarAppearanceInputs.withoutTransient(packet, type);
        check(corrected instanceof Object[] && !Boolean.TRUE.equals(((Object[]) corrected)[1]),
            "transient flag is corrected");
        check(((Object[]) corrected)[0] == packet[0], "shared packet identity is preserved");
        Object[] invalid = {new Object(), true, true, false, true, false};
        check(StatusBarAppearanceInputs.withoutTransient(invalid, type) == invalid, "wrong type stays unchanged");
    }

    private static void testShadeSelection() {
        XiaomiShadeBarSelection selection = new XiaomiShadeBarSelection();
        XiaomiShadeBarSelection.Root app = new XiaomiShadeBarSelection.Root(0, 1, 1, false, "App");
        check(selection.onAppTransition(0, app, List.of(app)), "first app selection changes state");
        check(selection.current(false, true) == XiaomiShadeBarSelection.APP, "app token selects APP");
        check(!selection.onAppTransition(0, app, List.of(app)), "same selection is not a change");
        XiaomiShadeBarSelection.Root home = new XiaomiShadeBarSelection.Root(0, 1, 2, false, "Home");
        check(selection.onAppTransition(0, home, List.of(home)), "home selection changes state");
        check(selection.current(false, true) == XiaomiShadeBarSelection.HOME, "home selects HOME");
        check(!selection.onAppTransition(1, app, List.of(app)), "nonzero display is ignored");
    }

    private static void testFolmeCommands() {
        BarFolmeCommands commands = new BarFolmeCommands();
        Object first = new Object(), second = new Object();
        commands.replace(first, 0, 2);
        commands.replace(second, 2, 2);
        check(commands.owns(first, 0) && commands.owns(second, 3), "commands own independent channels");
        check(commands.terminal(first, false) == 0, "partial completion is not terminal");
        check(commands.terminal(second, true) == 2, "cancelled final channel reports cancel");
        commands.replace(first, 0, 2);
        commands.replace(second, 2, 2);
        commands.terminal(first, false);
        check(commands.terminal(second, false) == 1, "uncancelled completion reports success");
    }

    private static void testRecentsOpenSession() {
        Object owner = new Object(), controller = new Object(), transition = new Object();
        Object info = new Object(), task = new Object(), token = new Object();
        RecentsOpenSession session = new RecentsOpenSession(
            owner, controller, transition, info, task, 9, token, "top", true);
        RecentsOpenSession.Attempt attempt = session.toApp();
        check(attempt != null && attempt == session.toApp(), "toApp keeps one attempt");
        Object handler = new Object(), finishToken = new Object();
        check(attempt.bindPendingFinish(handler, finishToken), "matching finish binds");
        check(!attempt.bindPendingFinish(new Object(), new Object()), "different finish is rejected");
        check(attempt.matchesPendingFinish(handler, finishToken), "bound finish matches");
        check(attempt.matches(owner, controller, transition, info, task, 9), "exact identity matches");
        attempt.completedToApp();
        check(attempt.isCompletedToApp() && !attempt.isActive(), "completion retires attempt");
        session.close();
        check(session.toApp() == null, "closed session cannot reopen");
    }
}
