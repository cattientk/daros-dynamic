package daros.io.dynamic.compile.groovy.register.util;

import java.io.File;
import java.io.FilenameFilter;

/**
 * @author Tran Khai Cat Tien
 * @version 1.0.0-SNAPSHOT
 * @email tientkc@fpt.com.vn
 * @since 23-Nov-2017
 */
public class GroovyFilenameFilter implements FilenameFilter {
    @Override
    public boolean accept(File dir, String name) {

        File child = new File(dir,name);
        if(child.isDirectory()){
            return true;
        }
        if (name.endsWith(".groovy")) {
            return true;
        }
        return false;
    }
}
