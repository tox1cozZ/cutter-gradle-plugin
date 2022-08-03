package io.github.tox1cozz.cutter;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target({PACKAGE, TYPE, FIELD, METHOD, CONSTRUCTOR})
public @interface CutterTargetOnly {

    CutterTarget value();
}