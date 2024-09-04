package com.example.weather.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.weather.model.Region;

@Mapper
public interface WeatherMapper {

	public int insertRegionData(Region region);
	
	public Region selectRegionData(int id);
	
	public List<Region> selectSeoulList();
	
	public void updateWeather(Region region);
}
