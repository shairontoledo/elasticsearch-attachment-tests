#ElasticSearch mapper-attachment plugin test case
It's a java port from Lukáš Vlček's [blog posted script](http://www.elasticsearch.org/tutorials/2011/07/18/attachment-type-in-action.html)

##Maven dependency for elasticsearch-mapper-attachments 1.0.0

Downloaded from

    bin/plugin -install elasticsearch/elasticsearch-mapper-attachments/1.0.0
    
##Mapper plugin register at main/resources/es-plugin.properties

    plugin=org.elasticsearch.plugin.mapper.attachments.MapperAttachmentsPlugin