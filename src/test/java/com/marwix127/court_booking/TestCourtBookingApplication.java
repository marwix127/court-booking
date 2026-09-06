package com.marwix127.court_booking;

import org.springframework.boot.SpringApplication;

public class TestCourtBookingApplication {

	public static void main(String[] args) {
		SpringApplication.from(CourtBookingApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
