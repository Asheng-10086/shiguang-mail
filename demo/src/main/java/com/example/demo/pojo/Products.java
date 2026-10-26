package com.example.demo.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

@Data
@Document(indexName = "products",createIndex = true)
public class Products {

    @Id
    private Long id;                // 主键 ID

    @Field(type = FieldType.Long)
    private Long brandId;           // 品牌 ID

    @Field(type = FieldType.Long)
    private Long categoryId;        // 分类 ID

    @MultiField(
        mainField = @Field(type = FieldType.Text,
            analyzer = "ik_max_word",
            searchAnalyzer = "ik_smart"
        ),
        otherFields = @InnerField(suffix = "keyword",type = FieldType.Keyword)
    )
    private String name;            // 商品名称

    @Field(type = FieldType.Double)
    private BigDecimal price;       // 销售价格

    @Field(type = FieldType.Long)
    private Integer stock;          // 库存数量

    @Field(type = FieldType.Long)
    private Integer sale;           // 销量

    @Field(type = FieldType.Text,
        analyzer = "ik_max_word",
        searchAnalyzer = "ik_smart"
    )
    private String detail;          // 商品详情

    @Field(type = FieldType.Long)
    private Integer status;         // 上架状态 (0：下架，1：上架)


}
