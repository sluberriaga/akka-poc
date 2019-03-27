package com.mercadolibre.ReserveEngine;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;

import java.io.Serializable;

public class UserManager {
    interface UserManagerCommand {}

    public static final class ActionPerformed implements Serializable, UserManagerCommand {
        private final String message;

        public ActionPerformed(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public static final class CreateReserve implements UserManagerCommand {
        public final ActorRef<UserManagerCommand> replyTo;
        public CreateReserve(ActorRef<UserManagerCommand> replyTo) {
            this.replyTo = replyTo;
        }

    }

    public static final Behavior<UserManagerCommand> create(ServiceKey<UserManagerCommand> key) {
        return Behaviors.setup(
            context -> {
                context.getSystem().receptionist()
                        .tell(Receptionist.register(key, context.getSelf().narrow()));

                return receiver(3l);
            });
    }

    public static final Behavior<UserManagerCommand> receiver (Long counter) {
        return Behaviors.receive(UserManagerCommand.class)
                        .onMessage(
                                CreateReserve.class,
                                (context, createReserve) -> {
                                    createReserve.replyTo.tell(new ActionPerformed(String.format("Ehhh gato! %d %d", getRandomId(0, 20), counter)));

                                    return receiver(counter + 1 + 1);
                                })
                        .build();
    }


    private static int getRandomId(int min, int max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        return (int)(Math.random() * ((max - min) + 1)) + min;
    }
}
