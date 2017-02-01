# Ingest HTTP Plugin

The ingest HTTP plugin lets Elasticsearch to call an external web service and supplies the response to the document via the Ingest Node pipeline.

## Downloads

[ingest-http-5.2.0.zip](https://github.com/kosho/ingest-http/releases/download/5.2.0/ingest-http-5.2.0.zip)

## How to build

### IntelliJ IDEA

This repository is an IntelliJ IDEA project with `.idea` directory. In order to make a zipped plugin archive, goto **Build** > **Build Artifacts**, then choose **ingest-http:zip**. The gradle wrapper is used to make a build.

### Dependencies

- Elasticsearch 5.2.0
- org.apache.httpcomponents:httpclient:4.5.3

## Usages

### Installation

Stop the Elasticsearch service, then use the `elasticsearch-plugin` command to install the plugin.

```shell
$ bin/elasticsearch-plugin install file:///<path_to>/ingest-http.zip

```

### Options

| Name             | Required | Default  | Description                                                   |
|------------------|:--------:|:--------:|---------------------------------------------------------------|
| `field`          | yes      |          | The field name to be passed.                                  |
| `target_field`   | no       | response | The target JSON entity where the response to be.              |
| `url_prefix`     | yes      |          | The URL to be requested which `{}` is repalced with `field`.  |
| `ignore_missing` | no       | false    | `true` to ignore when `field` is empty or missing, otherwise `IllegalArgumentException` will be thrown.  |


### Testing the plugin

Before registering the pipeline, you can use the [Simulate Pipeline API](https://www.elastic.co/guide/en/elasticsearch/reference/master/simulate-pipeline-api.html) to call the `http` processor with the request body.


```json
POST _ingest/pipeline/_simulate
{
  "pipeline" :
  {
    "processors": [
      {
        "http" : {
          "field": "ip",
          "target_field: "response",
          "ignore_missing": true,
          "url_prefix" : "http://freegeoip.net/json/{}"
        }
      }
    ]
  },
  "docs": [
    {
      "_index": "index",
      "_type": "type",
      "_id": "id",
      "_source": {
        "ip": "8.8.8.8"
      }
    }
  ]
}
```