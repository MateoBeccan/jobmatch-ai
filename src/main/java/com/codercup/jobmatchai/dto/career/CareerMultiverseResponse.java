package com.codercup.jobmatchai.dto.career;

import java.util.List;

public record CareerMultiverseResponse(
		String provider,
		CareerRegion region,
		CareerProfileResponse profile,
		List<CareerPathResponse> paths
) {
}
