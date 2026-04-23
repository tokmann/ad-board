package com.adboard.config;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class WebAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

  @Override
  protected Class<?>[] getRootConfigClasses() {
    return new Class[] {
        JpaConfig.class,
        LiquibaseConfig.class
    };
  }

  @Override
  protected Class<?>[] getServletConfigClasses() {
    return new Class[] {
        WebMvcConfig.class,
        OpenApiConfig.class
      };
  }

  @Override
  protected String[] getServletMappings() {
    return new String[] { "/" };
  }
}
