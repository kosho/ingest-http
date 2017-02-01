package org.elasticsearch.plugin.ingest.http;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import org.elasticsearch.common.xcontent.json.JsonXContentParser;
import org.elasticsearch.ingest.AbstractProcessor;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.Processor;

import java.io.IOException;
import java.util.Map;

import static org.elasticsearch.ingest.ConfigurationUtils.readStringProperty;
import static org.elasticsearch.ingest.ConfigurationUtils.readBooleanProperty;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;

import org.elasticsearch.common.logging.Loggers;

public final class HttpProcessor extends AbstractProcessor {

    public static final String TYPE = "http";

    private final String field;
    private final String urlPrefix;
    private final String targetField;
    private final String extraHeader;

    private final Logger logger;

    public boolean isIgnoreMissing() {
        return ignoreMissing;
    }

    private final boolean ignoreMissing;

    HttpProcessor(String tag, String field, String targetField, String urlPrefix, String extraHeader,
                  boolean ignoreMissing) throws IOException {
        super(tag);
        this.field = field;
        this.urlPrefix = urlPrefix;
        this.targetField = targetField;
        this.ignoreMissing = ignoreMissing;
        this.extraHeader = extraHeader;
        this.logger = Loggers.getLogger(IngestHttpPlugin.class);
    }


    @Override
    public void execute(IngestDocument ingestDocument) throws java.io.IOException {

        String fieldValue = ingestDocument.getFieldValue(field, String.class, ignoreMissing);

        if (fieldValue == null && ignoreMissing) {
            return;
        } else if (fieldValue == null){
            throw new IllegalArgumentException("field [" + field + "] is null, cannot extract URL.");
        }

        CloseableHttpClient httpclient = HttpClients.createDefault();

        try {
            HttpGet httpget = new HttpGet(urlPrefix.replace("{}", fieldValue));

            if (extraHeader != null) {
                if (extraHeader.indexOf(":") > 0){
                    httpget.addHeader(extraHeader.substring(0, extraHeader.indexOf(":")).trim(), extraHeader.substring(extraHeader.indexOf(":") + 1).trim());
                }
            }

            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

                @Override
                public String handleResponse(
                        final HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        HttpEntity entity = response.getEntity();
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } else {
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                }
            };

            String responseBody = httpclient.execute(httpget, responseHandler);
            logger.debug("responseBody: " + responseBody);

            Map<String, Object> mapValue = JsonXContent.jsonXContent.createParser(NamedXContentRegistry.EMPTY, responseBody).map();
            ingestDocument.setFieldValue(targetField, mapValue);

        } finally {
            httpclient.close();
        }

    }

    @Override
    public String getType() {
        return TYPE;
    }

    String getField() {
        return field;
    }

    String getTargetField() {
        return targetField;
    }

    String getUrlPrefix() {
        return urlPrefix;
    }

    String getExtraHeader() { return extraHeader; }


    public static final class Factory implements Processor.Factory {

        @Override
        public HttpProcessor create(Map<String, Processor.Factory> registry, String processorTag,
                                    Map<String, Object> config) throws Exception {
            String field = readStringProperty(TYPE, processorTag, config, "field");
            String urlPrefix = readStringProperty(TYPE, processorTag, config, "url_prefix");
            String targetField = readStringProperty(TYPE, processorTag, config, "target_field", "out");
            String extraHeader = readStringProperty(TYPE, processorTag, config, "extra_header", "");
            boolean ignoreMissing = readBooleanProperty(TYPE, processorTag, config, "ignore_missing", false);

            return new HttpProcessor(processorTag, field, targetField, urlPrefix, extraHeader, ignoreMissing);
        }
    }

}
