package com.featherservices.quill.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SerializableLocation {
    private String world;
    private int x;
    private int y;
    private int z;
}
