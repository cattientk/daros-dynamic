package daros.io.dynamic.compile.groovy.register;

import daros.io.dynamic.compile.groovy.annotation.MapByClassName;
import daros.io.dynamic.compile.groovy.register.LoadClassListenerEven.GroovyFileWatchListener;
import daros.io.dynamic.compile.groovy.register.util.DynamicClassCache;
import daros.io.dynamic.compile.groovy.register.util.DynamicUtil;
import lombok.Setter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/**
 * @author Tran Khai Cat Tien
 * @version 1.0.0-SNAPSHOT
 * @email tientkc@fpt.com.vn
 * @since 27-Oct-2017
 */
@Setter
public class DynamicRegisterGroovyFile {
    private static final Log logger = LogFactory.getLog(DynamicRegisterGroovyFile.class);

    //RequestMappingHandlerMapping
    private static Method detectHandlerMethodsMethod =
            ReflectionUtils.findMethod(RequestMappingHandlerMapping.class, "detectHandlerMethods", Object.class);
    private static Method getMappingForMethodMethod =
            ReflectionUtils.findMethod(RequestMappingHandlerMapping.class, "getMappingForMethod", Method.class, Class.class);
    private static Method getMappingPathPatternsMethod =
            ReflectionUtils.findMethod(RequestMappingHandlerMapping.class, "getMappingPathPatterns", RequestMappingInfo.class);
    private static Method getPathMatcherMethod =
            ReflectionUtils.findMethod(RequestMappingHandlerMapping.class, "getPathMatcher");

    private static Field injectionMetadataCacheField =
            ReflectionUtils.findField(AutowiredAnnotationBeanPostProcessor.class, "injectionMetadataCache");

    static {
        detectHandlerMethodsMethod.setAccessible(true);
        getMappingForMethodMethod.setAccessible(true);
        getMappingPathPatternsMethod.setAccessible(true);
        getPathMatcherMethod.setAccessible(true);
        injectionMetadataCacheField.setAccessible(true);
    }

    private ApplicationContext ctx;
    private DefaultListableBeanFactory beanFactory;

    public DynamicRegisterGroovyFile() {
    }

    @Autowired
    public void setApplicationContext(ApplicationContext ctx) {
        if (!DefaultListableBeanFactory.class.isAssignableFrom(ctx.getAutowireCapableBeanFactory().getClass())) {
            throw new IllegalArgumentException("BeanFactory must be DefaultListableBeanFactory type");
        }
        this.ctx = ctx;
        this.beanFactory = (DefaultListableBeanFactory) ctx.getAutowireCapableBeanFactory();
    }


    public void registerBeanGroovyFile(String className) {

        Object object = null;
        try {
            object = DynamicUtil.getDynamicClassLoader().loadClass(className).newInstance();
            Field[] fields = object.getClass().getDeclaredFields();
            DynamicClassCache dynamicClassCache = GroovyFileWatchListener.classCacheMap.get(className);
            for (Field field : fields) {
                MapByClassName annotation = field.getAnnotation(MapByClassName.class);
                if (annotation == null) {
                    continue;
                }

                String classNameMapping = annotation.value();
                Assert.notNull(classNameMapping, "value is required; it must not be null");
                Object inject = null;

                try {
                    //check class not found
                    DynamicUtil.getDynamicClassLoader().loadClass(classNameMapping);

                    while (true) {
                        if (!ctx.containsBean(classNameMapping)) {
                            continue;
                        }
                        inject = ctx.getBean(classNameMapping);
                        if (inject != null) {
                            DynamicClassCache dynamicClassCacheRef = GroovyFileWatchListener.classCacheMap.get(classNameMapping);
                            dynamicClassCacheRef.addClassNameRef(className);
                            dynamicClassCache.addClassNameRef(classNameMapping);
                            field.setAccessible(true);
                            field.set(object, inject);

                            break;
                        }
                    }
                } catch (BeansException e) {
                    e.printStackTrace();
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        String beanName = object.getClass().getName();

        if (beanFactory.containsBean(beanName)) {
            beanFactory.destroySingleton(beanName);
            removeInjectCache(beanName);

        }
        beanFactory.registerSingleton(beanName, object);
        beanFactory.autowireBean(beanName);

        if (object.getClass().getAnnotation(RestController.class) != null) {
            addControllerMapping(beanName);
        }
    }

    public void removeBeanGroovyFile(String beanName) {
        removeOldControllerMapping(beanName);
        if (beanFactory.containsBean(beanName)) {
            beanFactory.destroyBean(beanName);
            removeInjectCache(beanName);
        }
    }

    private void removeOldControllerMapping(String controllerBeanName) {

        if (!beanFactory.containsBean(controllerBeanName)) {
            return;
        }
        RequestMappingHandlerMapping requestMappingHandlerMapping = requestMappingHandlerMapping();

        //remove old
        Class<?> handlerType = ctx.getType(controllerBeanName);
        final Class<?> userType = ClassUtils.getUserClass(handlerType);

        final RequestMappingHandlerMapping innerRequestMappingHandlerMapping = requestMappingHandlerMapping;
        Set<Method> methods = MethodIntrospector.selectMethods(userType, new ReflectionUtils.MethodFilter() {
            public boolean matches(Method method) {
                return ReflectionUtils.invokeMethod(
                        getMappingForMethodMethod,
                        innerRequestMappingHandlerMapping,
                        method, userType) != null;
            }
        });

        for (Method method : methods) {
            RequestMappingInfo mapping =
                    (RequestMappingInfo) ReflectionUtils.invokeMethod(getMappingForMethodMethod, requestMappingHandlerMapping, method, userType);
            if (mapping != null) {

                requestMappingHandlerMapping.unregisterMapping(mapping);
            }
        }
    }

    private void addControllerMapping(String controllerBeanName) {
        removeOldControllerMapping(controllerBeanName);
        RequestMappingHandlerMapping requestMappingHandlerMapping = requestMappingHandlerMapping();
        ReflectionUtils.invokeMethod(detectHandlerMethodsMethod, requestMappingHandlerMapping, controllerBeanName);
    }

    private RequestMappingHandlerMapping requestMappingHandlerMapping() {
        try {
            return ctx.getBean(RequestMappingHandlerMapping.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("applicationContext must has RequestMappingHandlerMapping");
        }
    }

    private void removeInjectCache(Object controller) {
        AutowiredAnnotationBeanPostProcessor autowiredAnnotationBeanPostProcessor =
                ctx.getBean(AutowiredAnnotationBeanPostProcessor.class);
        Map<String, InjectionMetadata> injectionMetadataMap =
                (Map<String, InjectionMetadata>) ReflectionUtils.getField(injectionMetadataCacheField, autowiredAnnotationBeanPostProcessor);
        injectionMetadataMap.remove(controller.getClass().getName());
    }
}
