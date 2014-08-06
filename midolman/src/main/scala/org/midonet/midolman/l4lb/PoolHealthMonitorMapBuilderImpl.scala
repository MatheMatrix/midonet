/*
* Copyright (c) 2014 Midokura Pte.Ltd.
*/
package org.midonet.midolman.l4lb

import org.midonet.cluster.client.PoolHealthMonitorMapBuilder
import java.util.UUID
import org.slf4j.{LoggerFactory, Logger}
import akka.actor.ActorRef
import scala.collection.immutable.Map
import org.midonet.midolman.state.zkManagers.PoolHealthMonitorZkManager.PoolHealthMonitorConfig


class PoolHealthMonitorMapBuilderImpl(val poolHealthMonitorMapManager: ActorRef)
        extends PoolHealthMonitorMapBuilder {

    private final val log: Logger =
        LoggerFactory.getLogger(classOf[PoolHealthMonitorMapBuilderImpl])

    private var poolIdToMappingConfig
        = Map[UUID, PoolHealthMonitorConfig]()

    def build(): Unit = {
        poolHealthMonitorMapManager !
            PoolHealthMonitorMapManager.TriggerUpdate(poolIdToMappingConfig)
    }

    /**
    * Adds a map of pools and health monitors..
    *
    * @param mappings Map of pool and health monitor mappings
    */
    override def set(mappings: Map[UUID, PoolHealthMonitorConfig]) {
        log.debug("PoolHealthMonitorMapBuilderImpl.set")

        mappings match {
            case null =>
                this.poolIdToMappingConfig
                    = Map[UUID, PoolHealthMonitorConfig]()
            case _ =>
                this.poolIdToMappingConfig = mappings
        }
    }
}
