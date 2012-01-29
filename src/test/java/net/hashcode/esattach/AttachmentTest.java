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
    //uncomment this client if you want to use an ES already running 
    //client = new TransportClient().addTransportAddress(new InetSocketTransportAddress("localhost", 9300));
    client = node.client();
  }

  @Test
  public void mapperAttachmentTest() throws Exception {
    String idxName = "test";
    String idxType = "attachment";
    XContentBuilder map = jsonBuilder().startObject()
            .startObject(idxType)
              .startObject("properties")
                .startObject("file")
                  .field("type", "attachment")
                  .startObject("fields")
                    .startObject("title")
                      .field("store", "yes")
                    .endObject()
                    .startObject("file")
                      .field("term_vector","with_positions_offsets")
                      .field("store","yes")
                    .endObject()
                  .endObject()
                .endObject()
              .endObject()
         .endObject();
    try {
      client.admin().indices().prepareDelete(idxName).execute().actionGet();
    } catch (Exception ex) {}

    log.info("create index and mapping");
    CreateIndexResponse resp = client.admin().indices().prepareCreate(idxName).setSettings(
            ImmutableSettings.settingsBuilder()
            .put("number_of_shards", 1)
            .put("index.numberOfReplicas", 1))
            .addMapping("attachment", map).execute().actionGet();
    assertThat(resp.acknowledged(), equalTo(true));

    String pdfPath = ClassLoader.getSystemResource("fn6742.pdf").getPath();
    log.info("MD5: original file ");
    assertThat(DigestUtils.md5Hex(new FileInputStream(pdfPath)), equalTo("66ffff795be61474c7b611a70b72a7f2"));

    String data64 = org.elasticsearch.common.Base64.encodeFromFile(pdfPath);
    log.info("MD5: encoded file ");
    assertThat(DigestUtils.md5Hex(data64), equalTo("b83d178c1dbb8e6030b2aba3ad08a58b"));

    log.info("Indexing");
    XContentBuilder source = jsonBuilder().startObject()
            .field("file", data64).endObject();

    IndexResponse idxResp = client.prepareIndex().setIndex(idxName).setType(idxType).setId("80")
            .setSource(source).setRefresh(true).execute().actionGet();

    assertThat(idxResp.id(), equalTo("80"));
    assertThat(idxResp.type(), equalTo(idxType));

    String queryString = "amplifier";
    
    log.info("Searching by "+queryString);
    QueryBuilder query = QueryBuilders.queryString(queryString);

    SearchRequestBuilder searchBuilder = client.prepareSearch().setQuery(query)
            .addField("title")
            .addHighlightedField("file");
            
    SearchResponse search = searchBuilder.execute().actionGet();
    assertThat(search.hits().totalHits(), equalTo(1L));
    assertThat(search.hits().hits().length, equalTo(1));
    assertThat(search.hits().getAt(0).highlightFields().get("file"), notNullValue());
    assertThat(search.hits().getAt(0).highlightFields().get("file").toString(), containsString("<em>Amplifier</em>"));
    
    client.close();
  }
}
