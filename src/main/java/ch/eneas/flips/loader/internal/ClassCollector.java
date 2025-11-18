package ch.eneas.flips.loader.internal;

import ch.eneas.flips.loader.internal.JarFinder.JarInfo;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

class ClassCollector {
    private ClassCollector() {}

    public static Set<Class<?>> collectPluginClasses(JavaPlugin plugin) throws IOException, ClassNotFoundException {
        JarInfo jar = new JarFinder(plugin).getPluginJar();
        try {
            return loadJarClasses(plugin, jar.file());
        } finally {
            if (jar.shouldClose()) {
                jar.file().close();
            }
        }
    }

    private static Set<Class<?>> loadJarClasses(JavaPlugin plugin, JarFile jarFile) throws ClassNotFoundException {
        var classes = new HashSet<Class<?>>();
        ClassLoader pluginClassLoader = plugin.getClass().getClassLoader();
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();
            if (entryName.endsWith(".class") && !entry.isDirectory()) {
                String className = entryName.replace('/', '.').replace(".class", "");

                Class<?> clazz = Class.forName(className, false, pluginClassLoader);
                classes.add(clazz);

                collectInnerClasses(clazz, classes);
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
