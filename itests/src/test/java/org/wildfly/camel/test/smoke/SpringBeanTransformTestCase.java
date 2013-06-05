/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.camel.test.smoke;

import java.io.InputStream;
import java.net.URL;
import java.util.Collection;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.osgi.metadata.ManifestBuilder;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.wildfly.camel.CamelContextFactory;
import org.wildfly.camel.SpringCamelContextFactory;
import org.wildfly.camel.test.smoke.subA.HelloBean;
import org.wildfly.camel.test.smoke.subA.SpringBeanTransformActivator;

/**
 * Deploys a module/bundle which contain a {@link HelloBean} referenced from a spring context definition.
 *
 * The tests then build a route through the {@link CamelContextFactory} API.
 * This verifies access to beans within the same deployemnt.
 *
 * @author thomas.diesler@jboss.com
 * @since 21-Apr-2013
 */
@RunWith(Arquillian.class)
public class SpringBeanTransformTestCase {

    static final String CAMEL_BUNDLE = "camel-spring-bundle.jar";

    @ArquillianResource
    Deployer deployer;

    @ArquillianResource
    BundleContext context;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-spring-tests");
        archive.addClasses(HelloBean.class);
        archive.addAsResource(SpringBeanTransformActivator.SPRING_CAMEL_CONTEXT_XML);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                builder.addManifestHeader("Dependencies", "org.wildfly.camel,org.apache.camel");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testSpringContextFromURL() throws Exception {
        URL resourceUrl = getClass().getResource("/" + SpringBeanTransformActivator.SPRING_CAMEL_CONTEXT_XML);
        CamelContext camelctx = SpringCamelContextFactory.createSpringCamelContext(resourceUrl, null);
        camelctx.start();
        ProducerTemplate producer = camelctx.createProducerTemplate();
        String result = producer.requestBody("direct:start", "Kermit", String.class);
        Assert.assertEquals("Hello Kermit", result);
    }

    @Test
    public void testSpringContextFromBundle() throws Exception {
        InputStream input = deployer.getDeployment(CAMEL_BUNDLE);
        Bundle bundle = context.installBundle(CAMEL_BUNDLE, input);
        try {
            bundle.start();
            String filter = "(name=spring-context)";
            BundleContext context = bundle.getBundleContext();
            Collection<ServiceReference<CamelContext>> srefs = context.getServiceReferences(CamelContext.class, filter);
            CamelContext camelctx = context.getService(srefs.iterator().next());
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String result = producer.requestBody("direct:start", "Kermit", String.class);
            Assert.assertEquals("Hello Kermit", result);
        } finally {
            bundle.uninstall();
        }
    }

    @Deployment(name = CAMEL_BUNDLE, managed = false, testable = false)
    public static JavaArchive getBundle() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, CAMEL_BUNDLE);
        archive.addClasses(SpringBeanTransformActivator.class, HelloBean.class);
        archive.addAsResource(SpringBeanTransformActivator.SPRING_CAMEL_CONTEXT_XML);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addBundleActivator(SpringBeanTransformActivator.class);
                builder.addImportPackages(CamelContext.class, CamelContextFactory.class);
                builder.addImportPackages(BundleActivator.class);
                return builder.openStream();
            }
        });
        return archive;
    }
}
