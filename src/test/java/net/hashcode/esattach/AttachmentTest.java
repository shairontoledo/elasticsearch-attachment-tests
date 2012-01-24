package net.hashcode.esattach;

import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.client.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.index.IndexResponse;
import java.io.FileInputStream;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.codec.digest.DigestUtils;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.node.Node;
import static org.elasticsearch.node.NodeBuilder.*;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static org.elasticsearch.common.xcontent.XContentFactory.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class AttachmentTest {

  private Node node;
  Client client;
  ESLogger log = Loggers.getLogger(AttachmentTest.class);

  @BeforeClass
  public void setupNode() {
    node = nodeBuilder().local(true).node();
    client = node.client();
  }

  @Test
  public void testSimpleMappings() throws Exception {
    String idxName = "msdocs";
    String attachFiled = "attachment";
    XContentBuilder map = jsonBuilder().startObject()
            .startObject("properties").startObject(attachFiled)
            .field("type", "attachment")
            .field("store", "yes")
            .field("term_vector", "with_positions_offsets")
            .endObject().endObject().endObject();

    try {
      client.admin().indices().prepareDelete(idxName).execute().actionGet();
    } catch (Exception ex) {
    }

    log.info("create index and mapping");
    CreateIndexResponse resp = client.admin().indices().prepareCreate(idxName).setSettings(
            ImmutableSettings.settingsBuilder()
            .put("number_of_shards", 1)
            .put("index.numberOfReplicas", 1))
            .addMapping("doc", map).execute().actionGet();

    assertThat(resp.acknowledged(), equalTo(true));

    String pdfPath = ClassLoader.getSystemResource("es_doc_test.pdf").getPath();

    log.info("MD5: original file ");
    assertThat(DigestUtils.md5Hex(new FileInputStream(pdfPath)), equalTo("d606145e589ff30c348a0ffd2a45ae3f"));

    String data64 = org.elasticsearch.common.Base64.encodeFromFile(pdfPath);

    log.info("MD5: encoded file ");
    assertThat(DigestUtils.md5Hex(data64), equalTo("63dc6a1cdfdf390110555293793b0ddb"));

    log.info("Indexing");
    XContentBuilder source = jsonBuilder().startObject()
            .field("filename", "elasticsearch home.pdf")
            .field("folder", "/foo/bar/folder")
            .field(attachFiled, data64).endObject();

    IndexResponse idxResp = client.prepareIndex().setIndex(idxName).setType("doc").setId("77")
            .setSource(source).setRefresh(true).execute().actionGet();

    assertThat(idxResp.id(), equalTo("77"));
    assertThat(idxResp.type(), equalTo("doc"));

     String queryString = "elasticsearch";
    log.info("Searching by "+queryString);
    QueryBuilder query = QueryBuilders.queryString(queryString);

    SearchRequestBuilder searchBuilder = client.prepareSearch().setQuery(query)
            .addHighlightedField(attachFiled)
            .addHighlightedField("filename");

    SearchResponse search = searchBuilder.execute().actionGet();
    client.close();

    assertThat(search.hits().totalHits(), equalTo(1L));
    assertThat(search.hits().hits().length, equalTo(1));
    assertThat(search.hits().getAt(0).highlightFields().get("filename"), notNullValue());
    assertThat(search.hits().getAt(0).highlightFields().get(attachFiled), notNullValue());
  }
}
