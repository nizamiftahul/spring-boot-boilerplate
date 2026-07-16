package com.nizamiftahul.spring_boilerplate.modules.sample.event;

import com.nizamiftahul.spring_boilerplate.modules.user.event.UserRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class UserRegisteredEventListener {

	private static final Logger log = LoggerFactory.getLogger(UserRegisteredEventListener.class);

	@EventListener
	public void onUserRegistered(UserRegisteredEvent event) {
		log.info("User registered: id={}, username={} — sample module reacting", event.userId(), event.username());
	}
}
