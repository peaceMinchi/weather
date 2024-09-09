package com.example.weather.controller;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Description;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.weather.model.RegionWeather;
import com.example.weather.model.Weather;
import com.example.weather.service.WeatherService;

import ch.qos.logback.core.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/weather")
public class RestWeatherController {
	
	@Value("${resources.location}")
	private String resourceLocation;

	@Autowired
	private WeatherService weatherService;
	
	@Value("${weatherApi.serviceKey}")
    private String serviceKey;

	private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);

	@PostMapping("/postRegionData")
	@Description("지역 정보 csv 파일 파싱 및 DB 저장")
	public ResponseEntity<String> postRegionData() {

		String fileLocation = String.format("%s/%s", resourceLocation, "REGION_LIST.csv"); // 위도 경도 파일 경로
		Path path = Paths.get(fileLocation);
        URI uri = path.toUri();
        BufferedReader br = null;

        try {
        	
            br = new BufferedReader(new InputStreamReader(
					new UrlResource(uri).getInputStream()));
					//resource.getInputStream()));
            String line = br.readLine(); // 첫줄 제거

            if (!StringUtil.isNullOrEmpty(line)) {
            	weatherService.truncateRegion(); // region_tbl 데이터를 새로 등록하기 위한 기존 데이터 전체 삭제
            }

            while ((line = br.readLine()) != null) {
            	String[] splits = line.split(",");

            	RegionWeather region = new RegionWeather(Integer.parseInt(splits[0]), splits[1], splits[2], Integer.parseInt(splits[3]), Integer.parseInt(splits[4]));
            	weatherService.insertRegionData(region);
            }
        } catch (IOException e) {
        	e.printStackTrace();
            throw new RuntimeException(e);
        } catch (Exception e) {
        	e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            try {
                br.close();
            } catch (IOException e) {
            	e.printStackTrace();
                throw new RuntimeException(e);   
            }
        }
        
        return ResponseEntity.ok("지역 정보 csv 파일 파싱 및 DB 저장 완료");
	}
	
	@GetMapping("getRegionWeather")
	@Description("지역 날씨 단건 조회")
	public ResponseEntity<RegionWeather> getRegionWeather(@RequestParam int regionId) {
		logger.info("weather data for regionId: {}", regionId);
		return ResponseEntity.ok(weatherService.selectRegionData(regionId));
	}
	
	@GetMapping("getSeoulWeather")
	@Description("서울 날씨 목록 조회")
	public ResponseEntity<List<RegionWeather>> getSeoulWeather() {
		return ResponseEntity.ok(weatherService.selectSeoulList());
	}
	
	@GetMapping("getRegionWeatherList")
	@Description("지역 날씨 목록 조회")
	public ResponseEntity<List<RegionWeather>> getRegionWeatherList() {
		return ResponseEntity.ok(weatherService.selectRegionWeatherList());
	}
	
	@PutMapping("/putAllWeather")
	@Description("날씨 예보 수정")
	public ResponseEntity<List<RegionWeather>> putAllWeather() {
        List<RegionWeather> seoulList = weatherService.selectAlllList();

        // 2. 요청 시각 조회
        LocalDateTime now = LocalDateTime.now();
        String yyyyMMdd = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int hour = now.getHour();
        int min = now.getMinute();
        if(min <= 30) { // 해당 시각 발표 전에는 자료가 없음 - 이전시각을 기준으로 해야함
            hour -= 1;
        }
        String hourStr = hour + "00"; // 정시 기준
        
        for (int i = 0; i < seoulList.size(); i++) {
        	RegionWeather region = seoulList.get(i);
        	
	        String nx = Integer.toString(region.getNx());
	        String ny = Integer.toString(region.getNy());
	        String currentChangeTime = now.format(DateTimeFormatter.ofPattern("yy.MM.dd ")) + hour;

	
	        try {
	            StringBuilder urlBuilder =  new StringBuilder("http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst");
	            urlBuilder.append("?" + URLEncoder.encode("serviceKey", "UTF-8") + "=" + serviceKey);
	            urlBuilder.append("&" + URLEncoder.encode("pageNo","UTF-8") + "=" + URLEncoder.encode("1", "UTF-8")); /*페이지번호*/
	            urlBuilder.append("&" + URLEncoder.encode("numOfRows","UTF-8") + "=" + URLEncoder.encode("1000", "UTF-8")); /*한 페이지 결과 수*/
	            urlBuilder.append("&" + URLEncoder.encode("dataType","UTF-8") + "=" + URLEncoder.encode("JSON", "UTF-8")); /*요청자료형식(XML/JSON) Default: XML*/
	            urlBuilder.append("&" + URLEncoder.encode("base_date","UTF-8") + "=" + URLEncoder.encode(yyyyMMdd, "UTF-8")); /*‘21년 6월 28일 발표*/
	            urlBuilder.append("&" + URLEncoder.encode("base_time","UTF-8") + "=" + URLEncoder.encode(hourStr, "UTF-8")); /*06시 발표(정시단위) */
	            urlBuilder.append("&" + URLEncoder.encode("nx","UTF-8") + "=" + URLEncoder.encode(nx, "UTF-8")); /*예보지점의 X 좌표값*/
	            urlBuilder.append("&" + URLEncoder.encode("ny","UTF-8") + "=" + URLEncoder.encode(ny, "UTF-8")); /*예보지점의 Y 좌표값*/
	
	            URL url = new URL(urlBuilder.toString());
	
	            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	            conn.setRequestMethod("GET");
	            conn.setRequestProperty("Content-type", "application/json");
	
	            BufferedReader rd;
	            if(conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
	                rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	            } else {
	                rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
	            }
	            StringBuilder sb = new StringBuilder();
	            String line;
	            while ((line = rd.readLine()) != null) {
	                sb.append(line);
	            }
	            rd.close();
	            conn.disconnect();
	            
	            String data = sb.toString();
	            BigDecimal temp = null;
	            BigDecimal humid = null;
	            BigDecimal rainAmount = null;
	
	            JSONObject jObject = new JSONObject(data);
	            JSONObject response = jObject.getJSONObject("response");
	            JSONObject header = response.getJSONObject("header");
	            String resultCode = header.getString("resultCode");
	            if (!"00".equals(resultCode)) continue;
	            
	            JSONObject body = response.getJSONObject("body");
	            JSONObject items = body.getJSONObject("items");
	            JSONArray jArray = items.getJSONArray("item");
	  
	            for(int j = 0; j < jArray.length(); j++) {
	                JSONObject obj = jArray.getJSONObject(j);
	                String category = obj.getString("category");
	                BigDecimal obsrValue = obj.getBigDecimal("obsrValue");
	
	                switch (category) {
	                    case "T1H":
	                        temp = obsrValue;
	                        break;
	                    case "RN1":
	                        rainAmount = obsrValue;
	                        break;
	                    case "REH":
	                        humid = obsrValue;
	                        break;
	                }
	            }
				region.setTemp(temp);
				region.setRainAmount(rainAmount);
				region.setHumid(humid);
				//region.setRegionChild(currentChangeTime);
	            weatherService.updateWeather(region); // DB 업데이트
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
        }
        
        return ResponseEntity.ok(seoulList);
    }

	@PutMapping("/putSeoulWeather")
	@Description("날씨 예보 서울지역 수정")
	public ResponseEntity<List<RegionWeather>> putSeoulWeather() {
		List<RegionWeather> seoulList = weatherService.selectSeoulList();

		// 2. 요청 시각 조회
		LocalDateTime now = LocalDateTime.now();
		String yyyyMMdd = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		int hour = now.getHour();
		int min = now.getMinute();
		if(min <= 30) { // 해당 시각 발표 전에는 자료가 없음 - 이전시각을 기준으로 해야함
			hour -= 1;
		}
		String hourStr = hour + "00"; // 정시 기준

		for (int i = 0; i < seoulList.size(); i++) {
			RegionWeather region = seoulList.get(i);

			String nx = Integer.toString(region.getNx());
			String ny = Integer.toString(region.getNy());
			String currentChangeTime = now.format(DateTimeFormatter.ofPattern("yy.MM.dd ")) + hour;


			try {
				StringBuilder urlBuilder =  new StringBuilder("http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst");
				urlBuilder.append("?" + URLEncoder.encode("serviceKey", "UTF-8") + "=" + serviceKey);
				urlBuilder.append("&" + URLEncoder.encode("pageNo","UTF-8") + "=" + URLEncoder.encode("1", "UTF-8")); /*페이지번호*/
				urlBuilder.append("&" + URLEncoder.encode("numOfRows","UTF-8") + "=" + URLEncoder.encode("1000", "UTF-8")); /*한 페이지 결과 수*/
				urlBuilder.append("&" + URLEncoder.encode("dataType","UTF-8") + "=" + URLEncoder.encode("JSON", "UTF-8")); /*요청자료형식(XML/JSON) Default: XML*/
				urlBuilder.append("&" + URLEncoder.encode("base_date","UTF-8") + "=" + URLEncoder.encode(yyyyMMdd, "UTF-8")); /*‘21년 6월 28일 발표*/
				urlBuilder.append("&" + URLEncoder.encode("base_time","UTF-8") + "=" + URLEncoder.encode(hourStr, "UTF-8")); /*06시 발표(정시단위) */
				urlBuilder.append("&" + URLEncoder.encode("nx","UTF-8") + "=" + URLEncoder.encode(nx, "UTF-8")); /*예보지점의 X 좌표값*/
				urlBuilder.append("&" + URLEncoder.encode("ny","UTF-8") + "=" + URLEncoder.encode(ny, "UTF-8")); /*예보지점의 Y 좌표값*/

				URL url = new URL(urlBuilder.toString());

				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("GET");
				conn.setRequestProperty("Content-type", "application/json");

				BufferedReader rd;
				if(conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
					rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				} else {
					rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
				}
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = rd.readLine()) != null) {
					sb.append(line);
				}
				rd.close();
				conn.disconnect();

				String data = sb.toString();
				BigDecimal temp = null;
				BigDecimal humid = null;
				BigDecimal rainAmount = null;

				JSONObject jObject = new JSONObject(data);
				JSONObject response = jObject.getJSONObject("response");
				JSONObject header = response.getJSONObject("header");
				String resultCode = header.getString("resultCode");
				if (!"00".equals(resultCode)) continue;

				JSONObject body = response.getJSONObject("body");
				JSONObject items = body.getJSONObject("items");
				JSONArray jArray = items.getJSONArray("item");

				for(int j = 0; j < jArray.length(); j++) {
					JSONObject obj = jArray.getJSONObject(j);
					String category = obj.getString("category");
					BigDecimal obsrValue = obj.getBigDecimal("obsrValue");

					switch (category) {
						case "T1H":
							temp = obsrValue;
							break;
						case "RN1":
							rainAmount = obsrValue;
							break;
						case "REH":
							humid = obsrValue;
							break;
					}
				}
				region.setTemp(temp);
				region.setRainAmount(rainAmount);
				region.setHumid(humid);
				//region.setRegionChild(currentChangeTime);
				weatherService.updateWeather(region); // DB 업데이트
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return ResponseEntity.ok(seoulList);
	}

	@PutMapping("/putOnceWeather")
	@Description("날씨 예보 단건 수정")
	public ResponseEntity<RegionWeather> putOnceWeather(@RequestParam int regionId) {

        StringBuilder urlBuilder =  new StringBuilder("http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst");
        RegionWeather region = weatherService.selectRegionData(regionId);

        // 2. 요청 시각 조회
        LocalDateTime now = LocalDateTime.now();
        String yyyyMMdd = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int hour = now.getHour();
        int min = now.getMinute();
        if(min <= 30) { // 해당 시각 발표 전에는 자료가 없음 - 이전시각을 기준으로 해야함
            hour -= 1;
        }
        String hourStr = hour + "00"; // 정시 기준
        String nx = Integer.toString(region.getNx());
        String ny = Integer.toString(region.getNy());
        String currentChangeTime = now.format(DateTimeFormatter.ofPattern("yy.MM.dd ")) + hour;

        try {
            urlBuilder.append("?" + URLEncoder.encode("serviceKey", "UTF-8") + "=" + serviceKey);
            urlBuilder.append("&" + URLEncoder.encode("pageNo","UTF-8") + "=" + URLEncoder.encode("1", "UTF-8")); /*페이지번호*/
            urlBuilder.append("&" + URLEncoder.encode("numOfRows","UTF-8") + "=" + URLEncoder.encode("1000", "UTF-8")); /*한 페이지 결과 수*/
            urlBuilder.append("&" + URLEncoder.encode("dataType","UTF-8") + "=" + URLEncoder.encode("JSON", "UTF-8")); /*요청자료형식(XML/JSON) Default: XML*/
            urlBuilder.append("&" + URLEncoder.encode("base_date","UTF-8") + "=" + URLEncoder.encode(yyyyMMdd, "UTF-8")); /*‘21년 6월 28일 발표*/
            urlBuilder.append("&" + URLEncoder.encode("base_time","UTF-8") + "=" + URLEncoder.encode(hourStr, "UTF-8")); /*06시 발표(정시단위) */
            urlBuilder.append("&" + URLEncoder.encode("nx","UTF-8") + "=" + URLEncoder.encode(nx, "UTF-8")); /*예보지점의 X 좌표값*/
            urlBuilder.append("&" + URLEncoder.encode("ny","UTF-8") + "=" + URLEncoder.encode(ny, "UTF-8")); /*예보지점의 Y 좌표값*/

            URL url = new URL(urlBuilder.toString());

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-type", "application/json");

            BufferedReader rd;
            if(conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
                rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            } else {
                rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            }
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            rd.close();
            conn.disconnect();
            String data = sb.toString();

            BigDecimal temp = null;
            BigDecimal humid = null;
            BigDecimal rainAmount = null;

            JSONObject jObject = new JSONObject(data);
            JSONObject response = jObject.getJSONObject("response");
            JSONObject body = response.getJSONObject("body");
            JSONObject items = body.getJSONObject("items");
            JSONArray jArray = items.getJSONArray("item");

            for(int i = 0; i < jArray.length(); i++) {
                JSONObject obj = jArray.getJSONObject(i);
                String category = obj.getString("category");
                BigDecimal obsrValue = obj.getBigDecimal("obsrValue");

                switch (category) {
                    case "T1H":
                        temp = obsrValue;
                        break;
                    case "RN1":
                        rainAmount = obsrValue;
                        break;
                    case "REH":
                        humid = obsrValue;
                        break;
                }
            }
			region.setTemp(temp);
			region.setRainAmount(rainAmount);
			region.setHumid(humid);
			//region.setRegionChild(currentChangeTime);
			//region.setLastUpdateTime(currentChangeTime);
            weatherService.updateWeather(region);
           
            return ResponseEntity.ok(region);

        } catch (IOException e) {
            e.printStackTrace();
        }
        
		return ResponseEntity.ok(null);
    }
	
	@DeleteMapping("/deleteRegion")
	@Description("지역 정보 단건 삭제")
	public ResponseEntity<Integer> deleteRegion(@RequestParam int regionId) {
		weatherService.deleteRegion(regionId);
		return ResponseEntity.ok(regionId);
    }

}
