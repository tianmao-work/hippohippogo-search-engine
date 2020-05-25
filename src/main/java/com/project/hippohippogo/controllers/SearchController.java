package com.project.hippohippogo.controllers;

import com.project.hippohippogo.entities.DummyItem;
import com.project.hippohippogo.entities.Page;
import com.project.hippohippogo.entities.SearchQuery;
import com.project.hippohippogo.repositories.DummyRepository;
import com.project.hippohippogo.repositories.PagesRepository;
import com.project.hippohippogo.repositories.QueriesRepository;
import com.project.hippohippogo.services.QueryProcessorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Controller
public class SearchController {

    private DummyRepository dummyRepository;
    private QueriesRepository queriesRepository;
    private PagesRepository pagesRepository;
    private QueryProcessorService queryProcessorService;

    @Autowired
    public void setDummyRepository(DummyRepository dummyRepository) {
        this.dummyRepository = dummyRepository;
    }

    @Autowired
    public void setQueriesRepository(QueriesRepository queriesRepository) {
        this.queriesRepository = queriesRepository;
    }

    @Autowired
    public void setPagesRepository(PagesRepository pagesRepository) {
        this.pagesRepository = pagesRepository;
    }

    @Autowired
    public void setQueryProcessorService(QueryProcessorService queryProcessorService) {
        this.queryProcessorService = queryProcessorService;
    }

    @GetMapping("/search")
    public String getWebResults(Model model, @RequestParam("q") String queryString, @RequestParam(value = "offset", required = false, defaultValue = "0") int offset, @RequestParam(value = "limit", required = false, defaultValue = "20") int limit) {
        // Return to landing page if query is empty
        if (queryString.equals("")) {
            return "index";
        }

        // Register query for suggestions
        Optional<SearchQuery> searchQuery = queriesRepository.findById(queryString);
        if (!searchQuery.isPresent()) {
            SearchQuery newSearchQuery = new SearchQuery(queryString.toLowerCase());
            queriesRepository.save(newSearchQuery);
        } else {
            searchQuery.get().setQuery(searchQuery.get().getQuery().toLowerCase());
            searchQuery.get().incrementHits();
            queriesRepository.save(searchQuery.get());
        }

        // Get Results
        String title = queryString + " at HippoHippoGo";
        // Those are dummy ids. Ids should be received from the ranker service
        List<Integer> resultsIds = queryProcessorService.getPageResults(queryString);
        Pageable pageable = PageRequest.of(offset, limit);
        List<Page> results = pagesRepository.findAllByIdIn(resultsIds, pageable);
        model.addAttribute("query", queryString);
        model.addAttribute("results", results);
        return "showDummyData";
    }

    @GetMapping("/img")
    public String getImgResults(Model model) {
        List<DummyItem> items = (List<DummyItem>) dummyRepository.findAll();
        model.addAttribute("items", items);
        return "showDummyData";
    }
}