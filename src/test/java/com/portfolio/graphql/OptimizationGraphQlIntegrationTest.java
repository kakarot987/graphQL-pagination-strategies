package com.portfolio.graphql;

import com.portfolio.graphql.model.Activity;
import com.portfolio.graphql.repository.ProductRepository;
import com.portfolio.graphql.service.ActivityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureHttpGraphQlTester
class OptimizationGraphQlIntegrationTest {
    @Autowired HttpGraphQlTester tester;
    @Autowired ActivityService activityService;
    @Autowired ProductRepository products;

    @Test
    void facetsRespectCategoryPriceAndStock() {
        var result = tester.document("""
            { products(size: 100, filter: { category: "Electronics", minPrice: 1000, inStockOnly: true }) {
                nodes { id price } categoryFacets { name count }
                priceRange { min max }
                totalCount
            } }
            """).execute();
        List<Map> nodes = result.path("products.nodes").entityList(Map.class).get();
        List<Map> facets = result.path("products.categoryFacets").entityList(Map.class).get();
        assertThat(nodes).isNotEmpty();
        assertThat(facets).hasSize(1);
        assertThat(facets.get(0).get("name")).isEqualTo("Electronics");
        assertThat(((Number)facets.get(0).get("count")).longValue()).isEqualTo(nodes.size());
    }

    @Test
    void noMatchesReturnEmptyFacetsAndZeroPriceRange() {
        var response = tester.document("""
            { products(filter: { category: "__missing__" }) {
                categoryFacets { name count } priceRange { min max average }
            } }
            """).execute();
        response.path("products.categoryFacets").entityList(Map.class).hasSize(0);
        response.path("products.priceRange.min").entity(Double.class).isEqualTo(0.0);
        response.path("products.priceRange.max").entity(Double.class).isEqualTo(0.0);
    }

    @Test
    void moneyIsStoredAsDecimal() {
        assertThat(products.findAll().getFirst().getPrice()).isInstanceOf(BigDecimal.class);
    }

    @Test
    void offsetDuplicatesAfterInsertionButCursorDoesNot() {
        String query = "query($after:String){activityFeed(first:5,after:$after){nodes{id} pageInfo{endCursor}}}";
        var first = tester.document(query).execute();
        String saved = first.path("activityFeed.pageInfo.endCursor").entity(String.class).get();
        List<String> firstIds = first.path("activityFeed.nodes[*].id").entityList(String.class).get();
        List<String> expected = tester.document(query).variable("after",saved).execute()
            .path("activityFeed.nodes[*].id").entityList(String.class).get();
        activityService.createActivity(99999L,"comparison",Activity.ActivityType.VIEW,"new",null,null,null);
        List<String> next = tester.document(query).variable("after",saved).execute()
            .path("activityFeed.nodes[*].id").entityList(String.class).get();
        List<String> offset = tester.document("{activityFeedOffset(page:1,size:5){nodes{id}}}").execute()
            .path("activityFeedOffset.nodes[*].id").entityList(String.class).get();
        assertThat(next).containsExactlyElementsOf(expected);
        assertThat(next).doesNotContainAnyElementsOf(firstIds);
        assertThat(offset).contains(firstIds.getLast());
    }
}
