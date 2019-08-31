package com.amazmod.service.util;

import java.io.File;
import java.util.Comparator;

public class CaseInsensitiveFileComparator implements Comparator<Object> {
    public int compare(Object o1, Object o2) {
        File s1 = (File) o1;
        File s2 = (File) o2;
        return s1.getName().toLowerCase().compareTo(s2.getName().toLowerCase());
    }
}