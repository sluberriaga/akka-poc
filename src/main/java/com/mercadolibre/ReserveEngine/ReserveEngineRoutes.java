package com.mercadolibre.ReserveEngine;

import akka.actor.typed.ActorRef;

import akka.actor.ActorSystem;
import akka.actor.typed.RecipientRef;
import akka.actor.typed.javadsl.AskPattern;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import scala.util.Success;

import static akka.http.javadsl.server.PathMatchers.integerSegment;
import static akka.http.javadsl.server.PathMatchers.segment;


public class ReserveEngineRoutes extends AllDirectives {
    final private LoggingAdapter log;
    final private ActorSystem system;
    final private Duration timeout =  Duration.ofMillis(1000);


    public ReserveEngineRoutes(ActorSystem system) {
        this.system = system;
        log = Logging.getLogger(system, this);
    }


    public Route routes() {
        return concat(
                path(
                        segment("request").slash(segment("reserve")).slash(integerSegment()),
                        userId -> postRequestReserve(userId)
                )
        );
    }

    @SuppressWarnings("unchecked")
    private Route postRequestReserve(Integer userId) {
        ServiceKey<UserManager.UserManagerCommand> key = ServiceKey.create(UserManager.UserManagerCommand.class, "userManager-" + userId.toString());

        return post(() -> {
            akka.actor.typed.ActorSystem typedActorSystem = akka.actor.typed.ActorSystem.wrap(this.system);

            CompletionStage<Receptionist.Listing> future = AskPattern.ask(
                    typedActorSystem.receptionist(),
                    system -> Receptionist.find(key, system.narrow()),
                    timeout,
                    this.system.getScheduler()
            );

            return onComplete(future, (result) -> {
                Receptionist.Listing list = result.get();
                log.info(String.format("listing %s", list.toString()));

                if (list.isForKey(key)) {
                    Set<ActorRef<UserManager.UserManagerCommand>> userManagers = list.getServiceInstances(key);
                    RecipientRef<UserManager.UserManagerCommand> manager;

                    if (userManagers.size() > 0) {
                        manager = (ActorRef<UserManager.UserManagerCommand>)userManagers.toArray()[0];
                    } else {
                        manager = typedActorSystem.create(UserManager.create(key), String.format("user-%d", userId));
                    }

                    CompletionStage<UserManager.UserManagerCommand> command = AskPattern.ask(
                            manager,
                            system -> new UserManager.CreateReserve(system),
                            timeout,
                            this.system.getScheduler()
                    );

                    return onComplete(command, (response) -> complete(StatusCodes.OK, ((UserManager.ActionPerformed) response.get()).getMessage()));
                } else {
                    return complete(StatusCodes.BAD_REQUEST);
                }
            });

        });
    }
}
