package com.example.weather.service;

import java.util.List;

import com.example.weather.model.RegionWeather;

public interface WeatherService {

	public int insertRegionData(RegionWeather region);
	
	public RegionWeather selectRegionData(int regionId);
	
	public List<RegionWeather> selectSeoulList();
	
	public List<RegionWeather> selectRegionWeatherList();
	
	public void updateWeather(RegionWeather weather);
	
	public void deleteRegion(int regionId);
	
	public void truncateRegion();
}
