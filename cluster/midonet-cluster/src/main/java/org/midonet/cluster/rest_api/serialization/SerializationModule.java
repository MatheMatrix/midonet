/*
 * Copyright 2014 Midokura SARL
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
package org.midonet.cluster.rest_api.serialization;

import com.google.inject.AbstractModule;

import org.midonet.cluster.rest_api.jaxrs.WildCardJacksonJaxbJsonProvider;

/**
 * Bindings specific to serialization in api.
 */
public class SerializationModule extends AbstractModule {

    @Override
    protected void configure() {

        install(new org.midonet.midolman.cluster.serialization
                .SerializationModule());

        bind(ObjectMapperProvider.class).asEagerSingleton();
        bind(WildCardJacksonJaxbJsonProvider.class).asEagerSingleton();
        bind(JsonMappingExceptionMapper.class).asEagerSingleton();

    }
}
