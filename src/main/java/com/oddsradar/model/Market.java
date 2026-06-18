package com.oddsradar.model;

import lombok.Data;
import java.util.List;

@Data
public class Market {
    private String key;
    private List<Outcome> outcomes;
}
