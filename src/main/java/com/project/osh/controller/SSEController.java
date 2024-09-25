package com.project.osh.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.project.osh.service.TestService;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDateTime;

@RestController
public class SSEController {
    
    @Autowired
    private TestService testService;

    @GetMapping("/events")
    public Flux<ServerSentEvent<String>> getEvents() {
        return Flux.interval(Duration.ofSeconds(1))
                .map(sequence -> ServerSentEvent.<String>builder()
                        .id(String.valueOf(sequence))
                        .event("message")
                        .data("Event #" + sequence + " at " + LocalDateTime.now())
                        .build());
    }
    
   	@RequestMapping(value = "/st", method = RequestMethod.GET)
    public Flux<ServerSentEvent<String>> streamtest() {
        return Flux.interval(Duration.ofSeconds(10)).map(i -> ServerSentEvent.builder(testService.getWeatherJsonObject().toString()).build());
    }
}