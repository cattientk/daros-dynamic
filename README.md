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
