package com.oddsradar.model;

import lombok.Data;
import java.util.List;

@Data
public class Bookmaker {
    private String key;
    private String title;
    private String last_update;
    private List<Market> markets;
}
