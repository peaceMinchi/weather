package com.example.weather.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.weather.mapper.WeatherMapper;
import com.example.weather.model.RegionWeather;
import com.example.weather.service.WeatherService;

@Service
public class WeatherServiceImpl implements WeatherService {

	@Autowired
	private WeatherMapper weatherMapper;

	@Override
	public int insertRegionData(RegionWeather region) {
		return weatherMapper.insertRegionData(region);
	}

	@Transactional
	@Override
	public RegionWeather selectRegionData(int id) {
		return weatherMapper.selectRegionData(id);
	}
	
	@Transactional
	@Override
	public List<RegionWeather> selectSeoulList() {
		return weatherMapper.selectSeoulList();
	}

	@Override
	public List<RegionWeather> selectRegionWeatherList() {
		return weatherMapper.selectRegionWeatherList();
	}
	
	@Override
	public void updateWeather(RegionWeather region) {
		weatherMapper.updateWeather(region);
	}

	@Override
	public void deleteRegion(int regionId) {
		weatherMapper.deleteRegion(regionId);
	}

	@Override
	public void truncateRegion() {
		weatherMapper.truncateRegion();
	}

}
