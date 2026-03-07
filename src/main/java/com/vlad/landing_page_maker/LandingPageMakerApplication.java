package com.vlad.landing_page_maker;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LandingPageMakerApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure()
				.ignoreIfMissing()
				.load();

		System.setProperty("OPENAI_API_KEY", dotenv.get("OPENAI_API_KEY"));
		SpringApplication.run(LandingPageMakerApplication.class, args);
	}

}
