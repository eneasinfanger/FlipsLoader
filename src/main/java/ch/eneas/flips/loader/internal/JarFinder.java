package ch.eneas.flips.loader.internal;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.PluginClassLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import static ch.eneas.flips.loader.internal.FlipsLoaderDelegate.getLogger;

class JarFinder {
    private static final Field pluginClassLoaderJarField;

    static {
        Field field = null;
        try {
            //noinspection UnstableApiUsage
            field = PluginClassLoader.class.getDeclaredField("jar");

            if (field.getType() != JarFile.class) {
                getLogger().warn("Got WRONG 'jar' field in PluginClassLoader: {}", field);
                field = null;
            } else {
                field.setAccessible(true);
            }
        } catch (NoSuchFieldException e) {
            getLogger().warn("Failed to get 'jar' field in PluginClassLoader: {}", e.toString());
        } catch (InaccessibleObjectException e) {
            getLogger().warn("Failed to access 'jar' field in PluginClassLoader: {}", e.toString());
        }
        pluginClassLoaderJarField = field;
    }

    record JarInfo(JarFile file, boolean shouldClose) {}

    static JarInfo getPluginJar(Plugin plugin) throws IOException {
        JarFile pluginJar;
        if (pluginClassLoaderJarField != null && (pluginJar = getFromClassLoader(plugin)) != null) {
            return new JarInfo(pluginJar, false);
        }
        var jar = new JarInfo(new JarFile(findPluginJar(plugin)), true);
        getLogger().warn("Had to obtain plugin '{}' classes via the file in server plugins folder", plugin);
        return jar;
    }

    private static JarFile getFromClassLoader(Plugin plugin) {
        //noinspection UnstableApiUsage
        if (plugin.getClass().getClassLoader() instanceof PluginClassLoader plCl) {
            try {
                return (JarFile) pluginClassLoaderJarField.get(plCl);
            } catch (IllegalAccessException e) {
                getLogger().warn("Failed to get jar from field from plugins classloader: {}", e.toString());
            }
        }
        return null;
    }

    private static File findPluginJar(Plugin plugin) throws FileNotFoundException {
        //noinspection UnstableApiUsage
        var pluginNamePattern = Pattern.compile("%s(-%s)?\\.jar".formatted(Pattern.quote(plugin.getPluginMeta().getName()), Pattern.quote(plugin.getPluginMeta().getVersion())));
        //noinspection DataFlowIssue
        for (File f : plugin.getDataFolder().getParentFile().listFiles()) {
            if (pluginNamePattern.matcher(f.getName()).matches()) {
                return f;
            }
        }
        throw new FileNotFoundException("Could not find plugin jar file");
    }
}
