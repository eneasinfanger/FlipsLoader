package ch.eneas.flips.loader.internal;

import java.lang.reflect.InvocationTargetException;
import java.security.PrivilegedActionException;

class RefUtil {
    static Throwable unwrapCause(Throwable throwable) {
        var cause = throwable;
        if (throwable instanceof InvocationTargetException | throwable instanceof PrivilegedActionException) {
            cause = throwable.getCause();
        }
        return cause;
    }
}
