package com.example.weather.service;

import java.util.List;

import com.example.weather.model.RegionWeatherSelectDTO;
import org.springframework.http.HttpStatus;

import com.example.weather.model.RegionWeatherUpdateDTO;

public interface WeatherService {
	/**
	 * CSV 파일 등록
	 * 
	 * @param region
	 * @return
	 */
	public HttpStatus insertRegionData();
	/**
	 * 날씨 정보 단건 조회
	 * 
	 * @param regionId
	 * @return
	 */
	public RegionWeatherSelectDTO selectRegionWeather(int regionId);
	/**
	 * 서울 날씨 정보 목록 조회
	 * 
	 * @return
	 */
	public List<RegionWeatherSelectDTO> selectSeoulWeatherList();
	/**
	 * 전 지역 날씨 목록 조회
	 * 
	 * @return
	 */
	public List<RegionWeatherSelectDTO> selectRegionWeatherList();
	/**
	 * 전 지역 날씨 예보 수정
	 * 
	 * @return
	 */
	public List<RegionWeatherUpdateDTO> putAllWeather();
	/**
	 * 서울 지역 날씨 예보 수정
	 * 
	 * @return
	 */
	public List<RegionWeatherUpdateDTO> putSeoulWeather();
	/**
	 * 날씨 예보 단건 수정
	 * 
	 * @param regionId
	 * @return
	 */
	public RegionWeatherUpdateDTO putOnceWeather(int regionId);
	/**
	 * 날씨 정보 단건 삭제
	 * 
	 * @param regionId
	 */
	public void deleteRegion(int regionId);
	
	// -------------------------------------------------------- before

//	public int insertRegionData(RegionWeatherSelectDTO region);
//	
//	public RegionWeatherSelectDTO selectRegionData(int regionId);
//	
//	public List<RegionWeatherSelectDTO> selectSeoulList();
//	
//	public List<RegionWeatherSelectDTO> selectRegionWeatherList();
//	
//	public void updateWeather(RegionWeatherSelectDTO weather);
//	
//	public void deleteRegion(int regionId);
//	
//	public void truncateRegion();
//
//	public List<RegionWeatherSelectDTO> selectAlllList();
}
