package com.github.tox1cozz.cutter;

import com.github.tox1cozz.cutter.function.ByteSupplier;
import com.github.tox1cozz.cutter.function.CharSupplier;
import com.github.tox1cozz.cutter.function.FloatSupplier;
import com.github.tox1cozz.cutter.function.ShortSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.*;

public final class Cutter {

    @Nullable
    private static Supplier<String> runtimeTarget;

    static {
        String targetProperty = System.getProperty("cutter.runtimeTarget");
        if (targetProperty != null) {
            setRuntimeTarget(() -> targetProperty);
        }
    }

    private Cutter() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Initializing boolean primitive field value only for a specific target build
     * Another build target value is false
     *
     * @param target The target build, on which the field value should remain
     * @param value  Boolean primitive field value
     */
    public static boolean booleanValue(@NotNull Enum<?> target, @NotNull BooleanSupplier value) {
        return booleanValue(target, () -> false, value);
    }

    /**
     * Initializing boolean primitive field value only for a specific target build
     *
     * @param target     The target build, on which the field value should remain
     * @param value      Boolean primitive field value
     * @param otherValue The value to use if the current target is not valid
     */
    public static boolean booleanValue(@NotNull Enum<?> target, @NotNull BooleanSupplier otherValue, @NotNull BooleanSupplier value) {
        return isRuntimeTarget(target) ? value.getAsBoolean() : otherValue.getAsBoolean();
    }

    /**
     * Initializing byte primitive field value only for a specific target build
     * Another build target value is 0
     *
     * @param target The target build, on which the field value should remain
     * @param value  Byte primitive field value
     */
    public static byte byteValue(@NotNull Enum<?> target, @NotNull ByteSupplier value) {
        return byteValue(target, () -> (byte)0, value);
    }

    /**
     * Initializing byte primitive field value only for a specific target build
     *
     * @param target     The target build, on which the field value should remain
     * @param value      Byte primitive field value
     * @param otherValue The value to use if the current target is not valid
     */
    public static byte byteValue(@NotNull Enum<?> target, @NotNull ByteSupplier otherValue, @NotNull ByteSupplier value) {
        return isRuntimeTarget(target) ? value.getAsByte() : otherValue.getAsByte();
    }

    /**
     * Initializing short primitive field value only for a specific target build
     * Another build target value is 0
     *
     * @param target The target build, on which the field value should remain
     * @param value  Short primitive field value
     */
    public static short shortValue(@NotNull Enum<?> target, @NotNull ShortSupplier value) {
        return shortValue(target, () -> (short)0, value);
    }

    /**
     * Initializing short primitive field value only for a specific target build
     *
     * @param target     The target build, on which the field value should remain
     * @param value      Short primitive field value
     * @param otherValue The value to use if the current target is not valid
     */
    public static short shortValue(@NotNull Enum<?> target, @NotNull ShortSupplier otherValue, @NotNull ShortSupplier value) {
        return isRuntimeTarget(target) ? value.getAsShort() : otherValue.getAsShort();
    }

    /**
     * Initializing character primitive field value only for a specific target build
     * Another build target value is '\u0000'
     *
     * @param target The target build, on which the field value should remain
     * @param value  Character primitive field value
     */
    public static char charValue(@NotNull Enum<?> target, @NotNull CharSupplier value) {
        return charValue(target, () -> '\u0000', value);
    }

    /**
     * Initializing character primitive field value only for a specific target build
     *
     * @param target     The target build, on which the field value should remain
     * @param value      Character primitive field value
     * @param otherValue The value to use if the current target is not valid
     */
    public static char charValue(@NotNull Enum<?> target, @NotNull CharSupplier otherValue, @NotNull CharSupplier value) {
        return isRuntimeTarget(target) ? value.getAsChar() : otherValue.getAsChar();
    }

    /**
     * Initializing integer primitive field value only for a specific target build
     * Another build target value is 0
     *
     * @param target The target build, on which the field value should remain
     * @param value  Integer primitive field value
     */
    public static int intValue(@NotNull Enum<?> target, @NotNull IntSupplier value) {
        return intValue(target, () -> 0, value);
    }

    /**
     * Initializing integer primitive field value only for a specific target build
     *
     * @param target     The target build, on which the field value should remain
     * @param value      Integer primitive field value
     * @param otherValue The value to use if the current target is not valid
     */
    public static int intValue(@NotNull Enum<?> target, @NotNull IntSupplier otherValue, @NotNull IntSupplier value) {
        return isRuntimeTarget(target) ? value.getAsInt() : otherValue.getAsInt();
    }

    /**
     * Initializing long primitive field value only for a specific target build
     * Another build target value is 0L
     *
     * @param target The target build, on which the field value should remain
     * @param value  Long primitive field value
     */
    public static long longValue(@NotNull Enum<?> target, @NotNull LongSupplier value) {
        return longValue(target, () -> 0L, value);
    }

    /**
     * Initializing long primitive field value only for a specific target build
     *
     * @param target     The target build, on which the field value should remain
     * @param value      Long primitive field value
     * @param otherValue The value to use if the current target is not valid
     */
    public static long longValue(@NotNull Enum<?> target, @NotNull LongSupplier otherValue, @NotNull LongSupplier value) {
        return isRuntimeTarget(target) ? value.getAsLong() : otherValue.getAsLong();
    }

    /**
     * Initializing float primitive field value only for a specific target build
     * Another build target value is 0.0F
     *
     * @param target The target build, on which the field value should remain
     * @param value  Float primitive field value
     */
    public static float floatValue(@NotNull Enum<?> target, @NotNull FloatSupplier value) {
        return floatValue(target, () -> 0.0F, value);
    }

    /**
     * Initializing float primitive field value only for a specific target build
     *
     * @param target     The target build, on which the field value should remain
     * @param value      Float primitive field value
     * @param otherValue The value to use if the current target is not valid
     */
    public static float floatValue(@NotNull Enum<?> target, @NotNull FloatSupplier otherValue, @NotNull FloatSupplier value) {
        return isRuntimeTarget(target) ? value.getAsFloat() : otherValue.getAsFloat();
    }

    /**
     * Initializing double primitive field value only for a specific target build
     * Another build target value is 0.0
     *
     * @param target The target build, on which the field value should remain
     * @param value  Double primitive field value
     */
    public static double doubleValue(@NotNull Enum<?> target, @NotNull DoubleSupplier value) {
        return doubleValue(target, () -> 0.0, value);
    }

    /**
     * Initializing double primitive field value only for a specific target build
     *
     * @param target     The target build, on which the field value should remain
     * @param value      Double primitive field value
     * @param otherValue The value to use if the current target is not valid
     */
    public static double doubleValue(@NotNull Enum<?> target, @NotNull DoubleSupplier otherValue, @NotNull DoubleSupplier value) {
        return isRuntimeTarget(target) ? value.getAsDouble() : otherValue.getAsDouble();
    }

    /**
     * Initializing field value only for a specific target build
     * Another build target value is null
     * <p>
     * If you are coding in Kotlin, you may need to add the '-Xno-call-assertions' flag to the compiler options,
     * otherwise there will be a NullPointerException exception at runtime, because compiler generate non-null assert
     *
     * @param target The target build, on which the field value should remain
     * @param value  Field value
     */
    public static <T> T referenceValue(@NotNull Enum<?> target, @NotNull Supplier<@Nullable T> value) {
        return referenceValue(target, () -> null, value);
    }

    /**
     * Initializing field value only for a specific target build
     *
     * @param target     The target build, on which the field value should remain
     * @param value      Field value
     * @param otherValue The value to use if the current target is not valid
     */
    public static <T> T referenceValue(@NotNull Enum<?> target, @NotNull Supplier<@Nullable T> otherValue, @NotNull Supplier<@Nullable T> value) {
        return isRuntimeTarget(target) ? value.get() : otherValue.get();
    }

    /**
     * Calling code only for a specific target build
     *
     * @param target The target build, on which the called code should remain
     * @param code   Code to execute
     */
    public static void execute(@NotNull Enum<?> target, @NotNull Runnable code) {
        if (isRuntimeTarget(target)) {
            code.run();
        }
    }

    /**
     * Allows the build target to be calculated dynamically during program execution
     * If the build target is persistent, use the JVM option -Dcutter.runtimeTarget
     * The call to this method will be cut when processing
     *
     * @param target Runtime target build
     */
    // TODO: Вырезать ВСЕГДА
    public static void setRuntimeTarget(@NotNull Supplier<String> target) {
        runtimeTarget = target;
        System.out.println("[Cutter] Setup runtime target: " + target.get());
    }

    private static boolean isRuntimeTarget(@NotNull Enum<?> target) {
        return runtimeTarget == null || target.name().equals(CutterTarget.DEBUG.name()) || runtimeTarget.get().equals(target.name());
    }
}