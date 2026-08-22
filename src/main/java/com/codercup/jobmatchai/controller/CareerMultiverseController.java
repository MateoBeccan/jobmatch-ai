package com.codercup.jobmatchai.controller;

import com.codercup.jobmatchai.dto.career.CareerMarketRequest;
import com.codercup.jobmatchai.dto.career.CareerMarketResponse;
import com.codercup.jobmatchai.dto.career.CareerMultiverseRequest;
import com.codercup.jobmatchai.dto.career.CareerMultiverseResponse;
import com.codercup.jobmatchai.service.CareerMarketService;
import com.codercup.jobmatchai.service.CareerMultiverseService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CareerMultiverseController {

	private final CareerMarketService careerMarketService;
	private final CareerMultiverseService careerMultiverseService;

	public CareerMultiverseController(
			CareerMarketService careerMarketService,
			CareerMultiverseService careerMultiverseService
	) {
		this.careerMarketService = careerMarketService;
		this.careerMultiverseService = careerMultiverseService;
	}

	@PostMapping(path = "/api/career/market", consumes = MediaType.APPLICATION_JSON_VALUE)
	public CareerMarketResponse market(@RequestBody(required = false) CareerMarketRequest request) {
		return careerMarketService.analyze(request);
	}

	@PostMapping(path = "/api/career/multiverse", consumes = MediaType.APPLICATION_JSON_VALUE)
	public CareerMultiverseResponse multiverse(@RequestBody(required = false) CareerMultiverseRequest request) {
		return careerMultiverseService.generate(request);
	}
}
