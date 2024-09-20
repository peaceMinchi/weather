package com.example.weather.mapper;

import java.util.List;

import com.example.weather.model.RegionWeatherSelectDTO;
import org.apache.ibatis.annotations.Mapper;

import com.example.weather.model.RegionWeatherUpdateDTO;

@Mapper
public interface WeatherMapper {

	public int insertRegionData(RegionWeatherUpdateDTO regionWeatherUpdateDTO);
	
	public RegionWeatherSelectDTO selectRegionWeather(int regionId);

	public List<RegionWeatherSelectDTO> selectRegionWeatherList();
	
	public List<RegionWeatherSelectDTO> selectSeoulWeatherList();
	
	public RegionWeatherUpdateDTO selectRegion(int regionId);
	
	public List<RegionWeatherUpdateDTO> selectRegionList();

	public List<RegionWeatherUpdateDTO> selectSeoulList();
	
	public void updateWeather(RegionWeatherUpdateDTO regionWeatherUpdateDTO);
	
	public int deleteRegion(int regionId);
	
	public void truncateRegion();

	String test();
}
