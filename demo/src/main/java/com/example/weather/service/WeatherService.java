package com.example.weather.service;

import java.util.List;

import com.example.weather.model.Region;

public interface WeatherService {

	public int insertRegionData(Region region);
	
	public Region selectRegionData(int id);
	
	public List<Region> selectSeoulList();
	
	public void updateWeather(Region weather);
}
