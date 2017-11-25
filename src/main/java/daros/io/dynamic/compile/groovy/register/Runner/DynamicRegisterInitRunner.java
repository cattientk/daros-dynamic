package daros.io.dynamic.compile.groovy.register.Runner;

import daros.io.dynamic.compile.groovy.conf.properties.DynamicComponentProperties;
import daros.io.dynamic.compile.groovy.register.LoadClassListenerEven.GroovyFileWatchListener;
import daros.io.dynamic.compile.groovy.register.util.DynamicUtil;
import daros.io.dynamic.compile.groovy.register.util.GroovyFilenameFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Tran Khai Cat Tien
 * @version 1.0.0-SNAPSHOT
 * @email tientkc@fpt.com.vn
 * @since 20-Nov-2017
 */
public class DynamicRegisterInitRunner implements CommandLineRunner {

    private static final Log logger = LogFactory.getLog(DynamicRegisterInitRunner.class);

    public static boolean isStart = false;

    @Autowired
    private DynamicComponentProperties dynamicComponentProperties;

    @Autowired
    private GroovyFileWatchListener groovyFileWatchListener;

    private final DynamicLoadClassAsync dynamicLoadClassAsync;

    private GroovyFilenameFilter filenameFilter = new GroovyFilenameFilter();

    public DynamicRegisterInitRunner(DynamicLoadClassAsync dynamicLoadClassAsync) {
        this.dynamicLoadClassAsync = dynamicLoadClassAsync;
    }

    @Override
    public void run(String... args) throws Exception {
        new Thread(new Runnable() {
            @Override
            public void run() {
                logger.info("Start up init");
                loadInitFile(dynamicComponentProperties.getFolderGroovyRoot());
//                loadInitFile(dynamicComponentProperties.getControllerRoot());
                isStart = true;
                logger.info("Complete start up init");

                groovyFileWatchListener.processEvents(dynamicLoadClassAsync);
            }
        }).start();

    }

    private void loadInitFile(String folderRoot) {
        ArrayList<File> files = new ArrayList<>();

        File directoryRoot = new File(folderRoot);

        getAllFile(directoryRoot ,files);
        //File[] listFile = file.listFiles(filenameFilter);


        //CompletableFuture<List<String>>[] compileList = new CompletableFuture[files.size()];
        List<CompletableFuture<List<String>>> compileList = new ArrayList<CompletableFuture<List<String>>>();

        for (File scriptLocation : files) {
            String path = scriptLocation.getPath();

            GroovyFileWatchListener.scriptLastModifiedMap.put(path,scriptLocation.lastModified());
            CompletableFuture<List<String>> register = dynamicLoadClassAsync.compileGroovy(path);
            //compileList[i++] = register;
            compileList.add(register);
        }
        logger.info("Start compile groovy file");
        CompletableFuture.allOf(compileList.toArray(new CompletableFuture[compileList.size()])).join();
        logger.info("Complete compile groovy file");


        logger.info("Start register bean from groovy file");
        DynamicUtil.registerBean(compileList, dynamicLoadClassAsync,null);
        logger.info("Complete register bean from groovy file");

    }

    private void getAllFile(File directory, ArrayList<File> files) {
        //File directory = new File(directoryName);

        // get all the files from a directory
        File[] fList = directory.listFiles(filenameFilter);
        for (File file : fList) {
            if (file.isFile()) {
                files.add(file);
            } else if (file.isDirectory()) {
                getAllFile(file, files);
            }
        }
    }
}
