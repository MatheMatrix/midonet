/*
 * Copyright 2011 Midokura KK
 * Copyright 2012 Midokura PTE LTD.
 */
package org.midonet.api.network.rest_api;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.servlet.RequestScoped;
import org.midonet.api.VendorMediaType;
import org.midonet.api.auth.ForbiddenHttpException;
import org.midonet.api.network.Route;
import org.midonet.api.rest_api.AbstractResource;
import org.midonet.api.rest_api.NotFoundHttpException;
import org.midonet.api.ResourceUriBuilder;
import org.midonet.api.auth.AuthAction;
import org.midonet.api.auth.AuthRole;
import org.midonet.api.auth.Authorizer;
import org.midonet.api.network.auth.RouteAuthorizer;
import org.midonet.api.network.auth.RouterAuthorizer;
import org.midonet.api.rest_api.BadRequestHttpException;
import org.midonet.api.rest_api.RestApiConfig;
import org.midonet.midolman.state.InvalidStateOperationException;
import org.midonet.midolman.state.StateAccessException;
import org.midonet.cluster.DataClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Root resource class for ports.
 */
@RequestScoped
public class RouteResource extends AbstractResource {
    /*
     * Implements REST API endpoints for routes.
     */

    private final static Logger log = LoggerFactory
            .getLogger(RouteResource.class);

    private final Authorizer authorizer;
    private final DataClient dataClient;

    @Inject
    public RouteResource(RestApiConfig config, UriInfo uriInfo,
                         SecurityContext context, RouteAuthorizer authorizer,
                         DataClient dataClient) {
        super(config, uriInfo, context);
        this.authorizer = authorizer;
        this.dataClient = dataClient;
    }

    /**
     * Handler to deleting a route.
     *
     * @param id
     *            Route ID from the request.
     * @throws StateAccessException
     *             Data access error.
     */
    @DELETE
    @RolesAllowed({ AuthRole.ADMIN, AuthRole.TENANT_ADMIN })
    @Path("{id}")
    public void delete(@PathParam("id") UUID id)
            throws StateAccessException, InvalidStateOperationException {

        org.midonet.cluster.data.Route routeData =
                dataClient.routesGet(id);
        if (routeData == null) {
            return;
        }

        if (!authorizer.authorize(context, AuthAction.WRITE, id)) {
            throw new ForbiddenHttpException(
                    "Not authorized to delete this route.");
        }

        dataClient.routesDelete(id);
    }

    /**
     * Handler to getting a route.
     *
     * @param id
     *            Route ID from the request.
     * @throws StateAccessException
     *             Data access error.
     * @return A Route object.
     */
    @GET
    @PermitAll
    @Path("{id}")
    @Produces({ VendorMediaType.APPLICATION_ROUTE_JSON,
            MediaType.APPLICATION_JSON })
    public Route get(@PathParam("id") UUID id) throws StateAccessException {

        if (!authorizer.authorize(context, AuthAction.READ, id)) {
            throw new ForbiddenHttpException(
                    "Not authorized to view this route.");
        }

        org.midonet.cluster.data.Route routeData =
                dataClient.routesGet(id);
        if (routeData == null) {
            throw new NotFoundHttpException(
                    "The requested resource was not found.");
        }

        // Convert to the REST API DTO
        Route route = new Route(routeData);
        route.setBaseUri(getBaseUri());

        return route;
    }

    /**
     * Sub-resource class for router's route.
     */
    @RequestScoped
    public static class RouterRouteResource extends AbstractResource {

        private final UUID routerId;
        private final Authorizer authorizer;
        private final Validator validator;
        private final DataClient dataClient;

        @Inject
        public RouterRouteResource(RestApiConfig config,
                                   UriInfo uriInfo,
                                   SecurityContext context,
                                   RouterAuthorizer authorizer,
                                   Validator validator,
                                   DataClient dataClient,
                                   @Assisted UUID routerId) {
            super(config, uriInfo, context);
            this.routerId = routerId;
            this.authorizer = authorizer;
            this.validator = validator;
            this.dataClient = dataClient;
        }

        /**
         * Handler for creating a router route.
         *
         * @param route
         *            Route object.
         * @throws StateAccessException
         *             Data access error.
         * @returns Response object with 201 status code set if successful.
         */
        @POST
        @RolesAllowed({ AuthRole.ADMIN, AuthRole.TENANT_ADMIN })
        @Consumes({ VendorMediaType.APPLICATION_ROUTE_JSON,
                MediaType.APPLICATION_JSON })
        public Response create(Route route)
                throws StateAccessException, InvalidStateOperationException {

            route.setRouterId(routerId);

            Set<ConstraintViolation<Route>> violations = validator.validate(
                    route, Route.RouteGroupSequence.class);
            if (!violations.isEmpty()) {
                throw new BadRequestHttpException(violations);
            }

            if (!authorizer.authorize(context, AuthAction.WRITE, routerId)) {
                throw new ForbiddenHttpException(
                        "Not authorized to add route to this router.");
            }

            UUID id = dataClient.routesCreate(route.toData());
            return Response.created(
                    ResourceUriBuilder.getRoute(getBaseUri(), id))
                    .build();
        }

        /**
         * Handler to list routes.
         *
         * @throws StateAccessException
         *             Data access error.
         * @return A list of Route objects.
         */
        @GET
        @PermitAll
        @Produces({ VendorMediaType.APPLICATION_ROUTE_COLLECTION_JSON,
                MediaType.APPLICATION_JSON })
        public List<Route> list()
                throws StateAccessException {

            if (!authorizer.authorize(context, AuthAction.READ, routerId)) {
                throw new ForbiddenHttpException(
                        "Not authorized to view these routes.");
            }

            List<org.midonet.cluster.data.Route> routeDataList =
                    dataClient.routesFindByRouter(routerId);
            List<Route> routes = new ArrayList<Route>();
            if (routeDataList != null) {

                for (org.midonet.cluster.data.Route routeData :
                        routeDataList) {
                    Route route = new Route(routeData);
                    route.setBaseUri(getBaseUri());
                    routes.add(route);
                }

            }

            return routes;
        }
    }
}