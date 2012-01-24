#ElasticSearch mapper-attachment plugin test case

##Maven dependency for elasticsearch-mapper-attachments 1.0.0

Downloaded from

    bin/plugin -install elasticsearch/elasticsearch-mapper-attachments/1.0.0
    
Or add 0.17.6 to pom.xml

    <dependency>
      <groupId>org.elasticsearch</groupId>
      <artifactId>elasticsearch-mapper-attachments</artifactId>
      <version>0.17.6</version>
    </dependency>
    
##Mapper plugin register at main/resources/es-plugin.properties

    plugin=org.elasticsearch.plugin.mapper.attachments.MapperAttachmentsPlugin
    
    
