package com.project.osh.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.project.osh.service.MessageService;
import reactor.core.publisher.Flux;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Date;

@RestController
// @CrossOrigin
// @RequestMapping("/message")
public class MessageController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageController.class);

    @Autowired
    private MessageService processor;

    @PostMapping("/send")
    public String send(@RequestBody String message) {
        LOGGER.debug("Received '{}'", message);
        processor.process(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " " + message);
        return "Done";
    }

    @GetMapping(path = "/receive", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> receive() {
        return Flux.create(sink -> {
            processor.register(sink::next);
        });
    }

    @GetMapping(path = "/timestamps", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> timestamps() {
        return Flux.interval(Duration.ofSeconds(1))
                .map(sequence -> LocalTime.now().toString());
    }
}