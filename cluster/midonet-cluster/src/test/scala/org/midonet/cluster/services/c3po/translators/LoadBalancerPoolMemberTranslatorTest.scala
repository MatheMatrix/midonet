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

package org.midonet.cluster.services.c3po.translators

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import org.midonet.cluster.models.ModelsUtil._
import org.midonet.cluster.models.Neutron.NeutronLoadBalancerPoolMember
import org.midonet.cluster.models.Topology.PoolMember
import org.midonet.cluster.services.c3po.{midonet, neutron}
import org.midonet.cluster.util.IPAddressUtil
import org.midonet.cluster.util.UUIDUtil

@RunWith(classOf[JUnitRunner])
class LoadBalancerPoolMemberTranslatorTest extends TranslatorTestBase {
    private var translator: LoadBalancerPoolMemberTranslator = _
    before {
        initMockStorage()
        translator = new LoadBalancerPoolMemberTranslator(storage)
    }

    private val memberId = UUIDUtil.toProtoFromProtoStr("msb: 1 lsb: 1")
    private val poolId = UUIDUtil.toProtoFromProtoStr("msb: 2 lsb: 1")
    private val pool2Id = UUIDUtil.toProtoFromProtoStr("msb: 2 lsb: 2")

    private val commonMemberFldsNoPoolNoAddress = s"""
        id { $memberId }
        admin_state_up: true
        protocol_port: 12345
        weight: 100
    """
    private val commonMemberFlds = commonMemberFldsNoPoolNoAddress + s"""
        pool_id { $poolId }
        address { ${IPAddressUtil.toProto("10.0.0.1")} }
    """

    private val nMember = nLoadBalancerPoolMemberFromTxt(
        commonMemberFlds + s"""
        status: "status"
        status_description: "status desc"
    """)

    private val nMemberNoPoolNoAddress = nLoadBalancerPoolMemberFromTxt(
        commonMemberFldsNoPoolNoAddress + s"""
        status: "status"
        status_description: "status desc"
    """)

    private val mMember = mPoolMemberFromTxt(
        commonMemberFlds + s"""
        status: ACTIVE
    """)

    private val mMemberNoPoolNoAddress = mPoolMemberFromTxt(
        commonMemberFldsNoPoolNoAddress + s"""
        status: ACTIVE
    """)

    private val mMemberInactive = mPoolMemberFromTxt(
        commonMemberFlds + s"""
        status: INACTIVE
    """)

    private val commonMemberFldsUpdated = s"""
        id { $memberId }
        pool_id { $pool2Id }
        address { ${IPAddressUtil.toProto("10.0.0.2")} }
        admin_state_up: false
        protocol_port: 22222
        weight: 50
    """
    private val nMemberUpdated = nLoadBalancerPoolMemberFromTxt(
        commonMemberFldsUpdated + s"""
        status: "status2"
        status_description: "status desc2"
    """)

    private val mMemberUpdated = mPoolMemberFromTxt(
        commonMemberFldsUpdated + s"""
        status: INACTIVE
    """)

    "Creation of a Neutron Member" should "create a MidoNet Member" in {
        val midoOps = translator.translate(neutron.Create(nMember))
        midoOps should contain only midonet.Create(mMember)
    }

    "Creation of a Neutron Member without Pool ID / address" should "create " +
    "a MidoNet Member without Pool ID / address" in {
        val midoOps = translator.translate(neutron.Create(
                nMemberNoPoolNoAddress))
        midoOps should contain only midonet.Create(
                mMemberNoPoolNoAddress)
    }

    "Update of a Neutron Member" should "update a MidoNet Member" in {
        bind(memberId, mMember)
        val midoOps = translator.translate(neutron.Update(nMember))
        midoOps should contain only midonet.Update(mMember)
    }

    "Update of a Neutron Member" should "not modify the Member status" in {
        bind(memberId, mMemberInactive)
        val midoOps = translator.translate(neutron.Update(nMemberUpdated))
        midoOps should contain only midonet.Update(mMemberUpdated)
    }

    "Deletion of a Neutron Member" should "delete the MidoNet Member" in {
        val midoOps = translator.translate(
                neutron.Delete(classOf[NeutronLoadBalancerPoolMember],
                               memberId))
        midoOps should contain only midonet.Delete(classOf[PoolMember],
                                                   memberId)
    }
}