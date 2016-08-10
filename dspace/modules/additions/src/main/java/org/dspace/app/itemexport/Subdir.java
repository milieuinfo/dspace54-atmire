package org.dspace.app.itemexport;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.Item;

import java.io.File;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 08 Jul 2016
 */
public class Subdir {

    public String getSubDir(Item item) {
        String handle = item.getHandle();
        return getSubDir(handle);
    }

    public String getSubDir(String handle) {
        String subdir;
        if (handle != null) {
            subdir = "";
            handle = StringUtils.substringAfter(handle, "/");
            int handleChar = 0;
            while (handleChar < handle.length()) {
                int nextHandleChar = handleChar + 2;
                if (handle.length() >= nextHandleChar) {
                    subdir += handle.substring(handleChar, nextHandleChar);
                } else {
                    subdir += handle.substring(handleChar);
                }
                if (handle.length() > nextHandleChar) {
                    subdir += File.separator;
                }
                handleChar = nextHandleChar;
            }
        } else {
            subdir = "00";
        }
        return subdir;
    }
}
