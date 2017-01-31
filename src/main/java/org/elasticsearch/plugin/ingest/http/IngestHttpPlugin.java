package org.elasticsearch.plugin.ingest.http;

import java.util.Collections;
import java.util.Map;

import org.elasticsearch.ingest.Processor;
import org.elasticsearch.plugins.IngestPlugin;
import org.elasticsearch.plugins.Plugin;

public class IngestHttpPlugin extends Plugin implements IngestPlugin {

    @Override
    public Map<String, Processor.Factory> getProcessors(Processor.Parameters parameters) {
        return Collections.singletonMap(HttpProcessor.TYPE, new HttpProcessor.Factory());
    }
}
