package pl.planner.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.planner.dto.Attraction;
import pl.planner.dto.PlanRequest;
import pl.planner.dto.PlanResponse;
import pl.planner.dto.PlannerOptions;
import pl.planner.service.AttractionCatalog;
import pl.planner.service.OptionsService;
import pl.planner.service.PlannerService;

@RestController
@RequestMapping("/api/plans")
public class PlanController {
    private final AttractionCatalog catalog;
    private final OptionsService options;
    private final PlannerService planner;

    public PlanController(AttractionCatalog catalog, OptionsService options, PlannerService planner) {
        this.catalog = catalog;
        this.options = options;
        this.planner = planner;
    }

    @GetMapping("/attractions")
    List<Attraction> attractions(@RequestParam(required = false) String city,
                                 @RequestParam(required = false) String category) {
        return catalog.list(city, category);
    }

    @GetMapping("/attractions/{id}")
    Attraction attraction(@PathVariable String id) {
        return catalog.detail(id);
    }

    @GetMapping("/options")
    PlannerOptions options() {
        return options.options();
    }

    @PostMapping("/preview")
    PlanResponse preview(@Valid @RequestBody PlanRequest request) {
        return planner.plan(request, false);
    }

    @PostMapping("/optimize")
    PlanResponse optimize(@Valid @RequestBody PlanRequest request) {
        return planner.plan(request, true);
    }
}
