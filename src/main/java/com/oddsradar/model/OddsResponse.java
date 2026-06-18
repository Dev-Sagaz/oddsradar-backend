package com.oddsradar.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OddsResponse {
    private String id;
    private String sport_key;
    private String sport_title;
    private String commence_time;
    private String home_team;
    private String away_team;
    private List<Bookmaker> bookmakers;
}
