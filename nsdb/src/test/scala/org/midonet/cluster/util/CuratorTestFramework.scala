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
package org.midonet.cluster.util

import java.util.concurrent.TimeUnit

import scala.collection.concurrent.TrieMap

import org.apache.curator.RetryPolicy
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.RetryNTimes
import org.apache.curator.test.TestingServer
import org.scalatest.{BeforeAndAfterEach, BeforeAndAfterAll, Suite}

/**
 * Provides boilerplate for:
 * 1. Starting up a Curator TestingServer.
 * 2. Connecting a Curator client to the TestingServer.
 * 3. Deleting data on the TestingServer before and after each test.
 *
 * Only one TestingServer is created for each test class.
 *
 * To add additional per-test setup/teardown behavior, override the setup()
 * and teardown() methods. There's no need to call super.setup() or
 * super.teardown().
 *
 * To add additional per-class setup/teardown behavior, override the
 * beforeAll() and afterAll() methods. For these, it is necessary to call the
 * super.beforeAll() and super.afterAll() methods. The inconsistency is due
 * to an inconsistency in the way scalatest's BeforeAndAfter and
 * BeforeAndAfterAll traits work.
 */
trait CuratorTestFramework extends BeforeAndAfterEach
                           with BeforeAndAfterAll { this: Suite =>
    import org.midonet.cluster.util.CuratorTestFramework.testServers

    protected val ZK_ROOT = "/test"
    protected var zk: TestingServer = _
    implicit protected var curator: CuratorFramework = _

    protected def retryPolicy: RetryPolicy = new RetryNTimes(2, 1000)
    protected def cnxnTimeoutMs: Int = 2 * 1000
    protected def sessionTimeoutMs: Int = 10 * 1000

    override protected def beforeAll(): Unit = {
        super.beforeAll()
        val ts = new TestingServer
        testServers(this.getClass) = ts
        ts.start()
    }

    protected def setup(): Unit = {}

    protected def teardown(): Unit = {}

    /**
     * Sets up a new CuratorFramework client against a in-memory test ZooKeeper
     * server. If one needs to test against a different ZooKeeper setup, he/she
     * should override this method.
     */
    protected def setUpCurator(): Unit = {
        zk = testServers(this.getClass)
        curator = CuratorFrameworkFactory.newClient(zk.getConnectString,
                                                    sessionTimeoutMs,
                                                    cnxnTimeoutMs,
                                                    retryPolicy)
    }

    override def beforeEach() {
        setUpCurator()
        curator.start()
        if (!curator.blockUntilConnected(1000, TimeUnit.SECONDS))
           fail("Curator did not connect to the test ZK server")

        clearZookeeper()
        curator.create().forPath(ZK_ROOT)

        setup()
    }

    override def afterEach() {
        clearZookeeper()
        curator.close()
        teardown()
    }

    protected def clearZookeeper(): Unit =
        try curator.delete().deletingChildrenIfNeeded().forPath(ZK_ROOT) catch {
            case _: InterruptedException =>
                fail("Curator did not connect to the test ZK server")
            case _: Throwable => // OK, doesn't exist
        }

    override protected def afterAll(): Unit = {
        super.afterAll()
        testServers.remove(this.getClass).foreach(_.close())
    }

    /** Adds a path under the root node, asynchronously. The contents of the
      * node will be s.getBytes() */
    def makePath(s: String): String = {
        val path = s"$ZK_ROOT/$s"
        curator.create().inBackground().forPath(path, s.getBytes)
        path
    }

    /** Adds n paths under, from ZK_ROOT/0 to ZK_ROOT/(n-1] */
    def makePaths(n: Int)
    : Map[String, String] = makePaths(0, n)

    /** Adds n paths under, from ZK_ROOT/start to ZK_ROOT/(end-1) */
    def makePaths(start: Int, end: Int): Map[String, String] = {
        var data = Map.empty[String, String]
        start.until(end) foreach { i =>
            val childPath = s"$ZK_ROOT/$i"
            val childData = i.toString
            data += (childPath -> childData)
            curator.create().inBackground()
                .forPath(childPath, childData.getBytes)
        }
        data
    }

}

object CuratorTestFramework {
    protected val testServers =
        new TrieMap[Class[_ <: CuratorTestFramework], TestingServer]
}
