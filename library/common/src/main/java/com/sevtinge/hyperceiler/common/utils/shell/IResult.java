package com.sevtinge.hyperceiler.common.utils.shell;

public interface IResult {
    default void readOutput(String out, boolean finish) {
    }

    default void readError(String out) {
    }

    default void result(String command, int result) {
    }

    default void error(String reason) {
    }
}
