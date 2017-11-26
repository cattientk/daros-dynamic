# Spring boot dynamic compile groovy file at runtime 
This is project auto compile groovy file to class and register Bean and Controller at runtime, project support by Spring boot frameworks

## Installation
  * Create new web project for Spring boot . [SPRING INITIALIZR](http://start.spring.io/)
  
  * Add repositories and dependency to file `pom.xml`:
  
        <repositories>
          <repository>
            <id>bintray-cattientk-daros-dynamic</id>
            <name>bintray</name>
            <url>https://dl.bintray.com/cattientk/daros-dynamic</url>
            <snapshots>
              <enabled>true</enabled>
            </snapshots>
          </repository>
        </repositories>
        
        <dependencies>
          <dependency>
            <groupId>daros.io</groupId>
            <artifactId>dynamic-compile-groovy</artifactId>
            <version>1.0</version>
          </dependency>
        </dependencies>
  * Setting folder groovy in file `application.yml` (example):
  
        dynamic:
          register:
            enabled: true
            folderGroovyRoot: D:/groovy/

## Example
  * Run project
  * Create groovy service file: `service.groovy`

        package dynamic.org;
        
        class DynamicService {
            
            public String getMessage() { 
                return "Hello Works"; 
            } 
        }

  * Crete groovy controller file: `controller.groovy`
  
        package dynamic.org;
        
        import org.springframework.web.bind.annotation.RestController;
        import org.springframework.web.bind.annotation.RequestMapping;
        import org.springframework.web.bind.annotation.RequestMethod;
        import daros.io.core.annotation.MapByClassName;
        import org.springframework.context.ApplicationContext;
        import org.springframework.beans.factory.DisposableBean;

        @RestController
        class HelloController {

            //class of groovy file
            @MapByClassName("dynamic.org.DynamicService")
            def dynamicService;

            @RequestMapping(value = "/home", method = RequestMethod.GET)
            String home() {
                return dynamicService.getMessage();
            }
        }
