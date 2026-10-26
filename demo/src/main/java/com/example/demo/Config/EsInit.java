package com.example.demo.Config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Component;

import com.example.demo.Mapper.ProductsMapper;
import com.example.demo.pojo.Products;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class EsInit implements ApplicationRunner{

    private final ElasticsearchOperations eOperations;
    private final ProductsMapper productsMapper;

    public EsInit(ElasticsearchOperations eOperations, ProductsMapper productsMapper){
        this.eOperations = eOperations;
        this.productsMapper = productsMapper;
    }

    @Override
    public void run(ApplicationArguments args){
        IndexOperations indexOperations = eOperations.indexOps(Products.class);

        if(!indexOperations.exists()){
            indexOperations.createWithMapping();
            log.info("索引创建完成，索引名字为：{}",indexOperations.getIndexCoordinates().getIndexName());
        }else{
            log.info("索引已经存在，索引名字：{}",indexOperations.getIndexCoordinates().getIndexName());
        }

        var products = productsMapper.getAllProductsForIndex();
        if (products.isEmpty()) {
            log.info("MySQL 商品表没有商品，无需导入 ES");
            return;
        }

        eOperations.save(products);
        log.info("已将 {} 条商品写入 ES", products.size());
    }

    public SearchHits<Products> searchProducts(String keyword){

        Query query = NativeQuery.builder()
        .withQuery(q -> q.bool(b -> b
                .must(m -> m.match(t -> t.field("name").query(keyword)))
                .filter(f -> f.term(t -> t.field("status").value(1)))
        ))
        .withPageable(PageRequest.of(0, 10))
        .withSort(Sort.by(Sort.Order.desc("sale")))
        .build();

        SearchHits<Products> hits = eOperations.search(query, Products.class); 
        
        return hits;
    }
    
}
