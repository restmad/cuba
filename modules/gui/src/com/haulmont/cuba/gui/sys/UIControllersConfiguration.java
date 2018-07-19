package com.haulmont.cuba.gui.sys;

import com.haulmont.cuba.core.global.Scripting;
import com.haulmont.cuba.gui.Screen;
import com.haulmont.cuba.gui.screen.UIController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JavaDoc
 */
public class UIControllersConfiguration {
    private static final Logger log = LoggerFactory.getLogger(UIControllersConfiguration.class);

    @Inject
    protected Scripting scripting;

    protected List<String> packages;

    // todo add explicit exports

    public UIControllersConfiguration() {
    }

    public List<String> getPackages() {
        return packages;
    }

    public void setPackages(List<String> packages) {
        this.packages = packages;
    }

    public List<UIControllerDefinition> getUIControllers() {
        ClassPathScanningCandidateComponentProvider provider = createComponentScanner();

        log.trace("Scanning packages {}", packages);

        return packages.stream()
                .flatMap(scanPackage -> provider.findCandidateComponents(scanPackage).stream())
                .map(BeanDefinition::getBeanClassName)
                .map(className -> {
                    log.trace("Found screen controller {}", className);

                    @SuppressWarnings("unchecked")
                    Class<? extends Screen> screenClass = (Class<? extends Screen>) scripting.loadClassNN(className);

                    UIController uiController = screenClass.getAnnotation(UIController.class);
                    if (uiController == null) {
                        throw new RuntimeException("Screen class does not have @UIController : " + screenClass);
                    }

                    String id = UIControllerUtils.getInferredScreenId(uiController, screenClass);

                    return new UIControllerDefinition(id, className);
                })
                .collect(Collectors.toList());
    }

    protected ClassPathScanningCandidateComponentProvider createComponentScanner() {
        // Don't pull default filters (@Component, etc.):
        ClassPathScanningCandidateComponentProvider provider
                = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(UIController.class));
        return provider;
    }

    public final static class UIControllerDefinition {
        private final String id;
        private final String controllerClass;

        public UIControllerDefinition(String id, String controllerClass) {
            this.id = id;
            this.controllerClass = controllerClass;
        }

        public String getId() {
            return id;
        }

        public String getControllerClass() {
            return controllerClass;
        }
    }
}