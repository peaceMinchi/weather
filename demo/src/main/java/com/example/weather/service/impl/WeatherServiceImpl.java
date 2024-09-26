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
import com.example.weather.model.OpenApiSearchTimeDTO;
import com.example.weather.model.RegionWeatherUpdateDTO;
import com.example.weather.model.RegionWeatherSelectDTO;
import com.example.weather.service.WeatherService;

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
		String fileLocation = String.format("%s/%s", resourceLocation, "REGION_LIST.csv");
		Path path = Paths.get(fileLocation);	// 유연성
        URI uri = path.toUri();					// 네트워크 리소스나 + 로컬 파일에 통합된 방식으로 접근, 확장성 o
        BufferedReader br = null;

        try {
            br = new BufferedReader(new InputStreamReader(
					new UrlResource(uri).getInputStream()));

            String line = br.readLine();

            if (!line.isEmpty()) {
            	weatherMapper.truncateRegion(); // region_tbl 데이터를 새로 등록하기 위한 기존 데이터 전체 삭제
            } else {
            	return HttpStatus.BAD_REQUEST;
            }

            while ((line = br.readLine()) != null) {
            	String[] splits = line.split(",");

            	RegionWeatherUpdateDTO regionWeatherUpdateDTO = new RegionWeatherUpdateDTO(Integer.parseInt(splits[0]), splits[1], splits[2], Integer.parseInt(splits[3]), Integer.parseInt(splits[4]));
            	weatherMapper.insertRegionData(regionWeatherUpdateDTO);
            }
            return HttpStatus.OK;

        } catch (IOException ex) {
			logger.error(ex.getMessage());
            throw new RuntimeException(ex);
        } catch (Exception ex) {
			logger.error(ex.getMessage());
            throw new RuntimeException(ex);
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
				logger.error(ex.getMessage());
                throw new RuntimeException(ex);
            }
        }
	}

	@Override
	public RegionWeatherSelectDTO selectRegionWeather(int id) {
		return weatherMapper.selectRegionWeather(id);
	}
	
	@Override
	public List<RegionWeatherSelectDTO> selectSeoulWeatherList() {
		return weatherMapper.selectSeoulWeatherList();
	}

	@Override
	public List<RegionWeatherSelectDTO> selectRegionWeatherList() {
		return weatherMapper.selectRegionWeatherList();
	}

	@Override
	public List<RegionWeatherUpdateDTO> putAllWeather() {
		// 1. Open Api 요청에 필요한 '시간' 가져오기
		OpenApiSearchTimeDTO search = this.setOpenApiSearch();
		
		// 2. Open Api 요청 '시각' 가져오기
        String requestTime = this.getLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));

		// 3. 전체 지역 목록 조회
		List<RegionWeatherUpdateDTO> list = weatherMapper.selectRegionList();

        for (int i = 0; i < list.size(); i++) {
        	RegionWeatherUpdateDTO regionWeatherUpdateDTO = list.get(i);
        	regionWeatherUpdateDTO.setRequestTime(requestTime); // Open Api 를 요청한 '시각' 셋팅
        	this.httpConnectionOpenApiWeather(search, regionWeatherUpdateDTO); // API 요청
        }

		return list;
	}
	
	@Override
	public List<RegionWeatherUpdateDTO> putSeoulWeather() {
		// 1. Open Api 요청에 필요한 '시간' 가져오기
		OpenApiSearchTimeDTO search = setOpenApiSearch();

		// 2. Open Api 요청 '시각' 가져오기
        String requestTime = getLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        
		// 3. 서울 지역 목록 조회
		List<RegionWeatherUpdateDTO> list = weatherMapper.selectSeoulList();
		for (int i = 0; i < list.size(); i++) {
			RegionWeatherUpdateDTO regionWeatherUpdateDTO = list.get(i);
        	regionWeatherUpdateDTO.setRequestTime(requestTime); // Open Api 를 요청한 '시각' 셋팅
        	this.httpConnectionOpenApiWeather(search, regionWeatherUpdateDTO); // API 요청
		}

		return list;
	}


	@Override
	public RegionWeatherUpdateDTO putOnceWeather(int regionId) {
		// 1. Open Api 요청에 필요한 '시간' 가져오기
		OpenApiSearchTimeDTO search = setOpenApiSearch();

		// 2. Open Api 요청 '시각' 가져오기
        String requestTime = getLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        
        // 3. 지역 정보 '단건' 조회
        RegionWeatherUpdateDTO regionWeatherUpdateDTO = weatherMapper.selectRegion(regionId);
    	regionWeatherUpdateDTO.setRequestTime(requestTime); // Open Api 를 요청한 '시각' 셋팅
    	
    	this.httpConnectionOpenApiWeather(search, regionWeatherUpdateDTO); // API 요청
		return regionWeatherUpdateDTO;
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
	private OpenApiSearchTimeDTO setOpenApiSearch() {
        // Open Api 요청에 필요한 파라미터 중 발표 시각 format 하기
        String yyyyMMdd = getLocalDateTime().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int hour = getLocalDateTime().getHour();
        int min = getLocalDateTime().getMinute();
        if(min <= 10) { // 10분 이전에는, 이전시각을 기준으로 해야함
            hour -= 1;
        }
        String hourStr = hour + "00"; // 정시 기준
        
		return OpenApiSearchTimeDTO.builder()
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
	 * @param regionWeatherUpdateDTO
	 */
	@Transactional
	private void httpConnectionOpenApiWeather(OpenApiSearchTimeDTO search, RegionWeatherUpdateDTO regionWeatherUpdateDTO) {
		logger.info("[Start] httpConnectionOpenApiWeather()");
		
        String nx = Integer.toString(regionWeatherUpdateDTO.getNx());
        String ny = Integer.toString(regionWeatherUpdateDTO.getNy());
    	
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
	
	        HttpURLConnection conn = (HttpURLConnection) url.openConnection(); // HTTP 관련 설정 및 요청
	        conn.setRequestMethod("GET");
	        conn.setRequestProperty("Content-type", "application/json");
	
	        BufferedReader rd;
			logger.info("[HttpURLConnection ResponseCode] : {}", conn.getResponseCode());

			// ResponseCode 가 성공인 경우 InputStream 을 BufferedReader 으로 읽어서 응답 데이터를 읽어온다.
	        if(conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
	            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = rd.readLine()) != null) {
					sb.append(line); // 전체 응답 데이터를 하나의 문자열로 만듬
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
				if (!data.isEmpty()) {       // String type -> Json Object Type
					jsonObject = new JSONObject(data);
					response = jsonObject.getJSONObject("response");
					header = response.getJSONObject("header");
					String resultCode = header.getString("resultCode"); // API 상태 코드를 확인하기 위한 값

					logger.info("[resultCode] {}, [regionId] {}", resultCode, regionWeatherUpdateDTO.getId());
					this.openApiReturnMessage(resultCode);

					// resultCode 가 00 인 경우는 정상임으로, 정상일 경우에 응답 데이터를 JSONObject 변경 하여 필요한 데이터를 가져온다.
					if ("00".equals(resultCode)) {
						JSONObject body = response.getJSONObject("body");
						JSONObject items = body.getJSONObject("items");
						JSONArray itemArray = items.getJSONArray("item");

						for(int j = 0; j < itemArray.length(); j++) {
							JSONObject obj = itemArray.getJSONObject(j);
							String category = obj.getString("category");
							BigDecimal obsrValue = obj.getBigDecimal("obsrValue"); // category 별 값

							switch (category) {
								case "T1H": // 기온
									temp = obsrValue;
									break;
								case "RN1": // 1시간 강수량
									rainAmount = obsrValue;
									break;
								case "REH": // 습도
									humid = obsrValue;
									break;
							}
						}
						regionWeatherUpdateDTO.setTemp(temp);
						regionWeatherUpdateDTO.setRainAmount(rainAmount);
						regionWeatherUpdateDTO.setHumid(humid);
						weatherMapper.updateWeather(regionWeatherUpdateDTO); // DB 업데이트
					}
				}
			}
        } catch (UnsupportedEncodingException ex) {
        	// urlBuilder 에 대한 UnsupportedEncodingException 예외처리
        	logger.error(ex.getMessage());
		} catch (JSONException ex) {
			// 가져올 JSONObject 가 없는 경우 JSON Parser Exception 이 발생하여 예외처리
			logger.error(ex.getMessage());
		} catch (Exception ex) {
			logger.error(ex.getMessage());
		}
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
