package com.example.weather.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import com.example.weather.model.RegionWeather;

@Mapper
public interface WeatherMapper {

	public int insertRegionData(RegionWeather region);
	
	public RegionWeather selectRegionData(int regionId);
	
	public List<RegionWeather> selectSeoulList();
	
	public List<RegionWeather> selectRegionWeatherList();
	
	public void updateWeather(RegionWeather region);
	
	public int deleteRegion(int regionId);
	
	public void truncateRegion();

	public List<RegionWeather> selectAlllList();
}
