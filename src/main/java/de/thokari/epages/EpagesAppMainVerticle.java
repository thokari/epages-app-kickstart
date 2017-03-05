package de.thokari.epages;

import io.vertx.core.AbstractVerticle;

public class EpagesAppMainVerticle extends AbstractVerticle {

    public void start() {
        
        System.out.println(config().toString());
        
        
    }
}
