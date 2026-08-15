package com.mcm.passport.domain.journey.service;

public interface JourneyDataReader {

	JourneyDataSnapshot read(Long passportSessionId);
}
