package daros.io.dynamic.compile.groovy.register.util;

import daros.io.dynamic.compile.groovy.register.ClassLoading.DynamicClassLoader;
import daros.io.dynamic.compile.groovy.register.LoadClassListenerEven.GroovyFileWatchListener;
import daros.io.dynamic.compile.groovy.register.Runner.DynamicLoadClassAsync;
import daros.io.dynamic.compile.groovy.register.Runner.DynamicRegisterInitRunner;
import daros.io.dynamic.compile.groovy.register.exception.RegisterException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author Tran Khai Cat Tien
 * @version 1.0.0-SNAPSHOT
 * @email tientkc@fpt.com.vn
 * @since 28-Oct-2017
 */
public class DynamicUtil {

    protected static final Log logger = LogFactory.getLog(DynamicUtil.class);

    private static String folderClass = "class";
    public static boolean isModify = false;

    private static DynamicClassLoader dynamicClassLoader;
    private static final File classDir;

    static {
        System.setProperty("file.encoding", "UTF-8");
        dynamicClassLoader = DynamicClassLoader.getInstance();
        classDir = new File(folderClass);
        if (!classDir.exists()) {
            classDir.mkdir();
        }
        try {
            FileUtils.cleanDirectory(classDir);
            dynamicClassLoader.addClassPath(classDir.toURI().toURL());
            //loadClass(folderClass);
        } catch (IOException e) {
        }

    }

    private String oldGroovyFile;

    public static synchronized void loadClass(String path) {
        File file = new File(path);
        try {
            /*We are using reflection here to circumvent encapsulation; addURL is not public*/
            URLClassLoader loader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            URL url = file.toURI().toURL();
            /*Disallow if already loaded*/
            for (URL it : java.util.Arrays.asList(loader.getURLs())) {
                if (it.equals(url)) {
                    return;
                }
            }

            java.lang.reflect.Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
            method.setAccessible(true); /*promote the method to public access*/
            method.invoke(loader, new Object[]{url});
        } catch (final NoSuchMethodException |
                IllegalAccessException |
                MalformedURLException |
                java.lang.reflect.InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static List<String> compileGroovyFile(String scriptLocation) {
        String oldGroovyFile = null;
        List<String> listClassName = new ArrayList<>();
        try {
            CompilerConfiguration configuration = new CompilerConfiguration();
            configuration.setTargetDirectory("class");
            configuration.setTolerance(2);
            CompilationUnit compilationUnit = new CompilationUnit(configuration);
            File file = new File(scriptLocation);
            //URL url = file.toURI().toURL();
            compilationUnit.addSource(file);
            compilationUnit.compile();
            logger.info("Compile file :" + scriptLocation);
            String className = null;
            for (ClassNode classNode : compilationUnit.getAST().getModules().get(0).getClasses()) {
                className = classNode.getName();

                GroovyFileWatchListener.fileMapClassName.put(className, scriptLocation);

                oldGroovyFile = GroovyFileWatchListener.fileMapClassName.get(className);
                if (oldGroovyFile != null && !oldGroovyFile.equals(scriptLocation)) {
                    throw new RegisterException("class: " + className + " is duplicate, required one file, please check again");
                }
                DynamicClassCache dynamicClassCache = new DynamicClassCache(className);
                if (isModify == false) {
                    GroovyFileWatchListener.classCacheMap.put(className, dynamicClassCache);
            }
                listClassName.add(className);
            }
            GroovyFileWatchListener.classNameMapFile.put(scriptLocation, listClassName);
            return listClassName;

        } catch (RegisterException e) {
            logger.error(e.getMessage());
            if (DynamicRegisterInitRunner.isStart == false) {
                System.exit(1);
            } else {
                logger.error("Please check " + scriptLocation + "has class duplicate with file " + oldGroovyFile);
                return compileGroovyFile(oldGroovyFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    public static void registerBean(List<CompletableFuture<List<String>>> compileList, DynamicLoadClassAsync dynamicLoadClassAsync, List<String> listClassModife) {
        if (compileList.size() == 0) {
            return;
        }
        List<CompletableFuture<?>> registerList = new ArrayList<CompletableFuture<?>>();
        for (CompletableFuture<List<String>> future : compileList) {
            try {
                for (String className : future.get()) {
                    if (listClassModife != null) {
                        listClassModife.add(className);
                    }
                    registerList.add(dynamicLoadClassAsync.register(className));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        CompletableFuture.allOf((registerList.toArray(new CompletableFuture<?>[registerList.size()]))).join();


    }

    public static void reloadClassPath(){
        try {
            dynamicClassLoader.removeClassPath(new File(folderClass).toURI().toURL());
            dynamicClassLoader.addClassPath(new File(folderClass).toURI().toURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public static DynamicClassLoader getDynamicClassLoader() {
        return dynamicClassLoader;
    }
}
