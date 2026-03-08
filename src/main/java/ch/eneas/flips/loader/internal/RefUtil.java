package ch.eneas.flips.loader.internal;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.InvocationTargetException;
import java.security.PrivilegedActionException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class RefUtil {
    static Throwable unwrapCause(Throwable throwable) {
        var cause = throwable;
        if (throwable instanceof InvocationTargetException || throwable instanceof PrivilegedActionException) {
            cause = throwable.getCause();
            if (cause == null) {
                cause = throwable;
            }
        }
        return cause;
    }
}
