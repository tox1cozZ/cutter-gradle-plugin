package ua.tox1cozz.cutter;

import java.util.function.*;

public final class Cutter {

    private Cutter() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Initializing field value only for a specific target build
     *
     * @param target The target build, on which the field value should remain
     * @param value  Field value
     */
    public static <T> T fieldValue(Enum<?> target, Supplier<T> value) {
        return value.get();
    }

    /**
     * Calling code only for a specific target build
     *
     * @param target The target build, on which the called code should remain
     * @param code   Code to execute
     */
    public static void execute(Enum<?> target, Runnable code) {
        code.run();
    }
}