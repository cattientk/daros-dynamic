package daros.io.dynamic.compile.groovy.register.util;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tran Khai Cat Tien
 * @version 1.0.0-SNAPSHOT
 * @email tientkc@fpt.com.vn
 * @since 23-Nov-2017
 */
@Getter
public class DynamicClassCache {

    private String className;

    private List<String> listClassNameRef;

    public DynamicClassCache(String className) {
        this.className = className;
        this.listClassNameRef = new ArrayList<>();
    }

    public void removeClassNameRef(String className) {
        this.listClassNameRef.remove(className);
    }

    public void addClassNameRef(String className) {
        if (!this.listClassNameRef.contains(className)) {
            this.listClassNameRef.add(className);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DynamicClassCache that = (DynamicClassCache) o;

        return className.equals(that.className);
    }

    @Override
    public int hashCode() {
        return className.hashCode();
    }
}
