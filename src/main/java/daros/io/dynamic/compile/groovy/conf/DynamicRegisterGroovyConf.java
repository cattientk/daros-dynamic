package daros.io.dynamic.compile.groovy.conf;

import daros.io.dynamic.compile.groovy.conf.properties.DynamicComponentProperties;
import daros.io.dynamic.compile.groovy.register.DynamicRegisterGroovyFile;
import daros.io.dynamic.compile.groovy.register.LoadClassListenerEven.GroovyFileWatchListener;
import daros.io.dynamic.compile.groovy.register.Runner.DynamicLoadClassAsync;
import daros.io.dynamic.compile.groovy.register.Runner.DynamicRegisterInitRunner;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.IOException;
import java.util.concurrent.Executor;

/**
 * @author Tran Khai Cat Tien
 * @version 1.0.0-SNAPSHOT
 * @email tientkc@fpt.com.vn
 * @since 28-Oct-2017
 */
@Configuration
@ConditionalOnProperty(value = "dynamic.register.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DynamicComponentProperties.class)
@EnableAsync
@Getter
@Setter
public class DynamicRegisterGroovyConf {

    protected static final Log logger = LogFactory.getLog(DynamicRegisterGroovyConf.class);

    @Autowired
    private DynamicComponentProperties dynamicComponentProperties;


    @Bean
    public DynamicRegisterGroovyFile dynamicRegisterGroovyFile(){
        DynamicRegisterGroovyFile dynamicRegisterGroovyFile = new DynamicRegisterGroovyFile();
        return dynamicRegisterGroovyFile;
    }

    @Bean
    public GroovyFileWatchListener groovyFileWatchListener(){
        try {
            GroovyFileWatchListener groovyFileWatchListener = new GroovyFileWatchListener(dynamicComponentProperties);
            return groovyFileWatchListener;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Bean
    public DynamicLoadClassAsync dynamicLoadClassAsync(){
        return new DynamicLoadClassAsync();
    }

    @Bean
    public DynamicRegisterInitRunner dynamicRegisterInitRunner(DynamicLoadClassAsync dynamicLoadClassAsync){
        return new DynamicRegisterInitRunner(dynamicLoadClassAsync);
    }

    @Bean(name = "register")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("RegisterInit-");
        executor.initialize();
        return executor;
    }

}
