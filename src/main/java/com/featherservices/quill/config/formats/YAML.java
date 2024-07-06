package com.featherservices.quill.config.formats;

import com.featherservices.quill.config.formats.util.YAMLUtil;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class YAML implements Format {

    @Override
    public void writeFile(File file, Map<String, Object> map) {
        YAMLUtil.writeFile(file, map);
    }

    @Override
    public Map<String, Object> readFile(File file) {
        if (file.length() == 0)
            return new HashMap<>();

        return YAMLUtil.readFile(file);
    }

}
