package com.featherservices.quill.config.formats;

import java.io.File;
import java.util.Map;

public interface Format {

    static FormatFactory factory() {
        return FormatFactory.getFactory();
    }

    /**
     * Write the map to the file
     *
     * @param file File
     * @param map  Key-value map to be written to the file
     * @see #readFile(File)
     */
    void writeFile(File file, Map<String, Object> map);

    /**
     * Read the file and return the key-value map
     *
     * @param file File
     * @return deserialized file in a form of key-value map
     * @see #writeFile(File, Map)
     */
    Map<String, Object> readFile(File file);

}
