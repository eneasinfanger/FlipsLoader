package ch.eneas.flips.loader.internal;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.PluginClassLoader;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

class JarFinder {
    private static final Field pluginClassLoaderJarField;

    private final JavaPlugin plugin;
    private final Logger log;

    JarFinder(JavaPlugin plugin) {
        this.plugin = plugin;
        this.log = LogUtil.getProcessLogger(plugin, getClass());
    }

    static {
        final Logger log = LogUtil.getLoaderLogger(JarFinder.class);

        Field field = null;
        try {
            //noinspection UnstableApiUsage
            field = PluginClassLoader.class.getDeclaredField("jar");

            if (field.getType() != JarFile.class) {
                log.warn("Got WRONG 'jar' field in PluginClassLoader: {}", field);
                field = null;
            } else {
                field.setAccessible(true);
            }
        } catch (NoSuchFieldException e) {
            log.warn("Failed to get 'jar' field in PluginClassLoader: {}", e.toString());
        } catch (InaccessibleObjectException e) {
            log.warn("Failed to access 'jar' field in PluginClassLoader: {}", e.toString());
        }
        pluginClassLoaderJarField = field;
    }

    JarInfo getPluginJar() throws IOException {
        JarFile pluginJar;
        if (pluginClassLoaderJarField != null && (pluginJar = getFromClassLoader()) != null) {
            return new JarInfo(pluginJar, false);
        }
        var jar = new JarInfo(new JarFile(findPluginJar()), true);
        log.warn("Had to obtain plugin '{}' classes via the file in server plugins folder", plugin);
        return jar;
    }

    private JarFile getFromClassLoader() {
        //noinspection UnstableApiUsage
        if (plugin.getClass().getClassLoader() instanceof PluginClassLoader plCl) {
            try {
                return (JarFile) pluginClassLoaderJarField.get(plCl);
            } catch (IllegalAccessException e) {
                log.warn("Failed to get jar from field from plugins classloader: {}", e.toString());
            }
        }
        return null;
    }

    private File findPluginJar() throws FileNotFoundException {
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

    record JarInfo(JarFile file, boolean shouldClose) {}
}
