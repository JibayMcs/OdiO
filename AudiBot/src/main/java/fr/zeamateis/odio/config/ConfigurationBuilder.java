package fr.zeamateis.odio.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.util.ClasspathHelper;

public class ConfigurationBuilder
{
    private final File       configFile;
    private final Properties properties;

    public ConfigurationBuilder(File configFile) {
        this.configFile = configFile;
        this.properties = new Properties();
    }

    public void build() throws IOException {
        if (this.configFile == null)
            throw new IllegalStateException("File not initialized");

        Reflections reflections = new Reflections(new org.reflections.util.ConfigurationBuilder()
                .setUrls(ClasspathHelper.forClass(Configuration.class)).addScanners(new FieldAnnotationsScanner()));
        Set<Field> options = reflections.getFieldsAnnotatedWith(Option.class);

        if (this.configFile.exists())
            this.properties.load(new FileInputStream(this.configFile));

        options.forEach(o -> {
            Option option = o.getAnnotation(Option.class);
            try {
                Object value = this.configFile.exists() ? this.properties.getOrDefault(option.value(), o.get(null)) : o.get(null);
                o.setAccessible(true);
                o.set(null, value);
                if (value.getClass().isAssignableFrom(String.class))
                    this.properties.setProperty(option.value(), (String) value);
            } catch (IllegalAccessException e) {
                System.err.println("Could not load configuration, IllegalAccessException");
                System.exit(-1);
            }
        });

        this.properties.store(new FileOutputStream(this.configFile), null);
    }
}
