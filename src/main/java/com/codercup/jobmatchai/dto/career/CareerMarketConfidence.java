package com.codercup.jobmatchai.dto.career;

public enum CareerMarketConfidence {
	HIGH,
	MEDIUM,
	LOW,
	INSUFFICIENT;

	public static CareerMarketConfidence fromSampleSize(int sampleSize) {
		if (sampleSize >= 12) {
			return HIGH;
		}
		if (sampleSize >= 5) {
			return MEDIUM;
		}
		if (sampleSize >= 2) {
			return LOW;
		}
		return INSUFFICIENT;
	}
}
