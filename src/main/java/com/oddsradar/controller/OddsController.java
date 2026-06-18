package com.oddsradar.controller;

import com.oddsradar.model.BestOddsResult;
import com.oddsradar.service.OddsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/odds")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class OddsController {

    private final OddsService oddsService;

    @GetMapping("/best")
    public ResponseEntity<List<BestOddsResult>> getBestOdds(
            @RequestParam(defaultValue = "soccer_brazil_campeonato")
            String sport) {

        return ResponseEntity.ok(oddsService.getBestOdds(sport));
    }

    @GetMapping("/sports")
    public ResponseEntity<String> listSports() {
        return ResponseEntity.ok(
                "soccer_brazil_campeonato | soccer_brazil_serie_b |" +
                        " soccer_epl | basketball_nba | tennis_atp"
        );
    }
}
