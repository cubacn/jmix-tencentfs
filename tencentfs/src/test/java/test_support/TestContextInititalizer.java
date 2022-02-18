package test_support;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class TestContextInititalizer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ((DefaultListableBeanFactory) applicationContext.getBeanFactory()).setAllowBeanDefinitionOverriding(false);
    }
}
