package com.featherservices.quill.config.formats.util;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class YAMLUtil {

    private static Yaml createYaml() {
        final LoaderOptions loaderOptions = new LoaderOptions();
        final Representer representer = new Representer(new DumperOptions());

        representer.getPropertyUtils().setSkipMissingProperties(true);

        final DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        return new Yaml(new Constructor(loaderOptions), representer, options);
    }

    public static void writeFile(File file, Map<String, Object> map) {
        final Yaml yaml = createYaml();

        try (FileWriter writer = new FileWriter(file)) {
            yaml.dump(map, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Object> readFile(File file) {
        final Yaml yaml = createYaml();

        try (InputStream input = new FileInputStream(file)) {
            return yaml.load(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
