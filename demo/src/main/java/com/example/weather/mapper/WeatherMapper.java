package com.example.weather.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.weather.model.Region;
import com.example.weather.model.RegionWeather;

@Mapper
public interface WeatherMapper {

	public int insertRegionData(Region region);
	
	public RegionWeather selectRegionWeather(int regionId);

	public List<RegionWeather> selectRegionWeatherList();
	
	public List<RegionWeather> selectSeoulWeatherList();
	
	public Region selectRegion(int regionId);
	
	public List<Region> selectRegionList();

	public List<Region> selectSeoulList();
	
	public void updateWeather(Region region);
	
	public int deleteRegion(int regionId);
	
	public void truncateRegion();

	String test();
}
