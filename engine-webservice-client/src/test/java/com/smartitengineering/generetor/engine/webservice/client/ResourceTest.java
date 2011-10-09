/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smartitengineering.generetor.engine.webservice.client;

import com.google.inject.AbstractModule;
import com.smartitengineering.cms.api.common.MediaType;
import com.smartitengineering.generator.engine.webservice.domain.ReportConfig;
import com.smartitengineering.generetor.engine.webservice.client.api.Impl.RootResourceImpl;
import com.smartitengineering.generetor.engine.webservice.client.api.RootResource;
import com.smartitengineering.util.bean.guice.GuiceUtil;
import com.smartitengineering.util.rest.client.ApplicationWideClientFactoryImpl;
import com.smartitengineering.util.rest.client.ConnectionConfig;
import com.smartitengineering.util.rest.client.jersey.cache.CacheableClient;
import com.sun.jersey.api.client.Client;
import java.io.File;
import java.net.URI;
import java.util.Properties;
import javax.ws.rs.core.HttpHeaders;
import junit.framework.Assert;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author saumitra
 */
public class ResourceTest {

  private static Server jettyServer;
  private static final Logger LOGGER = LoggerFactory.getLogger(ResourceTest.class);
  private static final String CONTEXT_PATH = "/generator-engine";
  private static final String HOST = "localhost";
  private static final int PORT = 10080;
  private static final int SLEEP_DURATION = 3000;
  private static final HBaseTestingUtility TEST_UTIL = new HBaseTestingUtility();
  private static final URI ROOT_URI = URI.create("http://" + HOST + ":" + PORT + CONTEXT_PATH);

  @BeforeClass
  public static void setUp() throws Exception {
    System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
                       "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");

    TEST_UTIL.startMiniCluster();

    Properties properties = new Properties();
    properties.setProperty(GuiceUtil.CONTEXT_NAME_PROP,
                           "com.smartitengineering.dao.impl.hbase,com.smartitengineering.user.client");
    properties.setProperty(GuiceUtil.IGNORE_MISSING_DEP_PROP, Boolean.TRUE.toString());
    properties.setProperty(GuiceUtil.MODULES_LIST_PROP, ConfigurationModule.class.getName());
    GuiceUtil.getInstance(properties).register();
    /*
     * Start web application container
     */
    jettyServer = new Server(PORT);
    HandlerList handlerList = new HandlerList();
    /*
     * The following is for solr for later, when this is to be used it
     */
    System.setProperty("solr.solr.home", "./target/sample-conf/");
    Handler solr = new WebAppContext("./target/solr/", "/solr");
    handlerList.addHandler(solr);
    final String webapp = "../engine-webservice/src/main/webapp/";
    if (!new File(webapp).exists()) {
      throw new IllegalStateException("WebApp file/dir does not exist!");
    }
    WebAppContext webAppHandler = new WebAppContext(webapp, CONTEXT_PATH);
    handlerList.addHandler(webAppHandler);
    jettyServer.setHandler(handlerList);
    jettyServer.setSendDateHeader(true);
    jettyServer.start();

    /*
     * Setup client properties
     */
    System.setProperty(ApplicationWideClientFactoryImpl.TRACE, "true");

    Client client = CacheableClient.create();
    client.resource("http://localhost:9090/api/channels/test").header(HttpHeaders.CONTENT_TYPE,
                                                                      MediaType.APPLICATION_JSON).put(
        "{\"name\":\"test\"}");
    LOGGER.info("Created test channel!");
  }

  public static class ConfigurationModule extends AbstractModule {

    @Override
    protected void configure() {
      bind(Configuration.class).toInstance(TEST_UTIL.getConfiguration());
      ConnectionConfig config = new ConnectionConfig();
      config.setBasicUri("");
      config.setContextPath(CONTEXT_PATH);
      config.setHost(HOST);
      config.setPort(PORT);
      bind(ConnectionConfig.class).toInstance(config);
    }
  }

  @AfterClass
  public static void tearDown() throws Exception {
    TEST_UTIL.shutdownMiniCluster();
    jettyServer.stop();
  }

  @Test
  public void testRootResource() {
    RootResource rootResource = RootResourceImpl.getRoot(ROOT_URI);
    Assert.assertNotNull(rootResource);
    Assert.assertNotNull(rootResource.getConfigsResource());
  }

  @Test
  public void testReportConfig() {
    ReportConfig reportConfig1 = new ReportConfig();
    reportConfig1.setId("test1");
    reportConfig1.setName("TestConfig");
    sleep();
  }

  private static void sleep() {
    try {
      Thread.sleep(SLEEP_DURATION);
    }
    catch (Exception ex) {
      LOGGER.warn("Sleep broken!", ex);
    }
  }
}