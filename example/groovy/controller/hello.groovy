package dynamic.org;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import daros.io.dynamic.compile.groovy.annotation.MapByClassName;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.DisposableBean;

@RestController
class HelloController {

    @MapByClassName("dynamic.org.DynamicService")
    def dynamicService;


    @RequestMapping(value = "/home", method = RequestMethod.GET)
    String home() {
        
        return dynamicService.getMessage();
    }

}