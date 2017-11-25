package daros.io.dynamic.compile.groovy.conf.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Tran Khai Cat Tien
 * @version 1.0.0-SNAPSHOT
 * @email tientkc@fpt.com.vn
 * @since 03-Nov-2017
 */
@Configuration
@ConfigurationProperties(prefix = "dynamic.register")
@Getter
@Setter
public class DynamicComponentProperties {

    private String folderGroovyRoot;

}
