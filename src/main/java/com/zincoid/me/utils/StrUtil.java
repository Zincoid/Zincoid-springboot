package com.zincoid.me.utils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StrUtil {

    public static List<String> extractAts(String content) {
        if (content == null) return List.of();
        Matcher m = Pattern.compile("@(\\w{2,50})").matcher(content);
        Set<String> names = new LinkedHashSet<>();
        while (m.find()) names.add(m.group(1));
        return List.copyOf(names);
    }
}
