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
     * Calcula a distribuição ideal de stakes para um conjunto de odds (arbitragem),
     * dado um valor total a apostar. Usado pela calculadora no modal do frontend.
     */
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
