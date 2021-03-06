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

package org.midonet.cluster.rest_api.models;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.xml.bind.annotation.XmlTransient;

import com.google.protobuf.Message;

import org.codehaus.jackson.annotate.JsonIgnore;

import org.midonet.cluster.data.ZoomClass;
import org.midonet.cluster.data.ZoomField;
import org.midonet.cluster.models.Topology;
import org.midonet.cluster.rest_api.ResourceUris;
import org.midonet.cluster.util.IPAddressUtil;
import org.midonet.cluster.util.IPSubnetUtil;
import org.midonet.cluster.util.UUIDUtil;
import org.midonet.packets.IPSubnet;
import org.midonet.packets.IPv4;

@ZoomClass(clazz = Topology.Dhcp.class)
public class DhcpSubnet extends UriResource {

    @JsonIgnore
    @ZoomField(name = "id", converter = UUIDUtil.Converter.class)
    public UUID id;

    @JsonIgnore
    @ZoomField(name = "network_id", converter = UUIDUtil.Converter.class)
    public UUID bridgeId;

    @JsonIgnore
    @ZoomField(name = "subnet_address",
               converter = IPSubnetUtil.Converter.class)
    public IPSubnet<?> subnetAddress;

    @NotNull
    @Pattern(regexp = IPv4.regex, message = "is an invalid IP format")
    public String subnetPrefix;

    @Min(0)
    @Max(32)
    public int subnetLength;

    @Pattern(regexp = IPv4.regex, message = "is an invalid IP format")
    @ZoomField(name = "default_gateway",
               converter = IPAddressUtil.Converter.class)
    public String defaultGateway;

    @Pattern(regexp = IPv4.regex, message = "is an invalid IP format")
    @ZoomField(name = "server_address",
               converter = IPAddressUtil.Converter.class)
    public String serverAddr;

    @ZoomField(name = "dns_server_address",
               converter = IPAddressUtil.Converter.class)
    public List<String> dnsServerAddrs;

    @Min(0)
    @Max(65536)
    @ZoomField(name = "interface_mtu")
    public int interfaceMTU;

    @ZoomField(name = "opt121_routes")
    public List<DhcpOption121> opt121Routes;

    @JsonIgnore
    @ZoomField(name = "hosts")
    public List<DhcpHost> dhcpHosts;

    @ZoomField(name = "enabled")
    public Boolean enabled = true;

    @Override
    public URI getUri() {
        return absoluteUri(ResourceUris.BRIDGES, bridgeId,
                           ResourceUris.DHCP, subnetAddress.toZkString());
    }

    public URI getHosts() {
        return relativeUri(ResourceUris.HOSTS);
    }

    @JsonIgnore
    @Override
    public void afterFromProto(Message proto) {
        subnetPrefix = subnetAddress.getAddress().toString();
        subnetLength = subnetAddress.getPrefixLen();
        if (dnsServerAddrs != null && dnsServerAddrs.isEmpty()) {
            dnsServerAddrs = null;
        }
    }

    @JsonIgnore
    @Override
    public void beforeToProto() {
        subnetAddress = IPSubnet.fromString(subnetPrefix + "/" + subnetLength);
    }

    @JsonIgnore
    public void create(UUID bridgeId) {
        if (null == id) {
            id = UUID.randomUUID();
        }
        this.bridgeId = bridgeId;
    }

    @JsonIgnore
    public void update(DhcpSubnet from) {
        id = from.id;
        subnetAddress = from.subnetAddress;
        bridgeId = from.bridgeId;
        dhcpHosts = from.dhcpHosts;
    }
}
