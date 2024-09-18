package com.example.weather.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
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
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.weather.mapper.WeatherMapper;
import com.example.weather.model.OpenApiSearch;
import com.example.weather.model.Region;
import com.example.weather.model.RegionWeather;
import com.example.weather.service.WeatherService;

import ch.qos.logback.core.util.StringUtil;

@Service
public class WeatherServiceImpl implements WeatherService {
	@Value("${resources.location}")
	private String resourceLocation;
	
	@Value("${weatherApi.serviceKey}")
    private String serviceKey;
	
	private final String OPEN_API_URL = "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst";

	@Autowired
	private WeatherMapper weatherMapper;
	
	private static final Logger logger = LoggerFactory.getLogger(WeatherServiceImpl.class.getName());

	@Override
	public HttpStatus insertRegionData() {
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
            	weatherMapper.truncateRegion(); // region_tbl 데이터를 새로 등록하기 위한 기존 데이터 전체 삭제
            } else {
            	return HttpStatus.BAD_REQUEST;
            }

            while ((line = br.readLine()) != null) {
            	String[] splits = line.split(",");

            	Region region = new Region(Integer.parseInt(splits[0]), splits[1], splits[2], Integer.parseInt(splits[3]), Integer.parseInt(splits[4]));
            	weatherMapper.insertRegionData(region);
            }
            
            return HttpStatus.OK;
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
	}

	@Override
	public RegionWeather selectRegionWeather(int id) {
		return weatherMapper.selectRegionWeather(id);
	}
	
	@Override
	public List<RegionWeather> selectSeoulWeatherList() {
		return weatherMapper.selectSeoulWeatherList();
	}

	@Override
	public List<RegionWeather> selectRegionWeatherList() {
		return weatherMapper.selectRegionWeatherList();
	}

	@Override
	public List<Region> putAllWeather() {
		// 1. Open Api 의 파라미터 가져오기
		OpenApiSearch search = this.setOpenApiSearch();
		
		// 2. Open Api 를 요청 시각 가져오기
        String requestTime = this.getLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));

		// 3. 지역 목록 조회
		List<Region> list = weatherMapper.selectRegionList();
        for (int i = 0; i < list.size(); i++) {
        	Region region = list.get(i);
        	region.setRequestTime(requestTime); // Open Api 를 요청한 시각 셋팅
        	Boolean result = this.httpConnectionOpenApiWeather(search, region);
        	if (!result) continue; // 요청 반환값이 false 인 경우에는 continue;
        }
        
		return list;
	}
	
	@Override
	public List<Region> putSeoulWeather() {
		// 1. Open Api 의 파라미터 가져오기
		OpenApiSearch search = setOpenApiSearch();
		
		// 2. Open Api 를 요청 시각 가져오기
        String requestTime = getLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        
		// 3. 서울 지역 목록 조회
		List<Region> list = weatherMapper.selectSeoulList();
		for (int i = 0; i < list.size(); i++) {
			Region region = list.get(i);
        	region.setRequestTime(requestTime); // Open Api 를 요청한 시각 셋팅
        	Boolean result = this.httpConnectionOpenApiWeather(search, region);
        	if (!result) continue; // 요청 반환값이 false 인 경우에는 continue;
		}

		return list;
	}


	@Override
	public Region putOnceWeather(int regionId) {
		// 1. Open Api 의 파라미터 가져오기
		OpenApiSearch search = setOpenApiSearch();
		
		// 2. Open Api 를 요청 시각 가져오기
        String requestTime = getLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        
        // 3. 지역 정보 조회
        Region region = weatherMapper.selectRegion(regionId);
    	region.setRequestTime(requestTime); // Open Api 를 요청한 시각 셋팅
    	
    	this.httpConnectionOpenApiWeather(search, region);
		return region;
	}

	@Override
	public void deleteRegion(int regionId) {
		weatherMapper.deleteRegion(regionId);
	}

	/**
	 * 현재 시각 조회
	 * 
	 * @return
	 */
	private LocalDateTime getLocalDateTime() {
		return LocalDateTime.now();
	}
	
	/**
	 * Open Api 파라미터 셋팅
	 * 
	 * @return
	 */
	private OpenApiSearch setOpenApiSearch() {
        // Open Api 요청에 필요한 파라미터 중 발표 시각 format 하기
        String yyyyMMdd = getLocalDateTime().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int hour = getLocalDateTime().getHour();
        int min = getLocalDateTime().getMinute();
        if(min <= 10) { // 해당 시각 발표 전에는 자료가 없음 - 이전시각을 기준으로 해야함 - 10분 부터 적용
            hour -= 1;
        }
        String hourStr = hour + "00"; // 정시 기준
        
		return OpenApiSearch.builder()
				.date(yyyyMMdd)
				.hour(hourStr)
				.build();
	}

	/**
	 * httpConnectionOpenApiWeather
	 * 
	 * Weather Open API 호출
	 * 
	 * @param search
	 * @param region
	 */
	@Transactional
	private Boolean httpConnectionOpenApiWeather(OpenApiSearch search, Region region) {
		logger.info("[Start] httpConnectionOpenApiWeather()");
		Boolean result = true;
		
        String nx = Integer.toString(region.getNx());
        String ny = Integer.toString(region.getNy());
    	
		StringBuilder urlBuilder =  new StringBuilder(OPEN_API_URL);
        try {
			urlBuilder.append("?" + URLEncoder.encode("serviceKey", "UTF-8") + "=" + serviceKey);
	        urlBuilder.append("&" + URLEncoder.encode("pageNo","UTF-8") + "=" + URLEncoder.encode("1", "UTF-8")); /*페이지번호*/
	        urlBuilder.append("&" + URLEncoder.encode("numOfRows","UTF-8") + "=" + URLEncoder.encode("1000", "UTF-8")); /*한 페이지 결과 수*/
	        urlBuilder.append("&" + URLEncoder.encode("dataType","UTF-8") + "=" + URLEncoder.encode("JSON", "UTF-8")); /*요청자료형식(XML/JSON) Default: XML*/
	        urlBuilder.append("&" + URLEncoder.encode("base_date","UTF-8") + "=" + URLEncoder.encode(search.getDate(), "UTF-8")); /*‘21년 6월 28일 발표*/
	        urlBuilder.append("&" + URLEncoder.encode("base_time","UTF-8") + "=" + URLEncoder.encode(search.getHour(), "UTF-8")); /*06시 발표(정시단위) */
	        urlBuilder.append("&" + URLEncoder.encode("nx","UTF-8") + "=" + URLEncoder.encode(nx, "UTF-8")); /*예보지점의 X 좌표값*/
	        urlBuilder.append("&" + URLEncoder.encode("ny","UTF-8") + "=" + URLEncoder.encode(ny, "UTF-8")); /*예보지점의 Y 좌표값*/
	
	        URL url = new URL(urlBuilder.toString());
	        
	        logger.info("[url] {}", urlBuilder.toString());
	
	        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	        conn.setRequestMethod("GET"); // 인터페이스 표준
	        conn.setRequestProperty("Content-type", "application/json"); // 교환 데이터 표준 설정
	
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
			logger.info("[data] {}", data);

	        BigDecimal temp = null;
	        BigDecimal humid = null;
	        BigDecimal rainAmount = null;
	        
	        JSONObject jsonObject = null;
	        JSONObject response = null;
	        JSONObject header = null;
	        if (!StringUtil.isNullOrEmpty(data)) {
				jsonObject = new JSONObject(data); // String type -> Json Type
				response = jsonObject.getJSONObject("response");
				header = response.getJSONObject("header");
				String resultCode = header.getString("resultCode"); // API 상태 코드를 확인하기 위한 값

				logger.info("[resultCode] {}, [regionId] {}", resultCode, region.getId());
				this.openApiReturnMessage(resultCode);
				if (!"00".equals(resultCode)) {
					result = false;
					return result;
				}
				
				JSONObject body = response.getJSONObject("body");
				JSONObject items = body.getJSONObject("items");
				JSONArray itemArray = items.getJSONArray("item");
		
		        for(int j = 0; j < itemArray.length(); j++) {
		            JSONObject obj = itemArray.getJSONObject(j);
		            String category = obj.getString("category");
		            BigDecimal obsrValue = obj.getBigDecimal("obsrValue"); // category 별 값
		
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
		        weatherMapper.updateWeather(region); // DB 업데이트
	        }
        } catch (UnsupportedEncodingException ex) {
        	// urlBuilder 에 대한 UnsupportedEncodingException 예외처리
        	logger.error(ex.getMessage());
        	result = false;
			return result;
		} catch (JSONException ex) {
			// 가져올 JSONObject 가 없는 경우 JSON Parser Exception 이 발생하여 예외처리
			logger.error(ex.getMessage());
			result = false;
			return result;
		} catch (Exception ex) {
			logger.error(ex.getMessage());
			result = false;
			return result;
		}
        
    	return result;
	}

	/**
	 * Open Api Return Message
	 *
	 * @param resultCode
	 * @return
	 */
	private void openApiReturnMessage(String resultCode) {
		switch (resultCode) {
			case "00": {
				logger.info("[Open Api Return Message] 정상");
				break;
			}
			case "01": {
				logger.info("[Open Api Return Message] 어플리케이션 에러");
				break;
			}
			case "02": {
				logger.info("[Open Api Return Message] 데이터베이스 에러");
				break;
			}
			case "03": {
				logger.info("[Open Api Return Message] 데이터 없음 에러");
				break;
			}
			case "04": {
				logger.info("[Open Api Return Message] HTTP 에러");
				break;
			}
			case "05": {
				logger.info("[Open Api Return Message] 서비스 연결 실패 에러");
				break;
			}
			case "10": {
				logger.info("[Open Api Return Message] 잘못된 요청 파라미터 에러");
				break;
			}
			case "11": {
				logger.info("[Open Api Return Message] 필수 요청 파라미터 없음");
				break;
			}
			case "12": {
				logger.info("[Open Api Return Message] 해당 Open API 서비스가 없거나 폐기됨");
				break;
			}
			case "20": {
				logger.info("[Open Api Return Message] 서비스 접근 거부");
				break;
			}
			case "21": {
				logger.info("[Open Api Return Message] 일시적 사용할 수 없는 서비스 Key");
				break;
			}
			case "22": {
				logger.info("[Open Api Return Message] 서비스 요청 제한 횟수 초과 에러");
				break;
			}
			case "30": {
				logger.info("[Open Api Return Message] 등록되지 안흔 서비스 Key");
				break;
			}
			case "31": {
				logger.info("[Open Api Return Message] 기한 만료된 서비스 Key");
				break;
			}
			case "32": {
				logger.info("[Open Api Return Message] 등록되지 않은 IP");
				break;
			}
			case "33": {
				logger.info("[Open Api Return Message] 성형되지 않은 호출");
				break;
			}
			case "99": {
				logger.info("[Open Api Return Message] 기타 에러");
				break;
			}
		}
	}
}
