package ch.eneas.flips.loader.internal;

import ch.eneas.flips.loader.api.ClassProcessStage;
import ch.eneas.flips.loader.api.FlipsLoader;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;

import static ch.eneas.flips.loader.internal.RefUtil.unwrapCause;

// hack because FlipsLoader is loaded as different java.lang.Class instance by each plugin
@ApiStatus.Internal
public class FlipsLoaderDelegate implements FlipsLoader {
    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private static FlipsLoaderDelegate instance;

    public static FlipsLoaderDelegate getInstance() {
        if (STACK_WALKER.getCallerClass() != FlipsLoader.class) {
            throw new IllegalAccessError("FlipsLoaderDelegate is only supported with FlipsLoader");
        }
        return ensureInstance();
    }

    private static FlipsLoaderDelegate ensureInstance() {
        return (instance == null) ? (instance = new FlipsLoaderDelegate()) : instance;
    }

    static Logger getLogger() {
        return ensureInstance().logger;
    }

    @SuppressWarnings("removal")
    private static Method getImplMethod(Class<?> loaderImplClass, Method baseMethod) throws PrivilegedActionException {
        // no import because of deprecation warning
        return java.security.AccessController.doPrivileged((PrivilegedExceptionAction<Method>) () -> {
            var m = loaderImplClass.getDeclaredMethod(baseMethod.getName(), baseMethod.getParameterTypes());
            m.setAccessible(true);
            return m;
        });
    }

    private final Object loaderImpl;
    private final Method implRegisterPluginClasses;
    private final Logger logger;

    private FlipsLoaderDelegate() {
        loaderImpl = Arrays.stream(Bukkit.getPluginManager().getPlugins()) // JavaPlugin.getPlugin(Class) doesn't work for the same reasons below
            // 'instanceof' or x.getClass() == FlipsLoader.class doesn't work because of the different classloaders
            // cannot actually guarantee that the class is this class, but it has the same package and name
            .filter(plugin -> plugin.getClass().getName().equals(FlipsLoaderImpl.class.getName()))
            // PluginManager.getPlugin(String) could have been used but would have to be manually adjusted if the name was changed
            .findAny()
            .orElseThrow(() -> new IllegalStateException("no loader plugin instance found (is the loader plugin present & loaded successfully?)"));
        if (!ClassComparer.structurallyEquals(loaderImpl.getClass(), FlipsLoaderImpl.class)) {
            throw new IllegalStateException("Wrong loader implementation class or version! Are all plugins up to date? / are the correct dependency versions installed?");
        }
        try {
            // First the method of the base class is obtained to get IDE integration if method signature is changed.
            // (The IDE doesn't recognize the method name if it's used directly on getClass())
            implRegisterPluginClasses = getImplMethod(loaderImpl.getClass(),
                FlipsLoaderImpl.class.getMethod("registerPluginClasses", JavaPlugin.class, ClassProcessStage[].class)
            );
        } catch (PrivilegedActionException | ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to load LOADER class!", unwrapCause(ex));
        }
        logger = ((Plugin) loaderImpl).getSLF4JLogger();
    }

    @Override
    public void registerPluginClasses(JavaPlugin plugin, ClassProcessStage<?>[] customStages) {
        try {
            implRegisterPluginClasses.invoke(loaderImpl, plugin, customStages);
        } catch (IllegalAccessException | InvocationTargetException e) {
            var cause = unwrapCause(e);
            throw new IllegalStateException("Unexpected behaviour from FlipsLoader implementation: " + cause, cause);
        }
    }
}
