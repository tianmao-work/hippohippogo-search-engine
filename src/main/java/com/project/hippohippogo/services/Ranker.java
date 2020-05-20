package com.project.hippohippogo.services;

import com.project.hippohippogo.entities.PageRank;
import com.project.hippohippogo.entities.PagesConnection;
import com.project.hippohippogo.repositories.PageRankRepository;
import com.project.hippohippogo.repositories.PagesConnectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class Ranker {
    private final int pageRankIterations = 20; // Number of iterations on page ranks
    float d = 0.85f; // Damping factor
    private PagesConnectionRepository pagesConnection;
    private PageRankRepository pageRankRepository;


    @Autowired
    public void setPagesConnection (PagesConnectionRepository pagesConnection) {
        this.pagesConnection = pagesConnection;
    }
    @Autowired
    public void setPageRankRepository(PageRankRepository pageRankRepository) {
        this.pageRankRepository = pageRankRepository;
    }

    // This function is used to set pages rank in its table
    public void rankPages() {
        // Empty table before beginning
        pageRankRepository.deleteAll();
        List<PagesConnection> pageConnectionsArray = (List<PagesConnection>) pagesConnection.findAll();
        // Holding Page rank of each iteration
        HashMap<String,Float> pageRankHashTable = new HashMap<String,Float>();
        // Temp map for page_rank table as a database table for page_rank table
        HashMap<String,PageRank> tempHashMap = new HashMap<String,PageRank>();
        // Initialize pages in page_rank table with rank = 1
        for (PagesConnection p : pageConnectionsArray) {
            Optional<PageRank> pageRank1 = pageRankRepository.findById(p.getReferred());
            Optional<PageRank> pageRank2 = pageRankRepository.findById(p.getReferring());
            // If we found that the page is added and we find it again in Referring column then outLinks++
            // Else add the page to page_rank table
            if (pageRank2.isPresent()) {
                pageRank2.get().setOut_links(pageRank2.get().getOut_links()+1);
                pageRankRepository.save(pageRank2.get());
                tempHashMap.put(pageRank2.get().getPage(),pageRank2.get());
            } else {
                PageRank pr = new PageRank(p.getReferring(),1,1);
                pageRankRepository.save(pr);
                tempHashMap.put(pr.getPage(),pr);
                pageRankHashTable.put(pr.getPage(),0f);
            }
            // If the page was in the referred column and it wasn't added before then add it to page_rank table
            if (pageRank1.isPresent()) {
                continue;
            } else {
                PageRank pr = new PageRank(p.getReferred(),1,0);
                pageRankRepository.save(pr);
                tempHashMap.put(pr.getPage(),pr);
                pageRankHashTable.put(pr.getPage(),0f);
            }
        }

        List<PageRank> pageRankList = pageRankRepository.findAll(); // Holding pages in Page Rank table to iterate on it and get the rank value for each page in it
        // This loop is used to iterate on page ranks and update them
        // For each loop we calculate PR(A) = (d-1)+d*(PR(B)/C(B)+...+PR(N)/C(N)
        for (int i=0;i<pageRankIterations;i++) {
            // Initialize each page rank with (1-d) at the beginning
            pageRankHashTable.replaceAll((k,v) -> (1-d));
            // Then iterate and when finding a page refer to another page update referred page rank value
            for (PagesConnection p : pageConnectionsArray) {
                PageRank pr = tempHashMap.get(p.getReferring());
                pageRankHashTable.put(p.getReferred(),pageRankHashTable.get(p.getReferred())+d*(pr.getRank()/pr.getOut_links()));
            }

            // Updating temp hash map with the last calculated values
            for (PageRank p : pageRankList) {
                p.setRank(pageRankHashTable.get(p.getPage()));
                tempHashMap.put(p.getPage(),p);
            }
        }

        // Updating database with the page rank values from the last iteration
        for (PageRank p : pageRankList) {
            p.setRank(tempHashMap.get(p.getPage()).getRank());
            pageRankRepository.save(p);
        }

    }

    
}