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
package org.midonet.api.neutron.loadbalancer;

import java.util.List;
import java.util.UUID;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.midonet.api.auth.AuthRole;
import org.midonet.api.rest_api.AbstractResource;
import org.midonet.api.rest_api.RestApiConfig;
import org.midonet.cluster.rest_api.NotFoundHttpException;
import org.midonet.cluster.rest_api.neutron.NeutronMediaType;
import org.midonet.cluster.rest_api.neutron.models.Member;
import org.midonet.cluster.services.rest_api.neutron.plugin.LoadBalancerApi;
import org.midonet.event.neutron.PoolMemberEvent;

import static org.midonet.cluster.rest_api.validation.MessageProperty.RESOURCE_NOT_FOUND;
import static org.midonet.cluster.rest_api.validation.MessageProperty.getMessage;

public class MemberResource extends AbstractResource {

    private final static Logger log = LoggerFactory.getLogger(
        MemberResource.class);
    private final static PoolMemberEvent POOL_MEMBER_EVENT =
        new PoolMemberEvent();

    private final LoadBalancerApi api;

    @Inject
    public MemberResource(RestApiConfig config, UriInfo uriInfo,
                          SecurityContext context, LoadBalancerApi api) {
        super(config, uriInfo, context, null, null);
        this.api = api;
    }

    @GET
    @Path("{id}")
    @Produces(NeutronMediaType.MEMBER_JSON_V1)
    @RolesAllowed(AuthRole.ADMIN)
    public Member get(@PathParam("id") UUID id) {
        log.info("MemberResource.get entered {}", id);

        Member member = api.getMember(id);
        if (member == null) {
            throw new NotFoundHttpException(getMessage(RESOURCE_NOT_FOUND));
        }

        log.info("MemberResource.get exiting {}", member);
        return member;
    }

    @GET
    @Produces(NeutronMediaType.MEMBERS_JSON_V1)
    @RolesAllowed(AuthRole.ADMIN)
    public List<Member> list() {
        log.info("MemberResource.list entered");
        return api.getMembers();
    }

    @POST
    @Consumes(NeutronMediaType.MEMBER_JSON_V1)
    @Produces(NeutronMediaType.MEMBER_JSON_V1)
    @RolesAllowed(AuthRole.ADMIN)
    public Response create(Member member) {
        log.info("PoolMemberResource.create entered {}", member);

        api.createMember(member);
        POOL_MEMBER_EVENT.create(member.id, member);
        log.info("PoolMemberResource.create exiting {}", member);
        return Response.created(
            LBUriBuilder.getMember(getBaseUri(), member.id))
                        .entity(member).build();
    }

    @DELETE
    @Path("{id}")
    @RolesAllowed(AuthRole.ADMIN)
    public void delete(@PathParam("id") UUID id) {
        log.info("PoolMemberResource.delete entered {}", id);
        api.deleteMember(id);
        POOL_MEMBER_EVENT.delete(id);
    }

    @PUT
    @Path("{id}")
    @Consumes(NeutronMediaType.MEMBER_JSON_V1)
    @Produces(NeutronMediaType.MEMBER_JSON_V1)
    @RolesAllowed(AuthRole.ADMIN)
    public Response update(@PathParam("id") UUID id, Member member) {
        log.info("PoolMemberResource.update entered {}", member);
        api.updateMember(id, member);
        POOL_MEMBER_EVENT.update(id, member);
        log.info("PoolMemberResource.update exiting {}", member);
        return Response.ok(
            LBUriBuilder.getMember(getBaseUri(), member.id))
            .entity(member).build();
    }
}
