package com.example.weather.controller;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.weather.model.Region;
import com.example.weather.model.Weather;
import com.example.weather.service.WeatherService;

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
	
	
	private Logger logger = LoggerFactory.getLogger(RestWeatherController.class);

	/**
	 * csv 파일 저장
	 * 
	 */
	@GetMapping("/region")
	public ResponseEntity<String> setRegionData() {
		
		String fileLocation = String.format("%s/%s", resourceLocation, "REGION_LIST.csv"); // 위도 경도 파일 경로
        Path path = Paths.get(fileLocation);
        URI uri = path.toUri();
        BufferedReader br = null;
    
        try {
        	
            br = new BufferedReader(new InputStreamReader(
                    new UrlResource(uri).getInputStream()));
            String line = br.readLine(); // head 떼기

            while ((line = br.readLine()) != null) {
            	String[] splits = line.split(",");
            	
            	Region region = new Region(Integer.parseInt(splits[0]), splits[1], splits[2], Integer.parseInt(splits[3]), Integer.parseInt(splits[4]));
            	weatherService.insertRegionData(region);
               
            }
        } catch (IOException e) {
        	
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
        
        return ResponseEntity.ok("데이터 저장 완료");
	}
	
	@Transactional
	@GetMapping("/getSeoulWeather")
	@Description("서울날씨 조회")
	public ResponseEntity<List<Region>> getSeoulWeather() {
        List<Region> seoulList = weatherService.selectSeoulList();

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
        	Region region = seoulList.get(i);
        	
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
	            
	            logger.debug(":::::::::::::: [요청 url] > " +  urlBuilder.toString());
	
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
	
	            //// 응답 수신 완료 ////
	            //// 응답 결과를 JSON 파싱 ////
	
	            Double temp = null;
	            Double humid = null;
	            Double rainAmount = null;
	            
	            logger.debug(":::::::::::::: [응답 데이터] > " + region.getRegionParent() + " > " + region.getRegionChild() + " > "  + data);
	
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
	                double obsrValue = obj.getDouble("obsrValue");
	
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
	
	            region.setWeather(new Weather(temp, rainAmount, humid, currentChangeTime));
	            weatherService.updateWeather(region); // DB 업데이트
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
        }
        
        return ResponseEntity.ok(seoulList);
    }
	
	@Transactional
	@GetMapping("/getWeather")
	@Description("날씨 단건 조회")
	public ResponseEntity<Region> getRegionWeather(@RequestParam int regionId) {

        StringBuilder urlBuilder =  new StringBuilder("http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst");
        Region region = weatherService.selectRegionData(regionId);

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

        // 기준 시각 조회 자료가 이미 존재하고 있다면 API 요청 없이 기존 자료 그대로 넘김
        Weather prevWeather = region.getWeather();
//        if(prevWeather != null && prevWeather.getLastUpdateTime() != null) {
//            if(prevWeather.getLastUpdateTime().equals(currentChangeTime)) {
//                log.info("기존 자료를 재사용합니다");
//                WeatherResponseDTO dto = WeatherResponseDTO.builder()
//                        .weather(prevWeather)
//                        .message("OK").build();
//                return ResponseEntity.ok(dto);
//            }
//        }

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

            //// 응답 수신 완료 ////
            //// 응답 결과를 JSON 파싱 ////

            Double temp = null;
            Double humid = null;
            Double rainAmount = null;

            JSONObject jObject = new JSONObject(data);
            JSONObject response = jObject.getJSONObject("response");
            JSONObject body = response.getJSONObject("body");
            JSONObject items = body.getJSONObject("items");
            JSONArray jArray = items.getJSONArray("item");

            for(int i = 0; i < jArray.length(); i++) {
                JSONObject obj = jArray.getJSONObject(i);
                String category = obj.getString("category");
                double obsrValue = obj.getDouble("obsrValue");

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

            region.setWeather(new Weather(temp, rainAmount, humid, currentChangeTime));
            weatherService.updateWeather(region); // DB 업데이트
           
            return ResponseEntity.ok(region);

        } catch (IOException e) {
            e.printStackTrace();
        }
        
		return null;
    }
	
}
