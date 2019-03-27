package com.mercadolibre;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import com.mercadolibre.ReserveEngine.ReserveEngineRoutes;

public class Server extends AllDirectives {
    private final ReserveEngineRoutes reserveEngineRoutes;

    public Server(ActorSystem system) {
        reserveEngineRoutes = new ReserveEngineRoutes(system);
    }

    public static void main(String[] args) throws Exception {
        ActorSystem system = ActorSystem.create("httpServer");

        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);

        Server app = new Server(system);
        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.createRoute().flow(system, materializer);
        http.bindAndHandle(routeFlow, ConnectHttp.toHost("localhost", 8080), materializer);

        System.out.println("Server online at http://localhost:8080/");
    }

    protected Route createRoute() {
        return concat(
			reserveEngineRoutes.routes()
        );
    }
}


