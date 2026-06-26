package com.oddsradar.client;

import com.oddsradar.model.OddsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OddsApiClient {

    private final RestTemplate restTemplate;

    @Value("${oddsapi.key}")
    private String apiKey;

    private static final String BASE_URL =
            "https://api.the-odds-api.com/v4/sports";

    public List<OddsResponse> getOdds(String sport) {
        String url = BASE_URL + "/" + sport + "/odds"
                + "?apiKey=" + apiKey
                + "&regions=eu,uk,us,au"
                + "&markets=h2h"
                + "&oddsFormat=decimal";

        OddsResponse[] response = restTemplate.getForObject(
                url, OddsResponse[].class);

        return response != null ? Arrays.asList(response)
                : Collections.emptyList();
    }
}
