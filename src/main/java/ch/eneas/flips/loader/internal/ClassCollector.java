package ch.eneas.flips.loader.internal;

import ch.eneas.flips.loader.internal.JarFinder.JarInfo;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

class ClassCollector {
    private ClassCollector() {}

    public static Set<Class<?>> collectPluginClasses(JavaPlugin plugin, InternalLoaderConfig config) throws IOException {
        JarInfo jar = new JarFinder(plugin).getPluginJar();
        try {
            return loadJarClasses(plugin, jar.file(), LogUtil.getProcessLogger(plugin, ClassCollector.class), config);
        } finally {
            if (jar.shouldClose()) {
                jar.file().close();
            }
        }
    }

    private static Set<Class<?>> loadJarClasses(JavaPlugin plugin, JarFile jarFile, Logger log, InternalLoaderConfig config) {
        var classes = new HashSet<Class<?>>();
        ClassLoader pluginClassLoader = plugin.getClass().getClassLoader();
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();
            if (entryName.endsWith(".class") && !entry.isDirectory()) {
                if (entryName.endsWith("module-info.class") || entryName.endsWith("package-info.class")) {
                    LogUtil.debugCL(config, log, "Skipping module-/package-info: {}", entryName);
                    continue;
                }

                String className = entryName.replace('/', '.').replace(".class", "");

                if (!config.packageFilter().test(className)) {
                    LogUtil.debugCL(config, log, "Skipping class from discovery: {}", className);
                    continue;
                }

                try {
                    Class<?> clazz = Class.forName(className, false, pluginClassLoader);
                    if (!clazz.isSynthetic() && !clazz.isAnonymousClass()) {
                        classes.add(clazz);
                        collectInnerClasses(clazz, classes);
                    } else {
                        LogUtil.debugCL(config, log, "Skipping synthetic/anonymous class: {}", clazz);
                    }
                } catch (ClassNotFoundException e) {
                    log.warn("Failed to load class '{}' ({}):", className, entryName, e);
                }
            }
        }
        return classes;
    }

    private static void collectInnerClasses(Class<?> clazz, Set<Class<?>> classes) {
        for (var innerClazz : clazz.getDeclaredClasses()) {
            classes.add(innerClazz);
            collectInnerClasses(innerClazz, classes);
        }
    }
}
