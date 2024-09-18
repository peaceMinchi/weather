package com.example.weather.controller;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.weather.model.Region;
import com.example.weather.model.RegionWeather;
import com.example.weather.service.WeatherService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/weather")
public class RestWeatherController {
	
	@Autowired
	private WeatherService weatherService;

	private static final Logger logger = LoggerFactory.getLogger(RestWeatherController.class.getName());

	@PostMapping("/postRegionData")
	@Description("지역 정보 csv 파일 파싱 및 DB 저장")
	public ResponseEntity<String> postRegionData() {
		logger.info("[Start] postRegionData()");
		HttpStatus result = weatherService.insertRegionData();
		if (HttpStatus.OK.equals(result)) {
			return ResponseEntity.ok("[Success] 지역 정보 csv 파일 파싱 및 DB 저장 완료");
		}
		return ResponseEntity.ok("[Fail] 지역 정보 csv 파일 파싱 및 DB 저장 실패");
	}
	
	@GetMapping("getRegionWeather")
	@Description("지역 날씨 단건 조회")
	public ResponseEntity<RegionWeather> getRegionWeather(@RequestParam int regionId) {
		logger.info("[Start] getRegionWeather() parameter : {}", regionId);
		return ResponseEntity.ok(weatherService.selectRegionWeather(regionId));
	}
	
	@GetMapping("getSeoulWeather")
	@Description("서울 날씨 목록 조회")
	public ResponseEntity<List<RegionWeather>> getSeoulWeather() {
		logger.info("[Start] getSeoulWeather()");
		return ResponseEntity.ok(weatherService.selectSeoulWeatherList());
	}
	
	@GetMapping("getRegionWeatherList")
	@Description("전 지역 날씨 목록 조회")
	public ResponseEntity<List<RegionWeather>> getRegionWeatherList() {
		logger.info("[Start] getRegionWeatherList()");
		return ResponseEntity.ok(weatherService.selectRegionWeatherList());
	}
	
	
	@PutMapping("/putAllWeather")
	@Description("전 지역 날씨 예보 수정")
	public ResponseEntity<List<Region>> putAllWeather() {
		logger.info("[Start] putAllWeather()");
		return ResponseEntity.ok(weatherService.putAllWeather());
    }

	@PutMapping("/putSeoulWeather")
	@Description("서울 지역 날씨 예보 수정")
	public ResponseEntity<List<Region>> putSeoulWeather() {
		logger.info("[Start] putSeoulWeather()");
		return ResponseEntity.ok(weatherService.putSeoulWeather());
	}

	@PutMapping("/putOnceWeather")
	@Description("날씨 예보 단건 수정")
	public ResponseEntity<Region> putOnceWeather(@RequestParam int regionId) {
		logger.info("[Start] putOnceWeather() parameter : {}", regionId);
		return ResponseEntity.ok(weatherService.putOnceWeather(regionId));
    }
	
	@DeleteMapping("/deleteRegion")
	@Description("날씨 정보 단건 삭제")
	public ResponseEntity<Integer> deleteRegion(@RequestParam int regionId) {
		logger.info("[Start] deleteRegion() parameter : {}", regionId);
		weatherService.deleteRegion(regionId);
		return ResponseEntity.ok(regionId);
    }

	@GetMapping("/getLogger")
	public void printLog() {
        logger.debug("[debug] log!");
        logger.info("[info] log!");
        logger.warn("[warn] log!");
        logger.error("[error] log!");
    }
}
