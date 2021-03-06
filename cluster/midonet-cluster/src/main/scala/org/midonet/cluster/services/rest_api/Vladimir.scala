/*
 * Copyright 2015 Midokura SARL
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.midonet.cluster.services.rest_api

import java.util
import javax.validation.Validator
import javax.ws.rs.core.MediaType
import javax.ws.rs.ext.Provider
import javax.ws.rs.{Consumes, Produces}

import com.google.inject.servlet.{GuiceFilter, GuiceServletContextListener}
import com.google.inject.{Guice, Inject, Injector}
import com.sun.jersey.guice.JerseyServletModule
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer
import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider
import org.codehaus.jackson.map.DeserializationConfig.Feature._
import org.codehaus.jackson.map.ObjectMapper
import org.eclipse.jetty.server.{DispatcherType, Server}
import org.eclipse.jetty.servlet.{DefaultServlet, ServletContextHandler}
import org.slf4j.LoggerFactory

import org.midonet.cluster.auth.{AuthService, MockAuthService}
import org.midonet.cluster.rest_api.validation.ValidatorProvider
import org.midonet.cluster.services.rest_api.resources._
import org.midonet.cluster.services.rest_api.serialization.MidonetObjectMapper
import org.midonet.cluster.services.{ClusterService, MidonetBackend, Minion}
import org.midonet.cluster.storage.MidonetBackendConfig
import org.midonet.cluster.{ClusterConfig, ClusterNode}

object Vladimir {

    final val ContainerResponseFiltersClass =
        "com.sun.jersey.spi.container.ContainerResponseFilters"
    final val ContainerRequestFiltersClass =
        "com.sun.jersey.spi.container.ContainerRequestFilters"
    final val LoggingFilterClass =
        "com.sun.jersey.api.container.filter.LoggingFilter"
    final val POJOMappingFeatureClass =
        "com.sun.jersey.api.json.POJOMappingFeature"

    @Provider
    @Consumes(Array(MediaType.WILDCARD))
    @Produces(Array(MediaType.WILDCARD))
    class WildcardJacksonJaxbJsonProvider extends JacksonJaxbJsonProvider {

        val mapper = new MidonetObjectMapper()
        mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
        mapper.configure(USE_GETTERS_AS_SETTERS, false)
        configure(FAIL_ON_UNKNOWN_PROPERTIES, false)

        override def locateMapper(`type`: Class[_], mediaType: MediaType)
        : ObjectMapper = {
            mapper
        }
    }

    def servletModule(backend: MidonetBackend,
                      config: ClusterConfig) = new JerseyServletModule {
        override def configureServlets(): Unit = {
            bind(classOf[WildcardJacksonJaxbJsonProvider]).asEagerSingleton()
            bind(classOf[MidonetBackend]).toInstance(backend)
            bind(classOf[AuthService]).to(classOf[MockAuthService])
                .asEagerSingleton()
            bind(classOf[MidonetBackendConfig]).toInstance(config.backend)
            bind(classOf[ApplicationResource])
            bind(classOf[Validator]).toProvider(classOf[ValidatorProvider])
                                    .asEagerSingleton()
            val initParams = new java.util.HashMap[String, String]
            initParams.put(ContainerRequestFiltersClass,
                           LoggingFilterClass)
            initParams.put(ContainerResponseFiltersClass,
                           LoggingFilterClass)
            initParams.put(POJOMappingFeatureClass, "true")
            serve("/*").`with`(classOf[GuiceContainer], initParams)
        }
    }

}

@ClusterService(name = "rest_api")
class Vladimir @Inject()(nodeContext: ClusterNode.Context,
                         backend: MidonetBackend,
                         config: ClusterConfig)
    extends Minion(nodeContext) {

    import Vladimir._

    private var server: Server = _
    private val log = LoggerFactory.getLogger("org.midonet.rest-api")

    override def isEnabled = config.restApi.isEnabled

    override def doStart(): Unit = {
        log.info(s"Starting REST API service at port: " +
                 s"${config.restApi.httpPort} and " +
                 s"root uri = ${config.restApi.rootUri}")

        server = new Server(config.restApi.httpPort)

        val context = new ServletContextHandler(server, config.restApi.rootUri,
                                                ServletContextHandler.SESSIONS)
        context.addEventListener(new GuiceServletContextListener {
            override def getInjector: Injector = {
                Guice.createInjector(servletModule(backend, config))
            }
        })
        context.addFilter(classOf[GuiceFilter], "/*", util.EnumSet.allOf(classOf[DispatcherType]))
        context.addFilter(classOf[CorsFilter], "/*", util.EnumSet.allOf(classOf[DispatcherType]))
        context.addServlet(classOf[DefaultServlet], "/*")
        context.setClassLoader(Thread.currentThread().getContextClassLoader)

        try {
            server.setHandler(context)
            server.start()
        } finally {
            notifyStarted()
        }

    }

    override def doStop(): Unit = {
        try {
            if (server ne null) {
                server.stop()
                server.join()
            }
        } finally {
            server.destroy()
        }
        notifyStopped()
    }

}

