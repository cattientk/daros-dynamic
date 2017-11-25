package daros.io.dynamic.compile.groovy.register.Runner;

import daros.io.core.register.DynamicRegisterGroovyFile;
import daros.io.core.register.LoadClassListenerEven.GroovyFileWatchListener;
import daros.io.core.register.util.DynamicUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Tran Khai Cat Tien
 * @version 1.0.0-SNAPSHOT
 * @email tientkc@fpt.com.vn
 * @since 20-Nov-2017
 */

public class DynamicLoadClassAsync {
    private static final Log logger = LogFactory.getLog(DynamicLoadClassAsync.class);

    @Autowired
    private DynamicRegisterGroovyFile dynamicRegisterGroovyFile;

    @Async("register")
    public CompletableFuture<List<String>> compileGroovy(String path) {
        logger.info("Start compile groovy file: " + path);
        List<String> listClassName = DynamicUtil.compileGroovyFile(path);

        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("Complete compile groovy file: " + path);
        return CompletableFuture.completedFuture(listClassName);
    }

    @Async("register")
    public CompletableFuture<?> register(String className)  {
        logger.info("Start register bean for class name: " + className);
        dynamicRegisterGroovyFile.registerBeanGroovyFile(className);

        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("Complete register bean for class name: " + className);
        return CompletableFuture.completedFuture(null);
    }

    @Async("register")
    public CompletableFuture<?> remove(List<String> beanNames)  {
        for(String beanName: beanNames){
            logger.info("Remove bean: " + beanName);
            dynamicRegisterGroovyFile.removeBeanGroovyFile(beanName);
            GroovyFileWatchListener.fileMapClassName.remove(beanName);
        }


        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return CompletableFuture.completedFuture(null);
    }

}
