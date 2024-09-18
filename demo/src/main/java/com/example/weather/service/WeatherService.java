package com.example.weather.service;

import java.util.List;

import org.springframework.http.HttpStatus;

import com.example.weather.model.Region;
import com.example.weather.model.RegionWeather;

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
	public RegionWeather selectRegionWeather(int regionId);
	/**
	 * 서울 날씨 정보 목록 조회
	 * 
	 * @return
	 */
	public List<RegionWeather> selectSeoulWeatherList();	
	/**
	 * 전 지역 날씨 목록 조회
	 * 
	 * @return
	 */
	public List<RegionWeather> selectRegionWeatherList();	
	/**
	 * 전 지역 날씨 예보 수정
	 * 
	 * @return
	 */
	public List<Region> putAllWeather();	
	/**
	 * 서울 지역 날씨 예보 수정
	 * 
	 * @return
	 */
	public List<Region> putSeoulWeather();	
	/**
	 * 날씨 예보 단건 수정
	 * 
	 * @param regionId
	 * @return
	 */
	public Region putOnceWeather(int regionId);	
	/**
	 * 날씨 정보 단건 삭제
	 * 
	 * @param regionId
	 */
	public void deleteRegion(int regionId);
	
	// -------------------------------------------------------- before

//	public int insertRegionData(RegionWeather region);
//	
//	public RegionWeather selectRegionData(int regionId);
//	
//	public List<RegionWeather> selectSeoulList();
//	
//	public List<RegionWeather> selectRegionWeatherList();
//	
//	public void updateWeather(RegionWeather weather);
//	
//	public void deleteRegion(int regionId);
//	
//	public void truncateRegion();
//
//	public List<RegionWeather> selectAlllList();
}
