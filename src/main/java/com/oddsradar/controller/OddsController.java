package com.oddsradar.controller;

import com.oddsradar.model.ArbitrageCalculation;
import com.oddsradar.model.ArbitrageCalculationRequest;
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

    /**
     * Debug — lista todos os títulos de bookmakers que a The Odds API
     * está retornando para um determinado esporte.
     * Use para confirmar os nomes exatos e ajustar o filtro BOOKMAKERS_BR.
     * Exemplo: GET /api/odds/bookmakers?sport=soccer_brazil_campeonato
     */
    @GetMapping("/bookmakers")
    public ResponseEntity<List<String>> getAvailableBookmakers(
            @RequestParam(defaultValue = "soccer_brazil_campeonato")
            String sport) {
        return ResponseEntity.ok(oddsService.getAvailableBookmakers(sport));
    }

    @PostMapping("/calculate-arbitrage")
    public ResponseEntity<ArbitrageCalculation> calculateArbitrage(
            @RequestBody ArbitrageCalculationRequest request) {

        ArbitrageCalculation result = oddsService.calculateArbitrage(
                request.getOdds(),
                request.getBookmakers(),
                request.getOutcomeNames(),
                request.getTotalStake()
        );

        return ResponseEntity.ok(result);
    }
}
