package daros.io.dynamic.compile.groovy.register.LoadClassListenerEven;

import daros.io.dynamic.compile.groovy.conf.properties.DynamicComponentProperties;
import daros.io.dynamic.compile.groovy.register.Runner.DynamicLoadClassAsync;
import daros.io.dynamic.compile.groovy.register.util.DynamicClassCache;
import daros.io.dynamic.compile.groovy.register.util.DynamicUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * @author Tran Khai Cat Tien
 * @version 1.0.0-SNAPSHOT
 * @email tientkc@fpt.com.vn
 * @since 22-Nov-2017
 */
public class GroovyFileWatchListener {

    private final WatchService watcher;
    private final Map<WatchKey, Path> keys;

    private final DynamicComponentProperties dynamicComponentProperties;

    public static Map<String, Long> scriptLastModifiedMap = new ConcurrentHashMap<String, Long>();//in millis

    public static Map<String, List<String>> classNameMapFile = new HashMap<String, List<String>>();

    public static Map<String, String> fileMapClassName = new HashMap<String, String>();

    public static Map<String, DynamicClassCache> classCacheMap = new HashMap<String, DynamicClassCache>();

    private List<CompletableFuture<List<String>>> compileList = new ArrayList<CompletableFuture<List<String>>>();

    private List<String> listClassModifie = new ArrayList<>();


    /**
     * Creates a WatchService and registers the given directory
     */
    @Autowired
    public GroovyFileWatchListener(DynamicComponentProperties dynamicComponentProperties) throws IOException {
        this.dynamicComponentProperties = dynamicComponentProperties;
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<WatchKey, Path>();

        Assert.notNull(dynamicComponentProperties.getFolderGroovyRoot(), "folderFroovyRoot is required; it must not be null");
        System.out.println(dynamicComponentProperties.getFolderGroovyRoot());
        walkAndRegisterDirectories(Paths.get(dynamicComponentProperties.getFolderGroovyRoot()));

    }

    /**
     * Register the given directory with the WatchService; This function will be called by FileVisitor
     */
    private void registerDirectory(Path dir) {
        WatchKey key = null;
        try {
            key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            keys.put(key, dir);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Register the given directory, and all its sub-directories, with the WatchService.
     */
    private void walkAndRegisterDirectories(final Path dir) {
        // register directory and sub-directories
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    registerDirectory(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Process all events for keys queued to the watcher
     */
    public void processEvents(DynamicLoadClassAsync dynamicLoadClassAsync) {
        while (true) {
            compileList.clear();
            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                x.printStackTrace();
                return;
            }

            Path dir = keys.get(key);
            if (dir == null) {
                System.err.println("WatchKey not recognized!!");
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                @SuppressWarnings("rawtypes")
                WatchEvent.Kind kind = event.kind();

                // Context for directory entry event is the file name of entry
                @SuppressWarnings("unchecked")
                Path name = ((WatchEvent<Path>) event).context();
                Path child = dir.resolve(name);

                // print out event
                System.out.format("%s: %s\n", event.kind().name(), child);

                // if directory is created, and watching recursively, then register it and its sub-directories
                String path = child.toString();
                if (kind == ENTRY_CREATE) {
                    if (Files.isDirectory(child)) {
                        walkAndRegisterDirectories(child);
                    } else {
                        compileList.add(dynamicLoadClassAsync.compileGroovy(path));
                    }
                } else if (kind == ENTRY_MODIFY) {
                    if (child.toFile().lastModified() != scriptLastModifiedMap.get(path)) {
                        scriptLastModifiedMap.put(path, child.toFile().lastModified());
                        compileList.add(dynamicLoadClassAsync.compileGroovy(path));
                    }
                } else if (kind == ENTRY_DELETE) {
                    if (Files.isDirectory(child)) {
                        continue;
                    }
                    List<String> listClassName = classNameMapFile.get(path);
                    CompletableFuture.allOf(dynamicLoadClassAsync.remove(listClassName)).join();
                    scriptLastModifiedMap.remove(path);
                    for (String className : listClassName) {
                        removeClassRef(className);
                    }
                }
            }
            DynamicUtil.isModify = true;
            CompletableFuture.allOf((compileList.toArray(new CompletableFuture<?>[compileList.size()]))).join();

            DynamicUtil.reloadClassPath();

            // reset key and remove from set if directory no longer accessible
            listClassModifie.clear();
            DynamicUtil.registerBean(compileList, dynamicLoadClassAsync, listClassModifie);

            if (listClassModifie.size() > 0) {
                List<String> classRefeshList = new ArrayList<>();
                for (String classNameModifie : listClassModifie) {
                    if (!classCacheMap.containsKey(classNameModifie)) {
                        continue;
                    }
                    classRefeshList.addAll(classCacheMap.get(classNameModifie).getListClassNameRef());
                }
                List<CompletableFuture<?>> registerList = new ArrayList<CompletableFuture<?>>();
                List<CompletableFuture<?>> removeBeanList = new ArrayList<CompletableFuture<?>>();
                for (String classRefesh : classRefeshList) {
                    removeBeanList.add(dynamicLoadClassAsync.remove(Arrays.asList(classRefesh)));
                    registerList.add(dynamicLoadClassAsync.register(classRefesh));
                }

                CompletableFuture.allOf(removeBeanList.toArray(new CompletableFuture<?>[registerList.size()])).join();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                CompletableFuture.allOf(registerList.toArray(new CompletableFuture<?>[removeBeanList.size()])).join();
            }


            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);

                // all directories are inaccessible
                if (keys.isEmpty()) {
                    break;
                }
            }
        }

    }

    private void removeClassRef(String className) {
        DynamicClassCache dynamicClassCache = classCacheMap.get(className);
        for (String classNameRef : dynamicClassCache.getListClassNameRef()) {
            classCacheMap.get(classNameRef).removeClassNameRef(className);
        }
        classCacheMap.remove(className);
    }
}
